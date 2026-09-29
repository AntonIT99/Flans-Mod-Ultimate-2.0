package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** One damageable part surface and the nearby points used to measure block cover. */
public record VehicleExplosionTarget(EnumDriveablePart part, EnumArmorFacing facing,
                                     Vec3 surfaceWorldPosition, double distanceMeters,
                                     List<Vec3> exposureSamples)
{
    public VehicleExplosionTarget
    {
        exposureSamples = List.copyOf(exposureSamples);
    }
}
