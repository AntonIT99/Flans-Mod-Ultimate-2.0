package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.damage.DriveableDamageClass;
import com.flansmodultimate.common.driveables.damage.WreckLoad;
import com.flansmodultimate.common.types.DriveableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Something that is drawn as a driveable and whose parts can be damaged: a live driveable, or an entity wearing a
 * part body, such as an NPC drawn as a tank. The damage effects only read it.
 */
interface DamageEffectSource
{
    Entity entity();

    DriveableType type();

    DriveableDamageClass damageClass();

    /** @return the parts with boxes, as they stand */
    List<DriveablePart> parts();

    /** @return whether the model still draws the part */
    boolean isDrawn(DriveablePart part);

    /** @return the world position of a point given as shares of the part box's extent */
    Vec3 pointInPart(DriveablePart part, double fx, double fy, double fz);

    /** @return ticks since the source was wrecked, or -1 while it works */
    int wreckAge();

    /** @return whether rotor blades are turning overhead, pushing smoke down */
    boolean rotorTurning();

    /** @return what was aboard when the source was destroyed */
    default WreckLoad load()
    {
        return WreckLoad.UNKNOWN;
    }

    /** @return world displacement over the last tick */
    default Vec3 motion()
    {
        Entity entity = entity();
        return new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
    }

    /** @return the part the source is centred on, for death effects, or null when it has none */
    @Nullable
    default DriveablePart core()
    {
        for (DriveablePart part : parts())
        {
            if (part.getType() == EnumDriveablePart.CORE)
                return part;
        }
        return null;
    }
}
