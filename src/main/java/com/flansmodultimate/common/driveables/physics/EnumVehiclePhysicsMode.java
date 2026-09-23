package com.flansmodultimate.common.driveables.physics;

/** Which physics path a driveable type actually runs on after resolution. */
public enum EnumVehiclePhysicsMode
{
    /** No usable real-world data. Behaviour is exactly what it was before this system existed. */
    LEGACY("legacy"),
    /**
     * The coupled real-world profile for this category is incomplete or invalid,
     * but at least one independently usable parameter was authored and is active.
     */
    LEGACY_WITH_OVERRIDES("legacy_with_overrides"),
    /** The coupled real-world profile is complete; the new physics path owns propulsion. */
    REAL_WORLD_PROFILE("real_world_profile");

    private final String translationSuffix;

    EnumVehiclePhysicsMode(String translationSuffix)
    {
        this.translationSuffix = translationSuffix;
    }

    public String translationSuffix()
    {
        return translationSuffix;
    }
}
