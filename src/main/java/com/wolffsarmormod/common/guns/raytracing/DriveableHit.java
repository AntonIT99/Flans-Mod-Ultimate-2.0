package com.wolffsarmormod.common.guns.raytracing;

import com.wolffsarmormod.common.entity.Driveable;

import net.minecraft.world.entity.Entity;

public class DriveableHit extends BulletHit
{
    public Driveable driveable;
    //public EnumDriveablePart part;

    //TODO: Drivealbes
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
