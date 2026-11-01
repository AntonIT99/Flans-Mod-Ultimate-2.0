package com.flansmodultimate.client.render.layer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Pure selection rules for Flan armour level of detail, shared by the armour layer and its tests. Distances are in
 * blocks, sizes in physical screen pixels.
 *
 * <p>
 * Uniforms are culled by the size they project to, never by distance alone, so magnification keeps a soldier's full
 * detail: a scope raises the projection scale, which raises every box's projected diameter past the threshold again.
 * Distance only decides how large that threshold is, and it is blended in smoothly so that no row of boxes vanishes at
 * one step of the camera.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArmorLodPolicy
{
    /** Camera distance up to which the near threshold is used unchanged: beyond ordinary infantry fighting range. */
    static final float NEAR_DISTANCE = 16F;
    /** Camera distance at which the far threshold is fully reached. */
    static final float FAR_DISTANCE = 64F;

    /**
     * @param base
     *            the near projected part diameter, which always applies
     * @param maximum
     *            the far projected part diameter, before the detail multiplier
     * @param detailMultiplier
     *            multiplies the far threshold; 1 keeps it as configured
     * @param distance
     *            camera distance to the wearer
     * @return the projected part diameter below which a uniform's boxes are left out at that distance
     */
    public static float partThreshold(float base, float maximum, float detailMultiplier, double distance)
    {
        if (base <= 0F || maximum <= base || !Double.isFinite(distance))
            return base;
        float blend = clamp((float) ((distance - NEAR_DISTANCE) / (FAR_DISTANCE - NEAR_DISTANCE)));
        blend = blend * blend * (3F - 2F * blend);
        float far = Math.max(base, maximum * detailMultiplier);
        return base + (far - base) * blend;
    }

    private static float clamp(float value)
    {
        return Math.max(0F, Math.min(1F, value));
    }
}
