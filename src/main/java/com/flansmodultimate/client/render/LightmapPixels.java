package com.flansmodultimate.client.render;

import com.mojang.blaze3d.platform.NativeImage;

/** Internal bridge into the vanilla lightmap, for what is lit outside the vanilla shaders. Render thread. */
public interface LightmapPixels
{
    /** The lightmap as last computed on the CPU: block light along x, sky light along y, in ABGR. */
    NativeImage flansmodultimateLightPixels();
}
