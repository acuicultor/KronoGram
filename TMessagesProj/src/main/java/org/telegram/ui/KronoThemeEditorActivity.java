package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.ActionBar.ThemeColors;
import org.telegram.ui.ActionBar.ThemeKeyLocalizer;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.KronoThemeColorPickerSheet;
import org.telegram.ui.Components.KronoThemeEditorView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.WallpaperUpdater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Full-screen KronoGram Theme Studio inside KronoGram Settings -> Appearance.
 * Supports safe cloning of built-in themes, category browsing across all theme keys,
 * live mini-preview, HEX/RGBA/Contrast color picker, Undo/Redo (single & batch),
 * Accent Palette harmonizer, Find & Replace color, .attheme import/export, and Floating Live Editor.
 */
public class KronoThemeEditorActivity extends BaseFragment {

    private static final int REQUEST_IMPORT_ATTHEME = 49102;
    private static final int MAX_UNDO_STEPS = 50;
    private static final String PREFS_NAME = "krono_theme_editor";
    private static final String KEY_SHOW_TECH = "show_tech_keys";

    private static final int MENU_SEARCH = 1;
    private static final int MENU_UNDO = 2;
    private static final int MENU_REDO = 3;
    private static final int MENU_SAVE = 4;
    private static final int MENU_MORE = 5;

    private static final int SUB_FLOATING = 101;
    private static final int SUB_ACCENT = 102;
    private static final int SUB_REPLACE = 103;
    private static final int SUB_WALLPAPER = 104;
    private static final int SUB_SWITCH = 105;
    private static final int SUB_DUPLICATE = 106;
    private static final int SUB_RENAME = 107;
    private static final int SUB_IMPORT = 108;
    private static final int SUB_EXPORT = 109;
    private static final int SUB_TECH_KEYS = 110;
    private static final int SUB_RESET_ALL = 111;
    private static final int SUB_DELETE = 112;

    private static final int[] ACCENT_KEYS = new int[]{
            Theme.key_actionBarDefault,
            Theme.key_windowBackgroundWhiteBlueHeader,
            Theme.key_windowBackgroundWhiteBlueText,
            Theme.key_windowBackgroundWhiteBlueText2,
            Theme.key_windowBackgroundWhiteBlueText4,
            Theme.key_windowBackgroundWhiteValueText,
            Theme.key_windowBackgroundWhiteLinkText,
            Theme.key_switchTrackChecked,
            Theme.key_switch2TrackChecked,
            Theme.key_checkbox,
            Theme.key_checkboxSquareBackground,
            Theme.key_radioBackgroundChecked,
            Theme.key_chats_actionBackground,
            Theme.key_chats_unreadCounter,
            Theme.key_dialogButton,
            Theme.key_dialogTextBlue,
            Theme.key_dialogTextBlue2,
            Theme.key_dialogTextBlue4,
            Theme.key_dialogTextLink,
            Theme.key_dialogCheckboxSquareBackground,
            Theme.key_dialogRadioBackgroundChecked,
            Theme.key_dialogRoundCheckBox,
            Theme.key_chat_messageLinkIn,
            Theme.key_chat_messageLinkOut,
            Theme.key_chat_outBubble,
            Theme.key_avatar_backgroundActionBarBlue,
            Theme.key_featuredStickers_addButton,
            Theme.key_player_progress
    };

    private static final class KeyItem {
        final int key;
        final String rawName;
        final String localizedTitle;
        final int category;

        KeyItem(int key, String rawName, String localizedTitle, int category) {
            this.key = key;
            this.rawName = rawName;
            this.localizedTitle = localizedTitle;
            this.category = category;
        }
    }

    private static final class ColorDelta {
        final int key;
        final int oldColor;
        final boolean oldWasDefault;
        final int newColor;
        final boolean newWasDefault;

        ColorDelta(int key, int oldColor, boolean oldWasDefault, int newColor, boolean newWasDefault) {
            this.key = key;
            this.oldColor = oldColor;
            this.oldWasDefault = oldWasDefault;
            this.newColor = newColor;
            this.newWasDefault = newWasDefault;
        }
    }

    private static final class UndoAction {
        final String label;
        final ArrayList<ColorDelta> deltas;

        UndoAction(String label, ArrayList<ColorDelta> deltas) {
            this.label = label;
            this.deltas = deltas;
        }
    }

    private final ArrayList<KeyItem> allKeys = new ArrayList<>();
    private final ArrayList<KeyItem> filteredKeys = new ArrayList<>();
    private final ArrayDeque<UndoAction> undoStack = new ArrayDeque<>();
    private final ArrayDeque<UndoAction> redoStack = new ArrayDeque<>();

    private int selectedCategory = KronoThemeColorPickerSheet.CAT_ALL;
    private String searchQuery = "";
    private boolean showTechKeys = true;
    private boolean hasUnsavedChanges = false;
    private boolean refreshPosted = false;

    private LinearLayout readOnlyBanner;
    private LinearLayout categoryChipsRow;
    private LivePreviewCard previewCard;
    private RecyclerListView listView;
    private KeysAdapter adapter;
    private TextView emptyView;

    private ActionBarMenuItem undoItem;
    private ActionBarMenuItem redoItem;
    private ActionBarMenuItem saveItem;
    private ActionBarMenuSubItem deleteSubItem;

    private WallpaperUpdater wallpaperUpdater;

    private final Runnable liveRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshPosted = false;
            try {
                Theme.applyCommonTheme();
                Theme.applyDialogsTheme();
                Theme.applyProfileTheme();
                Theme.applyChatTheme(false, false);
                ThemeBind.updateAll();
                if (LaunchActivity.instance != null) {
                    LaunchActivity.instance.notifyThemeColorsChanged();
                }
                if (previewCard != null) {
                    previewCard.refreshColors();
                }
                updateActionBarChrome();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    };

