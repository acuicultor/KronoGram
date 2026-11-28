package org.telegram.ui.Components;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Shared Light/Dark-aware color picker BottomSheet for KronoThemeEditorActivity and KronoThemeEditorView.
 * Provides HSV color wheel, brightness & alpha sliders, synced HEX & RGBA fields,
 * Copy/Paste HEX, WCAG contrast ratio badge, Before/After preview, and Recent + Theme color strips.
 */
public class KronoThemeColorPickerSheet extends BottomSheet {

    public static final int CAT_ALL = 0;
    public static final int CAT_MODIFIED = 1;
    public static final int CAT_ACTION_BAR = 2;
    public static final int CAT_WINDOW = 3;
    public static final int CAT_CHATS = 4;
    public static final int CAT_DIALOGS = 5;
    public static final int CAT_CONTROLS = 6;
    public static final int CAT_PROFILE = 7;
    public static final int CAT_MEDIA = 8;
    public static final int CAT_CALLS = 9;
    public static final int CAT_OTHER = 10;

    private static final String PREFS_NAME = "krono_theme_editor";
    private static final String KEY_RECENT_COLORS = "recent_colors";
    private static final int MAX_RECENT_COLORS = 10;

    public interface Callback {
        void onColorChanged(int color);
        void onColorApplied(int color);
        void onResetToDefault();
        void onCancelled();
    }

    private final boolean opaqueOnly;
    private final Callback callback;
    private final int initialColor;
    private int currentColor;
    private final float[] hsv = new float[]{0f, 0f, 1f};
    private int alphaVal = 255;
    private boolean updatingInputs = false;
    private boolean actionCommitted = false;

    private final ColorWheelView wheelView;
    private final GradientSliderView valueSlider;
    private final GradientSliderView alphaSlider;
    private final View afterSwatch;
    private final TextView contrastBadge;
    private final EditTextBoldCursor hexInput;
    private final EditTextBoldCursor rInput;
    private final EditTextBoldCursor gInput;
    private final EditTextBoldCursor bInput;
    private final EditTextBoldCursor aInput;

    public KronoThemeColorPickerSheet(
            Context context,
            String title,
            String subtitle,
            int initialColor,
            boolean opaqueOnly,
            boolean showReset,
            ArrayList<Integer> paletteColors,
            Callback callback
    ) {
        super(context, true);
        this.opaqueOnly = opaqueOnly;
        this.callback = callback;
        this.initialColor = opaqueOnly ? (initialColor | 0xFF000000) : initialColor;
        this.currentColor = this.initialColor;
        Color.colorToHSV(this.currentColor, hsv);
        this.alphaVal = opaqueOnly ? 255 : Color.alpha(this.currentColor);

        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        int textColor = Theme.getColor(Theme.key_dialogTextBlack);
        int subTextColor = Theme.getColor(Theme.key_dialogTextGray2);
        int accentColor = Theme.getColor(Theme.key_dialogButton);
        int dividerColor = Theme.getColor(Theme.key_divider);
        int fieldBg = Theme.isCurrentThemeDark() ? 0xFF2A2C30 : 0xFFF2F4F7;

        fixNavigationBar(bgColor);
        setBackgroundColor(bgColor);

        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(12));

