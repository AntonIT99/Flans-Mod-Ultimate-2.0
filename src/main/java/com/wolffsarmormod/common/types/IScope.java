package com.wolffsarmormod.common.types;

public interface IScope
{
    float getFOVFactor();

    float getZoomFactor();

    boolean hasZoomOverlay();

    String getZoomOverlay();
}
