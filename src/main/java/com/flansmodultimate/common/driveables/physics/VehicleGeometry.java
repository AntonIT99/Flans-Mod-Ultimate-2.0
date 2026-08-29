package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

/**
 * Physical dimensions derived from data a content pack already declares, in
 * blocks, which are metres under the mod's convention.
 *
 * <p>No new parsing keys exist for length, width, beam or wheelbase. Length,
 * width and height come from the {@code SetupPart core} collision box; wheelbase
 * and track width come from the spread of the declared wheel positions. Checking
 * this against a shipped definition, the S-100 Schnellboot's core box yields
 * 35.1 m by 5.3 m against a real 34.9 m by 5.1 m, so the existing geometry is
 * already accurate enough to derive from.
 *
 * <p>Every field is nullable because a definition may omit the core box or
 * declare no wheels; consumers must fall back rather than assume.
 */
public record VehicleGeometry(
    @Nullable Float lengthM,
    @Nullable Float widthM,
    @Nullable Float heightM,
    @Nullable Float wheelbaseM,
    @Nullable Float trackWidthM)
{
    public static final VehicleGeometry EMPTY = new VehicleGeometry(null, null, null, null, null);

    /**
     * Builds geometry from a core collision box expressed in blocks.
     *
     * <p>The legacy {@code SetupPart} width parameter spans the fore-aft axis, so
     * {@code CollisionBox} stores it as depth. Length therefore reads from depth
     * and lateral width from width, not the other way round.
     */
    public static VehicleGeometry fromCoreBox(float boxWidth, float boxHeight, float boxDepth,
                                              @Nullable Float wheelbaseM, @Nullable Float trackWidthM)
    {
        return new VehicleGeometry(
            positiveOrNull(boxDepth),
            positiveOrNull(boxWidth),
            positiveOrNull(boxHeight),
            positiveOrNull(wheelbaseM),
            positiveOrNull(trackWidthM));
    }

    /** Longest horizontal dimension, useful as a single size proxy. */
    @Nullable
    public Float horizontalExtentM()
    {
        if (lengthM == null)
            return widthM;
        if (widthM == null)
            return lengthM;
        return Math.max(lengthM, widthM);
    }

    @Nullable
    private static Float positiveOrNull(@Nullable Float value)
    {
        return VehiclePhysicsUnits.isUsablePositive(value) ? value : null;
    }

    @Nullable
    private static Float positiveOrNull(float value)
    {
        return VehiclePhysicsUnits.isUsablePositive(value) ? value : null;
    }
}
