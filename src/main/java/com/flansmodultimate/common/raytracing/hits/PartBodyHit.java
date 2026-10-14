package com.flansmodultimate.common.raytracing.hits;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.driveables.parts.PartBody;
import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A round striking one part of an entity that wears the part boxes of a driveable definition. */
// Hits are ordered and compared by intersect time and entity only, as defined in BulletHit.
@SuppressWarnings("java:S2160")
@Getter
public class PartBodyHit extends BulletHit
{
    private final Entity entity;
    private final PartBody body;
    private final EnumDriveablePart part;
    /** World-space impact point. */
    private final Vec3 hitPosition;
    /** Full projectile motion in the frame armour facings are named in; its magnitude remains blocks/tick. */
    private final Vec3 localProjectileDirection;
    private final EnumArmorFacing facing;

    public PartBodyHit(Entity entity, PartBody body, EnumDriveablePart part, float fraction, Vec3 hitPosition, Vec3 localProjectileDirection, EnumArmorFacing facing)
    {
        super(fraction);
        this.entity = entity;
        this.body = body;
        this.part = part;
        this.hitPosition = hitPosition;
        this.localProjectileDirection = localProjectileDirection;
        this.facing = facing;
    }
}
