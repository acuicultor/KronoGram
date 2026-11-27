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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class KronoBackupManager {

    public static final String CLOUD_BACKUP_BOT_USERNAME = "KronoGramCloudBot";
    public static final String BACKUP_FORMAT_ID = "kronogram_backup";
    public static final int BACKUP_SCHEMA_VERSION = 3;
    public static final String BACKUP_FILE_EXTENSION = ".kronogram";

    private static final String CLOUD_KEY_PREFIX = "kg_bkp_v1_";
    private static final String CLOUD_META_KEY = CLOUD_KEY_PREFIX + "meta";
    private static final String CLOUD_CHUNK_PREFIX = CLOUD_KEY_PREFIX + "c";
    private static final int CLOUD_CHUNK_SIZE = 3500;
    private static final int CLOUD_MAX_CHUNK_UTF8_BYTES = 4000;
    private static final int CLOUD_MAX_CHUNKS = 512;
    private static final int CLOUD_GET_BATCH_SIZE = 20;
    private static final int CLOUD_RETRY_MAX_ATTEMPTS = 3;
    private static final int MAX_BACKUP_FILE_BYTES = 8 * 1024 * 1024;

    private static final long DAILY_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final long WEEKLY_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final long AUTO_CHECK_THROTTLE_MS = 60L * 1000L;
    private static final long ON_CHANGE_DEBOUNCE_MS = 20L * 1000L;
    private static final long ON_CHANGE_MIN_COOLDOWN_MS = 2L * 60L * 1000L;

    private static final Pattern FLOOD_WAIT_PATTERN = Pattern.compile("FLOOD_WAIT_(\\d+)");

    private static final AtomicBoolean autoBackupInProgress = new AtomicBoolean(false);
    private static final AtomicBoolean dirtyDuringUpload = new AtomicBoolean(false);
    private static volatile int suppressObserverDepth = 0;
    private static final Map<Integer, Long> lastAutoCheckElapsedByAccount = new HashMap<>();
    private static final Map<Integer, Long> lastAutoUploadElapsedByAccount = new HashMap<>();
    private static final Map<Integer, TLRPC.User> cachedBotByAccount = new HashMap<>();
    private static final Set<Integer> remoteCheckedAccounts = new HashSet<>();
    private static final Set<Integer> remoteCheckInProgressAccounts = new HashSet<>();

    // Strong references so Android SharedPreferences WeakHashMap never drops listeners
    private static volatile boolean observerInitialized = false;
    private static volatile int observedAccount = 0;
    private static SharedPreferences observedAccountMainPrefs = null;
    private static SharedPreferences observedAccountUserPrefs = null;
    private static SharedPreferences observedAccountNotifPrefs = null;
    private static SharedPreferences.OnSharedPreferenceChangeListener kronoPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener mainPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener userConfPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener themePrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener notifPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener auxPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener systemPrefListener;
    private static SharedPreferences.OnSharedPreferenceChangeListener articlesPrefListener;
    private static NotificationCenter.NotificationCenterDelegate connectionStateDelegate;
    private static final Runnable debouncedOnChangeRunnable = () -> executeOnChangeBackup(observedAccount);

    public static class BackupOptions {
        public boolean includeKrono = true;
        public boolean includeTelegramMain = true;
        public boolean includeTelegramTheme = true;
        public boolean includeTelegramNotifications = true;
        public boolean includeHiddenVault = true;

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

    /**
     * Tracks original snapshots of every SharedPreferences touched during applyBackup so
     * that any write failure or exception triggers an automatic atomic rollback.
     */
    private static final class RollbackSession {
        private static final class PrefSnapshot {
            final SharedPreferences prefs;
            final Map<String, ?> originalValues;

            PrefSnapshot(SharedPreferences prefs, Map<String, ?> originalValues) {
                this.prefs = prefs;
                this.originalValues = originalValues;
            }
        }

        private final Map<SharedPreferences, PrefSnapshot> snapshots = new LinkedHashMap<>();

        void captureIfNeeded(SharedPreferences prefs) {
            if (prefs == null || snapshots.containsKey(prefs)) {
                return;
            }
            Map<String, ?> current = prefs.getAll();
            Map<String, Object> copy = new HashMap<>();
            if (current != null) {
                for (Map.Entry<String, ?> entry : current.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof Set<?>) {
                        Set<String> clonedSet = new HashSet<>();
                        for (Object item : (Set<?>) val) {
                            if (item instanceof String) {
                                clonedSet.add((String) item);
                            }
                        }
                        copy.put(entry.getKey(), clonedSet);
                    } else if (val != null) {
                        copy.put(entry.getKey(), val);
                    }
                }
            }
            snapshots.put(prefs, new PrefSnapshot(prefs, copy));
        }

        void commitOrThrow(SharedPreferences prefs, SharedPreferences.Editor editor, String label) throws IOException {
            if (prefs == null || editor == null) {
                return;
            }
            if (!editor.commit()) {
                throw new IOException("No se pudieron guardar los ajustes en disco (" + label + ")");
            }
        }

        @SuppressWarnings("unchecked")
        void rollbackAll() {
            for (PrefSnapshot snap : snapshots.values()) {
                try {
                    SharedPreferences.Editor editor = snap.prefs.edit();
                    editor.clear();
                    for (Map.Entry<String, ?> entry : snap.originalValues.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        if (TextUtils.isEmpty(key) || val == null) {
                            continue;
                        }
                        if (val instanceof Boolean) {
                            editor.putBoolean(key, (Boolean) val);
                        } else if (val instanceof Integer) {
                            editor.putInt(key, (Integer) val);
                        } else if (val instanceof Long) {
                            editor.putLong(key, (Long) val);
                        } else if (val instanceof Float) {
                            editor.putFloat(key, (Float) val);
                        } else if (val instanceof String) {
                            editor.putString(key, (String) val);
                        } else if (val instanceof Set<?>) {
                            editor.putStringSet(key, (Set<String>) val);
                        }
                    }
                    editor.commit();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // JSON Serialization & Deserialization (Typed SharedPreferences)
    // -------------------------------------------------------------------------

    private static SharedPreferences getUserConfigPrefs(Context context, int account) {
        if (context == null) return null;
        return context.getSharedPreferences(account == 0 ? "userconfing" : ("userconfig" + account), Context.MODE_PRIVATE);
    }

    private static SharedPreferences getMainConfigPrefs(Context context, int account) {
        if (context == null) return null;
        return context.getSharedPreferences(account == 0 ? "mainconfig" : ("mainconfig" + account), Context.MODE_PRIVATE);
    }

    private static SharedPreferences getNotificationsPrefs(Context context, int account) {
        if (context == null) return null;
        return context.getSharedPreferences(account == 0 ? "Notifications" : ("Notifications" + account), Context.MODE_PRIVATE);
    }

    private static SharedPreferences getEmojiPrefs(Context context, int account) {
        if (context == null) return null;
        return context.getSharedPreferences(account == 0 ? "emoji" : ("emoji" + account), Context.MODE_PRIVATE);
    }

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
        root.put("sourceAccount", account);

        JSONObject sections = new JSONObject();

        if (options.includeKrono) {
            SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
            sections.put("krono", serializePreferences(kronoPrefs,
                    key -> isExcludedKronoKey(key) || key.startsWith("filter_icon_") || key.startsWith("customFilterOrder_"), false));
            JSONObject folderIconsObj = serializeAccountFolderIcons(kronoPrefs, account);
            if (folderIconsObj.length() > 0) {
                sections.put("krono_folder_icons", folderIconsObj);
            }
            String folderOrder = kronoPrefs.getString("customFilterOrder_" + account, "");
            if (!TextUtils.isEmpty(folderOrder)) {
                sections.put("krono_folder_order", folderOrder);
            }
            JSONObject kronoMultiObj = new JSONObject();
            for (int acc = 0; acc < UserConfig.MAX_ACCOUNT_COUNT; acc++) {
                if (acc == account) continue;
                JSONObject accIcons = serializeAccountFolderIcons(kronoPrefs, acc);
                String accOrder = kronoPrefs.getString("customFilterOrder_" + acc, "");
                if (accIcons.length() > 0 || !TextUtils.isEmpty(accOrder)) {
                    JSONObject entry = new JSONObject();
                    if (accIcons.length() > 0) entry.put("folder_icons", accIcons);
                    if (!TextUtils.isEmpty(accOrder)) entry.put("folder_order", accOrder);
                    kronoMultiObj.put(String.valueOf(acc), entry);
                }
            }
            if (kronoMultiObj.length() > 0) {
                sections.put("krono_multi", kronoMultiObj);
            }
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
            JSONObject vaultMulti = new JSONObject();
            for (int acc = 0; acc < UserConfig.MAX_ACCOUNT_COUNT; acc++) {
                if (acc == account) continue;
                String d = kronoPrefs.getString("hiddenDialogs_" + acc, "");
                String f = kronoPrefs.getString("hiddenFilters_" + acc, "");
                if (!TextUtils.isEmpty(d) || !TextUtils.isEmpty(f)) {
                    JSONObject accVault = new JSONObject();
                    putTypedEntry(accVault, "hiddenDialogs", d);
                    putTypedEntry(accVault, "hiddenFilters", f);
                    vaultMulti.put(String.valueOf(acc), accVault);
                }
            }
            if (vaultMulti.length() > 0) {
                vaultObj.put("multi", vaultMulti);
            }
            sections.put("vault", vaultObj);
        }

        if (options.includeTelegramMain) {
            SharedPreferences mainPrefs = getMainConfigPrefs(context, 0);
            sections.put("tg_main", serializePreferences(mainPrefs,
                    key -> isExcludedMainConfigKey(key) || isThemeMainConfigKey(key), false));
            if (account != 0) {
                SharedPreferences accMainPrefs = getMainConfigPrefs(context, account);
                JSONObject accMainObj = serializePreferences(accMainPrefs,
                        key -> isExcludedMainConfigKey(key) || isThemeMainConfigKey(key), false);
                if (accMainObj.length() > 0) {
                    sections.put("tg_main_account", accMainObj);
                }
                SharedPreferences globalUserConfPrefs = getUserConfigPrefs(context, 0);
                JSONObject globalUserConfObj = serializePreferences(globalUserConfPrefs,
                        key -> !isSafeUserConfigKey(key), false);
                if (globalUserConfObj.length() > 0) {
                    sections.put("tg_userconfig_global", globalUserConfObj);
                }
            }

            SharedPreferences userConfPrefs = getUserConfigPrefs(context, account);
            JSONObject userConfObj = serializePreferences(userConfPrefs,
                    key -> !isSafeUserConfigKey(key), false);
            if (userConfObj.length() > 0) {
                sections.put("tg_userconfig", userConfObj);
            }

            SharedPreferences langPrefs = context.getSharedPreferences("langconfig", Context.MODE_PRIVATE);
            JSONObject langObj = serializePreferences(langPrefs, null, false);
            if (langObj.length() > 0) {
                sections.put("tg_langconfig", langObj);
            }

            SharedPreferences playbackPrefs = context.getSharedPreferences("playback_speed", Context.MODE_PRIVATE);
            JSONObject playbackObj = serializePreferences(playbackPrefs, null, false);
            if (playbackObj.length() > 0) {
                sections.put("tg_playback_speed", playbackObj);
            }

            SharedPreferences emojiPrefs = getEmojiPrefs(context, account);
            JSONObject emojiObj = serializePreferences(emojiPrefs, null, false);
            if (emojiObj.length() > 0) {
                sections.put("tg_emoji", emojiObj);
            }

            JSONObject galleryExceptionsObj = serializeGalleryExceptions(context, account);
            if (galleryExceptionsObj.length() > 0) {
                sections.put("tg_save_gallery_exceptions", galleryExceptionsObj);
            }

            // Additional native preferences previously missed
            JSONObject sysConfObj = serializePreferences(
                    context.getSharedPreferences("systemConfig", Context.MODE_PRIVATE),
                    key -> !isSafeSystemConfigKey(key), false);
            if (sysConfObj.length() > 0) {
                sections.put("tg_system_config", sysConfObj);
            }

            JSONObject articlesObj = serializePreferences(
                    context.getSharedPreferences("articles", Context.MODE_PRIVATE),
                    KronoBackupManager::isExcludedArticleKey, false);
            if (articlesObj.length() > 0) {
                sections.put("tg_articles", articlesObj);
            }

            JSONObject cameraObj = serializePreferences(
                    context.getSharedPreferences("camera", Context.MODE_PRIVATE),
                    null, false);
            if (cameraObj.length() > 0) {
                sections.put("tg_camera", cameraObj);
            }

            JSONObject paintObj = serializePreferences(
                    context.getSharedPreferences("paint", Context.MODE_PRIVATE),
                    null, false);
            if (paintObj.length() > 0) {
                sections.put("tg_paint", paintObj);
            }

            JSONObject shapeObj = serializePreferences(
                    context.getSharedPreferences("shapedetector_conf", Context.MODE_PRIVATE),
                    null, false);
            if (shapeObj.length() > 0) {
                sections.put("tg_shapedetector", shapeObj);
            }

            JSONObject pipBundle = new JSONObject();
            JSONObject pipRound = serializePreferences(context.getSharedPreferences("pipconfig", Context.MODE_PRIVATE), null, false);
            JSONObject pipVoip = serializePreferences(context.getSharedPreferences("voippipconfig", Context.MODE_PRIVATE), null, false);
            JSONObject pipGroup = serializePreferences(context.getSharedPreferences("groupcallpipconfig", Context.MODE_PRIVATE), null, false);
            if (pipRound.length() > 0) pipBundle.put("round", pipRound);
            if (pipVoip.length() > 0) pipBundle.put("voip", pipVoip);
            if (pipGroup.length() > 0) pipBundle.put("groupcall", pipGroup);
            if (pipBundle.length() > 0) {
                sections.put("tg_pip", pipBundle);
            }

            // Additional accounts (for multi-account setups)
            JSONObject multiAccountsObj = new JSONObject();
            for (int acc = 0; acc < UserConfig.MAX_ACCOUNT_COUNT; acc++) {
                if (acc == account) continue;
                boolean active = UserConfig.getInstance(acc).isClientActivated();
                JSONObject accBundle = new JSONObject();
                if (acc != 0) {
                    JSONObject m = serializePreferences(getMainConfigPrefs(context, acc),
                            key -> isExcludedMainConfigKey(key) || isThemeMainConfigKey(key), false);
                    if (m.length() > 0) accBundle.put("main", m);
                }
                JSONObject u = serializePreferences(getUserConfigPrefs(context, acc),
                        key -> !isSafeUserConfigKey(key), false);
                if (u.length() > 0) accBundle.put("userconfig", u);
                JSONObject e = serializePreferences(getEmojiPrefs(context, acc), null, false);
                if (e.length() > 0) accBundle.put("emoji", e);
                JSONObject g = serializeGalleryExceptions(context, acc);
                if (g.length() > 0) accBundle.put("save_gallery_exceptions", g);
                if (active || accBundle.length() > 0) {
                    if (accBundle.length() > 0) {
                        multiAccountsObj.put(String.valueOf(acc), accBundle);
                    }
                }
            }
            if (multiAccountsObj.length() > 0) {
                sections.put("tg_multi_accounts", multiAccountsObj);
            }
        }

        if (options.includeTelegramTheme) {
            SharedPreferences themePrefs = context.getSharedPreferences("themeconfig", Context.MODE_PRIVATE);
            sections.put("tg_theme", serializePreferences(themePrefs, KronoBackupManager::isExcludedThemeConfigKey, false));
            SharedPreferences mainPrefs = getMainConfigPrefs(context, 0);
            JSONObject themeMainObj = serializePreferences(mainPrefs, key -> !isThemeMainConfigKey(key), false);
            if (themeMainObj.length() > 0) {
                sections.put("tg_theme_main", themeMainObj);
            }
        }

        if (options.includeTelegramNotifications) {
            SharedPreferences globalNotifPrefs = getNotificationsPrefs(context, 0);
            JSONObject notifBundle = new JSONObject();
            notifBundle.put("global", serializePreferences(globalNotifPrefs, KronoBackupManager::isExcludedNotificationKey, true));
            if (account != 0) {
                SharedPreferences accountNotifPrefs = getNotificationsPrefs(context, account);
                notifBundle.put("account", serializePreferences(accountNotifPrefs, KronoBackupManager::isExcludedNotificationKey, true));
            }
            JSONObject multiNotifObj = new JSONObject();
            for (int acc = 1; acc < UserConfig.MAX_ACCOUNT_COUNT; acc++) {
                if (acc == account) continue;
                JSONObject accNotif = serializePreferences(getNotificationsPrefs(context, acc),
                        KronoBackupManager::isExcludedNotificationKey, true);
                if (accNotif.length() > 0) {
                    multiNotifObj.put(String.valueOf(acc), accNotif);
                }
            }
            if (multiNotifObj.length() > 0) {
                notifBundle.put("multi", multiNotifObj);
            }
            sections.put("tg_notifications", notifBundle);
        }

        root.put("sections", sections);
        return root;
    }

    private static JSONObject serializeGalleryExceptions(Context context, int account) throws Exception {
        JSONObject galleryExceptionsObj = new JSONObject();
        JSONObject usersEx = serializePreferences(context.getSharedPreferences(SaveToGallerySettingsHelper.USERS_PREF_NAME + "_" + account, Context.MODE_PRIVATE), null, false);
        JSONObject groupsEx = serializePreferences(context.getSharedPreferences(SaveToGallerySettingsHelper.GROUPS_PREF_NAME + "_" + account, Context.MODE_PRIVATE), null, false);
        JSONObject channelsEx = serializePreferences(context.getSharedPreferences(SaveToGallerySettingsHelper.CHANNELS_PREF_NAME + "_" + account, Context.MODE_PRIVATE), null, false);
        if (usersEx.length() > 0) galleryExceptionsObj.put("users", usersEx);
        if (groupsEx.length() > 0) galleryExceptionsObj.put("groups", groupsEx);
        if (channelsEx.length() > 0) galleryExceptionsObj.put("channels", channelsEx);
        return galleryExceptionsObj;
    }

    public static ParsedBackup parseBackupJson(String rawJson) throws Exception {
        if (TextUtils.isEmpty(rawJson)) {
            throw new IllegalArgumentException("El contenido de la copia está vacío");
        }
        String sanitized = rawJson.trim();
        if (sanitized.startsWith("\uFEFF")) {
            sanitized = sanitized.substring(1).trim();
        }
        JSONObject root = new JSONObject(sanitized);
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
        meta.rawByteSize = sanitized.getBytes(StandardCharsets.UTF_8).length;
        meta.sha256 = sha256Hex(sanitized);
        meta.hasKrono = sections.has("krono") && sections.optJSONObject("krono") != null;
        meta.hasHiddenVault = sections.has("vault") && sections.optJSONObject("vault") != null;
        meta.hasTelegramMain = hasAnyTelegramMainSection(sections);
        meta.hasTelegramTheme = (sections.has("tg_theme") && sections.optJSONObject("tg_theme") != null)
                || (sections.has("tg_theme_main") && sections.optJSONObject("tg_theme_main") != null);
        meta.hasTelegramNotifications = sections.has("tg_notifications") && sections.optJSONObject("tg_notifications") != null;

        ParsedBackup parsed = new ParsedBackup();
        parsed.metadata = meta;
        parsed.root = root;
        return parsed;
    }

    private static boolean hasAnyTelegramMainSection(JSONObject sections) {
        String[] keys = new String[]{
                "tg_main", "tg_main_account", "tg_userconfig", "tg_userconfig_global",
                "tg_langconfig", "tg_playback_speed", "tg_emoji", "tg_save_gallery_exceptions",
                "tg_system_config", "tg_articles", "tg_camera", "tg_paint",
                "tg_shapedetector", "tg_pip", "tg_multi_accounts"
        };
        for (String k : keys) {
            if (sections.has(k) && sections.optJSONObject(k) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates every selected section's typed entries before mutating any SharedPreferences.
     */
    private static void validateBackupForApply(ParsedBackup backup, BackupOptions options) throws Exception {
        if (backup == null || backup.root == null) {
            throw new IllegalArgumentException("Copia de seguridad inválida");
        }
        if (options == null || !options.hasAnySelected()) {
            throw new IllegalArgumentException("Debes seleccionar al menos una categoría");
        }
        JSONObject sections = backup.root.optJSONObject("sections");
        if (sections == null || sections.length() == 0) {
            throw new IllegalArgumentException("La copia no contiene datos restaurables");
        }

        if (options.includeKrono && sections.has("krono")) {
            validateTypedMapObject(sections.optJSONObject("krono"), "krono");
        }
        if (options.includeHiddenVault && sections.has("vault")) {
            JSONObject vault = sections.optJSONObject("vault");
            if (vault == null) throw new IllegalArgumentException("Sección 'vault' dañada");
            String[] vaultKeys = new String[]{
                    "hiddenUseBiometric", "hiddenMuteNotifications",
                    "hiddenHideFolderChatsEverywhere", "hiddenAutoLockOnBackground",
                    "hiddenDialogs", "hiddenFilters"
            };
            for (String vk : vaultKeys) {
                if (vault.has(vk)) {
                    validateTypedEntry(vk, vault.optJSONObject(vk));
                }
            }
        }
        if (options.includeTelegramMain) {
            String[] mapSections = new String[]{
                    "tg_main", "tg_main_account", "tg_userconfig", "tg_userconfig_global",
                    "tg_langconfig", "tg_playback_speed", "tg_emoji",
                    "tg_system_config", "tg_articles", "tg_camera", "tg_paint", "tg_shapedetector"
            };
            for (String sec : mapSections) {
                if (sections.has(sec)) {
                    validateTypedMapObject(sections.optJSONObject(sec), sec);
                }
            }
            if (sections.has("tg_save_gallery_exceptions")) {
                JSONObject ge = sections.optJSONObject("tg_save_gallery_exceptions");
                if (ge != null) {
                    if (ge.has("users")) validateTypedMapObject(ge.optJSONObject("users"), "tg_save_gallery_exceptions.users");
                    if (ge.has("groups")) validateTypedMapObject(ge.optJSONObject("groups"), "tg_save_gallery_exceptions.groups");
                    if (ge.has("channels")) validateTypedMapObject(ge.optJSONObject("channels"), "tg_save_gallery_exceptions.channels");
                }
            }
            if (sections.has("tg_pip")) {
                JSONObject pip = sections.optJSONObject("tg_pip");
                if (pip != null) {
                    if (pip.has("round")) validateTypedMapObject(pip.optJSONObject("round"), "tg_pip.round");
                    if (pip.has("voip")) validateTypedMapObject(pip.optJSONObject("voip"), "tg_pip.voip");
                    if (pip.has("groupcall")) validateTypedMapObject(pip.optJSONObject("groupcall"), "tg_pip.groupcall");
                }
            }
        }
        if (options.includeTelegramTheme) {
            if (sections.has("tg_theme")) {
                validateTypedMapObject(sections.optJSONObject("tg_theme"), "tg_theme");
            }
            if (sections.has("tg_theme_main")) {
                validateTypedMapObject(sections.optJSONObject("tg_theme_main"), "tg_theme_main");
            }
        }
        if (options.includeTelegramNotifications && sections.has("tg_notifications")) {
            JSONObject notif = sections.optJSONObject("tg_notifications");
            if (notif == null) throw new IllegalArgumentException("Sección 'tg_notifications' dañada");
            if (notif.has("global")) validateTypedMapObject(notif.optJSONObject("global"), "tg_notifications.global");
            if (notif.has("account")) validateTypedMapObject(notif.optJSONObject("account"), "tg_notifications.account");
        }
    }

    private static void validateTypedMapObject(JSONObject obj, String sectionName) throws Exception {
        if (obj == null) {
            throw new IllegalArgumentException("Sección '" + sectionName + "' inválida");
        }
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (TextUtils.isEmpty(key)) continue;
            JSONObject entry = obj.optJSONObject(key);
            if (entry == null) {
                throw new IllegalArgumentException("Entrada inválida en '" + sectionName + "." + key + "'");
            }
            validateTypedEntry(sectionName + "." + key, entry);
        }
    }

    private static void validateTypedEntry(String keyLabel, JSONObject entry) throws Exception {
        if (entry == null || !entry.has("t") || !entry.has("v")) {
            throw new IllegalArgumentException("Entrada incompleta en '" + keyLabel + "'");
        }
        String type = entry.optString("t", "");
        switch (type) {
            case "b":
                entry.getBoolean("v");
                break;
            case "i":
                entry.getInt("v");
                break;
            case "l":
                entry.getLong("v");
                break;
            case "f":
                entry.getDouble("v");
                break;
            case "s":
                entry.getString("v");
                break;
            case "ss":
                JSONArray arr = entry.getJSONArray("v");
                for (int i = 0; i < arr.length(); i++) {
                    arr.getString(i);
                }
                break;
            default:
                throw new IllegalArgumentException("Tipo desconocido '" + type + "' en '" + keyLabel + "'");
        }
    }

    public static RestoreResult applyBackup(int account, ParsedBackup backup, BackupOptions options) throws Exception {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            throw new IllegalStateException("Contexto de aplicación no disponible");
        }
        validateBackupForApply(backup, options);

        JSONObject sections = backup.root.optJSONObject("sections");
        RestoreResult result = new RestoreResult();
        AndroidUtilities.cancelRunOnUIThread(debouncedOnChangeRunnable);
        int schema = backup.root.optInt("schema", 1);
        boolean restoredTheme = false;
        boolean restoredEmoji = false;
        boolean restoredSystemLogs = false;
        boolean restoredPlaybackSpeed = false;
        final Set<Integer> affectedAccounts = new HashSet<>();
        affectedAccounts.add(account);

        RollbackSession rollback = new RollbackSession();
        setSuppressObserver(true);
        try {
            if (options.includeKrono && sections.has("krono")) {
                SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
                int sourceAccount = backup.root.optInt("sourceAccount", -1);
                JSONObject kronoObj = sections.optJSONObject("krono");
                if (kronoObj != null) {
                    result.appliedKeysCount += deserializeKronoPreferences(kronoPrefs, account, sourceAccount, kronoObj, rollback);
                }
                JSONObject folderIconsObj = sections.optJSONObject("krono_folder_icons");
                if (folderIconsObj != null) {
                    result.appliedKeysCount += deserializeAccountFolderIcons(kronoPrefs, account, folderIconsObj, rollback);
                }
                if (sections.has("krono_folder_order")) {
                    rollback.captureIfNeeded(kronoPrefs);
                    String folderOrder = sections.optString("krono_folder_order", "");
                    SharedPreferences.Editor orderEd = kronoPrefs.edit();
                    if (TextUtils.isEmpty(folderOrder)) {
                        orderEd.remove("customFilterOrder_" + account);
                    } else {
                        orderEd.putString("customFilterOrder_" + account, folderOrder);
                        result.appliedKeysCount++;
                    }
                    rollback.commitOrThrow(kronoPrefs, orderEd, "kronoconfig.folder_order");
                } else if (!hasLegacyFolderOrderKey(kronoObj)) {
                    rollback.captureIfNeeded(kronoPrefs);
                    SharedPreferences.Editor rmEd = kronoPrefs.edit().remove("customFilterOrder_" + account);
                    rollback.commitOrThrow(kronoPrefs, rmEd, "kronoconfig.remove_folder_order");
                }

                JSONObject kronoMulti = sections.optJSONObject("krono_multi");
                if (kronoMulti != null) {
                    Iterator<String> accKeys = kronoMulti.keys();
                    while (accKeys.hasNext()) {
                        String accStr = accKeys.next();
                        int targetAcc;
                        try {
                            targetAcc = Integer.parseInt(accStr);
                        } catch (Exception ignore) {
                            continue;
                        }
                        if (targetAcc < 0 || targetAcc >= UserConfig.MAX_ACCOUNT_COUNT || targetAcc == account) {
                            continue;
                        }
                        JSONObject accEntry = kronoMulti.optJSONObject(accStr);
                        if (accEntry == null) continue;
                        if (accEntry.has("folder_icons")) {
                            result.appliedKeysCount += deserializeAccountFolderIcons(kronoPrefs, targetAcc, accEntry.optJSONObject("folder_icons"), rollback);
                        }
                        if (accEntry.has("folder_order")) {
                            rollback.captureIfNeeded(kronoPrefs);
                            SharedPreferences.Editor orderEd = kronoPrefs.edit();
                            String ord = accEntry.optString("folder_order", "");
                            if (TextUtils.isEmpty(ord)) {
                                orderEd.remove("customFilterOrder_" + targetAcc);
                            } else {
                                orderEd.putString("customFilterOrder_" + targetAcc, ord);
                                result.appliedKeysCount++;
                            }
                            rollback.commitOrThrow(kronoPrefs, orderEd, "kronoconfig.folder_order_" + targetAcc);
                        }
                    }
                }
            }

            if (options.includeHiddenVault && sections.has("vault")) {
                JSONObject vaultObj = sections.optJSONObject("vault");
                if (vaultObj != null) {
                    SharedPreferences kronoPrefs = context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE);
                    rollback.captureIfNeeded(kronoPrefs);
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
                    JSONObject vaultMulti = vaultObj.optJSONObject("multi");
                    if (vaultMulti != null) {
                        Iterator<String> accKeys = vaultMulti.keys();
                        while (accKeys.hasNext()) {
                            String accStr = accKeys.next();
                            int targetAcc;
                            try {
                                targetAcc = Integer.parseInt(accStr);
                            } catch (Exception ignore) {
                                continue;
                            }
                            if (targetAcc < 0 || targetAcc >= UserConfig.MAX_ACCOUNT_COUNT || targetAcc == account) {
                                continue;
                            }
                            JSONObject accVault = vaultMulti.optJSONObject(accStr);
                            if (accVault == null) continue;
                            if (accVault.has("hiddenDialogs")) {
                                count += applyTypedEntry(editor, "hiddenDialogs_" + targetAcc, accVault.optJSONObject("hiddenDialogs"));
                            }
                            if (accVault.has("hiddenFilters")) {
                                count += applyTypedEntry(editor, "hiddenFilters_" + targetAcc, accVault.optJSONObject("hiddenFilters"));
                            }
                        }
                    }
                    rollback.commitOrThrow(kronoPrefs, editor, "kronoconfig.vault");
                    result.appliedKeysCount += count;
                    result.restoredVault = count > 0;
                }
            }

            if (options.includeTelegramMain) {
                if (sections.has("tg_main")) {
                    JSONObject mainObj = sections.optJSONObject("tg_main");
                    if (mainObj != null) {
                        SharedPreferences mainPrefs = getMainConfigPrefs(context, 0);
                        int c = deserializeIntoPreferences(mainPrefs, mainObj,
                                key -> isExcludedMainConfigKey(key) || isThemeMainConfigKey(key), true, rollback, "mainconfig");
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                        if (mainObj.has("playbackSpeed") || mainObj.has("musicPlaybackSpeed")
                                || mainObj.has("fastPlaybackSpeed") || mainObj.has("fastMusicPlaybackSpeed")) {
                            restoredPlaybackSpeed = true;
                        }
                    }
                }
                if (account != 0 && sections.has("tg_main_account")) {
                    JSONObject accMainObj = sections.optJSONObject("tg_main_account");
                    if (accMainObj != null) {
                        SharedPreferences accMainPrefs = getMainConfigPrefs(context, account);
                        int c = deserializeIntoPreferences(accMainPrefs, accMainObj,
                                key -> isExcludedMainConfigKey(key) || isThemeMainConfigKey(key), true, rollback, "mainconfig" + account);
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                    }
                }
                if (account != 0 && sections.has("tg_userconfig_global")) {
                    JSONObject globalUserConfObj = sections.optJSONObject("tg_userconfig_global");
                    if (globalUserConfObj != null) {
                        SharedPreferences globalUserConfPrefs = getUserConfigPrefs(context, 0);
                        int c = deserializeIntoPreferences(globalUserConfPrefs, globalUserConfObj,
                                key -> !isSafeUserConfigKey(key), false, rollback, "userconfing");
                        result.appliedKeysCount += c;
                        affectedAccounts.add(0);
                    }
                }
                if (sections.has("tg_userconfig")) {
                    JSONObject userConfObj = sections.optJSONObject("tg_userconfig");
                    if (userConfObj != null) {
                        SharedPreferences userConfPrefs = getUserConfigPrefs(context, account);
                        // clearNonExcludedExisting = false so internal session/user/offset keys are untouched
                        // while explicitly removing stale safe keys missing from the backup.
                        int c = deserializeSafeUserConfig(userConfPrefs, userConfObj, rollback, account == 0 ? "userconfing" : ("userconfig" + account));
                        result.appliedKeysCount += c;
                    }
                }
                if (sections.has("tg_langconfig")) {
                    JSONObject langObj = sections.optJSONObject("tg_langconfig");
                    if (langObj != null) {
                        SharedPreferences langPrefs = context.getSharedPreferences("langconfig", Context.MODE_PRIVATE);
                        int c = deserializeIntoPreferences(langPrefs, langObj, null, true, rollback, "langconfig");
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                    }
                }
                if (sections.has("tg_playback_speed")) {
                    JSONObject playbackObj = sections.optJSONObject("tg_playback_speed");
                    if (playbackObj != null) {
                        SharedPreferences playbackPrefs = context.getSharedPreferences("playback_speed", Context.MODE_PRIVATE);
                        result.appliedKeysCount += deserializeIntoPreferences(playbackPrefs, playbackObj, null, true, rollback, "playback_speed");
                    }
                }
                if (sections.has("tg_emoji")) {
                    JSONObject emojiObj = sections.optJSONObject("tg_emoji");
                    if (emojiObj != null) {
                        SharedPreferences emojiPrefs = getEmojiPrefs(context, account);
                        int c = deserializeIntoPreferences(emojiPrefs, emojiObj, null, true, rollback, account == 0 ? "emoji" : ("emoji" + account));
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            restoredEmoji = true;
                        }
                    }
                }
                if (sections.has("tg_save_gallery_exceptions")) {
                    result.appliedKeysCount += deserializeGalleryExceptions(
                            context, account, sections.optJSONObject("tg_save_gallery_exceptions"), rollback);
                }
                if (sections.has("tg_system_config")) {
                    JSONObject sysObj = sections.optJSONObject("tg_system_config");
                    if (sysObj != null) {
                        SharedPreferences sysPrefs = context.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
                        int c = deserializeIntoPreferences(sysPrefs, sysObj, key -> !isSafeSystemConfigKey(key), false, rollback, "systemConfig");
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            restoredSystemLogs = true;
                        }
                    }
                }
                if (sections.has("tg_articles")) {
                    JSONObject artObj = sections.optJSONObject("tg_articles");
                    if (artObj != null) {
                        SharedPreferences artPrefs = context.getSharedPreferences("articles", Context.MODE_PRIVATE);
                        result.appliedKeysCount += deserializeIntoPreferences(artPrefs, artObj, KronoBackupManager::isExcludedArticleKey, true, rollback, "articles");
                    }
                }
                if (sections.has("tg_camera")) {
                    JSONObject camObj = sections.optJSONObject("tg_camera");
                    if (camObj != null) {
                        SharedPreferences camPrefs = context.getSharedPreferences("camera", Context.MODE_PRIVATE);
                        result.appliedKeysCount += deserializeIntoPreferences(camPrefs, camObj, null, true, rollback, "camera");
                    }
                }
                if (sections.has("tg_paint")) {
                    JSONObject paintObj = sections.optJSONObject("tg_paint");
                    if (paintObj != null) {
                        SharedPreferences paintPrefs = context.getSharedPreferences("paint", Context.MODE_PRIVATE);
                        result.appliedKeysCount += deserializeIntoPreferences(paintPrefs, paintObj, null, true, rollback, "paint");
                    }
                }
                if (sections.has("tg_shapedetector")) {
                    JSONObject shapeObj = sections.optJSONObject("tg_shapedetector");
                    if (shapeObj != null) {
                        SharedPreferences shapePrefs = context.getSharedPreferences("shapedetector_conf", Context.MODE_PRIVATE);
                        result.appliedKeysCount += deserializeIntoPreferences(shapePrefs, shapeObj, null, true, rollback, "shapedetector_conf");
                    }
                }
                if (sections.has("tg_pip")) {
                    JSONObject pipBundle = sections.optJSONObject("tg_pip");
                    if (pipBundle != null) {
                        if (pipBundle.has("round")) {
                            result.appliedKeysCount += deserializeIntoPreferences(
                                    context.getSharedPreferences("pipconfig", Context.MODE_PRIVATE),
                                    pipBundle.optJSONObject("round"), null, true, rollback, "pipconfig");
                        }
                        if (pipBundle.has("voip")) {
                            result.appliedKeysCount += deserializeIntoPreferences(
                                    context.getSharedPreferences("voippipconfig", Context.MODE_PRIVATE),
                                    pipBundle.optJSONObject("voip"), null, true, rollback, "voippipconfig");
                        }
                        if (pipBundle.has("groupcall")) {
                            result.appliedKeysCount += deserializeIntoPreferences(
                                    context.getSharedPreferences("groupcallpipconfig", Context.MODE_PRIVATE),
                                    pipBundle.optJSONObject("groupcall"), null, true, rollback, "groupcallpipconfig");
                        }
                    }
                }
                if (sections.has("tg_multi_accounts")) {
                    JSONObject multiObj = sections.optJSONObject("tg_multi_accounts");
                    if (multiObj != null) {
                        Iterator<String> accKeys = multiObj.keys();
                        while (accKeys.hasNext()) {
                            String accStr = accKeys.next();
                            int targetAcc;
                            try {
                                targetAcc = Integer.parseInt(accStr);
                            } catch (Exception ignore) {
                                continue;
                            }
                            if (targetAcc < 0 || targetAcc >= UserConfig.MAX_ACCOUNT_COUNT || targetAcc == account) {
                                continue;
                            }
                            JSONObject accBundle = multiObj.optJSONObject(accStr);
                            if (accBundle == null) continue;
                            affectedAccounts.add(targetAcc);
                            if (targetAcc != 0 && accBundle.has("main")) {
                                int c = deserializeIntoPreferences(
                                        getMainConfigPrefs(context, targetAcc),
                                        accBundle.optJSONObject("main"),
                                        key -> isExcludedMainConfigKey(key) || isThemeMainConfigKey(key),
                                        true, rollback, "mainconfig" + targetAcc);
                                result.appliedKeysCount += c;
                                if (c > 0) result.requiresRestart = true;
                            }
                            if (accBundle.has("userconfig")) {
                                result.appliedKeysCount += deserializeSafeUserConfig(
                                        getUserConfigPrefs(context, targetAcc),
                                        accBundle.optJSONObject("userconfig"),
                                        rollback, targetAcc == 0 ? "userconfing" : ("userconfig" + targetAcc));
                            }
                            if (accBundle.has("emoji")) {
                                int c = deserializeIntoPreferences(
                                        getEmojiPrefs(context, targetAcc),
                                        accBundle.optJSONObject("emoji"),
                                        null, true, rollback, targetAcc == 0 ? "emoji" : ("emoji" + targetAcc));
                                result.appliedKeysCount += c;
                                if (c > 0) restoredEmoji = true;
                            }
                            if (accBundle.has("save_gallery_exceptions")) {
                                result.appliedKeysCount += deserializeGalleryExceptions(
                                        context, targetAcc, accBundle.optJSONObject("save_gallery_exceptions"), rollback);
                            }
                        }
                    }
                }
            }

            if (options.includeTelegramTheme) {
                JSONObject themeObj = sections.optJSONObject("tg_theme");
                JSONObject sanitizedThemeObj = themeObj != null ? filterThemesJsonEntries(themeObj) : null;
                if (sanitizedThemeObj != null) {
                    SharedPreferences themePrefs = context.getSharedPreferences("themeconfig", Context.MODE_PRIVATE);
                    int c = deserializeIntoPreferences(themePrefs, sanitizedThemeObj,
                            key -> isExcludedThemeConfigKey(key) || isUnavailableThemeReference(key, sanitizedThemeObj, sanitizedThemeObj),
                            true, rollback, "themeconfig");
                    result.appliedKeysCount += c;
                    if (c > 0) {
                        restoredTheme = true;
                        result.requiresRestart = true;
                    }
                }
                JSONObject themeMainSource = sections.optJSONObject("tg_theme_main");
                if (themeMainSource == null && schema == 1) {
                    themeMainSource = sections.optJSONObject("tg_main");
                }
                if (themeMainSource != null) {
                    final JSONObject refThemeObj = sanitizedThemeObj;
                    final JSONObject src = themeMainSource;
                    SharedPreferences mainPrefs = getMainConfigPrefs(context, 0);
                    int c = deserializeIntoPreferences(mainPrefs, src,
                            key -> !isThemeMainConfigKey(key) || isUnavailableThemeReference(key, src, refThemeObj),
                            true, rollback, "mainconfig.theme");
                    result.appliedKeysCount += c;
                    if (c > 0) {
                        restoredTheme = true;
                        result.requiresRestart = true;
                    }
                }
            }

            if (options.includeTelegramNotifications && sections.has("tg_notifications")) {
                JSONObject notifBundle = sections.optJSONObject("tg_notifications");
                if (notifBundle != null) {
                    JSONObject globalObj = notifBundle.optJSONObject("global");
                    if (globalObj != null) {
                        SharedPreferences globalNotifPrefs = getNotificationsPrefs(context, 0);
                        int c = deserializeIntoPreferences(globalNotifPrefs, globalObj,
                                KronoBackupManager::isExcludedNotificationKey, true, rollback, "Notifications");
                        result.appliedKeysCount += c;
                        affectedAccounts.add(0);
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                    }
                    JSONObject accObj = notifBundle.optJSONObject("account");
                    if (accObj != null && account != 0) {
                        SharedPreferences accNotifPrefs = getNotificationsPrefs(context, account);
                        int c = deserializeIntoPreferences(accNotifPrefs, accObj,
                                KronoBackupManager::isExcludedNotificationKey, true, rollback, "Notifications" + account);
                        result.appliedKeysCount += c;
                        if (c > 0) {
                            result.requiresRestart = true;
                        }
                    }
                    JSONObject multiNotifObj = notifBundle.optJSONObject("multi");
                    if (multiNotifObj != null) {
                        Iterator<String> accKeys = multiNotifObj.keys();
                        while (accKeys.hasNext()) {
                            String accStr = accKeys.next();
                            int targetAcc;
                            try {
                                targetAcc = Integer.parseInt(accStr);
                            } catch (Exception ignore) {
                                continue;
                            }
                            if (targetAcc <= 0 || targetAcc >= UserConfig.MAX_ACCOUNT_COUNT || targetAcc == account) {
                                continue;
                            }
                            JSONObject oneNotif = multiNotifObj.optJSONObject(accStr);
                            if (oneNotif == null) continue;
                            int c = deserializeIntoPreferences(
                                    getNotificationsPrefs(context, targetAcc),
                                    oneNotif, KronoBackupManager::isExcludedNotificationKey,
                                    true, rollback, "Notifications" + targetAcc);
                            result.appliedKeysCount += c;
                            affectedAccounts.add(targetAcc);
                            if (c > 0) {
                                result.requiresRestart = true;
                            }
                        }
                    }
                }
            }

            // Reload in-memory controllers cleanly after all commits succeed.
            // Order matters: SharedConfig loads LiteMode from restored mainconfig first,
            // then KronoConfig reloads kronoconfig and syncs GlassEngine + LiteMode glass flags.
            SharedConfig.reloadConfigFromDisk();
            KronoConfig.reloadConfigFromDisk();
            KronoConfig.applyFirebaseAnalyticsPreference(context);
            KronoConfig.applyFirebasePushPreference();

            if (restoredSystemLogs) {
                SharedPreferences sysPrefs = context.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
                BuildVars.LOGS_ENABLED = BuildVars.DEBUG_VERSION || sysPrefs.getBoolean("logsEnabled", BuildVars.DEBUG_VERSION);
            }

            for (int acc : affectedAccounts) {
                if (acc >= 0 && acc < UserConfig.MAX_ACCOUNT_COUNT) {
                    UserConfig.getInstance(acc).reloadUserSettingsFromDisk();
                    NotificationsController.getInstance(acc).reloadPreferencesFromDisk();
                    if (UserConfig.getInstance(acc).isClientActivated()) {
                        DownloadController.getInstance(acc).reloadPreferencesFromDisk();
                        ConnectionsManager.getInstance(acc).reloadPreferencesFromDisk();
                    }
                }
            }

            try {
                JSONObject postRestoreJson = buildBackupJson(account, BackupOptions.fromAutoConfig());
                JSONObject postSections = postRestoreJson.optJSONObject("sections");
                String postHash = sha256Hex(postSections != null ? postSections.toString() : postRestoreJson.toString());
                KronoConfig.recordCloudBackupCompleted(account, System.currentTimeMillis(), postHash);
            } catch (Exception ignore) {}
        } catch (Throwable t) {
            FileLog.e(t);
            rollback.rollbackAll();
            try {
                SharedConfig.reloadConfigFromDisk();
                KronoConfig.reloadConfigFromDisk();
                for (int acc : affectedAccounts) {
                    if (acc >= 0 && acc < UserConfig.MAX_ACCOUNT_COUNT) {
                        UserConfig.getInstance(acc).reloadUserSettingsFromDisk();
                        NotificationsController.getInstance(acc).reloadPreferencesFromDisk();
                    }
                }
            } catch (Throwable ignore) {}
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            throw new RuntimeException(t);
        } finally {
            releaseSuppressObserverAfterMainDrain();
        }

        final boolean shouldReloadTheme = restoredTheme;
        final boolean shouldNotifyEmoji = restoredEmoji;
        final boolean shouldReloadPlayback = restoredPlaybackSpeed;
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (shouldReloadTheme) {
                    Theme.reloadThemesFromDisk();
                }
                if (shouldReloadPlayback) {
                    syncMediaPlaybackSpeedsFromMainConfig();
                }
                ConnectionsManager.reloadProxySettingsFromDisk();
                ApplicationLoader.startPushService();
                MessagesController.getInstance(account).syncLocalFavoritesFilter(true);
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload, true);
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogFiltersUpdated);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
                if (shouldNotifyEmoji) {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.emojiLoaded);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        return result;
    }

    private static void syncMediaPlaybackSpeedsFromMainConfig() {
        try {
            SharedPreferences mainPrefs = MessagesController.getGlobalMainSettings();
            if (mainPrefs == null) return;
            MediaController mc = MediaController.getInstance();
            if (mc == null) return;
            float targetNormal = mainPrefs.getFloat("playbackSpeed", 1.0f);
            if (Math.abs(mc.getPlaybackSpeed(false) - targetNormal) > 0.001f) {
                mc.setPlaybackSpeed(false, targetNormal);
            }
            float targetMusic = mainPrefs.getFloat("musicPlaybackSpeed", 1.0f);
            if (Math.abs(mc.getPlaybackSpeed(true) - targetMusic) > 0.001f) {
                mc.setPlaybackSpeed(true, targetMusic);
            }
        } catch (Throwable ignore) {}
    }

    private static int deserializeGalleryExceptions(Context context, int account, JSONObject galleryExObj, RollbackSession rollback) throws IOException {
        if (galleryExObj == null) {
            return 0;
        }
        int total = 0;
        if (galleryExObj.has("users")) {
            total += deserializeIntoPreferences(
                    context.getSharedPreferences(SaveToGallerySettingsHelper.USERS_PREF_NAME + "_" + account, Context.MODE_PRIVATE),
                    galleryExObj.optJSONObject("users"), null, true, rollback, "save_gallery_users_" + account);
        }
        if (galleryExObj.has("groups")) {
            total += deserializeIntoPreferences(
                    context.getSharedPreferences(SaveToGallerySettingsHelper.GROUPS_PREF_NAME + "_" + account, Context.MODE_PRIVATE),
                    galleryExObj.optJSONObject("groups"), null, true, rollback, "save_gallery_groups_" + account);
        }
        if (galleryExObj.has("channels")) {
            total += deserializeIntoPreferences(
                    context.getSharedPreferences(SaveToGallerySettingsHelper.CHANNELS_PREF_NAME + "_" + account, Context.MODE_PRIVATE),
                    galleryExObj.optJSONObject("channels"), null, true, rollback, "save_gallery_channels_" + account);
        }
        return total;
    }

    private static int deserializeSafeUserConfig(SharedPreferences prefs, JSONObject source, RollbackSession rollback, String label) throws IOException {
        if (prefs == null || source == null) {
            return 0;
        }
        rollback.captureIfNeeded(prefs);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> existing = prefs.getAll();
        if (existing != null) {
            for (String existingKey : existing.keySet()) {
                if (!TextUtils.isEmpty(existingKey) && isSafeUserConfigKey(existingKey)) {
                    editor.remove(existingKey);
                }
            }
        }
        int count = 0;
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (TextUtils.isEmpty(key) || !isSafeUserConfigKey(key)) {
                continue;
            }
            JSONObject entry = source.optJSONObject(key);
            count += applyTypedEntry(editor, key, entry);
        }
        rollback.commitOrThrow(prefs, editor, label);
        return count;
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

    private static int deserializeIntoPreferences(SharedPreferences prefs, JSONObject source, KeyFilter filter, boolean clearNonExcludedExisting, RollbackSession rollback, String label) throws IOException {
        if (prefs == null || source == null) {
            return 0;
        }
        rollback.captureIfNeeded(prefs);
        SharedPreferences.Editor editor = prefs.edit();
        if (clearNonExcludedExisting) {
            Map<String, ?> existing = prefs.getAll();
            if (existing != null) {
                for (String existingKey : existing.keySet()) {
                    if (!TextUtils.isEmpty(existingKey) && (filter == null || !filter.isExcluded(existingKey))) {
                        editor.remove(existingKey);
                    }
                }
            }
        }
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
        rollback.commitOrThrow(prefs, editor, label);
        return count;
    }

    private static JSONObject serializeAccountFolderIcons(SharedPreferences kronoPrefs, int account) throws Exception {
        JSONObject out = new JSONObject();
        if (kronoPrefs == null) {
            return out;
        }
        String prefix = "filter_icon_" + account + "_";
        Map<String, ?> all = kronoPrefs.getAll();
        if (all == null) {
            return out;
        }
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (key != null && key.startsWith(prefix) && val instanceof String && !TextUtils.isEmpty((String) val)) {
                String filterIdPart = key.substring(prefix.length());
                if (!TextUtils.isEmpty(filterIdPart)) {
                    out.put(filterIdPart, val);
                }
            }
        }
        return out;
    }

    private static int deserializeAccountFolderIcons(SharedPreferences kronoPrefs, int account, JSONObject iconsObj, RollbackSession rollback) throws IOException {
        if (kronoPrefs == null || iconsObj == null) {
            return 0;
        }
        rollback.captureIfNeeded(kronoPrefs);
        SharedPreferences.Editor editor = kronoPrefs.edit();
        String prefix = "filter_icon_" + account + "_";
        Map<String, ?> existing = kronoPrefs.getAll();
        if (existing != null) {
            for (String existingKey : existing.keySet()) {
                if (existingKey != null && existingKey.startsWith(prefix)) {
                    editor.remove(existingKey);
                }
            }
        }
        int count = 0;
        Iterator<String> keys = iconsObj.keys();
        while (keys.hasNext()) {
            String filterIdPart = keys.next();
            if (TextUtils.isEmpty(filterIdPart)) {
                continue;
            }
            String val = iconsObj.optString(filterIdPart, "");
            if (!TextUtils.isEmpty(val)) {
                editor.putString(prefix + filterIdPart, val);
                count++;
            }
        }
        rollback.commitOrThrow(kronoPrefs, editor, "kronoconfig.folder_icons_" + account);
        return count;
    }

    private static boolean hasLegacyFolderOrderKey(JSONObject source) {
        if (source == null) {
            return false;
        }
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key != null && key.startsWith("customFilterOrder_")) {
                return true;
            }
        }
        return false;
    }

    private static int deserializeKronoPreferences(SharedPreferences prefs, int targetAccount, int sourceAccount, JSONObject source, RollbackSession rollback) throws IOException {
        if (prefs == null || source == null) {
            return 0;
        }
        rollback.captureIfNeeded(prefs);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> existing = prefs.getAll();
        if (existing != null) {
            for (String existingKey : existing.keySet()) {
                if (!TextUtils.isEmpty(existingKey)
                        && !isExcludedKronoKey(existingKey)
                        && !existingKey.startsWith("filter_icon_")
                        && !existingKey.startsWith("customFilterOrder_")
                        && !existingKey.startsWith("localPinnedDialogs_")
                        && !existingKey.startsWith("favoriteDialogs_")
                        && !existingKey.equals("localPinnedDialogs")
                        && !existingKey.equals("favoriteDialogs")) {
                    editor.remove(existingKey);
                }
            }
        }
        editor.remove("localPinnedDialogs_" + targetAccount);
        editor.remove("favoriteDialogs_" + targetAccount);
        if (targetAccount == 0) {
            editor.remove("localPinnedDialogs");
            editor.remove("favoriteDialogs");
        }
        final String preferredPinnedKey = sourceAccount >= 0 && source.has("localPinnedDialogs_" + sourceAccount)
                ? ("localPinnedDialogs_" + sourceAccount)
                : null;
        final String preferredFavoriteKey = sourceAccount >= 0 && source.has("favoriteDialogs_" + sourceAccount)
                ? ("favoriteDialogs_" + sourceAccount)
                : null;
        boolean appliedPinned = false;
        boolean appliedFavorites = false;
        int count = 0;
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (TextUtils.isEmpty(key) || isExcludedKronoKey(key)) {
                continue;
            }
            if (key.startsWith("localPinnedDialogs_") || key.equals("localPinnedDialogs")) {
                if (preferredPinnedKey != null && !preferredPinnedKey.equals(key)) {
                    continue;
                }
                if (preferredPinnedKey == null && appliedPinned) {
                    continue;
                }
                JSONObject entry = source.optJSONObject(key);
                int added = applyTypedEntry(editor, "localPinnedDialogs_" + targetAccount, entry);
                if (added > 0) {
                    appliedPinned = true;
                    if (targetAccount == 0) {
                        applyTypedEntry(editor, "localPinnedDialogs", entry);
                    }
                    count += added;
                }
                continue;
            }
            if (key.startsWith("favoriteDialogs_") || key.equals("favoriteDialogs")) {
                if (preferredFavoriteKey != null && !preferredFavoriteKey.equals(key)) {
                    continue;
                }
                if (preferredFavoriteKey == null && appliedFavorites) {
                    continue;
                }
                JSONObject entry = source.optJSONObject(key);
                int added = applyTypedEntry(editor, "favoriteDialogs_" + targetAccount, entry);
                if (added > 0) {
                    appliedFavorites = true;
                    if (targetAccount == 0) {
                        applyTypedEntry(editor, "favoriteDialogs", entry);
                    }
                    count += added;
                }
                continue;
            }
            String destKey = key;
            if (key.startsWith("filter_icon_")) {
                int lastUnderscore = key.lastIndexOf('_');
                if (lastUnderscore > "filter_icon_".length() && lastUnderscore < key.length() - 1) {
                    destKey = "filter_icon_" + targetAccount + "_" + key.substring(lastUnderscore + 1);
                }
            } else if (key.startsWith("customFilterOrder_")) {
                destKey = "customFilterOrder_" + targetAccount;
            }
            JSONObject entry = source.optJSONObject(key);
            count += applyTypedEntry(editor, destKey, entry);
        }
        rollback.commitOrThrow(prefs, editor, "kronoconfig");
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
                || key.startsWith("lastRemoteFilterOrder_")
                || key.startsWith("ghost_read_max_")
                || key.startsWith("ghostLocalRead_")
                || key.startsWith("ghostPrev_")
                || key.startsWith("telemetryPrev_")
                || key.startsWith("cloudLastBackupTime")
                || key.startsWith("cloudLastBackupHash")
                || key.startsWith("cloudBackupPending");
    }

    private static boolean isSafeUserConfigKey(String key) {
        if (TextUtils.isEmpty(key)) return false;
        return "sortContactsByName".equals(key)
                || "sortFilesByName".equals(key)
                || "saveIncomingPhotos".equals(key)
                || "allowScreenCapture".equals(key)
                || "proxyRotationEnabled".equals(key)
                || "proxyRotationTimeout".equals(key)
                || "syncContacts".equals(key)
                || "suggestContacts".equals(key)
                || "showCallsTab".equals(key)
                || key.startsWith("keep_media_exceptions_")
                || "textSelectionHintShows".equals(key)
                || "scheduledOrNoSoundHintShows".equals(key)
                || "scheduledHintShows".equals(key)
                || "forwardingOptionsHintShown".equals(key)
                || "replyingOptionsHintShown".equals(key)
                || "lockRecordAudioVideoHint".equals(key);
    }

    private static boolean isSafeSystemConfigKey(String key) {
        return "logsEnabled".equals(key);
    }

    private static boolean isExcludedArticleKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        return key.startsWith("article");
    }

    private static boolean isThemeMainConfigKey(String key) {
        if (TextUtils.isEmpty(key)) return false;
        return "theme".equals(key)
                || "nighttheme".equals(key)
                || "selectedAutoNightType".equals(key)
                || "autoNightScheduleByLocation".equals(key)
                || "autoNightBrighnessThreshold".equals(key)
                || "autoNightDayStartTime".equals(key)
                || "autoNightDayEndTime".equals(key)
                || "autoNightSunsetTime".equals(key)
                || "autoNightSunriseTime".equals(key)
                || "autoNightCityName".equals(key)
                || "autoNightLocationLatitude3".equals(key)
                || "autoNightLocationLongitude3".equals(key)
                || "autoNightLastSunCheckDay".equals(key)
                || "dayNightThemeSwitchHintCount".equals(key)
                || "dayNightWallpaperSwitchHint".equals(key)
                || "selectedBackground2".equals(key)
                || "overrideThemeWallpaper".equals(key)
                || "selectedColor".equals(key)
                || "selectedBackgroundSlug".equals(key)
                || "selectedGradientColor".equals(key)
                || "selectedGradientColor2".equals(key)
                || "selectedGradientColor3".equals(key)
                || "selectedGradientRotation".equals(key)
                || "selectedBackgroundBlurred".equals(key)
                || "selectedBackgroundMotion".equals(key)
                || "selectedIntensity".equals(key)
                || key.startsWith("accent_for_");
    }

    private static boolean isExcludedMainConfigKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        String lower = key.toLowerCase(Locale.US);
        // Secrets, push tokens, device/session caches, and ephemeral per-dialog state
        if (lower.contains("passcode")
                || lower.contains("applocked")
                || lower.contains("fingerprint")
                || lower.contains("pushstring")
                || lower.contains("pushstat")
                || lower.contains("pushauthkey")
                || lower.contains("gcm")
                || lower.contains("fcm")
                || lower.equals("passportconfigjson")
                || lower.equals("passportconfighash")
                || lower.equals("lastlocalid")
                || lower.contains("camera_cache")
                || lower.equals("cameracache")
                || lower.equals("deviceperformanceclass")
                || lower.equals("selectedaccount")
                || lower.equals("hasemaillogin")
                || lower.equals("directsharehash2")
                || lower.equals("proxy_dialog")
                || lower.equals("proxydialogaddress")
                || lower.equals("promo_psa_message")
                || lower.equals("promo_psa_type")
                || lower.equals("promo_dialog_type")
                || lower.equals("nextpromoinfochecktime")
                || lower.equals("lastkeepmediachecktime")
                || lower.equals("lastlogschecktime")
                || lower.equals("lastupdatechecktime")
                || lower.equals("pendingappupdate")
                || lower.equals("pendingappupdatebuild")
                || lower.equals("appupdate")
                || lower.equals("appupdatebuild")
                || lower.equals("appupdatechecktime")
                || lower.equals("dontaskmanagestorage")
                || lower.equals("remoteconfigloaded")
                || lower.equals("installreferer")
                || lower.equals("pk")
                || lower.equals("floatingdebugactive")
                || lower.startsWith("storage_cache_dir")
                || lower.equals("storagecachedir")
                || lower.startsWith("chatpendingrequests")
                || lower.startsWith("effects_last_")
                || lower.startsWith("forumlasttopic")
                || lower.startsWith("2dialogs")
                || lower.startsWith("2pinned")
                || lower.startsWith("2total")
                || lower.startsWith("6migrate")
                || lower.startsWith("lastloading")) {
            return true;
        }
        // Server-pushed remote appConfig values & heavy lookup lists (fetched automatically from Telegram)
        return lower.equals("autologintoken")
                || lower.equals("autologindomains")
                || lower.equals("authdomains")
                || lower.equals("dcdomainname2")
                || lower.equals("webfiledatacenterid")
                || lower.equals("googleauthclientid")
                || lower.equals("tonproxyaddress")
                || lower.equals("tonblockchainexplorerurl")
                || lower.equals("weathersearchusername")
                || lower.equals("premiummanagesubscriptionurl")
                || lower.equals("premiuminvoiceslug")
                || lower.equals("premiumbotusername")
                || lower.equals("premiumlocked")
                || lower.equals("verifyagebotusername")
                || lower.equals("verifyagecountry")
                || lower.equals("verifyagemin")
                || lower.equals("peercolors")
                || lower.equals("profilepeercolors")
                || lower.equals("emojisounds")
                || lower.equals("diceemojies")
                || lower.equals("dicesuccess")
                || lower.equals("gifsearchemojies")
                || lower.equals("premiumfeaturestypestoposition")
                || lower.equals("businessfeaturestypestoposition")
                || lower.equals("stargiftscraftattributespermilles")
                || lower.equals("tonstakedicestakesuggestedamounts")
                || lower.equals("whitelistedbots")
                || lower.equals("pendingsuggestions")
                || lower.equals("dismissedsuggestions")
                || lower.equals("ignorerestrictionreasons")
                || lower.equals("exporturi2")
                || lower.equals("exportgroupuri")
                || lower.equals("exportprivateuri")
                || lower.equals("directpaymentscurrency")
                || lower.equals("starsgroupcallmessagelimits")
                || lower.equals("webappallowedprotocols")
                || lower.equals("starrefstartparamprefixes")
                || lower.equals("freezeappealurl")
                || lower.equals("freezesincedate")
                || lower.equals("freezeuntildate")
                || lower.equals("telegramantispamuserid")
                || lower.equals("telegramantispamgroupsizemin")
                || lower.equals("stories_changelog_user_id")
                || lower.startsWith("stories_stealth_")
                || lower.startsWith("giveaway_")
                || lower.startsWith("boosts_")
                || lower.startsWith("uploadmaxfileparts")
                || lower.startsWith("uploadpremiumspeedup")
                || lower.equals("maxgroupcount")
                || lower.equals("maxmegagroupcount")
                || lower.equals("maxrecentgifscount")
                || lower.equals("maxrecentstickerscount")
                || lower.equals("maxfavestickerscount")
                || lower.equals("maxedittime")
                || lower.equals("ratingdecay")
                || lower.equals("linkprefix")
                || lower.equals("callreceivetimeout")
                || lower.equals("callringtimeout")
                || lower.equals("callconnecttimeout")
                || lower.equals("callpackettimeout")
                || lower.equals("updatecheckdelay")
                || lower.startsWith("maxfolderpinneddialogscount")
                || lower.equals("maxmessagelength")
                || lower.equals("maxcaptionlength")
                || lower.equals("mapprovider")
                || lower.equals("availablemapproviders")
                || lower.equals("revoketimelimit")
                || lower.equals("revoketimepmlimit")
                || lower.equals("canrevokepminbox")
                || lower.equals("preloadfeaturedstickers")
                || lower.equals("gifsearchbot")
                || lower.equals("imagesearchbot")
                || lower.equals("blockedcountry")
                || lower.equals("suggestedlangcode")
                || lower.equals("getfileexperimentalparams")
                || lower.equals("android_download_improvement_killswitch")
                || lower.equals("smsjobsstickynotificationenabled")
                || lower.equals("autoarchiveavailable")
                || lower.equals("groipcallvideomaxparticipants")
                || lower.equals("chatreadmarksizethreshold")
                || lower.equals("chatreadmarkexpireperiod")
                || lower.equals("ringtonedurationmax")
                || lower.equals("ringtonesizemax")
                || lower.equals("pmreaddateexpireperiod")
                || lower.equals("suggeststickersapionly")
                || lower.equals("roundvideosize")
                || lower.equals("roundvideobitrate")
                || lower.equals("roundaudiobitrate")
                || lower.startsWith("channelslimit")
                || lower.startsWith("savedgifslimit")
                || lower.startsWith("stickersfavedlimit")
                || lower.startsWith("maxpinneddialogscount")
                || lower.startsWith("dialogfilterslimit")
                || lower.startsWith("dialogfilterschatslimit")
                || lower.startsWith("dialogfilterspinnedlimit")
                || lower.startsWith("publiclinkslimit")
                || lower.startsWith("captionlengthlimit")
                || lower.startsWith("storycaptionlengthlimit")
                || lower.startsWith("aboutlengthlimit")
                || lower.startsWith("reactionsusermax")
                || lower.equals("reactionsinchatmax")
                || lower.equals("reactionsuniqmax")
                || lower.equals("forumupgradeparticipantsmin")
                || lower.equals("topicspinnedlimit")
                || lower.equals("hiddenmembersgroupsizemin")
                || lower.equals("chatlistupdateperiod")
                || lower.equals("smallqueuemaxactiveoperations")
                || lower.equals("largequeuemaxactiveoperations")
                || lower.startsWith("chatlistinviteslimit")
                || lower.startsWith("chatlistjoinedlimit")
                || lower.startsWith("storyexpiringlimit")
                || lower.startsWith("storiessent")
                || lower.startsWith("storiessuggested")
                || lower.endsWith("levelmin")
                || lower.equals("quickreplieslimit")
                || lower.startsWith("stargifts")
                || lower.startsWith("stars")
                || lower.startsWith("starref")
                || lower.equals("botpreviewmediasmax")
                || lower.equals("botverificationdescriptionlengthlimit")
                || lower.equals("conferencecallsizelimit")
                || lower.equals("callrequestsdisabled")
                || lower.startsWith("todo")
                || lower.equals("translationsmanualenabled")
                || lower.equals("translationsautoenabled")
                || lower.equals("enablegiftsinprofile")
                || lower.equals("storiesposting")
                || lower.equals("storiesentities")
                || lower.equals("storiesexportnopubliclink")
                || lower.equals("authorization_autoconfirm_period")
                || lower.equals("quotelengthmax")
                || lower.equals("fpnvallowed")
                || lower.equals("secretwebpage2")
                || lower.equals("androiddisableroundcamera2")
                || lower.equals("storyweatherpreload")
                || lower.equals("showannualpermonth")
                || lower.equals("caneditfactcheck")
                || lower.equals("factchecklengthlimit")
                || lower.equals("sponsoredlinksinappallow")
                || lower.equals("disablebotfullscreenblur")
                || lower.equals("videoignorealtdocuments")
                || lower.equals("uploadmarkupvideo")
                || lower.equals("giftattachmenuicon")
                || lower.equals("gifttextfieldicon")
                || lower.equals("checkresetlangpack")
                || lower.startsWith("transcribe")
                || lower.equals("introtitlelengthlimit")
                || lower.equals("introdescriptionlengthlimit")
                || lower.equals("businesschatlinkslimit")
                || lower.equals("channelrevenuewithdrawalenabled")
                || lower.equals("newnoncontactpeersrequirepremiumwithoutownpremium");
    }

    private static boolean isExcludedThemeConfigKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        String lower = key.toLowerCase(Locale.US);
        return lower.equals("remote_version")
                || lower.startsWith("2remotethemeshash")
                || lower.startsWith("lastloadingthemestime")
                || lower.startsWith("theme_file_");
    }

    private static JSONObject filterThemesJsonEntries(JSONObject themeObj) {
        if (themeObj == null || !themeObj.has("themes2")) {
            return themeObj;
        }
        try {
            JSONObject themes2Entry = themeObj.optJSONObject("themes2");
            if (themes2Entry == null || !"s".equals(themes2Entry.optString("t"))) {
                return themeObj;
            }
            JSONArray arr = new JSONArray(themes2Entry.optString("v", "[]"));
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) continue;
                boolean hasRemoteInfo = item.has("info") && !TextUtils.isEmpty(item.optString("info"));
                String path = item.optString("path", "");
                boolean hasLocalFile = !TextUtils.isEmpty(path) && new File(path).exists();
                if (hasRemoteInfo || hasLocalFile) {
                    filtered.put(item);
                }
            }
            JSONObject copy = new JSONObject(themeObj.toString());
            JSONObject newEntry = new JSONObject();
            newEntry.put("t", "s");
            newEntry.put("v", filtered.toString());
            copy.put("themes2", newEntry);
            return copy;
        } catch (Exception e) {
            return themeObj;
        }
    }

    private static boolean isBuiltInThemeKey(String themeName) {
        return "Blue".equals(themeName)
                || "Dark Blue".equals(themeName)
                || "Arctic Blue".equals(themeName)
                || "Day".equals(themeName)
                || "Night".equals(themeName)
                || "AMOLED".equals(themeName)
                || "Monet Light".equals(themeName)
                || "Monet Dark".equals(themeName)
                || "Monet AMOLED".equals(themeName)
                || "Default".equals(themeName)
                || "Dark".equals(themeName);
    }

    private static boolean isThemePresentInBackupThemes2(String themeName, JSONObject themeObj) {
        if (themeObj == null || !themeObj.has("themes2")) {
            return false;
        }
        try {
            JSONObject themes2Entry = themeObj.optJSONObject("themes2");
            if (themes2Entry == null || !"s".equals(themes2Entry.optString("t"))) {
                return false;
            }
            JSONArray arr = new JSONArray(themes2Entry.optString("v", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) continue;
                String name = item.optString("name", "");
                if (themeName.equals(name)) {
                    return true;
                }
                if (item.has("info") && !TextUtils.isEmpty(item.optString("info"))) {
                    try {
                        byte[] bytes = Utilities.hexToBytes(item.optString("info"));
                        if (bytes != null) {
                            org.telegram.tgnet.SerializedData sd = new org.telegram.tgnet.SerializedData(bytes);
                            TLRPC.TL_theme tl = (TLRPC.TL_theme) TLRPC.Theme.TLdeserialize(sd, sd.readInt32(true), true);
                            sd.cleanup();
                            if (tl != null && ("remote" + tl.id).equals(themeName)) {
                                return true;
                            }
                        }
                    } catch (Throwable ignore) {}
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    private static boolean isUnavailableThemeReference(String key, JSONObject entryContainer, JSONObject themeObj) {
        if (!"theme".equals(key) && !"nighttheme".equals(key) && !"lastDayTheme".equals(key) && !"lastDarkTheme".equals(key)) {
            return false;
        }
        if (entryContainer == null) {
            return false;
        }
        JSONObject entry = entryContainer.optJSONObject(key);
        if (entry == null || !"s".equals(entry.optString("t"))) {
            return false;
        }
        String themeName = entry.optString("v", "");
        if (TextUtils.isEmpty(themeName)) {
            return true;
        }
        if (isBuiltInThemeKey(themeName) || Theme.getTheme(themeName) != null) {
            return false;
        }
        return !isThemePresentInBackupThemes2(themeName, themeObj);
    }

    private static boolean isExcludedNotificationKey(String key) {
        if (TextUtils.isEmpty(key)) return true;
        String lower = key.toLowerCase(Locale.US);
        return lower.equals("otherkey")
                || lower.contains("sound_path")
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
                ParsedBackup parsed = parseBackupJson(formatted);
                byte[] bytes = formatted.getBytes(StandardCharsets.UTF_8);
                try (java.io.OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) {
                        throw new IllegalStateException("No se pudo abrir el archivo de destino");
                    }
                    os.write(bytes);
                    os.flush();
                }
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
                String content = readStreamUtf8OrGzipBounded(is);
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
                    String content = readStreamUtf8OrGzipBounded(is);
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
                parseBackupJson(formatted);
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
                invokeCloudMethodWithRetry(account, botUser, "getStorageValues", params, 0, resultJson -> {
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
                JSONObject sectionsObj = backupJson.optJSONObject("sections");
                String sectionsHash = sha256Hex(sectionsObj != null ? sectionsObj.toString() : backupJson.toString());
                uploadPreparedBackupToCloud(account, options, backupJson, sectionsHash, false, onSuccess, onError);
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage() != null ? e.getMessage() : "Error al preparar copia en la nube";
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(msg);
                });
            }
        });
    }

    private static void uploadPreparedBackupToCloud(int account, BackupOptions options, JSONObject backupJson, String sectionsHash, boolean isAutoUpload, Utilities.Callback<BackupMetadata> onSuccess, Utilities.Callback<String> onError) {
        try {
            String rawJson = backupJson.toString();
            String sha256 = sha256Hex(rawJson);
            String compressed = compressToBase64(rawJson);
            List<String> chunks = splitIntoChunksUtf8Safe(compressed, CLOUD_CHUNK_SIZE, CLOUD_MAX_CHUNK_UTF8_BYTES);
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
                // Save all chunks sequentially with retries, then commit metadata atomically last, then clean up stale chunks
                saveChunksSequentially(account, botUser, chunks, 0, () -> {
                    try {
                        JSONObject metaParams = new JSONObject();
                        metaParams.put("key", CLOUD_META_KEY);
                        metaParams.put("value", metaJsonStr);
                        invokeCloudMethodWithRetry(account, botUser, "saveStorageValue", metaParams, 0, ignored -> {
                            if (!isAutoUpload || !dirtyDuringUpload.get()) {
                                KronoConfig.recordCloudBackupCompleted(account, meta.timestamp, sectionsHash);
                            }
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
            invokeCloudMethodWithRetry(account, botUser, "getStorageKeys", new JSONObject(), 0, resultJson -> {
                try {
                    List<String> keysToDelete = extractKronoBackupKeys(resultJson);
                    if (keysToDelete.isEmpty()) {
                        KronoConfig.recordCloudBackupCompleted(account, 0L, "");
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onSuccess != null) onSuccess.run();
                        });
                        return;
                    }
                    deleteKeysInBatches(account, botUser, keysToDelete, 0, () -> {
                        KronoConfig.recordCloudBackupCompleted(account, 0L, "");
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

    public static void releaseSuppressObserverAfterMainDrain() {
        AndroidUtilities.runOnUIThread(() -> setSuppressObserver(false));
    }

    private static long getLastAutoUploadElapsed(int account) {
        synchronized (lastAutoUploadElapsedByAccount) {
            Long val = lastAutoUploadElapsedByAccount.get(account);
            return val != null ? val : 0L;
        }
    }

    private static void setLastAutoUploadElapsed(int account, long elapsedMs) {
        synchronized (lastAutoUploadElapsedByAccount) {
            lastAutoUploadElapsedByAccount.put(account, elapsedMs);
        }
    }

    private static long getLastAutoCheckElapsed(int account) {
        synchronized (lastAutoCheckElapsedByAccount) {
            Long val = lastAutoCheckElapsedByAccount.get(account);
            return val != null ? val : 0L;
        }
    }

    private static void setLastAutoCheckElapsed(int account, long elapsedMs) {
        synchronized (lastAutoCheckElapsedByAccount) {
            lastAutoCheckElapsedByAccount.put(account, elapsedMs);
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
                int activeAcc = observedAccount;
                if (key.startsWith("filter_icon_")) {
                    if (!key.startsWith("filter_icon_" + activeAcc + "_")) return;
                } else if (isVaultKronoKey(key)) {
                    if (!KronoConfig.cloudAutoIncludeVault) return;
                } else if (isExcludedKronoKey(key)) {
                    return;
                }
                scheduleOnChangeBackup(activeAcc);
            };
            mainPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                boolean isThemeKey = isThemeMainConfigKey(key);
                if (isThemeKey) {
                    if (!KronoConfig.cloudAutoIncludeTheme) return;
                } else {
                    if (!KronoConfig.cloudAutoIncludeTelegram || isExcludedMainConfigKey(key)) return;
                }
                scheduleOnChangeBackup(observedAccount);
            };
            userConfPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeTelegram || !isSafeUserConfigKey(key)) return;
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
            auxPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || TextUtils.isEmpty(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeTelegram) return;
                scheduleOnChangeBackup(observedAccount);
            };
            systemPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || !isSafeSystemConfigKey(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeTelegram) return;
                scheduleOnChangeBackup(observedAccount);
            };
            articlesPrefListener = (prefs, key) -> {
                if (suppressObserverDepth > 0 || isExcludedArticleKey(key)) return;
                if (KronoConfig.cloudBackupMode != KronoConfig.CLOUD_BACKUP_ON_CHANGE) return;
                if (!KronoConfig.cloudAutoIncludeTelegram) return;
                scheduleOnChangeBackup(observedAccount);
            };
            connectionStateDelegate = (id, acc, args) -> {
                if (id == NotificationCenter.didUpdateConnectionState && acc == observedAccount) {
                    int state = ConnectionsManager.getInstance(acc).getConnectionState();
                    if (state == ConnectionsManager.ConnectionStateConnected
                            && KronoConfig.cloudBackupMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
                        if (KronoConfig.isCloudBackupPending(acc)) {
                            scheduleOnChangeBackup(acc);
                        } else if (KronoConfig.getCloudLastBackupTime(acc) == 0L) {
                            checkRemoteAndMaybeRunInitialBackup(acc);
                        }
                    }
                }
            };

            context.getSharedPreferences("kronoconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(kronoPrefListener);
            context.getSharedPreferences("mainconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(mainPrefListener);
            context.getSharedPreferences("userconfing", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(userConfPrefListener);
            context.getSharedPreferences("themeconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(themePrefListener);
            context.getSharedPreferences("Notifications", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(notifPrefListener);
            context.getSharedPreferences("playback_speed", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("camera", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("paint", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("pipconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("voippipconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("groupcallpipconfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("shapedetector_conf", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(auxPrefListener);
            context.getSharedPreferences("systemConfig", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(systemPrefListener);
            context.getSharedPreferences("articles", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(articlesPrefListener);

            observedAccount = account;
            attachAccountSpecificObserversLocked(context, account);
            observerInitialized = true;
        } else if (observedAccount != account) {
            AndroidUtilities.cancelRunOnUIThread(debouncedOnChangeRunnable);
            detachAccountSpecificObserversLocked(observedAccount);
            observedAccount = account;
            attachAccountSpecificObserversLocked(context, account);
            if (KronoConfig.cloudBackupMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
                if (KronoConfig.isCloudBackupPending(account)) {
                    scheduleOnChangeBackup(account);
                } else if (KronoConfig.getCloudLastBackupTime(account) == 0L) {
                    checkRemoteAndMaybeRunInitialBackup(account);
                }
            }
        }
    }

    private static void attachAccountSpecificObserversLocked(Context context, int account) {
        try {
            if (account != 0) {
                observedAccountMainPrefs = getMainConfigPrefs(context, account);
                if (observedAccountMainPrefs != null && mainPrefListener != null) {
                    observedAccountMainPrefs.registerOnSharedPreferenceChangeListener(mainPrefListener);
                }
                observedAccountUserPrefs = getUserConfigPrefs(context, account);
                if (observedAccountUserPrefs != null && userConfPrefListener != null) {
                    observedAccountUserPrefs.registerOnSharedPreferenceChangeListener(userConfPrefListener);
                }
                observedAccountNotifPrefs = getNotificationsPrefs(context, account);
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
            if (observedAccountMainPrefs != null && mainPrefListener != null) {
                observedAccountMainPrefs.unregisterOnSharedPreferenceChangeListener(mainPrefListener);
                observedAccountMainPrefs = null;
            }
            if (observedAccountUserPrefs != null && userConfPrefListener != null) {
                observedAccountUserPrefs.unregisterOnSharedPreferenceChangeListener(userConfPrefListener);
                observedAccountUserPrefs = null;
            }
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
        KronoConfig.setCloudBackupPending(account, true);
        if (autoBackupInProgress.get()) {
            dirtyDuringUpload.set(true);
            return;
        }
        long lastUpload = getLastAutoUploadElapsed(account);
        long elapsedSinceLast = lastUpload > 0L
                ? (SystemClock.elapsedRealtime() - lastUpload)
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
            KronoConfig.setCloudBackupPending(account, true);
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
                    KronoConfig.setCloudBackupPending(account, false);
                    autoBackupInProgress.set(false);
                    return;
                }
                JSONObject json = buildBackupJson(account, options);
                JSONObject sections = json.optJSONObject("sections");
                String contentHash = sha256Hex(sections != null ? sections.toString() : json.toString());
                String lastHash = KronoConfig.getCloudLastBackupHash(account);
                if (!TextUtils.isEmpty(lastHash) && contentHash.equals(lastHash)) {
                    KronoConfig.setCloudBackupPending(account, false);
                    autoBackupInProgress.set(false);
                    return;
                }
                uploadPreparedBackupToCloud(account, options, json, contentHash, true, meta -> {
                    setLastAutoUploadElapsed(account, SystemClock.elapsedRealtime());
                    autoBackupInProgress.set(false);
                    if (dirtyDuringUpload.getAndSet(false)) {
                        scheduleOnChangeBackup(observedAccount);
                    }
                }, err -> {
                    FileLog.e("KronoBackupManager on-change auto-backup failed: " + err);
                    KronoConfig.setCloudBackupPending(account, true);
                    autoBackupInProgress.set(false);
                });
            } catch (Exception e) {
                FileLog.e(e);
                KronoConfig.setCloudBackupPending(account, true);
                autoBackupInProgress.set(false);
            }
        });
    }

    private static void checkRemoteAndMaybeRunInitialBackup(int account) {
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return;
        }
        int connState = ConnectionsManager.getInstance(account).getConnectionState();
        if (connState != ConnectionsManager.ConnectionStateConnected) {
            return;
        }
        synchronized (remoteCheckedAccounts) {
            if (remoteCheckedAccounts.contains(account) || !remoteCheckInProgressAccounts.add(account)) {
                return;
            }
        }
        fetchCloudMetadata(account, meta -> {
            synchronized (remoteCheckedAccounts) {
                remoteCheckInProgressAccounts.remove(account);
                remoteCheckedAccounts.add(account);
            }
            if (meta != null && meta.timestamp > 0) {
                // Existing cloud backup found on @KronoGramCloudBot — do NOT overwrite with fresh defaults
                return;
            }
            if (KronoConfig.cloudBackupMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
                executeOnChangeBackup(account);
            }
        }, err -> {
            synchronized (remoteCheckedAccounts) {
                remoteCheckInProgressAccounts.remove(account);
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
            if (KronoConfig.isCloudBackupPending(account)) {
                executeOnChangeBackup(account);
            } else if (KronoConfig.getCloudLastBackupTime(account) == 0L) {
                checkRemoteAndMaybeRunInitialBackup(account);
            }
            return;
        }

        long nowElapsed = SystemClock.elapsedRealtime();
        long prevCheck = getLastAutoCheckElapsed(account);
        if (prevCheck > 0L && (nowElapsed - prevCheck) < AUTO_CHECK_THROTTLE_MS) {
            return;
        }
        setLastAutoCheckElapsed(account, nowElapsed);

        long nowWall = System.currentTimeMillis();
        long interval = mode == KronoConfig.CLOUD_BACKUP_WEEKLY ? WEEKLY_INTERVAL_MS : DAILY_INTERVAL_MS;
        long lastBackupTime = KronoConfig.getCloudLastBackupTime(account);
        if (lastBackupTime > 0 && (nowWall - lastBackupTime) < interval) {
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
                String lastHash = KronoConfig.getCloudLastBackupHash(account);
                if (!TextUtils.isEmpty(lastHash) && contentHash.equals(lastHash)) {
                    KronoConfig.recordCloudBackupCompleted(account, nowWall, contentHash);
                    autoBackupInProgress.set(false);
                    return;
                }
                uploadPreparedBackupToCloud(account, options, json, contentHash, false, meta -> {
                    setLastAutoUploadElapsed(account, SystemClock.elapsedRealtime());
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
    // Internal CloudStorage Helpers (with Automatic Retry & Backoff)
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

    private static void invokeCloudMethodWithRetry(
            int account,
            TLRPC.User botUser,
            String method,
            JSONObject params,
            int attempt,
            Utilities.Callback<String> onSuccess,
            Utilities.Callback<String> onError
    ) {
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
                String errText = error.text != null ? error.text : "";
                boolean botInvalid = "BOT_INVALID".equalsIgnoreCase(errText) || "USER_ID_INVALID".equalsIgnoreCase(errText);
                if (botInvalid) {
                    synchronized (cachedBotByAccount) {
                        cachedBotByAccount.remove(account);
                    }
                    if (attempt == 0) {
                        resolveBackupBot(account, freshBot ->
                                invokeCloudMethodWithRetry(account, freshBot, method, params, attempt + 1, onSuccess, onError),
                                onError);
                        return;
                    }
                }
                long retryDelayMs = computeRetryDelayMs(errText, attempt);
                if (retryDelayMs > 0 && attempt + 1 < CLOUD_RETRY_MAX_ATTEMPTS) {
                    Utilities.globalQueue.postRunnable(() ->
                            invokeCloudMethodWithRetry(account, botUser, method, params, attempt + 1, onSuccess, onError),
                            retryDelayMs);
                    return;
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(formatTelegramError(errText));
                });
                return;
            }
            if (response instanceof TLRPC.TL_dataJSON) {
                String data = ((TLRPC.TL_dataJSON) response).data;
                if (onSuccess != null) {
                    onSuccess.run(data != null ? data : "{}");
                }
            } else {
                if (attempt + 1 < CLOUD_RETRY_MAX_ATTEMPTS) {
                    Utilities.globalQueue.postRunnable(() ->
                            invokeCloudMethodWithRetry(account, botUser, method, params, attempt + 1, onSuccess, onError),
                            700L);
                    return;
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run("Respuesta inesperada del servidor de Telegram");
                });
            }
        });
    }

    private static long computeRetryDelayMs(String errText, int attempt) {
        if (TextUtils.isEmpty(errText)) {
            return 600L * (attempt + 1);
        }
        String upper = errText.toUpperCase(Locale.US);
        Matcher m = FLOOD_WAIT_PATTERN.matcher(upper);
        if (m.find()) {
            try {
                int seconds = Integer.parseInt(m.group(1));
                if (seconds >= 0 && seconds <= 6) {
                    return (seconds + 1L) * 1000L;
                }
            } catch (Exception ignore) {}
            return -1L;
        }
        if (upper.contains("TIMEOUT")
                || upper.contains("RPC_CALL_FAIL")
                || upper.contains("INTERNAL")
                || upper.contains("TRY_AGAIN")
                || upper.contains("NETWORK")
                || upper.contains("CONNECTION")) {
            return 600L * (attempt + 1);
        }
        return -1L;
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
            invokeCloudMethodWithRetry(account, botUser, "saveStorageValue", params, 0, ignored ->
                    saveChunksSequentially(account, botUser, chunks, index + 1, onDone, onError), onError);
        } catch (Exception e) {
            FileLog.e(e);
            AndroidUtilities.runOnUIThread(() -> {
                if (onError != null) onError.run("Error al subir bloque " + (index + 1));
            });
        }
    }

    private static void cleanupStaleCloudChunks(int account, TLRPC.User botUser, int currentChunkCount) {
        invokeCloudMethodWithRetry(account, botUser, "getStorageKeys", new JSONObject(), 0, resultJson -> {
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
            invokeCloudMethodWithRetry(account, botUser, "getStorageValues", params, 0, resultJson -> {
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
            invokeCloudMethodWithRetry(account, botUser, "deleteStorageValues", params, 0, ignored ->
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
                    throw new IllegalStateException("La copia descomprimida excede el límite máximo permitido");
                }
                out.write(buf, 0, read);
            }
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static List<String> splitIntoChunksUtf8Safe(String text, int maxChars, int maxUtf8Bytes) {
        List<String> list = new ArrayList<>();
        int len = text.length();
        int i = 0;
        while (i < len) {
            int end = Math.min(len, i + maxChars);
            String chunk = text.substring(i, end);
            while (chunk.getBytes(StandardCharsets.UTF_8).length > maxUtf8Bytes && end > i + 1) {
                end = i + Math.max(1, (end - i) * 3 / 4);
                chunk = text.substring(i, end);
            }
            list.add(chunk);
            i = end;
        }
        return list;
    }

    private static String readStreamUtf8OrGzipBounded(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;
        int read;
        while ((read = is.read(buf)) != -1) {
            total += read;
            if (total > MAX_BACKUP_FILE_BYTES) {
                throw new IllegalStateException("El archivo supera el límite de 8 MB");
            }
            baos.write(buf, 0, read);
        }
        byte[] rawBytes = baos.toByteArray();
        if (rawBytes.length >= 2 && (rawBytes[0] & 0xFF) == 0x1F && (rawBytes[1] & 0xFF) == 0x8B) {
            ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(rawBytes))) {
                int decTotal = 0;
                while ((read = gzip.read(buf)) != -1) {
                    decTotal += read;
                    if (decTotal > MAX_BACKUP_FILE_BYTES) {
                        throw new IllegalStateException("La copia descomprimida supera el límite de 8 MB");
                    }
                    decompressed.write(buf, 0, read);
                }
            }
            return decompressed.toString(StandardCharsets.UTF_8.name());
        }
        return new String(rawBytes, StandardCharsets.UTF_8);
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
