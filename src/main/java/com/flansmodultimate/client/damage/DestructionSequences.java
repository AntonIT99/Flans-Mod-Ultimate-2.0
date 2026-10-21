package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.damage.DamageVisualCurves;
import com.flansmodultimate.common.driveables.damage.PartDamageKind;
import com.flansmodultimate.common.driveables.damage.PartPieces;
import com.flansmodultimate.common.driveables.damage.WreckLoad;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * How each kind of machine dies, played out over the ticks after it is destroyed.
 *
 * <p>
 * The blast that destroys a driveable is one moment; what follows depends on the machine. A tank's ammunition cooks
 * off in jets of flame from the turret with rounds popping, and now and then the turret goes up into the air. A car
 * burns, maybe losing a wheel. An aircraft's fuel goes up in its wings and a helicopter throws its rotor. A mecha
 * sparks as it falls and lands in a cloud of dust. A ship vents its boilers before its magazine goes up. Whether a
 * turret is thrown is drawn from the entity's identity, so every player sees the same death.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DestructionSequences
{
    /** Death sequences playing at once, so a battle cannot stack unbounded work. */
    private static final int MAX_ACTIVE = 16;
    /** Share of tank deaths in which the turret is thrown off. */
    private static final float TURRET_TOSS_CHANCE = 0.35F;
    private static final float WHEEL_LOSS_CHANCE = 0.4F;
    private static final float WING_LOSS_CHANCE = 0.3F;
    private static final int COOK_OFF_BASE_TICKS = 40;
    private static final int COOK_OFF_TICKS_PER_LOAD = 100;
    /** Ticks before the end of a cook-off at which the last jet of flame goes up. */
    private static final int COOK_OFF_TAIL = 25;
    /** Pieces at most a wreck blown apart throws off. */
    private static final int MAX_BREAK_UP_PIECES = 5;
    /** Two-block steps searched down for ground to scorch. */
    private static final int SCORCH_SEARCH_BLOCKS = 3;

    private static final List<Sequence> ACTIVE = new ArrayList<>();

    private static final class Sequence
    {
        private final DamageEffectSource source;
        private final RandomSource random;
        /** Draws from the entity's identity, so every client makes the same choices. */
        private final RandomSource fate;
        private final int duration;
        private final int magazineTick;
        /** Whether there is ammunition aboard to go off, and the type lets it. */
        private final boolean cookOff;
        private final WreckLoad load;
        private int age;
        private int nextJet;

        private Sequence(DamageEffectSource source)
        {
            this.source = source;
            random = source.entity().level().random;
            fate = RandomSource.create(source.entity().getUUID().getLeastSignificantBits() ^ source.entity().getUUID().getMostSignificantBits());
            load = source.load();
            cookOff = source.type().isCookOff() && load.cooksOff();
            duration = switch (source.damageClass())
            {
                // The more rounds aboard, the longer they go on going off.
                case TANK -> cookOff ? COOK_OFF_BASE_TICKS + Math.round(COOK_OFF_TICKS_PER_LOAD * load.ammunition()) : 30;
                case SHIP -> 200;
                case MECHA -> 50;
                case GROUND -> 60;
                default -> 30;
            };
            magazineTick = 30 + fate.nextInt(40);
        }
    }

    /** Plays the death of the source, which has just been destroyed. */
    static void start(DamageEffectSource source)
    {
        if (ACTIVE.size() >= MAX_ACTIVE)
            ACTIVE.remove(0);
        Sequence sequence = new Sequence(source);
        Vec3 centre = centre(source);
        DamageParticles.burst(centre, (float) Math.max(1D, coreSize(source)), sequence.random);
        // What burns on the ground leaves it black; an aircraft dying in the air scorches where it comes down instead.
        scorch(centre, Math.max(1.5F, source.entity().getBbWidth() * 0.8F));
        ACTIVE.add(sequence);
    }

    /** Blows a wreck apart: one last blast, and what is still on it thrown in every direction. */
    static void breakUp(DamageEffectSource source)
    {
        RandomSource random = source.entity().level().random;
        Vec3 centre = centre(source);
        float size = (float) Math.max(1D, coreSize(source));
        DamageParticles.burst(centre, 1.3F * size, random);
        explosionSound(source.entity(), 3F, 0.8F);
        int thrown = 0;
        for (DriveablePart part : source.parts())
        {
            PartDamageKind kind = source.type().getPartDamageKind(part.getType());
            if (thrown >= MAX_BREAK_UP_PIECES || !detaches(kind) || !source.isDrawn(part) || hasDrawnParent(source, part.getType()))
                continue;
            Vec3 at = source.pointInPart(part, 0.5D, 0.5D, 0.5D);
            if (throwOff(source, withChildren(source, part.getType()), at, 0.5D + random.nextDouble() * 0.4D, 0.35D, 1.5F, random.nextBoolean()))
                ++thrown;
        }
        for (int i = 0; i < 12; i++)
            DamageParticles.puff(DamageParticles.DUST, centre, (random.nextDouble() - 0.5D) * 0.3D, random.nextDouble() * 0.1D, (random.nextDouble() - 0.5D) * 0.3D, (1F + random.nextFloat()) * size,
                40 + random.nextInt(30), true);
    }

    /** Lays a scorch mark on the ground below the point, when there is ground close enough below to burn. */
    static void scorch(Vec3 point, float radius)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        for (int drop = 0; drop < SCORCH_SEARCH_BLOCKS; drop++)
        {
            double ground = DetachedPieces.groundBelow(level, point.subtract(0D, drop * 2D, 0D));
            if (Double.isFinite(ground))
            {
                DamageParticles.scorchGround(point.x, ground, point.z, radius);
                return;
            }
        }
    }

    /** @return whether a part the given part hangs from is still drawn, so that it goes with that one rather than alone */
    private static boolean hasDrawnParent(DamageEffectSource source, EnumDriveablePart type)
    {
        for (EnumDriveablePart parent : type.getParents())
        {
            DriveablePart state = parent == EnumDriveablePart.CORE ? null : part(source, parent);
            if (state != null && source.isDrawn(state))
                return true;
        }
        return false;
    }

    /** Advances every death being played. Client tick. */
    static void tick()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            ACTIVE.clear();
            return;
        }
        Iterator<Sequence> sequences = ACTIVE.iterator();
        while (sequences.hasNext())
        {
            Sequence sequence = sequences.next();
            Entity entity = sequence.source.entity();
            if (++sequence.age > sequence.duration || entity.level() != level)
            {
                sequences.remove();
                continue;
            }
            switch (sequence.source.damageClass())
            {
                case TANK -> tank(sequence);
                case GROUND -> ground(sequence);
                case PLANE -> plane(sequence);
                case HELICOPTER -> helicopter(sequence);
                case MECHA -> mecha(sequence);
                case SHIP -> ship(sequence);
            }
        }
    }

    // Tanks: ammunition cook-off

    private static void tank(Sequence sequence)
    {
        DamageEffectSource source = sequence.source;
        DriveablePart turret = drawnPart(source, EnumDriveablePart.TURRET);
        if (sequence.age == 4 && turret != null && sequence.fate.nextFloat() < turretTossChance(sequence))
        {
            Vec3 at = source.pointInPart(turret, 0.5D, 0.5D, 0.5D);
            if (throwOff(source, Set.of(EnumDriveablePart.TURRET), at, 0.85D + sequence.random.nextDouble() * 0.35D, 0.12D, 1.2F, true))
            {
                DamageParticles.burst(source.pointInPart(turret, 0.5D, 0D, 0.5D), 1.4F, sequence.random);
                explosionSound(source.entity(), 3F, 0.7F);
                turret = null;
            }
        }
        DriveablePart seat = turret != null ? turret : source.core();
        int end = sequence.duration - COOK_OFF_TAIL;
        if (!sequence.cookOff || seat == null || sequence.age >= end)
            return;
        // Jets of burning propellant out of the hatches and the turret ring.
        if (sequence.age >= sequence.nextJet)
        {
            sequence.nextJet = sequence.age + 3 + sequence.random.nextInt(4);
            Vec3 at = source.pointInPart(seat, 0.3D + sequence.random.nextDouble() * 0.4D, 1D, 0.3D + sequence.random.nextDouble() * 0.4D);
            int half = end / 2;
            float strength = sequence.age < half ? 1F : 1F - (sequence.age - half) / (float) Math.max(1, end - half);
            for (int i = 0; i < 6; i++)
                DamageParticles.puff(DamageParticles.FIRE, at, (sequence.random.nextDouble() - 0.5D) * 0.05D, (0.35D + sequence.random.nextDouble() * 0.3D) * strength,
                    (sequence.random.nextDouble() - 0.5D) * 0.05D, 0.45F + sequence.random.nextFloat() * 0.3F, 10 + sequence.random.nextInt(6), true);
            DamageParticles.sparks(DamageParticles.SPARK, at, 3, 0.2D, sequence.random);
        }
        // Rounds going off inside.
        if (sequence.random.nextFloat() < 0.03F + 0.06F * sequence.load.ammunition())
            pop(source, seat, sequence.random);
    }

    /** @return the chance the cook-off throws the turret: the type's own or the default, and less with fewer rounds aboard */
    private static float turretTossChance(Sequence sequence)
    {
        if (!sequence.cookOff)
            return 0F;
        float authored = sequence.source.type().getTurretTossChance();
        float base = authored >= 0F ? Math.min(authored, 1F) : TURRET_TOSS_CHANCE;
        return base * Math.min(1F, 0.3F + sequence.load.ammunition());
    }

    private static void pop(DamageEffectSource source, DriveablePart part, RandomSource random)
    {
        Vec3 at = source.pointInPart(part, random.nextDouble(), 0.4D + random.nextDouble() * 0.6D, random.nextDouble());
        DamageParticles.puff(DamageParticles.FLASH, at, 0D, 0D, 0D, 0.6F, 4, false);
        DamageParticles.sparks(DamageParticles.SPARK, at, 6, 0.3D, random);
        explosionSound(source.entity(), 0.45F, 1.4F + random.nextFloat() * 0.4F);
    }

    // Wheeled vehicles

    private static void ground(Sequence sequence)
    {
        DamageEffectSource source = sequence.source;
        if (sequence.age == 3 && sequence.fate.nextFloat() < WHEEL_LOSS_CHANCE)
        {
            List<DriveablePart> wheels = new ArrayList<>();
            for (DriveablePart part : source.parts())
            {
                if (EnumDriveablePart.isWheel(part.getType()) && source.isDrawn(part))
                    wheels.add(part);
            }
            if (!wheels.isEmpty())
            {
                DriveablePart wheel = wheels.get(sequence.fate.nextInt(wheels.size()));
                throwOff(source, Set.of(wheel.getType()), source.pointInPart(wheel, 0.5D, 0.5D, 0.5D), 0.45D, 0.25D, 1.5F, true);
            }
        }
        DriveablePart core = source.core();
        if (core != null && sequence.age < 40 && sequence.random.nextFloat() < 0.03F)
            pop(source, core, sequence.random);
    }

    // Aircraft

    private static void plane(Sequence sequence)
    {
        DamageEffectSource source = sequence.source;
        // The fuel in the wings goes up with the airframe, as much as there was of it.
        float fuel = sequence.load.fuel();
        if (sequence.age != 1 || fuel < 0.05F)
            return;
        for (EnumDriveablePart wing : List.of(EnumDriveablePart.LEFT_WING, EnumDriveablePart.RIGHT_WING))
        {
            DriveablePart part = drawnPart(source, wing);
            if (part == null)
                continue;
            Vec3 at = source.pointInPart(part, 0.5D, 0.5D, 0.5D);
            DamageParticles.burst(at, 0.4F + 0.6F * fuel, sequence.random);
            if (sequence.fate.nextFloat() < WING_LOSS_CHANCE)
                throwOff(source, withChildren(source, wing), at, 0.2D, 0.3D, 1F, true);
        }
    }

    private static void helicopter(Sequence sequence)
    {
        DamageEffectSource source = sequence.source;
        if (sequence.age != 2)
            return;
        DriveablePart blades = drawnPart(source, EnumDriveablePart.BLADES);
        if (blades != null)
        {
            Vec3 at = source.pointInPart(blades, 0.5D, 0.5D, 0.5D);
            // The rotor head lets go and the blades fly off still turning.
            throwOff(source, Set.of(EnumDriveablePart.BLADES), at, 0.45D, 0.35D, 3F, false);
            DamageParticles.sparks(DamageParticles.SPARK, at, 12, 0.4D, sequence.random);
        }
        DriveablePart tail = drawnPart(source, EnumDriveablePart.TAIL);
        if (tail != null && sequence.fate.nextFloat() < WING_LOSS_CHANCE)
            throwOff(source, withChildren(source, EnumDriveablePart.TAIL), source.pointInPart(tail, 0.5D, 0.5D, 0.5D), 0.2D, 0.25D, 1F, true);
    }

    // Mechas

    private static void mecha(Sequence sequence)
    {
        DamageEffectSource source = sequence.source;
        RandomSource random = sequence.random;
        if (sequence.age < 30 && sequence.age % 3 == 0)
        {
            DriveablePart part = randomDrawnPart(source, random);
            if (part != null)
                DamageParticles.sparks(DamageParticles.ELECTRIC, source.pointInPart(part, random.nextDouble(), random.nextDouble(), random.nextDouble()), 5, 0.25D, random);
        }
        if (sequence.age == DamageVisualCurves.COLLAPSE_TICKS)
        {
            // It hits the ground.
            Entity entity = source.entity();
            double radius = Math.max(1.5D, entity.getBbWidth());
            for (int i = 0; i < 14; i++)
            {
                double angle = i / 14D * Mth.TWO_PI;
                Vec3 at = entity.position().add(Math.cos(angle) * radius * 0.6D, 0.2D, Math.sin(angle) * radius * 0.6D);
                DamageParticles.puff(DamageParticles.DUST, at, Math.cos(angle) * 0.12D, 0.02D, Math.sin(angle) * 0.12D, 1.0F + random.nextFloat() * 0.6F, 40 + random.nextInt(20), true);
            }
            explosionSound(entity, 1.2F, 0.5F);
        }
    }

    // Ships

    private static void ship(Sequence sequence)
    {
        DamageEffectSource source = sequence.source;
        RandomSource random = sequence.random;
        if (sequence.age == 1)
            boilerHiss(source.entity());
        if (sequence.age < 60)
        {
            for (DriveablePart part : source.parts())
            {
                if (source.type().getPartDamageKind(part.getType()) == PartDamageKind.BOILER && random.nextFloat() < 0.5F)
                    DamageParticles.puff(DamageParticles.STEAM, source.pointInPart(part, random.nextDouble(), 1D, random.nextDouble()), 0D, 0.25D + random.nextDouble() * 0.15D, 0D,
                        1.0F + random.nextFloat() * 0.6F, 50 + random.nextInt(30), true);
            }
        }
        if (sequence.age == sequence.magazineTick && sequence.cookOff)
        {
            // The magazine goes up, if there was anything left in it.
            DriveablePart turret = firstDrawnTurret(source);
            DriveablePart seat = turret != null ? turret : source.core();
            if (seat == null)
                return;
            Vec3 at = source.pointInPart(seat, 0.5D, 0.5D, 0.5D);
            DamageParticles.burst(at, (float) Math.max(2D, coreSize(source)), random);
            explosionSound(source.entity(), 6F, 0.6F);
            if (turret != null)
                throwOff(source, Set.of(turret.getType()), at, 0.9D, 0.15D, 1F, true);
        }
    }

    // Shared

    /** Shows parts shot off a still-working source: a burst where each was, and each thrown clear with what it carries. */
    static void partsLost(DamageEffectSource source, List<EnumDriveablePart> lost)
    {
        RandomSource random = source.entity().level().random;
        for (Set<EnumDriveablePart> piece : PartPieces.group(lost))
        {
            EnumDriveablePart type = piece.iterator().next();
            DriveablePart part = part(source, type);
            if (part == null)
                continue;
            PartDamageKind kind = source.type().getPartDamageKind(type);
            Vec3 at = source.pointInPart(part, 0.5D, 0.5D, 0.5D);
            float size = (float) DamageEffectEmitter.sizeOf(source, part);
            switch (kind)
            {
                case ARMOR, RUNNING_GEAR -> DamageParticles.sparks(DamageParticles.SPARK, at, 10, 0.3D, random);
                case ELECTRONICS, LIMB -> DamageParticles.sparks(DamageParticles.ELECTRIC, at, 12, 0.3D, random);
                case BOILER -> DamageParticles.puff(DamageParticles.STEAM, at, 0D, 0.2D, 0D, 1.5F * size, 60, true);
                case NONE -> {
                    // Nothing to show.
                }
                default -> DamageParticles.burst(at, 0.6F * size, random);
            }
            if (detaches(kind))
            {
                boolean burning = part.isOnFire() || kind.burns() && random.nextFloat() < 0.5F;
                throwOff(source, piece, at, kind == PartDamageKind.TURRET ? 0.6D : 0.35D, 0.18D, 1F, burning);
            }
        }
    }

    private static boolean detaches(PartDamageKind kind)
    {
        return switch (kind)
        {
            case TURRET, RUNNING_GEAR, AIRFOIL, ROTOR, LIMB, ARMOR, ELECTRONICS, SUPERSTRUCTURE -> true;
            default -> false;
        };
    }

    /** @return the part and every part it carries that is still drawn, for a piece thrown off whole */
    private static Set<EnumDriveablePart> withChildren(DamageEffectSource source, EnumDriveablePart part)
    {
        Set<EnumDriveablePart> parts = EnumSet.of(part);
        for (EnumDriveablePart child : part.getChildren())
        {
            DriveablePart state = part(source, child);
            if (state == null || source.isDrawn(state))
                parts.add(child);
        }
        return parts;
    }

    /**
     * Throws parts off the source as one piece, and stops drawing them on it.
     *
     * @return whether a piece was thrown
     */
    private static boolean throwOff(DamageEffectSource source, Set<EnumDriveablePart> parts, Vec3 from, double up, double out, float spin, boolean burning)
    {
        Entity entity = source.entity();
        if (!(entity.level() instanceof ClientLevel level))
            return false;
        RandomSource random = level.random;
        Vec3 pivot = DetachedPieces.aboveGround(level, from);
        Vec3 velocity = DetachedPieces.throwVelocity(pivot, entity.position(), source.motion(), up, out, random);
        if (!DetachedPieces.spawn(entity, parts, pivot, velocity, DetachedPieces.randomSpin(random, spin), burning))
            return false;
        for (EnumDriveablePart part : parts)
            DriveableDamageEffects.detach(entity, part);
        return true;
    }

    @Nullable
    private static DriveablePart part(DamageEffectSource source, EnumDriveablePart type)
    {
        for (DriveablePart part : source.parts())
        {
            if (part.getType() == type)
                return part;
        }
        return null;
    }

    @Nullable
    private static DriveablePart drawnPart(DamageEffectSource source, EnumDriveablePart type)
    {
        DriveablePart part = part(source, type);
        return part != null && source.isDrawn(part) ? part : null;
    }

    @Nullable
    private static DriveablePart firstDrawnTurret(DamageEffectSource source)
    {
        for (DriveablePart part : source.parts())
        {
            if (source.type().getPartDamageKind(part.getType()) == PartDamageKind.TURRET && source.isDrawn(part))
                return part;
        }
        return null;
    }

    @Nullable
    private static DriveablePart randomDrawnPart(DamageEffectSource source, RandomSource random)
    {
        List<DriveablePart> drawn = new ArrayList<>();
        for (DriveablePart part : source.parts())
        {
            if (source.isDrawn(part))
                drawn.add(part);
        }
        return drawn.isEmpty() ? null : drawn.get(random.nextInt(drawn.size()));
    }

    private static Vec3 centre(DamageEffectSource source)
    {
        DriveablePart core = source.core();
        Entity entity = source.entity();
        return core == null ? entity.position().add(0D, entity.getBbHeight() * 0.5D, 0D) : source.pointInPart(core, 0.5D, 0.5D, 0.5D);
    }

    private static double coreSize(DamageEffectSource source)
    {
        DriveablePart core = source.core();
        return core == null ? 1D : DamageEffectEmitter.sizeOf(source, core);
    }

    /** The roar of a ship's boilers venting as it dies. */
    private static void boilerHiss(Entity entity)
    {
        entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 2.5F, 0.6F, false);
    }

    private static void explosionSound(Entity entity, float volume, float pitch)
    {
        entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, volume, pitch, false);
    }

    /** Forgets every death being played, as when leaving a world. */
    static void clear()
    {
        ACTIVE.clear();
    }
}
