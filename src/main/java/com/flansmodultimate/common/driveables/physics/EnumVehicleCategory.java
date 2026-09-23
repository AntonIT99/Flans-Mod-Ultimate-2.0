package com.flansmodultimate.common.driveables.physics;

/**
 * Which coupled real-world profile a driveable type can qualify for.
 * Marine craft are ground vehicles in this repository: they are {@code VehicleType}
 * definitions carrying the legacy {@code Boat} keyword, so they share the ground
 * propulsion profile and only add the independent {@code RealDraftM} override.
 */
public enum EnumVehicleCategory
{
    GROUND,
    AIRCRAFT,
    /** Mechas share {@code DriveableType} but have no real-world profile of their own. */
    OTHER
}
