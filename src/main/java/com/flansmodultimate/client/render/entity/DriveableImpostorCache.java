package com.flansmodultimate.client.render.entity;

import net.minecraft.util.Mth;

/**
 * The 1.21 off-screen framebuffer impostor implementation cannot be carried
 * across the 26.1 extraction-renderer boundary. Vehicles are deliberately
 * rendered with their complete geometry until a native render-state cache can
 * replace it; this favors visual correctness over the former distance LOD.
 */
public final class DriveableImpostorCache
{
    private static final float HYSTERESIS = 1.2F;

    private DriveableImpostorCache() { }

    /** Retained for LOD policy callers while native impostor capture is disabled. */
    public static float adaptivePartThreshold(float baseThreshold, float maximumThreshold,
                                              float projectedPixelDiameter, float impostorPixelThreshold)
    {
        if (baseThreshold <= 0F || maximumThreshold <= baseThreshold
            || !Float.isFinite(projectedPixelDiameter) || impostorPixelThreshold <= 0F)
            return baseThreshold;

        float start = impostorPixelThreshold * 3F;
        float blend = Mth.clamp((start - projectedPixelDiameter) / (start - impostorPixelThreshold), 0F, 1F);
        return Mth.lerp(blend, baseThreshold, maximumThreshold);
    }

    static boolean shouldUseImpostor(float projectedPixels, double cameraDistance,
                                     float pixelThreshold, float maximumDistance,
                                     boolean wasUsingImpostor)
    {
        float activePixelThreshold = wasUsingImpostor ? pixelThreshold * HYSTERESIS : pixelThreshold;
        double activeDistanceThreshold = wasUsingImpostor ? maximumDistance / HYSTERESIS : maximumDistance;
        return (pixelThreshold > 0F && projectedPixels <= activePixelThreshold)
            || (maximumDistance > 0F && cameraDistance >= activeDistanceThreshold);
    }

    public static void clear()
    {
        // No native impostor resources are allocated on 26.1.
    }
}
