package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.IScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/** Bounds and steps variable magnification values from legacy scope definitions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScopeZoom
{
    public static float next(float current, IScope scope, boolean increase)
    {
        float augment = Math.abs(scope.getZoomAugment());
        if (!Float.isFinite(augment) || augment <= 0F)
            return clamp(current, scope);
        return clamp(current + (increase ? augment : -augment), scope);
    }

    public static float clamp(float zoom, IScope scope)
    {
        float min = minimum(scope);
        float first = finitePositive(scope.getMinZoom(), 1F);
        float second = finitePositive(scope.getMaxZoom(), first);
        float max = Math.max(first, second);
        return Mth.clamp(Float.isFinite(zoom) ? zoom : min, min, max);
    }

    public static float minimum(IScope scope)
    {
        float first = finitePositive(scope.getMinZoom(), 1F);
        float second = finitePositive(scope.getMaxZoom(), first);
        return Math.min(first, second);
    }

    private static float finitePositive(float value, float fallback)
    {
        return Float.isFinite(value) && value > 0F ? value : fallback;
    }
}
