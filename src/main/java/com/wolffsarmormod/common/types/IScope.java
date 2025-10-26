package com.wolffsarmormod.common.types;

import net.minecraft.resources.ResourceLocation;

public interface IScope
{
    float getFOVFactor();

    float getZoomFactor();

    boolean hasZoomOverlay();

    ResourceLocation getZoomOverlay();
}
