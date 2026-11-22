package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.ui.ActionBar.Theme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class KronoBackupManager {

    public static final String CLOUD_BACKUP_BOT_USERNAME = "KronoGramCloudBot";
    public static final String BACKUP_FORMAT_ID = "kronogram_backup";
    public static final int BACKUP_SCHEMA_VERSION = 1;
    public static final String BACKUP_FILE_EXTENSION = ".kronogram";

    private static final String CLOUD_KEY_PREFIX = "kg_bkp_v1_";
    private static final String CLOUD_META_KEY = CLOUD_KEY_PREFIX + "meta";
    private static final String CLOUD_CHUNK_PREFIX = CLOUD_KEY_PREFIX + "c";
    private static final int CLOUD_CHUNK_SIZE = 3500;
    private static final int CLOUD_MAX_CHUNKS = 256;
    private static final int CLOUD_GET_BATCH_SIZE = 20;
    private static final int MAX_BACKUP_FILE_BYTES = 5 * 1024 * 1024;

    private static final long DAILY_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final long WEEKLY_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final long AUTO_CHECK_THROTTLE_MS = 60L * 1000L;
    private static final long ON_CHANGE_DEBOUNCE_MS = 20L * 1000L;
    private static final long ON_CHANGE_MIN_COOLDOWN_MS = 2L * 60L * 1000L;

    private static final AtomicBoolean autoBackupInProgress = new AtomicBoolean(false);
    private static final AtomicBoolean dirtyDuringUpload = new AtomicBoolean(false);
    private static volatile int suppressObserverDepth = 0;
    private static volatile long lastAutoCheckElapsedMs = 0L;
    private static volatile long lastAutoUploadElapsedMs = 0L;
    private static final Map<Integer, TLRPC.User> cachedBotByAccount = new HashMap<>();

    // Strong references so Android SharedPreferences WeakHashMap never drops listeners
    private static volatile boolean observerInitialized = false;
    private static volatile int observedAccount = 0;
    private static SharedPreferences observedAccountNotifPrefs = null;
    private static SharedPreferences.OnSharedPreferenceChangeListener kronoPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener mainPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener themePrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener notifPrefListener;
    private static NotificationCenter.NotificationCenterDelegate connectionStateDelegate;
    private static final Runnable debouncedOnChangeRunnable = () -> executeOnChangeBackup(observedAccount);

    public static class BackupOptions {
        public boolean includeKrono = true;
        public boolean includeTelegramMain = true;
        public boolean includeTelegramTheme = true;
        public boolean includeTelegramNotifications = false;
        public boolean includeHiddenVault = false;

        public boolean hasAnySelected() {
            return includeKrono || includeTelegramMain || includeTelegramTheme || includeTelegramNotifications || includeHiddenVault;
        }

        public static BackupOptions createDefault() {
            return new BackupOptions();
        }

        public static BackupOptions fromAutoConfig() {
            KronoConfig.loadConfig();
            BackupOptions opts = new BackupOptions();
            opts.includeKrono = true;
            opts.includeTelegramMain = KronoConfig.cloudAutoIncludeTelegram;
            opts.includeTelegramTheme = KronoConfig.cloudAutoIncludeTheme;
            opts.includeTelegramNotifications = KronoConfig.cloudAutoIncludeNotifications;
            opts.includeHiddenVault = KronoConfig.cloudAutoIncludeVault;
            return opts;
        }

        public String buildCategoriesSummary() {
            ArrayList<String> list = new ArrayList<>();
            if (includeKrono) list.add("KronoGram");
            if (includeTelegramMain) list.add("Telegram");
            if (includeTelegramTheme) list.add("Temas");
            if (includeTelegramNotifications) list.add("Notificaciones");
            if (includeHiddenVault) list.add("Bóveda");
            return TextUtils.join(", ", list);
        }
    }

    public static class BackupMetadata {
        public int schemaVersion = BACKUP_SCHEMA_VERSION;
        public long timestamp;
        public String deviceModel = "";
        public int chunkCount;
        public int rawByteSize;
        public String sha256 = "";
        public boolean hasKrono;
        public boolean hasTelegramMain;
        public boolean hasTelegramTheme;
        public boolean hasTelegramNotifications;
        public boolean hasHiddenVault;

        public String buildCategoriesSummary() {
            ArrayList<String> list = new ArrayList<>();
            if (hasKrono) list.add("KronoGram");
            if (hasTelegramMain) list.add("Telegram");
            if (hasTelegramTheme) list.add("Temas");
            if (hasTelegramNotifications) list.add("Notificaciones");
            if (hasHiddenVault) list.add("Bóveda");
            return list.isEmpty() ? "Sin categorías" : TextUtils.join(", ", list);
        }
    }

    public static class ParsedBackup {
        public BackupMetadata metadata;
        public JSONObject root;
    }

    public static class RestoreResult {
        public int appliedKeysCount;
        public boolean requiresRestart;
        public boolean restoredVault;
    }

    // -------------------------------------------------------------------------
    // JSON Serialization & Deserialization (Typed SharedPreferences)
    // -------------------------------------------------------------------------

    public static JSONObject buildBackupJson(int account, BackupOptions options) throws Exception {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            throw new IllegalStateException("Contexto de aplicación no disponible");
        }
        if (options == null || !options.hasAnySelected()) {
            throw new IllegalArgumentException("Debes seleccionar al menos una categoría");
        }
        KronoConfig.loadConfig();

        JSONObject root = new JSONObject();
        root.put("format", BACKUP_FORMAT_ID);
        root.put("schema", BACKUP_SCHEMA_VERSION);
        root.put("timestamp", System.currentTimeMillis());
        root.put("device", getDeviceModelLabel());

        JSONObject sections = new JSONObject();

        if (options.includeKrono) {
            SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
            sections.put("krono", serializePreferences(kronoPrefs, KronoBackupManager::isExcludedKronoKey, false));
        }

        if (options.includeHiddenVault) {
            JSONObject vaultObj = new JSONObject();
            SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
            putTypedEntry(vaultObj, "hiddenUseBiometric", kronoPrefs.getBoolean("hiddenUseBiometric", true));
            putTypedEntry(vaultObj, "hiddenMuteNotifications", kronoPrefs.getBoolean("hiddenMuteNotifications", true));
            putTypedEntry(vaultObj, "hiddenHideFolderChatsEverywhere", kronoPrefs.getBoolean("hiddenHideFolderChatsEverywhere", true));
            putTypedEntry(vaultObj, "hiddenAutoLockOnBackground", kronoPrefs.getBoolean("hiddenAutoLockOnBackground", true));
            String hiddenDialogs = kronoPrefs.getString("hiddenDialogs_" + account, "");
            String hiddenFilters = kronoPrefs.getString("hiddenFilters_" + account, "");
            putTypedEntry(vaultObj, "hiddenDialogs", hiddenDialogs);
            putTypedEntry(vaultObj, "hiddenFilters", hiddenFilters);
            sections.put("vault", vaultObj);
        }

        if (options.includeTelegramMain) {
            SharedPreferences mainPrefs = context.getSharedPreferences("mainconfig", Context.MODE_PRIVATE);
            sections.put("tg_main", serializePreferences(mainPrefs, KronoBackupManager::isExcludedMainConfigKey, false));
        }

        if (options.includeTelegramTheme) {
            SharedPreferences themePrefs = context.getSharedPreferences("themeconfig", Context.MODE_PRIVATE);
            sections.put("tg_theme", serializePreferences(themePrefs, KronoBackupManager::isExcludedThemeConfigKey, true));
        }

        if (options.includeTelegramNotifications) {
            SharedPreferences globalNotifPrefs = MessagesController.getGlobalNotificationsSettings();
            JSONObject notifBundle = new JSONObject();
            notifBundle.put("global", serializePreferences(globalNotifPrefs, KronoBackupManager::isExcludedNotificationKey, true));
            if (account != 0) {
                SharedPreferences accountNotifPrefs = MessagesController.getNotificationsSettings(account);
                notifBundle.put("account", serializePreferences(accountNotifPrefs, KronoBackupManager::isExcludedNotificationKey, true));
            }
            sections.put("tg_notifications", notifBundle);
        }

        root.put("sections", sections);
        return root;
    }

    public static ParsedBackup parseBackupJson(String rawJson) throws Exception {
        if (TextUtils.isEmpty(rawJson)) {
            throw new IllegalArgumentException("El contenido de la copia está vacío");
        }
        JSONObject root = new JSONObject(rawJson);
        String format = root.optString("format", "");
        if (!BACKUP_FORMAT_ID.equals(format)) {
            throw new IllegalArgumentException("El archivo no es una copia de seguridad válida de KronoGram");
        }
        int schema = root.optInt("schema", 0);
        if (schema <= 0 || schema > BACKUP_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Versión de copia no compatible (v" + schema + ")");
        }
        JSONObject sections = root.optJSONObject("sections");
        if (sections == null || sections.length() == 0) {
            throw new IllegalArgumentException("La copia no contiene ninguna sección de ajustes");
        }

        BackupMetadata meta = new BackupMetadata();
        meta.schemaVersion = schema;
        meta.timestamp = root.optLong("timestamp", 0L);
        meta.deviceModel = root.optString("device", "");
        meta.rawByteSize = rawJson.getBytes(StandardCharsets.UTF_8).length;
        meta.sha256 = sha256Hex(rawJson);
        meta.hasKrono = sections.has("krono") && sections.optJSONObject("krono") != null;
        meta.hasHiddenVault = sections.has("vault") && sections.optJSONObject("vault") != null;
        meta.hasTelegramMain = sections.has("tg_main") && sections.optJSONObject("tg_main") != null;
        meta.hasTelegramTheme = sections.has("tg_theme") && sections.optJSONObject("tg_theme") != null;
        meta.hasTelegramNotifications = sections.has("tg_notifications") && sections.optJSONObject("tg_notifications") != null;

        ParsedBackup parsed = new ParsedBackup();
        parsed.metadata = meta;
        parsed.root = root;
        return parsed;
    }

    public static RestoreResult applyBackup(int account, ParsedBackup backup, BackupOptions options) throws Exception {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            throw new IllegalStateException("Contexto de aplicación no disponible");
        }
        if (backup == null || backup.root == null) {
            throw new IllegalArgumentException("Copia de seguridad inválida");
        }
        JSONObject sections = backup.root.optJSONObject("sections");
        if (sections == null) {
            throw new IllegalArgumentException("La copia no contiene datos restaurables");
        }

        RestoreResult result = new RestoreResult();
        setSuppressObserver(true);
        try {
            if (options.includeKrono && sections.has("krono")) {
                JSONObject kronoObj = sections.optJSONObject("krono");
                if (kronoObj != null) {
                    SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
                    result.appliedKeysCount += deserializeIntoPreferences(kronoPrefs, kronoObj, KronoBackupManager::isExcludedKronoKey);
                }
            }

            if (options.includeHiddenVault && sections.has("vault")) {
                JSONObject vaultObj = sections.optJSONObject("vault");
                if (vaultObj != null) {
                    SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = kronoPrefs.edit();
                    int count = 0;
                    if (vaultObj.has("hiddenUseBiometric")) {
                        count += applyTypedEntry(editor, "hiddenUseBiometric", vaultObj.optJSONObject("hiddenUseBiometric"));
                    }
                    if (vaultObj.has("hiddenMuteNotifications")) {
                        count += applyTypedEntry(editor, "hiddenMuteNotifications", vaultObj.optJSONObject("hiddenMuteNotifications"));
                    }
                    if (vaultObj.has("hiddenHideFolderChatsEverywhere")) {
                        count += applyTypedEntry(editor, "hiddenHideFolderChatsEverywhere", vaultObj.optJSONObject("hiddenHideFolderChatsEverywhere"));
                    }
                    if (vaultObj.has("hiddenAutoLockOnBackground")) {
                        count += applyTypedEntry(editor, "hiddenAutoLockOnBackground", vaultObj.optJSONObject("hiddenAutoLockOnBackground"));
                    }
                    if (vaultObj.has("hiddenDialogs")) {
                        count += applyTypedEntry(editor, "hiddenDialogs_" + account, vaultObj.optJSONObject("hiddenDialogs"));
                    }
                    if (vaultObj.has("hiddenFilters")) {
                        count += applyTypedEntry(editor, "hiddenFilters_" + account, vaultObj.optJSONObject("hiddenFilters"));
                    }
                    editor.commit();
                    result.appliedKeysCount += count;
                    result.restoredVault = count > 0;
                }
            }

            if (options.includeTelegramMain && sections.has("tg_main")) {
                JSONObject mainObj = sections.optJSONObject("tg_main");
                if (mainObj != null) {
                    SharedPreferences mainPrefs = context.getSharedPreferences("mainconfig", Context.MODE_PRIVATE);
                    int c = deserializeIntoPreferences(mainPrefs, mainObj, KronoBackupManager::isExcludedMainConfigKey);
                    result.appliedKeysCount += c;
                    if (c > 0) {
                        result.requiresRestart = true;
                    }
                }
            }

            if (options.includeTelegramTheme && sections.has("tg_theme")) {
                JSONObject themeObj = sections.optJSONObject("tg_theme");
                if (themeObj != null) {
                    SharedPreferences themePrefs = context.getSharedPreferences("themeconfig", Context.MODE_PRIVATE);
                    int c = deserializeIntoPreferences(themePrefs, themeObj, key -> isExcludedThemeConfigKey(key) || isUnavailableThemeReference(key, themeObj));
                    result.appliedKeysCount += c;
                    if (c > 0) {
                        result.requiresRestart = true;
                    }
                }
            }

            if (options.includeTelegramNotifications && sections.has("tg_notifications")) {
                JSONObject notifBundle = sections.optJSONObject("tg_notifications");
                if (notifBundle != null) {
                    JSONObject globalObj = notifBundle.optJSONObject("global");
                    if (globalObj != null) {
                        SharedPreferences globalNotifPrefs = MessagesController.getGlobalNotificationsSettings();
                        int c = deserializeIntoPreferences(globalNotifPrefs, globalObj, KronoBackupManager::isExcludedNotificationKey);
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                    }
                    JSONObject accObj = notifBundle.optJSONObject("account");
                    if (accObj != null && account != 0) {
                        SharedPreferences accNotifPrefs = MessagesController.getNotificationsSettings(account);
                        int c = deserializeIntoPreferences(accNotifPrefs, accObj, KronoBackupManager::isExcludedNotificationKey);
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                    }
                }
            }

            KronoConfig.reloadConfigFromDisk();
            try {
                JSONObject postRestoreJson = buildBackupJson(account, BackupOptions.fromAutoConfig());
                JSONObject postSections = postRestoreJson.optJSONObject("sections");
                String postHash = sha256Hex(postSections != null ? postSections.toString() : postRestoreJson.toString());
                KronoConfig.recordCloudBackupCompleted(System.currentTimeMillis(), postHash);
            } catch (Exception ignore) {}
        } finally {
            setSuppressObserver(false);
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload, true);
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogFiltersUpdated);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        return result;
    }

    private interface KeyFilter {
        boolean isExcluded(String key);
    }

    private static JSONObject serializePreferences(SharedPreferences prefs, KeyFilter filter, boolean skipLocalUris) throws Exception {
        JSONObject out = new JSONObject();
        if (prefs == null) {
            return out;
        }
        Map<String, ?> all = prefs.getAll();
        if (all == null) {
            return out;
        }
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (TextUtils.isEmpty(key) || val == null) {
                continue;
            }
            if (filter != null && filter.isExcluded(key)) {
                continue;
            }
            if (skipLocalUris && val instanceof String && looksLikeDeviceLocalPathOrUri((String) val)) {
                continue;
            }
            putTypedEntry(out, key, val);
        }
        return out;
    }

    private static void putTypedEntry(JSONObject target, String key, Object val) throws Exception {
        JSONObject item = new JSONObject();
        if (val instanceof Boolean) {
            item.put("t", "b");
            item.put("v", val);
        } else if (val instanceof Integer) {
            item.put("t", "i");
            item.put("v", val);
        } else if (val instanceof Long) {
            item.put("t", "l");
            item.put("v", val);
        } else if (val instanceof Float) {
            item.put("t", "f");
            item.put("v", ((Float) val).doubleValue());
        } else if (val instanceof String) {
            item.put("t", "s");
            item.put("v", val);
        } else if (val instanceof Set<?>) {
            JSONArray arr = new JSONArray();
            for (Object o : (Set<?>) val) {
                if (o instanceof String) {
                    arr.put(o);
                }
            }
            item.put("t", "ss");
            item.put("v", arr);
        } else {
            return;
        }
        target.put(key, item);
    }

    private static int deserializeIntoPreferences(SharedPreferences prefs, JSONObject source, KeyFilter filter) {
        if (prefs == null || source == null) {
            return 0;
        }
        SharedPreferences.Editor editor = prefs.edit();
        int count = 0;
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (TextUtils.isEmpty(key) || (filter != null && filter.isExcluded(key))) {
                continue;
            }
            JSONObject entry = source.optJSONObject(key);
            count += applyTypedEntry(editor, key, entry);
        }
        editor.commit();
        return count;
    }

    private static int applyTypedEntry(SharedPreferences.Editor editor, String key, JSONObject entry) {
        if (editor == null || entry == null || !entry.has("t") || !entry.has("v")) {
            return 0;
        }
        String type = entry.optString("t", "");
        try {
            switch (type) {
                case "b":
                    editor.putBoolean(key, entry.getBoolean("v"));
                    return 1;
                case "i":
                    editor.putInt(key, entry.getInt("v"));
                    return 1;
                case "l":
                    editor.putLong(key, entry.getLong("v"));
                    return 1;
                case "f":
                    editor.putFloat(key, (float) entry.getDouble("v"));
                    return 1;
                case "s":
                    editor.putString(key, entry.getString("v"));
                    return 1;
                case "ss":
                    JSONArray arr = entry.getJSONArray("v");
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        set.add(arr.getString(i));
                    }
                    editor.putStringSet(key, set);
                    return 1;
                default:
                    return 0;
            }
        } catch (Exception e) {
            FileLog.e(e);
            return 0;
        }
    }

    private static boolean looksLikeDeviceLocalPathOrUri(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String lower = value.toLowerCase(Locale.US);
        return lower.startsWith("content://")
                || lower.startsWith("file://")
                || lower.startsWith("/data/")
                || lower.startsWith("/storage/")
                || lower.startsWith("/sdcard/")
                || lower.startsWith("/mnt/");
    }

    private static boolean isVaultKronoKey(String key) {
        if (TextUtils.isEmpty(key)) return false;
        return key.equals("hiddenUseBiometric")
                || key.equals("hiddenMuteNotifications")
                || key.equals("hiddenHideFolderChatsEverywhere")
                || key.equals("hiddenAutoLockOnBackground")
                || key.startsWith("hiddenDialogs_")
                || key.startsWith("hiddenFilters_");
    }

    private static boolean isExcludedKronoKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        return key.equals("hiddenLockHash")
                || key.equals("hiddenLockType")
                || isVaultKronoKey(key)
                || key.startsWith("ghostLocalRead_")
                || key.startsWith("ghostPrev_")
                || key.startsWith("telemetryPrev_")
                || key.equals("cloudLastBackupTime")
                || key.equals("cloudLastBackupHash")
                || key.equals("cloudBackupPending");
    }

    private static boolean isExcludedMainConfigKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        String lower = key.toLowerCase(Locale.US);
        return lower.contains("passcode")
                || lower.contains("applocked")
                || lower.contains("fingerprint")
                || lower.contains("pushstring")
                || lower.contains("pushstat")
                || lower.contains("gcm")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("hint")
                || lower.contains("camera_cache")
                || lower.equals("cameracache")
                || lower.equals("deviceperformanceclass")
                || lower.equals("selectedaccount")
                || lower.equals("hasemaillogin")
                || lower.equals("directsharehash2")
                || lower.equals("proxy_dialog")
                || lower.equals("proxydialogaddress")
                || lower.equals("nextpromoinfochecktime")
                || lower.equals("lastkeepmediachecktime")
                || lower.equals("lastlogschecktime")
                || lower.equals("lastupdatechecktime")
                || lower.equals("pendingappupdate")
                || lower.equals("pendingappupdatebuild")
                || lower.equals("dontaskmanagestorage")
                || lower.startsWith("storage_cache_dir")
                || lower.startsWith("save_gallery_");
    }

    private static boolean isExcludedThemeConfigKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        String lower = key.toLowerCase(Locale.US);
        return lower.contains("path")
                || lower.contains("wallpaper_file")
                || lower.startsWith("theme_file_");
    }

    private static boolean isUnavailableThemeReference(String key, JSONObject themeObj) {
        if (!"theme".equals(key) && !"lastDayTheme".equals(key) && !"lastDarkTheme".equals(key)) {
            return false;
        }
        JSONObject entry = themeObj.optJSONObject(key);
        if (entry == null || !"s".equals(entry.optString("t"))) {
            return false;
        }
        String themeName = entry.optString("v", "");
        if (TextUtils.isEmpty(themeName)) {
            return true;
        }
        return Theme.getTheme(themeName) == null;
    }

    private static boolean isExcludedNotificationKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        String lower = key.toLowerCase(Locale.US);
        return lower.contains("sound_path")
                || lower.contains("ringtone_path")
                || lower.startsWith("notify_channel")
                || lower.startsWith("dialog_show_translate_count")
                || lower.contains("token")
                || lower.contains("fcm");
    }

    // -------------------------------------------------------------------------
    // Local File & Saved Messages Export / Import
    // -------------------------------------------------------------------------

    public static String buildSuggestedFileName() {
        String date = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
        return "KronoGram_Backup_" + date + BACKUP_FILE_EXTENSION;
    }

    public static void exportToUri(Context context, int account, Uri uri, BackupOptions options, Utilities.Callback<BackupMetadata> onSuccess, Utilities.Callback<String> onError) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                JSONObject json = buildBackupJson(account, options);
                String formatted = json.toString(2);
                byte[] bytes = formatted.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) {
                        throw new IllegalStateException("No se pudo abrir el archivo de destino");
                    }
                    os.write(bytes);
                    os.flush();
                }
                ParsedBackup parsed = parseBackupJson(formatted);
                AndroidUtilities.runOnUIThread(() -> {
                    if (onSuccess != null) onSuccess.run(parsed.metadata);
                });
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage() != null ? e.getMessage() : "Error al exportar archivo";
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(msg);
                });
            }
        });
    }

    public static void parseFromUri(Context context, Uri uri, Utilities.Callback<ParsedBackup> onSuccess, Utilities.Callback<String> onError) {
        Utilities.globalQueue.postRunnable(() -> {
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    throw new IllegalStateException("No se pudo leer el archivo seleccionado");
                }
                String content = readStreamUtf8Bounded(is);
                ParsedBackup parsed = parseBackupJson(content);
                AndroidUtilities.runOnUIThread(() -> {
                    if (onSuccess != null) onSuccess.run(parsed);
                });
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage() != null ? e.getMessage() : "Archivo de copia inválido";
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(msg);
                });
            }
        });
    }

    public static void parseFromFile(File file, Utilities.Callback<ParsedBackup> onSuccess, Utilities.Callback<String> onError) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                if (file == null || !file.exists() || !file.canRead()) {
                    throw new IllegalStateException("El archivo todavía no está descargado o no se puede leer");
                }
                try (InputStream is = new FileInputStream(file)) {
                    String content = readStreamUtf8Bounded(is);
                    ParsedBackup parsed = parseBackupJson(content);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onSuccess != null) onSuccess.run(parsed);
                    });
                }
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage() != null ? e.getMessage() : "No se pudo leer la copia de KronoGram";
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(msg);
                });
            }
        });
    }

    public static void exportToSavedMessages(int account, BackupOptions options, Utilities.Callback<String> onSuccess, Utilities.Callback<String> onError) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                long selfId = UserConfig.getInstance(account).getClientUserId();
                if (selfId == 0) {
                    throw new IllegalStateException("Debes iniciar sesión en una cuenta de Telegram");
                }
                JSONObject json = buildBackupJson(account, options);
                String formatted = json.toString(2);
                File cacheDir = FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE);
                if (cacheDir == null || !cacheDir.exists()) {
                    cacheDir = ApplicationLoader.applicationContext.getCacheDir();
                }
                String fileName = buildSuggestedFileName();
                File outFile = new File(cacheDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(formatted.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                }
                String dateText = formatTimestamp(System.currentTimeMillis());
                String caption = "Copia de seguridad de KronoGram\n"
                        + "Fecha: " + dateText + "\n"
                        + "Dispositivo: " + getDeviceModelLabel() + "\n"
                        + "Categorías: " + options.buildCategoriesSummary() + "\n"
                        + "Toca este archivo para restaurar tus ajustes.";
                final String path = outFile.getAbsolutePath();
                AndroidUtilities.runOnUIThread(() -> {
                    SendMessagesHelper.prepareSendingDocument(
                            AccountInstance.getInstance(account),
                            path,
                            path,
                            null,
                            caption,
                            "application/json",
                            selfId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            true,
                            0,
                            null,
                            null,
                            false
                    );
                    if (onSuccess != null) {
                        onSuccess.run(fileName);
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage() != null ? e.getMessage() : "Error al enviar copia a Mensajes Guardados";
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(msg);
                });
            }
        });
    }

    // -------------------------------------------------------------------------
    // Telegram CloudStorage via @KronoGramCloudBot (MTProto invokeWebViewCustomMethod)
    // -------------------------------------------------------------------------

    public static void fetchCloudMetadata(int account, Utilities.Callback<BackupMetadata> onSuccess, Utilities.Callback<String> onError) {
        resolveBackupBot(account, botUser -> {
            try {
                JSONObject params = new JSONObject();
                JSONArray keys = new JSONArray();
                keys.put(CLOUD_META_KEY);
                params.put("keys", keys);
                invokeCloudMethod(account, botUser, "getStorageValues", params, resultJson -> {
                    try {
                        JSONObject map = new JSONObject(resultJson);
                        String metaRaw = map.optString(CLOUD_META_KEY, "");
                        if (TextUtils.isEmpty(metaRaw)) {
                            AndroidUtilities.runOnUIThread(() -> {
                                if (onSuccess != null) onSuccess.run(null);
                            });
                            return;
                        }
                        BackupMetadata meta = parseCloudMetadataJson(metaRaw);
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onSuccess != null) onSuccess.run(meta);
                        });
                    } catch (Exception e) {
                        FileLog.e(e);
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onError != null) onError.run("Metadatos de copia en la nube inválidos");
                        });
                    }
                }, onError);
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run("Error al consultar la nube");
                });
            }
        }, onError);
    }

    public static void uploadToCloud(int account, BackupOptions options, Utilities.Callback<BackupMetadata> onSuccess, Utilities.Callback<String> onError) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                JSONObject backupJson = buildBackupJson(account, options);
                String rawJson = backupJson.toString();
                String sha256 = sha256Hex(rawJson);
                JSONObject sectionsObj = backupJson.optJSONObject("sections");
                final String sectionsHash = sha256Hex(sectionsObj != null ? sectionsObj.toString() : rawJson);
                String compressed = compressToBase64(rawJson);
                List<String> chunks = splitIntoChunks(compressed, CLOUD_CHUNK_SIZE);
                if (chunks.size() > CLOUD_MAX_CHUNKS) {
                    throw new IllegalStateException("La copia supera el límite máximo permitido por Telegram CloudStorage");
                }

                BackupMetadata meta = new BackupMetadata();
                meta.schemaVersion = BACKUP_SCHEMA_VERSION;
                meta.timestamp = backupJson.optLong("timestamp", System.currentTimeMillis());
                meta.deviceModel = backupJson.optString("device", getDeviceModelLabel());
                meta.chunkCount = chunks.size();
                meta.rawByteSize = rawJson.getBytes(StandardCharsets.UTF_8).length;
                meta.sha256 = sha256;
                meta.hasKrono = options.includeKrono;
                meta.hasTelegramMain = options.includeTelegramMain;
                meta.hasTelegramTheme = options.includeTelegramTheme;
                meta.hasTelegramNotifications = options.includeTelegramNotifications;
                meta.hasHiddenVault = options.includeHiddenVault;

                String metaJsonStr = serializeCloudMetadataJson(meta);

                resolveBackupBot(account, botUser -> {
                    // Save chunks sequentially, then commit metadata atomically last, then clean up stale chunks
                    saveChunksSequentially(account, botUser, chunks, 0, () -> {
                        try {
                            JSONObject metaParams = new JSONObject();
                            metaParams.put("key", CLOUD_META_KEY);
                            metaParams.put("value", metaJsonStr);
                            invokeCloudMethod(account, botUser, "saveStorageValue", metaParams, ignored -> {
                                KronoConfig.recordCloudBackupCompleted(meta.timestamp, sectionsHash);
                                cleanupStaleCloudChunks(account, botUser, chunks.size());
                                AndroidUtilities.runOnUIThread(() -> {
                                    if (onSuccess != null) onSuccess.run(meta);
                                });
                            }, onError);
                        } catch (Exception e) {
                            FileLog.e(e);
                            AndroidUtilities.runOnUIThread(() -> {
                                if (onError != null) onError.run("Error al guardar metadatos en la nube");
                            });
                        }
                    }, onError);
                }, onError);
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage() != null ? e.getMessage() : "Error al preparar copia en la nube";
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(msg);
                });
            }
        });
    }

    public static void downloadFromCloud(int account, Utilities.Callback<ParsedBackup> onSuccess, Utilities.Callback<String> onError) {
        resolveBackupBot(account, botUser -> fetchCloudMetadata(account, meta -> {
            if (meta == null || meta.chunkCount <= 0) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run("No existe ninguna copia de seguridad guardada en la nube");
                });
                return;
            }
            if (meta.chunkCount > CLOUD_MAX_CHUNKS) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run("La copia en la nube está dañada (demasiados bloques)");
                });
                return;
            }
            Map<String, String> collected = new HashMap<>();
            downloadChunksInBatches(account, botUser, meta.chunkCount, 0, collected, () -> {
                Utilities.globalQueue.postRunnable(() -> {
                    try {
                        StringBuilder b64 = new StringBuilder();
                        for (int i = 0; i < meta.chunkCount; i++) {
                            String part = collected.get(CLOUD_CHUNK_PREFIX + i);
                            if (TextUtils.isEmpty(part)) {
                                throw new IllegalStateException("Falta el bloque " + (i + 1) + " de la copia en la nube");
                            }
                            b64.append(part);
                        }
                        String rawJson = decompressFromBase64(b64.toString());
                        if (!TextUtils.isEmpty(meta.sha256)) {
                            String actualHash = sha256Hex(rawJson);
                            if (!meta.sha256.equalsIgnoreCase(actualHash)) {
                                throw new IllegalStateException("Error de integridad (SHA-256 no coincide)");
                            }
                        }
                        ParsedBackup parsed = parseBackupJson(rawJson);
                        parsed.metadata.chunkCount = meta.chunkCount;
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onSuccess != null) onSuccess.run(parsed);
                        });
                    } catch (Exception e) {
                        FileLog.e(e);
                        String msg = e.getMessage() != null ? e.getMessage() : "Error al decodificar copia de la nube";
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onError != null) onError.run(msg);
                        });
                    }
                });
            }, onError);
        }, onError), onError);
    }

    public static void deleteCloudBackup(int account, Runnable onSuccess, Utilities.Callback<String> onError) {
        resolveBackupBot(account, botUser -> {
            invokeCloudMethod(account, botUser, "getStorageKeys", new JSONObject(), resultJson -> {
                try {
                    List<String> keysToDelete = extractKronoBackupKeys(resultJson);
                    if (keysToDelete.isEmpty()) {
                        KronoConfig.recordCloudBackupCompleted(0L, "");
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onSuccess != null) onSuccess.run();
                        });
                        return;
                    }
                    deleteKeysInBatches(account, botUser, keysToDelete, 0, () -> {
                        KronoConfig.recordCloudBackupCompleted(0L, "");
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onSuccess != null) onSuccess.run();
                        });
                    }, onError);
                } catch (Exception e) {
                    FileLog.e(e);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onError != null) onError.run("Error al eliminar la copia de la nube");
                    });
                }
            }, onError);
        }, onError);
    }

    // -------------------------------------------------------------------------
    // Real-Time Change Observer & Periodic Auto-Backup Scheduler
    // -------------------------------------------------------------------------

    public static synchronized void setSuppressObserver(boolean suppress) {
        if (suppress) {
            suppressObserverDepth++;
        } else if (suppressObserverDepth > 0) {
            suppressObserverDepth--;
        }
    }

    public static synchronized void startSettingsObserver(int account) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        KronoConfig.loadConfig();

        if (!observerInitialized) {
            kronoPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (isVaultKronoKey(key)) {
                    if (!KronoConfig.cloudAutoIncludeVault) return;
                } else if (isExcludedKronoKey(key)) {
                    return;
                }
                scheduleOnChangeBackup(observedAccount);
            };
            mainPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeTelegram || isExcludedMainConfigKey(key)) return;
                scheduleOnChangeBackup(observedAccount);
            };
            themePrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeTheme || isExcludedThemeConfigKey(key)) return;
                scheduleOnChangeBackup(observedAccount);
            };
            notifPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeNotifications || isExcludedNotificationKey(key)) return;
                scheduleOnChangeBackup(observedAccount);
            };
            connectionStateDelegate = (id, acc, args) -> {
                if (id == NotificationCenter.didUpdateConnectionState && acc == observedAccount) {
                    int state = ConnectionsManager.getInstance(acc).getConnectionState();
                    if (state == ConnectionsManager.ConnectionStateConnected
                            && KronoConfig.cloudBackupMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE
                            && KronoConfig.cloudBackupPending) {
                        scheduleOnChangeBackup(acc);
                    }
                }
            };

            context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(kronoPrefListener);
            context.getSharedPreferences("mainconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(mainPrefListener);
            context.getSharedPreferences("themeconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(themePrefListener);
            MessagesController.getGlobalNotificationsSettings().registerOnSharedPreferenceChangeListener(notifPrefListener);

            observedAccount = account;
            attachAccountSpecificObserversLocked(account);
            observerInitialized = true;
        } else if (observedAccount != account) {
            detachAccountSpecificObserversLocked(observedAccount);
            observedAccount = account;
            attachAccountSpecificObserversLocked(account);
        }
    }

    private static void attachAccountSpecificObserversLocked(int account) {
        try {
            if (account != 0) {
                observedAccountNotifPrefs = MessagesController.getNotificationsSettings(account);
                if (observedAccountNotifPrefs != null && notifPrefListener != null) {
                    observedAccountNotifPrefs.registerOnSharedPreferenceChangeListener(notifPrefListener);
                }
            }
            if (connectionStateDelegate != null) {
                AndroidUtilities.runOnUIThread(() ->
                        NotificationCenter.getInstance(account).addObserver(connectionStateDelegate, NotificationCenter.didUpdateConnectionState)
                );
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void detachAccountSpecificObserversLocked(int oldAccount) {
        try {
            if (observedAccountNotifPrefs != null && notifPrefListener != null) {
                observedAccountNotifPrefs.unregisterOnSharedPreferenceChangeListener(notifPrefListener);
                observedAccountNotifPrefs = null;
            }
            if (connectionStateDelegate != null) {
                AndroidUtilities.runOnUIThread(() ->
                        NotificationCenter.getInstance(oldAccount).removeObserver(connectionStateDelegate, NotificationCenter.didUpdateConnectionState)
                );
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void scheduleOnChangeBackup(int account) {
        if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
            return;
        }
        KronoConfig.setCloudBackupPending(true);
        if (autoBackupInProgress.get()) {
            dirtyDuringUpload.set(true);
            return;
        }
        long elapsedSinceLast = lastAutoUploadElapsedMs > 0L
                ? (SystemClock.elapsedRealtime() - lastAutoUploadElapsedMs)
                : ON_CHANGE_MIN_COOLDOWN_MS;
        long remainingCooldown = Math.max(0L, ON_CHANGE_MIN_COOLDOWN_MS - elapsedSinceLast);
        long delayMs = Math.max(ON_CHANGE_DEBOUNCE_MS, remainingCooldown);
        AndroidUtilities.cancelRunOnUIThread(debouncedOnChangeRunnable);
        AndroidUtilities.runOnUIThread(debouncedOnChangeRunnable, delayMs);
    }

    private static void executeOnChangeBackup(int account) {
        KronoConfig.loadConfig();
        if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
            return;
        }
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return;
        }
        int connState = ConnectionsManager.getInstance(account).getConnectionState();
        if (connState != ConnectionsManager.ConnectionStateConnected) {
            KronoConfig.setCloudBackupPending(true);
            return;
        }
        if (!autoBackupInProgress.compareAndSet(false, true)) {
            dirtyDuringUpload.set(true);
            return;
        }
        dirtyDuringUpload.set(false);

        Utilities.globalQueue.postRunnable(() -> {
            try {
                BackupOptions options = BackupOptions.fromAutoConfig();
                if (!options.hasAnySelected()) {
                    KronoConfig.setCloudBackupPending(false);
                    autoBackupInProgress.set(false);
                    return;
                }
                JSONObject json = buildBackupJson(account, options);
                JSONObject sections = json.optJSONObject("sections");
                String contentHash = sha256Hex(sections != null ? sections.toString() : json.toString());
                if (!TextUtils.isEmpty(KronoConfig.cloudLastBackupHash) && contentHash.equals(KronoConfig.cloudLastBackupHash)) {
                    KronoConfig.setCloudBackupPending(false);
                    autoBackupInProgress.set(false);
                    return;
                }
                uploadToCloud(account, options, meta -> {
                    lastAutoUploadElapsedMs = SystemClock.elapsedRealtime();
                    KronoConfig.recordCloudBackupCompleted(meta.timestamp, contentHash);
                    autoBackupInProgress.set(false);
                    if (dirtyDuringUpload.getAndSet(false)) {
                        scheduleOnChangeBackup(account);
                    }
                }, err -> {
                    FileLog.e("KronoBackupManager on-change auto-backup failed: " + err);
                    KronoConfig.setCloudBackupPending(true);
                    autoBackupInProgress.set(false);
                });
            } catch (Exception e) {
                FileLog.e(e);
                KronoConfig.setCloudBackupPending(true);
                autoBackupInProgress.set(false);
            }
        });
    }

    public static void maybeRunAutoBackup(int account) {
        KronoConfig.loadConfig();
        startSettingsObserver(account);
        int mode = KronoConfig.cloudBackupMode;
        if (mode == KronoConfig.CLOUD_BACKUP_MANUAL) {
            return;
        }
        if (mode == KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
            if (KronoConfig.cloudBackupPending || KronoConfig.cloudLastBackupTime == 0L) {
                executeOnChangeBackup(account);
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAutoCheckElapsedMs < AUTO_CHECK_THROTTLE_MS) {
            return;
        }
        lastAutoCheckElapsedMs = now;

        long interval = mode == KronoConfig.CLOUD_BACKUP_WEEKLY ? WEEKLY_INTERVAL_MS : DAILY_INTERVAL_MS;
        if (KronoConfig.cloudLastBackupTime > 0 && (now - KronoConfig.cloudLastBackupTime) < interval) {
            return;
        }
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return;
        }
        if (!autoBackupInProgress.compareAndSet(false, true)) {
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            try {
                BackupOptions options = BackupOptions.fromAutoConfig();
                if (!options.hasAnySelected()) {
                    autoBackupInProgress.set(false);
                    return;
                }
                JSONObject json = buildBackupJson(account, options);
                JSONObject sections = json.optJSONObject("sections");
                String contentHash = sha256Hex(sections != null ? sections.toString() : json.toString());
                if (!TextUtils.isEmpty(KronoConfig.cloudLastBackupHash) && contentHash.equals(KronoConfig.cloudLastBackupHash)) {
                    KronoConfig.recordCloudBackupCompleted(now, contentHash);
                    autoBackupInProgress.set(false);
                    return;
                }
                uploadToCloud(account, options, meta -> {
                    lastAutoUploadElapsedMs = SystemClock.elapsedRealtime();
                    KronoConfig.recordCloudBackupCompleted(meta.timestamp, contentHash);
                    autoBackupInProgress.set(false);
                }, err -> {
                    FileLog.e("KronoBackupManager auto-backup skipped: " + err);
                    autoBackupInProgress.set(false);
                });
            } catch (Exception e) {
                FileLog.e(e);
                autoBackupInProgress.set(false);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Internal CloudStorage Helpers
    // -------------------------------------------------------------------------

    private static void resolveBackupBot(int account, Utilities.Callback<TLRPC.User> onSuccess, Utilities.Callback<String> onError) {
        if (!UserConfig.getInstance(account).isClientActivated()) {
            AndroidUtilities.runOnUIThread(() -> {
                if (onError != null) onError.run("Debes iniciar sesión en Telegram para usar la copia en la nube");
            });
            return;
        }
        synchronized (cachedBotByAccount) {
            TLRPC.User cached = cachedBotByAccount.get(account);
            if (cached != null && cached.bot && cached.id != 0) {
                onSuccess.run(cached);
                return;
            }
        }
        TLRPC.User existing = MessagesController.getInstance(account).getUser(CLOUD_BACKUP_BOT_USERNAME);
        if (existing != null && existing.bot && existing.id != 0 && existing.access_hash != 0) {
            synchronized (cachedBotByAccount) {
                cachedBotByAccount.put(account, existing);
            }
            onSuccess.run(existing);
            return;
        }
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = CLOUD_BACKUP_BOT_USERNAME;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(formatTelegramError(error.text));
                });
                return;
            }
            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) response;
                if (peer.users != null && !peer.users.isEmpty()) {
                    MessagesController.getInstance(account).putUsers(peer.users, false);
                    TLRPC.User user = peer.users.get(0);
                    if (user != null && user.bot) {
                        synchronized (cachedBotByAccount) {
                            cachedBotByAccount.put(account, user);
                        }
                        onSuccess.run(user);
                        return;
                    }
                }
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (onError != null) onError.run("No se encontró el bot @" + CLOUD_BACKUP_BOT_USERNAME);
            });
        });
    }

    private static void invokeCloudMethod(int account, TLRPC.User botUser, String method, JSONObject params, Utilities.Callback<String> onSuccess, Utilities.Callback<String> onError) {
        TL_bots.invokeWebViewCustomMethod req = new TL_bots.invokeWebViewCustomMethod();
        TLRPC.InputUser inputUser = MessagesController.getInstance(account).getInputUser(botUser.id);
        if (inputUser == null || inputUser instanceof TLRPC.TL_inputUserEmpty) {
            TLRPC.TL_inputUser direct = new TLRPC.TL_inputUser();
            direct.user_id = botUser.id;
            direct.access_hash = botUser.access_hash;
            inputUser = direct;
        }
        req.bot = inputUser;
        req.custom_method = method;
        req.params = new TLRPC.TL_dataJSON();
        req.params.data = params != null ? params.toString() : "{}";
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error != null) {
                if ("BOT_INVALID".equalsIgnoreCase(error.text) || "USER_ID_INVALID".equalsIgnoreCase(error.text)) {
                    synchronized (cachedBotByAccount) {
                        cachedBotByAccount.remove(account);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(formatTelegramError(error.text));
                });
                return;
            }
            if (response instanceof TLRPC.TL_dataJSON) {
                String data = ((TLRPC.TL_dataJSON) response).data;
                if (onSuccess != null) {
                    onSuccess.run(data != null ? data : "{}");
                }
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run("Respuesta inesperada del servidor de Telegram");
                });
            }
        });
    }

    private static void saveChunksSequentially(int account, TLRPC.User botUser, List<String> chunks, int index, Runnable onDone, Utilities.Callback<String> onError) {
        if (index >= chunks.size()) {
            onDone.run();
            return;
        }
        try {
            JSONObject params = new JSONObject();
            params.put("key", CLOUD_CHUNK_PREFIX + index);
            params.put("value", chunks.get(index));
            invokeCloudMethod(account, botUser, "saveStorageValue", params, ignored ->
                    saveChunksSequentially(account, botUser, chunks, index + 1, onDone, onError), onError);
        } catch (Exception e) {
            FileLog.e(e);
            AndroidUtilities.runOnUIThread(() -> {
                if (onError != null) onError.run("Error al subir bloque " + (index + 1));
            });
        }
    }

    private static void cleanupStaleCloudChunks(int account, TLRPC.User botUser, int currentChunkCount) {
        invokeCloudMethod(account, botUser, "getStorageKeys", new JSONObject(), resultJson -> {
            try {
                List<String> allKeys = extractKronoBackupKeys(resultJson);
                List<String> stale = new ArrayList<>();
                for (String k : allKeys) {
                    if (k.startsWith(CLOUD_CHUNK_PREFIX)) {
                        try {
                            int idx = Integer.parseInt(k.substring(CLOUD_CHUNK_PREFIX.length()));
                            if (idx >= currentChunkCount) {
                                stale.add(k);
                            }
                        } catch (Exception ignore) {}
                    }
                }
                if (!stale.isEmpty()) {
                    deleteKeysInBatches(account, botUser, stale, 0, () -> {}, err -> {});
                }
            } catch (Exception ignore) {}
        }, err -> {});
    }

    private static void downloadChunksInBatches(int account, TLRPC.User botUser, int totalChunks, int startIndex, Map<String, String> collected, Runnable onDone, Utilities.Callback<String> onError) {
        if (startIndex >= totalChunks) {
            onDone.run();
            return;
        }
        int endIndex = Math.min(totalChunks, startIndex + CLOUD_GET_BATCH_SIZE);
        try {
            JSONObject params = new JSONObject();
            JSONArray keys = new JSONArray();
            for (int i = startIndex; i < endIndex; i++) {
                keys.put(CLOUD_CHUNK_PREFIX + i);
            }
            params.put("keys", keys);
            invokeCloudMethod(account, botUser, "getStorageValues", params, resultJson -> {
                try {
                    JSONObject map = new JSONObject(resultJson);
                    for (int i = startIndex; i < endIndex; i++) {
                        String key = CLOUD_CHUNK_PREFIX + i;
                        collected.put(key, map.optString(key, ""));
                    }
                    downloadChunksInBatches(account, botUser, totalChunks, endIndex, collected, onDone, onError);
                } catch (Exception e) {
                    FileLog.e(e);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onError != null) onError.run("Error al leer bloques de la nube");
                    });
                }
            }, onError);
        } catch (Exception e) {
            FileLog.e(e);
            AndroidUtilities.runOnUIThread(() -> {
                if (onError != null) onError.run("Error al solicitar bloques a Telegram");
            });
        }
    }

    private static void deleteKeysInBatches(int account, TLRPC.User botUser, List<String> keys, int startIndex, Runnable onDone, Utilities.Callback<String> onError) {
        if (startIndex >= keys.size()) {
            onDone.run();
            return;
        }
        int endIndex = Math.min(keys.size(), startIndex + CLOUD_GET_BATCH_SIZE);
        try {
            JSONObject params = new JSONObject();
            JSONArray arr = new JSONArray();
            for (int i = startIndex; i < endIndex; i++) {
                arr.put(keys.get(i));
            }
            params.put("keys", arr);
            invokeCloudMethod(account, botUser, "deleteStorageValues", params, ignored ->
                    deleteKeysInBatches(account, botUser, keys, endIndex, onDone, onError), onError);
        } catch (Exception e) {
            FileLog.e(e);
            AndroidUtilities.runOnUIThread(() -> {
                if (onError != null) onError.run("Error al eliminar claves de la nube");
            });
        }
    }

    private static List<String> extractKronoBackupKeys(String resultJson) throws Exception {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(resultJson)) {
            return out;
        }
        Object parsed = new JSONTokener(resultJson).nextValue();
        JSONArray arr = null;
        if (parsed instanceof JSONArray) {
            arr = (JSONArray) parsed;
        } else if (parsed instanceof JSONObject) {
            arr = ((JSONObject) parsed).optJSONArray("keys");
        }
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String k = arr.optString(i, "");
                if (k.startsWith(CLOUD_KEY_PREFIX)) {
                    out.add(k);
                }
            }
        }
        return out;
    }

    private static String serializeCloudMetadataJson(BackupMetadata meta) throws Exception {
        JSONObject o = new JSONObject();
        o.put("v", meta.schemaVersion);
        o.put("ts", meta.timestamp);
        o.put("dev", meta.deviceModel);
        o.put("chunks", meta.chunkCount);
        o.put("size", meta.rawByteSize);
        o.put("sha256", meta.sha256);
        o.put("krono", meta.hasKrono);
        o.put("tg_main", meta.hasTelegramMain);
        o.put("tg_theme", meta.hasTelegramTheme);
        o.put("tg_notif", meta.hasTelegramNotifications);
        o.put("vault", meta.hasHiddenVault);
        return o.toString();
    }

    private static BackupMetadata parseCloudMetadataJson(String raw) throws Exception {
        JSONObject o = new JSONObject(raw);
        BackupMetadata meta = new BackupMetadata();
        meta.schemaVersion = o.optInt("v", BACKUP_SCHEMA_VERSION);
        meta.timestamp = o.optLong("ts", 0L);
        meta.deviceModel = o.optString("dev", "");
        meta.chunkCount = o.optInt("chunks", 0);
        meta.rawByteSize = o.optInt("size", 0);
        meta.sha256 = o.optString("sha256", "");
        meta.hasKrono = o.optBoolean("krono", false);
        meta.hasTelegramMain = o.optBoolean("tg_main", false);
        meta.hasTelegramTheme = o.optBoolean("tg_theme", false);
        meta.hasTelegramNotifications = o.optBoolean("tg_notif", false);
        meta.hasHiddenVault = o.optBoolean("vault", false);
        return meta;
    }

    private static String formatTelegramError(String errorText) {
        if (TextUtils.isEmpty(errorText)) {
            return "Error de conexión con Telegram";
        }
        if (errorText.contains("USERNAME_NOT_OCCUPIED") || errorText.contains("USERNAME_INVALID")) {
            return "No se encontró el bot @" + CLOUD_BACKUP_BOT_USERNAME + " en Telegram";
        }
        if (errorText.contains("BOT_INVALID")) {
            return "El bot @" + CLOUD_BACKUP_BOT_USERNAME + " no admite CloudStorage en este momento";
        }
        if (errorText.startsWith("FLOOD_WAIT")) {
            return "Demasiadas peticiones seguidas. Espera un momento e inténtalo de nuevo.";
        }
        return "Error de Telegram: " + errorText;
    }

    // -------------------------------------------------------------------------
    // Compression, Hashing & Formatting Utilities
    // -------------------------------------------------------------------------

    private static String compressToBase64(String input) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(input.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private static String decompressFromBase64(String base64) throws Exception {
        byte[] compressed = Base64.decode(base64, Base64.DEFAULT);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buf = new byte[4096];
            int total = 0;
            int read;
            while ((read = gzip.read(buf)) != -1) {
                total += read;
                if (total > MAX_BACKUP_FILE_BYTES) {
                    throw new IllegalStateException("La copia descomprimida excede el tamaño máximo permitido");
                }
                out.write(buf, 0, read);
            }
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> list = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += chunkSize) {
            list.add(text.substring(i, Math.min(len, i + chunkSize)));
        }
        return list;
    }

    private static String readStreamUtf8Bounded(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;
        int read;
        while ((read = is.read(buf)) != -1) {
            total += read;
            if (total > MAX_BACKUP_FILE_BYTES) {
                throw new IllegalStateException("El archivo supera el límite de 5 MB");
            }
            baos.write(buf, 0, read);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format(Locale.US, "%02x", b & 0xFF));
        }
        return sb.toString();
    }

    public static String getDeviceModelLabel() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.trim() : "";
        String model = Build.MODEL != null ? Build.MODEL.trim() : "";
        if (TextUtils.isEmpty(manufacturer)) {
            return TextUtils.isEmpty(model) ? "Android" : model;
        }
        if (model.toLowerCase(Locale.US).startsWith(manufacturer.toLowerCase(Locale.US))) {
            return model;
        }
        return manufacturer + " " + model;
    }

    public static String formatTimestamp(long timestampMs) {
        if (timestampMs <= 0L) {
            return "Nunca";
        }
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(timestampMs));
    }
}
