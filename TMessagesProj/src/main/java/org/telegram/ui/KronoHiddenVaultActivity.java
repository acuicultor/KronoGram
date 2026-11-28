package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.FolderIconHelper;
import org.telegram.messenger.KronoConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KronoHiddenVaultActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int MENU_LOCK = 1;
    private static final int MENU_SETTINGS = 2;

    private final int targetFilterId;
    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private FlagSecureReason flagSecure;
    private boolean authPromptInFlight = false;

    private final ArrayList<RowItem> items = new ArrayList<>();

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FOLDER = 1;
    private static final int TYPE_DIALOG = 2;
    private static final int TYPE_SHADOW = 3;
    private static final int TYPE_INFO = 4;

    private static class RowItem {
        final int viewType;
        final String text;
        final MessagesController.DialogFilter filter;
        final TLRPC.Dialog dialog;
        final boolean divider;

        RowItem(int viewType, String text, MessagesController.DialogFilter filter, TLRPC.Dialog dialog, boolean divider) {
            this.viewType = viewType;
            this.text = text;
            this.filter = filter;
            this.dialog = dialog;
            this.divider = divider;
        }
    }

    public KronoHiddenVaultActivity() {
        this(0);
    }

    public KronoHiddenVaultActivity(int targetFilterId) {
        super();
        this.targetFilterId = targetFilterId;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        getNotificationCenter().addObserver(this, NotificationCenter.dialogsNeedReload);
        getNotificationCenter().addObserver(this, NotificationCenter.dialogFiltersUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        rebuildData();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        authPromptInFlight = false;
        if (flagSecure != null) {
            flagSecure.detach();
            flagSecure = null;
        }
        super.onFragmentDestroy();
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogsNeedReload);
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogFiltersUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getParentActivity();
        if (activity != null) {
            Window window = activity.getWindow();
            if (window != null) {
                if (flagSecure == null) {
                    flagSecure = new FlagSecureReason(window, () -> true);
                }
                flagSecure.attach();
            }
        }
        if (!KronoConfig.isHiddenVaultUnlocked()) {
            if (activity != null && !KronoHiddenLockHelper.isProtectionConfigured(activity)) {
                KronoConfig.setHiddenVaultUnlocked(true);
            } else if (!authPromptInFlight) {
                authPromptInFlight = true;
                KronoHiddenLockHelper.authenticate(this, LocaleController.getString(R.string.KronoVaultTitle), LocaleController.getString(R.string.KronoVaultVerifySubtitle), () -> {
                    authPromptInFlight = false;
                    if (!isFinishing() && getParentActivity() != null) {
                        rebuildData();
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }, () -> {
                    authPromptInFlight = false;
                    if (!KronoConfig.isHiddenVaultUnlocked() && !isFinishing()) {
                        finishFragment(false);
                    }
                });
                return;
            } else {
                return;
            }
        }
        rebuildData();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBecomeFullyHidden() {
        if (flagSecure != null) {
            flagSecure.detach();
        }
        super.onBecomeFullyHidden();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        if (targetFilterId != 0) {
            MessagesController.DialogFilter filter = getMessagesController().dialogFiltersById.get(targetFilterId);
            actionBar.setTitle(filter != null ? filter.name : LocaleController.getString(R.string.KronoHiddenFolderTitle));
        } else {
            actionBar.setTitle(LocaleController.getString(R.string.KronoVaultTitle));
        }

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_LOCK, R.drawable.msg_pin_code).setContentDescription(LocaleController.getString(R.string.KronoVaultLockNow));
        if (targetFilterId == 0) {
            menu.addItem(MENU_SETTINGS, R.drawable.msg_settings).setContentDescription(LocaleController.getString(R.string.KronoVaultSettings));
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_LOCK) {
                    KronoConfig.setHiddenVaultUnlocked(false);
                    finishFragment();
                } else if (id == MENU_SETTINGS) {
                    presentFragment(new KronoSettingsActivity(KronoSettingsActivity.SECTION_PRIVACY));
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            RowItem item = items.get(position);
            if (item.viewType == TYPE_FOLDER && item.filter != null) {
                presentFragment(new KronoHiddenVaultActivity(item.filter.id));
            } else if (item.viewType == TYPE_DIALOG && item.dialog != null) {
                openDialog(item.dialog.id);
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return false;
            }
            RowItem item = items.get(position);
            if (item.viewType == TYPE_FOLDER && item.filter != null) {
                showFolderOptions(item.filter);
                return true;
            } else if (item.viewType == TYPE_DIALOG && item.dialog != null) {
                showDialogOptions(item.dialog);
                return true;
            }
            return false;
        });

        return fragmentView;
    }

    private void openDialog(long dialogId) {
        Bundle args = new Bundle();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
        } else if (dialogId > 0) {
            args.putLong("user_id", dialogId);
        } else {
            args.putLong("chat_id", -dialogId);
        }
        presentFragment(new ChatActivity(args));
    }

    private void showFolderOptions(MessagesController.DialogFilter filter) {
        if (getParentActivity() == null || filter == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(filter.name);
        builder.setItems(new CharSequence[]{
                LocaleController.getString(R.string.KronoVaultOpenFolder),
                LocaleController.getString(R.string.KronoVaultUnhideFolder)
        }, (d, which) -> {
            if (which == 0) {
                presentFragment(new KronoHiddenVaultActivity(filter.id));
            } else if (which == 1) {
                KronoConfig.setFilterHidden(currentAccount, filter.id, false);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_archive_show, LocaleController.getString(R.string.KronoVaultFolderRestored)).show();
                rebuildData();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        });
        showDialog(builder.create());
    }

    private void showDialogOptions(TLRPC.Dialog dialog) {
        if (getParentActivity() == null || dialog == null) {
            return;
        }
        boolean isDirectlyHidden = KronoConfig.isDialogHiddenDirectly(currentAccount, dialog.id);
        boolean hasUnread = dialog.unread_count > 0 || dialog.unread_mark;

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();

        labels.add(LocaleController.getString(R.string.KronoVaultOpenChat));
        actions.add(() -> openDialog(dialog.id));

        if (isDirectlyHidden) {
            labels.add(LocaleController.getString(R.string.KronoVaultUnhideChat));
            actions.add(() -> {
                KronoConfig.setDialogHidden(currentAccount, dialog.id, false);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_archive_show, LocaleController.getString(R.string.KronoVaultChatRestored)).show();
                rebuildData();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            });
        }

        if (hasUnread) {
            labels.add(LocaleController.getString(R.string.MarkAsRead));
            actions.add(() -> {
                getMessagesController().markDialogAsRead(dialog.id, dialog.top_message, 0, dialog.last_message_date, false, 0, 0, true, 0);
                rebuildData();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setItems(labels.toArray(new CharSequence[0]), (d, which) -> {
            if (which >= 0 && which < actions.size()) {
                actions.get(which).run();
            }
        });
        showDialog(builder.create());
    }

    private void rebuildData() {
        items.clear();
        MessagesController mc = getMessagesController();
        ArrayList<TLRPC.Dialog> allDialogs = mc.getAllDialogs();

        if (targetFilterId != 0) {
            MessagesController.DialogFilter filter = mc.dialogFiltersById.get(targetFilterId);
            if (filter != null) {
                ArrayList<TLRPC.Dialog> matched = new ArrayList<>();
                for (int i = 0; i < allDialogs.size(); i++) {
                    TLRPC.Dialog d = allDialogs.get(i);
                    if (d != null && filter.includesDialog(getAccountInstance(), d.id)) {
                        matched.add(d);
                    }
                }
                if (!matched.isEmpty()) {
                    items.add(new RowItem(TYPE_HEADER, filter.name, null, null, false));
                    for (int i = 0; i < matched.size(); i++) {
                        items.add(new RowItem(TYPE_DIALOG, null, null, matched.get(i), i < matched.size() - 1));
                    }
                    items.add(new RowItem(TYPE_INFO, LocaleController.getString(R.string.KronoVaultFolderHint), null, null, false));
                } else {
                    items.add(new RowItem(TYPE_INFO, LocaleController.getString(R.string.KronoVaultFolderEmpty), null, null, false));
                }
            } else {
                items.add(new RowItem(TYPE_INFO, LocaleController.getString(R.string.KronoVaultFolderMissing), null, null, false));
            }
            return;
        }

        // Hidden folders
        List<Integer> hiddenFilterIds = KronoConfig.getHiddenFilterIds(currentAccount);
        ArrayList<MessagesController.DialogFilter> hiddenFilters = new ArrayList<>();
        ArrayList<MessagesController.DialogFilter> allFilters = mc.getDialogFilters();
        for (int i = 0; i < allFilters.size(); i++) {
            MessagesController.DialogFilter f = allFilters.get(i);
            if (f != null && !f.isDefault() && hiddenFilterIds.contains(f.id)) {
                hiddenFilters.add(f);
            }
        }
        if (!hiddenFilters.isEmpty()) {
            items.add(new RowItem(TYPE_HEADER, LocaleController.getString(R.string.KronoVaultHiddenFolders), null, null, false));
            for (int i = 0; i < hiddenFilters.size(); i++) {
                items.add(new RowItem(TYPE_FOLDER, null, hiddenFilters.get(i), null, i < hiddenFilters.size() - 1));
            }
        }

        // Directly hidden chats
        Set<Long> directHiddenSet = new HashSet<>(KronoConfig.getHiddenDialogs(currentAccount));
        ArrayList<TLRPC.Dialog> directHiddenDialogs = new ArrayList<>();
        if (!directHiddenSet.isEmpty()) {
            for (int i = 0; i < allDialogs.size(); i++) {
                TLRPC.Dialog d = allDialogs.get(i);
                if (d != null && directHiddenSet.contains(d.id)) {
                    directHiddenDialogs.add(d);
                }
            }
        }
        if (!directHiddenDialogs.isEmpty()) {
            if (!hiddenFilters.isEmpty()) {
                items.add(new RowItem(TYPE_SHADOW, null, null, null, false));
            }
            items.add(new RowItem(TYPE_HEADER, LocaleController.getString(R.string.KronoVaultHiddenChats), null, null, false));
            for (int i = 0; i < directHiddenDialogs.size(); i++) {
                items.add(new RowItem(TYPE_DIALOG, null, null, directHiddenDialogs.get(i), i < directHiddenDialogs.size() - 1));
            }
        }

        if (hiddenFilters.isEmpty() && directHiddenDialogs.isEmpty()) {
            items.add(new RowItem(TYPE_INFO, LocaleController.getString(R.string.KronoVaultEmptyHint), null, null, false));
        } else {
            items.add(new RowItem(TYPE_INFO, LocaleController.getString(R.string.KronoVaultMainHint), null, null, false));
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload || id == NotificationCenter.dialogFiltersUpdated || id == NotificationCenter.updateInterfaces) {
            rebuildData();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        ListAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == TYPE_FOLDER || type == TYPE_DIALOG;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_FOLDER:
                    view = new TextSettingsCell(mContext, getResourceProvider());
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_DIALOG:
                    DialogCell cell = new DialogCell(null, mContext, false, false, currentAccount, getResourceProvider());
                    cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view = cell;
                    break;
                case TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext);
                    break;
                case TYPE_INFO:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowItem item = items.get(position);
            switch (item.viewType) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setText(item.text);
                    break;
                }
                case TYPE_FOLDER: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    MessagesController.DialogFilter filter = item.filter;
                    int unread = filter.unreadCount;
                    String summary = unread > 0 ? LocaleController.formatString(R.string.KronoUnreadCount, unread) : LocaleController.getString(R.string.KronoOpen);
                    int iconRes = FolderIconHelper.getTabIconResId(currentAccount, filter, false);
                    cell.setIcon(iconRes != 0 ? iconRes : R.drawable.msg_secret);
                    cell.setTextAndValue(filter.name, summary, item.divider);
                    break;
                }
                case TYPE_DIALOG: {
                    DialogCell cell = (DialogCell) holder.itemView;
                    cell.useSeparator = item.divider;
                    cell.setDialog(item.dialog, 0, 0);
                    break;
                }
                case TYPE_INFO: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setText(item.text);
                    break;
                }
            }
        }
    }
}
