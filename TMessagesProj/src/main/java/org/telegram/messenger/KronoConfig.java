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
    private static final Set<Long> localPinnedDialogs = new HashSet<>();
    private static final Set<Long> favoriteDialogs = new HashSet<>();

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

        localPinnedDialogs.clear();
        String pinnedRaw = preferences.getString("localPinnedDialogs", "");
        if (!TextUtils.isEmpty(pinnedRaw)) {
            String[] parts = pinnedRaw.split(",");
            for (String p : parts) {
                try {
                    localPinnedDialogs.add(Long.parseLong(p.trim()));
                } catch (Exception ignore) {}
            }
        }

        favoriteDialogs.clear();
        String favRaw = preferences.getString("favoriteDialogs", "");
        if (!TextUtils.isEmpty(favRaw)) {
            String[] parts = favRaw.split(",");
            for (String p : parts) {
                try {
                    favoriteDialogs.add(Long.parseLong(p.trim()));
                } catch (Exception ignore) {}
            }
        }

        uiStyle = preferences.getInt("uiStyle", UI_STYLE_MATERIAL_YOU);
        disableTitleMarquee = preferences.getBoolean("disableTitleMarquee", false);
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
        loaded = true;
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
        StringBuilder pinnedBuilder = new StringBuilder();
        for (Long did : localPinnedDialogs) {
            if (pinnedBuilder.length() > 0) {
                pinnedBuilder.append(",");
            }
            pinnedBuilder.append(did);
        }
        StringBuilder favBuilder = new StringBuilder();
        for (Long did : favoriteDialogs) {
            if (favBuilder.length() > 0) {
                favBuilder.append(",");
            }
            favBuilder.append(did);
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
                .putString("localPinnedDialogs", pinnedBuilder.toString())
                .putString("favoriteDialogs", favBuilder.toString())
                .putInt("uiStyle", uiStyle)
                .putBoolean("disableTitleMarquee", disableTitleMarquee)
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
    }

    public static synchronized void setDisableTitleMarquee(boolean value) {
        loadConfig();
        disableTitleMarquee = value;
        saveConfig();
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

    public static synchronized boolean isFavoriteDialog(long dialogId) {
        loadConfig();
        return favoriteDialogs.contains(dialogId);
    }

    public static synchronized void setFavoriteDialog(long dialogId, boolean favorite) {
        loadConfig();
        boolean changed;
        if (favorite) {
            changed = favoriteDialogs.add(dialogId);
        } else {
            changed = favoriteDialogs.remove(dialogId);
        }
        if (changed) {
            saveConfig();
        }
    }

    public static synchronized boolean toggleFavoriteDialog(long dialogId) {
        loadConfig();
        boolean nowFavorite = !favoriteDialogs.contains(dialogId);
        setFavoriteDialog(dialogId, nowFavorite);
        return nowFavorite;
    }

    public static synchronized Set<Long> getFavoriteDialogs() {
        loadConfig();
        return new HashSet<>(favoriteDialogs);
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
        if (dialogId == 0) {
            return false;
        }
        return isDialogHiddenDirectly(account, dialogId) || isDialogHiddenInHiddenFolder(account, dialogId);
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

    public static synchronized boolean isLocallyPinned(long dialogId) {
        loadConfig();
        return unlimitedPinnedChats && localPinnedDialogs.contains(dialogId);
    }

    public static synchronized void setLocallyPinned(long dialogId, boolean pinned) {
        loadConfig();
        if (pinned) {
            localPinnedDialogs.add(dialogId);
        } else {
            localPinnedDialogs.remove(dialogId);
        }
        saveConfig();
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
                return defaultPaddingPx > 0 ? AndroidUtilities.dp(14) : 0;
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

    public static int getRecommendedBubbleRadius() {
        loadConfig();
        switch (uiStyle) {
            case UI_STYLE_TELEGRAM_X:
                return 10;
            case UI_STYLE_IOS:
            case UI_STYLE_MATERIAL_YOU:
            default:
                return 18;
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

    public static long calculateCacheSize() {
        long total = 0;
        try {
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                total += getDirSize(context.getCacheDir());
                File extCache = context.getExternalCacheDir();
                if (extCache != null) {
                    total += getDirSize(extCache);
                }
            }
            for (int a = 0; a <= FileLoader.MEDIA_DIR_STORIES; a++) {
                File dir = FileLoader.checkDirectory(a);
                if (dir != null && dir.exists()) {
                    total += getDirSize(dir);
                }
            }
        } catch (Exception ignore) {}
        return total;
    }

    public static long clearCacheFilesSync() {
        long freed = 0;
        try {
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                freed += deleteDirContents(context.getCacheDir());
                File extCache = context.getExternalCacheDir();
                if (extCache != null) {
                    freed += deleteDirContents(extCache);
                }
            }
            int[] cacheMediaDirs = new int[]{
                    FileLoader.MEDIA_DIR_CACHE,
                    FileLoader.MEDIA_DIR_IMAGE,
                    FileLoader.MEDIA_DIR_VIDEO,
                    FileLoader.MEDIA_DIR_AUDIO,
                    FileLoader.MEDIA_DIR_DOCUMENT,
                    FileLoader.MEDIA_DIR_STORIES
            };
            File downloadsPub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String downloadsPath = downloadsPub != null ? downloadsPub.getAbsolutePath() : null;
            for (int dirType : cacheMediaDirs) {
                File dir = FileLoader.checkDirectory(dirType);
                if (dir != null && dir.exists()) {
                    if (downloadsPath != null && dir.getAbsolutePath().startsWith(downloadsPath)) {
                        continue;
                    }
                    freed += deleteDirContents(dir);
                }
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
