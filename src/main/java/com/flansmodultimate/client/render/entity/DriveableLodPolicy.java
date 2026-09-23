package com.flansmodultimate.client.render.entity;

/** Pure selection rules shared by geometry LOD and generated impostors. Distances are in blocks. */
public final class DriveableLodPolicy
{
    private DriveableLodPolicy() {}

    public static float distanceScale(float radius, boolean groundVehicle, float groundFactor)
    {
        if (!Float.isFinite(radius) || radius <= 0F)
            return Float.POSITIVE_INFINITY;
        // Do not cap this: exceptionally large ships must keep growing their LOD distances.
        float size = Math.max(1F, radius / 3F);
        // Discount small land vehicles, fading it out entirely for very large machines.
        float largeModelBlend = clamp((radius - 6F) / 6F);
        float factor = groundVehicle ? groundFactor + (1F - groundFactor) * largeModelBlend : 1F;
        return size * factor;
    }

    public static float partThreshold(float base, float maximum, float detailMultiplier,
                                      double distance, float distanceScale)
    {
        if (base <= 0F || maximum <= base || !Double.isFinite(distance)
            || !Float.isFinite(distanceScale) || distanceScale <= 0F)
            return base;
        float blend = clamp((float)(distance / distanceScale - 24D) / 56F);
        // Smooth the endpoints while retaining a monotonic transition from 24 to 80 scaled blocks.
        blend = blend * blend * (3F - 2F * blend);
        float far = Math.max(base, maximum * detailMultiplier);
        return base + (far - base) * blend;
    }

    public static int resolution(int configured, int qualityMultiplier)
    {
        return Math.min(256, configured * qualityMultiplier);
    }

    public static int yawAngles(int configured, int qualityMultiplier)
    {
        return Math.min(16, configured * qualityMultiplier);
    }

    public static float qualityPixelLimit(int resolution)
    {
        // Capture includes a 12% margin. Permit only modest upsampling of the useful image.
        return resolution / 1.12F * 1.25F;
    }

    public static float impostorThreshold(float configured, double distance, float minimumDistance,
                                          float maximumDistance, boolean groundVehicle, int resolution)
    {
        // Keep explicit off / pixel-only configurations: no distance promotion without a maximum.
        if (!groundVehicle || configured <= 0F || maximumDistance <= minimumDistance)
            return configured;
        float blend = clamp((float)((distance - minimumDistance) / (maximumDistance - minimumDistance)));
        float promoted = Math.max(configured, qualityPixelLimit(resolution));
        return configured + (promoted - configured) * blend;
    }

    public static boolean withinImageQuality(float projectedPixels, int resolution, boolean previous)
    {
        return Float.isFinite(projectedPixels) && projectedPixels > 0F
            && projectedPixels <= qualityPixelLimit(resolution) * (previous ? 1.1F : 1F);
    }

    private static float clamp(float value)
    {
        return Math.max(0F, Math.min(1F, value));
    }
}
