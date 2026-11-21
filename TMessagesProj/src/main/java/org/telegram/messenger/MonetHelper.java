package org.telegram.messenger;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import java.util.HashMap;

public class MonetHelper {

    private static final HashMap<String, Integer> ids = new HashMap<String, Integer>() {{
        put("a1_0", android.R.color.system_accent1_0);
        put("a1_10", android.R.color.system_accent1_10);
        put("a1_50", android.R.color.system_accent1_50);
        put("a1_100", android.R.color.system_accent1_100);
        put("a1_200", android.R.color.system_accent1_200);
        put("a1_300", android.R.color.system_accent1_300);
        put("a1_400", android.R.color.system_accent1_400);
        put("a1_500", android.R.color.system_accent1_500);
        put("a1_600", android.R.color.system_accent1_600);
        put("a1_700", android.R.color.system_accent1_700);
        put("a1_800", android.R.color.system_accent1_800);
        put("a1_900", android.R.color.system_accent1_900);
        put("a1_1000", android.R.color.system_accent1_1000);
        put("a2_0", android.R.color.system_accent2_0);
        put("a2_10", android.R.color.system_accent2_10);
        put("a2_50", android.R.color.system_accent2_50);
        put("a2_100", android.R.color.system_accent2_100);
        put("a2_200", android.R.color.system_accent2_200);
        put("a2_300", android.R.color.system_accent2_300);
        put("a2_400", android.R.color.system_accent2_400);
        put("a2_500", android.R.color.system_accent2_500);
        put("a2_600", android.R.color.system_accent2_600);
        put("a2_700", android.R.color.system_accent2_700);
        put("a2_800", android.R.color.system_accent2_800);
        put("a2_900", android.R.color.system_accent2_900);
        put("a2_1000", android.R.color.system_accent2_1000);
        put("a3_0", android.R.color.system_accent3_0);
        put("a3_10", android.R.color.system_accent3_10);
        put("a3_50", android.R.color.system_accent3_50);
        put("a3_100", android.R.color.system_accent3_100);
        put("a3_200", android.R.color.system_accent3_200);
        put("a3_300", android.R.color.system_accent3_300);
        put("a3_400", android.R.color.system_accent3_400);
        put("a3_500", android.R.color.system_accent3_500);
        put("a3_600", android.R.color.system_accent3_600);
        put("a3_700", android.R.color.system_accent3_700);
        put("a3_800", android.R.color.system_accent3_800);
        put("a3_900", android.R.color.system_accent3_900);
        put("a3_1000", android.R.color.system_accent3_1000);
        put("n1_0", android.R.color.system_neutral1_0);
        put("n1_10", android.R.color.system_neutral1_10);
        put("n1_50", android.R.color.system_neutral1_50);
        put("n1_100", android.R.color.system_neutral1_100);
        put("n1_200", android.R.color.system_neutral1_200);
        put("n1_300", android.R.color.system_neutral1_300);
        put("n1_400", android.R.color.system_neutral1_400);
        put("n1_500", android.R.color.system_neutral1_500);
        put("n1_600", android.R.color.system_neutral1_600);
        put("n1_700", android.R.color.system_neutral1_700);
        put("n1_800", android.R.color.system_neutral1_800);
        put("n1_900", android.R.color.system_neutral1_900);
        put("n1_1000", android.R.color.system_neutral1_1000);
        put("n2_0", android.R.color.system_neutral2_0);
        put("n2_10", android.R.color.system_neutral2_10);
        put("n2_50", android.R.color.system_neutral2_50);
        put("n2_100", android.R.color.system_neutral2_100);
        put("n2_200", android.R.color.system_neutral2_200);
        put("n2_300", android.R.color.system_neutral2_300);
        put("n2_400", android.R.color.system_neutral2_400);
        put("n2_500", android.R.color.system_neutral2_500);
        put("n2_600", android.R.color.system_neutral2_600);
        put("n2_700", android.R.color.system_neutral2_700);
        put("n2_800", android.R.color.system_neutral2_800);
        put("n2_900", android.R.color.system_neutral2_900);
        put("n2_1000", android.R.color.system_neutral2_1000);
    }};

