package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.ActionBar.ThemeColors;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.ActionBar.ThemeKeyLocalizer;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Enhanced KronoGram floating live theme editor.
 * Copied and evolved from ThemeEditorView with Light/Dark theming, HEX/RGBA/Contrast picker,
 * Undo/Redo history, dual search (localized + raw key + HEX), and safe WindowManager lifecycle.
 */
public class KronoThemeEditorView {

    private static final int MAX_UNDO_STEPS = 50;

    private static final class UndoEntry {
        final int key;
        final int oldColor;
        final boolean oldWasDefault;
        final int newColor;
        final boolean newWasDefault;

        UndoEntry(int key, int oldColor, boolean oldWasDefault, int newColor, boolean newWasDefault) {
            this.key = key;
            this.oldColor = oldColor;
            this.oldWasDefault = oldWasDefault;
            this.newColor = newColor;
            this.newWasDefault = newWasDefault;
        }
    }

    private final ArrayDeque<UndoEntry> undoStack = new ArrayDeque<>();
    private final ArrayDeque<UndoEntry> redoStack = new ArrayDeque<>();

    private boolean themeColorsRefreshPosted;
    private final Runnable refreshEditedThemeColors = new Runnable() {
        @Override
        public void run() {
            themeColorsRefreshPosted = false;
            try {
                Theme.applyCommonTheme();
                Theme.applyDialogsTheme();
                Theme.applyProfileTheme();
                Theme.applyChatTheme(false, false);
                ThemeBind.updateAll();
                if (LaunchActivity.instance != null) {
                    LaunchActivity.instance.notifyThemeColorsChanged();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    };

    private void onThemeColorEdited() {
        if (!themeColorsRefreshPosted) {
            themeColorsRefreshPosted = true;
            AndroidUtilities.runOnUIThread(refreshEditedThemeColors);
        }
    }

    private FrameLayout windowView;
    private Activity parentActivity;
    private boolean windowAttached;
    private boolean hidden;

    private final int editorWidth = AndroidUtilities.dp(54);
    private final int editorHeight = AndroidUtilities.dp(54);

    private WindowManager.LayoutParams windowLayoutParams;
    private WindowManager windowManager;
    private DecelerateInterpolator decelerateInterpolator;
    private SharedPreferences preferences;
    private WallpaperUpdater wallpaperUpdater;
    private FloatingSheet editorSheet;
    private Theme.ThemeInfo themeInfo;

    @SuppressLint("StaticFieldLeak")
    private static volatile KronoThemeEditorView Instance = null;

    public static KronoThemeEditorView getInstance() {
        return Instance;
    }

    public static boolean isShowing() {
        return Instance != null;
    }

    private static ArrayList<ThemeDescription> collectEditorDescriptions(BaseFragment fragment) {
        final ArrayList<ThemeDescription> result = new ArrayList<>();
        final HashSet<Integer> seen = new HashSet<>();
        ArrayList<ThemeDescription> legacy = null;
        try {
            legacy = fragment != null ? fragment.getThemeDescriptions() : null;
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (legacy != null) {
            for (int i = 0; i < legacy.size(); ++i) {
                final ThemeDescription description = legacy.get(i);
                if (description != null) {
                    result.add(description);
                    seen.add(description.getCurrentKey());
                }
            }
        }
        final HashSet<Integer> keySet = ThemeBind.collectColorKeys();
        if (fragment != null) {
            try {
                fragment.addThemeColorKeys(keySet);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        final ArrayList<Integer> keys = new ArrayList<>(keySet);
        Collections.sort(keys, (a, b) -> String.valueOf(ThemeColors.getStringName(a)).compareTo(String.valueOf(ThemeColors.getStringName(b))));
        for (int i = 0; i < keys.size(); ++i) {
            final int key = keys.get(i);
            if (key >= 0 && key < Theme.colorsCount && seen.add(key)) {
                result.add(new ThemeDescription(null, 0, null, null, null, null, key));
            }
        }
        return result;
    }

    public void show(Activity activity, final Theme.ThemeInfo theme) {
        if (activity == null || activity.isFinishing() || theme == null) {
            return;
        }
        ThemeEditorView nativeEditor = ThemeEditorView.getInstance();
        if (nativeEditor != null) {
            nativeEditor.destroy();
        }
        if (Instance != null) {
            Instance.destroy();
        }
        hidden = false;
        themeInfo = theme;
        parentActivity = activity;
        decelerateInterpolator = new DecelerateInterpolator();

        windowView = new FrameLayout(activity) {
            private float startX;
            private float startY;
            private boolean dragging;

            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                float x = event.getRawX();
                float y = event.getRawY();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = x;
                    startY = y;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && !dragging) {
                    if (Math.abs(startX - x) >= AndroidUtilities.getPixelsInCM(0.3f, true)
                            || Math.abs(startY - y) >= AndroidUtilities.getPixelsInCM(0.3f, false)) {
                        dragging = true;
                        startX = x;
                        startY = y;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!dragging) {
                        openSheetForCurrentScreen();
                    }
                }
                if (dragging) {
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        float dx = x - startX;
                        float dy = y - startY;
                        windowLayoutParams.x += dx;
                        windowLayoutParams.y += dy;
                        int maxDiff = editorWidth / 2;
                        if (windowLayoutParams.x < -maxDiff) {
                            windowLayoutParams.x = -maxDiff;
                        } else if (windowLayoutParams.x > AndroidUtilities.displaySize.x - windowLayoutParams.width + maxDiff) {
                            windowLayoutParams.x = AndroidUtilities.displaySize.x - windowLayoutParams.width + maxDiff;
                        }
                        float alpha = 1.0f;
                        if (windowLayoutParams.x < 0) {
                            alpha = 1.0f + windowLayoutParams.x / (float) maxDiff * 0.5f;
                        } else if (windowLayoutParams.x > AndroidUtilities.displaySize.x - windowLayoutParams.width) {
                            alpha = 1.0f - (windowLayoutParams.x - AndroidUtilities.displaySize.x + windowLayoutParams.width) / (float) maxDiff * 0.5f;
                        }
                        if (windowView.getAlpha() != alpha) {
                            windowView.setAlpha(alpha);
                        }
                        if (windowLayoutParams.y < 0) {
                            windowLayoutParams.y = 0;
                        } else if (windowLayoutParams.y > AndroidUtilities.displaySize.y - windowLayoutParams.height) {
                            windowLayoutParams.y = AndroidUtilities.displaySize.y - windowLayoutParams.height;
                        }
                        safeUpdateViewLayout();
                        startX = x;
                        startY = y;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        dragging = false;
                        animateToBoundsMaybe();
                    }
                }
                return true;
            }
        };

        windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Context.MODE_PRIVATE);

        int sidex = preferences.getInt("sidex", 1);
        int sidey = preferences.getInt("sidey", 0);
        float px = preferences.getFloat("px", 0);
        float py = preferences.getFloat("py", 0);

        try {
            windowLayoutParams = new WindowManager.LayoutParams();
            windowLayoutParams.width = editorWidth;
            windowLayoutParams.height = editorHeight;
            windowLayoutParams.x = getSideCoord(true, sidex, px, editorWidth);
            windowLayoutParams.y = getSideCoord(false, sidey, py, editorHeight);
            windowLayoutParams.format = PixelFormat.TRANSLUCENT;
            windowLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            windowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
            windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            AndroidUtilities.setPreferredMaxRefreshRate(windowManager, windowView, windowLayoutParams);
            windowManager.addView(windowView, windowLayoutParams);
            windowAttached = true;
        } catch (Exception e) {
            FileLog.e(e);
            parentActivity = null;
            windowView = null;
            return;
        }

        wallpaperUpdater = new WallpaperUpdater(activity, null, new WallpaperUpdater.WallpaperUpdaterDelegate() {
            @Override
            public void didSelectWallpaper(File file, Bitmap bitmap, boolean gallery) {
                if (themeInfo != null && !TextUtils.isEmpty(themeInfo.pathToFile)) {
                    Theme.setThemeWallpaper(themeInfo, bitmap, file);
                }
            }

            @Override
            public void needOpenColorPicker() {
                if (parentActivity != null && !parentActivity.isFinishing()) {
                    openColorPickerForKey(Theme.key_chat_wallpaper, null);
                }
            }
        });

        Instance = this;
        showWithAnimation();
    }

    private void openSheetForCurrentScreen() {
        if (editorSheet != null || !(parentActivity instanceof LaunchActivity) || parentActivity.isFinishing()) {
            return;
        }
        LaunchActivity launchActivity = (LaunchActivity) parentActivity;
        INavigationLayout actionBarLayout = null;
        if (AndroidUtilities.isTablet()) {
            actionBarLayout = launchActivity.getLayersActionBarLayout();
            if (actionBarLayout != null && actionBarLayout.getFragmentStack().isEmpty()) {
                actionBarLayout = null;
            }
            if (actionBarLayout == null) {
                actionBarLayout = launchActivity.getRightActionBarLayout();
                if (actionBarLayout != null && actionBarLayout.getFragmentStack().isEmpty()) {
                    actionBarLayout = null;
                }
            }
        }
        if (actionBarLayout == null) {
            actionBarLayout = launchActivity.getActionBarLayout();
        }
        if (actionBarLayout == null || actionBarLayout.getFragmentStack().isEmpty()) {
            return;
        }
        BaseFragment fragment = actionBarLayout.getFragmentStack().get(actionBarLayout.getFragmentStack().size() - 1);
        if (fragment == null) {
            return;
        }
        ArrayList<ThemeDescription> items = collectEditorDescriptions(fragment);
        if (items.isEmpty()) {
            return;
        }
        editorSheet = new FloatingSheet(parentActivity, items);
        editorSheet.setOnDismissListener(dialog -> {
            editorSheet = null;
            if (Instance == this && parentActivity != null && !parentActivity.isFinishing()) {
                showBubble();
            }
        });
        editorSheet.show();
        hideBubble();
    }

    private void openColorPickerForKey(int key, ArrayList<ThemeDescription> descriptions) {
        if (parentActivity == null || parentActivity.isFinishing()) {
            return;
        }
        if (key == Theme.key_chat_wallpaper && descriptions != null && wallpaperUpdater != null) {
            wallpaperUpdater.showAlert(true);
            return;
        }
        final String rawName = ThemeColors.getStringName(key);
        final String title = ThemeKeyLocalizer.getLocalizedTitle(rawName);
        final int oldColor = Theme.getColor(key);
        final boolean oldWasDefault = !Theme.hasThemeKey(key);
        final boolean opaqueOnly = KronoThemeColorPickerSheet.isOpaqueRequiredKey(key);

        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                descriptions.get(i).startEditing();
            }
        }

        KronoThemeColorPickerSheet picker = new KronoThemeColorPickerSheet(
                parentActivity,
                title,
                rawName,
                oldColor,
                opaqueOnly,
                true,
                null,
                new KronoThemeColorPickerSheet.Callback() {
                    @Override
                    public void onColorChanged(int color) {
                        applyKeyColorLive(key, descriptions, color, false);
                    }

                    @Override
                    public void onColorApplied(int color) {
                        applyKeyColorLive(key, descriptions, color, false);
                        if (color != oldColor || oldWasDefault) {
                            pushUndo(new UndoEntry(key, oldColor, oldWasDefault, color, false));
                        }
                        persistThemeIncremental();
                        if (editorSheet != null) {
                            editorSheet.refreshListAndButtons();
                        }
                    }

                    @Override
                    public void onResetToDefault() {
                        int def = Theme.getDefaultColor(key);
                        applyKeyColorLive(key, descriptions, def, true);
                        if (!oldWasDefault) {
                            pushUndo(new UndoEntry(key, oldColor, false, def, true));
                        }
                        persistThemeIncremental();
                        if (editorSheet != null) {
                            editorSheet.refreshListAndButtons();
                        }
                    }

                    @Override
                    public void onCancelled() {
                        applyKeyColorLive(key, descriptions, oldColor, oldWasDefault);
                    }
                }
        );
        picker.show();
    }

