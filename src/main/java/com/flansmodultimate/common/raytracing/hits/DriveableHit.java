package com.flansmodultimate.common.raytracing.hits;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.entity.Driveable;
import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Getter
public class DriveableHit extends BulletHit
{
    private final Driveable driveable;
    private final EnumDriveablePart part;
    /** World-space impact point. */
    private final Vec3 hitPosition;
    /** Impact point in the collision frame relevant to this part (turret-local for turret parts). */
    private final Vec3 localHitPosition;
    /** Full projectile motion in the same local frame; its magnitude remains blocks/tick. */
    private final Vec3 localProjectileDirection;
    /** Nominal collision-box outward normal in the same local frame. */
    private final Vec3 localOutwardNormal;
    private final EnumArmorFacing facing;

    public DriveableHit(Driveable d, EnumDriveablePart p, float f)
    {
        this(d, p, f, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, EnumArmorFacing.FRONT);
    }

    public DriveableHit(Driveable driveable, EnumDriveablePart part, float fraction,
                        Vec3 hitPosition, Vec3 localHitPosition, Vec3 localProjectileDirection,
                        Vec3 localOutwardNormal, EnumArmorFacing facing)
    {
        super(fraction);
        this.part = part;
        this.driveable = driveable;
        this.hitPosition = hitPosition == null ? Vec3.ZERO : hitPosition;
        this.localHitPosition = localHitPosition == null ? Vec3.ZERO : localHitPosition;
        this.localProjectileDirection = localProjectileDirection == null ? Vec3.ZERO : localProjectileDirection;
        this.localOutwardNormal = localOutwardNormal == null ? Vec3.ZERO : localOutwardNormal;
        this.facing = facing == null ? EnumArmorFacing.FRONT : facing;
    }

    @Override
    public Entity getEntity()
    {
        return driveable;
    }
}
