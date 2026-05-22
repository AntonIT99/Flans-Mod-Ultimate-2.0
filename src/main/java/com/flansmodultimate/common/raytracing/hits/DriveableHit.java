package com.flansmodultimate.common.raytracing.hits;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.Driveable;
import lombok.Getter;

import net.minecraft.world.entity.Entity;

@Getter
public class DriveableHit extends BulletHit
{
    private final Driveable driveable;
    private final EnumDriveablePart part;

    public DriveableHit(Driveable d, EnumDriveablePart p, float f)
    {
        super(f);
        part = p;
        driveable = d;
    }

    @Override
    public Entity getEntity()
    {
        return driveable;
    }
}
