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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Restrained bow spray, water entry, ground dust, rotor downwash and machinery exhaust for driveables and NPC part bodies alike. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class VehicleAmbientEffects
{
    /** Ticks between the puffs of an engine's exhaust: a ship's funnel smokes steadily, a ground vehicle's pipe faster under power. */
    private static final int SHIP_EXHAUST_INTERVAL = 12;
    private static final int GROUND_EXHAUST_INTERVAL = 8;
    /** Blocks per tick over which a hull throws up dust from the ground it runs on, and the ticks between the puffs it throws. */
    private static final double DUST_SPEED = 0.05D;
    private static final int DUST_INTERVAL = 3;
    /** Blocks of the hull's width behind its middle that the dust comes off, where its wheels or tracks meet the ground. */
    private static final double DUST_TRAIL_SHARE = 0.45D;
    /** Blocks below a rotorcraft that its downwash still reaches, and the ticks between the rings it blows out. */
    private static final double DOWNWASH_REACH = 10D;
    private static final int DOWNWASH_INTERVAL = 4;
    /** Puffs in one ring of downwash, and the blocks a second of it carries them outward. */
    private static final int DOWNWASH_PUFFS = 3;
    private static final double DOWNWASH_SPEED = 0.18D;

    static void tick(DamageEffectSource source)
    {
        Entity entity = source.entity();
        if (source.wreckAge() >= 0 || DamageParticles.density() <= 0D)
            return;
        water(source);
        ground(source);
        downwash(source);
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

    /**
     * The dust a vehicle's wheels or tracks throw up off the ground behind it, in the colour of what it runs on. Only
     * something that drives on the ground raises any: a ship leaves its wake and an aircraft in the air has nothing under it.
     */
    private static void ground(DamageEffectSource source)
    {
        Entity entity = source.entity();
        if (!drivesOnGround(source.damageClass()) || entity.tickCount % DUST_INTERVAL != 0)
            return;
        Vec3 motion = source.motion();
        double speed = motion.horizontalDistance();
        if (speed < DUST_SPEED || !entity.onGround())
            return;
        BlockPos below = entity.blockPosition().below();
        Level level = entity.level();
        if (!level.hasChunkAt(below))
            return;
        BlockState ground = level.getBlockState(below);
        if (ground.isAir() || !level.getFluidState(below).isEmpty())
            return;
        Vec3 behind = entity.position().subtract(motion.normalize().scale(entity.getBbWidth() * DUST_TRAIL_SHARE));
        double share = Math.min(1D, speed / (DUST_SPEED * 6D));
        DamageParticles.puff(DamageParticles.groundDust(ground, level, below), behind, motion.x * -0.3D, 0.02D, motion.z * -0.3D, (float) (0.5D + share), 18 + (int) (share * 14D), false);
    }

    /** The ring of dust, or of ripples, a rotorcraft blows out from under itself while it hovers low over the ground. */
    private static void downwash(DamageEffectSource source)
    {
        Entity entity = source.entity();
        if (!source.rotorTurning() || entity.tickCount % DOWNWASH_INTERVAL != 0)
            return;
        Level level = entity.level();
        Vec3 at = entity.position();
        double surface = WaterEffects.surface(level, at, (int) DOWNWASH_REACH);
        if (Double.isFinite(surface) && at.y - surface <= DOWNWASH_REACH)
        {
            WaterEffects.splash(level, new Vec3(at.x, surface, at.z), 1.5F);
            return;
        }
        BlockPos floor = solidBelow(level, entity.blockPosition());
        if (floor == null)
            return;
        BlockState ground = level.getBlockState(floor);
        var look = DamageParticles.groundDust(ground, level, floor);
        double radius = Math.max(1.5D, entity.getBbWidth() * 0.4D);
        for (int puff = 0; puff < DOWNWASH_PUFFS; puff++)
        {
            double angle = Math.PI * 2D * puff / DOWNWASH_PUFFS + entity.tickCount * 0.3D;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            Vec3 point = new Vec3(at.x + dx * radius, floor.getY() + 1D, at.z + dz * radius);
            DamageParticles.puff(look, point, dx * DOWNWASH_SPEED, 0.01D, dz * DOWNWASH_SPEED, 1.1F, 22, false);
        }
    }

    /** @return the nearest solid block under the position within a rotor's reach, or null where the ground is further off than that */
    private static BlockPos solidBelow(Level level, BlockPos from)
    {
        for (int drop = 1; drop <= DOWNWASH_REACH; drop++)
        {
            BlockPos pos = from.below(drop);
            if (!level.hasChunkAt(pos))
                return null;
            if (!level.getBlockState(pos).isAir())
                return pos;
        }
        return null;
    }

    /**
     * Smoke from an engine room or boiler: a ship's funnel smokes whatever it does, while a ground vehicle's exhaust only
     * shows while it is actually pulling.
     */
    private static void exhaust(DamageEffectSource source)
    {
        Entity entity = source.entity();
        boolean ship = source.damageClass() == DriveableDamageClass.SHIP;
        if (!ship && !drivesOnGround(source.damageClass()))
            return;
        if (entity.tickCount % (ship ? SHIP_EXHAUST_INTERVAL : GROUND_EXHAUST_INTERVAL) != 0)
            return;
        if (!ship && source.motion().horizontalDistance() < DUST_SPEED)
            return;
        for (DriveablePart part : source.parts())
        {
            var kind = source.type().getPartDamageKind(part.getType());
            if (source.isDrawn(part) && (kind == PartDamageKind.ENGINE || kind == PartDamageKind.BOILER))
            {
                Vec3 funnel = source.pointInPart(part, 0.5D, 1D, 0.6D);
                if (entity.level().getFluidState(BlockPos.containing(funnel)).is(FluidTags.WATER))
                    return;
                if (ship)
                    ParticleHelper.spawnFromString(FlanParticles.FM_SHIP_SMOKE, funnel.x, funnel.y, funnel.z, 0D, 0.03D, 0D, 0.5F);
                else
                    DamageParticles.puff(DamageParticles.THIN_SMOKE, funnel, 0D, 0.03D, 0D, 0.4F, 16, false);
                return;
            }
        }
    }

    /** Whether the hull runs on the ground, and so raises dust and breathes through a pipe rather than a funnel. */
    private static boolean drivesOnGround(DriveableDamageClass damageClass)
    {
        return damageClass == DriveableDamageClass.TANK || damageClass == DriveableDamageClass.GROUND;
    }
}
