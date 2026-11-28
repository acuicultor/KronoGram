package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.KronoBackupManager;
import org.telegram.messenger.KronoConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;

public class KronoBackupActivity extends BaseFragment {

    private static final int REQUEST_CODE_EXPORT_BACKUP = 8901;
    private static final int REQUEST_CODE_IMPORT_BACKUP = 8902;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;
    private int cloudHeaderRow;
    private int cloudStatusRow;
    private int cloudSaveRow;
    private int cloudRestoreRow;
    private int cloudModeRow;
    private int cloudAutoScopeRow;
    private int cloudDeleteRow;
    private int cloudInfoRow;

    private int localHeaderRow;
    private int sendSavedMessagesRow;
    private int exportFileRow;
    private int importFileRow;
    private int localInfoRow;

    private int resetHeaderRow;
    private int resetKronoRow;
    private int resetInfoRow;

    private boolean cloudLoading = false;
    private KronoBackupManager.BackupMetadata cloudMetadata = null;
    private String cloudErrorText = null;
    private KronoBackupManager.BackupOptions pendingExportOptions = null;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        KronoConfig.loadConfig();
        updateRows();
        return true;
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

    private void updateRows() {
        rowCount = 0;
        cloudHeaderRow = rowCount++;
        cloudStatusRow = rowCount++;
        cloudSaveRow = rowCount++;
        cloudRestoreRow = rowCount++;
        cloudModeRow = rowCount++;
        cloudAutoScopeRow = rowCount++;
        cloudDeleteRow = rowCount++;
        cloudInfoRow = rowCount++;

        localHeaderRow = rowCount++;
        sendSavedMessagesRow = rowCount++;
        exportFileRow = rowCount++;
        importFileRow = rowCount++;
        localInfoRow = rowCount++;

        resetHeaderRow = rowCount++;
        resetKronoRow = rowCount++;
        resetInfoRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);

        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle(LocaleController.getString(R.string.KronoSectionBackupTitle));
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

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

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
        listView.setOnItemClickListener((view, position) -> handleItemClick(position));