    private static final HashMap<String, Integer> fallbackColors = new HashMap<String, Integer>() {{
        // Material You baseline blue-slate tonal palette (fallback for Android < 12)
        put("a1_0", 0xFFFFFFFF);
        put("a1_10", 0xFFFDFBFF);
        put("a1_50", 0xFFEAF1FF);
        put("a1_100", 0xFFD3E3FD);
        put("a1_200", 0xFFA8C7FA);
        put("a1_300", 0xFF7CACF8);
        put("a1_400", 0xFF4C8DF6);
        put("a1_500", 0xFF1B6EF3);
        put("a1_600", 0xFF0B57D0);
        put("a1_700", 0xFF0842A0);
        put("a1_800", 0xFF062E6F);
        put("a1_900", 0xFF041E49);
        put("a1_1000", 0xFF001233);

        put("a2_0", 0xFFFFFFFF);
        put("a2_10", 0xFFF8F9FD);
        put("a2_50", 0xFFEFF3FA);
        put("a2_100", 0xFFDCE4F2);
        put("a2_200", 0xFFBCCAE0);
        put("a2_300", 0xFF9BAEBF);
        put("a2_400", 0xFF7D91AD);
        put("a2_500", 0xFF617591);
        put("a2_600", 0xFF495B75);
        put("a2_700", 0xFF35455C);
        put("a2_800", 0xFF233044);
        put("a2_900", 0xFF131D2D);
        put("a2_1000", 0xFF080E17);

        put("a3_0", 0xFFFFFFFF);
        put("a3_10", 0xFFFFF8FE);
        put("a3_50", 0xFFF6ECFA);
        put("a3_100", 0xFFEAD9F5);
        put("a3_200", 0xFFD7BAEB);
        put("a3_300", 0xFFC19ADE);
        put("a3_400", 0xFFA77BCC);
        put("a3_500", 0xFF8B5EB5);
        put("a3_600", 0xFF704499);
        put("a3_700", 0xFF56307B);
        put("a3_800", 0xFF3D1E5E);
        put("a3_900", 0xFF280F42);
        put("a3_1000", 0xFF160426);

        put("n1_0", 0xFFFFFFFF);
        put("n1_10", 0xFFFAF9FD);
        put("n1_50", 0xFFF0F0F5);
        put("n1_100", 0xFFE2E2E8);
        put("n1_200", 0xFFC6C6CC);
        put("n1_300", 0xFFABABB1);
        put("n1_400", 0xFF8F8F96);
        put("n1_500", 0xFF74747B);
        put("n1_600", 0xFF5C5C62);
        put("n1_700", 0xFF44444A);
        put("n1_800", 0xFF2D2D33);
        put("n1_900", 0xFF19191E);
        put("n1_1000", 0xFF000000);

        put("n2_0", 0xFFFFFFFF);
        put("n2_10", 0xFFF8F9FC);
        put("n2_50", 0xFFEEF0F6);
        put("n2_100", 0xFFDFE2EC);
        put("n2_200", 0xFFC3C7D4);
        put("n2_300", 0xFFA7ACB9);
        put("n2_400", 0xFF8B909D);
        put("n2_500", 0xFF707582);
        put("n2_600", 0xFF575C68);
        put("n2_700", 0xFF40444F);
        put("n2_800", 0xFF2A2E38);
        put("n2_900", 0xFF161A22);
        put("n2_1000", 0xFF000000);
    }};

