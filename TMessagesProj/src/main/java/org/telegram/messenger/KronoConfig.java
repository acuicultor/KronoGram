package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.text.Spannable;
import android.text.TextUtils;

import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.TextStyleSpan;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KronoConfig {

    private static final String PREFS_NAME = "kronoconfig";
    private static SharedPreferences preferences;
    private static volatile boolean loaded = false;
    private static volatile boolean pendingGlassModeMigrationFromLiteMode = false;
    private static volatile boolean postInitCompleted = false;

    // 1. Ghost Mode & Privacy
    public static boolean ghostMode = false;
    public static boolean dontReadMessages = false;
    public static boolean dontReadStories = false;
    public static boolean dontSendTyping = false;
    public static boolean dontSendOnline = false;
    public static boolean hidePhoneNumber = false;
    public static boolean hideChatListNamesAvatars = false;
    public static boolean blockSecretChats = false;
    public static boolean antiDeleteMessages = false;
    public static boolean antiDeleteExcludeChannels = true;
    public static boolean unlimitedViewOnce = false;
    public static boolean viewOnceAllowSaveAndScreenshots = false;
    public static boolean allowRestrictedContentBypass = true;
    public static boolean zeroTelemetry = false;
    public static boolean blockTelegramTelemetry = false;
    public static boolean disableFirebaseAnalytics = false;
    public static boolean blockSponsoredMessages = false;
    public static boolean stripUrlTrackers = false;
    public static boolean disableFirebasePush = false;
    public static boolean persistentForegroundService = false;

    // 2. Chats, Folders & Files
    public static boolean keepOriginalFilename = false;
    public static boolean deleteForAllByDefault = false;
    public static boolean showTimeWithSeconds = false;
    public static boolean spoilerWordsEnabled = false;
    public static String spoilerWordsRaw = "";
    public static boolean hideAllChatsTab = false;
    public static boolean showFavoritesFolder = true;
    public static boolean unlimitedPinnedChats = false;
    public static boolean quickGoToMessageInSearch = true;
    public static boolean longClickMediaShowsMenu = true;
    public static boolean disablePullToNextChannel = false;
    private static final SparseArray<Set<Long>> localPinnedDialogsByAccount = new SparseArray<>();
    private static final SparseArray<Set<Long>> favoriteDialogsByAccount = new SparseArray<>();

    // Hidden Chats & Folders Vault
    public static final int HIDDEN_LOCK_NONE = 0;
    public static final int HIDDEN_LOCK_PIN = 1;
    public static final int HIDDEN_LOCK_PASSWORD = 2;
    public static int hiddenLockType = HIDDEN_LOCK_NONE;
    public static String hiddenLockHash = "";
    public static boolean hiddenUseBiometric = true;
    public static boolean hiddenMuteNotifications = true;
    public static boolean hiddenHideFolderChatsEverywhere = true;
    public static boolean hiddenAutoLockOnBackground = true;
    private static final SparseArray<Set<Long>> hiddenDialogsByAccount = new SparseArray<>();
    private static final SparseArray<Set<Integer>> hiddenFiltersByAccount = new SparseArray<>();
    private static final SparseArray<ArrayList<Integer>> customFilterOrderByAccount = new SparseArray<>();
    private static final SparseArray<ArrayList<Integer>> lastRemoteFilterOrderByAccount = new SparseArray<>();
    private static volatile boolean hiddenVaultUnlocked = false;

    // Chat Folder Tabs customization
    public static final int TAB_MODE_TEXT = 0;
    public static final int TAB_MODE_ICON_TEXT = 1;
    public static final int TAB_MODE_ICON = 2;
    public static int tabMode = TAB_MODE_TEXT;

    public static final int TAB_STYLE_DEFAULT = 0;
    public static final int TAB_STYLE_ROUNDED = 1;
    public static final int TAB_STYLE_PILL = 2;
    public static final int TAB_STYLE_VKUI = 3;
    public static final int TAB_STYLE_TEXT_ONLY = 4;
    public static int tabStyle = TAB_STYLE_DEFAULT;

    public static boolean tabStyleStroke = false;
    public static boolean hideTabsCounters = false;
    public static boolean folderNameInHeader = false;
    public static boolean disableTabsSwipe = false;
    public static boolean infiniteFolderScroll = true;
    public static boolean autoSendFolderSyncLogs = true;

    // Swipe gesture actions
    public static final int SWIPE_ACTION_REPLY = 0;
    public static final int SWIPE_ACTION_EDIT_OR_REPLY = 1;
    public static final int SWIPE_ACTION_SAVE = 2;
    public static final int SWIPE_ACTION_TRANSLATE = 3;
    public static final int SWIPE_ACTION_FORWARD = 4;
    public static final int SWIPE_ACTION_COPY = 5;
    public static final int SWIPE_ACTION_DISABLED = 6;
    public static int messageSwipeAction = SWIPE_ACTION_REPLY;
    public static boolean swipeToEditOwnMessage = true;

    // 3. Appearance / UI Style
    public static final int UI_STYLE_MATERIAL_YOU = 0;
    public static final int UI_STYLE_IOS = 1;
    public static final int UI_STYLE_TELEGRAM_X = 2;
    public static int uiStyle = UI_STYLE_MATERIAL_YOU;
    public static boolean disableTitleMarquee = false;
    public static boolean smoothSplashAnimation = true;

    // 3b. Glass / Blur Mode & Per-Component Toggles
    public static final int GLASS_MODE_LIQUID = 0;
    public static final int GLASS_MODE_FROSTED = 1;
    public static final int GLASS_MODE_OFF = 2;
    public static int glassMode = GLASS_MODE_LIQUID;
    public static boolean glassOnSettingsBar = true;
    public static boolean glassOnSearchBars = true;
    public static boolean glassOnBottomBar = true;
    public static boolean glassOnChatBars = true;

    // 4. AI, Voice & Translation
    public static boolean freeVoiceTranscription = true;
    public static boolean freeChatTranslation = true;
    public static final int VOICE_TRANSCRIBE_MODE_AUTO = 0;
    public static final int VOICE_TRANSCRIBE_MODE_LOCAL_ONLY = 1;
    public static int voiceTranscriptionMode = VOICE_TRANSCRIBE_MODE_AUTO;
    public static String voiceTranscriptionLanguage = "auto";
    public static final int TRANSLATE_PROVIDER_TELEGRAM = 0;
    public static final int TRANSLATE_PROVIDER_GOOGLE = 1;
    public static final int TRANSLATE_PROVIDER_YANDEX = 3;
    public static final int TRANSLATE_PROVIDER_LINGVA = 4;
    public static int translationProvider = TRANSLATE_PROVIDER_GOOGLE;

    // 5. Cloud & Local Backup
    public static final int CLOUD_BACKUP_MANUAL = 0;
    public static final int CLOUD_BACKUP_DAILY = 1;
    public static final int CLOUD_BACKUP_WEEKLY = 2;
    public static final int CLOUD_BACKUP_ON_CHANGE = 3;
    public static int cloudBackupMode = CLOUD_BACKUP_ON_CHANGE;
    public static boolean cloudAutoIncludeTelegram = true;
    public static boolean cloudAutoIncludeTheme = true;
    public static boolean cloudAutoIncludeNotifications = true;
    public static boolean cloudAutoIncludeVault = true;

    static {
        loadConfig();
    }

    public static synchronized void loadConfig() {
        if (loaded) {
            return;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ghostMode = preferences.getBoolean("ghostMode", false);
        dontReadMessages = preferences.getBoolean("dontReadMessages", false);
        dontReadStories = preferences.getBoolean("dontReadStories", false);
        dontSendTyping = preferences.getBoolean("dontSendTyping", false);
        dontSendOnline = preferences.getBoolean("dontSendOnline", false);
        hidePhoneNumber = preferences.getBoolean("hidePhoneNumber", false);
        hideChatListNamesAvatars = preferences.getBoolean("hideChatListNamesAvatars", false);
        blockSecretChats = preferences.getBoolean("blockSecretChats", false);
        antiDeleteMessages = preferences.getBoolean("antiDeleteMessages", false);
        antiDeleteExcludeChannels = preferences.getBoolean("antiDeleteExcludeChannels", true);
        unlimitedViewOnce = preferences.getBoolean("unlimitedViewOnce", false);
        viewOnceAllowSaveAndScreenshots = preferences.getBoolean("viewOnceAllowSaveAndScreenshots", false);
        allowRestrictedContentBypass = preferences.getBoolean("allowRestrictedContentBypass", true);
        zeroTelemetry = preferences.getBoolean("zeroTelemetry", false);
        blockTelegramTelemetry = preferences.getBoolean("blockTelegramTelemetry", false);
        disableFirebaseAnalytics = preferences.getBoolean("disableFirebaseAnalytics", false);
        blockSponsoredMessages = preferences.getBoolean("blockSponsoredMessages", false);
        stripUrlTrackers = preferences.getBoolean("stripUrlTrackers", false);
        disableFirebasePush = preferences.getBoolean("disableFirebasePush", false);
        persistentForegroundService = preferences.getBoolean("persistentForegroundService", false);

        keepOriginalFilename = preferences.getBoolean("keepOriginalFilename", false);
        deleteForAllByDefault = preferences.getBoolean("deleteForAllByDefault", false);
        showTimeWithSeconds = preferences.getBoolean("showTimeWithSeconds", false);
        spoilerWordsEnabled = preferences.getBoolean("spoilerWordsEnabled", false);
        spoilerWordsRaw = preferences.getString("spoilerWordsRaw", "");
        hideAllChatsTab = preferences.getBoolean("hideAllChatsTab", false);
        showFavoritesFolder = preferences.getBoolean("showFavoritesFolder", true);
        unlimitedPinnedChats = preferences.getBoolean("unlimitedPinnedChats", false);
        quickGoToMessageInSearch = preferences.getBoolean("quickGoToMessageInSearch", true);
        longClickMediaShowsMenu = preferences.getBoolean("longClickMediaShowsMenu", true);
        disablePullToNextChannel = preferences.getBoolean("disablePullToNextChannel", false);
        tabMode = preferences.getInt("tabMode", TAB_MODE_TEXT);
        tabStyle = preferences.getInt("tabStyle", TAB_STYLE_DEFAULT);
        tabStyleStroke = preferences.getBoolean("tabStyleStroke", false);
        hideTabsCounters = preferences.getBoolean("hideTabsCounters", false);
        folderNameInHeader = preferences.getBoolean("folderNameInHeader", false);
        disableTabsSwipe = preferences.getBoolean("disableTabsSwipe", false);
        infiniteFolderScroll = preferences.getBoolean("infiniteFolderScroll", true);
        autoSendFolderSyncLogs = preferences.getBoolean("autoSendFolderSyncLogs", true);

        localPinnedDialogsByAccount.clear();
        favoriteDialogsByAccount.clear();

        uiStyle = preferences.getInt("uiStyle", UI_STYLE_MATERIAL_YOU);
        disableTitleMarquee = preferences.getBoolean("disableTitleMarquee", false);
        smoothSplashAnimation = preferences.getBoolean("smoothSplashAnimation", true);
        if (preferences.contains("glassMode")) {
            glassMode = preferences.getInt("glassMode", GLASS_MODE_LIQUID);
            pendingGlassModeMigrationFromLiteMode = false;
        } else {
            glassMode = GLASS_MODE_LIQUID;
            pendingGlassModeMigrationFromLiteMode = true;
        }
        if (glassMode < GLASS_MODE_LIQUID || glassMode > GLASS_MODE_OFF) {
            glassMode = GLASS_MODE_LIQUID;
        }
        glassOnSettingsBar = preferences.getBoolean("glassOnSettingsBar", true);
        glassOnSearchBars = preferences.getBoolean("glassOnSearchBars", true);
        glassOnBottomBar = preferences.getBoolean("glassOnBottomBar", true);
        glassOnChatBars = preferences.getBoolean("glassOnChatBars", true);
        freeVoiceTranscription = preferences.getBoolean("freeVoiceTranscription", true);
        freeChatTranslation = preferences.getBoolean("freeChatTranslation", true);
        voiceTranscriptionMode = preferences.getInt("voiceTranscriptionMode", VOICE_TRANSCRIBE_MODE_AUTO);
        voiceTranscriptionLanguage = preferences.getString("voiceTranscriptionLanguage", "auto");
        if (TextUtils.isEmpty(voiceTranscriptionLanguage)) {
            voiceTranscriptionLanguage = "auto";
        }
        translationProvider = preferences.getInt("translationProvider", TRANSLATE_PROVIDER_GOOGLE);
        if (translationProvider == 2 || (translationProvider != TRANSLATE_PROVIDER_TELEGRAM
                && translationProvider != TRANSLATE_PROVIDER_GOOGLE
                && translationProvider != TRANSLATE_PROVIDER_YANDEX
                && translationProvider != TRANSLATE_PROVIDER_LINGVA)) {
            translationProvider = TRANSLATE_PROVIDER_GOOGLE;
            preferences.edit().putInt("translationProvider", translationProvider).apply();
        }
        messageSwipeAction = preferences.getInt("messageSwipeAction", SWIPE_ACTION_REPLY);
        swipeToEditOwnMessage = preferences.getBoolean("swipeToEditOwnMessage", true);

        hiddenLockType = preferences.getInt("hiddenLockType", HIDDEN_LOCK_NONE);
        hiddenLockHash = preferences.getString("hiddenLockHash", "");
        hiddenUseBiometric = preferences.getBoolean("hiddenUseBiometric", true);
        hiddenMuteNotifications = preferences.getBoolean("hiddenMuteNotifications", true);
        hiddenHideFolderChatsEverywhere = preferences.getBoolean("hiddenHideFolderChatsEverywhere", true);
        hiddenAutoLockOnBackground = preferences.getBoolean("hiddenAutoLockOnBackground", true);
        hiddenDialogsByAccount.clear();
        hiddenFiltersByAccount.clear();
        favoriteDialogsByAccount.clear();
        localPinnedDialogsByAccount.clear();
        customFilterOrderByAccount.clear();
        lastRemoteFilterOrderByAccount.clear();

        cloudBackupMode = preferences.getInt("cloudBackupMode", CLOUD_BACKUP_ON_CHANGE);
        if (cloudBackupMode != CLOUD_BACKUP_MANUAL
                && cloudBackupMode != CLOUD_BACKUP_DAILY
                && cloudBackupMode != CLOUD_BACKUP_WEEKLY
                && cloudBackupMode != CLOUD_BACKUP_ON_CHANGE) {
            cloudBackupMode = CLOUD_BACKUP_ON_CHANGE;
        }
        cloudAutoIncludeTelegram = preferences.getBoolean("cloudAutoIncludeTelegram", true);
        cloudAutoIncludeTheme = preferences.getBoolean("cloudAutoIncludeTheme", true);
        cloudAutoIncludeNotifications = preferences.getBoolean("cloudAutoIncludeNotifications", true);
        cloudAutoIncludeVault = preferences.getBoolean("cloudAutoIncludeVault", true);
        loaded = true;
    }

    public static synchronized void reloadConfigFromDisk() {
        loaded = false;
        preferences = null;
        loadConfig();
        if (postInitCompleted) {
            syncInitialGlassModeIfNeeded();
            if (preferences != null && preferences.contains("glassMode")) {
                LiteMode.setGlassFlagsFromKrono(glassMode != GLASS_MODE_OFF, glassMode == GLASS_MODE_LIQUID);
            }
            org.telegram.utils.glass.GlassEngine.refreshFromSettings();
        }
    }

    public static synchronized void resetKronoDefaults(boolean preserveVault) {
        loadConfig();
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        KronoBackupManager.setSuppressObserver(true);
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int savedLockType = hiddenLockType;
            String savedLockHash = hiddenLockHash;
            boolean savedUseBio = hiddenUseBiometric;
            boolean savedMuteNotif = hiddenMuteNotifications;
            boolean savedHideFolderChats = hiddenHideFolderChatsEverywhere;
            boolean savedAutoLock = hiddenAutoLockOnBackground;
            java.util.Map<String, ?> all = prefs.getAll();
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            if (preserveVault) {
                editor.putInt("hiddenLockType", savedLockType);
                editor.putString("hiddenLockHash", savedLockHash != null ? savedLockHash : "");
                editor.putBoolean("hiddenUseBiometric", savedUseBio);
                editor.putBoolean("hiddenMuteNotifications", savedMuteNotif);
                editor.putBoolean("hiddenHideFolderChatsEverywhere", savedHideFolderChats);
                editor.putBoolean("hiddenAutoLockOnBackground", savedAutoLock);
                if (all != null) {
                    for (java.util.Map.Entry<String, ?> entry : all.entrySet()) {
                        String k = entry.getKey();
                        if (k != null && (k.startsWith("hiddenDialogs_") || k.startsWith("hiddenFilters_")) && entry.getValue() instanceof String) {
                            editor.putString(k, (String) entry.getValue());
                        }
                    }
                }
            }
            editor.commit();
            reloadConfigFromDisk();
        } finally {
            KronoBackupManager.releaseSuppressObserverAfterMainDrain();
        }
    }

    public static synchronized void setCloudBackupMode(int mode) {
        loadConfig();
        if (mode != CLOUD_BACKUP_MANUAL
                && mode != CLOUD_BACKUP_DAILY
                && mode != CLOUD_BACKUP_WEEKLY
                && mode != CLOUD_BACKUP_ON_CHANGE) {
            mode = CLOUD_BACKUP_ON_CHANGE;
        }
        cloudBackupMode = mode;
        saveConfig();
    }

    public static String getCloudBackupModeName() {
        loadConfig();
        switch (cloudBackupMode) {
            case CLOUD_BACKUP_ON_CHANGE:
                return "Automática (al detectar cambios)";
            case CLOUD_BACKUP_DAILY:
                return "Periódica (diaria)";
            case CLOUD_BACKUP_WEEKLY:
                return "Periódica (semanal)";
            case CLOUD_BACKUP_MANUAL:
            default:
                return "Manual";
        }
    }

    public static synchronized void setCloudAutoBackupScope(boolean includeTelegram, boolean includeTheme, boolean includeNotifications, boolean includeVault) {
        loadConfig();
        cloudAutoIncludeTelegram = includeTelegram;
        cloudAutoIncludeTheme = includeTheme;
        cloudAutoIncludeNotifications = includeNotifications;
        cloudAutoIncludeVault = includeVault;
        saveConfig();
    }

    public static synchronized long getCloudLastBackupTime(int account) {
        loadConfig();
        if (preferences == null) {
            return 0L;
        }
        String key = "cloudLastBackupTime_" + account;
        if (preferences.contains(key)) {
            return preferences.getLong(key, 0L);
        }
        return account == 0 ? preferences.getLong("cloudLastBackupTime", 0L) : 0L;
    }

    public static synchronized String getCloudLastBackupHash(int account) {
        loadConfig();
        if (preferences == null) {
            return "";
        }
        String key = "cloudLastBackupHash_" + account;
        if (preferences.contains(key)) {
            return preferences.getString(key, "");
        }
        return account == 0 ? preferences.getString("cloudLastBackupHash", "") : "";
    }

    public static synchronized boolean isCloudBackupPending(int account) {
        loadConfig();
        if (preferences == null) {
            return false;
        }
        String key = "cloudBackupPending_" + account;
        if (preferences.contains(key)) {
            return preferences.getBoolean(key, false);
        }
        return account == 0 && preferences.getBoolean("cloudBackupPending", false);
    }

    public static synchronized void setCloudBackupPending(int account, boolean pending) {
        loadConfig();
        if (preferences == null) {
            return;
        }
        String key = "cloudBackupPending_" + account;
        if (preferences.contains(key) && preferences.getBoolean(key, false) == pending) {
            return;
        }
        preferences.edit().putBoolean(key, pending).apply();
    }

    public static synchronized void recordCloudBackupCompleted(int account, long timestampMs, String sha256) {
        loadConfig();
        String safeHash = sha256 != null ? sha256 : "";
        if (preferences != null) {
            preferences.edit()
                    .putLong("cloudLastBackupTime_" + account, timestampMs)
                    .putString("cloudLastBackupHash_" + account, safeHash)
                    .putBoolean("cloudBackupPending_" + account, false)
                    .commit();
        }
    }

    public static synchronized void saveConfig() {
        if (!loaded) {
            loadConfig();
            if (!loaded) {
                return;
            }
        }
        if (preferences == null && ApplicationLoader.applicationContext != null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putBoolean("ghostMode", ghostMode)
                .putBoolean("dontReadMessages", dontReadMessages)
                .putBoolean("dontReadStories", dontReadStories)
                .putBoolean("dontSendTyping", dontSendTyping)
                .putBoolean("dontSendOnline", dontSendOnline)
                .putBoolean("hidePhoneNumber", hidePhoneNumber)
                .putBoolean("hideChatListNamesAvatars", hideChatListNamesAvatars)
                .putBoolean("blockSecretChats", blockSecretChats)
                .putBoolean("antiDeleteMessages", antiDeleteMessages)
                .putBoolean("antiDeleteExcludeChannels", antiDeleteExcludeChannels)
                .putBoolean("unlimitedViewOnce", unlimitedViewOnce)
                .putBoolean("viewOnceAllowSaveAndScreenshots", viewOnceAllowSaveAndScreenshots)
                .putBoolean("allowRestrictedContentBypass", allowRestrictedContentBypass)
                .putBoolean("zeroTelemetry", zeroTelemetry)
                .putBoolean("blockTelegramTelemetry", blockTelegramTelemetry)
                .putBoolean("disableFirebaseAnalytics", disableFirebaseAnalytics)
                .putBoolean("blockSponsoredMessages", blockSponsoredMessages)
                .putBoolean("stripUrlTrackers", stripUrlTrackers)
                .putBoolean("disableFirebasePush", disableFirebasePush)
                .putBoolean("persistentForegroundService", persistentForegroundService)
                .putBoolean("keepOriginalFilename", keepOriginalFilename)
                .putBoolean("deleteForAllByDefault", deleteForAllByDefault)
                .putBoolean("showTimeWithSeconds", showTimeWithSeconds)
                .putBoolean("spoilerWordsEnabled", spoilerWordsEnabled)
                .putString("spoilerWordsRaw", spoilerWordsRaw)
                .putBoolean("hideAllChatsTab", hideAllChatsTab)
                .putBoolean("showFavoritesFolder", showFavoritesFolder)
                .putBoolean("unlimitedPinnedChats", unlimitedPinnedChats)
                .putBoolean("quickGoToMessageInSearch", quickGoToMessageInSearch)
                .putBoolean("longClickMediaShowsMenu", longClickMediaShowsMenu)
                .putBoolean("disablePullToNextChannel", disablePullToNextChannel)
                .putInt("tabMode", tabMode)
                .putInt("tabStyle", tabStyle)
                .putBoolean("tabStyleStroke", tabStyleStroke)
                .putBoolean("hideTabsCounters", hideTabsCounters)
                .putBoolean("folderNameInHeader", folderNameInHeader)
                .putBoolean("disableTabsSwipe", disableTabsSwipe)
                .putBoolean("infiniteFolderScroll", infiniteFolderScroll)
                .putBoolean("autoSendFolderSyncLogs", autoSendFolderSyncLogs)
                .putInt("uiStyle", uiStyle)
                .putBoolean("disableTitleMarquee", disableTitleMarquee)
                .putBoolean("smoothSplashAnimation", smoothSplashAnimation)
                .putInt("glassMode", glassMode)
                .putBoolean("glassOnSettingsBar", glassOnSettingsBar)
                .putBoolean("glassOnSearchBars", glassOnSearchBars)
                .putBoolean("glassOnBottomBar", glassOnBottomBar)
                .putBoolean("glassOnChatBars", glassOnChatBars)
                .putBoolean("freeVoiceTranscription", freeVoiceTranscription)
                .putBoolean("freeChatTranslation", freeChatTranslation)
                .putInt("voiceTranscriptionMode", voiceTranscriptionMode)
                .putString("voiceTranscriptionLanguage", voiceTranscriptionLanguage)
                .putInt("translationProvider", translationProvider)
                .putInt("messageSwipeAction", messageSwipeAction)
                .putBoolean("swipeToEditOwnMessage", swipeToEditOwnMessage)
                .putInt("hiddenLockType", hiddenLockType)
                .putString("hiddenLockHash", hiddenLockHash != null ? hiddenLockHash : "")
                .putBoolean("hiddenUseBiometric", hiddenUseBiometric)
                .putBoolean("hiddenMuteNotifications", hiddenMuteNotifications)
                .putBoolean("hiddenHideFolderChatsEverywhere", hiddenHideFolderChatsEverywhere)
                .putBoolean("hiddenAutoLockOnBackground", hiddenAutoLockOnBackground)
                .putInt("cloudBackupMode", cloudBackupMode)
                .putBoolean("cloudAutoIncludeTelegram", cloudAutoIncludeTelegram)
                .putBoolean("cloudAutoIncludeTheme", cloudAutoIncludeTheme)
                .putBoolean("cloudAutoIncludeNotifications", cloudAutoIncludeNotifications)
                .putBoolean("cloudAutoIncludeVault", cloudAutoIncludeVault)
                .apply();
    }

    public static synchronized void setGhostMode(boolean enabled) {
        loadConfig();
        if (enabled) {
            if (!ghostMode && preferences != null) {
                preferences.edit()
                        .putBoolean("ghostPrev_hasSnapshot", true)
                        .putBoolean("ghostPrev_dontReadMessages", dontReadMessages)
                        .putBoolean("ghostPrev_dontReadStories", dontReadStories)
                        .putBoolean("ghostPrev_dontSendTyping", dontSendTyping)
                        .putBoolean("ghostPrev_dontSendOnline", dontSendOnline)
                        .apply();
            }
            ghostMode = true;
            dontReadMessages = true;
            dontReadStories = true;
            dontSendTyping = true;
            dontSendOnline = true;
        } else {
            boolean restored = false;
            if (preferences != null && preferences.getBoolean("ghostPrev_hasSnapshot", false)) {
                boolean prevMsg = preferences.getBoolean("ghostPrev_dontReadMessages", false);
                boolean prevStory = preferences.getBoolean("ghostPrev_dontReadStories", false);
                boolean prevTyping = preferences.getBoolean("ghostPrev_dontSendTyping", false);
                boolean prevOnline = preferences.getBoolean("ghostPrev_dontSendOnline", false);
                preferences.edit().remove("ghostPrev_hasSnapshot").apply();
                if (!(prevMsg && prevStory && prevTyping && prevOnline)) {
                    dontReadMessages = prevMsg;
                    dontReadStories = prevStory;
                    dontSendTyping = prevTyping;
                    dontSendOnline = prevOnline;
                    restored = true;
                }
            }
            ghostMode = false;
            if (!restored) {
                dontReadMessages = false;
                dontReadStories = false;
                dontSendTyping = false;
                dontSendOnline = false;
            }
        }
        saveConfig();
    }

    public static synchronized void toggleHideAllChatsTab() {
        loadConfig();
        hideAllChatsTab = !hideAllChatsTab;
        saveConfig();
    }

    public static synchronized void toggleShowFavoritesFolder() {
        loadConfig();
        showFavoritesFolder = !showFavoritesFolder;
        saveConfig();
    }

    public static synchronized void setAutoSendFolderSyncLogs(boolean value) {
        loadConfig();
        autoSendFolderSyncLogs = value;
        saveConfig();
    }

    public static synchronized void setDontReadMessages(boolean value) {
        loadConfig();
        dontReadMessages = value;
        syncGhostMasterState();
    }

    public static synchronized void setDontReadStories(boolean value) {
        loadConfig();
        dontReadStories = value;
        syncGhostMasterState();
    }

    public static synchronized void setDontSendTyping(boolean value) {
        loadConfig();
        dontSendTyping = value;
        syncGhostMasterState();
    }

    public static synchronized void setDontSendOnline(boolean value) {
        loadConfig();
        dontSendOnline = value;
        syncGhostMasterState();
    }

    public static synchronized void setHiddenUseBiometric(boolean value) {
        loadConfig();
        hiddenUseBiometric = value;
        saveConfig();
    }

    public static synchronized void setHiddenMuteNotifications(boolean value) {
        loadConfig();
        hiddenMuteNotifications = value;
        saveConfig();
    }

    public static synchronized void setHiddenHideFolderChatsEverywhere(boolean value) {
        loadConfig();
        hiddenHideFolderChatsEverywhere = value;
        saveConfig();
    }

    public static synchronized void setHiddenAutoLockOnBackground(boolean value) {
        loadConfig();
        hiddenAutoLockOnBackground = value;
        saveConfig();
    }

    public static synchronized void setHidePhoneNumber(boolean value) {
        loadConfig();
        hidePhoneNumber = value;
        saveConfig();
    }

    public static synchronized void setHideChatListNamesAvatars(boolean value) {
        loadConfig();
        hideChatListNamesAvatars = value;
        saveConfig();
    }

    public static synchronized void setBlockSecretChats(boolean value) {
        loadConfig();
        blockSecretChats = value;
        saveConfig();
    }

    public static synchronized void setAntiDeleteMessages(boolean value) {
        loadConfig();
        antiDeleteMessages = value;
        saveConfig();
    }

    public static synchronized void setAntiDeleteExcludeChannels(boolean value) {
        loadConfig();
        antiDeleteExcludeChannels = value;
        saveConfig();
    }

    public static synchronized void setUnlimitedViewOnce(boolean value) {
        loadConfig();
        unlimitedViewOnce = value;
        saveConfig();
    }

    public static synchronized void setViewOnceAllowSaveAndScreenshots(boolean value) {
        loadConfig();
        viewOnceAllowSaveAndScreenshots = value;
        saveConfig();
    }

    public static synchronized void setAllowRestrictedContentBypass(boolean value) {
        loadConfig();
        allowRestrictedContentBypass = value;
        saveConfig();
    }

    public static synchronized void setBlockTelegramTelemetry(boolean value) {
        loadConfig();
        blockTelegramTelemetry = value;
        syncZeroTelemetryMasterState();
    }

    public static synchronized void setDisableFirebaseAnalytics(boolean value) {
        loadConfig();
        disableFirebaseAnalytics = value;
        syncZeroTelemetryMasterState();
    }

    public static synchronized void setBlockSponsoredMessages(boolean value) {
        loadConfig();
        blockSponsoredMessages = value;
        syncZeroTelemetryMasterState();
    }

    public static synchronized void setStripUrlTrackers(boolean value) {
        loadConfig();
        stripUrlTrackers = value;
        syncZeroTelemetryMasterState();
    }

    public static synchronized void setDisableFirebasePush(boolean value) {
        loadConfig();
        disableFirebasePush = value;
        saveConfig();
    }

    public static synchronized void setPersistentForegroundService(boolean value) {
        loadConfig();
        persistentForegroundService = value;
        saveConfig();
    }

    public static synchronized void setUiStyle(int style) {
        loadConfig();
        uiStyle = style;
        saveConfig();
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void setDisableTitleMarquee(boolean value) {
        loadConfig();
        disableTitleMarquee = value;
        saveConfig();
    }

    public static synchronized void setSmoothSplashAnimation(boolean value) {
        loadConfig();
        smoothSplashAnimation = value;
        saveConfig();
    }

    public static synchronized void setGlassMode(int mode) {
        loadConfig();
        if (mode < GLASS_MODE_LIQUID || mode > GLASS_MODE_OFF) {
            mode = GLASS_MODE_LIQUID;
        }
        glassMode = mode;
        pendingGlassModeMigrationFromLiteMode = false;
        saveConfig();
        LiteMode.setGlassFlagsFromKrono(mode != GLASS_MODE_OFF, mode == GLASS_MODE_LIQUID);
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void syncInitialGlassModeIfNeeded() {
        postInitCompleted = true;
        loadConfig();
        if (!pendingGlassModeMigrationFromLiteMode || preferences == null) {
            return;
        }
        pendingGlassModeMigrationFromLiteMode = false;
        if (preferences.contains("glassMode")) {
            return;
        }
        int oldMode = glassMode;
        boolean hasBlur = LiteMode.isEnabled(LiteMode.FLAG_CHAT_BLUR);
        boolean hasLiquid = LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS);
        glassMode = !hasBlur ? GLASS_MODE_OFF : (hasLiquid ? GLASS_MODE_LIQUID : GLASS_MODE_FROSTED);
        if (glassMode < GLASS_MODE_LIQUID || glassMode > GLASS_MODE_OFF) {
            glassMode = GLASS_MODE_LIQUID;
        }
        preferences.edit().putInt("glassMode", glassMode).commit();
        if (glassMode != oldMode) {
            org.telegram.utils.glass.GlassEngine.refreshFromSettings();
        }
    }

    public static synchronized void syncGlassModeFromLiteMode() {
        loadConfig();
        pendingGlassModeMigrationFromLiteMode = false;
        boolean blur = LiteMode.isEnabled(LiteMode.FLAG_CHAT_BLUR);
        boolean liquid = LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS);
        int derived = !blur ? GLASS_MODE_OFF : (liquid ? GLASS_MODE_LIQUID : GLASS_MODE_FROSTED);
        if (glassMode != derived) {
            glassMode = derived;
            saveConfig();
        }
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void setGlassOnSettingsBar(boolean value) {
        loadConfig();
        glassOnSettingsBar = value;
        saveConfig();
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void setGlassOnSearchBars(boolean value) {
        loadConfig();
        glassOnSearchBars = value;
        saveConfig();
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void setGlassOnBottomBar(boolean value) {
        loadConfig();
        glassOnBottomBar = value;
        saveConfig();
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void setGlassOnChatBars(boolean value) {
        loadConfig();
        glassOnChatBars = value;
        saveConfig();
        org.telegram.utils.glass.GlassEngine.refreshFromSettings();
    }

    public static synchronized void setTabMode(int mode) {
        loadConfig();
        tabMode = mode;
        saveConfig();
    }

    public static synchronized void setTabStyle(int style) {
        loadConfig();
        tabStyle = style;
        saveConfig();
    }

    public static synchronized void setTabStyleStroke(boolean value) {
        loadConfig();
        tabStyleStroke = value;
        saveConfig();
    }

    public static synchronized void setHideTabsCounters(boolean value) {
        loadConfig();
        hideTabsCounters = value;
        saveConfig();
    }

    public static synchronized void setFolderNameInHeader(boolean value) {
        loadConfig();
        folderNameInHeader = value;
        saveConfig();
    }

    public static synchronized void setDisableTabsSwipe(boolean value) {
        loadConfig();
        disableTabsSwipe = value;
        saveConfig();
    }

    public static synchronized void setInfiniteFolderScroll(boolean value) {
        loadConfig();
        infiniteFolderScroll = value;
        saveConfig();
    }

    public static synchronized void setMessageSwipeAction(int action) {
        loadConfig();
        messageSwipeAction = action;
        saveConfig();
    }

    public static synchronized void setSwipeToEditOwnMessage(boolean value) {
        loadConfig();
        swipeToEditOwnMessage = value;
        saveConfig();
    }

    public static synchronized void setDeleteForAllByDefault(boolean value) {
        loadConfig();
        deleteForAllByDefault = value;
        saveConfig();
    }

    public static synchronized void setShowTimeWithSeconds(boolean value) {
        loadConfig();
        showTimeWithSeconds = value;
        saveConfig();
    }

    public static synchronized void setUnlimitedPinnedChats(boolean value) {
        loadConfig();
        unlimitedPinnedChats = value;
        saveConfig();
    }

    public static synchronized void setQuickGoToMessageInSearch(boolean value) {
        loadConfig();
        quickGoToMessageInSearch = value;
        saveConfig();
    }

    public static synchronized void setLongClickMediaShowsMenu(boolean value) {
        loadConfig();
        longClickMediaShowsMenu = value;
        saveConfig();
    }

    public static synchronized void setDisablePullToNextChannel(boolean value) {
        loadConfig();
        disablePullToNextChannel = value;
        saveConfig();
    }

    public static synchronized void setSpoilerWordsEnabled(boolean value) {
        loadConfig();
        spoilerWordsEnabled = value;
        saveConfig();
    }

    public static synchronized void setSpoilerWordsRaw(String raw) {
        loadConfig();
        spoilerWordsRaw = raw != null ? raw : "";
        saveConfig();
    }

    public static synchronized void setKeepOriginalFilename(boolean value) {
        loadConfig();
        keepOriginalFilename = value;
        saveConfig();
    }

    public static synchronized void setFreeVoiceTranscription(boolean value) {
        loadConfig();
        freeVoiceTranscription = value;
        saveConfig();
    }

    public static synchronized void setVoiceTranscriptionMode(int mode) {
        loadConfig();
        voiceTranscriptionMode = mode;
        saveConfig();
    }

    public static synchronized void setVoiceTranscriptionLanguage(String lang) {
        loadConfig();
        voiceTranscriptionLanguage = TextUtils.isEmpty(lang) ? "auto" : lang;
        saveConfig();
    }

    public static synchronized void setFreeChatTranslation(boolean value) {
        loadConfig();
        freeChatTranslation = value;
        saveConfig();
    }

    public static synchronized void setTranslationProvider(int provider) {
        loadConfig();
        if (provider != TRANSLATE_PROVIDER_TELEGRAM
                && provider != TRANSLATE_PROVIDER_GOOGLE
                && provider != TRANSLATE_PROVIDER_YANDEX
                && provider != TRANSLATE_PROVIDER_LINGVA) {
            provider = TRANSLATE_PROVIDER_GOOGLE;
        }
        translationProvider = provider;
        saveConfig();
    }

    private static Set<Long> getOrLoadFavoriteDialogsLocked(int account) {
        loadConfig();
        Set<Long> set = favoriteDialogsByAccount.get(account);
        if (set != null) {
            return set;
        }
        set = new HashSet<>();
        if (preferences != null) {
            String key = "favoriteDialogs_" + account;
            String raw;
            if (preferences.contains(key)) {
                raw = preferences.getString(key, "");
            } else if (account == 0) {
                raw = preferences.getString("favoriteDialogs", "");
            } else {
                raw = "";
            }
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    try {
                        set.add(Long.parseLong(p.trim()));
                    } catch (Exception ignore) {}
                }
            }
        }
        favoriteDialogsByAccount.put(account, set);
        return set;
    }

    private static void saveFavoriteDialogsLocked(int account, Set<Long> set) {
        if (preferences == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Long did : set) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(did);
        }
        SharedPreferences.Editor editor = preferences.edit().putString("favoriteDialogs_" + account, sb.toString());
        if (account == 0) {
            editor.putString("favoriteDialogs", sb.toString());
        }
        editor.apply();
    }

    public static synchronized boolean isFavoriteDialog(int account, long dialogId) {
        return getOrLoadFavoriteDialogsLocked(account).contains(dialogId);
    }

    public static synchronized boolean isFavoriteDialog(long dialogId) {
        return isFavoriteDialog(UserConfig.selectedAccount, dialogId);
    }

    public static synchronized void setFavoriteDialog(int account, long dialogId, boolean favorite) {
        Set<Long> set = getOrLoadFavoriteDialogsLocked(account);
        boolean changed = favorite ? set.add(dialogId) : set.remove(dialogId);
        if (changed) {
            saveFavoriteDialogsLocked(account, set);
        }
    }

    public static synchronized void setFavoriteDialog(long dialogId, boolean favorite) {
        setFavoriteDialog(UserConfig.selectedAccount, dialogId, favorite);
    }

    public static synchronized boolean toggleFavoriteDialog(int account, long dialogId) {
        Set<Long> set = getOrLoadFavoriteDialogsLocked(account);
        boolean nowFavorite = !set.contains(dialogId);
        setFavoriteDialog(account, dialogId, nowFavorite);
        return nowFavorite;
    }

    public static synchronized boolean toggleFavoriteDialog(long dialogId) {
        return toggleFavoriteDialog(UserConfig.selectedAccount, dialogId);
    }

    public static synchronized Set<Long> getFavoriteDialogs(int account) {
        return new HashSet<>(getOrLoadFavoriteDialogsLocked(account));
    }

    public static synchronized Set<Long> getFavoriteDialogs() {
        return getFavoriteDialogs(UserConfig.selectedAccount);
    }

    // ==================== Hidden Chats & Folders Vault ====================

    public static boolean isHiddenVaultUnlocked() {
        return hiddenVaultUnlocked;
    }

    public static void setHiddenVaultUnlocked(boolean unlocked) {
        hiddenVaultUnlocked = unlocked;
    }

    public static boolean hasHiddenPasscode() {
        loadConfig();
        return hiddenLockType != HIDDEN_LOCK_NONE && !TextUtils.isEmpty(hiddenLockHash);
    }

    public static synchronized void setHiddenPasscode(int type, String rawSecret) {
        loadConfig();
        if (type == HIDDEN_LOCK_NONE || TextUtils.isEmpty(rawSecret)) {
            hiddenLockType = HIDDEN_LOCK_NONE;
            hiddenLockHash = "";
            saveConfig();
            return;
        }
        byte[] salt = new byte[16];
        Utilities.random.nextBytes(salt);
        String saltHex = Utilities.bytesToHex(salt);
        String digestHex = sha256Hex(saltHex + ":" + rawSecret);
        if (!TextUtils.isEmpty(digestHex)) {
            hiddenLockType = type;
            hiddenLockHash = saltHex + ":" + digestHex;
            saveConfig();
        }
    }

    public static synchronized void clearHiddenPasscode() {
        loadConfig();
        hiddenLockType = HIDDEN_LOCK_NONE;
        hiddenLockHash = "";
        saveConfig();
    }

    public static synchronized boolean verifyHiddenPasscode(String input) {
        loadConfig();
        if (!hasHiddenPasscode() || TextUtils.isEmpty(input)) {
            return false;
        }
        int sep = hiddenLockHash.indexOf(':');
        if (sep <= 0 || sep >= hiddenLockHash.length() - 1) {
            return false;
        }
        String saltHex = hiddenLockHash.substring(0, sep);
        String expectedHex = hiddenLockHash.substring(sep + 1);
        String actualHex = sha256Hex(saltHex + ":" + input);
        if (actualHex == null || actualHex.length() != expectedHex.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < actualHex.length(); i++) {
            diff |= actualHex.charAt(i) ^ expectedHex.charAt(i);
        }
        return diff == 0;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Utilities.bytesToHex(bytes);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    public static String getHiddenLockSummary(boolean deviceHasBiometrics) {
        loadConfig();
        boolean bio = hiddenUseBiometric && deviceHasBiometrics;
        if (hiddenLockType == HIDDEN_LOCK_PIN) {
            return bio ? "PIN y Biometría" : "PIN numérico";
        } else if (hiddenLockType == HIDDEN_LOCK_PASSWORD) {
            return bio ? "Contraseña y Biometría" : "Contraseña";
        } else if (bio) {
            return "Solo Biometría";
        }
        return "Sin configurar";
    }

    private static Set<Long> getOrLoadHiddenDialogsLocked(int account) {
        loadConfig();
        Set<Long> set = hiddenDialogsByAccount.get(account);
        if (set != null) {
            return set;
        }
        set = new HashSet<>();
        if (preferences != null) {
            String raw = preferences.getString("hiddenDialogs_" + account, "");
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    try {
                        set.add(Long.parseLong(p.trim()));
                    } catch (Exception ignore) {}
                }
            }
        }
        hiddenDialogsByAccount.put(account, set);
        return set;
    }

    private static void saveHiddenDialogsLocked(int account, Set<Long> set) {
        if (preferences == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Long did : set) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(did);
        }
        preferences.edit().putString("hiddenDialogs_" + account, sb.toString()).apply();
    }

    private static Set<Integer> getOrLoadHiddenFiltersLocked(int account) {
        loadConfig();
        Set<Integer> set = hiddenFiltersByAccount.get(account);
        if (set != null) {
            return set;
        }
        set = new HashSet<>();
        if (preferences != null) {
            String raw = preferences.getString("hiddenFilters_" + account, "");
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    try {
                        set.add(Integer.parseInt(p.trim()));
                    } catch (Exception ignore) {}
                }
            }
        }
        hiddenFiltersByAccount.put(account, set);
        return set;
    }

    private static void saveHiddenFiltersLocked(int account, Set<Integer> set) {
        if (preferences == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer fid : set) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(fid);
        }
        preferences.edit().putString("hiddenFilters_" + account, sb.toString()).apply();
    }

    public static synchronized boolean isDialogHiddenDirectly(int account, long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        return getOrLoadHiddenDialogsLocked(account).contains(dialogId);
    }

    public static synchronized void setDialogHidden(int account, long dialogId, boolean hidden) {
        if (dialogId == 0) {
            return;
        }
        Set<Long> set = getOrLoadHiddenDialogsLocked(account);
        boolean changed = hidden ? set.add(dialogId) : set.remove(dialogId);
        if (changed) {
            saveHiddenDialogsLocked(account, set);
        }
    }

    public static synchronized List<Long> getHiddenDialogs(int account) {
        return new ArrayList<>(getOrLoadHiddenDialogsLocked(account));
    }

    public static synchronized boolean isFilterHidden(int account, int filterId) {
        return getOrLoadHiddenFiltersLocked(account).contains(filterId);
    }

    public static synchronized void setFilterHidden(int account, int filterId, boolean hidden) {
        Set<Integer> set = getOrLoadHiddenFiltersLocked(account);
        boolean changed = hidden ? set.add(filterId) : set.remove(filterId);
        if (changed) {
            saveHiddenFiltersLocked(account, set);
        }
    }

    public static synchronized List<Integer> getHiddenFilterIds(int account) {
        return new ArrayList<>(getOrLoadHiddenFiltersLocked(account));
    }

    public static synchronized boolean hasAnyHiddenItems(int account) {
        return !getOrLoadHiddenDialogsLocked(account).isEmpty() || !getOrLoadHiddenFiltersLocked(account).isEmpty();
    }

    private static ArrayList<Integer> getOrLoadCustomFilterOrderLocked(int account) {
        loadConfig();
        ArrayList<Integer> list = customFilterOrderByAccount.get(account);
        if (list != null) {
            return list;
        }
        list = new ArrayList<>();
        if (preferences != null) {
            String raw = preferences.getString("customFilterOrder_" + account, "");
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    try {
                        int id = Integer.parseInt(p.trim());
                        if (!list.contains(id)) {
                            list.add(id);
                        }
                    } catch (Exception ignore) {}
                }
            }
        }
        customFilterOrderByAccount.put(account, list);
        return list;
    }

    public static synchronized ArrayList<Integer> getCustomFilterOrder(int account) {
        return new ArrayList<>(getOrLoadCustomFilterOrderLocked(account));
    }

    public static synchronized boolean hasCustomFilterOrder(int account) {
        return !getOrLoadCustomFilterOrderLocked(account).isEmpty();
    }

    public static synchronized void saveCustomFilterOrder(int account, List<Integer> orderIds) {
        loadConfig();
        ArrayList<Integer> list = new ArrayList<>();
        if (orderIds != null) {
            for (Integer id : orderIds) {
                if (id != null && !list.contains(id)) {
                    list.add(id);
                }
            }
        }
        customFilterOrderByAccount.put(account, list);
        if (preferences != null) {
            StringBuilder sb = new StringBuilder();
            for (Integer id : list) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(id);
            }
            preferences.edit().putString("customFilterOrder_" + account, sb.toString()).apply();
        }
    }

    public static synchronized void clearCustomFilterOrder(int account) {
        loadConfig();
        customFilterOrderByAccount.put(account, new ArrayList<>());
        if (preferences != null) {
            preferences.edit().remove("customFilterOrder_" + account).apply();
        }
    }

    private static ArrayList<Integer> getOrLoadLastRemoteFilterOrderLocked(int account) {
        loadConfig();
        ArrayList<Integer> list = lastRemoteFilterOrderByAccount.get(account);
        if (list != null) {
            return list;
        }
        list = new ArrayList<>();
        if (preferences != null) {
            String raw = preferences.getString("lastRemoteFilterOrder_" + account, "");
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    try {
                        int id = Integer.parseInt(p.trim());
                        if (!list.contains(id)) {
                            list.add(id);
                        }
                    } catch (Exception ignore) {}
                }
            }
        }
        lastRemoteFilterOrderByAccount.put(account, list);
        return list;
    }

    public static synchronized ArrayList<Integer> getLastRemoteFilterOrder(int account) {
        return new ArrayList<>(getOrLoadLastRemoteFilterOrderLocked(account));
    }

    public static synchronized void saveLastRemoteFilterOrder(int account, List<Integer> orderIds) {
        loadConfig();
        ArrayList<Integer> list = new ArrayList<>();
        if (orderIds != null) {
            for (Integer id : orderIds) {
                if (id != null && !list.contains(id)) {
                    list.add(id);
                }
            }
        }
        lastRemoteFilterOrderByAccount.put(account, list);
        if (preferences != null) {
            StringBuilder sb = new StringBuilder();
            for (Integer id : list) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(id);
            }
            preferences.edit().putString("lastRemoteFilterOrder_" + account, sb.toString()).apply();
        }
    }

    public static boolean isDialogHiddenInHiddenFolder(int account, long dialogId) {
        if (dialogId == 0 || !hiddenHideFolderChatsEverywhere) {
            return false;
        }
        final Set<Integer> hiddenFiltersSnapshot;
        synchronized (KronoConfig.class) {
            Set<Integer> hiddenFilters = getOrLoadHiddenFiltersLocked(account);
            if (hiddenFilters.isEmpty()) {
                return false;
            }
            hiddenFiltersSnapshot = new HashSet<>(hiddenFilters);
        }
        MessagesController mc = MessagesController.getInstance(account);
        AccountInstance ai = AccountInstance.getInstance(account);
        if (mc == null || ai == null) {
            return false;
        }
        try {
            ArrayList<MessagesController.DialogFilter> filters = mc.getDialogFilters();
            if (filters == null) {
                return false;
            }
            for (int i = 0; i < filters.size(); i++) {
                MessagesController.DialogFilter filter = filters.get(i);
                if (filter != null && hiddenFiltersSnapshot.contains(filter.id)) {
                    if (filter.includesDialog(ai, dialogId)) {
                        return true;
                    }
                }
            }
        } catch (IndexOutOfBoundsException | java.util.ConcurrentModificationException ignored) {
            // Filters mutated concurrently on UI thread; fall back to SparseArray lookup.
            try {
                for (Integer filterId : hiddenFiltersSnapshot) {
                    if (filterId == null) continue;
                    MessagesController.DialogFilter filter = mc.dialogFiltersById.get(filterId);
                    if (filter != null && filter.includesDialog(ai, dialogId)) {
                        return true;
                    }
                }
            } catch (Throwable ignored2) {
            }
        }
        return false;
    }

    public static boolean isDialogOrFolderHidden(int account, long dialogId) {
        return isDialogOrFolderHidden(account, dialogId, null);
    }

    public static boolean isDialogOrFolderHidden(int account, long dialogId, MessagesController.DialogFilter activeFilter) {
        if (dialogId == 0) {
            return false;
        }
        if (isDialogHiddenDirectly(account, dialogId)) {
            return true;
        }
        if (activeFilter != null && !activeFilter.isDefault() && !isFilterHidden(account, activeFilter.id)) {
            if (activeFilter.alwaysShow.contains(dialogId) || activeFilter.pinnedDialogs.indexOfKey(dialogId) >= 0) {
                return false;
            }
        }
        return isDialogHiddenInHiddenFolder(account, dialogId);
    }

    public static synchronized void syncGhostMasterState() {
        loadConfig();
        if (preferences != null && preferences.getBoolean("ghostPrev_hasSnapshot", false)) {
            preferences.edit().remove("ghostPrev_hasSnapshot").apply();
        }
        ghostMode = dontReadMessages && dontReadStories && dontSendTyping && dontSendOnline;
        saveConfig();
    }

    public static boolean isGhostDontReadMessages() {
        loadConfig();
        return ghostMode || dontReadMessages;
    }

    public static boolean isGhostDontReadStories() {
        loadConfig();
        return ghostMode || dontReadStories;
    }

    public static boolean isGhostDontSendTyping() {
        loadConfig();
        return ghostMode || dontSendTyping;
    }

    public static boolean isGhostDontSendOnline() {
        loadConfig();
        return ghostMode || dontSendOnline;
    }

    public static boolean isAllowRestrictedContentBypass() {
        loadConfig();
        return allowRestrictedContentBypass;
    }

    public static boolean isFreeChatTranslation() {
        loadConfig();
        return freeChatTranslation;
    }

    public static synchronized void setZeroTelemetry(boolean enabled) {
        loadConfig();
        if (enabled) {
            if (!zeroTelemetry && preferences != null) {
                preferences.edit()
                        .putBoolean("telemetryPrev_hasSnapshot", true)
                        .putBoolean("telemetryPrev_blockTelegramTelemetry", blockTelegramTelemetry)
                        .putBoolean("telemetryPrev_disableFirebaseAnalytics", disableFirebaseAnalytics)
                        .putBoolean("telemetryPrev_blockSponsoredMessages", blockSponsoredMessages)
                        .putBoolean("telemetryPrev_stripUrlTrackers", stripUrlTrackers)
                        .apply();
            }
            zeroTelemetry = true;
            blockTelegramTelemetry = true;
            disableFirebaseAnalytics = true;
            blockSponsoredMessages = true;
            stripUrlTrackers = true;
        } else {
            boolean restored = false;
            if (preferences != null && preferences.getBoolean("telemetryPrev_hasSnapshot", false)) {
                boolean prevTele = preferences.getBoolean("telemetryPrev_blockTelegramTelemetry", false);
                boolean prevFb = preferences.getBoolean("telemetryPrev_disableFirebaseAnalytics", false);
                boolean prevSpon = preferences.getBoolean("telemetryPrev_blockSponsoredMessages", false);
                boolean prevStrip = preferences.getBoolean("telemetryPrev_stripUrlTrackers", false);
                preferences.edit().remove("telemetryPrev_hasSnapshot").apply();
                if (!(prevTele && prevFb && prevSpon && prevStrip)) {
                    blockTelegramTelemetry = prevTele;
                    disableFirebaseAnalytics = prevFb;
                    blockSponsoredMessages = prevSpon;
                    stripUrlTrackers = prevStrip;
                    restored = true;
                }
            }
            zeroTelemetry = false;
            if (!restored) {
                blockTelegramTelemetry = false;
                disableFirebaseAnalytics = false;
                blockSponsoredMessages = false;
                stripUrlTrackers = false;
            }
        }
        saveConfig();
        applyFirebaseAnalyticsPreference(ApplicationLoader.applicationContext);
    }

    public static synchronized void syncZeroTelemetryMasterState() {
        loadConfig();
        if (preferences != null && preferences.getBoolean("telemetryPrev_hasSnapshot", false)) {
            preferences.edit().remove("telemetryPrev_hasSnapshot").apply();
        }
        zeroTelemetry = blockTelegramTelemetry && disableFirebaseAnalytics && blockSponsoredMessages && stripUrlTrackers;
        saveConfig();
        applyFirebaseAnalyticsPreference(ApplicationLoader.applicationContext);
    }

    public static boolean isBlockTelegramTelemetry() {
        loadConfig();
        return zeroTelemetry || blockTelegramTelemetry;
    }

    public static boolean isDisableFirebaseAnalytics() {
        loadConfig();
        return zeroTelemetry || disableFirebaseAnalytics;
    }

    public static boolean isBlockSponsoredMessages() {
        loadConfig();
        return zeroTelemetry || blockSponsoredMessages;
    }

    public static boolean isStripUrlTrackers() {
        loadConfig();
        return zeroTelemetry || stripUrlTrackers;
    }

    public static boolean isDisableFirebasePush() {
        loadConfig();
        return disableFirebasePush;
    }

    public static boolean isPersistentForegroundService() {
        loadConfig();
        return persistentForegroundService;
    }

    public static void applyFirebaseAnalyticsPreference(Context context) {
        if (context == null) {
            return;
        }
        try {
            boolean enabled = !isDisableFirebaseAnalytics();
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled);
        } catch (Throwable ignore) {
        }
    }

    public static void applyFirebasePushPreference() {
        try {
            boolean enabled = !isDisableFirebasePush();
            com.google.firebase.messaging.FirebaseMessaging.getInstance().setAutoInitEnabled(enabled);
        } catch (Throwable ignore) {
        }
    }

    public static synchronized int getGhostLocalReadMaxId(int currentAccount, long dialogId) {
        loadConfig();
        if (!isGhostDontReadMessages() || preferences == null) {
            return 0;
        }
        return preferences.getInt("ghost_read_max_" + currentAccount + "_" + dialogId, 0);
    }

    public static synchronized void setGhostLocalReadMaxId(int currentAccount, long dialogId, int maxId) {
        loadConfig();
        if (preferences == null || maxId <= 0 || maxId == Integer.MAX_VALUE) {
            return;
        }
        String key = "ghost_read_max_" + currentAccount + "_" + dialogId;
        int prev = preferences.getInt(key, 0);
        if (maxId > prev) {
            preferences.edit().putInt(key, maxId).apply();
        }
    }

    private static Set<Long> getOrLoadLocalPinnedLocked(int account) {
        loadConfig();
        Set<Long> set = localPinnedDialogsByAccount.get(account);
        if (set != null) {
            return set;
        }
        set = new HashSet<>();
        if (preferences != null) {
            String key = "localPinnedDialogs_" + account;
            String raw;
            if (preferences.contains(key)) {
                raw = preferences.getString(key, "");
            } else if (account == 0) {
                raw = preferences.getString("localPinnedDialogs", "");
            } else {
                raw = "";
            }
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    try {
                        set.add(Long.parseLong(p.trim()));
                    } catch (Exception ignore) {}
                }
            }
        }
        localPinnedDialogsByAccount.put(account, set);
        return set;
    }

    private static void saveLocalPinnedLocked(int account, Set<Long> set) {
        if (preferences == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Long did : set) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(did);
        }
        SharedPreferences.Editor editor = preferences.edit().putString("localPinnedDialogs_" + account, sb.toString());
        if (account == 0) {
            editor.putString("localPinnedDialogs", sb.toString());
        }
        editor.apply();
    }

    public static synchronized boolean isLocallyPinned(int account, long dialogId) {
        loadConfig();
        return unlimitedPinnedChats && getOrLoadLocalPinnedLocked(account).contains(dialogId);
    }

    public static synchronized boolean isLocallyPinned(long dialogId) {
        return isLocallyPinned(UserConfig.selectedAccount, dialogId);
    }

    public static synchronized void setLocallyPinned(int account, long dialogId, boolean pinned) {
        Set<Long> set = getOrLoadLocalPinnedLocked(account);
        boolean changed = pinned ? set.add(dialogId) : set.remove(dialogId);
        if (changed) {
            saveLocalPinnedLocked(account, set);
        }
    }

    public static synchronized void setLocallyPinned(long dialogId, boolean pinned) {
        setLocallyPinned(UserConfig.selectedAccount, dialogId, pinned);
    }

    public static ArrayList<String> getSpoilerWordsList() {
        loadConfig();
        ArrayList<String> list = new ArrayList<>();
        if (TextUtils.isEmpty(spoilerWordsRaw)) {
            return list;
        }
        String[] parts = spoilerWordsRaw.split("[,\\n]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    public static void applySpoilerFilterIfNeeded(CharSequence text, ArrayList<TLRPC.MessageEntity> entities) {
        loadConfig();
        if (!spoilerWordsEnabled || TextUtils.isEmpty(text)) {
            return;
        }
        ArrayList<String> words = getSpoilerWordsList();
        if (words.isEmpty()) {
            return;
        }
        for (String word : words) {
            if (TextUtils.isEmpty(word)) {
                continue;
            }
            Matcher matcher = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
            while (matcher.find()) {
                int index = matcher.start();
                int len = matcher.end() - index;
                if (len <= 0) {
                    continue;
                }
                if (entities != null) {
                    TLRPC.TL_messageEntitySpoiler spoiler = new TLRPC.TL_messageEntitySpoiler();
                    spoiler.offset = index;
                    spoiler.length = len;
                    entities.add(spoiler);
                }
                if (text instanceof Spannable) {
                    TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                    run.flags |= TextStyleSpan.FLAG_STYLE_SPOILER;
                    ((Spannable) text).setSpan(new TextStyleSpan(run), index, index + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    public static String getTabModeName() {
        loadConfig();
        switch (tabMode) {
            case TAB_MODE_ICON_TEXT:
                return "Icono y nombre";
            case TAB_MODE_ICON:
                return "Solo icono";
            case TAB_MODE_TEXT:
            default:
                return "Solo nombre";
        }
    }

    public static String getTabStyleName() {
        loadConfig();
        switch (tabStyle) {
            case TAB_STYLE_ROUNDED:
                return "Redondeado";
            case TAB_STYLE_PILL:
                return "Píldora";
            case TAB_STYLE_VKUI:
                return "Subrayado inferior (VKUI)";
            case TAB_STYLE_TEXT_ONLY:
                return "Solo texto (sin indicador)";
            case TAB_STYLE_DEFAULT:
            default:
                return "Predeterminado";
        }
    }

    public static String getFilterCustomIcon(int currentAccount, int filterId) {
        loadConfig();
        if (preferences == null) {
            return "";
        }
        return preferences.getString("filter_icon_" + currentAccount + "_" + filterId, "");
    }

    public static void setFilterCustomIcon(int currentAccount, int filterId, String emoticon) {
        loadConfig();
        if (preferences == null) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        String key = "filter_icon_" + currentAccount + "_" + filterId;
        if (TextUtils.isEmpty(emoticon)) {
            editor.remove(key);
        } else {
            editor.putString(key, emoticon);
        }
        editor.apply();
    }

    public static String getUiStyleName() {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return "Estilo iOS";
            case UI_STYLE_TELEGRAM_X:
                return "Estilo Clásico";
            case UI_STYLE_MATERIAL_YOU:
            default:
                return "Material You (Material 3)";
        }
    }

    public static boolean isMaterialYouStyle() {
        loadConfig();
        return uiStyle == UI_STYLE_MATERIAL_YOU;
    }

    public static boolean isIosStyle() {
        loadConfig();
        return uiStyle == UI_STYLE_IOS;
    }

    public static boolean isTelegramXStyle() {
        loadConfig();
        return uiStyle == UI_STYLE_TELEGRAM_X;
    }

    public static int getEffectiveSectionPaddingPx(int defaultPaddingPx) {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return defaultPaddingPx > 0 ? AndroidUtilities.dp(16) : 0;
            case UI_STYLE_TELEGRAM_X:
                return 0;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return defaultPaddingPx > 0 ? AndroidUtilities.dp(12) : 0;
        }
    }

    public static float getEffectiveSectionRadiusPx(float defaultRadiusPx) {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return defaultRadiusPx > 0 ? AndroidUtilities.dp(12) : 0;
            case UI_STYLE_TELEGRAM_X:
                return 0;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return defaultRadiusPx > 0 ? AndroidUtilities.dp(20) : 0;
        }
    }

    public static int getEffectiveSectionCardColor(int cardColor, int canvasBgColor) {
        loadConfig();
        if (uiStyle == UI_STYLE_TELEGRAM_X) {
            return cardColor;
        }
        int opaqueCard = cardColor | 0xFF000000;
        int opaqueBg = canvasBgColor | 0xFF000000;
        if (AndroidUtilities.getColorDistance(opaqueCard, opaqueBg) < 1200) {
            float lum = AndroidUtilities.computePerceivedBrightness(opaqueBg);
            if (lum < 0.25f) {
                int elevated = uiStyle == UI_STYLE_IOS ? 0xFF16161A : 0xFF14151B;
                return androidx.core.graphics.ColorUtils.blendARGB(opaqueCard, elevated, uiStyle == UI_STYLE_IOS ? 0.92f : 0.96f);
            } else if (lum > 0.85f) {
                return androidx.core.graphics.ColorUtils.blendARGB(opaqueCard, 0xFFF2F4F8, 0.55f);
            }
        }
        return cardColor;
    }

    public static int getEffectiveSectionStrokeColor(int cardColor, int canvasBgColor) {
        loadConfig();
        if (uiStyle == UI_STYLE_TELEGRAM_X) {
            return 0;
        }
        int opaqueBg = canvasBgColor | 0xFF000000;
        float lum = AndroidUtilities.computePerceivedBrightness(opaqueBg);
        if (lum < 0.25f) {
            return uiStyle == UI_STYLE_IOS ? 0x26FFFFFF : 0x1AFFFFFF;
        }
        return uiStyle == UI_STYLE_IOS ? 0x16000000 : 0x10000000;
    }

    public static int getDialogRowHeightDp(int defaultHeightDp) {
        loadConfig();
        if (defaultHeightDp != 70) {
            return defaultHeightDp;
        }
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return 68;
            case UI_STYLE_TELEGRAM_X:
                return 64;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return 74;
        }
    }

    public static int getDialogAvatarTopDp() {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return 8;
            case UI_STYLE_TELEGRAM_X:
                return 6;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return 11;
        }
    }

    public static int getRecommendedBubbleRadius() {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_TELEGRAM_X:
                return 10;
            case UI_STYLE_IOS:
                return 16;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return 18;
        }
    }

    public static boolean isLiquidGlassSelected() {
        loadConfig();
        return glassMode == GLASS_MODE_LIQUID;
    }

    public static boolean isFrostedGlassSelected() {
        loadConfig();
        return glassMode == GLASS_MODE_FROSTED;
    }

    public static boolean isGlassOff() {
        loadConfig();
        return glassMode == GLASS_MODE_OFF;
    }

    public static boolean isGlassEnabledForSettingsBar() {
        loadConfig();
        return glassMode != GLASS_MODE_OFF && glassOnSettingsBar;
    }

    public static boolean isGlassEnabledForSearchBars() {
        loadConfig();
        return glassMode != GLASS_MODE_OFF && glassOnSearchBars;
    }

    public static boolean isGlassEnabledForBottomBar() {
        loadConfig();
        return glassMode != GLASS_MODE_OFF && glassOnBottomBar;
    }

    public static boolean isGlassEnabledForChatBars() {
        loadConfig();
        return glassMode != GLASS_MODE_OFF && glassOnChatBars;
    }

    public static int getGlassTogglesMask() {
        loadConfig();
        int mask = (glassMode & 0x3);
        if (glassOnSettingsBar) mask |= (1 << 2);
        if (glassOnSearchBars) mask |= (1 << 3);
        if (glassOnBottomBar) mask |= (1 << 4);
        if (glassOnChatBars) mask |= (1 << 5);
        mask |= ((uiStyle & 0x3) << 6);
        return mask;
    }

    public static int getActionBarGlassRadiusDp() {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return 19;
            case UI_STYLE_TELEGRAM_X:
                return 14;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return 23;
        }
    }

    public static int getSearchFieldGlassRadiusDp() {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_IOS:
                return 16;
            case UI_STYLE_TELEGRAM_X:
                return 12;
            case UI_STYLE_MATERIAL_YOU:
            default:
                return 22;
        }
    }

    public static String getGlassModeName() {
        loadConfig();
        switch (glassMode) {
            case GLASS_MODE_FROSTED:
                return "Frosted Glass";
            case GLASS_MODE_OFF:
                return "Desactivado (Sólido)";
            case GLASS_MODE_LIQUID:
            default:
                if (android.os.Build.VERSION.SDK_INT < 33) {
                    return "Liquid Glass (Specular)";
                }
                return "Liquid Glass";
        }
    }

    public static String getVoiceTranscriptionModeName() {
        loadConfig();
        if (voiceTranscriptionMode == VOICE_TRANSCRIBE_MODE_LOCAL_ONLY) {
            return "Siempre local offline";
        }
        return "Automático (Telegram → Local offline)";
    }

    public static String getVoiceTranscriptionLanguageName() {
        loadConfig();
        return KronoVoiceTranscriber.getLanguageDisplayName(voiceTranscriptionLanguage);
    }

    public static String getTranslationProviderName() {
        loadConfig();
        switch (translationProvider) {
            case TRANSLATE_PROVIDER_GOOGLE:
                return "Google Translate";
            case TRANSLATE_PROVIDER_YANDEX:
                return "Yandex Translate";
            case TRANSLATE_PROVIDER_LINGVA:
                return "Lingva Translate";
            case TRANSLATE_PROVIDER_TELEGRAM:
            default:
                return "Telegram Oficial";
        }
    }

    public static String getMessageSwipeActionName() {
        loadConfig();
        switch (messageSwipeAction) {
            case SWIPE_ACTION_EDIT_OR_REPLY:
                return "Editar (propio) / Responder";
            case SWIPE_ACTION_SAVE:
                return "Guardar en Mensajes guardados";
            case SWIPE_ACTION_TRANSLATE:
                return "Traducir mensaje";
            case SWIPE_ACTION_FORWARD:
                return "Reenviar mensaje";
            case SWIPE_ACTION_COPY:
                return "Copiar texto";
            case SWIPE_ACTION_DISABLED:
                return "Desactivado";
            case SWIPE_ACTION_REPLY:
            default:
                return "Responder";
        }
    }

    public static boolean canDeleteMessageFromCache(int currentAccount, MessageObject msg) {
        if (msg == null || msg.messageOwner == null) {
            return false;
        }
        if (msg.isSticker() || msg.isAnimatedSticker() || msg.isAnimatedEmoji()) {
            return false;
        }
        try {
            if (!TextUtils.isEmpty(msg.messageOwner.attachPath)) {
                File f = new File(msg.messageOwner.attachPath);
                if (f.exists() && f.isFile()) {
                    return true;
                }
            }
            File f = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
            if (f != null && f.exists() && f.isFile()) {
                return true;
            }
            if (msg.getDocument() != null) {
                File docFile = FileLoader.getInstance(currentAccount).getPathToAttach(msg.getDocument(), true);
                if (docFile != null && docFile.exists() && docFile.isFile()) {
                    return true;
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    public static long deleteSingleMessageFromCache(int currentAccount, MessageObject msg) {
        if (msg == null || msg.messageOwner == null) {
            return 0;
        }
        long freed = 0;
        try {
            File f1 = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
            if (f1 != null && f1.exists() && f1.isFile()) {
                long len = f1.length();
                if (f1.delete()) {
                    freed += len;
                }
            }
            if (msg.getDocument() != null) {
                File f2 = FileLoader.getInstance(currentAccount).getPathToAttach(msg.getDocument(), true);
                if (f2 != null && f2.exists() && f2.isFile()) {
                    long len = f2.length();
                    if (f2.delete()) {
                        freed += len;
                    }
                }
                File f3 = FileLoader.getInstance(currentAccount).getPathToAttach(msg.getDocument(), false);
                if (f3 != null && f3.exists() && f3.isFile()) {
                    long len = f3.length();
                    if (f3.delete()) {
                        freed += len;
                    }
                }
            }
            if (msg.messageOwner.media != null && msg.messageOwner.media.photo != null && msg.messageOwner.media.photo.sizes != null) {
                for (TLRPC.PhotoSize size : msg.messageOwner.media.photo.sizes) {
                    File pf = FileLoader.getInstance(currentAccount).getPathToAttach(size, true);
                    if (pf != null && pf.exists() && pf.isFile()) {
                        long len = pf.length();
                        if (pf.delete()) {
                            freed += len;
                        }
                    }
                }
            }
            if (!TextUtils.isEmpty(msg.messageOwner.attachPath)) {
                File attach = new File(msg.messageOwner.attachPath);
                if (attach.exists() && attach.isFile()) {
                    long len = attach.length();
                    if (attach.delete()) {
                        freed += len;
                    }
                }
                msg.messageOwner.attachPath = "";
            }
            msg.mediaExists = false;
            msg.checkMediaExistance();
        } catch (Exception ignore) {}
        return freed;
    }

    public static long deleteMessageMediaFromCache(int currentAccount, MessageObject msg, MessageObject.GroupedMessages group) {
        long freed = 0;
        if (group != null && group.messages != null && !group.messages.isEmpty()) {
            for (int i = 0; i < group.messages.size(); i++) {
                freed += deleteSingleMessageFromCache(currentAccount, group.messages.get(i));
            }
        } else {
            freed += deleteSingleMessageFromCache(currentAccount, msg);
        }
        return freed;
    }

    private static List<File> getClearableCacheDirs() {
        ArrayList<File> dirs = new ArrayList<>();
        HashSet<String> seenPaths = new HashSet<>();
        try {
            File downloadsPub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String downloadsPath = downloadsPub != null ? downloadsPub.getAbsolutePath() : null;

            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                addCacheDirIfValid(dirs, seenPaths, context.getCacheDir(), downloadsPath);
                addCacheDirIfValid(dirs, seenPaths, context.getExternalCacheDir(), downloadsPath);
            }
            int[] cacheMediaDirs = new int[]{
                    FileLoader.MEDIA_DIR_CACHE,
                    FileLoader.MEDIA_DIR_IMAGE,
                    FileLoader.MEDIA_DIR_VIDEO,
                    FileLoader.MEDIA_DIR_AUDIO,
                    FileLoader.MEDIA_DIR_DOCUMENT,
                    FileLoader.MEDIA_DIR_STORIES
            };
            for (int dirType : cacheMediaDirs) {
                addCacheDirIfValid(dirs, seenPaths, FileLoader.checkDirectory(dirType), downloadsPath);
            }
        } catch (Exception ignore) {}
        return dirs;
    }

    private static void addCacheDirIfValid(List<File> dirs, Set<String> seenPaths, File dir, String downloadsPath) {
        if (dir == null || !dir.exists()) {
            return;
        }
        String path;
        try {
            path = dir.getCanonicalPath();
        } catch (Exception e) {
            path = dir.getAbsolutePath();
        }
        if (downloadsPath != null && path.startsWith(downloadsPath)) {
            return;
        }
        for (String existing : seenPaths) {
            if (path.equals(existing) || path.startsWith(existing + File.separator)) {
                return;
            }
        }
        seenPaths.add(path);
        dirs.add(dir);
    }

    public static long calculateCacheSize() {
        long total = 0;
        try {
            for (File dir : getClearableCacheDirs()) {
                total += getDirSize(dir);
            }
        } catch (Exception ignore) {}
        return total;
    }

    public static long clearCacheFilesSync() {
        long freed = 0;
        try {
            for (File dir : getClearableCacheDirs()) {
                freed += deleteDirContents(dir);
            }
        } catch (Exception ignore) {}
        return freed;
    }

    private static long getDirSize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    size += getDirSize(f);
                } else {
                    size += f.length();
                }
            }
        }
        return size;
    }

    private static long deleteDirContents(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        long freed = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    freed += deleteDirContents(f);
                } else {
                    long len = f.length();
                    if (f.delete()) {
                        freed += len;
                    }
                }
            }
        }
        return freed;
    }

    public static String translateTextSync(String text, String toLang) {
        return translateTextSync(text, "auto", toLang);
    }

    public static String translateTextSync(String text, String fromLang, String toLang) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }
        String sl = (TextUtils.isEmpty(fromLang) || "und".equalsIgnoreCase(fromLang)) ? "auto" : fromLang;
        String tl = TextUtils.isEmpty(toLang) ? "es" : toLang;
        try {
            if (translationProvider == TRANSLATE_PROVIDER_LINGVA) {
                String lingvaRes = translateWithLingvaChunked(text, sl, tl);
                if (!TextUtils.isEmpty(lingvaRes)) {
                    return lingvaRes;
                }
            } else if (translationProvider == TRANSLATE_PROVIDER_YANDEX) {
                String yandexRes = translateWithYandex(text, sl, tl);
                if (!TextUtils.isEmpty(yandexRes)) {
                    return yandexRes;
                }
            }
            return translateWithGoogleGtx(text, sl, tl);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private static String translateWithLingvaChunked(String text, String sl, String tl) {
        final int maxChunk = 900;
        StringBuilder combined = new StringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            int end = Math.min(text.length(), pos + maxChunk);
            if (end < text.length()) {
                int split = text.lastIndexOf('\n', end);
                if (split <= pos) split = text.lastIndexOf(' ', end);
                if (split > pos) end = split + 1;
            }
            String chunk = text.substring(pos, end);
            pos = end;
            String urlStr = "https://lingva.ml/api/v1/" + Uri.encode(sl) + "/" + Uri.encode(tl) + "/" + Uri.encode(chunk);
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    JSONObject obj = new JSONObject(sb.toString());
                    if (obj.has("translation")) {
                        combined.append(obj.getString("translation"));
                        continue;
                    }
                }
                return null;
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return combined.length() > 0 ? combined.toString() : null;
    }

    private static String translateWithYandex(String text, String sl, String tl) {
        HttpURLConnection conn = null;
        try {
            String langPair = "auto".equalsIgnoreCase(sl) ? tl : (sl + "-" + tl);
            String urlStr = "https://translate.yandex.net/api/v1/tr.json/translate?srv=android&lang=" + URLEncoder.encode(langPair, "UTF-8");
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            byte[] body = ("text=" + URLEncoder.encode(text, "UTF-8")).getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                JSONObject obj = new JSONObject(sb.toString());
                JSONArray texts = obj.optJSONArray("text");
                if (texts != null && texts.length() > 0) {
                    StringBuilder out = new StringBuilder();
                    for (int i = 0; i < texts.length(); i++) {
                        out.append(texts.optString(i, ""));
                    }
                    if (out.length() > 0) {
                        return out.toString();
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private static String translateWithGoogleGtx(String text, String sl, String tl) throws Exception {
        String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                + URLEncoder.encode(sl, "UTF-8")
                + "&tl=" + URLEncoder.encode(tl, "UTF-8")
                + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                JSONArray root = new JSONArray(sb.toString());
                JSONArray sentences = root.getJSONArray(0);
                StringBuilder translated = new StringBuilder();
                for (int i = 0; i < sentences.length(); i++) {
                    JSONArray part = sentences.getJSONArray(i);
                    if (part.length() > 0 && !part.isNull(0)) {
                        translated.append(part.getString(0));
                    }
                }
                if (translated.length() > 0) {
                    return translated.toString();
                }
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    public static void translateWithSelectedEngine(String text, boolean isUrlEncoded, String fromLng, String toLng, Utilities.Callback2<String, Boolean> done) {
        if (done == null) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            String rawText = text;
            if (isUrlEncoded && rawText != null) {
                try {
                    rawText = android.net.Uri.decode(rawText);
                } catch (Exception ignored) {
                }
            }
            String result = translateTextSync(rawText, fromLng, toLng);
            AndroidUtilities.runOnUIThread(() -> done.run(result, false));
        });
    }
}
