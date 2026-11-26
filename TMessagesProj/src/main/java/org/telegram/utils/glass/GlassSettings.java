package org.telegram.utils.glass;

import java.util.Objects;

public class GlassSettings {

    public final boolean isBlurEnabled;
    public final boolean isLiquidGlassEnabled;
    public final int togglesMask;

    public GlassSettings(boolean isBlurEnabled, boolean isLiquidGlassEnabled, int togglesMask) {
        this.isBlurEnabled = isBlurEnabled;
        this.isLiquidGlassEnabled = isLiquidGlassEnabled;
        this.togglesMask = togglesMask;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GlassSettings)) {
            return false;
        }

        GlassSettings settings = (GlassSettings) object;
        return isBlurEnabled == settings.isBlurEnabled
                && isLiquidGlassEnabled == settings.isLiquidGlassEnabled
                && togglesMask == settings.togglesMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isBlurEnabled, isLiquidGlassEnabled, togglesMask);
    }
}