    private static final HashMap<String, Integer> specialColors = new HashMap<String, Integer>() {{
        put("monetRedDark", 0xFFB3261E);
        put("monetRedLight", 0xFFF2B8B5);
        put("monetRedCall", 0xFFDC362E);
        put("monetGreenCall", 0xFF386A20);

        // Monet harmonized avatar background colors
        put("monetAvatarBlue", 0xFF5CAFFA);
        put("monetAvatarCyan", 0xFF4DD0E1);
        put("monetAvatarGreen", 0xFF76C86F);
        put("monetAvatarOrange", 0xFFF5A352);
        put("monetAvatarPink", 0xFFF27EA9);
        put("monetAvatarRed", 0xFFE56B6F);
        put("monetAvatarViolet", 0xFFA68DF7);

        // Monet avatar sender name colors (Light theme)
        put("monetAvatarNameBlue", 0xFF2A75C4);
        put("monetAvatarNameCyan", 0xFF00838F);
        put("monetAvatarNameGreen", 0xFF2E7D32);
        put("monetAvatarNameOrange", 0xFFC65D14);
        put("monetAvatarNamePink", 0xFFC2185B);
        put("monetAvatarNameRed", 0xFFC62828);
        put("monetAvatarNameViolet", 0xFF6A48C8);

        // Monet avatar sender name colors (Dark / AMOLED theme)
        put("monetAvatarNameDarkBlue", 0xFF8CC5FF);
        put("monetAvatarNameDarkCyan", 0xFF7CE8F5);
        put("monetAvatarNameDarkGreen", 0xFF9BE695);
        put("monetAvatarNameDarkOrange", 0xFFFFC17D);
        put("monetAvatarNameDarkPink", 0xFFFFA3C7);
        put("monetAvatarNameDarkRed", 0xFFFF9A9E);
        put("monetAvatarNameDarkViolet", 0xFFC8B4FF);
    }};

    @ColorInt
    public static int getColor(String color) {
        return getColor(color, false);
    }

    @ColorInt
    public static int getColor(String color, boolean amoled) {
        if (color == null) {
            return 0;
        }
        String key = color.trim();
        if (key.isEmpty()) {
            return 0;
        }
        try {
            Integer special = specialColors.get(key);
            if (special != null) {
                if (key.startsWith("monetAvatar")) {
                    int accent = getBaseTokenColor("a1_500");
                    float ratio = key.startsWith("monetAvatarName") ? 0.14f : 0.18f;
                    return ColorUtils.blendARGB(special, accent, ratio);
                }
                return special;
            }

            // Support opacity suffixes like n1_800_80, a1_100_90, n2_800_70
            int alphaPercent = -1;
            int lastUnderscore = key.lastIndexOf('_');
            if (lastUnderscore > 0 && lastUnderscore < key.length() - 1) {
                String maybeBase = key.substring(0, lastUnderscore);
                String maybeAlpha = key.substring(lastUnderscore + 1);
                if (ids.containsKey(maybeBase) && maybeAlpha.length() <= 3) {
                    try {
                        int parsed = Integer.parseInt(maybeAlpha);
                        if (parsed >= 0 && parsed <= 100) {
                            key = maybeBase;
                            alphaPercent = parsed;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            int baseColor = getBaseTokenColor(key);
            if (alphaPercent >= 0 && baseColor != 0) {
                float factor = Math.max(0f, Math.min(1f, alphaPercent / 100f));
                return ColorUtils.blendARGB(Color.BLACK, baseColor, factor);
            }
            return baseColor;
        } catch (Exception e) {
            FileLog.e("Error loading color " + color, e);
            return 0;
        }
    }

    @ColorInt
    private static int getBaseTokenColor(String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Context context = ApplicationLoader.applicationContext;
            Integer idObj = ids.get(key);
            if (context != null && idObj != null) {
                try {
                    return context.getColor(idObj);
                } catch (Exception ignored) {
                }
            }
        }
        Integer fb = fallbackColors.get(key);
        if (fb != null) {
            return fb;
        }
        return Color.BLACK;
    }

    public static boolean isMonetColor(String key) {
        if (key == null) {
            return false;
        }
        String trimmed = key.trim();
        if (ids.containsKey(trimmed) || specialColors.containsKey(trimmed)) {
            return true;
        }
        int lastUnderscore = trimmed.lastIndexOf('_');
        if (lastUnderscore > 0 && lastUnderscore < trimmed.length() - 1) {
            String maybeBase = trimmed.substring(0, lastUnderscore);
            String maybeAlpha = trimmed.substring(lastUnderscore + 1);
            if (ids.containsKey(maybeBase) && maybeAlpha.length() <= 3) {
                try {
                    int parsed = Integer.parseInt(maybeAlpha);
                    return parsed >= 0 && parsed <= 100;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }
}
