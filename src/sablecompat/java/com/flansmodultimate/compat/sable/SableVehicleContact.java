package com.flansmodultimate.compat.sable;

import com.flansmodultimate.common.entity.Driveable;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SableVehicleContact
{
    private SableVehicleContact() {}

    public static Vec3 release(Driveable driveable, BlockPos support)
    {
        var deck = Sable.HELPER.getContaining(driveable.level(), support);
        if (deck == null || deck.isRemoved())
            return Vec3.ZERO;
        // Entity.move would otherwise carry the root a second time on the departure tick.
        if (Sable.HELPER.getTrackingSubLevel(driveable) == deck)
            ((EntityMovementExtension) driveable).sable$setTrackingSubLevel(null);
        Vec3 position = driveable.position();
        return deck.logicalPose().transformPosition(deck.lastPose().transformPositionInverse(position)).subtract(position);
    }

    public static void apply(Driveable vehicle, BlockPos support)
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
