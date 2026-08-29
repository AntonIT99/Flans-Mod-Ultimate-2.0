package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;

/** Nearest damageable collision surface selected for one explosion and one vehicle. */
public record VehicleExplosionTarget(EnumDriveablePart part, EnumArmorFacing facing, double distanceMeters) {}
