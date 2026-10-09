package com.flansmodultimate.common.types;

import net.minecraft.resources.ResourceLocation;

public interface IScope
{
    float getFovFactor();

    float getZoomFactor();

    boolean hasVariableZoom();

    float getMinZoom();

    float getMaxZoom();

    float getZoomAugment();

    boolean hasZoomOverlay();

    ResourceLocation getZoomOverlay();

    /** Whether looking through this scope shows thermal imaging, from the Labjac Edition's {@code HasThermalVision}. */
    default boolean hasThermalVision()
    {
        return false;
    }
}
