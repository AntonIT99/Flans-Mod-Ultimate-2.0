package com.flansmodultimate.common.driveables.physics;

/**
 * Draft-based flotation from the optional {@code RealDraftM} key.
 *
 * <p>This is a conservative, independently usable override rather than a naval
 * physics rewrite. Legacy flotation adds a constant upward velocity while the
 * hull is in water, which means a boat's resting height is whatever the constant
 * happens to produce. With a declared draft the hull instead settles so that its
 * bottom sits the authored distance below the water surface, using a damped
 * restoring response in place of the constant.
 *
 * <p>A boat that does not declare {@code RealDraftM} never reaches this class and
 * keeps the legacy constant exactly.
 */
public final class MarineDraftPhysics
{
    private MarineDraftPhysics() {}

    /**
     * Vertical velocity for a hull with a declared draft.
     *
     * @param currentVerticalVelocity current vertical velocity in blocks per tick
     * @param hullBottomY             world Y of the bottom of the hull
     * @param waterSurfaceY           world Y of the water surface above the hull
     * @param draftM                  authored draft in metres, which are blocks
     * @param maxBuoyancy             the legacy buoyancy clamp, reused as the rise ceiling
     * @return the new vertical velocity in blocks per tick
     */
    public static double verticalVelocity(double currentVerticalVelocity, double hullBottomY,
                                          double waterSurfaceY, double draftM, double maxBuoyancy)
    {
        if (!Double.isFinite(currentVerticalVelocity) || !Double.isFinite(hullBottomY)
            || !Double.isFinite(waterSurfaceY) || !Double.isFinite(draftM) || draftM <= 0D)
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
