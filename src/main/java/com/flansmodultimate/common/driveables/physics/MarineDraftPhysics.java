package com.flansmodultimate.common.driveables.physics;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.UnaryOperator;

/**
 * Draft-based flotation from the optional {@code RealDraftM} key.
 *
 * <p>
 * This is a conservative, independently usable override rather than a naval
 * physics rewrite. Legacy flotation adds a constant upward velocity while the
 * hull is in water, which means a boat's resting height is whatever the constant
 * happens to produce. With a declared draft the hull instead settles so that its
 * bottom sits the authored distance below the water surface, using a damped
 * restoring response in place of the constant.
 *
 * <p>
 * A boat without {@code RealDraftM} or an authored core box keeps legacy
 * flotation. An entity's compact collision box is not its keel.
 */
public final class MarineDraftPhysics
{
    private MarineDraftPhysics()
    {}

    /** Lowest world-space core corner, using the rendered model's scale and pose. */
    public static double hullBottomY(AABB core, double scale, UnaryOperator<Vec3> modelToWorld)
    {
        double bottom = Double.POSITIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++)
        {
            Vec3 point = new Vec3((corner & 1) == 0 ? core.minX : core.maxX, (corner & 2) == 0 ? core.minY : core.maxY, (corner & 4) == 0 ? core.minZ : core.maxZ);
            bottom = Math.min(bottom, modelToWorld.apply(point.scale(scale)).y);
        }
        return bottom;
    }

    /**
     * Vertical velocity for a hull with a declared draft.
     *
     * @param currentVerticalVelocity
     *            current vertical velocity in blocks per tick
     * @param hullBottomY
     *            world Y of the bottom of the hull
     * @param waterSurfaceY
     *            world Y of the water surface above the hull
     * @param draftM
     *            authored draft in metres, which are blocks
     * @param maxBuoyancy
     *            the legacy buoyancy clamp, reused as the rise ceiling
     * @return the new vertical velocity in blocks per tick
     */
    public static double verticalVelocity(double currentVerticalVelocity, double hullBottomY, double waterSurfaceY, double draftM, double maxBuoyancy)
    {
        if (!Double.isFinite(currentVerticalVelocity) || !Double.isFinite(hullBottomY) || !Double.isFinite(waterSurfaceY) || !Double.isFinite(draftM) || draftM <= 0D)
            return Double.isFinite(currentVerticalVelocity) ? currentVerticalVelocity : 0D;

        double ceiling = Double.isFinite(maxBuoyancy) ? Math.max(0D, maxBuoyancy) : 0D;
        // Positive error means the hull is riding deeper than its draft and needs
        // to rise; negative means it is sitting too high and should settle.
        double targetHullBottomY = waterSurfaceY - draftM;
        double error = targetHullBottomY - hullBottomY;
        double target = error * VehiclePhysicsConstants.DRAFT_RESTORING_STIFFNESS;
        target = Math.max(-ceiling, Math.min(ceiling, target));

        // Damped approach rather than a direct assignment, so a hull dropped from
        // height does not stop instantly at the waterline.
        double blended = currentVerticalVelocity + (target - currentVerticalVelocity) * 0.35D;
        return Double.isFinite(blended) ? blended : 0D;
    }
}
