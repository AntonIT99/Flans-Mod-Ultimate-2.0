package com.flansmodultimate.client.damage;

import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.client.particle.WaterEffects;
import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.damage.DriveableDamageClass;
import com.flansmodultimate.common.driveables.damage.PartDamageKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Restrained bow spray, water entry and machinery exhaust for driveables and NPC part bodies alike. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class VehicleAmbientEffects
{
    static void tick(DamageEffectSource source)
    {
        Entity entity = source.entity();
        if (source.wreckAge() >= 0 || DamageParticles.density() <= 0D)
            return;
        water(source);
        if (source.damageClass() == DriveableDamageClass.SHIP && entity.tickCount % 12 == 0)
            exhaust(source);
    }

    private static void water(DamageEffectSource source)
    {
        Entity entity = source.entity();
        Vec3 motion = source.motion();
        double speed = motion.horizontalDistance();
        Vec3 at = entity.position();
        double surface = WaterEffects.surface(entity.level(), at, 2);
        if (Double.isFinite(surface) && motion.lengthSqr() > 0.0016D)
        {
            BlockPos previous = BlockPos.containing(entity.xo, entity.yo, entity.zo);
            if (entity.level().hasChunkAt(previous) && !entity.level().getFluidState(previous).is(FluidTags.WATER) && motion.length() > 0.15D)
                WaterEffects.splash(entity.level(), at, (float) Math.min(3D, motion.length() * 3D));
            if (entity.tickCount % 6 == 0 && speed > 0.08D)
            {
                Vec3 bow = at.add(motion.normalize().scale(Math.min(3D, entity.getBbWidth() * 0.5D)));
                ParticleHelper.spawnFromString(FlanParticles.FM_WATER_MINI, bow.x, surface, bow.z, motion.x * 0.2D, 0.08D, motion.z * 0.2D, (float) Math.min(1.5D, speed * 2D));
            }
        }
    }

    private static void exhaust(DamageEffectSource source)
    {
        Entity entity = source.entity();
        for (DriveablePart part : source.parts())
        {
            var kind = source.type().getPartDamageKind(part.getType());
            if (source.isDrawn(part) && (kind == PartDamageKind.ENGINE || kind == PartDamageKind.BOILER))
            {
                Vec3 funnel = source.pointInPart(part, 0.5D, 1D, 0.6D);
                if (!entity.level().getFluidState(BlockPos.containing(funnel)).is(FluidTags.WATER))
                    ParticleHelper.spawnFromString(FlanParticles.FM_SHIP_SMOKE, funnel.x, funnel.y, funnel.z, 0D, 0.03D, 0D, 0.5F);
                return;
            }
        }
    }
}