    private void scheduleLiveRefresh() {
        if (!refreshPosted) {
            refreshPosted = true;
            AndroidUtilities.runOnUIThread(liveRefreshRunnable);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        showTechKeys = prefs.getBoolean(KEY_SHOW_TECH, true);
        buildAllKeysIndex();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (wallpaperUpdater != null) {
            try {
                wallpaperUpdater.cleanup();
            } catch (Exception e) {
                FileLog.e(e);
            }
            wallpaperUpdater = null;
        }
    }

    private void buildAllKeysIndex() {
        allKeys.clear();
        for (int key = 0; key < Theme.colorsCount; key++) {
            if (key == Theme.key_wallpaperFileOffset) {
                continue;
            }
            String raw = ThemeColors.getStringName(key);
            if (TextUtils.isEmpty(raw)) {
                continue;
            }
            String localized = ThemeKeyLocalizer.getLocalizedTitle(raw);
            int cat = KronoThemeColorPickerSheet.categorizeKey(raw);
            allKeys.add(new KeyItem(key, raw, localized, cat));
        }
        Collections.sort(allKeys, (a, b) -> a.localizedTitle.compareToIgnoreCase(b.localizedTitle));
    }

    private static boolean isEditableCustomTheme(Theme.ThemeInfo info) {
        return info != null && !TextUtils.isEmpty(info.pathToFile) && info.assetName == null;
    }

    private Theme.ThemeInfo getActiveThemeSafe() {
        Theme.ThemeInfo active = Theme.getActiveTheme();
        return active != null ? active : Theme.getCurrentTheme();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.KronoThemeEditorTitle));

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(MENU_SEARCH, R.drawable.msg_search).setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchCollapse() {
                        searchQuery = "";
                        applyFilters();
                    }