        TextView titleTv = new TextView(context);
        titleTv.setText(title != null ? title : "");
        titleTv.setTextColor(textColor);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        titleTv.setTypeface(AndroidUtilities.bold());
        titleTv.setSingleLine(true);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        content.addView(titleTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subTv = new TextView(context);
            subTv.setText(subtitle);
            subTv.setTextColor(subTextColor);
            subTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            subTv.setSingleLine(true);
            subTv.setEllipsize(TextUtils.TruncateAt.END);
            content.addView(subTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 10));
        }

        // Before / After + WCAG Contrast row
        LinearLayout previewRow = new LinearLayout(context);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);

        View beforeSwatch = new View(context);
        setRoundedSwatch(beforeSwatch, this.initialColor, 8);
        LinearLayout beforeCol = makeSwatchColumn(context, LocaleController.getString(R.string.KronoThemeEditorBefore), beforeSwatch, subTextColor);
        beforeCol.setOnClickListener(v -> setColorInternal(this.initialColor, true, true));
        previewRow.addView(beforeCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));

        afterSwatch = new View(context);
        setRoundedSwatch(afterSwatch, this.currentColor, 8);
        LinearLayout afterCol = makeSwatchColumn(context, LocaleController.getString(R.string.KronoThemeEditorAfter), afterSwatch, subTextColor);
        previewRow.addView(afterCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 6, 0, 10, 0));

        contrastBadge = new TextView(context);
        contrastBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        contrastBadge.setTypeface(AndroidUtilities.bold());
        contrastBadge.setGravity(Gravity.CENTER);
        contrastBadge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        previewRow.addView(contrastBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        content.addView(previewRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // HSV Color Wheel
        wheelView = new ColorWheelView(context);
        wheelView.setOnHueSatChanged((h, s) -> {
            hsv[0] = h;
            hsv[1] = s;
            applyFromHsvAlpha(true, true);
        });
        content.addView(wheelView, LayoutHelper.createLinear(188, 188, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        // Brightness slider
        content.addView(makeSliderLabel(context, LocaleController.getString(R.string.KronoThemeEditorBrightness), subTextColor),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
        valueSlider = new GradientSliderView(context, false);
        valueSlider.setOnFractionChanged(frac -> {
            hsv[2] = frac;
            applyFromHsvAlpha(true, true);
        });
        content.addView(valueSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 30, 0, 0, 0, 10));

        // Alpha slider
        if (!opaqueOnly) {
            content.addView(makeSliderLabel(context, LocaleController.getString(R.string.KronoThemeEditorOpacity), subTextColor),
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
            alphaSlider = new GradientSliderView(context, true);
            alphaSlider.setOnFractionChanged(frac -> {
                alphaVal = Math.round(frac * 255f);
                applyFromHsvAlpha(true, true);
            });
            content.addView(alphaSlider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 30, 0, 0, 0, 12));
        } else {
            alphaSlider = null;
        }

        // HEX input + Copy / Paste
        LinearLayout hexRow = new LinearLayout(context);
        hexRow.setOrientation(LinearLayout.HORIZONTAL);
        hexRow.setGravity(Gravity.CENTER_VERTICAL);

        hexInput = makeBoxedEditText(context, textColor, subTextColor, accentColor, fieldBg, 9,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hexInput.setHint("#RRGGBB");
        hexInput.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (updatingInputs) return;
                Integer parsed = parseHexColor(s.toString(), opaqueOnly);
                if (parsed != null) {
                    setColorInternal(parsed, false, true);
                }
            }
        });
        hexRow.addView(hexInput, LayoutHelper.createLinear(0, 40, 1f, 0, 0, 8, 0));

        TextView copyBtn = makeChipButton(context, LocaleController.getString(R.string.KronoThemeEditorCopyHex), accentColor, fieldBg);
        copyBtn.setOnClickListener(v -> {
            String hex = formatHex(currentColor, !opaqueOnly);
            copyToClipboard(context, hex);
            Toast.makeText(context, LocaleController.formatString(R.string.KronoThemeEditorHexCopied, hex), Toast.LENGTH_SHORT).show();
        });
        hexRow.addView(copyBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, 0, 0, 6, 0));

        TextView pasteBtn = makeChipButton(context, LocaleController.getString(R.string.KronoThemeEditorPasteHex), accentColor, fieldBg);
        pasteBtn.setOnClickListener(v -> {
            String clip = readFromClipboard(context);
            Integer parsed = parseHexColor(clip, opaqueOnly);
            if (parsed == null) {
                Toast.makeText(context, LocaleController.getString(R.string.KronoThemeEditorInvalidHex), Toast.LENGTH_SHORT).show();
            } else {
                setColorInternal(parsed, true, true);
            }
        });
        hexRow.addView(pasteBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40));
        content.addView(hexRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // RGBA row
        LinearLayout rgbaRow = new LinearLayout(context);
        rgbaRow.setOrientation(LinearLayout.HORIZONTAL);
        rInput = makeBoxedEditText(context, textColor, subTextColor, accentColor, fieldBg, 3, InputType.TYPE_CLASS_NUMBER);
        gInput = makeBoxedEditText(context, textColor, subTextColor, accentColor, fieldBg, 3, InputType.TYPE_CLASS_NUMBER);
        bInput = makeBoxedEditText(context, textColor, subTextColor, accentColor, fieldBg, 3, InputType.TYPE_CLASS_NUMBER);
        aInput = makeBoxedEditText(context, textColor, subTextColor, accentColor, fieldBg, 3, InputType.TYPE_CLASS_NUMBER);

        rgbaRow.addView(makeChannelColumn(context, "R", rInput, subTextColor), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));
        rgbaRow.addView(makeChannelColumn(context, "G", gInput, subTextColor), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));
        rgbaRow.addView(makeChannelColumn(context, "B", bInput, subTextColor), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, opaqueOnly ? 0 : 6, 0));
        if (!opaqueOnly) {
            rgbaRow.addView(makeChannelColumn(context, "A", aInput, subTextColor), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        }
        TextWatcher rgbaWatcher = new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (updatingInputs) return;
                int r = parseByte(rInput.getText().toString(), Color.red(currentColor));
                int g = parseByte(gInput.getText().toString(), Color.green(currentColor));
                int b = parseByte(bInput.getText().toString(), Color.blue(currentColor));
                int a = opaqueOnly ? 255 : parseByte(aInput.getText().toString(), Color.alpha(currentColor));
                setColorInternal(Color.argb(a, r, g, b), false, true);
            }
        };
        rInput.addTextChangedListener(rgbaWatcher);
        gInput.addTextChangedListener(rgbaWatcher);
        bInput.addTextChangedListener(rgbaWatcher);
        aInput.addTextChangedListener(rgbaWatcher);
        content.addView(rgbaRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Recent colors strip
        ArrayList<Integer> recent = loadRecentColors();
        if (!recent.isEmpty()) {
            content.addView(makeSliderLabel(context, LocaleController.getString(R.string.KronoThemeEditorRecentColors), subTextColor),
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            content.addView(buildColorStrip(context, recent), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        }

        // Theme palette colors strip
        if (paletteColors != null && !paletteColors.isEmpty()) {
            content.addView(makeSliderLabel(context, LocaleController.getString(R.string.KronoThemeEditorThemePalette), subTextColor),
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            content.addView(buildColorStrip(context, paletteColors), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
        }

        scrollView.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        int maxScrollHeight = Math.max(AndroidUtilities.dp(300), (int) (AndroidUtilities.displaySize.y * 0.68f));
        outer.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, maxScrollHeight));

        View divider = new View(context);
        divider.setBackgroundColor(dividerColor);
        outer.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        // Bottom actions bar
        LinearLayout bottomBar = new LinearLayout(context);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));

        TextView cancelBtn = makeBottomAction(context, LocaleController.getString(R.string.Cancel).toUpperCase(), subTextColor);
        cancelBtn.setOnClickListener(v -> dismiss());
        bottomBar.addView(cancelBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44));

        if (showReset) {
            TextView resetBtn = makeBottomAction(context, LocaleController.getString(R.string.KronoThemeEditorResetKey).toUpperCase(), Theme.getColor(Theme.key_text_RedRegular));
            resetBtn.setOnClickListener(v -> {
                actionCommitted = true;
                if (callback != null) {
                    callback.onResetToDefault();
                }
                dismiss();
            });
            bottomBar.addView(resetBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44));
        }

        View spacer = new View(context);
        bottomBar.addView(spacer, LayoutHelper.createLinear(0, 1, 1f));

        TextView applyBtn = makeBottomAction(context, LocaleController.getString(R.string.KronoThemeEditorApply).toUpperCase(), accentColor);
        applyBtn.setOnClickListener(v -> {
            actionCommitted = true;
            saveRecentColor(currentColor);
            if (callback != null) {
                callback.onColorApplied(currentColor);
            }
            dismiss();
        });
        bottomBar.addView(applyBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44));

        outer.addView(bottomBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        setCustomView(outer);

        setOnDismissListener(dialog -> {
            if (!actionCommitted && callback != null) {
                callback.onCancelled();
            }
        });

        syncUiFromState(true);
    }

    private void applyFromHsvAlpha(boolean updateTextInputs, boolean notifyCallback) {
        int rgb = Color.HSVToColor(hsv);
        currentColor = opaqueOnly
                ? (rgb | 0xFF000000)
                : Color.argb(alphaVal, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        syncUiFromState(updateTextInputs);
        if (notifyCallback && callback != null) {
            callback.onColorChanged(currentColor);
        }
    }

    private void setColorInternal(int color, boolean updateTextInputs, boolean notifyCallback) {
        currentColor = opaqueOnly ? (color | 0xFF000000) : color;
        Color.colorToHSV(currentColor, hsv);
        alphaVal = opaqueOnly ? 255 : Color.alpha(currentColor);
        syncUiFromState(updateTextInputs);
        if (notifyCallback && callback != null) {
            callback.onColorChanged(currentColor);
        }
    }

    private void syncUiFromState(boolean updateTextInputs) {
        wheelView.setHueSat(hsv[0], hsv[1]);
        int fullBright = Color.HSVToColor(new float[]{hsv[0], hsv[1], 1f});
        valueSlider.setColors(0xFF000000, fullBright);
        valueSlider.setFraction(hsv[2]);
        if (alphaSlider != null) {
            int opaque = currentColor | 0xFF000000;
            alphaSlider.setColors(opaque & 0x00FFFFFF, opaque);
            alphaSlider.setFraction(alphaVal / 255f);
        }
        setRoundedSwatch(afterSwatch, currentColor, 8);
        updateContrastBadge();

        if (updateTextInputs) {
            updatingInputs = true;
            hexInput.setText(formatHex(currentColor, !opaqueOnly));
            hexInput.setSelection(hexInput.length());
            rInput.setText(String.valueOf(Color.red(currentColor)));
            gInput.setText(String.valueOf(Color.green(currentColor)));
            bInput.setText(String.valueOf(Color.blue(currentColor)));
            aInput.setText(String.valueOf(Color.alpha(currentColor)));
            updatingInputs = false;
        }
    }

    private void updateContrastBadge() {
        int bgRef = Theme.getColor(Theme.key_windowBackgroundWhite) | 0xFF000000;
        int fgComp = ColorUtils.compositeColors(currentColor, bgRef);
        double ratio = ColorUtils.calculateContrast(fgComp, bgRef);
        String grade = ratio >= 7.0 ? "AAA" : (ratio >= 4.5 ? "AA" : (ratio >= 3.0 ? "AA+" : "Low"));
        contrastBadge.setText(String.format(Locale.US, "%s %.1f:1 (%s)",
                LocaleController.getString(R.string.KronoThemeEditorContrast), ratio, grade));
        int badgeBg = ratio >= 4.5 ? 0xFF2E7D32 : (ratio >= 3.0 ? 0xFFEF6C00 : 0xFFC62828);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(10));
        gd.setColor(badgeBg);
        contrastBadge.setBackground(gd);
        contrastBadge.setTextColor(0xFFFFFFFF);
    }

    private HorizontalScrollView buildColorStrip(Context context, ArrayList<Integer> colors) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colors.size(); i++) {
            final int col = colors.get(i);
            View dot = new View(context);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(col);
            gd.setStroke(AndroidUtilities.dp(1.2f), Theme.isCurrentThemeDark() ? 0x66FFFFFF : 0x33000000);
            dot.setBackground(gd);
            dot.setOnClickListener(v -> setColorInternal(col, true, true));
            row.addView(dot, LayoutHelper.createLinear(32, 32, 0, 0, 8, 0));
        }
        scroll.addView(row, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        return scroll;
    }

    private static LinearLayout makeSwatchColumn(Context context, String label, View swatch, int labelColor) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextColor(labelColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        col.addView(tv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
        col.addView(swatch, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 30));
        return col;
    }

    private static LinearLayout makeChannelColumn(Context context, String label, EditTextBoldCursor et, int labelColor) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextColor(labelColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 3));
        col.addView(et, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38));
        return col;
    }

    private static TextView makeSliderLabel(Context context, String text, int color) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        return tv;
    }

    private static EditTextBoldCursor makeBoxedEditText(Context context, int textColor, int hintColor, int cursorColor, int bgColor, int maxLen, int inputType) {
        EditTextBoldCursor et = new EditTextBoldCursor(context);
        et.setTextColor(textColor);
        et.setHintTextColor(hintColor);
        et.setCursorColor(cursorColor);
        et.setCursorSize(AndroidUtilities.dp(18));
        et.setCursorWidth(1.5f);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        et.setGravity(Gravity.CENTER);
        et.setSingleLine(true);
        et.setInputType(inputType);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        et.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(8));
        bg.setColor(bgColor);
        et.setBackground(bg);
        return et;
    }

    private static TextView makeChipButton(Context context, String label, int textColor, int bgColor) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(8));
        gd.setColor(bgColor);
        tv.setBackground(gd);
        return tv;
    }

    private static TextView makeBottomAction(Context context, String label, int color) {
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

    private static void setRoundedSwatch(View view, int color, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        gd.setColor(color);
        gd.setStroke(AndroidUtilities.dp(1), Theme.isCurrentThemeDark() ? 0x55FFFFFF : 0x33000000);
        view.setBackground(gd);
    }

    private static int parseByte(String s, int fallback) {
        try {
            int v = Integer.parseInt(s.trim());
            return Math.max(0, Math.min(255, v));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static ArrayList<Integer> loadRecentColors() {
        ArrayList<Integer> list = new ArrayList<>();
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_RECENT_COLORS, "");
            if (!TextUtils.isEmpty(raw)) {
                String[] parts = raw.split(",");
                for (String p : parts) {
                    if (!TextUtils.isEmpty(p)) {
                        list.add(Integer.parseInt(p.trim()));
                    }
                }
            }
        } catch (Exception ignore) { }
        return list;
    }

    private static void saveRecentColor(int color) {
        try {
            ArrayList<Integer> list = loadRecentColors();
            list.remove(Integer.valueOf(color));
            list.add(0, color);
            while (list.size() > MAX_RECENT_COLORS) {
                list.remove(list.size() - 1);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(list.get(i));
            }
            ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_RECENT_COLORS, sb.toString()).apply();
        } catch (Exception ignore) { }
    }

    // --- Shared Static Utilities ---

    public static boolean isOpaqueRequiredKey(int key) {
        return key == Theme.key_chat_wallpaper
                || key == Theme.key_chat_wallpaper_gradient_to1
                || key == Theme.key_chat_wallpaper_gradient_to2
                || key == Theme.key_chat_wallpaper_gradient_to3
                || key == Theme.key_windowBackgroundWhite
                || key == Theme.key_windowBackgroundGray
                || key == Theme.key_actionBarDefault
                || key == Theme.key_actionBarDefaultArchived;
    }

    public static String formatHex(int color, boolean includeAlphaIfTranslucent) {
        int a = Color.alpha(color);
        if (includeAlphaIfTranslucent && a != 255) {
            return String.format(Locale.US, "#%08X", color);
        }
        return String.format(Locale.US, "#%06X", (color & 0x00FFFFFF));
    }

    public static Integer parseHexColor(String input, boolean forceOpaque) {
        if (input == null) {
            return null;
        }
        String s = input.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.length() == 3) {
            char r = s.charAt(0), g = s.charAt(1), b = s.charAt(2);
            s = "" + r + r + g + g + b + b;
        }
        if (s.length() != 6 && s.length() != 8) {
            return null;
        }
        try {
            long val = Long.parseLong(s, 16);
            int color = (s.length() == 6) ? (int) (0xFF000000L | val) : (int) val;
            return forceOpaque ? (color | 0xFF000000) : color;
        } catch (Exception e) {
            return null;
        }
    }

    public static void copyToClipboard(Context context, String text) {
        if (context == null || text == null) return;
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("KronoColorHex", text));
            }
        } catch (Exception ignore) { }
    }

    public static String readFromClipboard(Context context) {
        if (context == null) return null;
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(context);
                return cs != null ? cs.toString() : null;
            }
        } catch (Exception ignore) { }
        return null;
    }

    public static int categorizeKey(String rawName) {
        if (TextUtils.isEmpty(rawName)) {
            return CAT_OTHER;
        }
        String k = rawName.toLowerCase(Locale.ROOT);
        if (k.startsWith("actionbar") || k.startsWith("actionmode") || k.contains("topbar") || k.startsWith("contextprogress")) {
            return CAT_ACTION_BAR;
        }
        if (k.startsWith("windowbackground") || k.startsWith("divider") || k.startsWith("listselector")
                || k.startsWith("graysection") || k.startsWith("empty") || k.startsWith("fastscroll")) {
            return CAT_WINDOW;
        }
        if (k.startsWith("chat_") || k.startsWith("chats_") || k.startsWith("inapp") || k.startsWith("sticker")) {
            return CAT_CHATS;
        }
        if (k.startsWith("dialog") || k.startsWith("undo_") || k.startsWith("tooltip") || k.startsWith("menu") || k.startsWith("sheet")) {
            return CAT_DIALOGS;
        }
        if (k.startsWith("switch") || k.startsWith("checkbox") || k.startsWith("radio") || k.startsWith("featuredstickers_")
                || k.startsWith("progress") || k.startsWith("button") || k.startsWith("key_text_") || k.startsWith("text_")) {
            return CAT_CONTROLS;
        }
        if (k.startsWith("profile_") || k.startsWith("avatar_") || k.startsWith("groupcreate_") || k.startsWith("sharedmedia_")) {
            return CAT_PROFILE;
        }
        if (k.startsWith("player_") || k.startsWith("music") || k.startsWith("video") || k.startsWith("photo")
                || k.startsWith("media") || k.startsWith("picker_") || k.startsWith("location_")) {
            return CAT_MEDIA;
        }
        if (k.startsWith("voip") || k.startsWith("calls_") || k.startsWith("stories_") || k.startsWith("story_")) {
            return CAT_CALLS;
        }
        return CAT_OTHER;
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }

    // --- Custom HSV Wheel View ---

    private static class ColorWheelView extends View {
        interface Listener {
            void onChanged(float hue, float sat);
        }

        private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float hue = 0f;
        private float sat = 0f;
        private float cx, cy, radius;
        private Listener listener;

        ColorWheelView(Context context) {
            super(context);
            thumbStroke.setStyle(Paint.Style.STROKE);
            thumbStroke.setStrokeWidth(AndroidUtilities.dp(2.5f));
            thumbStroke.setColor(0xFFFFFFFF);
            thumbStroke.setShadowLayer(AndroidUtilities.dp(2), 0, 0, 0x88000000);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        void setOnHueSatChanged(Listener l) {
            this.listener = l;
        }

        void setHueSat(float h, float s) {
            this.hue = h;
            this.sat = s;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            cx = w / 2f;
            cy = h / 2f;
            radius = Math.min(w, h) / 2f - AndroidUtilities.dp(10);
            if (radius > 0) {
                int[] sweepColors = new int[]{
                        0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
                        0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
                };
                Shader sweep = new SweepGradient(cx, cy, sweepColors, null);
                Shader radial = new RadialGradient(cx, cy, radius, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
                wheelPaint.setShader(new ComposeShader(sweep, radial, PorterDuff.Mode.SRC_OVER));
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (radius <= 0) return;
            canvas.drawCircle(cx, cy, radius, wheelPaint);
            double rad = Math.toRadians(hue);
            float tx = cx + (float) (Math.cos(rad) * sat * radius);
            float ty = cy + (float) (Math.sin(rad) * sat * radius);
            thumbFill.setColor(Color.HSVToColor(new float[]{hue, sat, 1f}));
            float tr = AndroidUtilities.dp(9);
            canvas.drawCircle(tx, ty, tr, thumbFill);
            canvas.drawCircle(tx, ty, tr, thumbStroke);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                getParent().requestDisallowInterceptTouchEvent(true);
                float dx = event.getX() - cx;
                float dy = event.getY() - cy;
                float dist = (float) Math.hypot(dx, dy);
                sat = Math.min(1f, Math.max(0f, dist / Math.max(1f, radius)));
                float deg = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (deg < 0) deg += 360f;
                hue = deg;
                invalidate();
                if (listener != null) {
                    listener.onChanged(hue, sat);
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
    }

    // --- Custom Gradient Slider View ---

    private static class GradientSliderView extends View {
        interface Listener {
            void onChanged(float fraction);
        }

        private final boolean checkered;
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF barRect = new RectF();
        private int startColor = 0xFF000000;
        private int endColor = 0xFFFFFFFF;
        private float fraction = 1f;
        private Listener listener;

        GradientSliderView(Context context, boolean checkered) {
            super(context);
            this.checkered = checkered;
            checkerPaint.setColor(0x33888888);
            thumbPaint.setColor(0xFFFFFFFF);
            thumbBorder.setStyle(Paint.Style.STROKE);
            thumbBorder.setStrokeWidth(AndroidUtilities.dp(2));
            thumbBorder.setColor(0x99000000);
        }

        void setOnFractionChanged(Listener l) {
            this.listener = l;
        }

        void setColors(int start, int end) {
            this.startColor = start;
            this.endColor = end;
            updateShader();
            invalidate();
        }

        void setFraction(float f) {
            this.fraction = Math.max(0f, Math.min(1f, f));
            invalidate();
        }

        private void updateShader() {
            if (barRect.width() > 0) {
                barPaint.setShader(new LinearGradient(barRect.left, 0, barRect.right, 0, startColor, endColor, Shader.TileMode.CLAMP));
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float pad = AndroidUtilities.dp(10);
            float barH = AndroidUtilities.dp(12);
            barRect.set(pad, (h - barH) / 2f, w - pad, (h + barH) / 2f);
            updateShader();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float r = barRect.height() / 2f;
            if (checkered) {
                float cell = AndroidUtilities.dp(4);
                int cols = (int) Math.ceil(barRect.width() / cell);
                int rows = (int) Math.ceil(barRect.height() / cell);
                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < cols; x++) {
                        if (((x + y) & 1) == 0) {
                            float l = barRect.left + x * cell;
                            float t = barRect.top + y * cell;
                            canvas.drawRect(l, t, Math.min(l + cell, barRect.right), Math.min(t + cell, barRect.bottom), checkerPaint);
                        }
                    }
                }
            }
            canvas.drawRoundRect(barRect, r, r, barPaint);
            float tx = barRect.left + fraction * barRect.width();
            float ty = barRect.centerY();
            float tr = AndroidUtilities.dp(10);
            canvas.drawCircle(tx, ty, tr, thumbPaint);
            canvas.drawCircle(tx, ty, tr, thumbBorder);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                getParent().requestDisallowInterceptTouchEvent(true);
                float f = (event.getX() - barRect.left) / Math.max(1f, barRect.width());
                fraction = Math.max(0f, Math.min(1f, f));
                invalidate();
                if (listener != null) {
                    listener.onChanged(fraction);
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
