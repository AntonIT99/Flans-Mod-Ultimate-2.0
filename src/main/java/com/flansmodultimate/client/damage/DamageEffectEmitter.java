package com.flansmodultimate.client.damage;

import com.flansmodultimate.client.particle.BlastPuffParticle;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.damage.DamageVisualCurves;
import com.flansmodultimate.common.driveables.damage.DriveableDamageClass;
import com.flansmodultimate.common.driveables.damage.PartDamageKind;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Gives off what each damaged part shows, every client tick, by its {@link PartDamageKind}.
 *
 * <p>
 * Nothing shows until a part has lost a good share of its hit points, then the effect grows with the damage. Smoke
 * rises from the upper half of the part box, as the model draws it, turret included; a fast mover lays its smoke and
 * vapour along the path it travelled this tick rather than in puffs one tick apart; a helicopter's rotor pushes its
 * smoke down. A wreck burns in the stages {@link DamageVisualCurves} gives it and raises a smoke column seen from
 * afar.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DamageEffectEmitter
{
    /** Speed in blocks per tick above which effects are spread along the path rather than left in one place. */
    private static final double TRAIL_SPEED = 0.25D;
    /** Speed in blocks per tick above which a damaged wing leaks a visible vapour trail. */
    private static final double VAPOUR_SPEED = 0.5D;
    /** How much a wreck's fire is held down by rain falling on it. */
    private static final float RAIN_DAMPING = 0.5F;
    /** Ages up to which a fresh wreck creaks as it settles. */
    private static final int CREAK_TICKS = 400;

    /** Everything one source's parts share for one tick. */
    private record Tick(DamageEffectSource source, RandomSource random, double density, Vec3 motion, double speed, boolean wreck, float wreckFire, float wreckSmoke, boolean raining)
    {}

    static void tick(DamageEffectSource source)
    {
        double density = DamageParticles.density();
        Entity entity = source.entity();
        Minecraft minecraft = Minecraft.getInstance();
        if (density <= 0D || minecraft.level == null || entity.level() != minecraft.level)
            return;
        ModClientConfig config = ModClientConfig.get();
        double reach = DamageParticles.LANDMARK_RANGE * (config == null ? 1D : config.particleDistanceScale);
        if (entity.position().distanceToSqr(minecraft.gameRenderer.getMainCamera().getPosition()) > reach * reach)
            return;
        int wreckAge = source.wreckAge();
        int lifetime = ModCommonConfig.driveableWreckLifetimeTicks();
        Vec3 motion = source.motion();
        boolean raining = entity.level().isRainingAt(BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ()));
        // A wreck burns as hard as the fuel it had, and less so in the rain.
        float fire = DamageVisualCurves.wreckFire(wreckAge, lifetime) * source.load().fireStrength() * (raining ? RAIN_DAMPING : 1F);
        Tick tick = new Tick(source, entity.level().random, density, motion, motion.length(), wreckAge >= 0, fire, DamageVisualCurves.wreckSmoke(wreckAge, lifetime), raining);
        for (DriveablePart part : source.parts())
            emitPart(tick, part);
        if (tick.wreck())
            emitWreck(tick);
    }

    private static void emitPart(Tick tick, DriveablePart part)
    {
        DamageEffectSource source = tick.source();
        PartDamageKind kind = source.type().getPartDamageKind(part.getType());
        if (kind == PartDamageKind.NONE)
            return;
        boolean drawn = source.isDrawn(part);
        // A part shot off leaves a smoking hole where it was, but nothing of a lost wheel or plate goes on burning.
        if (!drawn && !kind.burns())
            return;
        float fraction = part.getMaxHealth() > 0F ? part.getHealth() / part.getMaxHealth() : 1F;
        float damage = drawn ? DamageVisualCurves.damage(fraction) : 0.35F;
        float fire = Math.max(part.isOnFire() ? 1F : 0F, drawn ? wreckFire(tick, part, kind) : 0F);
        float smoke = Math.max(kind.smokes() ? damage : 0F, drawn ? wreckSmoke(tick, part, kind) : 0F);
        if (damage <= 0F && fire <= 0F && smoke <= 0F)
            return;
        double size = sizeOf(source, part);
        if (smoke > 0F)
            emitSmoke(tick, part, kind, damage, smoke, fire > 0F, size);
        if (fire > 0F)
            emitFire(tick, part, fire, size);
        if (drawn && damage > 0F)
            emitKind(tick, part, kind, damage, size);
    }

    /** @return whether the part is where a wreck burns hottest: its hull, engine or turret */
    private static boolean isWreckSeat(DriveablePart part, PartDamageKind kind)
    {
        return part.getType() == EnumDriveablePart.CORE || kind == PartDamageKind.ENGINE || kind == PartDamageKind.TURRET;
    }

    /** @return how strongly the part burns as part of a wreck, 0 while the source works */
    private static float wreckFire(Tick tick, DriveablePart part, PartDamageKind kind)
    {
        if (!tick.wreck() || !kind.burns() && kind != PartDamageKind.ENGINE)
            return 0F;
        return tick.wreckFire() * (isWreckSeat(part, kind) ? 0.8F : 0.35F);
    }

    /** @return how much the part smokes as part of a wreck, 0 while the source works */
    private static float wreckSmoke(Tick tick, DriveablePart part, PartDamageKind kind)
    {
        if (!tick.wreck() || !kind.smokes())
            return 0F;
        return tick.wreckSmoke() * (isWreckSeat(part, kind) ? 0.6F : 0.3F);
    }

    private static void emitSmoke(Tick tick, DriveablePart part, PartDamageKind kind, float damage, float smoke, boolean burning, double size)
    {
        RandomSource random = tick.random();
        double rate = smoke * 0.3D * size * tick.density() * (kind == PartDamageKind.BOILER ? 1.6D : 1D) * (kind == PartDamageKind.HULL ? 0.5D : 1D);
        int count = DamageParticles.count(Math.min(rate, 6D), random);
        if (count == 0)
            return;
        BlastPuffParticle.Look look = smokeLook(tick, kind, damage, burning);
        // Ships vent from deck level: their engine and boiler rooms sit inside the hull.
        boolean vents = tick.source().damageClass() == DriveableDamageClass.SHIP && (kind == PartDamageKind.ENGINE || kind == PartDamageKind.BOILER);
        float scale = (float) Math.sqrt(size);
        for (int i = 0; i < count; i++)
        {
            Vec3 at = tick.source().pointInPart(part, random.nextDouble(), vents ? 1D : 0.5D + random.nextDouble() * 0.5D, random.nextDouble());
            Vec3 velocity = new Vec3((random.nextDouble() - 0.5D) * 0.02D, 0.02D + random.nextDouble() * 0.03D + (vents ? 0.05D : 0D), (random.nextDouble() - 0.5D) * 0.02D);
            Vec3[] placed = trail(tick, at, velocity);
            DamageParticles.puff(look, placed[0], placed[1].x, placed[1].y, placed[1].z, (0.45F + random.nextFloat() * 0.35F) * scale * (1F + 0.4F * smoke),
                50 + random.nextInt(40) + Math.round(40F * smoke), false);
        }
    }

    private static BlastPuffParticle.Look smokeLook(Tick tick, PartDamageKind kind, float damage, boolean burning)
    {
        if (kind == PartDamageKind.BOILER)
            return DamageParticles.STEAM;
        if (tick.wreck() && tick.wreckFire() <= 0F)
            return DamageParticles.THIN_SMOKE;
        if (kind == PartDamageKind.ENGINE)
            return damage > 0.25F || burning ? DamageParticles.OIL_SMOKE : DamageParticles.GREY_SMOKE;
        return damage < 0.55F && !burning ? DamageParticles.GREY_SMOKE : DamageParticles.BLACK_SMOKE;
    }

    private static void emitFire(Tick tick, DriveablePart part, float fire, double size)
    {
        RandomSource random = tick.random();
        int count = DamageParticles.count(Math.min(fire * 0.45D * size * tick.density(), 5D), random);
        float scale = (float) Math.sqrt(size);
        for (int i = 0; i < count; i++)
        {
            Vec3 at = tick.source().pointInPart(part, random.nextDouble(), 0.3D + random.nextDouble() * 0.7D, random.nextDouble());
            Vec3 velocity = new Vec3((random.nextDouble() - 0.5D) * 0.02D, 0.03D + random.nextDouble() * 0.04D, (random.nextDouble() - 0.5D) * 0.02D);
            Vec3[] placed = trail(tick, at, velocity);
            if (quenched(tick, placed[0], scale))
                continue;
            DamageParticles.puff(DamageParticles.FIRE, placed[0], placed[1].x, placed[1].y, placed[1].z, (0.35F + random.nextFloat() * 0.35F) * scale, 10 + random.nextInt(10), false);
            if (random.nextFloat() < 0.6F)
                DamageParticles.puff(DamageParticles.BLACK_SMOKE, placed[0].add(0D, 0.4D * scale, 0D), placed[1].x, placed[1].y + 0.02D, placed[1].z, (0.5F + random.nextFloat() * 0.4F) * scale,
                    60 + random.nextInt(40), false);
        }
    }

    /**
     * Fire that would burn under water boils it off instead: a burst of steam and, now and then, a hiss.
     *
     * @return whether the flame was quenched and should not be drawn
     */
    private static boolean quenched(Tick tick, Vec3 at, float scale)
    {
        Entity entity = tick.source().entity();
        if (!entity.level().getFluidState(BlockPos.containing(at)).is(FluidTags.WATER))
            return false;
        RandomSource random = tick.random();
        double surface = waterSurface(entity.level(), BlockPos.containing(at));
        Vec3 steam = Double.isFinite(surface) ? new Vec3(at.x, surface + 0.1D, at.z) : at;
        DamageParticles.puff(DamageParticles.STEAM, steam, 0D, 0.08D, 0D, (0.5F + random.nextFloat() * 0.4F) * scale, 40 + random.nextInt(20), false);
        if (random.nextFloat() < 0.04F)
            entity.level().playLocalSound(steam.x, steam.y, steam.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4F, 0.8F + random.nextFloat() * 0.4F, false);
        return true;
    }

    /** The effects particular to a kind of part, beyond its smoke and fire. */
    private static void emitKind(Tick tick, DriveablePart part, PartDamageKind kind, float damage, double size)
    {
        DamageEffectSource source = tick.source();
        RandomSource random = tick.random();
        double density = tick.density();
        switch (kind)
        {
            case ARMOR -> sparksAt(source, part, DamageParticles.SPARK, damage * 0.05D * density, 3, random);
            case ELECTRONICS -> sparksAt(source, part, DamageParticles.ELECTRIC, damage * 0.12D * density, 3, random);
            case ROTOR -> sparksAt(source, part, DamageParticles.SPARK, damage * 0.1D * density, 2, random);
            case LIMB -> {
                sparksAt(source, part, DamageParticles.ELECTRIC, damage * 0.15D * density, 3, random);
                if (damage > 0.4F && random.nextDouble() < 0.12D * density)
                {
                    Vec3 at = source.pointInPart(part, random.nextDouble(), random.nextDouble(), random.nextDouble());
                    DamageParticles.puff(DamageParticles.HYDRAULIC, at, (random.nextDouble() - 0.5D) * 0.08D, -0.02D, (random.nextDouble() - 0.5D) * 0.08D, 0.25F + random.nextFloat() * 0.2F,
                        20 + random.nextInt(15), false);
                }
            }
            case RUNNING_GEAR -> {
                if (tick.speed() > 0.05D && damage > 0.3F)
                    sparksAt(source, part, DamageParticles.SPARK, damage * tick.speed() * 1.5D * density, 2, random);
            }
            case AIRFOIL, GASBAG -> emitVapour(tick, part, kind, damage);
            case HULL -> emitWater(tick, part, damage, size);
            default -> {
                // Structure, engines, turrets, boilers and superstructure show their smoke and fire alone.
            }
        }
    }

    private static void sparksAt(DamageEffectSource source, DriveablePart part, BlastPuffParticle.Look look, double rate, int perBurst, RandomSource random)
    {
        if (random.nextDouble() >= rate)
            return;
        Vec3 at = source.pointInPart(part, random.nextDouble(), random.nextDouble(), random.nextDouble());
        DamageParticles.sparks(look, at, perBurst, 0.12D, random);
    }

    /** A holed wing or gas cell leaking: a thin white trail behind a fast aircraft, a pale wisp otherwise. */
    private static void emitVapour(Tick tick, DriveablePart part, PartDamageKind kind, float damage)
    {
        RandomSource random = tick.random();
        boolean trail = kind == PartDamageKind.AIRFOIL && tick.speed() > VAPOUR_SPEED;
        if (damage < 0.15F)
            return;
        double rate = 0D;
        if (trail)
            rate = damage * 2.5D;
        else if (kind == PartDamageKind.GASBAG)
            rate = damage * 0.2D;
        int count = DamageParticles.count(Math.min(rate * tick.density(), 6D), random);
        for (int i = 0; i < count; i++)
        {
            Vec3 at = tick.source().pointInPart(part, random.nextDouble(), random.nextDouble(), random.nextDouble());
            Vec3[] placed = trail(tick, at, new Vec3(0D, trail ? 0D : 0.02D, 0D));
            DamageParticles.puff(DamageParticles.VAPOUR, placed[0], 0D, placed[1].y, 0D, 0.3F + random.nextFloat() * 0.25F, 30 + random.nextInt(20), false);
        }
    }

    /** A holed hull compartment at the waterline: bubbles where it is under water, and oil spreading on the surface. */
    private static void emitWater(Tick tick, DriveablePart part, float damage, double size)
    {
        RandomSource random = tick.random();
        Level level = tick.source().entity().level();
        if (damage < 0.3F && !tick.wreck())
            return;
        Vec3 at = tick.source().pointInPart(part, random.nextDouble(), random.nextDouble() * 0.4D, random.nextDouble());
        BlockPos pos = BlockPos.containing(at);
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER))
            return;
        int bubbles = DamageParticles.count((tick.wreck() ? 1.5D : damage) * size * 0.8D * tick.density(), random);
        for (int i = 0; i < bubbles; i++)
            DamageParticles.bubble(at.add((random.nextDouble() - 0.5D) * 0.6D, 0D, (random.nextDouble() - 0.5D) * 0.6D), 0.06D + random.nextDouble() * 0.05D);
        if (random.nextDouble() < (tick.wreck() ? 0.15D : damage * 0.05D) * tick.density())
        {
            double surface = waterSurface(level, pos);
            if (Double.isFinite(surface))
                DamageParticles.puff(DamageParticles.OIL_SLICK, new Vec3(at.x + (random.nextDouble() - 0.5D) * 2D, surface + 0.05D, at.z + (random.nextDouble() - 0.5D) * 2D), 0D, 0D, 0D,
                    1.0F + random.nextFloat() * (float) size, 160 + random.nextInt(80), false);
        }
    }

    /** The heavy fire and the smoke column of a wreck, from the top of its hull, and its crackle. */
    private static void emitWreck(Tick tick)
    {
        DamageEffectSource source = tick.source();
        DriveablePart core = source.core();
        Entity entity = source.entity();
        RandomSource random = tick.random();
        double size = core == null ? 1D : sizeOf(source, core);
        float scale = (float) Math.sqrt(size);
        Vec3 top = core == null ? entity.position().add(0D, entity.getBbHeight(), 0D) : source.pointInPart(core, 0.5D, 1D, 0.5D);
        boolean submerged = entity.level().getFluidState(BlockPos.containing(top)).is(FluidTags.WATER);
        if (submerged)
        {
            int bubbles = DamageParticles.count(3D * tick.density(), random);
            for (int i = 0; i < bubbles; i++)
                DamageParticles.bubble(top.add((random.nextDouble() - 0.5D) * 2D * scale, 0D, (random.nextDouble() - 0.5D) * 2D * scale), 0.1D);
            return;
        }
        int column = DamageParticles.count(tick.wreckSmoke() * 0.45D * scale * tick.density(), random);
        for (int i = 0; i < column; i++)
        {
            Vec3 at = top.add((random.nextDouble() - 0.5D) * scale, 0.2D, (random.nextDouble() - 0.5D) * scale);
            DamageParticles.puff(tick.wreckFire() > 0F ? DamageParticles.COLUMN_SMOKE : DamageParticles.THIN_SMOKE, at, (random.nextDouble() - 0.5D) * 0.02D, 0.05D + random.nextDouble() * 0.04D,
                (random.nextDouble() - 0.5D) * 0.02D, (1.0F + random.nextFloat() * 0.8F) * scale * (0.5F + 0.5F * tick.wreckSmoke()), 120 + random.nextInt(60), true);
        }
        int flames = DamageParticles.count(tick.wreckFire() * 0.8D * scale * tick.density(), random);
        for (int i = 0; i < flames; i++)
        {
            Vec3 at = top.add((random.nextDouble() - 0.5D) * scale, -0.2D, (random.nextDouble() - 0.5D) * scale);
            DamageParticles.puff(DamageParticles.FIRE, at, (random.nextDouble() - 0.5D) * 0.03D, 0.05D + random.nextDouble() * 0.06D, (random.nextDouble() - 0.5D) * 0.03D,
                (0.6F + random.nextFloat() * 0.6F) * scale, 12 + random.nextInt(12), true);
        }
        // Smouldering: the odd ember once the flames are out.
        if (tick.wreckFire() <= 0F && random.nextDouble() < 0.05D * tick.density())
            DamageParticles.puff(DamageParticles.SPARK, top, 0D, 0.08D, 0D, 0.1F, 25, false);
        // Rain falling into the fire boils off.
        if (tick.raining() && tick.wreckFire() > 0F && random.nextDouble() < 0.4D * tick.density())
            DamageParticles.puff(DamageParticles.STEAM, top.add((random.nextDouble() - 0.5D) * scale, 0.3D, (random.nextDouble() - 0.5D) * scale), 0D, 0.06D, 0D, 0.7F * scale, 40, false);
        // A fresh wreck groans as it settles and the metal cools.
        if (source.wreckAge() < CREAK_TICKS && random.nextFloat() < 0.012F)
            entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 0.35F, 0.4F + random.nextFloat() * 0.2F, false);
    }

    /**
     * Lays an effect along the path the source travelled this tick, and pushes it down under a turning rotor.
     *
     * @return the position and velocity to spawn with
     */
    private static Vec3[] trail(Tick tick, Vec3 at, Vec3 velocity)
    {
        Vec3 position = at;
        Vec3 drift = velocity;
        if (tick.speed() > TRAIL_SPEED)
        {
            position = at.subtract(tick.motion().scale(tick.random().nextDouble()));
            // What is left behind keeps a little of the speed it was shed at.
            drift = drift.add(tick.motion().scale(0.15D));
        }
        if (tick.source().rotorTurning())
        {
            Vec3 outward = position.subtract(tick.source().entity().position()).multiply(1D, 0D, 1D);
            outward = outward.lengthSqr() > 1.0E-6D ? outward.normalize().scale(0.05D) : Vec3.ZERO;
            drift = drift.add(outward.x, -0.12D, outward.z);
        }
        return new Vec3[]{position, drift};
    }

    /** @return a size factor for the part from its box as drawn: about 1 for a box a block and a half across */
    static double sizeOf(DamageEffectSource source, DriveablePart part)
    {
        Vec3 origin = source.pointInPart(part, 0D, 0D, 0D);
        double width = source.pointInPart(part, 1D, 0D, 0D).distanceTo(origin);
        double height = source.pointInPart(part, 0D, 1D, 0D).distanceTo(origin);
        double depth = source.pointInPart(part, 0D, 0D, 1D).distanceTo(origin);
        return Mth.clamp(Math.cbrt(Math.max(1.0E-3D, width * height * depth)) / 1.5D, 0.35D, 3.5D);
    }

    /** @return the height of the water surface above the position, or NaN when there is none within a few blocks */
    private static double waterSurface(Level level, BlockPos start)
    {
        BlockPos.MutableBlockPos cursor = start.mutable();
        double surface = Double.NaN;
        for (int i = 0; i < 6; i++)
        {
            FluidState fluid = level.getFluidState(cursor);
            if (!fluid.is(FluidTags.WATER))
                return surface;
            surface = cursor.getY() + fluid.getHeight(level, cursor);
            cursor.move(0, 1, 0);
        }
        return surface;
    }
}