                    @Override
                    public void onTextChanged(EditText editText) {
                        searchQuery = editText.getText() != null ? editText.getText().toString() : "";
                        applyFilters();
                    }
                });
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.KronoThemeEditorSearchHint));

        undoItem = menu.addItem(MENU_UNDO, R.drawable.iv_undo);
        undoItem.setContentDescription(LocaleController.getString(R.string.KronoThemeEditorUndo));

        redoItem = menu.addItem(MENU_REDO, R.drawable.iv_redo);
        redoItem.setContentDescription(LocaleController.getString(R.string.KronoThemeEditorRedo));

        saveItem = menu.addItem(MENU_SAVE, R.drawable.ic_ab_done);
        saveItem.setContentDescription(LocaleController.getString(R.string.KronoThemeEditorSave));

        ActionBarMenuItem moreItem = menu.addItem(MENU_MORE, R.drawable.ic_ab_other);
        moreItem.setContentDescription(LocaleController.getString(R.string.KronoThemeEditorTools));
        moreItem.addSubItem(SUB_FLOATING, R.drawable.msg_theme, LocaleController.getString(R.string.KronoThemeEditorFloatingMode));
        moreItem.addSubItem(SUB_ACCENT, R.drawable.msg_palette, LocaleController.getString(R.string.KronoThemeEditorAccentTool));
        moreItem.addSubItem(SUB_REPLACE, R.drawable.msg_edit, LocaleController.getString(R.string.KronoThemeEditorReplaceTool));
        moreItem.addSubItem(SUB_WALLPAPER, R.drawable.msg_photos, LocaleController.getString(R.string.KronoThemeEditorWallpaper));
        moreItem.addSubItem(SUB_SWITCH, R.drawable.msg_list, LocaleController.getString(R.string.KronoThemeEditorSwitchTheme));
        moreItem.addSubItem(SUB_DUPLICATE, R.drawable.msg_copy, LocaleController.getString(R.string.KronoThemeEditorDuplicateTheme));
        moreItem.addSubItem(SUB_RENAME, R.drawable.msg_input_attach2, LocaleController.getString(R.string.KronoThemeEditorRenameTheme));
        moreItem.addSubItem(SUB_IMPORT, R.drawable.msg_openin, LocaleController.getString(R.string.KronoThemeEditorImportTheme));
        moreItem.addSubItem(SUB_EXPORT, R.drawable.msg_share, LocaleController.getString(R.string.KronoThemeEditorExportTheme));
        moreItem.addSubItem(SUB_TECH_KEYS, R.drawable.msg_info, LocaleController.getString(R.string.KronoThemeEditorShowTechKeys));
        moreItem.addSubItem(SUB_RESET_ALL, R.drawable.msg_reset, LocaleController.getString(R.string.KronoThemeEditorResetAll));
        deleteSubItem = moreItem.addSubItem(SUB_DELETE, R.drawable.msg_delete, LocaleController.getString(R.string.Delete));
        deleteSubItem.setColors(Theme.getColor(Theme.key_text_RedRegular), Theme.getColor(Theme.key_text_RedRegular));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (onBackPressed(true)) {
                        finishFragment();
                    }
                } else if (id == MENU_UNDO) {
                    performUndo();
                } else if (id == MENU_REDO) {
                    performRedo();
                } else if (id == MENU_SAVE) {
                    saveActiveTheme(true);
                } else if (id == SUB_FLOATING) {
                    launchFloatingMode();
                } else if (id == SUB_ACCENT) {
                    ensureEditableThen(KronoThemeEditorActivity.this::openAccentTool);
                } else if (id == SUB_REPLACE) {
                    ensureEditableThen(KronoThemeEditorActivity.this::openFindAndReplaceTool);
                } else if (id == SUB_WALLPAPER) {
                    ensureEditableThen(() -> {
                        if (wallpaperUpdater != null) {
                            wallpaperUpdater.showAlert(true);
                        }
                    });
                } else if (id == SUB_SWITCH) {
                    showSwitchCustomThemeDialog();
                } else if (id == SUB_DUPLICATE) {
                    promptCreateEditableCopy(false);
                } else if (id == SUB_RENAME) {
                    promptRenameCurrentTheme();
                } else if (id == SUB_IMPORT) {
                    launchImportAtthemePicker();
                } else if (id == SUB_EXPORT) {
                    exportActiveTheme();
                } else if (id == SUB_TECH_KEYS) {
                    showTechKeys = !showTechKeys;
                    ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_SHOW_TECH, showTechKeys).apply();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                } else if (id == SUB_RESET_ALL) {
                    ensureEditableThen(KronoThemeEditorActivity.this::confirmResetAllModified);
                } else if (id == SUB_DELETE) {
                    confirmDeleteCurrentTheme();
                }
            }
        });

        if (getParentActivity() != null) {
            wallpaperUpdater = new WallpaperUpdater(getParentActivity(), this, new WallpaperUpdater.WallpaperUpdaterDelegate() {
                @Override
                public void didSelectWallpaper(File file, Bitmap bitmap, boolean gallery) {
                    Theme.ThemeInfo active = getActiveThemeSafe();
                    if (isEditableCustomTheme(active)) {
                        Theme.setThemeWallpaper(active, bitmap, file);
                        hasUnsavedChanges = true;
                        scheduleLiveRefresh();
                        BulletinFactory.of(KronoThemeEditorActivity.this).createSimpleBulletin(
                                R.drawable.msg_photos,
                                LocaleController.getString(R.string.KronoThemeEditorThemeSaved)
                        ).show();
                    }
                }

                @Override
                public void needOpenColorPicker() {
                    openKeyPicker(findKeyItem(Theme.key_chat_wallpaper));
                }
            });
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        readOnlyBanner = new LinearLayout(context);
        readOnlyBanner.setOrientation(LinearLayout.VERTICAL);
        readOnlyBanner.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        readOnlyBanner.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        TextView roText = new TextView(context);
        roText.setText(LocaleController.getString(R.string.KronoThemeEditorReadOnlyBanner));
        roText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        roText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        readOnlyBanner.addView(roText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView createCopyBtn = new TextView(context);
        createCopyBtn.setText(LocaleController.getString(R.string.KronoThemeEditorCreateCopy));
        createCopyBtn.setTextColor(0xFFFFFFFF);
        createCopyBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        createCopyBtn.setTypeface(AndroidUtilities.bold());
        createCopyBtn.setGravity(Gravity.CENTER);
        createCopyBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(9), AndroidUtilities.dp(16), AndroidUtilities.dp(9));
        createCopyBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(10),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)
        ));
        createCopyBtn.setOnClickListener(v -> promptCreateEditableCopy(false));
        readOnlyBanner.addView(createCopyBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(readOnlyBanner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        previewCard = new LivePreviewCard(context);
        root.addView(previewCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 10, 12, 8));

        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(2), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        categoryChipsRow = new LinearLayout(context);
        categoryChipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsScroll.addView(categoryChipsRow, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        root.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout listContainer = new FrameLayout(context);
        listContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new KeysAdapter(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= filteredKeys.size()) {
                return;
            }
            KeyItem item = filteredKeys.get(position);
            ensureEditableThen(() -> openKeyPicker(item));
        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (position < 0 || position >= filteredKeys.size()) {
                return false;
            }
            showKeyQuickMenu(filteredKeys.get(position));
            return true;
        });
        listContainer.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new TextView(context);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyView.setVisibility(View.GONE);
        listContainer.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        root.addView(listContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));

        rebuildCategoryChips(context);
        updateStateUi();
        applyFilters();

        return fragmentView;
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (hasUnsavedChanges && isEditableCustomTheme(getActiveThemeSafe()) && getParentActivity() != null) {
            if (!invoked) {
                return false;
            }
            Theme.ThemeInfo active = getActiveThemeSafe();
            String name = active != null ? active.name : "";
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString(R.string.KronoThemeEditorUnsavedTitle));
            builder.setMessage(LocaleController.formatString(R.string.KronoThemeEditorUnsavedMessage, name));
            builder.setPositiveButton(LocaleController.getString(R.string.KronoThemeEditorSave), (d, w) -> {
                saveActiveTheme(true);
                finishFragment();
            });
            builder.setNeutralButton(LocaleController.getString(R.string.KronoThemeEditorDiscard), (d, w) -> {
                hasUnsavedChanges = false;
                if (active != null) {
                    Theme.applyTheme(active);
                    if (parentLayout != null) {
                        parentLayout.rebuildAllFragmentViews(false, false);
                    }
                }
                finishFragment();
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            showDialog(builder.create());
            return false;
        }
        return true;
    }

    private KeyItem findKeyItem(int key) {
        for (int i = 0; i < allKeys.size(); i++) {
            if (allKeys.get(i).key == key) {
                return allKeys.get(i);
            }
        }
        String raw = ThemeColors.getStringName(key);
        return new KeyItem(key, raw, ThemeKeyLocalizer.getLocalizedTitle(raw), KronoThemeColorPickerSheet.categorizeKey(raw));
    }

    private int countModifiedKeys() {
        int count = 0;
        for (int i = 0; i < allKeys.size(); i++) {
            if (Theme.hasThemeKey(allKeys.get(i).key)) {
                count++;
            }
        }
        return count;
    }

    private void updateStateUi() {
        Theme.ThemeInfo active = getActiveThemeSafe();
        boolean editable = isEditableCustomTheme(active);
        if (readOnlyBanner != null) {
            readOnlyBanner.setVisibility(editable ? View.GONE : View.VISIBLE);
        }
        if (deleteSubItem != null) {
            deleteSubItem.setVisibility(editable ? View.VISIBLE : View.GONE);
        }
        if (undoItem != null) {
            boolean canUndo = !undoStack.isEmpty();
            undoItem.setEnabled(canUndo);
            undoItem.setAlpha(canUndo ? 1f : 0.35f);
        }
        if (redoItem != null) {
            boolean canRedo = !redoStack.isEmpty();
            redoItem.setEnabled(canRedo);
            redoItem.setAlpha(canRedo ? 1f : 0.35f);
        }
        if (saveItem != null) {
            saveItem.setVisibility(editable ? View.VISIBLE : View.GONE);
        }
        if (actionBar != null) {
            String name = active != null && !TextUtils.isEmpty(active.name)
                    ? active.name
                    : LocaleController.getString(R.string.KronoDefault);
            int mod = countModifiedKeys();
            actionBar.setSubtitle(name + " · " + LocaleController.formatString(R.string.KronoThemeEditorModifiedCount, mod));
        }
        if (previewCard != null) {
            previewCard.refreshColors();
        }
        if (getParentActivity() != null && categoryChipsRow != null) {
            rebuildCategoryChips(getParentActivity());
        }
    }

    private void updateActionBarChrome() {
        if (actionBar != null) {
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
            actionBar.setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle));
            actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        }
    }

    private void rebuildCategoryChips(Context context) {
        categoryChipsRow.removeAllViews();
        int[][] cats = new int[][]{
                {KronoThemeColorPickerSheet.CAT_ALL, R.string.KronoThemeEditorCatAll},
                {KronoThemeColorPickerSheet.CAT_MODIFIED, R.string.KronoThemeEditorCatModified},
                {KronoThemeColorPickerSheet.CAT_ACTION_BAR, R.string.KronoThemeEditorCatActionBar},
                {KronoThemeColorPickerSheet.CAT_WINDOW, R.string.KronoThemeEditorCatWindow},
                {KronoThemeColorPickerSheet.CAT_CHATS, R.string.KronoThemeEditorCatChats},
                {KronoThemeColorPickerSheet.CAT_DIALOGS, R.string.KronoThemeEditorCatDialogs},
                {KronoThemeColorPickerSheet.CAT_CONTROLS, R.string.KronoThemeEditorCatControls},
                {KronoThemeColorPickerSheet.CAT_PROFILE, R.string.KronoThemeEditorCatProfile},
                {KronoThemeColorPickerSheet.CAT_MEDIA, R.string.KronoThemeEditorCatMedia},
                {KronoThemeColorPickerSheet.CAT_CALLS, R.string.KronoThemeEditorCatCalls},
                {KronoThemeColorPickerSheet.CAT_OTHER, R.string.KronoThemeEditorCatOther}
        };
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        int surface = Theme.getColor(Theme.key_windowBackgroundWhite);
        int textNormal = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);

        for (int[] c : cats) {
            final int catId = c[0];
            String label = LocaleController.getString(c[1]);
            if (catId == KronoThemeColorPickerSheet.CAT_MODIFIED) {
                label = label + " (" + countModifiedKeys() + ")";
            }
            boolean selected = selectedCategory == catId;
            TextView chip = new TextView(context);
            chip.setText(label);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            chip.setTypeface(selected ? AndroidUtilities.bold() : null);
            chip.setTextColor(selected ? 0xFFFFFFFF : textNormal);
            chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(AndroidUtilities.dp(16));
            gd.setColor(selected ? accent : surface);
            if (!selected) {
                gd.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
            }
            chip.setBackground(gd);
            chip.setOnClickListener(v -> {
                selectedCategory = catId;
                rebuildCategoryChips(context);
                applyFilters();
            });
            categoryChipsRow.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        }
    }

    private void applyFilters() {
        filteredKeys.clear();
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < allKeys.size(); i++) {
            KeyItem item = allKeys.get(i);
            if (selectedCategory == KronoThemeColorPickerSheet.CAT_MODIFIED) {
                if (!Theme.hasThemeKey(item.key)) {
                    continue;
                }
            } else if (selectedCategory != KronoThemeColorPickerSheet.CAT_ALL && item.category != selectedCategory) {
                continue;
            }
            if (!TextUtils.isEmpty(q)) {
                String hex = KronoThemeColorPickerSheet.formatHex(Theme.getColor(item.key), true).toLowerCase(Locale.ROOT);
                boolean match = item.localizedTitle.toLowerCase(Locale.ROOT).contains(q)
                        || item.rawName.toLowerCase(Locale.ROOT).contains(q)
                        || hex.contains(q);
                if (!match) {
                    continue;
                }
            }
            filteredKeys.add(item);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (emptyView != null) {
            if (filteredKeys.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(selectedCategory == KronoThemeColorPickerSheet.CAT_MODIFIED && TextUtils.isEmpty(q)
                        ? LocaleController.getString(R.string.KronoThemeEditorNoModified)
                        : LocaleController.getString(R.string.NoResult));
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    private void ensureEditableThen(Runnable action) {
        if (isEditableCustomTheme(getActiveThemeSafe())) {
            action.run();
        } else {
            promptCreateEditableCopy(true, action);
        }
    }

    private void promptCreateEditableCopy(boolean autoContinue) {
        promptCreateEditableCopy(autoContinue, null);
    }

    private void promptCreateEditableCopy(boolean autoContinue, Runnable onCreated) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        Theme.ThemeInfo current = getActiveThemeSafe();
        String baseName = current != null && !TextUtils.isEmpty(current.name) ? current.name : "Krono";
        String suggested = baseName + " Custom";

        FrameLayout box = new FrameLayout(activity);
        box.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        EditTextBoldCursor et = new EditTextBoldCursor(activity);
        et.setText(suggested);
        et.setSelection(suggested.length());
        et.setHint(LocaleController.getString(R.string.KronoThemeEditorNewThemeHint));
        et.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        et.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(et, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.KronoThemeEditorNewThemeTitle));
        builder.setView(box);
        builder.setPositiveButton(LocaleController.getString(R.string.KronoThemeEditorCreateCopy), (dialog, which) -> {
            String name = et.getText() != null ? et.getText().toString().trim() : "";
            if (TextUtils.isEmpty(name)) {
                name = suggested;
            }
            AndroidUtilities.hideKeyboard(et);
            Theme.ThemeInfo newTheme = Theme.createNewTheme(name);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeListUpdated);
            undoStack.clear();
            redoStack.clear();
            hasUnsavedChanges = false;
            updateStateUi();
            applyFilters();
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_theme,
                    LocaleController.formatString(R.string.KronoThemeEditorThemeCreated, newTheme.name)
            ).show();
            if (autoContinue && onCreated != null) {
                onCreated.run();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private ArrayList<Integer> collectTopThemeColors() {
        HashMap<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < allKeys.size(); i++) {
            int c = Theme.getColor(allKeys.get(i).key);
            Integer n = freq.get(c);
            freq.put(c, n == null ? 1 : n + 1);
        }
        ArrayList<Map.Entry<Integer, Integer>> entries = new ArrayList<>(freq.entrySet());
        Collections.sort(entries, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = 0; i < entries.size() && i < 12; i++) {
            res.add(entries.get(i).getKey());
        }
        return res;
    }

    private void openKeyPicker(KeyItem item) {
        if (getParentActivity() == null || item == null) {
            return;
        }
        if (item.key == Theme.key_chat_wallpaper && wallpaperUpdater != null) {
            wallpaperUpdater.showAlert(true);
            return;
        }
        final int oldColor = Theme.getColor(item.key);
        final boolean oldWasDefault = !Theme.hasThemeKey(item.key);
        final boolean opaqueOnly = KronoThemeColorPickerSheet.isOpaqueRequiredKey(item.key);

        KronoThemeColorPickerSheet sheet = new KronoThemeColorPickerSheet(
                getParentActivity(),
                item.localizedTitle,
                item.rawName,
                oldColor,
                opaqueOnly,
                true,
                collectTopThemeColors(),
                new KronoThemeColorPickerSheet.Callback() {
                    @Override
                    public void onColorChanged(int color) {
                        Theme.setColor(item.key, color, false);
                        scheduleLiveRefresh();
                    }

                    @Override
                    public void onColorApplied(int color) {
                        Theme.setColor(item.key, color, false);
                        if (color != oldColor || oldWasDefault) {
                            ArrayList<ColorDelta> d = new ArrayList<>(1);
                            d.add(new ColorDelta(item.key, oldColor, oldWasDefault, color, false));
                            pushUndoAction(new UndoAction(item.localizedTitle, d));
                            hasUnsavedChanges = true;
                        }
                        saveActiveTheme(false);
                        scheduleLiveRefresh();
                        updateStateUi();
                        applyFilters();
                    }

                    @Override
                    public void onResetToDefault() {
                        int def = Theme.getDefaultColor(item.key);
                        Theme.setColor(item.key, def, true);
                        if (!oldWasDefault) {
                            ArrayList<ColorDelta> d = new ArrayList<>(1);
                            d.add(new ColorDelta(item.key, oldColor, false, def, true));
                            pushUndoAction(new UndoAction(item.localizedTitle, d));
                            hasUnsavedChanges = true;
                        }
                        saveActiveTheme(false);
                        scheduleLiveRefresh();
                        updateStateUi();
                        applyFilters();
                    }

                    @Override
                    public void onCancelled() {
                        Theme.setColor(item.key, oldColor, oldWasDefault);
                        scheduleLiveRefresh();
                    }
                }
        );
        showDialog(sheet);
    }

    private void showKeyQuickMenu(KeyItem item) {
        Activity activity = getParentActivity();
        if (activity == null || item == null) {
            return;
        }
        String hex = KronoThemeColorPickerSheet.formatHex(Theme.getColor(item.key), true);
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString(R.string.KronoThemeEditorCopyHex) + " (" + hex + ")",
                LocaleController.getString(R.string.KronoThemeEditorPasteHex),
                LocaleController.getString(R.string.KronoThemeEditorResetKey)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(item.localizedTitle);
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                KronoThemeColorPickerSheet.copyToClipboard(activity, hex);
                BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.msg_copy,
                        LocaleController.formatString(R.string.KronoThemeEditorHexCopied, hex)
                ).show();
            } else if (which == 1) {
                ensureEditableThen(() -> {
                    String clip = KronoThemeColorPickerSheet.readFromClipboard(activity);
                    boolean opaque = KronoThemeColorPickerSheet.isOpaqueRequiredKey(item.key);
                    Integer parsed = KronoThemeColorPickerSheet.parseHexColor(clip, opaque);
                    if (parsed == null) {
                        BulletinFactory.of(this).createSimpleBulletin(
                                R.drawable.msg_info,
                                LocaleController.getString(R.string.KronoThemeEditorInvalidHex)
                        ).show();
                        return;
                    }
                    int oldColor = Theme.getColor(item.key);
                    boolean oldDef = !Theme.hasThemeKey(item.key);
                    Theme.setColor(item.key, parsed, false);
                    ArrayList<ColorDelta> d = new ArrayList<>(1);
                    d.add(new ColorDelta(item.key, oldColor, oldDef, parsed, false));
                    pushUndoAction(new UndoAction(item.localizedTitle, d));
                    hasUnsavedChanges = true;
                    saveActiveTheme(false);
                    scheduleLiveRefresh();
                    updateStateUi();
                    applyFilters();
                });
            } else if (which == 2) {
                ensureEditableThen(() -> {
                    if (!Theme.hasThemeKey(item.key)) {
                        return;
                    }
                    int oldColor = Theme.getColor(item.key);
                    int def = Theme.getDefaultColor(item.key);
                    Theme.setColor(item.key, def, true);
                    ArrayList<ColorDelta> d = new ArrayList<>(1);
                    d.add(new ColorDelta(item.key, oldColor, false, def, true));
                    pushUndoAction(new UndoAction(item.localizedTitle, d));
                    hasUnsavedChanges = true;
                    saveActiveTheme(false);
                    scheduleLiveRefresh();
                    updateStateUi();
                    applyFilters();
                });
            }
        });
        showDialog(builder.create());
    }

    private void pushUndoAction(UndoAction action) {
        undoStack.push(action);
        while (undoStack.size() > MAX_UNDO_STEPS) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private void performUndo() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoAction action = undoStack.pop();
        redoStack.push(action);
        for (int i = action.deltas.size() - 1; i >= 0; i--) {
            ColorDelta d = action.deltas.get(i);
            Theme.setColor(d.key, d.oldColor, d.oldWasDefault);
        }
        hasUnsavedChanges = true;
        saveActiveTheme(false);
        scheduleLiveRefresh();
        updateStateUi();
        applyFilters();
        BulletinFactory.of(this).createSimpleBulletin(
                R.drawable.iv_undo,
                LocaleController.formatString(R.string.KronoThemeEditorUndoApplied, action.label)
        ).show();
    }

    private void performRedo() {
        if (redoStack.isEmpty()) {
            return;
        }
        UndoAction action = redoStack.pop();
        undoStack.push(action);
        for (int i = 0; i < action.deltas.size(); i++) {
            ColorDelta d = action.deltas.get(i);
            Theme.setColor(d.key, d.newColor, d.newWasDefault);
        }
        hasUnsavedChanges = true;
        saveActiveTheme(false);
        scheduleLiveRefresh();
        updateStateUi();
        applyFilters();
        BulletinFactory.of(this).createSimpleBulletin(
                R.drawable.iv_redo,
                LocaleController.formatString(R.string.KronoThemeEditorRedoApplied, action.label)
        ).show();
    }

    private void openAccentTool() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        int seed = Theme.getColor(Theme.key_actionBarDefault);
        KronoThemeColorPickerSheet sheet = new KronoThemeColorPickerSheet(
                activity,
                LocaleController.getString(R.string.KronoThemeEditorAccentTool),
                LocaleController.getString(R.string.KronoThemeEditorAccentToolDesc),
                seed,
                true,
                false,
                collectTopThemeColors(),
                new KronoThemeColorPickerSheet.Callback() {
                    @Override
                    public void onColorChanged(int color) { }

                    @Override
                    public void onColorApplied(int color) {
                        applyAccentHarmony(color | 0xFF000000);
                    }

                    @Override
                    public void onResetToDefault() { }

                    @Override
                    public void onCancelled() { }
                }
        );
        showDialog(sheet);
    }

    private void applyAccentHarmony(int accent) {
        boolean dark = Theme.isCurrentThemeDark();
        int softTrack = ColorUtils.setAlphaComponent(accent, 0x66);
        int bubbleColor = dark
                ? ColorUtils.blendARGB(accent, 0xFF1A1D21, 0.35f)
                : ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.78f);

        ArrayList<ColorDelta> deltas = new ArrayList<>();
        for (int key : ACCENT_KEYS) {
            int oldColor = Theme.getColor(key);
            boolean oldDef = !Theme.hasThemeKey(key);
            int target = accent;
            if (key == Theme.key_switchTrackChecked || key == Theme.key_switch2TrackChecked) {
                target = softTrack;
            } else if (key == Theme.key_chat_outBubble) {
                target = bubbleColor;
            }
            if (KronoThemeColorPickerSheet.isOpaqueRequiredKey(key)) {
                target |= 0xFF000000;
            }
            Theme.setColor(key, target, false);
            deltas.add(new ColorDelta(key, oldColor, oldDef, target, false));
        }
        pushUndoAction(new UndoAction(LocaleController.getString(R.string.KronoThemeEditorAccentTool), deltas));
        hasUnsavedChanges = true;
        saveActiveTheme(false);
        scheduleLiveRefresh();
        updateStateUi();
        applyFilters();
        BulletinFactory.of(this).createSimpleBulletin(
                R.drawable.msg_palette,
                LocaleController.formatString(R.string.KronoThemeEditorAccentApplied, deltas.size())
        ).show();
    }

    private void openFindAndReplaceTool() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        HashMap<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < allKeys.size(); i++) {
            int c = Theme.getColor(allKeys.get(i).key);
            Integer prev = counts.get(c);
            counts.put(c, prev == null ? 1 : prev + 1);
        }
        ArrayList<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(counts.entrySet());
        Collections.sort(sorted, (a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int maxShown = Math.min(24, sorted.size());
        CharSequence[] items = new CharSequence[maxShown];
        final int[] colors = new int[maxShown];
        for (int i = 0; i < maxShown; i++) {
            int c = sorted.get(i).getKey();
            int n = sorted.get(i).getValue();
            colors[i] = c;
            items[i] = KronoThemeColorPickerSheet.formatHex(c, true) + "  ·  "
                    + LocaleController.formatString(R.string.KronoThemeEditorReplaceCount, n);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.KronoThemeEditorReplacePickSource));
        builder.setItems(items, (dialog, which) -> {
            int sourceColor = colors[which];
            AlertDialog.Builder modeBuilder = new AlertDialog.Builder(activity);
            modeBuilder.setTitle(LocaleController.getString(R.string.KronoThemeEditorReplaceTool));
            modeBuilder.setItems(new CharSequence[]{
                    LocaleController.getString(R.string.KronoThemeEditorReplaceExact),
                    LocaleController.getString(R.string.KronoThemeEditorReplaceSimilar)
            }, (d2, modeIdx) -> openReplaceTargetPicker(sourceColor, modeIdx == 1));
            showDialog(modeBuilder.create());
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openReplaceTargetPicker(int sourceColor, boolean similarShades) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        KronoThemeColorPickerSheet picker = new KronoThemeColorPickerSheet(
                activity,
                LocaleController.getString(R.string.KronoThemeEditorReplacePickTarget),
                KronoThemeColorPickerSheet.formatHex(sourceColor, true),
                sourceColor,
                false,
                false,
                collectTopThemeColors(),
                new KronoThemeColorPickerSheet.Callback() {
                    @Override
                    public void onColorChanged(int color) { }

                    @Override
                    public void onColorApplied(int targetColor) {
                        ArrayList<ColorDelta> deltas = new ArrayList<>();
                        int sr = Color.red(sourceColor);
                        int sg = Color.green(sourceColor);
                        int sb = Color.blue(sourceColor);
                        for (int i = 0; i < allKeys.size(); i++) {
                            int key = allKeys.get(i).key;
                            int cur = Theme.getColor(key);
                            boolean match;
                            if (!similarShades) {
                                match = (cur == sourceColor);
                            } else {
                                int dr = Color.red(cur) - sr;
                                int dg = Color.green(cur) - sg;
                                int db = Color.blue(cur) - sb;
                                match = (dr * dr + dg * dg + db * db) <= (42 * 42);
                            }
                            if (match) {
                                boolean oldDef = !Theme.hasThemeKey(key);
                                int finalColor = KronoThemeColorPickerSheet.isOpaqueRequiredKey(key)
                                        ? (0xFF000000 | targetColor)
                                        : targetColor;
                                Theme.setColor(key, finalColor, false);
                                deltas.add(new ColorDelta(key, cur, oldDef, finalColor, false));
                            }
                        }
                        if (!deltas.isEmpty()) {
                            pushUndoAction(new UndoAction(LocaleController.getString(R.string.KronoThemeEditorReplaceTool), deltas));
                            hasUnsavedChanges = true;
                            saveActiveTheme(false);
                            scheduleLiveRefresh();
                            updateStateUi();
                            applyFilters();
                        }
                        BulletinFactory.of(KronoThemeEditorActivity.this).createSimpleBulletin(
                                R.drawable.msg_edit,
                                LocaleController.formatString(R.string.KronoThemeEditorReplaceApplied, deltas.size())
                        ).show();
                    }

                    @Override
                    public void onResetToDefault() { }

                    @Override
                    public void onCancelled() { }
                }
        );
        showDialog(picker);
    }

    private void confirmResetAllModified() {
        Activity activity = getParentActivity();
        Theme.ThemeInfo active = getActiveThemeSafe();
        if (activity == null || active == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.KronoThemeEditorResetAll));
        builder.setMessage(LocaleController.formatString(R.string.KronoThemeEditorResetAllConfirm, active.name));
        builder.setPositiveButton(LocaleController.getString(R.string.KronoThemeEditorResetKey), (d, w) -> {
            ArrayList<ColorDelta> deltas = new ArrayList<>();
            for (int i = 0; i < allKeys.size(); i++) {
                int key = allKeys.get(i).key;
                if (Theme.hasThemeKey(key)) {
                    int oldColor = Theme.getColor(key);
                    int def = Theme.getDefaultColor(key);
                    Theme.setColor(key, def, true);
                    deltas.add(new ColorDelta(key, oldColor, false, def, true));
                }
            }
            if (!deltas.isEmpty()) {
                pushUndoAction(new UndoAction(LocaleController.getString(R.string.KronoThemeEditorResetAll), deltas));
                hasUnsavedChanges = true;
                saveActiveTheme(false);
                scheduleLiveRefresh();
                updateStateUi();
                applyFilters();
            }
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_reset,
                    LocaleController.formatString(R.string.KronoThemeEditorResetAllDone, deltas.size())
            ).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void saveActiveTheme(boolean showToast) {
        Theme.ThemeInfo active = getActiveThemeSafe();
        if (!isEditableCustomTheme(active)) {
            return;
        }
        Theme.saveCurrentTheme(active, showToast, false, false);
        if (showToast) {
            hasUnsavedChanges = false;
            if (parentLayout != null) {
                parentLayout.rebuildAllFragmentViews(false, false);
            }
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.ic_ab_done,
                    LocaleController.getString(R.string.KronoThemeEditorThemeSaved)
            ).show();
        }
    }

    private void launchFloatingMode() {
        ensureEditableThen(() -> {
            Activity activity = getParentActivity();
            Theme.ThemeInfo active = getActiveThemeSafe();
            if (activity == null || active == null) {
                return;
            }
            saveActiveTheme(false);
            new KronoThemeEditorView().show(activity, active);
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_theme,
                    LocaleController.getString(R.string.KronoThemeEditorFloatingHint)
            ).show();
        });
    }

    private void showSwitchCustomThemeDialog() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<Theme.ThemeInfo> custom = new ArrayList<>();
        if (Theme.themes != null) {
            for (int i = 0; i < Theme.themes.size(); i++) {
                Theme.ThemeInfo t = Theme.themes.get(i);
                if (isEditableCustomTheme(t)) {
                    custom.add(t);
                }
            }
        }
        if (custom.isEmpty()) {
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    LocaleController.getString(R.string.KronoThemeEditorNoCustomThemes)
            ).show();
            return;
        }
        Theme.ThemeInfo active = getActiveThemeSafe();
        CharSequence[] labels = new CharSequence[custom.size()];
        for (int i = 0; i < custom.size(); i++) {
            Theme.ThemeInfo t = custom.get(i);
            labels[i] = (t == active ? "✓ " : "") + t.name;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.KronoThemeEditorSwitchTheme));
        builder.setItems(labels, (d, which) -> {
            Theme.ThemeInfo chosen = custom.get(which);
            Theme.applyTheme(chosen);
            undoStack.clear();
            redoStack.clear();
            hasUnsavedChanges = false;
            if (parentLayout != null) {
                parentLayout.rebuildAllFragmentViews(false, false);
            }
            updateStateUi();
            applyFilters();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void promptRenameCurrentTheme() {
        Activity activity = getParentActivity();
        Theme.ThemeInfo oldTheme = getActiveThemeSafe();
        if (activity == null || !isEditableCustomTheme(oldTheme)) {
            promptCreateEditableCopy(false);
            return;
        }
        FrameLayout box = new FrameLayout(activity);
        box.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        EditTextBoldCursor et = new EditTextBoldCursor(activity);
        et.setText(oldTheme.name);
        et.setSelection(oldTheme.name.length());
        et.setHint(LocaleController.getString(R.string.KronoThemeEditorNewThemeHint));
        et.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        et.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        et.setSingleLine(true);
        box.addView(et, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.KronoThemeEditorRenameTheme));
        builder.setView(box);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (d, w) -> {
            String newName = et.getText() != null ? et.getText().toString().trim() : "";
            if (TextUtils.isEmpty(newName) || newName.equals(oldTheme.name)) {
                return;
            }
            AndroidUtilities.hideKeyboard(et);
            Theme.ThemeInfo renamed = Theme.createNewTheme(newName);
            Theme.deleteTheme(oldTheme);
            Theme.applyTheme(renamed);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeListUpdated);
            hasUnsavedChanges = false;
            updateStateUi();
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_theme,
                    LocaleController.formatString(R.string.KronoThemeEditorThemeRenamed, renamed.name)
            ).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void confirmDeleteCurrentTheme() {
        Activity activity = getParentActivity();
        Theme.ThemeInfo active = getActiveThemeSafe();
        if (activity == null || !isEditableCustomTheme(active)) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString(R.string.DeleteThemeTitle));
        builder.setMessage(LocaleController.formatString(R.string.KronoThemeEditorDeleteConfirm, active.name));
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
            MessagesController.getInstance(active.account).saveTheme(active, null, active == Theme.getCurrentNightTheme(), true);
            Theme.deleteTheme(active);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeListUpdated);
            undoStack.clear();
            redoStack.clear();
            hasUnsavedChanges = false;
            if (parentLayout != null) {
                parentLayout.rebuildAllFragmentViews(false, false);
            }
            updateStateUi();
            applyFilters();
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_delete,
                    LocaleController.getString(R.string.KronoThemeEditorThemeDeleted)
            ).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        TextView btn = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void launchImportAtthemePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_IMPORT_ATTHEME);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    LocaleController.getString(R.string.KronoThemeEditorThemeImportError)
            ).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (wallpaperUpdater != null) {
            wallpaperUpdater.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_IMPORT_ATTHEME && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            importAtthemeFromUri(data.getData());
        }
    }

    private void importAtthemeFromUri(Uri uri) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            File tempFile;
            try {
                String rawName = "Imported_" + (System.currentTimeMillis() % 100000);
                String lastSeg = uri.getLastPathSegment();
                if (!TextUtils.isEmpty(lastSeg)) {
                    int slash = lastSeg.lastIndexOf('/');
                    String base = slash >= 0 ? lastSeg.substring(slash + 1) : lastSeg;
                    if (base.endsWith(".attheme")) {
                        base = base.substring(0, base.length() - 8);
                    }
                    base = FileLoader.fixFileName(base).trim();
                    if (!TextUtils.isEmpty(base)) {
                        rawName = base;
                    }
                }
                tempFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), "import_" + System.currentTimeMillis() + ".attheme");
                try (InputStream is = activity.getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(tempFile)) {
                    if (is == null) {
                        throw new IllegalStateException("Null InputStream");
                    }
                    byte[] buf = new byte[8192];
                    int total = 0;
                    int read;
                    while ((read = is.read(buf)) > 0) {
                        total += read;
                        if (total > 12 * 1024 * 1024) {
                            throw new IllegalStateException("Theme file too large");
                        }
                        fos.write(buf, 0, read);
                    }
                }
                final File readyFile = tempFile;
                final String finalName = rawName;
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        Theme.ThemeInfo imported = Theme.applyThemeFile(readyFile, finalName, null, false);
                        if (imported == null) {
                            throw new IllegalStateException("applyThemeFile returned null");
                        }
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeListUpdated);
                        undoStack.clear();
                        redoStack.clear();
                        hasUnsavedChanges = false;
                        if (parentLayout != null) {
                            parentLayout.rebuildAllFragmentViews(false, false);
                        }
                        updateStateUi();
                        applyFilters();
                        BulletinFactory.of(this).createSimpleBulletin(
                                R.drawable.msg_theme,
                                LocaleController.formatString(R.string.KronoThemeEditorThemeImported, imported.name)
                        ).show();
                    } catch (Exception e) {
                        FileLog.e(e);
                        BulletinFactory.of(this).createSimpleBulletin(
                                R.drawable.msg_info,
                                LocaleController.getString(R.string.KronoThemeEditorThemeImportError)
                        ).show();
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.msg_info,
                        LocaleController.getString(R.string.KronoThemeEditorThemeImportError)
                ).show());
            }
        });
    }

    private void exportActiveTheme() {
        Activity activity = getParentActivity();
        Theme.ThemeInfo active = getActiveThemeSafe();
        if (activity == null || active == null) {
            return;
        }
        try {
            File sourceFile;
            if (isEditableCustomTheme(active)) {
                Theme.saveCurrentTheme(active, true, false, false);
                sourceFile = new File(active.pathToFile);
            } else if (active.assetName != null) {
                sourceFile = Theme.getAssetFile(active.assetName);
            } else {
                sourceFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), "export_" + System.currentTimeMillis() + ".attheme");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < allKeys.size(); i++) {
                    KeyItem item = allKeys.get(i);
                    sb.append(item.rawName).append('=').append(Theme.getColor(item.key)).append('\n');
                }
                try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
                    fos.write(AndroidUtilities.getStringBytes(sb.toString()));
                }
            }
            String fileName = TextUtils.isEmpty(active.name) ? "KronoTheme.attheme" : active.name;
            if (!fileName.endsWith(".attheme")) {
                fileName += ".attheme";
            }
            File shareFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), FileLoader.fixFileName(fileName));
            if (!sourceFile.equals(shareFile) && !AndroidUtilities.copyFile(sourceFile, shareFile)) {
                throw new IllegalStateException("Copy failed");
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            if (Build.VERSION.SDK_INT >= 24) {
                Uri contentUri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", shareFile);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shareFile));
            }
            startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createSimpleBulletin(
                    R.drawable.msg_info,
                    LocaleController.getString(R.string.KronoThemeEditorThemeExportError)
            ).show();
        }
    }

    private class LivePreviewCard extends LinearLayout {
        private final LinearLayout miniBar;
        private final TextView miniTitle;
        private final TextView miniSub;
        private final LinearLayout chatArea;
        private final TextView inBubble;
        private final TextView outBubble;
        private final TextView accentPill;

        LivePreviewCard(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setClipToOutline(true);

            miniBar = new LinearLayout(context);
            miniBar.setOrientation(VERTICAL);
            miniBar.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

            miniTitle = new TextView(context);
            miniTitle.setText(LocaleController.getString(R.string.KronoThemeEditorPreviewTitle));
            miniTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            miniTitle.setTypeface(AndroidUtilities.bold());
            miniBar.addView(miniTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            miniSub = new TextView(context);
            miniSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            miniBar.addView(miniSub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            addView(miniBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            chatArea = new LinearLayout(context);
            chatArea.setOrientation(VERTICAL);
            chatArea.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

            inBubble = new TextView(context);
            inBubble.setText(LocaleController.getString(R.string.KronoThemeEditorPreviewIncoming));
            inBubble.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            inBubble.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
            chatArea.addView(inBubble, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START, 0, 0, 24, 6));

            LinearLayout bottomRow = new LinearLayout(context);
            bottomRow.setOrientation(HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);

            accentPill = new TextView(context);
            accentPill.setText("HEX");
            accentPill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            accentPill.setTypeface(AndroidUtilities.bold());
            accentPill.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            bottomRow.addView(accentPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

            View spacer = new View(context);
            bottomRow.addView(spacer, LayoutHelper.createLinear(0, 1, 1f));

            outBubble = new TextView(context);
            outBubble.setText(LocaleController.getString(R.string.KronoThemeEditorPreviewOutgoing));
            outBubble.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            outBubble.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
            bottomRow.addView(outBubble, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            chatArea.addView(bottomRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            addView(chatArea, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            refreshColors();
        }

        void refreshColors() {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(AndroidUtilities.dp(14));
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
            setBackground(cardBg);

            miniBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            miniTitle.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
            miniSub.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle));
            miniSub.setText(LocaleController.formatString(R.string.KronoThemeEditorPreviewSubtitle, allKeys.size()));

            int chatBg = Theme.getColor(Theme.key_chat_wallpaper);
            if (chatBg == 0) {
                chatBg = Theme.getColor(Theme.key_windowBackgroundGray);
            }
            chatArea.setBackgroundColor(chatBg | 0xFF000000);

            GradientDrawable inBg = new GradientDrawable();
            inBg.setCornerRadius(AndroidUtilities.dp(12));
            inBg.setColor(Theme.getColor(Theme.key_chat_inBubble));
            inBubble.setBackground(inBg);
            inBubble.setTextColor(Theme.getColor(Theme.key_chat_messageTextIn));

            GradientDrawable outBg = new GradientDrawable();
            outBg.setCornerRadius(AndroidUtilities.dp(12));
            outBg.setColor(Theme.getColor(Theme.key_chat_outBubble));
            outBubble.setBackground(outBg);
            outBubble.setTextColor(Theme.getColor(Theme.key_chat_messageTextOut));

            int accent = Theme.getColor(Theme.key_chats_actionBackground);
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(AndroidUtilities.dp(10));
            pillBg.setColor(accent);
            accentPill.setBackground(pillBg);
            accentPill.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
            accentPill.setText(KronoThemeColorPickerSheet.formatHex(accent, false));
        }
    }

    private class KeysAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        KeysAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return filteredKeys.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(mContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(11), AndroidUtilities.dp(16), AndroidUtilities.dp(11));
            row.setBackground(Theme.getSelectorDrawable(false));
            row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            FrameLayout swatchBox = new FrameLayout(mContext);
            View swatch = new View(mContext);
            swatch.setTag("swatch");
            swatchBox.addView(swatch, LayoutHelper.createFrame(30, 30, Gravity.CENTER));

            View modDot = new View(mContext);
            modDot.setTag("modDot");
            swatchBox.addView(modDot, LayoutHelper.createFrame(8, 8, Gravity.END | Gravity.TOP));
            row.addView(swatchBox, LayoutHelper.createLinear(34, 34, 0, 0, 14, 0));

            LinearLayout col = new LinearLayout(mContext);
            col.setOrientation(LinearLayout.VERTICAL);

            TextView title = new TextView(mContext);
            title.setTag("title");
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            TextView sub = new TextView(mContext);
            sub.setTag("sub");
            sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
            return new RecyclerListView.Holder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            KeyItem item = filteredKeys.get(position);
            int color = Theme.getColor(item.key);
            boolean modified = Theme.hasThemeKey(item.key);

            View swatch = holder.itemView.findViewWithTag("swatch");
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(color);
            gd.setStroke(AndroidUtilities.dp(1.2f), Theme.isCurrentThemeDark() ? 0x55FFFFFF : 0x33000000);
            swatch.setBackground(gd);

            View modDot = holder.itemView.findViewWithTag("modDot");
            if (modified) {
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                dotBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_windowBackgroundWhite));
                modDot.setBackground(dotBg);
                modDot.setVisibility(View.VISIBLE);
            } else {
                modDot.setVisibility(View.GONE);
            }

            TextView title = holder.itemView.findViewWithTag("title");
            title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            title.setText(item.localizedTitle);

            TextView sub = holder.itemView.findViewWithTag("sub");
            if (showTechKeys) {
                sub.setVisibility(View.VISIBLE);
                sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                sub.setText(item.rawName + " · " + KronoThemeColorPickerSheet.formatHex(color, true));
            } else {
                sub.setVisibility(View.GONE);
            }
        }
    }
}
