package com.flansmodultimate.client.damage;

import com.flansmodultimate.api.driveables.AircraftDamage;
import com.flansmodultimate.api.driveables.AircraftMode;
import com.flansmodultimate.common.driveables.damage.AircraftDamageModel;
import com.flansmodultimate.common.driveables.damage.AircraftDamageReader;
import com.flansmodultimate.common.driveables.damage.DriveableDamageClass;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What a damaged aircraft gives away of its distress, the same for a driveable and an NPC drawn as an aircraft: a hit
 * engine coughing out puffs of oily smoke with a bang, and an aircraft going down screaming louder and higher the
 * faster it falls. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AircraftDistressEffects
{
    /** Blocks a tick a falling aircraft has to reach before the air screams past it. */
    private static final double SCREAM_SPEED = 0.6D;
    /** Blocks a tick at which the scream is at its loudest and highest. */
    private static final double FULL_SCREAM_SPEED = 2.4D;
    /** Blocks the scream carries. */
    private static final float SCREAM_RANGE = 96F;

    /** The scream of each aircraft coming down, while it plays. */
    private static final Map<Entity, FallScream> SCREAMS = new WeakHashMap<>();

    static void tick(DamageEffectSource source)
    {
        DriveableDamageClass damageClass = source.damageClass();
        if (!damageClass.flies() || !ModCommonConfig.realisticAircraftDamageEnabled())
            return;
        Entity entity = source.entity();
        AircraftMode mode = damageClass == DriveableDamageClass.HELICOPTER ? AircraftMode.ROTORCRAFT : AircraftMode.FIXED_WING;
        AircraftDamage damage = entity instanceof Plane plane ? plane.getAircraftDamage() : AircraftDamageReader.of(source.parts());
        if (entity instanceof Plane plane)
            mode = plane.getAircraftMode();
        boolean wreck = source.wreckAge() >= 0;
        if (!wreck && runs(entity))
            misfire(source, damage);
        boolean falling = wreck || AircraftDamageModel.crashMode(damage, mode).falling();
        if (falling && !entity.onGround())
            scream(entity);
    }

    /** @return whether the aircraft's engine is running: a driveable's when it is switched on, an NPC's while it lives */
    private static boolean runs(Entity entity)
    {
        if (entity instanceof Plane plane)
            return plane.isEngineActive();
        return entity instanceof LivingEntity living && living.isAlive();
    }

    /** A badly hit engine misfires now and then: a puff of black smoke and a bang, and its note drops out for a moment. */
    private static void misfire(DamageEffectSource source, AircraftDamage damage)
    {
        Entity entity = source.entity();
        RandomSource random = entity.level().random;
        if (random.nextFloat() >= AircraftDamageModel.misfireChance(damage))
            return;
        Vec3 at = entity.position().add(0D, entity.getBbHeight() * 0.6D, 0D);
        for (int i = 0; i < 3; i++)
            DamageParticles.puff(DamageParticles.OIL_SMOKE, at.add((random.nextDouble() - 0.5D) * 0.6D, 0D, (random.nextDouble() - 0.5D) * 0.6D), 0D, 0.03D, 0D,
                0.6F + random.nextFloat() * 0.4F, 40 + random.nextInt(20), false);
        if (random.nextFloat() < 0.6F)
            DamageParticles.puff(DamageParticles.FLASH, at, 0D, 0D, 0D, 0.4F, 3, false);
        entity.level().playLocalSound(at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 1.2F, 0.45F + random.nextFloat() * 0.15F, false);
    }

    private static void scream(Entity entity)
    {
        FallScream playing = SCREAMS.get(entity);
        if (playing != null && !playing.isStopped())
            return;
        FallScream scream = new FallScream(entity);
        SCREAMS.put(entity, scream);
        Minecraft.getInstance().getSoundManager().play(scream);
    }

    static void clear()
    {
        SCREAMS.clear();
    }

    /** The air screaming past an aircraft coming down, louder and higher the faster it falls, until it hits. */
    private static final class FallScream extends AbstractTickableSoundInstance
    {
        private final Entity aircraft;

        private FallScream(Entity aircraft)
        {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.aircraft = aircraft;
            looping = true;
            delay = 0;
            volume = 0.01F;
            attenuation = Attenuation.LINEAR;
            x = aircraft.getX();
            y = aircraft.getY();
            z = aircraft.getZ();
        }

        @Override
        public void tick()
        {
            if (aircraft.isRemoved() || aircraft.onGround() || aircraft.isInWater())
            {
                stop();
                return;
            }
            x = aircraft.getX();
            y = aircraft.getY();
            z = aircraft.getZ();
            double speed = new Vec3(aircraft.getX() - aircraft.xo, aircraft.getY() - aircraft.yo, aircraft.getZ() - aircraft.zo).length();
            float strength = (float) Mth.clamp((speed - SCREAM_SPEED) / (FULL_SCREAM_SPEED - SCREAM_SPEED), 0D, 1D);
            volume = strength * SCREAM_RANGE / 16F;
            pitch = 0.6F + 0.7F * strength;
        }

        @Override
        public boolean canStartSilent()
        {
            return true;
        }
    }
}
