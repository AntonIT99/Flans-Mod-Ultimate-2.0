package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.damage.DriveableDamageClass;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.types.DriveableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * An entity wearing a part body, such as an NPC drawn as a tank, as the damage effects read it. Its transient wreck age follows
 * the wearer's observed death and resets on respawn or a different part body.
 */
record PartBodyEffectSource(Entity entity, PartBody body, DriveableType type, DriveableDamageClass damageClass, int wreckAge) implements DamageEffectSource
{
    static final int MAX_WRECK_EFFECT_TICKS = 1200;

    @Nullable
    static PartBodyEffectSource of(Entity entity, PartBody body)
    {
        return body.getType() instanceof DriveableType type ? new PartBodyEffectSource(entity, body, type, DriveableDamageClass.of(type), -1) : null;
    }

    @Override
    public List<DriveablePart> parts()
    {
        return body.enabledParts();
    }

    @Override
    public boolean isDrawn(DriveablePart part)
    {
        return body.isDrawn(part.getType()) && !DriveableDamageEffects.isCosmeticallyDetached(entity, part.getType());
    }

    @Override
    public Vec3 pointInPart(DriveablePart part, double fx, double fy, double fz)
    {
        return body.partPointToWorld(part, fx, fy, fz);
    }

    PartBodyEffectSource atAge(int age)
    {
        return new PartBodyEffectSource(entity, body, type, damageClass, age);
    }

    @Override
    public boolean rotorTurning()
    {
        return damageClass == DriveableDamageClass.HELICOPTER && entity.isAlive();
    }
}
