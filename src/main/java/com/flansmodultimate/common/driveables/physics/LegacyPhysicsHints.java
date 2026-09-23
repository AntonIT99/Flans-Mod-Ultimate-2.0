package com.flansmodultimate.common.driveables.physics;

/**
 * The legacy fields the resolver needs in order to decide fallbacks, passed as a
 * plain record so the resolver never has to touch a {@code *Type} class and stays
 * unit testable without a Minecraft runtime.
 *
 * @param tank                  legacy {@code Tank}, used to infer a tracked drive layout
 * @param fourWheelDrive        legacy {@code FourWheelDrive}, used to infer all-wheel drive
 * @param maxNegativeThrottle   legacy reverse gate; zero still means "no reverse"
 * @param floatOnWater          legacy {@code FloatOnWater}, gates the marine draft override
 * @param newFlightControl      legacy experimental aircraft flag, outranked by a complete profile
 * @param useRealisticAcceleration legacy experimental ground flag, outranked by a complete profile
 */
public record LegacyPhysicsHints(
    boolean tank,
    boolean fourWheelDrive,
    float maxNegativeThrottle,
    boolean floatOnWater,
    boolean newFlightControl,
    boolean useRealisticAcceleration)
{
    public static final LegacyPhysicsHints EMPTY =
        new LegacyPhysicsHints(false, false, 0F, false, false, false);

    /** Whether the legacy configuration permits the vehicle to travel backwards at all. */
    public boolean allowsReverse()
    {
        return Float.isFinite(maxNegativeThrottle) && maxNegativeThrottle > 0F;
    }
}
