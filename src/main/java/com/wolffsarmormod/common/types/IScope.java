package com.wolffsarmormod.common.types;

import net.minecraft.resources.ResourceLocation;

public interface IScope
{
    float getFovFactor();

    float getZoomFactor();

    boolean hasZoomOverlay();

    ResourceLocation getZoomOverlay();
}
