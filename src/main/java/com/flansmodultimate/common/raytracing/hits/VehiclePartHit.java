package com.flansmodultimate.common.raytracing.hits;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** The part and entry face of a round hitting a driveable or an entity wearing its part boxes. */
public interface VehiclePartHit
{
    Entity getEntity();

    EnumDriveablePart getPart();

    Vec3 getHitPosition();

    Vec3 getLocalProjectileDirection();

    EnumArmorFacing getFacing();
}
