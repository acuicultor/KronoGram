package org.telegram.ui;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FolderSyncLogger;
import org.telegram.messenger.KronoConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.NotificationsService;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.EmojiThemes;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KronoSettingsActivity extends BaseFragment {

    public static final int SECTION_MAIN = 0;
    public static final int SECTION_PRIVACY = 1;
    public static final int SECTION_APPEARANCE = 2;
    public static final int SECTION_CHATS = 3;
    public static final int SECTION_AI = 4;
    public static final int SECTION_NOTIFICATIONS = 5;

    private final int currentSection;
    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;

    // Main section rows
    private int categoriesHeaderRow = -1;
    private int privacyCategoryRow = -1;
    private int appearanceCategoryRow = -1;
    private int chatsCategoryRow = -1;
    private int aiCategoryRow = -1;
    private int notificationsCategoryRow = -1;
    private int backupCategoryRow = -1;

    // Privacy section rows
    private int ghostHeaderRow = -1;
    private int ghostMasterRow = -1;
    private int ghostDontReadMessagesRow = -1;
    private int ghostDontReadStoriesRow = -1;
    private int ghostDontSendTypingRow = -1;
    private int ghostDontSendOnlineRow = -1;
    private int ghostInfoRow = -1;
    private int hiddenVaultHeaderRow = -1;
    private int hiddenVaultOpenRow = -1;
    private int hiddenVaultLockMethodRow = -1;
    private int hiddenVaultBiometricRow = -1;
    private int hiddenVaultMuteNotifRow = -1;
    private int hiddenVaultHideFolderChatsRow = -1;
    private int hiddenVaultAutoLockRow = -1;
    private int hiddenVaultInfoRow = -1;
    private int privacyExtraHeaderRow = -1;
    private int hidePhoneNumberRow = -1;
    private int hideChatListNamesRow = -1;
    private int blockSecretChatsRow = -1;
    private int antiDeleteMessagesRow = -1;
    private int antiDeleteExcludeChannelsRow = -1;
    private int unlimitedViewOnceRow = -1;
    private int viewOnceAllowSaveRow = -1;
    private int allowRestrictedContentBypassRow = -1;
    private int privacyExtraInfoRow = -1;
    private int telemetryHeaderRow = -1;
    private int zeroTelemetryRow = -1;
    private int blockTelegramTelemetryRow = -1;
    private int disableFirebaseAnalyticsRow = -1;
    private int blockSponsoredMessagesRow = -1;
    private int stripUrlTrackersRow = -1;
    private int disableFirebasePushRow = -1;
    private int telemetryInfoRow = -1;

    // Appearance section rows
    private int themeHeaderRow = -1;
    private int openThemesRow = -1;
    private int monetThemeRow = -1;
    private int uiStyleRow = -1;
    private int glassModeRow = -1;
    private int glassSettingsBarRow = -1;
    private int glassSearchBarsRow = -1;
    private int glassBottomBarRow = -1;
    private int glassChatBarsRow = -1;
    private int disableTitleMarqueeRow = -1;
    private int smoothSplashAnimationRow = -1;
    private int themeInfoRow = -1;

    // Chats, Folders & Storage section rows
    private int tabsHeaderRow = -1;
    private int tabModeRow = -1;
    private int tabStyleRow = -1;
    private int tabStyleStrokeRow = -1;
    private int hideTabsCountersRow = -1;
    private int folderNameInHeaderRow = -1;
    private int disableTabsSwipeRow = -1;
    private int infiniteFolderScrollRow = -1;
    private int showFavoritesFolderRow = -1;
    private int hideAllChatsTabRow = -1;
    private int autoSendFolderLogsRow = -1;
    private int sendFolderLogsNowRow = -1;
    private int tabsDividerRow = -1;
    private int chatsHeaderRow = -1;
    private int messageSwipeActionRow = -1;
    private int swipeToEditOwnRow = -1;
    private int deleteForAllDefaultRow = -1;
    private int showTimeWithSecondsRow = -1;
    private int unlimitedPinnedChatsRow = -1;
    private int quickGoToMessageRow = -1;
    private int longClickMediaMenuRow = -1;
    private int disablePullToNextChannelRow = -1;
    private int spoilerWordsToggleRow = -1;
    private int spoilerWordsEditRow = -1;
    private int chatsDividerRow = -1;
    private int storageHeaderRow = -1;
    private int keepOriginalFilenameRow = -1;
    private int clearCacheNowRow = -1;
    private int openStorageUsageRow = -1;
    private int storageInfoRow = -1;

    // AI & Translation section rows
    private int aiHeaderRow = -1;
    private int freeVoiceTranscriptionRow = -1;
    private int voiceTranscriptionModeRow = -1;
    private int voiceTranscriptionLanguageRow = -1;
    private int voiceTranscriptionModelsRow = -1;
    private int freeChatTranslationRow = -1;
    private int translationProviderRow = -1;
    private int aiInfoRow = -1;

    // Notifications & Diagnostics section rows
    private int notifServiceHeaderRow = -1;
    private int persistentForegroundServiceRow = -1;
    private int backgroundPushConnectionRow = -1;
    private int notifServiceInfoRow = -1;
    private int notifDividerRow = -1;
    private int diagHeaderRow = -1;
    private int diagPlayServicesRow = -1;
    private int diagFcmStatusRow = -1;
    private int diagFcmTokenRow = -1;
    private int diagNotifPermRow = -1;
    private int diagBatteryOptRow = -1;
    private int diagServiceRunningRow = -1;
    private int diagLastPushRow = -1;
    private int diagRefreshPushRow = -1;
    private int diagTestNotificationRow = -1;
    private int diagSystemBatteryRow = -1;
    private int diagInfoRow = -1;

    private String cacheSizeText = LocaleController.getString(R.string.KronoCalculating);
    private String voiceModelsStatusText = LocaleController.getString(R.string.KronoChecking);

    public KronoSettingsActivity() {
        this(SECTION_MAIN);
    }

    public KronoSettingsActivity(int section) {
        super();
        this.currentSection = section;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        KronoConfig.loadConfig();
        updateRows();
        if (currentSection == SECTION_CHATS) {
            refreshCacheSize();
        } else if (currentSection == SECTION_AI) {
            refreshVoiceModelsStatus();
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((currentSection == SECTION_NOTIFICATIONS || currentSection == SECTION_PRIVACY || currentSection == SECTION_APPEARANCE) && listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        } else if (currentSection == SECTION_AI) {
            refreshVoiceModelsStatus();
        }
    }

    private void refreshVoiceModelsStatus() {
        Utilities.globalQueue.postRunnable(() -> {
            long size = org.telegram.messenger.KronoVoiceTranscriber.getDownloadedModelsSizeBytes();
            boolean activeReady = org.telegram.messenger.KronoVoiceTranscriber.isModelDownloaded(KronoConfig.voiceTranscriptionLanguage);
            String label;
            if (size <= 0L) {
                label = LocaleController.getString(R.string.KronoNotDownloaded);
            } else {
                String formatted = AndroidUtilities.formatFileSize(size);
                label = activeReady ? LocaleController.formatString(R.string.KronoReadyWithSize, formatted) : formatted;
            }
            AndroidUtilities.runOnUIThread(() -> {
                voiceModelsStatusText = label;
                if (listAdapter != null && voiceTranscriptionModelsRow >= 0) {
                    listAdapter.notifyItemChanged(voiceTranscriptionModelsRow);
                }
            });
        });
    }

    private void refreshCacheSize() {
        Utilities.globalQueue.postRunnable(() -> {
            long size = KronoConfig.calculateCacheSize();
            String formatted = AndroidUtilities.formatFileSize(size);
            AndroidUtilities.runOnUIThread(() -> {
                cacheSizeText = formatted;
                if (listAdapter != null && clearCacheNowRow >= 0) {
                    listAdapter.notifyItemChanged(clearCacheNowRow);
                }
            });
        });
    }

    private void resetRowIndices() {
        rowCount = 0;
        categoriesHeaderRow = -1;
        privacyCategoryRow = -1;
        appearanceCategoryRow = -1;
        chatsCategoryRow = -1;
        aiCategoryRow = -1;
        notificationsCategoryRow = -1;
        backupCategoryRow = -1;

        ghostHeaderRow = -1;
        ghostMasterRow = -1;
        ghostDontReadMessagesRow = -1;
        ghostDontReadStoriesRow = -1;
        ghostDontSendTypingRow = -1;
        ghostDontSendOnlineRow = -1;
        ghostInfoRow = -1;
        hiddenVaultHeaderRow = -1;
        hiddenVaultOpenRow = -1;
        hiddenVaultLockMethodRow = -1;
        hiddenVaultBiometricRow = -1;
        hiddenVaultMuteNotifRow = -1;
        hiddenVaultHideFolderChatsRow = -1;
        hiddenVaultAutoLockRow = -1;
        hiddenVaultInfoRow = -1;
        privacyExtraHeaderRow = -1;
        hidePhoneNumberRow = -1;
        hideChatListNamesRow = -1;
        blockSecretChatsRow = -1;
        antiDeleteMessagesRow = -1;
        antiDeleteExcludeChannelsRow = -1;
        unlimitedViewOnceRow = -1;
        viewOnceAllowSaveRow = -1;
        allowRestrictedContentBypassRow = -1;
        privacyExtraInfoRow = -1;
        telemetryHeaderRow = -1;
        zeroTelemetryRow = -1;
        blockTelegramTelemetryRow = -1;
        disableFirebaseAnalyticsRow = -1;
        blockSponsoredMessagesRow = -1;
        stripUrlTrackersRow = -1;
        disableFirebasePushRow = -1;
        telemetryInfoRow = -1;

        themeHeaderRow = -1;
        openThemesRow = -1;
        monetThemeRow = -1;
        uiStyleRow = -1;
        glassModeRow = -1;
        glassSettingsBarRow = -1;
        glassSearchBarsRow = -1;
        glassBottomBarRow = -1;
        glassChatBarsRow = -1;
        disableTitleMarqueeRow = -1;
        smoothSplashAnimationRow = -1;
        themeInfoRow = -1;

        tabsHeaderRow = -1;
        tabModeRow = -1;
        tabStyleRow = -1;
        tabStyleStrokeRow = -1;
        hideTabsCountersRow = -1;
        folderNameInHeaderRow = -1;
        disableTabsSwipeRow = -1;
        infiniteFolderScrollRow = -1;
        showFavoritesFolderRow = -1;
        hideAllChatsTabRow = -1;
        autoSendFolderLogsRow = -1;
        sendFolderLogsNowRow = -1;
        tabsDividerRow = -1;
        chatsHeaderRow = -1;
        messageSwipeActionRow = -1;
        swipeToEditOwnRow = -1;
        deleteForAllDefaultRow = -1;
        showTimeWithSecondsRow = -1;
        unlimitedPinnedChatsRow = -1;
        quickGoToMessageRow = -1;
        longClickMediaMenuRow = -1;
        disablePullToNextChannelRow = -1;
        spoilerWordsToggleRow = -1;
        spoilerWordsEditRow = -1;
        chatsDividerRow = -1;
        storageHeaderRow = -1;
        keepOriginalFilenameRow = -1;
        clearCacheNowRow = -1;
        openStorageUsageRow = -1;
        storageInfoRow = -1;

        aiHeaderRow = -1;
        freeVoiceTranscriptionRow = -1;
        voiceTranscriptionModeRow = -1;
        voiceTranscriptionLanguageRow = -1;
        voiceTranscriptionModelsRow = -1;
        freeChatTranslationRow = -1;
        translationProviderRow = -1;
        aiInfoRow = -1;

        notifServiceHeaderRow = -1;
        persistentForegroundServiceRow = -1;
        backgroundPushConnectionRow = -1;
        notifServiceInfoRow = -1;
        notifDividerRow = -1;
        diagHeaderRow = -1;
        diagPlayServicesRow = -1;
        diagFcmStatusRow = -1;
        diagFcmTokenRow = -1;
        diagNotifPermRow = -1;
        diagBatteryOptRow = -1;
        diagServiceRunningRow = -1;
        diagLastPushRow = -1;
        diagRefreshPushRow = -1;
        diagTestNotificationRow = -1;
        diagSystemBatteryRow = -1;
        diagInfoRow = -1;
    }

    private void updateRows() {
        resetRowIndices();
        if (currentSection == SECTION_MAIN) {
            categoriesHeaderRow = rowCount++;
            privacyCategoryRow = rowCount++;
            appearanceCategoryRow = rowCount++;
            chatsCategoryRow = rowCount++;
            aiCategoryRow = rowCount++;
            notificationsCategoryRow = rowCount++;
            backupCategoryRow = rowCount++;
        } else if (currentSection == SECTION_PRIVACY) {
            ghostHeaderRow = rowCount++;
            ghostMasterRow = rowCount++;
            ghostDontReadMessagesRow = rowCount++;
            ghostDontReadStoriesRow = rowCount++;
            ghostDontSendTypingRow = rowCount++;
            ghostDontSendOnlineRow = rowCount++;
            ghostInfoRow = rowCount++;
            hiddenVaultHeaderRow = rowCount++;
            hiddenVaultOpenRow = rowCount++;
            hiddenVaultLockMethodRow = rowCount++;
            hiddenVaultBiometricRow = rowCount++;
            hiddenVaultMuteNotifRow = rowCount++;
            hiddenVaultHideFolderChatsRow = rowCount++;
            hiddenVaultAutoLockRow = rowCount++;
            hiddenVaultInfoRow = rowCount++;
            privacyExtraHeaderRow = rowCount++;
            hidePhoneNumberRow = rowCount++;
            hideChatListNamesRow = rowCount++;
            blockSecretChatsRow = rowCount++;
            antiDeleteMessagesRow = rowCount++;
            antiDeleteExcludeChannelsRow = rowCount++;
            unlimitedViewOnceRow = rowCount++;
            viewOnceAllowSaveRow = rowCount++;
            allowRestrictedContentBypassRow = rowCount++;
            privacyExtraInfoRow = rowCount++;
            telemetryHeaderRow = rowCount++;
            zeroTelemetryRow = rowCount++;
            blockTelegramTelemetryRow = rowCount++;
            disableFirebaseAnalyticsRow = rowCount++;
            blockSponsoredMessagesRow = rowCount++;
            stripUrlTrackersRow = rowCount++;
            disableFirebasePushRow = rowCount++;
            telemetryInfoRow = rowCount++;
        } else if (currentSection == SECTION_APPEARANCE) {
            themeHeaderRow = rowCount++;
            openThemesRow = rowCount++;
            monetThemeRow = rowCount++;
            uiStyleRow = rowCount++;
            glassModeRow = rowCount++;
            glassSettingsBarRow = rowCount++;
            glassSearchBarsRow = rowCount++;
            glassBottomBarRow = rowCount++;
            glassChatBarsRow = rowCount++;
            disableTitleMarqueeRow = rowCount++;
            smoothSplashAnimationRow = rowCount++;
            themeInfoRow = rowCount++;
        } else if (currentSection == SECTION_CHATS) {
            tabsHeaderRow = rowCount++;
            tabModeRow = rowCount++;
            tabStyleRow = rowCount++;
            tabStyleStrokeRow = rowCount++;
            hideTabsCountersRow = rowCount++;
            folderNameInHeaderRow = rowCount++;
            disableTabsSwipeRow = rowCount++;
            infiniteFolderScrollRow = rowCount++;
            showFavoritesFolderRow = rowCount++;
            hideAllChatsTabRow = rowCount++;
            autoSendFolderLogsRow = rowCount++;
            sendFolderLogsNowRow = rowCount++;
            tabsDividerRow = rowCount++;
            chatsHeaderRow = rowCount++;
            messageSwipeActionRow = rowCount++;
            swipeToEditOwnRow = rowCount++;
            deleteForAllDefaultRow = rowCount++;
            showTimeWithSecondsRow = rowCount++;
            unlimitedPinnedChatsRow = rowCount++;
            quickGoToMessageRow = rowCount++;
            longClickMediaMenuRow = rowCount++;
            disablePullToNextChannelRow = rowCount++;
            spoilerWordsToggleRow = rowCount++;
            spoilerWordsEditRow = rowCount++;
            chatsDividerRow = rowCount++;
            storageHeaderRow = rowCount++;
            keepOriginalFilenameRow = rowCount++;
            clearCacheNowRow = rowCount++;
            openStorageUsageRow = rowCount++;
            storageInfoRow = rowCount++;
        } else if (currentSection == SECTION_AI) {
            aiHeaderRow = rowCount++;
            freeVoiceTranscriptionRow = rowCount++;
            voiceTranscriptionModeRow = rowCount++;
            voiceTranscriptionLanguageRow = rowCount++;
            voiceTranscriptionModelsRow = rowCount++;
            freeChatTranslationRow = rowCount++;
            translationProviderRow = rowCount++;
            aiInfoRow = rowCount++;
        } else if (currentSection == SECTION_NOTIFICATIONS) {
            notifServiceHeaderRow = rowCount++;
            persistentForegroundServiceRow = rowCount++;
            backgroundPushConnectionRow = rowCount++;
            notifServiceInfoRow = rowCount++;
            notifDividerRow = rowCount++;
            diagHeaderRow = rowCount++;
            diagPlayServicesRow = rowCount++;
            diagFcmStatusRow = rowCount++;
            diagFcmTokenRow = rowCount++;
            diagNotifPermRow = rowCount++;
            diagBatteryOptRow = rowCount++;
            diagServiceRunningRow = rowCount++;
            diagLastPushRow = rowCount++;
            diagRefreshPushRow = rowCount++;
            diagTestNotificationRow = rowCount++;
            diagSystemBatteryRow = rowCount++;
            diagInfoRow = rowCount++;
        }
    }

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);

        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        switch (currentSection) {
            case SECTION_PRIVACY:
                simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSectionPrivacyTitle));
                break;
            case SECTION_APPEARANCE:
                simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSectionAppearanceTitle));
                break;
            case SECTION_CHATS:
                simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSectionChatsTitle));
                break;
            case SECTION_AI:
                simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSectionAiTitle));
                break;
            case SECTION_NOTIFICATIONS:
                simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSectionNotifTitle));
                break;
            case SECTION_MAIN:
            default:
                simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSettingsTitle));
                break;
        }
        if (parentLayout != null && parentLayout.isRightLayout()) {
            simpleActionBar.setBackButtonImage(R.drawable.ic_ab_close);
        }
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        listView.setClipToPadding(false);
        listView.setPadding(0, AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight(), 0, 0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == privacyCategoryRow) {
                presentFragment(new KronoSettingsActivity(SECTION_PRIVACY));
            } else if (position == appearanceCategoryRow) {
                presentFragment(new KronoSettingsActivity(SECTION_APPEARANCE));
            } else if (position == chatsCategoryRow) {
                presentFragment(new KronoSettingsActivity(SECTION_CHATS));
            } else if (position == aiCategoryRow) {
                presentFragment(new KronoSettingsActivity(SECTION_AI));
            } else if (position == notificationsCategoryRow) {
                presentFragment(new KronoSettingsActivity(SECTION_NOTIFICATIONS));
            } else if (position == backupCategoryRow) {
                presentFragment(new KronoBackupActivity());
            } else if (position == ghostMasterRow) {
                boolean newState = !KronoConfig.ghostMode;
                KronoConfig.setGhostMode(newState);
                listAdapter.notifyItemRangeChanged(ghostMasterRow, 5);
            } else if (position == ghostDontReadMessagesRow) {
                KronoConfig.setDontReadMessages(!KronoConfig.dontReadMessages);
                listAdapter.notifyItemRangeChanged(ghostMasterRow, 5);
            } else if (position == ghostDontReadStoriesRow) {
                KronoConfig.setDontReadStories(!KronoConfig.dontReadStories);
                listAdapter.notifyItemRangeChanged(ghostMasterRow, 5);
            } else if (position == ghostDontSendTypingRow) {
                KronoConfig.setDontSendTyping(!KronoConfig.dontSendTyping);
                listAdapter.notifyItemRangeChanged(ghostMasterRow, 5);
            } else if (position == ghostDontSendOnlineRow) {
                KronoConfig.setDontSendOnline(!KronoConfig.dontSendOnline);
                listAdapter.notifyItemRangeChanged(ghostMasterRow, 5);
            } else if (position == hiddenVaultOpenRow) {
                KronoHiddenLockHelper.authenticate(this, LocaleController.getString(R.string.KronoVaultTitle), LocaleController.getString(R.string.KronoVaultOpenPrompt), () -> presentFragment(new KronoHiddenVaultActivity()));
            } else if (position == hiddenVaultLockMethodRow) {
                KronoHiddenLockHelper.showSetupLockDialog(this, () -> {
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
            } else if (position == hiddenVaultBiometricRow) {
                if (!KronoHiddenLockHelper.canUseBiometrics(getParentActivity())) {
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.fingerprint, LocaleController.getString(R.string.KronoVaultNoBioDevice)).show();
                } else {
                    KronoConfig.setHiddenUseBiometric(!KronoConfig.hiddenUseBiometric);
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(KronoConfig.hiddenUseBiometric);
                    }
                    if (listAdapter != null && hiddenVaultLockMethodRow >= 0) {
                        listAdapter.notifyItemChanged(hiddenVaultLockMethodRow);
                    }
                }
            } else if (position == hiddenVaultMuteNotifRow) {
                KronoConfig.setHiddenMuteNotifications(!KronoConfig.hiddenMuteNotifications);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hiddenMuteNotifications);
                }
            } else if (position == hiddenVaultHideFolderChatsRow) {
                KronoConfig.setHiddenHideFolderChatsEverywhere(!KronoConfig.hiddenHideFolderChatsEverywhere);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hiddenHideFolderChatsEverywhere);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
            } else if (position == hiddenVaultAutoLockRow) {
                KronoConfig.setHiddenAutoLockOnBackground(!KronoConfig.hiddenAutoLockOnBackground);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hiddenAutoLockOnBackground);
                }
            } else if (position == hidePhoneNumberRow) {
                KronoConfig.setHidePhoneNumber(!KronoConfig.hidePhoneNumber);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hidePhoneNumber);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            } else if (position == hideChatListNamesRow) {
                KronoConfig.setHideChatListNamesAvatars(!KronoConfig.hideChatListNamesAvatars);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hideChatListNamesAvatars);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
            } else if (position == blockSecretChatsRow) {
                KronoConfig.setBlockSecretChats(!KronoConfig.blockSecretChats);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.blockSecretChats);
                }
            } else if (position == antiDeleteMessagesRow) {
                KronoConfig.setAntiDeleteMessages(!KronoConfig.antiDeleteMessages);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.antiDeleteMessages);
                }
            } else if (position == antiDeleteExcludeChannelsRow) {
                KronoConfig.setAntiDeleteExcludeChannels(!KronoConfig.antiDeleteExcludeChannels);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.antiDeleteExcludeChannels);
                }
            } else if (position == unlimitedViewOnceRow) {
                KronoConfig.setUnlimitedViewOnce(!KronoConfig.unlimitedViewOnce);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.unlimitedViewOnce);
                }
            } else if (position == viewOnceAllowSaveRow) {
                KronoConfig.setViewOnceAllowSaveAndScreenshots(!KronoConfig.viewOnceAllowSaveAndScreenshots);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.viewOnceAllowSaveAndScreenshots);
                }
            } else if (position == allowRestrictedContentBypassRow) {
                KronoConfig.setAllowRestrictedContentBypass(!KronoConfig.allowRestrictedContentBypass);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.allowRestrictedContentBypass);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, 0);
            } else if (position == zeroTelemetryRow) {
                boolean newState = !KronoConfig.zeroTelemetry;
                KronoConfig.setZeroTelemetry(newState);
                listAdapter.notifyItemRangeChanged(zeroTelemetryRow, 5);
            } else if (position == blockTelegramTelemetryRow) {
                KronoConfig.setBlockTelegramTelemetry(!KronoConfig.blockTelegramTelemetry);
                listAdapter.notifyItemRangeChanged(zeroTelemetryRow, 5);
            } else if (position == disableFirebaseAnalyticsRow) {
                KronoConfig.setDisableFirebaseAnalytics(!KronoConfig.disableFirebaseAnalytics);
                listAdapter.notifyItemRangeChanged(zeroTelemetryRow, 5);
            } else if (position == blockSponsoredMessagesRow) {
                KronoConfig.setBlockSponsoredMessages(!KronoConfig.blockSponsoredMessages);
                listAdapter.notifyItemRangeChanged(zeroTelemetryRow, 5);
            } else if (position == stripUrlTrackersRow) {
                KronoConfig.setStripUrlTrackers(!KronoConfig.stripUrlTrackers);
                listAdapter.notifyItemRangeChanged(zeroTelemetryRow, 5);
            } else if (position == disableFirebasePushRow) {
                KronoConfig.setDisableFirebasePush(!KronoConfig.disableFirebasePush);
                KronoConfig.applyFirebasePushPreference();
                org.telegram.messenger.ApplicationLoader.reinitPushServices();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.disableFirebasePush);
                }
                BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.msg_notifications,
                        LocaleController.getString(KronoConfig.disableFirebasePush
                                ? R.string.KronoFcmDisabledToast
                                : R.string.KronoFcmEnabledToast)
                ).show();
            } else if (position == openThemesRow) {
                presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
            } else if (position == monetThemeRow) {
                showMonetThemeSelectorDialog();
            } else if (position == uiStyleRow) {
                showUiStyleSelectorDialog();
            } else if (position == glassModeRow) {
                showGlassModeSelectorDialog();
            } else if (position == glassSettingsBarRow) {
                if (KronoConfig.isGlassOff()) {
                    return;
                }
                KronoConfig.setGlassOnSettingsBar(!KronoConfig.glassOnSettingsBar);
                applyGlassSettingsChange(view instanceof TextCheckCell ? (TextCheckCell) view : null, KronoConfig.glassOnSettingsBar);
            } else if (position == glassSearchBarsRow) {
                if (KronoConfig.isGlassOff()) {
                    return;
                }
                KronoConfig.setGlassOnSearchBars(!KronoConfig.glassOnSearchBars);
                applyGlassSettingsChange(view instanceof TextCheckCell ? (TextCheckCell) view : null, KronoConfig.glassOnSearchBars);
            } else if (position == glassBottomBarRow) {
                if (KronoConfig.isGlassOff()) {
                    return;
                }
                KronoConfig.setGlassOnBottomBar(!KronoConfig.glassOnBottomBar);
                applyGlassSettingsChange(view instanceof TextCheckCell ? (TextCheckCell) view : null, KronoConfig.glassOnBottomBar);
            } else if (position == glassChatBarsRow) {
                if (KronoConfig.isGlassOff()) {
                    return;
                }
                KronoConfig.setGlassOnChatBars(!KronoConfig.glassOnChatBars);
                applyGlassSettingsChange(view instanceof TextCheckCell ? (TextCheckCell) view : null, KronoConfig.glassOnChatBars);
            } else if (position == disableTitleMarqueeRow) {
                KronoConfig.setDisableTitleMarquee(!KronoConfig.disableTitleMarquee);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.disableTitleMarquee);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
            } else if (position == smoothSplashAnimationRow) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    return;
                }
                KronoConfig.setSmoothSplashAnimation(!KronoConfig.smoothSplashAnimation);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.smoothSplashAnimation);
                }
            } else if (position == messageSwipeActionRow) {
                showMessageSwipeActionDialog();
            } else if (position == swipeToEditOwnRow) {
                KronoConfig.setSwipeToEditOwnMessage(!KronoConfig.swipeToEditOwnMessage);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.swipeToEditOwnMessage);
                }
            } else if (position == deleteForAllDefaultRow) {
                KronoConfig.setDeleteForAllByDefault(!KronoConfig.deleteForAllByDefault);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.deleteForAllByDefault);
                }
            } else if (position == showTimeWithSecondsRow) {
                KronoConfig.setShowTimeWithSeconds(!KronoConfig.showTimeWithSeconds);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.showTimeWithSeconds);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
            } else if (position == unlimitedPinnedChatsRow) {
                KronoConfig.setUnlimitedPinnedChats(!KronoConfig.unlimitedPinnedChats);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.unlimitedPinnedChats);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
            } else if (position == quickGoToMessageRow) {
                KronoConfig.setQuickGoToMessageInSearch(!KronoConfig.quickGoToMessageInSearch);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.quickGoToMessageInSearch);
                }
            } else if (position == longClickMediaMenuRow) {
                KronoConfig.setLongClickMediaShowsMenu(!KronoConfig.longClickMediaShowsMenu);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.longClickMediaShowsMenu);
                }
            } else if (position == disablePullToNextChannelRow) {
                KronoConfig.setDisablePullToNextChannel(!KronoConfig.disablePullToNextChannel);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.disablePullToNextChannel);
                }
            } else if (position == tabModeRow) {
                showTabModeDialog();
            } else if (position == tabStyleRow) {
                showTabStyleDialog();
            } else if (position == tabStyleStrokeRow) {
                KronoConfig.setTabStyleStroke(!KronoConfig.tabStyleStroke);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.tabStyleStroke);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == hideTabsCountersRow) {
                KronoConfig.setHideTabsCounters(!KronoConfig.hideTabsCounters);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hideTabsCounters);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == folderNameInHeaderRow) {
                KronoConfig.setFolderNameInHeader(!KronoConfig.folderNameInHeader);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.folderNameInHeader);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == disableTabsSwipeRow) {
                KronoConfig.setDisableTabsSwipe(!KronoConfig.disableTabsSwipe);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.disableTabsSwipe);
                }
                if (listAdapter != null && infiniteFolderScrollRow >= 0) {
                    listAdapter.notifyItemChanged(infiniteFolderScrollRow);
                }
            } else if (position == infiniteFolderScrollRow) {
                if (KronoConfig.disableTabsSwipe) {
                    return;
                }
                KronoConfig.setInfiniteFolderScroll(!KronoConfig.infiniteFolderScroll);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.infiniteFolderScroll);
                }
            } else if (position == showFavoritesFolderRow) {
                KronoConfig.toggleShowFavoritesFolder();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.showFavoritesFolder);
                }
                getMessagesController().syncLocalFavoritesFilter(true);
            } else if (position == hideAllChatsTabRow) {
                KronoConfig.toggleHideAllChatsTab();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.hideAllChatsTab);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == autoSendFolderLogsRow) {
                KronoConfig.setAutoSendFolderSyncLogs(!KronoConfig.autoSendFolderSyncLogs);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.autoSendFolderSyncLogs);
                }
            } else if (position == sendFolderLogsNowRow) {
                FolderSyncLogger.sendLogsToSavedMessages(
                        currentAccount,
                        true,
                        "MANUAL",
                        LocaleController.getString(R.string.KronoFolderLogsManualReason),
                        fileName -> {
                            if (listAdapter != null && sendFolderLogsNowRow >= 0) {
                                listAdapter.notifyItemChanged(sendFolderLogsNowRow);
                            }
                            BulletinFactory.of(this).createSimpleBulletin(
                                    R.drawable.msg_folders,
                                    LocaleController.getString(R.string.KronoFolderLogsSent)
                            ).show();
                        },
                        err -> BulletinFactory.of(this).createSimpleBulletin(
                                R.drawable.msg_info,
                                err
                        ).show()
                );
            } else if (position == spoilerWordsToggleRow) {
                KronoConfig.setSpoilerWordsEnabled(!KronoConfig.spoilerWordsEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.spoilerWordsEnabled);
                }
            } else if (position == spoilerWordsEditRow) {
                showSpoilerWordsDialog();
            } else if (position == keepOriginalFilenameRow) {
                KronoConfig.setKeepOriginalFilename(!KronoConfig.keepOriginalFilename);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.keepOriginalFilename);
                }
            } else if (position == clearCacheNowRow) {
                clearCacheWithConfirmation();
            } else if (position == openStorageUsageRow) {
                presentFragment(new CacheControlActivity());
            } else if (position == freeVoiceTranscriptionRow) {
                KronoConfig.setFreeVoiceTranscription(!KronoConfig.freeVoiceTranscription);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.freeVoiceTranscription);
                }
            } else if (position == voiceTranscriptionModeRow) {
                showVoiceTranscriptionModeDialog();
            } else if (position == voiceTranscriptionLanguageRow) {
                showVoiceTranscriptionLanguageDialog();
            } else if (position == voiceTranscriptionModelsRow) {
                showVoiceTranscriptionModelsDialog();
            } else if (position == freeChatTranslationRow) {
                KronoConfig.setFreeChatTranslation(!KronoConfig.freeChatTranslation);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(KronoConfig.freeChatTranslation);
                }
                getMessagesController().getTranslateController().reset();
                getNotificationCenter().postNotificationName(NotificationCenter.updateSearchSettings);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, 0);
            } else if (position == translationProviderRow) {
                showTranslationProviderDialog();
            } else if (position == persistentForegroundServiceRow) {
                boolean enabled = !KronoConfig.isPersistentForegroundService();
                KronoConfig.setPersistentForegroundService(enabled);
                ApplicationLoader.startPushService();
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager.getInstance(a).setPushConnectionEnabled(ConnectionsManager.getInstance(a).isPushConnectionEnabled());
                    }
                }
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
                BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.msg_notifications,
                        LocaleController.getString(enabled
                                ? R.string.KronoPersistentEnabled
                                : R.string.KronoPersistentDisabled)
                ).show();
            } else if (position == backgroundPushConnectionRow) {
                if (KronoConfig.isPersistentForegroundService()) {
                    return;
                }
                SharedPreferences globalPrefs = MessagesController.getGlobalNotificationsSettings();
                boolean current = globalPrefs.getBoolean("pushConnection", false);
                boolean next = !current;
                globalPrefs.edit().putBoolean("pushConnection", next).apply();
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (a == currentAccount || UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager.getInstance(a).setPushConnectionEnabled(next || KronoConfig.isPersistentForegroundService() || KronoConfig.isDisableFirebasePush());
                    }
                }
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
            } else if (position == diagFcmTokenRow || position == diagFcmStatusRow) {
                showPushDiagnosticDetailsDialog();
            } else if (position == diagNotifPermRow) {
                openAppNotificationSettings();
            } else if (position == diagBatteryOptRow) {
                requestIgnoreBatteryOptimizations();
            } else if (position == diagSystemBatteryRow) {
                openBatteryOptimizationSettings();
            } else if (position == diagRefreshPushRow) {
                forcePushTokenReregister();
            } else if (position == diagTestNotificationRow) {
                sendTestLocalNotification();
            }
        });

        return fragmentView;
    }

    private void showMonetThemeSelectorDialog() {
        if (getParentActivity() == null) {
            return;
        }
        Theme.ThemeInfo current = Theme.getCurrentTheme();
        String currentName = current != null ? current.name : "";
        final String[] themeKeys = new String[]{"Monet Light", "Monet Dark", "Monet AMOLED"};
        final CharSequence[] items = new CharSequence[]{
                ("Monet Light".equals(currentName) ? "✓ " : "") + LocaleController.getString(R.string.KronoMonetLight),
                ("Monet Dark".equals(currentName) ? "✓ " : "") + LocaleController.getString(R.string.KronoMonetDark),
                ("Monet AMOLED".equals(currentName) ? "✓ " : "") + LocaleController.getString(R.string.KronoMonetAmoled)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoMonetTheme));
        builder.setItems(items, (dialog, which) -> {
            String key = themeKeys[which];
            Theme.ThemeInfo themeInfo = Theme.getTheme(key);
            if (themeInfo != null) {
                applyThemeAndPersist(themeInfo, true);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true);
                if (parentLayout != null) {
                    parentLayout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST);
                }
                if (listAdapter != null && monetThemeRow >= 0) {
                    listAdapter.notifyItemChanged(monetThemeRow);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, LocaleController.formatString(R.string.KronoThemeApplied, key)).show();
            } else {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, LocaleController.formatString(R.string.KronoThemeLoadFailed, key)).show();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void applyThemeAndPersist(Theme.ThemeInfo themeInfo, boolean turnOffAutoNight) {
        if (themeInfo == null) {
            return;
        }
        SharedPreferences themeConfig = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);
        themeConfig.edit()
                .putString(themeInfo.isDark() ? "lastDarkTheme" : "lastDayTheme", themeInfo.getKey())
                .commit();
        Theme.applyTheme(themeInfo, false);
        EmojiThemes.saveCustomTheme(themeInfo, themeInfo.currentAccentId);
        if (turnOffAutoNight) {
            Theme.turnOffAutoNight(this);
        } else if (themeInfo.isDark() && Theme.selectedAutoNightType != Theme.AUTO_NIGHT_TYPE_NONE) {
            Theme.setCurrentNightTheme(themeInfo);
            Theme.saveAutoNightThemeConfig();
        }
    }

    private void showUiStyleSelectorDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.uiStyle == 0 ? "✓ " : "") + LocaleController.getString(R.string.KronoUiStyleMaterial),
                (KronoConfig.uiStyle == 1 ? "✓ " : "") + LocaleController.getString(R.string.KronoUiStyleIos),
                (KronoConfig.uiStyle == 2 ? "✓ " : "") + LocaleController.getString(R.string.KronoUiStyleClassic)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoUiStyleDialogTitle));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setUiStyle(which);
            int recommendedRadius = KronoConfig.getRecommendedBubbleRadius();
            if (SharedConfig.bubbleRadius != recommendedRadius) {
                SharedConfig.bubbleRadius = recommendedRadius;
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                preferences.edit().putInt("bubbleRadius", SharedConfig.bubbleRadius).apply();
            }

            Theme.refreshThemeColors();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
            if (listView != null) {
                listView.refreshSectionsStyle();
            }
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (parentLayout != null) {
                parentLayout.rebuildAllFragmentViews(true, true);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, LocaleController.formatString(R.string.KronoStyleApplied, KronoConfig.getUiStyleName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void applyGlassSettingsChange(TextCheckCell cell, boolean checked) {
        if (cell != null) {
            cell.setChecked(checked);
        }
        Theme.refreshThemeColors();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
        if (simpleActionBar != null) {
            simpleActionBar.updateColors();
        }
        if (listView != null) {
            listView.refreshSectionsStyle();
        }
        if (parentLayout != null) {
            parentLayout.rebuildAllFragmentViews(true, true);
        }
    }

    private void showGlassModeSelectorDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.glassMode == KronoConfig.GLASS_MODE_LIQUID ? "✓ " : "")
                        + (Build.VERSION.SDK_INT >= 33 ? LocaleController.getString(R.string.KronoGlassLiquidDesc) : "Liquid Glass (Specular)"),
                (KronoConfig.glassMode == KronoConfig.GLASS_MODE_FROSTED ? "✓ " : "") + LocaleController.getString(R.string.KronoGlassFrostedDesc),
                (KronoConfig.glassMode == KronoConfig.GLASS_MODE_OFF ? "✓ " : "") + LocaleController.getString(R.string.KronoGlassOff)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoGlassMode));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setGlassMode(which);
            applyGlassSettingsChange(null, false);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, LocaleController.formatString(R.string.KronoGlassApplied, KronoConfig.getGlassModeName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showVoiceTranscriptionModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.voiceTranscriptionMode == KronoConfig.VOICE_TRANSCRIBE_MODE_AUTO ? "✓ " : "") + LocaleController.getString(R.string.KronoVoiceModeAuto),
                (KronoConfig.voiceTranscriptionMode == KronoConfig.VOICE_TRANSCRIBE_MODE_LOCAL_ONLY ? "✓ " : "") + LocaleController.getString(R.string.KronoVoiceModeLocal)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoVoiceModeDialogTitle));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setVoiceTranscriptionMode(which);
            if (listAdapter != null && voiceTranscriptionModeRow >= 0) {
                listAdapter.notifyItemChanged(voiceTranscriptionModeRow);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, LocaleController.formatString(R.string.KronoModeApplied, KronoConfig.getVoiceTranscriptionModeName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showVoiceTranscriptionLanguageDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final String[] codes = org.telegram.messenger.KronoVoiceTranscriber.getSelectableLanguageCodes();
        final CharSequence[] items = new CharSequence[codes.length];
        for (int i = 0; i < codes.length; i++) {
            boolean selected = codes[i].equalsIgnoreCase(KronoConfig.voiceTranscriptionLanguage);
            String label = org.telegram.messenger.KronoVoiceTranscriber.getLanguageDisplayName(codes[i]);
            if (!"auto".equals(codes[i])) {
                org.telegram.messenger.KronoVoiceTranscriber.LanguageModelSpec spec = org.telegram.messenger.KronoVoiceTranscriber.getModelSpec(codes[i]);
                if (spec != null) {
                    label += " (" + spec.approxSize + ")";
                }
            }
            items[i] = (selected ? "✓ " : "") + label;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoVoiceLang));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setVoiceTranscriptionLanguage(codes[which]);
            if (listAdapter != null && voiceTranscriptionLanguageRow >= 0) {
                listAdapter.notifyItemChanged(voiceTranscriptionLanguageRow);
            }
            refreshVoiceModelsStatus();
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, LocaleController.formatString(R.string.KronoVoiceLangApplied, KronoConfig.getVoiceTranscriptionLanguageName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showVoiceTranscriptionModelsDialog() {
        if (getParentActivity() == null) {
            return;
        }
        org.telegram.messenger.KronoVoiceTranscriber.LanguageModelSpec spec = org.telegram.messenger.KronoVoiceTranscriber.getModelSpec(KronoConfig.voiceTranscriptionLanguage);
        String activeName = spec != null ? (org.telegram.messenger.KronoVoiceTranscriber.getLanguageDisplayName(spec.code) + " (" + spec.approxSize + ")") : KronoConfig.getVoiceTranscriptionLanguageName();
        boolean isDownloaded = org.telegram.messenger.KronoVoiceTranscriber.isModelDownloaded(KronoConfig.voiceTranscriptionLanguage);

        final CharSequence[] items = new CharSequence[]{
                LocaleController.formatString(isDownloaded ? R.string.KronoVoiceModelCheck : R.string.KronoVoiceModelDownloadNow, activeName),
                LocaleController.formatString(R.string.KronoVoiceModelDelete, voiceModelsStatusText)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoVoiceModels));
        builder.setItems(items, (dialog, which) -> {
            dialog.dismiss();
            if (which == 0) {
                voiceModelsStatusText = LocaleController.getString(R.string.KronoDownloading);
                if (listAdapter != null && voiceTranscriptionModelsRow >= 0) {
                    listAdapter.notifyItemChanged(voiceTranscriptionModelsRow);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, LocaleController.formatString(R.string.KronoDownloadingModel, activeName)).show();
                org.telegram.messenger.KronoVoiceTranscriber.downloadModelAsync(KronoConfig.voiceTranscriptionLanguage, (ok, msg) -> {
                    if (isFinishing() || getParentActivity() == null || fragmentView == null) {
                        return;
                    }
                    refreshVoiceModelsStatus();
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, msg).show();
                });
            } else {
                org.telegram.messenger.KronoVoiceTranscriber.deleteAllModels(() -> {
                    if (isFinishing() || getParentActivity() == null || fragmentView == null) {
                        return;
                    }
                    refreshVoiceModelsStatus();
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, LocaleController.getString(R.string.KronoVoiceModelsDeleted)).show();
                });
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTranslationProviderDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final int[] providerValues = new int[]{
                KronoConfig.TRANSLATE_PROVIDER_TELEGRAM,
                KronoConfig.TRANSLATE_PROVIDER_GOOGLE,
                KronoConfig.TRANSLATE_PROVIDER_YANDEX,
                KronoConfig.TRANSLATE_PROVIDER_LINGVA
        };
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_TELEGRAM ? "✓ " : "") + LocaleController.getString(R.string.KronoProviderTelegram),
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_GOOGLE ? "✓ " : "") + "Google Translate",
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_YANDEX ? "✓ " : "") + "Yandex Translate",
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_LINGVA ? "✓ " : "") + "Lingva Translate"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoTranslationProviderDialog));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setTranslationProvider(providerValues[which]);
            getMessagesController().getTranslateController().reset();
            getNotificationCenter().postNotificationName(NotificationCenter.updateSearchSettings);
            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, 0);
            if (listAdapter != null && translationProviderRow >= 0) {
                listAdapter.notifyItemChanged(translationProviderRow);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_translate, LocaleController.formatString(R.string.KronoProviderApplied, KronoConfig.getTranslationProviderName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showMessageSwipeActionDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.messageSwipeAction == 0 ? "✓ " : "") + LocaleController.getString(R.string.KronoMsgSwipeReply),
                (KronoConfig.messageSwipeAction == 1 ? "✓ " : "") + LocaleController.getString(R.string.KronoMsgSwipeEditReply),
                (KronoConfig.messageSwipeAction == 2 ? "✓ " : "") + LocaleController.getString(R.string.KronoMsgSwipeSave),
                (KronoConfig.messageSwipeAction == 3 ? "✓ " : "") + LocaleController.getString(R.string.KronoMsgSwipeTranslate),
                (KronoConfig.messageSwipeAction == 4 ? "✓ " : "") + LocaleController.getString(R.string.KronoMsgSwipeForward),
                (KronoConfig.messageSwipeAction == 5 ? "✓ " : "") + LocaleController.getString(R.string.KronoMsgSwipeCopy),
                (KronoConfig.messageSwipeAction == 6 ? "✓ " : "") + LocaleController.getString(R.string.KronoDisabled)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoMsgSwipeAction));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setMessageSwipeAction(which);
            if (listAdapter != null && messageSwipeActionRow >= 0) {
                listAdapter.notifyItemChanged(messageSwipeActionRow);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.menu_reply, LocaleController.formatString(R.string.KronoSwipeApplied, KronoConfig.getMessageSwipeActionName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTabModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.tabMode == KronoConfig.TAB_MODE_TEXT ? "✓ " : "") + LocaleController.getString(R.string.KronoTabModeTitleOnly),
                (KronoConfig.tabMode == KronoConfig.TAB_MODE_ICON_TEXT ? "✓ " : "") + LocaleController.getString(R.string.KronoTabModeIconTitle),
                (KronoConfig.tabMode == KronoConfig.TAB_MODE_ICON ? "✓ " : "") + LocaleController.getString(R.string.KronoTabModeIconOnly)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoTabMode));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setTabMode(which);
            if (listAdapter != null && tabModeRow >= 0) {
                listAdapter.notifyItemChanged(tabModeRow);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_folders, LocaleController.formatString(R.string.KronoTabsApplied, KronoConfig.getTabModeName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTabStyleDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_DEFAULT ? "✓ " : "") + LocaleController.getString(R.string.KronoDefault),
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_ROUNDED ? "✓ " : "") + LocaleController.getString(R.string.KronoTabStyleRounded),
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_PILL ? "✓ " : "") + LocaleController.getString(R.string.KronoTabStylePill),
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_VKUI ? "✓ " : "") + LocaleController.getString(R.string.KronoTabStyleVkui),
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_TEXT_ONLY ? "✓ " : "") + LocaleController.getString(R.string.KronoTabStyleTextOnly)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoTabStyle));
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setTabStyle(which);
            if (listAdapter != null && tabStyleRow >= 0) {
                listAdapter.notifyItemChanged(tabStyleRow);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, LocaleController.formatString(R.string.KronoTabStyleApplied, KronoConfig.getTabStyleName())).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showSpoilerWordsDialog() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.KronoSpoilerWordsDialogTitle));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(24);
        layout.setPadding(pad, AndroidUtilities.dp(12), pad, AndroidUtilities.dp(4));

        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setHintColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setBackground(Theme.createEditTextDrawable(context, true));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setHint(LocaleController.getString(R.string.KronoSpoilerWordsHint));
        editText.setText(KronoConfig.spoilerWordsRaw);
        editText.setSelection(editText.getText().length());
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            String raw = editText.getText().toString().trim();
            KronoConfig.setSpoilerWordsRaw(raw);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (!raw.isEmpty() && !KronoConfig.spoilerWordsEnabled) {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, LocaleController.getString(R.string.KronoSpoilerWordsSaved)).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void clearCacheWithConfirmation() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoClearCacheDialogTitle));
        builder.setMessage(LocaleController.formatString(R.string.KronoClearCacheDialogMsg, cacheSizeText));
        builder.setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
            cacheSizeText = LocaleController.getString(R.string.KronoClearing);
            if (listAdapter != null && clearCacheNowRow >= 0) {
                listAdapter.notifyItemChanged(clearCacheNowRow);
            }
            Utilities.globalQueue.postRunnable(() -> {
                long freed = KronoConfig.clearCacheFilesSync();
                long remaining = KronoConfig.calculateCacheSize();
                final String freedStr = AndroidUtilities.formatFileSize(freed);
                final String remainingStr = AndroidUtilities.formatFileSize(remaining);
                AndroidUtilities.runOnUIThread(() -> {
                    if (isFinishing() || getParentActivity() == null || fragmentView == null) {
                        return;
                    }
                    cacheSizeText = remainingStr;
                    if (listAdapter != null && clearCacheNowRow >= 0) {
                        listAdapter.notifyItemChanged(clearCacheNowRow);
                    }
                    BulletinFactory.of(KronoSettingsActivity.this)
                            .createSimpleBulletin(R.drawable.msg_delete, LocaleController.formatString(R.string.KronoCacheCleared, freedStr))
                            .show();
                });
            });
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || getParentActivity() == null) {
            return true;
        }
        try {
            PowerManager pm = (PowerManager) getParentActivity().getSystemService(Context.POWER_SERVICE);
            return pm == null || pm.isIgnoringBatteryOptimizations(getParentActivity().getPackageName());
        } catch (Throwable ignore) {
            return false;
        }
    }

    private boolean hasNotificationPermission() {
        if (getParentActivity() == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getParentActivity(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return NotificationManagerCompat.from(getParentActivity()).areNotificationsEnabled();
    }

    private void requestIgnoreBatteryOptimizations() {
        if (getParentActivity() == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getParentActivity().getPackageName()));
                getParentActivity().startActivity(intent);
                return;
            } catch (Throwable ignore) {
            }
        }
        openBatteryOptimizationSettings();
    }

    private void openBatteryOptimizationSettings() {
        if (getParentActivity() == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                getParentActivity().startActivity(intent);
                return;
            } catch (Throwable ignore) {
            }
        }
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getParentActivity().getPackageName()));
            getParentActivity().startActivity(intent);
        } catch (Throwable ignore) {
        }
    }

    private void openAppNotificationSettings() {
        if (getParentActivity() == null) {
            return;
        }
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getParentActivity().getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getParentActivity().getPackageName()));
            }
            getParentActivity().startActivity(intent);
        } catch (Throwable ignore) {
        }
    }

    private void forcePushTokenReregister() {
        if (KronoConfig.isDisableFirebasePush()) {
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    LocaleController.getString(R.string.KronoDiagFcmDisabledMsg)
            ).show();
            return;
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig uc = UserConfig.getInstance(a);
            if (uc.isClientActivated()) {
                uc.registeredForPush = false;
                uc.saveConfig(false);
            }
        }
        SharedConfig.pushString = "";
        SharedConfig.pushStringStatus = "__REFRESHING_TOKEN__";
        PushListenerController.lastTokenError = "";
        ApplicationLoader.reinitPushServices();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        BulletinFactory.of(this).createSimpleBulletin(
                R.drawable.msg_notifications,
                LocaleController.getString(R.string.KronoDiagRefreshingFcm)
        ).show();
        AndroidUtilities.runOnUIThread(() -> {
            if (listAdapter != null && currentSection == SECTION_NOTIFICATIONS) {
                listAdapter.notifyDataSetChanged();
            }
        }, 2500);
    }

    private void sendTestLocalNotification() {
        if (getParentActivity() == null) {
            return;
        }
        if (!hasNotificationPermission()) {
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    LocaleController.getString(R.string.KronoDiagNeedNotifPerm)
            ).show();
            openAppNotificationSettings();
            return;
        }
        try {
            NotificationsController.checkOtherNotificationsChannel();
            String channelId = NotificationsController.OTHER_NOTIFICATIONS_CHANNEL;
            if (TextUtils.isEmpty(channelId)) {
                channelId = "kronogram_test_channel";
            }
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getParentActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(channelId) == null) {
                    NotificationChannel ch = new NotificationChannel(channelId, LocaleController.getString(R.string.KronoDiagTestChannel), NotificationManager.IMPORTANCE_HIGH);
                    nm.createNotificationChannel(ch);
                }
            }
            Intent launchIntent = new Intent(getParentActivity(), LaunchActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                pFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(getParentActivity(), 0, launchIntent, pFlags);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getParentActivity(), channelId)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(LocaleController.getString(R.string.KronoDiagTestTitle))
                    .setContentText(LocaleController.getString(R.string.KronoDiagTestBody))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            NotificationManagerCompat.from(getParentActivity()).notify(49199, builder.build());
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_notifications,
                    LocaleController.getString(R.string.KronoDiagTestSent)
            ).show();
        } catch (Throwable e) {
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    LocaleController.formatString(R.string.KronoDiagTestError, e.getMessage() != null ? e.getMessage() : "")
            ).show();
        }
    }

    private void showPushDiagnosticDetailsDialog() {
        if (getParentActivity() == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean hasGms = ApplicationLoader.getPushProvider().hasServices();
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagPlayServices)).append(": ").append(LocaleController.getString(hasGms ? R.string.KronoAvailable : R.string.KronoUnavailable)).append("\n");
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagFcm)).append(": ").append(LocaleController.getString(KronoConfig.isDisableFirebasePush() ? R.string.KronoDiagDetailDisabledByUser : R.string.KronoDiagDetailEnabled)).append("\n");
        sb.append("• FCM Token: ").append(TextUtils.isEmpty(SharedConfig.pushString) ? LocaleController.getString(R.string.KronoDiagDetailNotGenerated) : SharedConfig.pushString).append("\n");
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagDetailInternalStatus)).append(": ").append(TextUtils.isEmpty(SharedConfig.pushStringStatus) ? "OK" : SharedConfig.pushStringStatus).append("\n");
        if (!TextUtils.isEmpty(PushListenerController.lastTokenError)) {
            sb.append("• ").append(LocaleController.getString(R.string.KronoDiagDetailLastFcmError)).append(": ").append(PushListenerController.lastTokenError).append("\n");
        }
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagDetailAccountRegistered)).append(": ").append(LocaleController.getString(UserConfig.getInstance(currentAccount).registeredForPush ? R.string.KronoYes : R.string.KronoNo)).append("\n");
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagDetailMtprotoPush)).append(": ").append(LocaleController.getString(ConnectionsManager.getInstance(currentAccount).isPushConnectionEnabled() ? R.string.KronoActive : R.string.KronoInactive)).append("\n");
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagServiceRunning)).append(": ").append(LocaleController.getString(NotificationsService.isRunningInForeground ? R.string.KronoRunningFg : R.string.KronoInactive)).append("\n");
        sb.append("• ").append(LocaleController.getString(R.string.KronoDiagDetailBatteryUnrestricted)).append(": ").append(LocaleController.getString(isBatteryOptimizationIgnored() ? R.string.KronoYes : R.string.KronoDiagDetailNoOptimized)).append("\n");
        if (PushListenerController.lastPushReceiveTime > 0) {
            String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(PushListenerController.lastPushReceiveTime));
            sb.append("• ").append(LocaleController.getString(R.string.KronoDiagLastPush)).append(": ").append(PushListenerController.lastPushType).append(" · ").append(ts);
        } else {
            sb.append("• ").append(LocaleController.getString(R.string.KronoDiagLastPush)).append(": ").append(LocaleController.getString(R.string.KronoDiagDetailNoneThisSession));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoDiagDetailTitle));
        builder.setMessage(sb.toString());
        if (!TextUtils.isEmpty(SharedConfig.pushString)) {
            builder.setNeutralButton(LocaleController.getString(R.string.KronoCopyToken), (d, w) -> {
                AndroidUtilities.addToClipboard(SharedConfig.pushString);
                BulletinFactory.of(KronoSettingsActivity.this).createSimpleBulletin(R.drawable.msg_copy, LocaleController.getString(R.string.KronoTokenCopied)).show();
            });
        }
        builder.setPositiveButton(LocaleController.getString(R.string.KronoReRegister), (d, w) -> forcePushTokenReregister());
        builder.setNegativeButton(LocaleController.getString(R.string.Close), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == diagPlayServicesRow || position == diagServiceRunningRow || position == diagLastPushRow) {
                return false;
            }
            if (position == hiddenVaultBiometricRow && !KronoHiddenLockHelper.canUseBiometrics(mContext)) {
                return false;
            }
            if (position == infiniteFolderScrollRow && KronoConfig.disableTabsSwipe) {
                return false;
            }
            if (position == backgroundPushConnectionRow && KronoConfig.isPersistentForegroundService()) {
                return false;
            }
            if ((position == glassSettingsBarRow || position == glassSearchBarsRow
                    || position == glassBottomBarRow || position == glassChatBarsRow) && KronoConfig.isGlassOff()) {
                return false;
            }
            if (position == smoothSplashAnimationRow && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return false;
            }
            int type = holder.getItemViewType();
            return type == 1 || type == 2 || type == 3;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == categoriesHeaderRow || position == ghostHeaderRow || position == hiddenVaultHeaderRow || position == privacyExtraHeaderRow || position == telemetryHeaderRow ||
                position == themeHeaderRow || position == tabsHeaderRow || position == chatsHeaderRow || position == storageHeaderRow || position == aiHeaderRow ||
                position == notifServiceHeaderRow || position == diagHeaderRow) {
                return 0; // HeaderCell
            } else if (position == privacyCategoryRow || position == appearanceCategoryRow ||
                       position == chatsCategoryRow || position == aiCategoryRow ||
                       position == notificationsCategoryRow || position == backupCategoryRow) {
                return 1; // TextCell with icon
            } else if (position == ghostMasterRow || position == ghostDontReadMessagesRow ||
                       position == ghostDontReadStoriesRow || position == ghostDontSendTypingRow ||
                       position == ghostDontSendOnlineRow || position == hiddenVaultBiometricRow ||
                       position == hiddenVaultMuteNotifRow || position == hiddenVaultHideFolderChatsRow ||
                       position == hiddenVaultAutoLockRow || position == hidePhoneNumberRow ||
                       position == hideChatListNamesRow || position == blockSecretChatsRow ||
                       position == antiDeleteMessagesRow || position == antiDeleteExcludeChannelsRow ||
                       position == unlimitedViewOnceRow || position == viewOnceAllowSaveRow || position == allowRestrictedContentBypassRow ||
                       position == zeroTelemetryRow || position == blockTelegramTelemetryRow ||
                       position == disableFirebaseAnalyticsRow || position == blockSponsoredMessagesRow ||
                       position == stripUrlTrackersRow || position == disableFirebasePushRow ||
                       position == tabStyleStrokeRow || position == hideTabsCountersRow ||
                       position == folderNameInHeaderRow || position == disableTabsSwipeRow ||
                       position == infiniteFolderScrollRow || position == swipeToEditOwnRow ||
                       position == deleteForAllDefaultRow || position == showTimeWithSecondsRow ||
                       position == unlimitedPinnedChatsRow || position == quickGoToMessageRow ||
                       position == longClickMediaMenuRow || disablePullToNextChannelRow == position ||
                       position == showFavoritesFolderRow || position == hideAllChatsTabRow || position == autoSendFolderLogsRow || position == spoilerWordsToggleRow ||
                       position == keepOriginalFilenameRow || position == freeVoiceTranscriptionRow || position == freeChatTranslationRow ||
                       position == persistentForegroundServiceRow || position == backgroundPushConnectionRow ||
                       position == glassSettingsBarRow || position == glassSearchBarsRow ||
                       position == glassBottomBarRow || position == glassChatBarsRow ||
                       position == disableTitleMarqueeRow || position == smoothSplashAnimationRow) {
                return 2; // TextCheckCell
            } else if (position == hiddenVaultOpenRow || position == hiddenVaultLockMethodRow ||
                       position == openThemesRow || position == monetThemeRow || position == uiStyleRow ||
                       position == glassModeRow ||
                       position == tabModeRow || position == tabStyleRow ||
                       position == sendFolderLogsNowRow ||
                       position == messageSwipeActionRow ||
                       position == spoilerWordsEditRow || position == clearCacheNowRow ||
                       position == openStorageUsageRow || position == voiceTranscriptionModeRow ||
                       position == voiceTranscriptionLanguageRow || position == voiceTranscriptionModelsRow ||
                       position == translationProviderRow ||
                       position == diagPlayServicesRow || position == diagFcmStatusRow ||
                       position == diagFcmTokenRow || position == diagNotifPermRow ||
                       position == diagBatteryOptRow || position == diagServiceRunningRow ||
                       position == diagLastPushRow || position == diagRefreshPushRow ||
                       position == diagTestNotificationRow || position == diagSystemBatteryRow) {
                return 3; // TextSettingsCell
            } else if (position == tabsDividerRow || position == chatsDividerRow || position == notifDividerRow) {
                return 5; // ShadowSectionCell
            }
            return 4; // TextInfoPrivacyCell
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            final boolean flatClassic = KronoConfig.isTelegramXStyle();
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    if (flatClassic) {
                        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    }
                    break;
                case 1:
                    view = new SettingsActivity.SettingCell(mContext, getResourceProvider());
                    if (flatClassic) {
                        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    }
                    break;
                case 2:
                    view = new TextCheckCell(mContext);
                    if (flatClassic) {
                        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    }
                    break;
                case 3:
                    view = new TextSettingsCell(mContext);
                    if (flatClassic) {
                        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    }
                    break;
                case 5:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 4:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int type = holder.getItemViewType();
            if (type == 0) {
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                if (position == categoriesHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderSettings));
                } else if (position == ghostHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderGhost));
                } else if (position == hiddenVaultHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderHiddenVault));
                } else if (position == privacyExtraHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderPrivacyExtra));
                } else if (position == telemetryHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderTelemetry));
                } else if (position == themeHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderThemes));
                } else if (position == tabsHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderTabs));
                } else if (position == chatsHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderChats));
                } else if (position == storageHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderStorage));
                } else if (position == aiHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderAi));
                } else if (position == notifServiceHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderNotifService));
                } else if (position == diagHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoHeaderDiag));
                }
            } else if (type == 1) {
                SettingsActivity.SettingCell settingCell = (SettingsActivity.SettingCell) holder.itemView;
                if (position == privacyCategoryRow) {
                    settingCell.set(0xFF28B9F2, 0xFF0A98E6, R.drawable.settings_privacy, LocaleController.getString(R.string.KronoSectionPrivacyTitle), LocaleController.getString(R.string.KronoSectionPrivacySubtitle), null);
                } else if (position == appearanceCategoryRow) {
                    settingCell.set(0xFFF34A71, 0xFFDC284E, R.drawable.settings_chat, LocaleController.getString(R.string.KronoSectionAppearanceShort), LocaleController.getString(R.string.KronoSectionAppearanceSubtitle), null);
                } else if (position == chatsCategoryRow) {
                    settingCell.set(0xFFF39C27, 0xFFE37810, R.drawable.settings_folders, LocaleController.getString(R.string.KronoSectionChatsShort), LocaleController.getString(R.string.KronoSectionChatsSubtitle), null);
                } else if (position == aiCategoryRow) {
                    settingCell.set(0xFF9663FA, 0xFF7244E6, R.drawable.settings_language, LocaleController.getString(R.string.KronoSectionAiTitle), LocaleController.getString(R.string.KronoSectionAiSubtitle), null);
                } else if (position == notificationsCategoryRow) {
                    settingCell.set(0xFFFA5858, 0xFFE3324B, R.drawable.settings_sounds, LocaleController.getString(R.string.KronoSectionNotifTitle), LocaleController.getString(R.string.KronoSectionNotifSubtitle), null);
                } else if (position == backupCategoryRow) {
                    settingCell.set(0xFF19B5C9, 0xFF0E88A6, R.drawable.settings_data, LocaleController.getString(R.string.KronoSectionBackupTitle), LocaleController.getString(R.string.KronoSectionBackupSubtitle), null);
                }
            } else if (type == 2) {
                TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                if (position == ghostMasterRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoGhostMaster), LocaleController.getString(R.string.KronoGhostMasterDesc), KronoConfig.ghostMode, true, true);
                } else if (position == ghostDontReadMessagesRow) {
                    checkCell.setTextAndCheck(LocaleController.getString(R.string.KronoGhostDontReadMsg), KronoConfig.isGhostDontReadMessages(), true);
                } else if (position == ghostDontReadStoriesRow) {
                    checkCell.setTextAndCheck(LocaleController.getString(R.string.KronoGhostDontReadStories), KronoConfig.isGhostDontReadStories(), true);
                } else if (position == ghostDontSendTypingRow) {
                    checkCell.setTextAndCheck(LocaleController.getString(R.string.KronoGhostDontSendTyping), KronoConfig.isGhostDontSendTyping(), true);
                } else if (position == ghostDontSendOnlineRow) {
                    checkCell.setTextAndCheck(LocaleController.getString(R.string.KronoGhostDontSendOnline), KronoConfig.isGhostDontSendOnline(), false);
                } else if (position == hiddenVaultBiometricRow) {
                    boolean bioReady = KronoHiddenLockHelper.canUseBiometrics(mContext);
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoVaultBiometric), LocaleController.getString(R.string.KronoVaultBiometricDesc), bioReady && KronoConfig.hiddenUseBiometric, true, true);
                    checkCell.setEnabled(bioReady, null);
                } else if (position == hiddenVaultMuteNotifRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoVaultMuteNotif), LocaleController.getString(R.string.KronoVaultMuteNotifDesc), KronoConfig.hiddenMuteNotifications, true, true);
                } else if (position == hiddenVaultHideFolderChatsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoVaultHideFolderChats), LocaleController.getString(R.string.KronoVaultHideFolderChatsDesc), KronoConfig.hiddenHideFolderChatsEverywhere, true, true);
                } else if (position == hiddenVaultAutoLockRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoVaultAutoLock), LocaleController.getString(R.string.KronoVaultAutoLockDesc), KronoConfig.hiddenAutoLockOnBackground, true, false);
                } else if (position == hidePhoneNumberRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoHidePhone), LocaleController.getString(R.string.KronoHidePhoneDesc), KronoConfig.hidePhoneNumber, true, true);
                } else if (position == hideChatListNamesRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoHideChatNames), LocaleController.getString(R.string.KronoHideChatNamesDesc), KronoConfig.hideChatListNamesAvatars, true, true);
                } else if (position == blockSecretChatsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoBlockSecretChats), LocaleController.getString(R.string.KronoBlockSecretChatsDesc), KronoConfig.blockSecretChats, true, true);
                } else if (position == antiDeleteMessagesRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoAntiDelete), LocaleController.getString(R.string.KronoAntiDeleteDesc), KronoConfig.antiDeleteMessages, true, true);
                } else if (position == antiDeleteExcludeChannelsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoAntiDeleteNoChannels), LocaleController.getString(R.string.KronoAntiDeleteNoChannelsDesc), KronoConfig.antiDeleteExcludeChannels, true, true);
                } else if (position == unlimitedViewOnceRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoUnlimitedViewOnce), LocaleController.getString(R.string.KronoUnlimitedViewOnceDesc), KronoConfig.unlimitedViewOnce, true, true);
                } else if (position == viewOnceAllowSaveRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoViewOnceSave), LocaleController.getString(R.string.KronoViewOnceSaveDesc), KronoConfig.viewOnceAllowSaveAndScreenshots, true, true);
                } else if (position == allowRestrictedContentBypassRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoBypassRestricted), LocaleController.getString(R.string.KronoBypassRestrictedDesc), KronoConfig.allowRestrictedContentBypass, true, false);
                } else if (position == zeroTelemetryRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoZeroTelemetry), LocaleController.getString(R.string.KronoZeroTelemetryDesc), KronoConfig.zeroTelemetry, true, true);
                } else if (position == blockTelegramTelemetryRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoBlockTgTelemetry), LocaleController.getString(R.string.KronoBlockTgTelemetryDesc), KronoConfig.isBlockTelegramTelemetry(), true, true);
                } else if (position == disableFirebaseAnalyticsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoDisableFirebaseAnalytics), LocaleController.getString(R.string.KronoDisableFirebaseAnalyticsDesc), KronoConfig.isDisableFirebaseAnalytics(), true, true);
                } else if (position == blockSponsoredMessagesRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoBlockSponsored), LocaleController.getString(R.string.KronoBlockSponsoredDesc), KronoConfig.isBlockSponsoredMessages(), true, true);
                } else if (position == stripUrlTrackersRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoStripUrlTrackers), LocaleController.getString(R.string.KronoStripUrlTrackersDesc), KronoConfig.isStripUrlTrackers(), true, true);
                } else if (position == disableFirebasePushRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoDisableFcm), LocaleController.getString(R.string.KronoDisableFcmDesc), KronoConfig.isDisableFirebasePush(), true, false);
                } else if (position == tabStyleStrokeRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoTabStroke), LocaleController.getString(R.string.KronoTabStrokeDesc), KronoConfig.tabStyleStroke, true, true);
                } else if (position == hideTabsCountersRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoHideTabCounters), LocaleController.getString(R.string.KronoHideTabCountersDesc), KronoConfig.hideTabsCounters, true, true);
                } else if (position == folderNameInHeaderRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoFolderNameHeader), LocaleController.getString(R.string.KronoFolderNameHeaderDesc), KronoConfig.folderNameInHeader, true, true);
                } else if (position == disableTabsSwipeRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoDisableTabSwipe), LocaleController.getString(R.string.KronoDisableTabSwipeDesc), KronoConfig.disableTabsSwipe, true, true);
                } else if (position == infiniteFolderScrollRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoInfiniteFolderScroll), LocaleController.getString(R.string.KronoInfiniteFolderScrollDesc), KronoConfig.infiniteFolderScroll, true, true);
                    checkCell.setEnabled(!KronoConfig.disableTabsSwipe, null);
                } else if (position == swipeToEditOwnRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoSwipeEditOwn), LocaleController.getString(R.string.KronoSwipeEditOwnDesc), KronoConfig.swipeToEditOwnMessage, true, true);
                } else if (position == deleteForAllDefaultRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoDeleteForAll), LocaleController.getString(R.string.KronoDeleteForAllDesc), KronoConfig.deleteForAllByDefault, true, true);
                } else if (position == showTimeWithSecondsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoTimeWithSeconds), LocaleController.getString(R.string.KronoTimeWithSecondsDesc), KronoConfig.showTimeWithSeconds, true, true);
                } else if (position == unlimitedPinnedChatsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoUnlimitedPins), LocaleController.getString(R.string.KronoUnlimitedPinsDesc), KronoConfig.unlimitedPinnedChats, true, true);
                } else if (position == quickGoToMessageRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoQuickGoToMessage), LocaleController.getString(R.string.KronoQuickGoToMessageDesc), KronoConfig.quickGoToMessageInSearch, true, true);
                } else if (position == longClickMediaMenuRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoLongClickMediaMenu), LocaleController.getString(R.string.KronoLongClickMediaMenuDesc), KronoConfig.longClickMediaShowsMenu, true, true);
                } else if (position == disablePullToNextChannelRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoDisableNextChannel), LocaleController.getString(R.string.KronoDisableNextChannelDesc), KronoConfig.disablePullToNextChannel, true, true);
                } else if (position == showFavoritesFolderRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoShowFavorites), LocaleController.getString(R.string.KronoShowFavoritesDesc), KronoConfig.showFavoritesFolder, true, true);
                } else if (position == hideAllChatsTabRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoHideAllChatsTab), LocaleController.getString(R.string.KronoHideAllChatsTabDesc), KronoConfig.hideAllChatsTab, true, true);
                } else if (position == autoSendFolderLogsRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoAutoSendFolderLogs), LocaleController.getString(R.string.KronoAutoSendFolderLogsDesc), KronoConfig.autoSendFolderSyncLogs, true, true);
                } else if (position == spoilerWordsToggleRow) {
                    checkCell.setTextAndCheck(LocaleController.getString(R.string.KronoSpoilerWordsToggle), KronoConfig.spoilerWordsEnabled, true);
                } else if (position == keepOriginalFilenameRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoKeepOriginalFilename), LocaleController.getString(R.string.KronoKeepOriginalFilenameDesc), KronoConfig.keepOriginalFilename, true, true);
                } else if (position == freeVoiceTranscriptionRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoFreeVoice), LocaleController.getString(R.string.KronoFreeVoiceDesc), KronoConfig.freeVoiceTranscription, true, true);
                } else if (position == freeChatTranslationRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoFreeChatTranslation), LocaleController.getString(R.string.KronoFreeChatTranslationDesc), KronoConfig.freeChatTranslation, true, true);
                } else if (position == persistentForegroundServiceRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoPersistentService), LocaleController.getString(R.string.KronoPersistentServiceDesc), KronoConfig.isPersistentForegroundService(), true, true);
                } else if (position == backgroundPushConnectionRow) {
                    boolean persistent = KronoConfig.isPersistentForegroundService();
                    boolean bgConn = persistent || MessagesController.getGlobalNotificationsSettings().getBoolean("pushConnection", false);
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoBgTcp), LocaleController.getString(R.string.KronoBgTcpDesc), bgConn, true, false);
                    checkCell.setEnabled(!persistent, null);
                } else if (position == glassSettingsBarRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoGlassSettingsBar), LocaleController.getString(R.string.KronoGlassSettingsBarDesc), glassMasterOn && KronoConfig.glassOnSettingsBar, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == glassSearchBarsRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoGlassSearchBars), LocaleController.getString(R.string.KronoGlassSearchBarsDesc), glassMasterOn && KronoConfig.glassOnSearchBars, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == glassBottomBarRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoGlassBottomBar), LocaleController.getString(R.string.KronoGlassBottomBarDesc), glassMasterOn && KronoConfig.glassOnBottomBar, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == glassChatBarsRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoGlassChatBars), LocaleController.getString(R.string.KronoGlassChatBarsDesc), glassMasterOn && KronoConfig.glassOnChatBars, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == disableTitleMarqueeRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoDisableMarquee), LocaleController.getString(R.string.KronoDisableMarqueeDesc), KronoConfig.disableTitleMarquee, true, true);
                } else if (position == smoothSplashAnimationRow) {
                    final boolean splashSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
                    checkCell.setTextAndValueAndCheck(
                            LocaleController.getString(R.string.KronoSmoothSplashAnimation),
                            LocaleController.getString(R.string.KronoSmoothSplashAnimationDesc),
                            splashSupported && KronoConfig.smoothSplashAnimation,
                            true,
                            false
                    );
                    checkCell.setEnabled(splashSupported, null);
                }
            } else if (type == 3) {
                TextSettingsCell settingsCell = (TextSettingsCell) holder.itemView;
                if (position == hiddenVaultOpenRow) {
                    int chatsCount = KronoConfig.getHiddenDialogs(currentAccount).size();
                    int foldersCount = KronoConfig.getHiddenFilterIds(currentAccount).size();
                    String summary = (chatsCount == 0 && foldersCount == 0)
                            ? LocaleController.getString(R.string.KronoVaultEmpty)
                            : LocaleController.formatString(R.string.KronoVaultSummary, chatsCount, foldersCount);
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoVaultOpen), summary, true);
                } else if (position == hiddenVaultLockMethodRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoVaultLockMethod), KronoConfig.getHiddenLockSummary(KronoHiddenLockHelper.canUseBiometrics(mContext)), true);
                } else if (position == openThemesRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoHeaderThemes), LocaleController.getString(R.string.KronoOpenThemesDesc), true);
                } else if (position == monetThemeRow) {
                    Theme.ThemeInfo cur = Theme.getCurrentTheme();
                    String curName = cur != null && !android.text.TextUtils.isEmpty(cur.name) ? cur.name : LocaleController.getString(R.string.KronoDefault);
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoMonetTheme), curName, true);
                } else if (position == uiStyleRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoUiStyle), KronoConfig.getUiStyleName(), true);
                } else if (position == glassModeRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoGlassMode), KronoConfig.getGlassModeName(), true);
                } else if (position == tabModeRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoTabMode), KronoConfig.getTabModeName(), true);
                } else if (position == tabStyleRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoTabStyle), KronoConfig.getTabStyleName(), true);
                } else if (position == sendFolderLogsNowRow) {
                    String lastFail = FolderSyncLogger.getLastFailureSummary(currentAccount);
                    int events = FolderSyncLogger.getBufferedEventCount();
                    String summary = lastFail != null
                            ? LocaleController.formatString(R.string.KronoFolderLogsFailure, lastFail)
                            : (events > 0 ? LocaleController.formatString(R.string.KronoFolderLogsEvents, events) : LocaleController.getString(R.string.KronoSendNow));
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoSendFolderLogsNow), summary, false);
                } else if (position == messageSwipeActionRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoMsgSwipeAction), KronoConfig.getMessageSwipeActionName(), true);
                } else if (position == spoilerWordsEditRow) {
                    int count = KronoConfig.getSpoilerWordsList().size();
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoSpoilerWordsEdit), count == 0 ? LocaleController.getString(R.string.KronoSpoilerWordsNone) : LocaleController.formatString(R.string.KronoSpoilerWordsCount, count), false);
                } else if (position == clearCacheNowRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoClearCacheNow), cacheSizeText, true);
                } else if (position == openStorageUsageRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoOpenStorageUsage), LocaleController.getString(R.string.KronoOpen), false);
                } else if (position == voiceTranscriptionModeRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoVoiceMode), KronoConfig.getVoiceTranscriptionModeName(), true);
                } else if (position == voiceTranscriptionLanguageRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoVoiceLang), KronoConfig.getVoiceTranscriptionLanguageName(), true);
                } else if (position == voiceTranscriptionModelsRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoVoiceModels), voiceModelsStatusText, true);
                } else if (position == translationProviderRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoTranslationProvider), KronoConfig.getTranslationProviderName(), false);
                } else if (position == diagPlayServicesRow) {
                    boolean hasGms = ApplicationLoader.getPushProvider().hasServices();
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagPlayServices), LocaleController.getString(hasGms ? R.string.KronoAvailable : R.string.KronoUnavailable), true);
                } else if (position == diagFcmStatusRow) {
                    String fcmState;
                    if (KronoConfig.isDisableFirebasePush()) {
                        fcmState = LocaleController.getString(R.string.KronoDisabled);
                    } else if (!TextUtils.isEmpty(PushListenerController.lastTokenError) || "__FIREBASE_FAILED__".equals(SharedConfig.pushStringStatus)) {
                        fcmState = LocaleController.getString(R.string.KronoErrorDetails);
                    } else {
                        fcmState = LocaleController.getString(R.string.KronoActive);
                    }
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagFcm), fcmState, true);
                } else if (position == diagFcmTokenRow) {
                    String tokenSummary;
                    if (KronoConfig.isDisableFirebasePush()) {
                        tokenSummary = LocaleController.getString(R.string.KronoDisabled);
                    } else if (TextUtils.isEmpty(SharedConfig.pushString)) {
                        tokenSummary = LocaleController.getString("__FIREBASE_FAILED__".equals(SharedConfig.pushStringStatus) ? R.string.KronoTokenFetchErr : R.string.KronoTokenPending);
                    } else {
                        String tail = SharedConfig.pushString.length() > 6
                                ? SharedConfig.pushString.substring(SharedConfig.pushString.length() - 6)
                                : SharedConfig.pushString;
                        boolean reg = UserConfig.getInstance(currentAccount).registeredForPush;
                        tokenSummary = LocaleController.formatString(reg ? R.string.KronoTokenRegistered : R.string.KronoTokenUnregistered, tail);
                    }
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagFcmToken), tokenSummary, true);
                } else if (position == diagNotifPermRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagNotifPerm), LocaleController.getString(hasNotificationPermission() ? R.string.KronoGranted : R.string.KronoDeniedOpen), true);
                } else if (position == diagBatteryOptRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagBatteryOpt), LocaleController.getString(isBatteryOptimizationIgnored() ? R.string.KronoUnrestricted : R.string.KronoOptimizedDisable), true);
                } else if (position == diagServiceRunningRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagServiceRunning), LocaleController.getString(NotificationsService.isRunningInForeground ? R.string.KronoRunningFg : R.string.KronoInactive), true);
                } else if (position == diagLastPushRow) {
                    String lastPush;
                    if (PushListenerController.lastPushReceiveTime > 0) {
                        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(PushListenerController.lastPushReceiveTime));
                        lastPush = PushListenerController.lastPushType + " · " + ts;
                    } else {
                        lastPush = LocaleController.getString(R.string.KronoNoneSession);
                    }
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagLastPush), lastPush, true);
                } else if (position == diagRefreshPushRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagRefreshPush), LocaleController.getString(R.string.KronoRun), true);
                } else if (position == diagTestNotificationRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagTestNotif), LocaleController.getString(R.string.KronoTest), true);
                } else if (position == diagSystemBatteryRow) {
                    settingsCell.setTextAndValue(LocaleController.getString(R.string.KronoDiagSystemBattery), LocaleController.getString(R.string.KronoOpen), false);
                }
            } else if (type == 4) {
                TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                if (position == ghostInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoGhostInfo));
                } else if (position == hiddenVaultInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoVaultInfo));
                } else if (position == privacyExtraInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoPrivacyExtraInfo));
                } else if (position == telemetryInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoTelemetryInfo));
                } else if (position == themeInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoThemeInfo));
                } else if (position == storageInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoStorageInfo));
                } else if (position == aiInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoAiInfo));
                } else if (position == notifServiceInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoNotifServiceInfo));
                } else if (position == diagInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoDiagInfo));
                }
            }
        }
    }
}
