package com.flansmodultimate.common.types;

import net.minecraft.resources.Identifier;

public interface IScope
{
    float getFovFactor();

    float getZoomFactor();

    boolean hasZoomOverlay();

    Identifier getZoomOverlay();
}