    private void applyKeyColorLive(int key, ArrayList<ThemeDescription> descriptions, int color, boolean useDefault) {
        if (KronoThemeColorPickerSheet.isOpaqueRequiredKey(key)) {
            color |= 0xFF000000;
        }
        Theme.setColor(key, color, useDefault);
        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                if (useDefault) {
                    descriptions.get(i).setDefaultColor();
                } else {
                    descriptions.get(i).setColor(color, false);
                }
            }
        }
        onThemeColorEdited();
    }

    private void pushUndo(UndoEntry entry) {
        undoStack.push(entry);
        while (undoStack.size() > MAX_UNDO_STEPS) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private void performUndo() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoEntry e = undoStack.pop();
        redoStack.push(e);
        Theme.setColor(e.key, e.oldColor, e.oldWasDefault);
        onThemeColorEdited();
        persistThemeIncremental();
        if (parentActivity != null) {
            String name = ThemeKeyLocalizer.getLocalizedTitle(ThemeColors.getStringName(e.key));
            Toast.makeText(parentActivity, LocaleController.formatString(R.string.KronoThemeEditorUndoApplied, name), Toast.LENGTH_SHORT).show();
        }
        if (editorSheet != null) {
            editorSheet.refreshListAndButtons();
        }
    }

    private void performRedo() {
        if (redoStack.isEmpty()) {
            return;
        }
        UndoEntry e = redoStack.pop();
        undoStack.push(e);
        Theme.setColor(e.key, e.newColor, e.newWasDefault);
        onThemeColorEdited();
        persistThemeIncremental();
        if (parentActivity != null) {
            String name = ThemeKeyLocalizer.getLocalizedTitle(ThemeColors.getStringName(e.key));
            Toast.makeText(parentActivity, LocaleController.formatString(R.string.KronoThemeEditorRedoApplied, name), Toast.LENGTH_SHORT).show();
        }
        if (editorSheet != null) {
            editorSheet.refreshListAndButtons();
        }
    }

    private void persistThemeIncremental() {
        if (themeInfo != null && !TextUtils.isEmpty(themeInfo.pathToFile)) {
            Theme.saveCurrentTheme(themeInfo, false, false, false);
        }
    }

    private void saveAndClose() {
        if (themeInfo != null && !TextUtils.isEmpty(themeInfo.pathToFile)) {
            Theme.saveCurrentTheme(themeInfo, true, false, false);
        }
        if (parentActivity instanceof LaunchActivity) {
            ((LaunchActivity) parentActivity).rebuildAllFragments(false);
        }
        destroy();
    }

    public void destroy() {
        if (wallpaperUpdater != null) {
            try {
                wallpaperUpdater.cleanup();
            } catch (Exception e) {
                FileLog.e(e);
            }
            wallpaperUpdater = null;
        }
        if (editorSheet != null) {
            try {
                editorSheet.setOnDismissListener((android.content.DialogInterface.OnDismissListener) null);
                editorSheet.dismiss();
            } catch (Exception e) {
                FileLog.e(e);
            }
            editorSheet = null;
        }
        if (windowView != null && windowManager != null && windowAttached) {
            try {
                windowManager.removeViewImmediate(windowView);
            } catch (Exception e) {
                FileLog.e(e, false);
            }
        }
        windowAttached = false;
        windowView = null;
        parentActivity = null;
        if (Instance == this) {
            Instance = null;
        }
    }

    public void onConfigurationChanged() {
        if (preferences == null || windowLayoutParams == null || windowView == null) {
            return;
        }
        int sidex = preferences.getInt("sidex", 1);
        int sidey = preferences.getInt("sidey", 0);
        float px = preferences.getFloat("px", 0);
        float py = preferences.getFloat("py", 0);
        windowLayoutParams.x = getSideCoord(true, sidex, px, editorWidth);
        windowLayoutParams.y = getSideCoord(false, sidey, py, editorHeight);
        safeUpdateViewLayout();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (wallpaperUpdater != null) {
            wallpaperUpdater.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void safeUpdateViewLayout() {
        if (windowManager != null && windowView != null && windowAttached && windowView.getParent() != null) {
            try {
                windowManager.updateViewLayout(windowView, windowLayoutParams);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private void showWithAnimation() {
        if (windowView == null) {
            return;
        }
        windowView.setBackgroundResource(R.drawable.theme_picker);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(windowView, View.ALPHA, 0.0f, 1.0f),
                ObjectAnimator.ofFloat(windowView, View.SCALE_X, 0.0f, 1.0f),
                ObjectAnimator.ofFloat(windowView, View.SCALE_Y, 0.0f, 1.0f));
        animatorSet.setInterpolator(decelerateInterpolator);
        animatorSet.setDuration(150);
        animatorSet.start();
    }

    private void hideBubble() {
        if (parentActivity == null || windowView == null || !windowAttached) {
            return;
        }
        try {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(windowView, View.ALPHA, 1.0f, 0.0f),
                    ObjectAnimator.ofFloat(windowView, View.SCALE_X, 1.0f, 0.0f),
                    ObjectAnimator.ofFloat(windowView, View.SCALE_Y, 1.0f, 0.0f));
            animatorSet.setInterpolator(decelerateInterpolator);
            animatorSet.setDuration(150);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (windowView != null && windowAttached && windowManager != null) {
                        try {
                            windowView.setBackground(null);
                            windowManager.removeView(windowView);
                            windowAttached = false;
                        } catch (Exception ignore) { }
                    }
                }
            });
            animatorSet.start();
            hidden = true;
        } catch (Exception ignore) { }
    }

    private void showBubble() {
        if (parentActivity == null || parentActivity.isFinishing() || windowView == null || windowAttached) {
            return;
        }
        try {
            AndroidUtilities.setPreferredMaxRefreshRate(windowManager, windowView, windowLayoutParams);
            windowManager.addView(windowView, windowLayoutParams);
            windowAttached = true;
            hidden = false;
            showWithAnimation();
        } catch (Exception ignore) { }
    }

    private static int getSideCoord(boolean isX, int side, float p, int sideSize) {
        int total = isX
                ? AndroidUtilities.displaySize.x - sideSize
                : AndroidUtilities.displaySize.y - sideSize - ActionBar.getCurrentActionBarHeight();
        int result;
        if (side == 0) {
            result = AndroidUtilities.dp(10);
        } else if (side == 1) {
            result = total - AndroidUtilities.dp(10);
        } else {
            result = Math.round((total - AndroidUtilities.dp(20)) * p) + AndroidUtilities.dp(10);
        }
        if (!isX) {
            result += ActionBar.getCurrentActionBarHeight();
        }
        return result;
    }

    private void animateToBoundsMaybe() {
        int startX = getSideCoord(true, 0, 0, editorWidth);
        int endX = getSideCoord(true, 1, 0, editorWidth);
        int startY = getSideCoord(false, 0, 0, editorHeight);
        int endY = getSideCoord(false, 1, 0, editorHeight);
        ArrayList<Animator> animators = new ArrayList<>();
        SharedPreferences.Editor editor = preferences.edit();
        int maxDiff = AndroidUtilities.dp(20);
        boolean slideOut = false;

        if (Math.abs(startX - windowLayoutParams.x) <= maxDiff || (windowLayoutParams.x < 0 && windowLayoutParams.x > -editorWidth / 4)) {
            editor.putInt("sidex", 0);
            if (windowView.getAlpha() != 1.0f) {
                animators.add(ObjectAnimator.ofFloat(windowView, View.ALPHA, 1.0f));
            }
            animators.add(ObjectAnimator.ofInt(this, "x", startX));
        } else if (Math.abs(endX - windowLayoutParams.x) <= maxDiff || (windowLayoutParams.x > AndroidUtilities.displaySize.x - editorWidth && windowLayoutParams.x < AndroidUtilities.displaySize.x - editorWidth / 4 * 3)) {
            editor.putInt("sidex", 1);
            if (windowView.getAlpha() != 1.0f) {
                animators.add(ObjectAnimator.ofFloat(windowView, View.ALPHA, 1.0f));
            }
            animators.add(ObjectAnimator.ofInt(this, "x", endX));
        } else if (windowView.getAlpha() != 1.0f) {
            animators.add(ObjectAnimator.ofInt(this, "x", windowLayoutParams.x < 0 ? -editorWidth : AndroidUtilities.displaySize.x));
            slideOut = true;
        } else {
            editor.putFloat("px", (windowLayoutParams.x - startX) / (float) Math.max(1, endX - startX));
            editor.putInt("sidex", 2);
        }

        if (!slideOut) {
            if (Math.abs(startY - windowLayoutParams.y) <= maxDiff || windowLayoutParams.y <= ActionBar.getCurrentActionBarHeight()) {
                editor.putInt("sidey", 0);
                animators.add(ObjectAnimator.ofInt(this, "y", startY));
            } else if (Math.abs(endY - windowLayoutParams.y) <= maxDiff) {
                editor.putInt("sidey", 1);
                animators.add(ObjectAnimator.ofInt(this, "y", endY));
            } else {
                editor.putFloat("py", (windowLayoutParams.y - startY) / (float) Math.max(1, endY - startY));
                editor.putInt("sidey", 2);
            }
            editor.apply();
        }

        if (!animators.isEmpty()) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setInterpolator(decelerateInterpolator);
            animatorSet.setDuration(150);
            if (slideOut) {
                animators.add(ObjectAnimator.ofFloat(windowView, View.ALPHA, 0.0f));
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        saveAndClose();
                    }
                });
            }
            animatorSet.playTogether(animators);
            animatorSet.start();
        }
    }

    @Keep
    public int getX() {
        return windowLayoutParams != null ? windowLayoutParams.x : 0;
    }

    @Keep
    public int getY() {
        return windowLayoutParams != null ? windowLayoutParams.y : 0;
    }

    @Keep
    public void setX(int value) {
        if (windowLayoutParams != null) {
            windowLayoutParams.x = value;
            safeUpdateViewLayout();
        }
    }

    @Keep
    public void setY(int value) {
        if (windowLayoutParams != null) {
            windowLayoutParams.y = value;
            safeUpdateViewLayout();
        }
    }

    private class FloatingSheet extends BottomSheet {

        private final ArrayList<ArrayList<ThemeDescription>> allGroups = new ArrayList<>();
        private final ArrayList<ArrayList<ThemeDescription>> filteredGroups = new ArrayList<>();
        private final RecyclerListView listView;
        private final KeyListAdapter adapter;
        private final TextView undoButton;
        private final TextView redoButton;
        private final EditTextBoldCursor searchEditText;

        FloatingSheet(Context context, ArrayList<ThemeDescription> rawItems) {
            super(context, true);

            HashMap<Integer, ArrayList<ThemeDescription>> byKey = new HashMap<>();
            for (int i = 0; i < rawItems.size(); i++) {
                ThemeDescription d = rawItems.get(i);
                int k = d.getCurrentKey();
                if (k < 0 || k >= Theme.colorsCount || k == Theme.key_wallpaperFileOffset) {
                    continue;
                }
                ArrayList<ThemeDescription> list = byKey.get(k);
                if (list == null) {
                    list = new ArrayList<>();
                    byKey.put(k, list);
                    allGroups.add(list);
                }
                list.add(d);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !byKey.containsKey(Theme.key_windowBackgroundGray)) {
                ArrayList<ThemeDescription> list = new ArrayList<>();
                list.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_windowBackgroundGray));
                allGroups.add(list);
            }
            filteredGroups.addAll(allGroups);

            int bgColor = Theme.getColor(Theme.key_dialogBackground);
            int textColor = Theme.getColor(Theme.key_dialogTextBlack);
            int subTextColor = Theme.getColor(Theme.key_dialogTextGray2);
            int accentColor = Theme.getColor(Theme.key_dialogButton);
            int fieldBg = Theme.getColor(Theme.key_dialogSearchBackground);
            if (fieldBg == 0) {
                fieldBg = Theme.isCurrentThemeDark() ? 0xFF2C2C2E : 0xFFF2F4F5;
            }
            int dividerColor = Theme.getColor(Theme.key_divider);

            fixNavigationBar(bgColor);
            setBackgroundColor(bgColor);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);

            LinearLayout topBar = new LinearLayout(context);
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setGravity(Gravity.CENTER_VERTICAL);
            topBar.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(6));

            TextView titleTv = new TextView(context);
            titleTv.setText(themeInfo != null && !TextUtils.isEmpty(themeInfo.name)
                    ? themeInfo.name
                    : LocaleController.getString(R.string.KronoThemeEditorFloatingMode));
            titleTv.setTextColor(textColor);
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleTv.setTypeface(AndroidUtilities.bold());
            titleTv.setSingleLine(true);
            titleTv.setEllipsize(TextUtils.TruncateAt.END);
            topBar.addView(titleTv, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            undoButton = makeBarBtn(context, LocaleController.getString(R.string.KronoThemeEditorUndo), accentColor);
            undoButton.setOnClickListener(v -> performUndo());
            topBar.addView(undoButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 36));

            redoButton = makeBarBtn(context, LocaleController.getString(R.string.KronoThemeEditorRedo), accentColor);
            redoButton.setOnClickListener(v -> performRedo());
            topBar.addView(redoButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 36));

            root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            FrameLayout searchContainer = new FrameLayout(context);
            searchContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(2), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

            FrameLayout searchPill = new FrameLayout(context);
            searchPill.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), fieldBg));

            ImageView searchIcon = new ImageView(context);
            searchIcon.setScaleType(ImageView.ScaleType.CENTER);
            searchIcon.setImageResource(R.drawable.smiles_inputsearch);
            searchIcon.setColorFilter(new PorterDuffColorFilter(subTextColor, PorterDuff.Mode.MULTIPLY));
            searchPill.addView(searchIcon, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.CENTER_VERTICAL, 4, 0, 0, 0));

            searchEditText = new EditTextBoldCursor(context);
            searchEditText.setHint(LocaleController.getString(R.string.KronoThemeEditorSearchHint));
            searchEditText.setTextColor(textColor);
            searchEditText.setHintTextColor(subTextColor);
            searchEditText.setCursorColor(accentColor);
            searchEditText.setCursorSize(AndroidUtilities.dp(18));
            searchEditText.setCursorWidth(1.5f);
            searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            searchEditText.setBackground(null);
            searchEditText.setSingleLine(true);
            searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override
                public void afterTextChanged(Editable s) {
                    filterItems(s.toString());
                }
            });
            searchPill.addView(searchEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.CENTER_VERTICAL, 40, 0, 12, 0));
            searchContainer.addView(searchPill, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36));
            root.addView(searchContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new KeyListAdapter(context, textColor, subTextColor);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((view, position) -> {
                if (position < 0 || position >= filteredGroups.size()) {
                    return;
                }
                ArrayList<ThemeDescription> group = filteredGroups.get(position);
                if (!group.isEmpty()) {
                    openColorPickerForKey(group.get(0).getCurrentKey(), group);
                }
            });
            int maxListHeight = Math.max(AndroidUtilities.dp(260), (int) (AndroidUtilities.displaySize.y * 0.48f));
            root.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, maxListHeight));

            View divider = new View(context);
            divider.setBackgroundColor(dividerColor);
            root.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

            FrameLayout bottomBar = new FrameLayout(context);
            bottomBar.setBackgroundColor(bgColor);

            TextView closeBtn = makeBarBtn(context, LocaleController.getString(R.string.CloseEditor).toUpperCase(), accentColor);
            closeBtn.setOnClickListener(v -> dismiss());
            bottomBar.addView(closeBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

            TextView saveBtn = makeBarBtn(context, LocaleController.getString(R.string.SaveTheme).toUpperCase(), accentColor);
            saveBtn.setOnClickListener(v -> {
                setOnDismissListener((android.content.DialogInterface.OnDismissListener) null);
                dismiss();
                saveAndClose();
            });
            bottomBar.addView(saveBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

            root.addView(bottomBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

            setCustomView(root);
            updateUndoRedoState();
        }

        private TextView makeBarBtn(Context context, String label, int color) {
            TextView tv = new TextView(context);
            tv.setText(label);
            tv.setTextColor(color);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tv.setTypeface(AndroidUtilities.bold());
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
            tv.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 0));
            return tv;
        }

        private void filterItems(String query) {
            filteredGroups.clear();
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (TextUtils.isEmpty(q)) {
                filteredGroups.addAll(allGroups);
            } else {
                for (int i = 0; i < allGroups.size(); i++) {
                    ArrayList<ThemeDescription> group = allGroups.get(i);
                    int key = group.get(0).getCurrentKey();
                    String raw = ThemeColors.getStringName(key);
                    String localized = ThemeKeyLocalizer.getLocalizedTitle(raw);
                    String hex = KronoThemeColorPickerSheet.formatHex(Theme.getColor(key), true).toLowerCase(Locale.ROOT);
                    if ((localized != null && localized.toLowerCase(Locale.ROOT).contains(q))
                            || (raw != null && raw.toLowerCase(Locale.ROOT).contains(q))
                            || hex.contains(q)) {
                        filteredGroups.add(group);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        }

        void refreshListAndButtons() {
            updateUndoRedoState();
            adapter.notifyDataSetChanged();
        }

        private void updateUndoRedoState() {
            undoButton.setEnabled(!undoStack.isEmpty());
            undoButton.setAlpha(undoStack.isEmpty() ? 0.35f : 1.0f);
            redoButton.setEnabled(!redoStack.isEmpty());
            redoButton.setAlpha(redoStack.isEmpty() ? 0.35f : 1.0f);
        }

        private class KeyListAdapter extends RecyclerListView.SelectionAdapter {
            private final Context mContext;
            private final int textColor;
            private final int subTextColor;

            KeyListAdapter(Context context, int textColor, int subTextColor) {
                this.mContext = context;
                this.textColor = textColor;
                this.subTextColor = subTextColor;
            }

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return true;
            }

            @Override
            public int getItemCount() {
                return filteredGroups.size();
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LinearLayout row = new LinearLayout(mContext);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
                row.setBackground(Theme.getSelectorDrawable(false));
                row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                View swatch = new View(mContext);
                swatch.setTag("swatch");
                row.addView(swatch, LayoutHelper.createLinear(28, 28, 0, 0, 14, 0));

                LinearLayout col = new LinearLayout(mContext);
                col.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                title.setTag("title");
                title.setTextColor(textColor);
                title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                col.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                TextView sub = new TextView(mContext);
                sub.setTag("sub");
                sub.setTextColor(subTextColor);
                sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                sub.setSingleLine(true);
                sub.setEllipsize(TextUtils.TruncateAt.END);
                col.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

                row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
                return new RecyclerListView.Holder(row);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ArrayList<ThemeDescription> group = filteredGroups.get(position);
                int key = group.get(0).getCurrentKey();
                String raw = ThemeColors.getStringName(key);
                String localized = ThemeKeyLocalizer.getLocalizedTitle(raw);
                int color = key == Theme.key_chat_wallpaper ? 0 : Theme.getColor(key);

                View swatch = holder.itemView.findViewWithTag("swatch");
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(color);
                gd.setStroke(AndroidUtilities.dp(1.2f), Theme.isCurrentThemeDark() ? 0x55FFFFFF : 0x33000000);
                swatch.setBackground(gd);

                TextView title = holder.itemView.findViewWithTag("title");
                title.setText(localized);

                TextView sub = holder.itemView.findViewWithTag("sub");
                sub.setText(raw + " · " + KronoThemeColorPickerSheet.formatHex(color, true));
            }
        }
    }
}
