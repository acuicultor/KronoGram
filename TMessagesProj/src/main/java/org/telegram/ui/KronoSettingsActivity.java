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

    private String cacheSizeText = "Calculando...";
    private String voiceModelsStatusText = "Comprobando...";

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
                label = "Sin descargar";
            } else {
                String formatted = AndroidUtilities.formatFileSize(size);
                label = activeReady ? ("Listo (" + formatted + ")") : formatted;
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
                simpleActionBar.setTitle("Privacidad y Seguridad");
                break;
            case SECTION_APPEARANCE:
                simpleActionBar.setTitle("Apariencia y Temas");
                break;
            case SECTION_CHATS:
                simpleActionBar.setTitle("Chats, Carpetas y Caché");
                break;
            case SECTION_AI:
                simpleActionBar.setTitle("Voz y Traducción");
                break;
            case SECTION_NOTIFICATIONS:
                simpleActionBar.setTitle("Notificaciones y Diagnóstico");
                break;
            case SECTION_MAIN:
            default:
                simpleActionBar.setTitle("Ajustes de KronoGram");
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
                KronoHiddenLockHelper.authenticate(this, "Bóveda oculta", "Verifica tu identidad para abrir la bóveda", () -> presentFragment(new KronoHiddenVaultActivity()));
            } else if (position == hiddenVaultLockMethodRow) {
                KronoHiddenLockHelper.showSetupLockDialog(this, () -> {
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
            } else if (position == hiddenVaultBiometricRow) {
                if (!KronoHiddenLockHelper.canUseBiometrics(getParentActivity())) {
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.fingerprint, "Tu dispositivo no tiene biometría configurada").show();
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
                        KronoConfig.disableFirebasePush
                                ? "Push de Google (FCM) desactivado: usando servicio en segundo plano"
                                : "Push de Google (FCM) reactivado"
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
                        enabled
                                ? "Servicio persistente anti-cierre activado"
                                : "Servicio persistente anti-cierre desactivado"
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
                ("Monet Light".equals(currentName) ? "✓ " : "") + "Monet Light (Claro dinámico)",
                ("Monet Dark".equals(currentName) ? "✓ " : "") + "Monet Dark (Oscuro dinámico)",
                ("Monet AMOLED".equals(currentName) ? "✓ " : "") + "Monet AMOLED (Negro puro)"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Tema Monet (Material You)");
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
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, "Tema aplicado: " + key).show();
            } else {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, "No se pudo cargar el tema " + key).show();
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
                (KronoConfig.uiStyle == 0 ? "✓ " : "") + "Material You (Material 3 / Monet)",
                (KronoConfig.uiStyle == 1 ? "✓ " : "") + "Estilo iOS",
                (KronoConfig.uiStyle == 2 ? "✓ " : "") + "Estilo Clásico"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Estilo visual de componentes");
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
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, "Estilo aplicado: " + KronoConfig.getUiStyleName()).show();
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
                        + (Build.VERSION.SDK_INT >= 33 ? "Liquid Glass (Refracción y brillo)" : "Liquid Glass (Specular)"),
                (KronoConfig.glassMode == KronoConfig.GLASS_MODE_FROSTED ? "✓ " : "") + "Frosted Glass (Esmerilado)",
                (KronoConfig.glassMode == KronoConfig.GLASS_MODE_OFF ? "✓ " : "") + "Desactivado (Sólido)"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Efecto de cristal (Glass)");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setGlassMode(which);
            applyGlassSettingsChange(null, false);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, "Modo Glass: " + KronoConfig.getGlassModeName()).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showVoiceTranscriptionModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.voiceTranscriptionMode == KronoConfig.VOICE_TRANSCRIBE_MODE_AUTO ? "✓ " : "") + "Automático (Telegram gratis → Local offline)",
                (KronoConfig.voiceTranscriptionMode == KronoConfig.VOICE_TRANSCRIBE_MODE_LOCAL_ONLY ? "✓ " : "") + "Siempre local offline (sin servidores)"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Modo de transcripción de voz");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setVoiceTranscriptionMode(which);
            if (listAdapter != null && voiceTranscriptionModeRow >= 0) {
                listAdapter.notifyItemChanged(voiceTranscriptionModeRow);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, "Modo: " + KronoConfig.getVoiceTranscriptionModeName()).show();
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
        builder.setTitle("Idioma de transcripción local");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setVoiceTranscriptionLanguage(codes[which]);
            if (listAdapter != null && voiceTranscriptionLanguageRow >= 0) {
                listAdapter.notifyItemChanged(voiceTranscriptionLanguageRow);
            }
            refreshVoiceModelsStatus();
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, "Idioma local: " + KronoConfig.getVoiceTranscriptionLanguageName()).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showVoiceTranscriptionModelsDialog() {
        if (getParentActivity() == null) {
            return;
        }
        org.telegram.messenger.KronoVoiceTranscriber.LanguageModelSpec spec = org.telegram.messenger.KronoVoiceTranscriber.getModelSpec(KronoConfig.voiceTranscriptionLanguage);
        String activeName = spec != null ? (spec.displayName + " (" + spec.approxSize + ")") : KronoConfig.getVoiceTranscriptionLanguageName();
        boolean isDownloaded = org.telegram.messenger.KronoVoiceTranscriber.isModelDownloaded(KronoConfig.voiceTranscriptionLanguage);

        final CharSequence[] items = new CharSequence[]{
                (isDownloaded ? "Volver a comprobar / descargar modelo: " : "Descargar ahora: ") + activeName,
                "Eliminar modelos descargados (" + voiceModelsStatusText + ")"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Modelos de voz offline (Vosk)");
        builder.setItems(items, (dialog, which) -> {
            dialog.dismiss();
            if (which == 0) {
                voiceModelsStatusText = "Descargando...";
                if (listAdapter != null && voiceTranscriptionModelsRow >= 0) {
                    listAdapter.notifyItemChanged(voiceTranscriptionModelsRow);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, "Descargando modelo " + activeName + "...").show();
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
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, "Modelos de voz eliminados").show();
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
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_TELEGRAM ? "✓ " : "") + "Telegram Oficial",
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_GOOGLE ? "✓ " : "") + "Google Translate",
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_YANDEX ? "✓ " : "") + "Yandex Translate",
                (KronoConfig.translationProvider == KronoConfig.TRANSLATE_PROVIDER_LINGVA ? "✓ " : "") + "Lingva Translate"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Motor de traducción");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setTranslationProvider(providerValues[which]);
            getMessagesController().getTranslateController().reset();
            getNotificationCenter().postNotificationName(NotificationCenter.updateSearchSettings);
            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, 0);
            if (listAdapter != null && translationProviderRow >= 0) {
                listAdapter.notifyItemChanged(translationProviderRow);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_translate, "Motor activo: " + KronoConfig.getTranslationProviderName()).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showMessageSwipeActionDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.messageSwipeAction == 0 ? "✓ " : "") + "Responder",
                (KronoConfig.messageSwipeAction == 1 ? "✓ " : "") + "Editar (si es propio) / Responder",
                (KronoConfig.messageSwipeAction == 2 ? "✓ " : "") + "Guardar en Mensajes guardados",
                (KronoConfig.messageSwipeAction == 3 ? "✓ " : "") + "Traducir mensaje",
                (KronoConfig.messageSwipeAction == 4 ? "✓ " : "") + "Reenviar mensaje",
                (KronoConfig.messageSwipeAction == 5 ? "✓ " : "") + "Copiar texto",
                (KronoConfig.messageSwipeAction == 6 ? "✓ " : "") + "Desactivado"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Acción al deslizar sobre un mensaje");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setMessageSwipeAction(which);
            if (listAdapter != null && messageSwipeActionRow >= 0) {
                listAdapter.notifyItemChanged(messageSwipeActionRow);
            }
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.menu_reply, "Gesto al deslizar: " + KronoConfig.getMessageSwipeActionName()).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTabModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.tabMode == KronoConfig.TAB_MODE_TEXT ? "✓ " : "") + "Solo nombre",
                (KronoConfig.tabMode == KronoConfig.TAB_MODE_ICON_TEXT ? "✓ " : "") + "Icono y nombre",
                (KronoConfig.tabMode == KronoConfig.TAB_MODE_ICON ? "✓ " : "") + "Solo icono"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Contenido de las pestañas");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setTabMode(which);
            if (listAdapter != null && tabModeRow >= 0) {
                listAdapter.notifyItemChanged(tabModeRow);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_folders, "Pestañas: " + KronoConfig.getTabModeName()).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTabStyleDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final CharSequence[] items = new CharSequence[]{
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_DEFAULT ? "✓ " : "") + "Predeterminado",
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_ROUNDED ? "✓ " : "") + "Redondeado",
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_PILL ? "✓ " : "") + "Píldora",
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_VKUI ? "✓ " : "") + "Subrayado inferior (VKUI)",
                (KronoConfig.tabStyle == KronoConfig.TAB_STYLE_TEXT_ONLY ? "✓ " : "") + "Solo texto (sin indicador)"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Estilo de las pestañas");
        builder.setItems(items, (dialog, which) -> {
            KronoConfig.setTabStyle(which);
            if (listAdapter != null && tabStyleRow >= 0) {
                listAdapter.notifyItemChanged(tabStyleRow);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            dialog.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme, "Estilo de pestañas: " + KronoConfig.getTabStyleName()).show();
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
        builder.setTitle("Palabras ocultas bajo Spoiler");

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
        editText.setHint("Separadas por comas (ej: spoiler, final, clave)");
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
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, "Palabras guardadas (activa el filtro arriba para aplicarlas)").show();
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
        builder.setTitle("Limpiar archivos del caché");
        builder.setMessage("¿Deseas borrar todos los archivos temporales y multimedia en caché (" + cacheSizeText + ")? Tus chats y tus archivos guardados en Descargas no se eliminarán.");
        builder.setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
            cacheSizeText = "Limpiando...";
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
                            .createSimpleBulletin(R.drawable.msg_delete, "Caché eliminado: " + freedStr + " liberados")
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
                    "FCM está desactivado en Privacidad -> Cero telemetría"
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
                "Solicitando nuevo token FCM y re-registrando con Telegram..."
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
                    "Primero concede el permiso de notificaciones del sistema"
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
                    NotificationChannel ch = new NotificationChannel(channelId, "Pruebas de KronoGram", NotificationManager.IMPORTANCE_HIGH);
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
                    .setContentTitle("KronoGram — Prueba de notificación")
                    .setContentText("El canal y permisos de notificación funcionan correctamente.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            NotificationManagerCompat.from(getParentActivity()).notify(49199, builder.build());
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_notifications,
                    "Notificación local de prueba enviada"
            ).show();
        } catch (Throwable e) {
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    "Error al enviar prueba: " + e.getMessage()
            ).show();
        }
    }

    private void showPushDiagnosticDetailsDialog() {
        if (getParentActivity() == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean hasGms = ApplicationLoader.getPushProvider().hasServices();
        sb.append("• Google Play Services: ").append(hasGms ? "Disponible" : "No disponible").append("\n");
        sb.append("• Firebase Push (FCM): ").append(KronoConfig.isDisableFirebasePush() ? "Desactivado por usuario" : "Habilitado").append("\n");
        sb.append("• Token FCM: ").append(TextUtils.isEmpty(SharedConfig.pushString) ? "No generado" : SharedConfig.pushString).append("\n");
        sb.append("• Estado interno: ").append(TextUtils.isEmpty(SharedConfig.pushStringStatus) ? "OK" : SharedConfig.pushStringStatus).append("\n");
        if (!TextUtils.isEmpty(PushListenerController.lastTokenError)) {
            sb.append("• Último error FCM: ").append(PushListenerController.lastTokenError).append("\n");
        }
        sb.append("• Cuenta actual registrada: ").append(UserConfig.getInstance(currentAccount).registeredForPush ? "Sí" : "No").append("\n");
        sb.append("• Conexión push MTProto: ").append(ConnectionsManager.getInstance(currentAccount).isPushConnectionEnabled() ? "Activa" : "Inactiva").append("\n");
        sb.append("• Servicio anti-cierre: ").append(NotificationsService.isRunningInForeground ? "En primer plano" : "Inactivo").append("\n");
        sb.append("• Batería sin restricciones: ").append(isBatteryOptimizationIgnored() ? "Sí" : "No (optimizada)").append("\n");
        if (PushListenerController.lastPushReceiveTime > 0) {
            String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(PushListenerController.lastPushReceiveTime));
            sb.append("• Último push recibido: ").append(PushListenerController.lastPushType).append(" a las ").append(ts);
        } else {
            sb.append("• Último push recibido: Ninguno en esta sesión");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Detalle técnico Push");
        builder.setMessage(sb.toString());
        if (!TextUtils.isEmpty(SharedConfig.pushString)) {
            builder.setNeutralButton("Copiar token", (d, w) -> {
                AndroidUtilities.addToClipboard(SharedConfig.pushString);
                BulletinFactory.of(KronoSettingsActivity.this).createSimpleBulletin(R.drawable.msg_copy, "Token FCM copiado").show();
            });
        }
        builder.setPositiveButton("Re-registrar", (d, w) -> forcePushTokenReregister());
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
                       position == showFavoritesFolderRow || position == hideAllChatsTabRow || position == spoilerWordsToggleRow ||
                       position == keepOriginalFilenameRow || position == freeVoiceTranscriptionRow || position == freeChatTranslationRow ||
                       position == persistentForegroundServiceRow || position == backgroundPushConnectionRow ||
                       position == glassSettingsBarRow || position == glassSearchBarsRow ||
                       position == glassBottomBarRow || position == glassChatBarsRow ||
                       position == disableTitleMarqueeRow) {
                return 2; // TextCheckCell
            } else if (position == hiddenVaultOpenRow || position == hiddenVaultLockMethodRow ||
                       position == openThemesRow || position == monetThemeRow || position == uiStyleRow ||
                       position == glassModeRow ||
                       position == tabModeRow || position == tabStyleRow ||
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
                    headerCell.setText("Ajustes");
                } else if (position == ghostHeaderRow) {
                    headerCell.setText("Modo Fantasma (Ghost Mode)");
                } else if (position == hiddenVaultHeaderRow) {
                    headerCell.setText("Chats y carpetas ocultos");
                } else if (position == privacyExtraHeaderRow) {
                    headerCell.setText("Privacidad avanzada");
                } else if (position == telemetryHeaderRow) {
                    headerCell.setText("Cero telemetría y rastreadores");
                } else if (position == themeHeaderRow) {
                    headerCell.setText("Temas");
                } else if (position == tabsHeaderRow) {
                    headerCell.setText("Pestañas de chats");
                } else if (position == chatsHeaderRow) {
                    headerCell.setText("Chats y Carpetas");
                } else if (position == storageHeaderRow) {
                    headerCell.setText("Archivos y Caché");
                } else if (position == aiHeaderRow) {
                    headerCell.setText("Transcripción y Traducción");
                } else if (position == notifServiceHeaderRow) {
                    headerCell.setText("Servicio y entrega en segundo plano");
                } else if (position == diagHeaderRow) {
                    headerCell.setText("Diagnóstico de notificaciones Push");
                }
            } else if (type == 1) {
                SettingsActivity.SettingCell settingCell = (SettingsActivity.SettingCell) holder.itemView;
                if (position == privacyCategoryRow) {
                    settingCell.set(0xFF28B9F2, 0xFF0A98E6, R.drawable.settings_privacy, "Privacidad y seguridad", "Modo Fantasma, bóveda oculta, anti-borrado y 1 vista", null);
                } else if (position == appearanceCategoryRow) {
                    settingCell.set(0xFFF34A71, 0xFFDC284E, R.drawable.settings_chat, "Apariencia", "Temas, Material You y estilo de la interfaz", null);
                } else if (position == chatsCategoryRow) {
                    settingCell.set(0xFFF39C27, 0xFFE37810, R.drawable.settings_folders, "Chats", "Carpetas, gestos, anclados ilimitados y caché", null);
                } else if (position == aiCategoryRow) {
                    settingCell.set(0xFF9663FA, 0xFF7244E6, R.drawable.settings_language, "Voz y Traducción", "Transcripción de voz gratuita y motores de traducción", null);
                } else if (position == notificationsCategoryRow) {
                    settingCell.set(0xFFFA5858, 0xFFE3324B, R.drawable.settings_sounds, "Notificaciones y Diagnóstico", "Servicio anti-cierre, estado FCM, batería y pruebas push", null);
                } else if (position == backupCategoryRow) {
                    settingCell.set(0xFF19B5C9, 0xFF0E88A6, R.drawable.settings_data, "Copia de seguridad", "Nube de Telegram, archivo local y Mensajes Guardados", null);
                }
            } else if (type == 2) {
                TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                if (position == ghostMasterRow) {
                    checkCell.setTextAndValueAndCheck("Activar Modo Fantasma completo", "Oculta lectura, historias, escribiendo y en línea", KronoConfig.ghostMode, true, true);
                } else if (position == ghostDontReadMessagesRow) {
                    checkCell.setTextAndCheck("No marcar mensajes como leídos", KronoConfig.isGhostDontReadMessages(), true);
                } else if (position == ghostDontReadStoriesRow) {
                    checkCell.setTextAndCheck("No marcar historias como vistas", KronoConfig.isGhostDontReadStories(), true);
                } else if (position == ghostDontSendTypingRow) {
                    checkCell.setTextAndCheck("Ocultar estado «escribiendo...»", KronoConfig.isGhostDontSendTyping(), true);
                } else if (position == ghostDontSendOnlineRow) {
                    checkCell.setTextAndCheck("Ocultar estado «en línea»", KronoConfig.isGhostDontSendOnline(), false);
                } else if (position == hiddenVaultBiometricRow) {
                    boolean bioReady = KronoHiddenLockHelper.canUseBiometrics(mContext);
                    checkCell.setTextAndValueAndCheck("Desbloqueo biométrico", "Permite usar huella o rostro junto al PIN o contraseña", bioReady && KronoConfig.hiddenUseBiometric, true, true);
                    checkCell.setEnabled(bioReady, null);
                } else if (position == hiddenVaultMuteNotifRow) {
                    checkCell.setTextAndValueAndCheck("Silenciar notificaciones de ocultos", "Evita notificaciones y previsualizaciones de chats y carpetas ocultos", KronoConfig.hiddenMuteNotifications, true, true);
                } else if (position == hiddenVaultHideFolderChatsRow) {
                    checkCell.setTextAndValueAndCheck("Ocultar chats de carpetas ocultas", "También oculta sus chats en la pestaña «Todos» y en la búsqueda", KronoConfig.hiddenHideFolderChatsEverywhere, true, true);
                } else if (position == hiddenVaultAutoLockRow) {
                    checkCell.setTextAndValueAndCheck("Bloquear bóveda al salir", "Exige volver a desbloquear la bóveda al ir a segundo plano", KronoConfig.hiddenAutoLockOnBackground, true, false);
                } else if (position == hidePhoneNumberRow) {
                    checkCell.setTextAndValueAndCheck("Ocultar número de teléfono", "Oculta tu número en Ajustes, Cajón y Perfil", KronoConfig.hidePhoneNumber, true, true);
                } else if (position == hideChatListNamesRow) {
                    checkCell.setTextAndValueAndCheck("Ocultar nombres y avatares en lista", "Modo privacidad en la pantalla principal de chats", KronoConfig.hideChatListNamesAvatars, true, true);
                } else if (position == blockSecretChatsRow) {
                    checkCell.setTextAndValueAndCheck("Bloquear inicio de chats secretos", "Impide iniciar o aceptar chats secretos", KronoConfig.blockSecretChats, true, true);
                } else if (position == antiDeleteMessagesRow) {
                    checkCell.setTextAndValueAndCheck("Guardar mensajes eliminados", "Conserva localmente mensajes borrados y muestra «eliminado»", KronoConfig.antiDeleteMessages, true, true);
                } else if (position == antiDeleteExcludeChannelsRow) {
                    checkCell.setTextAndValueAndCheck("Excluir canales en anti-borrado", "Aplica anti-borrado solo a chats privados, grupos y supergrupos", KronoConfig.antiDeleteExcludeChannels, true, true);
                } else if (position == unlimitedViewOnceRow) {
                    checkCell.setTextAndValueAndCheck("Ver multimedia de 1 sola vista sin límite", "Permite reabrir fotos, vídeos, audios y notas redondas temporales", KronoConfig.unlimitedViewOnce, true, true);
                } else if (position == viewOnceAllowSaveRow) {
                    checkCell.setTextAndValueAndCheck("Permitir capturas y guardado en 1 vista", "Habilita capturas de pantalla y guardar en galería multimedia efímera", KronoConfig.viewOnceAllowSaveAndScreenshots, true, true);
                } else if (position == allowRestrictedContentBypassRow) {
                    checkCell.setTextAndValueAndCheck("Omitir restricciones en chats y canales", "Permite capturas, grabación de pantalla, copia, guardado y reenvío en canales, chats, historias y chats secretos", KronoConfig.allowRestrictedContentBypass, true, false);
                } else if (position == zeroTelemetryRow) {
                    checkCell.setTextAndValueAndCheck("Cero telemetría / Sin rastreadores", "Bloqueo integral: desactiva logs internos, Firebase Analytics, anuncios y URL trackers", KronoConfig.zeroTelemetry, true, true);
                } else if (position == blockTelegramTelemetryRow) {
                    checkCell.setTextAndValueAndCheck("Bloquear telemetría interna de Telegram", "Impide enviar eventos appLog, estadísticas de dispositivo e install referrer", KronoConfig.isBlockTelegramTelemetry(), true, true);
                } else if (position == disableFirebaseAnalyticsRow) {
                    checkCell.setTextAndValueAndCheck("Desactivar Firebase Analytics", "Detiene la recopilación de analíticas e ID de publicidad en tiempo de ejecución", KronoConfig.isDisableFirebaseAnalytics(), true, true);
                } else if (position == blockSponsoredMessagesRow) {
                    checkCell.setTextAndValueAndCheck("Bloquear anuncios y métricas patrocinadas", "Oculta mensajes/vídeos patrocinados y bloquea reportes de vista o clic", KronoConfig.isBlockSponsoredMessages(), true, true);
                } else if (position == stripUrlTrackersRow) {
                    checkCell.setTextAndValueAndCheck("Limpiar rastreadores en enlaces", "Elimina utm_*, fbclid, gclid, igshid, si y similares al abrir URLs", KronoConfig.isStripUrlTrackers(), true, true);
                } else if (position == disableFirebasePushRow) {
                    checkCell.setTextAndValueAndCheck("Desactivar Push de Google (FCM)", "Evita conectar con Firebase Push; usa servicio local en segundo plano", KronoConfig.isDisableFirebasePush(), true, false);
                } else if (position == tabStyleStrokeRow) {
                    checkCell.setTextAndValueAndCheck("Contorno en pestaña activa", "Añade un borde sutil alrededor de la pestaña seleccionada", KronoConfig.tabStyleStroke, true, true);
                } else if (position == hideTabsCountersRow) {
                    checkCell.setTextAndValueAndCheck("Ocultar contadores en pestañas", "Esconde el globo numérico de mensajes no leídos en las carpetas", KronoConfig.hideTabsCounters, true, true);
                } else if (position == folderNameInHeaderRow) {
                    checkCell.setTextAndValueAndCheck("Nombre de carpeta en cabecera", "Muestra el nombre de la carpeta activa en la barra superior", KronoConfig.folderNameInHeader, true, true);
                } else if (position == disableTabsSwipeRow) {
                    checkCell.setTextAndValueAndCheck("Desactivar deslizamiento entre carpetas", "Evita cambiar de carpeta al deslizar horizontalmente en la lista", KronoConfig.disableTabsSwipe, true, true);
                } else if (position == infiniteFolderScrollRow) {
                    checkCell.setTextAndValueAndCheck("Scroll infinito entre carpetas", "Al llegar a la última o primera carpeta y seguir deslizando, vuelve al otro extremo en lugar de ir a Contactos", KronoConfig.infiniteFolderScroll, true, true);
                    checkCell.setEnabled(!KronoConfig.disableTabsSwipe, null);
                } else if (position == swipeToEditOwnRow) {
                    checkCell.setTextAndValueAndCheck("Editar mensaje enviado con gesto", "Desliza hacia la izquierda un mensaje propio para editarlo", KronoConfig.swipeToEditOwnMessage, true, true);
                } else if (position == deleteForAllDefaultRow) {
                    checkCell.setTextAndValueAndCheck("«Borrar para todos» por defecto", "Marca automáticamente la casilla al eliminar mensajes", KronoConfig.deleteForAllByDefault, true, true);
                } else if (position == showTimeWithSecondsRow) {
                    checkCell.setTextAndValueAndCheck("Hora exacta con segundos", "Muestra los segundos en la hora de envío y recepción de los mensajes", KronoConfig.showTimeWithSeconds, true, true);
                } else if (position == unlimitedPinnedChatsRow) {
                    checkCell.setTextAndValueAndCheck("Chats anclados ilimitados (local)", "Permite fijar tantos chats como quieras sin límite", KronoConfig.unlimitedPinnedChats, true, true);
                } else if (position == quickGoToMessageRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoQuickGoToMessage), LocaleController.getString(R.string.KronoQuickGoToMessageDesc), KronoConfig.quickGoToMessageInSearch, true, true);
                } else if (position == longClickMediaMenuRow) {
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.KronoLongClickMediaMenu), LocaleController.getString(R.string.KronoLongClickMediaMenuDesc), KronoConfig.longClickMediaShowsMenu, true, true);
                } else if (position == disablePullToNextChannelRow) {
                    checkCell.setTextAndValueAndCheck("Desactivar salto al siguiente canal o tema", "Evita pasar automáticamente al siguiente canal o tema sin leer al seguir haciendo scroll al final", KronoConfig.disablePullToNextChannel, true, true);
                } else if (position == showFavoritesFolderRow) {
                    checkCell.setTextAndValueAndCheck("Mostrar carpeta «Favoritos»", "Carpeta local protegida (⭐) para tus chats favoritos", KronoConfig.showFavoritesFolder, true, true);
                } else if (position == hideAllChatsTabRow) {
                    checkCell.setTextAndValueAndCheck("Eliminar carpeta «Todos»", "Oculta la pestaña «Todos» cuando tienes carpetas", KronoConfig.hideAllChatsTab, true, false);
                } else if (position == spoilerWordsToggleRow) {
                    checkCell.setTextAndCheck("Filtro de palabras ocultas bajo Spoiler", KronoConfig.spoilerWordsEnabled, true);
                } else if (position == keepOriginalFilenameRow) {
                    checkCell.setTextAndValueAndCheck("Guardar archivos con nombre original", "Evita prefijos numéricos al guardar documentos", KronoConfig.keepOriginalFilename, true, true);
                } else if (position == freeVoiceTranscriptionRow) {
                    checkCell.setTextAndValueAndCheck("Transcripción de voz gratuita", "Habilita el botón voz-a-texto sin requerir Premium", KronoConfig.freeVoiceTranscription, true, true);
                } else if (position == freeChatTranslationRow) {
                    checkCell.setTextAndValueAndCheck("Traducir chats completos gratis", "Habilita la traducción completa de chats y canales sin Telegram Premium usando el motor elegido", KronoConfig.freeChatTranslation, true, true);
                } else if (position == persistentForegroundServiceRow) {
                    checkCell.setTextAndValueAndCheck("Servicio persistente (anti-cierre OEM)", "Mantiene una notificación silenciosa en primer plano para evitar cierres por Doze, Xiaomi, Samsung o Huawei", KronoConfig.isPersistentForegroundService(), true, true);
                } else if (position == backgroundPushConnectionRow) {
                    boolean persistent = KronoConfig.isPersistentForegroundService();
                    boolean bgConn = persistent || MessagesController.getGlobalNotificationsSettings().getBoolean("pushConnection", false);
                    checkCell.setTextAndValueAndCheck("Conexión TCP en segundo plano", "Mantiene el socket MTProto activo si FCM falla o está desactivado", bgConn, true, false);
                    checkCell.setEnabled(!persistent, null);
                } else if (position == glassSettingsBarRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck("Cristal en barra de ajustes", "Botones y barra superior en Ajustes, Perfil y listas", glassMasterOn && KronoConfig.glassOnSettingsBar, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == glassSearchBarsRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck("Cristal en barras de búsqueda", "Buscador principal, dentro de chats y filtros", glassMasterOn && KronoConfig.glassOnSearchBars, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == glassBottomBarRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck("Cristal en barra inferior y pestañas", "Barra de navegación inferior y pestañas de carpetas", glassMasterOn && KronoConfig.glassOnBottomBar, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == glassChatBarsRow) {
                    final boolean glassMasterOn = !KronoConfig.isGlassOff();
                    checkCell.setTextAndValueAndCheck("Cristal en chat y menús", "Barra superior de chats, campo de mensaje y menús flotantes", glassMasterOn && KronoConfig.glassOnChatBars, true, true);
                    checkCell.setEnabled(glassMasterOn, null);
                } else if (position == disableTitleMarqueeRow) {
                    checkCell.setTextAndValueAndCheck("Quitar efecto marquee en los títulos", "Evita el desplazamiento lateral continuo en títulos largos", KronoConfig.disableTitleMarquee, true, false);
                }
            } else if (type == 3) {
                TextSettingsCell settingsCell = (TextSettingsCell) holder.itemView;
                if (position == hiddenVaultOpenRow) {
                    int chatsCount = KronoConfig.getHiddenDialogs(currentAccount).size();
                    int foldersCount = KronoConfig.getHiddenFilterIds(currentAccount).size();
                    String summary = (chatsCount == 0 && foldersCount == 0)
                            ? "Vacía"
                            : (chatsCount + " chats · " + foldersCount + " carpetas");
                    settingsCell.setTextAndValue("Abrir bóveda oculta", summary, true);
                } else if (position == hiddenVaultLockMethodRow) {
                    settingsCell.setTextAndValue("Método de bloqueo", KronoConfig.getHiddenLockSummary(KronoHiddenLockHelper.canUseBiometrics(mContext)), true);
                } else if (position == openThemesRow) {
                    settingsCell.setTextAndValue("Temas", "Abrir Ajustes de chats", true);
                } else if (position == monetThemeRow) {
                    Theme.ThemeInfo cur = Theme.getCurrentTheme();
                    String curName = cur != null && !android.text.TextUtils.isEmpty(cur.name) ? cur.name : "Predeterminado";
                    settingsCell.setTextAndValue("Tema Monet (Material You)", curName, true);
                } else if (position == uiStyleRow) {
                    settingsCell.setTextAndValue("Estilo visual de interfaz", KronoConfig.getUiStyleName(), true);
                } else if (position == glassModeRow) {
                    settingsCell.setTextAndValue("Efecto de cristal (Glass)", KronoConfig.getGlassModeName(), true);
                } else if (position == tabModeRow) {
                    settingsCell.setTextAndValue("Contenido de las pestañas", KronoConfig.getTabModeName(), true);
                } else if (position == tabStyleRow) {
                    settingsCell.setTextAndValue("Estilo de las pestañas", KronoConfig.getTabStyleName(), true);
                } else if (position == messageSwipeActionRow) {
                    settingsCell.setTextAndValue("Acción al deslizar sobre un mensaje", KronoConfig.getMessageSwipeActionName(), true);
                } else if (position == spoilerWordsEditRow) {
                    int count = KronoConfig.getSpoilerWordsList().size();
                    settingsCell.setTextAndValue("Configurar palabras en Spoiler", count == 0 ? "Ninguna" : (count + " palabras"), false);
                } else if (position == clearCacheNowRow) {
                    settingsCell.setTextAndValue("Borrar archivos del caché ahora", cacheSizeText, true);
                } else if (position == openStorageUsageRow) {
                    settingsCell.setTextAndValue("Administrador avanzado de almacenamiento", "Abrir", false);
                } else if (position == voiceTranscriptionModeRow) {
                    settingsCell.setTextAndValue("Modo de transcripción", KronoConfig.getVoiceTranscriptionModeName(), true);
                } else if (position == voiceTranscriptionLanguageRow) {
                    settingsCell.setTextAndValue("Idioma de transcripción local", KronoConfig.getVoiceTranscriptionLanguageName(), true);
                } else if (position == voiceTranscriptionModelsRow) {
                    settingsCell.setTextAndValue("Modelos de voz offline (Vosk)", voiceModelsStatusText, true);
                } else if (position == translationProviderRow) {
                    settingsCell.setTextAndValue("Motor de traducción alternativo", KronoConfig.getTranslationProviderName(), false);
                } else if (position == diagPlayServicesRow) {
                    boolean hasGms = ApplicationLoader.getPushProvider().hasServices();
                    settingsCell.setTextAndValue("Servicios de Google Play", hasGms ? "Disponibles" : "No disponibles", true);
                } else if (position == diagFcmStatusRow) {
                    String fcmState;
                    if (KronoConfig.isDisableFirebasePush()) {
                        fcmState = "Desactivado";
                    } else if (!TextUtils.isEmpty(PushListenerController.lastTokenError) || "__FIREBASE_FAILED__".equals(SharedConfig.pushStringStatus)) {
                        fcmState = "Error (ver detalle)";
                    } else {
                        fcmState = "Activo";
                    }
                    settingsCell.setTextAndValue("Firebase Push (FCM)", fcmState, true);
                } else if (position == diagFcmTokenRow) {
                    String tokenSummary;
                    if (KronoConfig.isDisableFirebasePush()) {
                        tokenSummary = "Desactivado";
                    } else if (TextUtils.isEmpty(SharedConfig.pushString)) {
                        tokenSummary = "__FIREBASE_FAILED__".equals(SharedConfig.pushStringStatus) ? "Error al obtener" : "Sin token / Generando";
                    } else {
                        String tail = SharedConfig.pushString.length() > 6
                                ? SharedConfig.pushString.substring(SharedConfig.pushString.length() - 6)
                                : SharedConfig.pushString;
                        boolean reg = UserConfig.getInstance(currentAccount).registeredForPush;
                        tokenSummary = (reg ? "Registrado (…" : "Pendiente (…") + tail + ")";
                    }
                    settingsCell.setTextAndValue("Token FCM y registro", tokenSummary, true);
                } else if (position == diagNotifPermRow) {
                    settingsCell.setTextAndValue("Permiso de notificaciones", hasNotificationPermission() ? "Concedido" : "Denegado (Abrir)", true);
                } else if (position == diagBatteryOptRow) {
                    settingsCell.setTextAndValue("Optimización de batería", isBatteryOptimizationIgnored() ? "Sin restricciones" : "Optimizada (Desactivar)", true);
                } else if (position == diagServiceRunningRow) {
                    settingsCell.setTextAndValue("Servicio anti-cierre", NotificationsService.isRunningInForeground ? "Activo en primer plano" : "Inactivo", true);
                } else if (position == diagLastPushRow) {
                    String lastPush;
                    if (PushListenerController.lastPushReceiveTime > 0) {
                        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(PushListenerController.lastPushReceiveTime));
                        lastPush = PushListenerController.lastPushType + " · " + ts;
                    } else {
                        lastPush = "Ninguno en sesión";
                    }
                    settingsCell.setTextAndValue("Último push FCM recibido", lastPush, true);
                } else if (position == diagRefreshPushRow) {
                    settingsCell.setTextAndValue("Forzar re-registro de token FCM", "Ejecutar", true);
                } else if (position == diagTestNotificationRow) {
                    settingsCell.setTextAndValue("Enviar notificación local de prueba", "Probar", true);
                } else if (position == diagSystemBatteryRow) {
                    settingsCell.setTextAndValue("Ajustes de batería del sistema", "Abrir", false);
                }
            } else if (type == 4) {
                TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                if (position == ghostInfoRow) {
                    infoCell.setText("El Modo Fantasma te permite leer mensajes e historias sin enviar confirmaciones de lectura ni mostrar que estás en línea.");
                } else if (position == hiddenVaultInfoRow) {
                    infoCell.setText("Oculta chats manteniendo pulsado un chat en la lista principal o seleccionando varios, y oculta carpetas manteniendo pulsada su pestaña. Para entrar rápido a la bóveda, mantén pulsado el título «KronoGram» arriba.");
                } else if (position == privacyExtraInfoRow) {
                    infoCell.setText("Oculta información sensible, conserva mensajes eliminados por otros usuarios y visualiza multimedia efímera sin límite. Si activas «No marcar mensajes como leídos» en Modo Fantasma, tampoco se enviará confirmación de apertura al ver multimedia de 1 sola vista.");
                } else if (position == telemetryInfoRow) {
                    infoCell.setText("Todas las opciones de telemetría vienen desactivadas por defecto para que elijas libremente. Nota: si activas «Desactivar Push de Google (FCM)», las notificaciones dependerán del servicio en segundo plano de Telegram y podrían llegar con ligero retraso o consumir más batería.");
                } else if (position == themeInfoRow) {
                    infoCell.setText("Elige entre Monet Light, Monet Dark o Monet AMOLED y cambia al instante el estilo visual entre Material You, Estilo iOS o Estilo Clásico. También puedes elegir el modo Glass (Liquid Glass, Frosted Glass o Desactivado) y activar o desactivar el cristal de forma independiente por componente.");
                } else if (position == storageInfoRow) {
                    infoCell.setText("También puedes añadir o quitar cualquier chat de una carpeta manteniendo pulsado el chat en la lista principal y tocando el icono de carpetas.");
                } else if (position == aiInfoRow) {
                    infoCell.setText("La transcripción de voz funciona gratuitamente en cualquier versión de Android sin API keys mediante Vosk offline: la primera vez descargará el modelo ligero (~40 MB) del idioma seleccionado y después todo el reconocimiento se ejecuta localmente en tu dispositivo.");
                } else if (position == notifServiceInfoRow) {
                    infoCell.setText("Activa el «Servicio persistente» si notas retrasos al apagar la pantalla o si desactivaste Push de Google (FCM). Mostrará una notificación silenciosa de prioridad mínima para impedir que el sistema cierre la app.");
                } else if (position == diagInfoRow) {
                    infoCell.setText("Toca «Token FCM y registro» para ver el detalle técnico completo. Si tu teléfono (Xiaomi/HyperOS, Samsung, Huawei, Oppo) sigue reteniendo notificaciones, toca «Optimización de batería» para poner KronoGram en «Sin restricciones» y activa «Inicio automático» en los ajustes del sistema.");
                }
            }
        }
    }
}
