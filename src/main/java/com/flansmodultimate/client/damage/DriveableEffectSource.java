package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.damage.DriveableDamageClass;
import com.flansmodultimate.common.driveables.damage.WreckLoad;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** A live driveable as the damage effects read it. */
record DriveableEffectSource(Driveable driveable, DriveableType type, DriveableDamageClass damageClass, List<DriveablePart> parts) implements DamageEffectSource
{
    @Nullable
    static DriveableEffectSource of(Driveable driveable)
    {
        DriveableType type = driveable.getConfigType();
        DriveableData data = driveable.getDriveableData();
        if (type == null || data == null)
            return null;
        List<DriveablePart> boxed = new ArrayList<>();
        for (DriveablePart part : data.getParts().values())
        {
            if (part.getBox() != null)
                boxed.add(part);
        }
        return new DriveableEffectSource(driveable, type, driveable.getDamageClass(), boxed);
    }

    @Override
    public Entity entity()
    {
        return driveable;
    }

    @Override
    public boolean isDrawn(DriveablePart part)
    {
        return driveable.isPartDrawn(part.getType());
    }

    @Override
    public Vec3 pointInPart(DriveablePart part, double fx, double fy, double fz)
    {
        return driveable.partPointToWorld(part, fx, fy, fz);
    }

    @Override
    public int wreckAge()
    {
        return driveable.getWreckAge();
    }

    @Override
    public WreckLoad load()
    {
        return driveable.getWreckLoad();
    }

    @Override
    public boolean rotorTurning()
    {
        return damageClass == DriveableDamageClass.HELICOPTER && driveable.isEngineActive() && driveable.isPartIntact(EnumDriveablePart.BLADES);
    }
}
