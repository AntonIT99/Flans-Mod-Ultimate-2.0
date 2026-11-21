package com.flansmodultimate.compat.sable;

import com.flansmodultimate.common.entity.Vehicle;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SableVehicleContact
{
    private SableVehicleContact() {}

    public static void apply(Vehicle vehicle, BlockPos support)
    {
        var deck = Sable.HELPER.getContaining(vehicle.level(), support);
        if (deck == null)
            return;
        // Sable carries the root during Entity.move, including when suspension holds the body above the wheels.
        ((EntityMovementExtension) vehicle).sable$setTrackingSubLevel(deck);
        float heading = vehicle.getEntityFacingYaw();
        Vec3 forward = Vec3.directionFromRotation(0F, heading);
        Vec3 rotated = deck.logicalPose().transformNormal(deck.lastPose().transformNormalInverse(forward));
        if (rotated.horizontalDistanceSqr() > 1.0E-8D)
        {
            float yawDelta = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-rotated.x, rotated.z)) - heading);
            vehicle.setOrientation(vehicle.getYaw() + yawDelta, vehicle.getPitch(), vehicle.getRoll());
        }
    }
}