        refreshCloudStatus(false);
        return fragmentView;
    }

    private void handleItemClick(int position) {
        if (position == cloudStatusRow) {
            refreshCloudStatus(true);
        } else if (position == cloudSaveRow) {
            showCategoryPickerDialog(
                    this,
                    LocaleController.getString(R.string.KronoBackupCloudSave),
                    LocaleController.formatString(R.string.KronoBackupCloudSaveDesc, KronoBackupManager.CLOUD_BACKUP_BOT_USERNAME),
                    LocaleController.getString(R.string.KronoBackupSaveCloudBtn),
                    KronoBackupManager.BackupOptions.createDefault(),
                    null,
                    this::performCloudUpload
            );
        } else if (position == cloudRestoreRow) {
            performCloudDownloadAndRestore();
        } else if (position == cloudModeRow) {
            showCloudModeDialog();
        } else if (position == cloudAutoScopeRow) {
            showAutoBackupScopeDialog();
        } else if (position == cloudDeleteRow) {
            confirmDeleteCloudBackup();
        } else if (position == sendSavedMessagesRow) {
            showCategoryPickerDialog(
                    this,
                    LocaleController.getString(R.string.KronoBackupSendSaved),
                    LocaleController.getString(R.string.KronoBackupSendSavedDesc),
                    LocaleController.getString(R.string.KronoBackupSendBtn),
                    KronoBackupManager.BackupOptions.createDefault(),
                    null,
                    this::performSavedMessagesExport
            );
        } else if (position == exportFileRow) {
            showCategoryPickerDialog(
                    this,
                    LocaleController.getString(R.string.KronoBackupExportTitle),
                    LocaleController.getString(R.string.KronoBackupExportDesc),
                    LocaleController.getString(R.string.KronoBackupChooseLoc),
                    KronoBackupManager.BackupOptions.createDefault(),
                    null,
                    options -> {
                        pendingExportOptions = options;
                        launchCreateDocumentPicker();
                    }
            );
        } else if (position == importFileRow) {
            launchOpenDocumentPicker();
        } else if (position == resetKronoRow) {
            showResetKronoDialog();
        }
    }

    private boolean isFragmentAlive() {
        return !isFinishing() && getParentActivity() != null && !getParentActivity().isFinishing() && fragmentView != null;
    }

    private static boolean isFragmentAlive(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null || fragment.getParentActivity().isFinishing() || fragment.getFragmentView() == null) {
            return false;
        }
        if (fragment instanceof KronoBackupActivity && ((KronoBackupActivity) fragment).isFinishing()) {
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Cloud Operations
    // -------------------------------------------------------------------------

    private void refreshCloudStatus(boolean userTriggered) {
        if (cloudLoading) {
            return;
        }
        cloudLoading = true;
        cloudErrorText = null;
        if (listAdapter != null && cloudStatusRow >= 0) {
            listAdapter.notifyItemChanged(cloudStatusRow);
        }
        KronoBackupManager.fetchCloudMetadata(currentAccount, meta -> {
            cloudLoading = false;
            cloudMetadata = meta;
            cloudErrorText = null;
            if (!isFragmentAlive()) return;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (userTriggered) {
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.chats_infotip,
                        LocaleController.getString(meta != null ? R.string.KronoBackupCloudUpdated : R.string.KronoBackupCloudNone)
                ).show();
            }
        }, err -> {
            cloudLoading = false;
            cloudErrorText = err;
            if (!isFragmentAlive()) return;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (userTriggered) {
                BulletinFactory.of(this).createErrorBulletin(err).show();
            }
        });
    }

    private void performCloudUpload(KronoBackupManager.BackupOptions options) {
        if (!isFragmentAlive()) return;
        AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        showDialog(progress);

        KronoBackupManager.uploadToCloud(currentAccount, options, meta -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            cloudMetadata = meta;
            cloudErrorText = null;
            if (!isFragmentAlive()) return;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(
                    R.raw.contact_check,
                    LocaleController.formatString(R.string.KronoBackupCloudSaved, AndroidUtilities.formatFileSize(meta.rawByteSize))
            ).show();
        }, err -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive()) return;
            BulletinFactory.of(this).createErrorBulletin(err).show();
        });
    }

    private void performCloudDownloadAndRestore() {
        if (!isFragmentAlive()) return;
        AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        showDialog(progress);

        KronoBackupManager.downloadFromCloud(currentAccount, parsed -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive()) return;
            showRestoreDialogForParsedBackup(this, currentAccount, parsed, LocaleController.getString(R.string.KronoBackupCloudRestore), () -> {
                if (isFragmentAlive() && listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            });
        }, err -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive()) return;
            BulletinFactory.of(this).createErrorBulletin(err).show();
        });
    }

    private void confirmDeleteCloudBackup() {
        if (!isFragmentAlive()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoBackupCloudDelete));
        builder.setMessage(LocaleController.formatString(R.string.KronoBackupCloudDeleteMsg, KronoBackupManager.CLOUD_BACKUP_BOT_USERNAME));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
            if (!isFragmentAlive()) return;
            AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.setCanCancel(false);
            showDialog(progress);
            KronoBackupManager.deleteCloudBackup(currentAccount, () -> {
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                cloudMetadata = null;
                cloudErrorText = null;
                if (!isFragmentAlive()) return;
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_delete, LocaleController.getString(R.string.KronoBackupCloudDeleted)).show();
            }, err -> {
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                if (!isFragmentAlive()) return;
                BulletinFactory.of(this).createErrorBulletin(err).show();
            });
        });
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void showCloudModeDialog() {
        if (!isFragmentAlive()) return;
        final int[] modeValues = new int[]{
                KronoConfig.CLOUD_BACKUP_ON_CHANGE,
                KronoConfig.CLOUD_BACKUP_DAILY,
                KronoConfig.CLOUD_BACKUP_WEEKLY,
                KronoConfig.CLOUD_BACKUP_MANUAL
        };
        String[] items = new String[]{
                LocaleController.getString(R.string.KronoBackupModeAuto),
                LocaleController.getString(R.string.KronoBackupModeDaily),
                LocaleController.getString(R.string.KronoBackupModeWeekly),
                LocaleController.getString(R.string.KronoBackupModeManualLong)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.KronoBackupMode));
        builder.setItems(items, (dialog, which) -> {
            int chosenMode = modeValues[which];
            KronoConfig.setCloudBackupMode(chosenMode);
            if (chosenMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
                KronoBackupManager.startSettingsObserver(currentAccount);
            }
            if (!isFragmentAlive()) return;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(
                    R.raw.contact_check,
                    LocaleController.formatString(R.string.KronoBackupModeApplied, KronoConfig.getCloudBackupModeName())
            ).show();
        });
        showDialog(builder.create());
    }

    private void showAutoBackupScopeDialog() {
        KronoBackupManager.BackupOptions current = KronoBackupManager.BackupOptions.fromAutoConfig();
        showCategoryPickerDialog(
                this,
                LocaleController.getString(R.string.KronoBackupAutoScopeTitle),
                LocaleController.getString(R.string.KronoBackupAutoScopeDesc),
                LocaleController.getString(R.string.KronoSaveSelection),
                current,
                null,
                true,
                selected -> {
                    KronoConfig.setCloudAutoBackupScope(
                            selected.includeTelegramMain,
                            selected.includeTelegramTheme,
                            selected.includeTelegramNotifications,
                            selected.includeHiddenVault
                    );
                    if (!isFragmentAlive()) return;
                    if (listAdapter != null && cloudAutoScopeRow >= 0) {
                        listAdapter.notifyItemChanged(cloudAutoScopeRow);
                    }
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.KronoBackupScopeUpdated)).show();
                }
        );
    }

    // -------------------------------------------------------------------------
    // Local File & Saved Messages Operations
    // -------------------------------------------------------------------------

    private void performSavedMessagesExport(KronoBackupManager.BackupOptions options) {
        if (!isFragmentAlive()) return;
        AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        showDialog(progress);

        KronoBackupManager.exportToSavedMessages(currentAccount, options, fileName -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive()) return;
            BulletinFactory.of(this).createSimpleBulletin(
                    R.raw.saved_messages,
                    LocaleController.getString(R.string.KronoBackupSentSaved)
            ).show();
        }, err -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive()) return;
            BulletinFactory.of(this).createErrorBulletin(err).show();
        });
    }

    private void launchCreateDocumentPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, KronoBackupManager.buildSuggestedFileName());
            startActivityForResult(intent, REQUEST_CODE_EXPORT_BACKUP);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.KronoBackupPickerErr)).show();
        }
    }

    private void launchOpenDocumentPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_IMPORT_BACKUP);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.KronoBackupPickerErr)).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null || getParentActivity() == null) {
            if (requestCode == REQUEST_CODE_EXPORT_BACKUP) {
                pendingExportOptions = null;
            }
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_CODE_EXPORT_BACKUP) {
            KronoBackupManager.BackupOptions opts = pendingExportOptions != null
                    ? pendingExportOptions
                    : KronoBackupManager.BackupOptions.createDefault();
            pendingExportOptions = null;

            AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.setCanCancel(false);
            showDialog(progress);

            KronoBackupManager.exportToUri(getParentActivity(), currentAccount, uri, opts, meta -> {
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                if (!isFragmentAlive()) return;
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.contact_check,
                        LocaleController.formatString(R.string.KronoBackupExported, AndroidUtilities.formatFileSize(meta.rawByteSize))
                ).show();
            }, err -> {
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                if (!isFragmentAlive()) return;
                BulletinFactory.of(this).createErrorBulletin(err).show();
            });
        } else if (requestCode == REQUEST_CODE_IMPORT_BACKUP) {
            AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.setCanCancel(false);
            showDialog(progress);

            KronoBackupManager.parseFromUri(getParentActivity(), uri, parsed -> {
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                if (!isFragmentAlive()) return;
                showRestoreDialogForParsedBackup(this, currentAccount, parsed, LocaleController.getString(R.string.KronoBackupImportFile), () -> {
                    if (isFragmentAlive() && listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }, err -> {
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                if (!isFragmentAlive()) return;
                BulletinFactory.of(this).createErrorBulletin(err).show();
            });
        }
    }

    // -------------------------------------------------------------------------
    // Reset Defaults Dialog
    // -------------------------------------------------------------------------

    private void showResetKronoDialog() {
        if (getParentActivity() == null) return;
        Context ctx = getParentActivity();
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), 0);

        TextView msg = new TextView(ctx);
        msg.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        msg.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        msg.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        msg.setText(LocaleController.getString(R.string.KronoBackupResetMsg));
        container.addView(msg, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final boolean[] preserveVault = new boolean[]{true};
        CheckBoxCell vaultCheck = new CheckBoxCell(ctx, CheckBoxCell.TYPE_CHECK_BOX_DEFAULT);
        vaultCheck.setBackground(Theme.getSelectorDrawable(false));
        vaultCheck.setText(LocaleController.getString(R.string.KronoBackupKeepVault), "", true, false);
        vaultCheck.setOnClickListener(v -> {
            preserveVault[0] = !preserveVault[0];
            vaultCheck.setChecked(preserveVault[0], true);
        });
        container.addView(vaultCheck, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(LocaleController.getString(R.string.KronoBackupResetTitle));
        builder.setView(container);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString(R.string.KronoReset), (dialog, which) -> {
            KronoConfig.resetKronoDefaults(preserveVault[0]);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload, true);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogFiltersUpdated);
            if (!isFragmentAlive()) return;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.KronoBackupResetDone)).show();
        });
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    // -------------------------------------------------------------------------
    // Reusable Import Dialog (Used by KronoBackupActivity AND ChatActivity)
    // -------------------------------------------------------------------------

    public static void openImportFromFile(BaseFragment fragment, File file) {
        if (!isFragmentAlive(fragment)) {
            return;
        }
        AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        fragment.showDialog(progress);

        KronoBackupManager.parseFromFile(file, parsed -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive(fragment)) return;
            showRestoreDialogForParsedBackup(fragment, fragment.getCurrentAccount(), parsed, LocaleController.getString(R.string.KronoBackupImportTitle), null);
        }, err -> {
            try {
                progress.dismiss();
            } catch (Exception ignore) {}
            if (!isFragmentAlive(fragment)) return;
            BulletinFactory.of(fragment).createErrorBulletin(err).show();
        });
    }

    private static void showRestoreDialogForParsedBackup(
            BaseFragment fragment,
            int account,
            KronoBackupManager.ParsedBackup parsed,
            String title,
            Runnable onAfterRestore
    ) {
        if (!isFragmentAlive(fragment) || parsed == null || parsed.metadata == null) {
            return;
        }
        KronoBackupManager.BackupMetadata meta = parsed.metadata;
        StringBuilder subtitle = new StringBuilder();
        subtitle.append(LocaleController.formatString(R.string.KronoBackupDate, KronoBackupManager.formatTimestamp(meta.timestamp)));
        if (!TextUtils.isEmpty(meta.deviceModel)) {
            subtitle.append("\n").append(LocaleController.formatString(R.string.KronoBackupDevice, meta.deviceModel));
        }
        subtitle.append("\n").append(LocaleController.formatString(R.string.KronoBackupSize, AndroidUtilities.formatFileSize(meta.rawByteSize)));
        subtitle.append("\n\n").append(LocaleController.getString(R.string.KronoBackupChooseRestoreCats));

        KronoBackupManager.BackupOptions defaults = new KronoBackupManager.BackupOptions();
        defaults.includeKrono = meta.hasKrono;
        defaults.includeTelegramMain = meta.hasTelegramMain;
        defaults.includeTelegramTheme = meta.hasTelegramTheme;
        defaults.includeTelegramNotifications = meta.hasTelegramNotifications;
        defaults.includeHiddenVault = meta.hasHiddenVault;

        showCategoryPickerDialog(
                fragment,
                title,
                subtitle.toString(),
                LocaleController.getString(R.string.KronoRestore),
                defaults,
                meta,
                false,
                chosen -> {
                    if (!isFragmentAlive(fragment)) return;
                    AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
                    progress.setCanCancel(false);
                    fragment.showDialog(progress);

                    Utilities.globalQueue.postRunnable(() -> {
                        try {
                            KronoBackupManager.RestoreResult res = KronoBackupManager.applyBackup(account, parsed, chosen);
                            AndroidUtilities.runOnUIThread(() -> {
                                try {
                                    progress.dismiss();
                                } catch (Exception ignore) {}
                                if (onAfterRestore != null) {
                                    onAfterRestore.run();
                                }
                                if (!isFragmentAlive(fragment)) return;
                                if (res.requiresRestart) {
                                    showRestartPrompt(fragment, res);
                                } else {
                                    String msg = LocaleController.formatString(R.string.KronoBackupRestoredCount, res.appliedKeysCount);
                                    if (res.restoredVault) {
                                        msg += LocaleController.getString(R.string.KronoBackupVaultPinKept);
                                    }
                                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, msg).show();
                                }
                            });
                        } catch (Exception e) {
                            FileLog.e(e);
                            String errMsg = e.getMessage() != null ? e.getMessage() : LocaleController.getString(R.string.KronoBackupApplyErr);
                            AndroidUtilities.runOnUIThread(() -> {
                                try {
                                    progress.dismiss();
                                } catch (Exception ignore) {}
                                if (!isFragmentAlive(fragment)) return;
                                BulletinFactory.of(fragment).createErrorBulletin(errMsg).show();
                            });
                        }
                    });
                }
        );
    }

    private static void showCategoryPickerDialog(
            BaseFragment fragment,
            String title,
            String description,
            String positiveButtonText,
            KronoBackupManager.BackupOptions initial,
            KronoBackupManager.BackupMetadata availableFilter,
            Utilities.Callback<KronoBackupManager.BackupOptions> onConfirm
    ) {
        showCategoryPickerDialog(fragment, title, description, positiveButtonText, initial, availableFilter, false, onConfirm);
    }

    private static void showCategoryPickerDialog(
            BaseFragment fragment,
            String title,
            String description,
            String positiveButtonText,
            KronoBackupManager.BackupOptions initial,
            KronoBackupManager.BackupMetadata availableFilter,
            boolean lockKronoChecked,
            Utilities.Callback<KronoBackupManager.BackupOptions> onConfirm
    ) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) return;

        KronoBackupManager.BackupOptions opts = new KronoBackupManager.BackupOptions();
        opts.includeKrono = lockKronoChecked || initial.includeKrono;
        opts.includeTelegramMain = initial.includeTelegramMain;
        opts.includeTelegramTheme = initial.includeTelegramTheme;
        opts.includeTelegramNotifications = initial.includeTelegramNotifications;
        opts.includeHiddenVault = initial.includeHiddenVault;

        ScrollView scrollView = new ScrollView(ctx);
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), 0);
        scrollView.addView(container);

        if (!TextUtils.isEmpty(description)) {
            TextView descView = new TextView(ctx);
            descView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            descView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
            descView.setText(description);
            container.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        if (availableFilter == null || availableFilter.hasKrono) {
            if (lockKronoChecked) {
                addCategoryCheckbox(container, ctx, LocaleController.getString(R.string.KronoSettingsTitle), LocaleController.getString(R.string.KronoBackupCatKronoAlways), true, true, true, null);
            } else {
                addCategoryCheckbox(container, ctx, LocaleController.getString(R.string.KronoSettingsTitle), LocaleController.getString(R.string.KronoBackupCatKronoDesc), opts.includeKrono, true, false, checked -> opts.includeKrono = checked);
            }
        }
        if (availableFilter == null || availableFilter.hasTelegramMain) {
            addCategoryCheckbox(container, ctx, LocaleController.getString(R.string.KronoBackupCatTelegram), LocaleController.getString(R.string.KronoBackupCatTelegramDesc), opts.includeTelegramMain, true, false, checked -> opts.includeTelegramMain = checked);
        }
        if (availableFilter == null || availableFilter.hasTelegramTheme) {
            addCategoryCheckbox(container, ctx, LocaleController.getString(R.string.KronoBackupCatTheme), LocaleController.getString(R.string.KronoBackupCatThemeDesc), opts.includeTelegramTheme, true, false, checked -> opts.includeTelegramTheme = checked);
        }
        if (availableFilter == null || availableFilter.hasTelegramNotifications) {
            addCategoryCheckbox(container, ctx, LocaleController.getString(R.string.KronoBackupCatNotif), LocaleController.getString(R.string.KronoBackupCatNotifDesc), opts.includeTelegramNotifications, true, false, checked -> opts.includeTelegramNotifications = checked);
        }
        if (availableFilter == null || availableFilter.hasHiddenVault) {
            addCategoryCheckbox(container, ctx, LocaleController.getString(R.string.KronoBackupCatVault), LocaleController.getString(R.string.KronoBackupCatVaultDesc), opts.includeHiddenVault, false, false, checked -> opts.includeHiddenVault = checked);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title);
        builder.setView(scrollView);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(positiveButtonText, (dialog, which) -> {
            if (!opts.hasAnySelected()) {
                if (isFragmentAlive(fragment)) {
                    BulletinFactory.of(fragment).createErrorBulletin(LocaleController.getString(R.string.KronoBackupSelectOne)).show();
                }
                return;
            }
            if (onConfirm != null) {
                onConfirm.run(opts);
            }
        });
        fragment.showDialog(builder.create());
    }

    private static void addCategoryCheckbox(
            LinearLayout parent,
            Context ctx,
            String title,
            String subtitle,
            boolean initialChecked,
            boolean divider,
            boolean locked,
            Utilities.Callback<Boolean> onToggle
    ) {
        CheckBoxCell cell = new CheckBoxCell(ctx, CheckBoxCell.TYPE_CHECK_BOX_DEFAULT);
        cell.setMultiline(true);
        cell.setText(title + "\n" + subtitle, "", initialChecked, divider);
        if (locked) {
            cell.setEnabled(false);
            cell.setAlpha(0.75f);
        } else {
            cell.setBackground(Theme.getSelectorDrawable(false));
            final boolean[] state = new boolean[]{initialChecked};
            cell.setOnClickListener(v -> {
                state[0] = !state[0];
                cell.setChecked(state[0], true);
                if (onToggle != null) {
                    onToggle.run(state[0]);
                }
            });
        }
        parent.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private static void showRestartPrompt(BaseFragment fragment, KronoBackupManager.RestoreResult res) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(LocaleController.formatString(R.string.KronoBackupRestoredMsg, res.appliedKeysCount));
        if (res.restoredVault) {
            sb.append("\n\n").append(LocaleController.getString(R.string.KronoBackupRestoredVaultNote));
        }
        sb.append("\n\n").append(LocaleController.getString(R.string.KronoBackupRestartNote));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.KronoBackupRestoredTitle));
        builder.setMessage(sb.toString());
        builder.setNegativeButton(LocaleController.getString(R.string.KronoLater), null);
        builder.setPositiveButton(LocaleController.getString(R.string.KronoRestartNow), (dialog, which) -> restartApp(activity));
        fragment.showDialog(builder.create());
    }

    private static void restartApp(Activity activity) {
        try {
            KronoBackupManager.setSuppressObserver(true);
            PackageManager pm = activity.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(activity.getPackageName());
            activity.finishAffinity();
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
            }
            System.exit(0);
        } catch (Throwable e) {
            KronoBackupManager.setSuppressObserver(false);
            FileLog.e(e);
        }
    }

    // -------------------------------------------------------------------------
    // RecyclerView Adapter
    // -------------------------------------------------------------------------

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos == cloudStatusRow
                    || pos == cloudSaveRow
                    || pos == cloudRestoreRow
                    || pos == cloudModeRow
                    || pos == cloudAutoScopeRow
                    || pos == cloudDeleteRow
                    || pos == sendSavedMessagesRow
                    || pos == exportFileRow
                    || pos == importFileRow
                    || pos == resetKronoRow;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == cloudHeaderRow || position == localHeaderRow || position == resetHeaderRow) {
                return 0; // HeaderCell
            } else if (position == cloudStatusRow || position == cloudModeRow || position == cloudAutoScopeRow) {
                return 1; // TextDetailSettingsCell (multiline-safe)
            } else if (position == cloudInfoRow || position == localInfoRow || position == resetInfoRow) {
                return 3; // TextInfoPrivacyCell
            } else {
                return 4; // TextCell
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 4:
                default:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int type = holder.getItemViewType();
            if (type == 0) {
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                if (position == cloudHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoBackupCloudHeader));
                } else if (position == localHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoBackupLocalHeader));
                } else if (position == resetHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.KronoBackupResetHeader));
                }
            } else if (type == 1) {
                TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                detailCell.setMultilineDetail(true);
                if (position == cloudStatusRow) {
                    if (cloudLoading) {
                        detailCell.setTextAndValue(LocaleController.getString(R.string.KronoBackupCloudStatus), LocaleController.formatString(R.string.KronoBackupChecking, KronoBackupManager.CLOUD_BACKUP_BOT_USERNAME), true);
                    } else if (!TextUtils.isEmpty(cloudErrorText)) {
                        detailCell.setTextAndValue(LocaleController.getString(R.string.KronoBackupCloudRetry), cloudErrorText, true);
                    } else if (cloudMetadata != null) {
                        String summary = KronoBackupManager.formatTimestamp(cloudMetadata.timestamp)
                                + " · " + AndroidUtilities.formatFileSize(cloudMetadata.rawByteSize)
                                + (TextUtils.isEmpty(cloudMetadata.deviceModel) ? "" : (" · " + cloudMetadata.deviceModel))
                                + "\n" + LocaleController.formatString(R.string.KronoBackupIncludes, cloudMetadata.buildCategoriesSummary());
                        if (KronoConfig.isCloudBackupPending(currentAccount) && KronoConfig.cloudBackupMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE) {
                            summary += "\n" + LocaleController.getString(R.string.KronoBackupAutoSyncPending);
                        }
                        detailCell.setTextAndValue(LocaleController.getString(R.string.KronoBackupCloudAvail), summary, true);
                    } else {
                        String emptyMsg = LocaleController.getString(KronoConfig.cloudBackupMode == KronoConfig.CLOUD_BACKUP_ON_CHANGE
                                ? R.string.KronoBackupAutoHint
                                : R.string.KronoBackupManualHint);
                        detailCell.setTextAndValue(LocaleController.getString(R.string.KronoBackupCloudNoShort), emptyMsg, true);
                    }
                } else if (position == cloudModeRow) {
                    detailCell.setTextAndValue(LocaleController.getString(R.string.KronoBackupMode), KronoConfig.getCloudBackupModeName(), true);
                } else if (position == cloudAutoScopeRow) {
                    String scope = KronoBackupManager.BackupOptions.fromAutoConfig().buildCategoriesSummary();
                    detailCell.setTextAndValue(LocaleController.getString(R.string.KronoBackupAutoScope), scope, true);
                }
            } else if (type == 3) {
                TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                if (position == cloudInfoRow) {
                    infoCell.setText(LocaleController.formatString(R.string.KronoBackupCloudInfo, KronoBackupManager.CLOUD_BACKUP_BOT_USERNAME));
                } else if (position == localInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoBackupLocalInfo));
                } else if (position == resetInfoRow) {
                    infoCell.setText(LocaleController.getString(R.string.KronoBackupResetInfo));
                }
            } else if (type == 4) {
                TextCell textCell = (TextCell) holder.itemView;
                if (position == cloudSaveRow) {
                    textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupCloudSave), R.drawable.msg_saved, true);
                } else if (position == cloudRestoreRow) {
                    textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupCloudRestore), R.drawable.msg_download, true);
                } else if (position == cloudDeleteRow) {
                    textCell.setColors(Theme.key_text_RedRegular, Theme.key_text_RedRegular);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupCloudDelete), R.drawable.msg_delete, false);
                } else if (position == sendSavedMessagesRow) {
                    textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupSendSavedShort), R.drawable.msg_shareout, true);
                } else if (position == exportFileRow) {
                    textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupExportShort), R.drawable.msg_download, true);
                } else if (position == importFileRow) {
                    textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupImportFile), R.drawable.msg_folders, false);
                } else if (position == resetKronoRow) {
                    textCell.setColors(Theme.key_text_RedRegular, Theme.key_text_RedRegular);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.KronoBackupResetBtn), R.drawable.msg_reset, false);
                }
            }
        }
    }
}
