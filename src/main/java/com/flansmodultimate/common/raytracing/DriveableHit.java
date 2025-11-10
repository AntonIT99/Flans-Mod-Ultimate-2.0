package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.common.entity.Driveable;

import net.minecraft.world.entity.Entity;

public class DriveableHit extends BulletHit
{
    public Driveable driveable;
    //public EnumDriveablePart part;

    //TODO: Driveables
    public DriveableHit(Driveable d, /*EnumDriveablePart p,*/ float f)
    {
        super(f);
        //part = p;
        driveable = d;
    }

    @Override
    public Entity getEntity()
    {
        return driveable;
    }
}
