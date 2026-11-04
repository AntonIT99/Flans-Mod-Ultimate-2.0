/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import com.flansmodultimate.common.driveables.damage.AircraftCrashEffects;
import com.flansmodultimate.common.driveables.damage.AircraftCrashPhysics;
import com.flansmodultimate.common.driveables.damage.AircraftDamageModel;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * How damaged aircraft fly and fall, the same for Flan's driveables and for anything else drawn as an aircraft, such as
 * the NPCs of the NPC module. Every method but the effects is a pure function, safe on either side and any thread.
 */
@ApiStatus.Experimental
@SuppressWarnings("unused") // Read by the NPC module and other mods, which IntelliJ does not count from here.
public final class FlansAircraftCrashes
{
    private FlansAircraftCrashes()
    {}

    /**
     * @return whether the server has realistic aircraft damage on: progressive damage, and crippled or dead aircraft
     *         coming down as airframes. Off, aircraft keep the all-or-nothing behaviour. Readable on either side;
     *         clients see the server's setting once the config has synchronised.
     */
    public static boolean realisticDamageEnabled()
    {
        return ModCommonConfig.realisticAircraftDamageEnabled();
    }

    /**
     * @param damage
     *            what is left of the aircraft
     * @param mode
     *            how it flies
     * @return how it comes down with what it has lost, or {@link CrashMode#NONE} while it still flies
     */
    public static CrashMode crashMode(AircraftDamage damage, AircraftMode mode)
    {
        return AircraftDamageModel.crashMode(damage, mode);
    }

    /**
     * @param entity
     *            any entity
     * @return how a Flan's aircraft comes down with what it has lost, {@link CrashMode#NONE} while it can still be flown,
     *         and empty for anything that is not a Flan's aircraft; readable on either side
     */
    public static Optional<CrashMode> crashMode(Entity entity)
    {
        if (!(entity instanceof Plane plane))
            return Optional.empty();
        return Optional.of(AircraftDamageModel.crashMode(plane.getAircraftDamage(), plane.getAircraftMode()));
    }

    /** @return how the aircraft comes down once nobody flies it any more; never {@link CrashMode#NONE} */
    public static CrashMode wreckMode(AircraftDamage damage, AircraftMode mode)
    {
        return AircraftDamageModel.wreckMode(damage, mode);
    }

    /**
     * @return the share of its lift a falling aircraft still has, to pass to {@link #fall}: its wings', or its main
     *         rotor's for a rotorcraft
     */
    public static float fallLift(AircraftDamage damage, AircraftMode mode)
    {
        return mode == AircraftMode.ROTORCRAFT ? AircraftDamageModel.rotorLift(damage) : AircraftDamageModel.liftFraction(damage);
    }

    /** @return the share of its power the damaged engine still makes */
    public static float thrustFraction(AircraftDamage damage)
    {
        return AircraftDamageModel.thrustFraction(damage);
    }

    /** @return how hard the damaged airframe shakes, from 0 to 1 */
    public static float vibration(AircraftDamage damage, AircraftMode mode)
    {
        return AircraftDamageModel.vibration(damage, mode);
    }

    /**
     * Advances a falling aircraft by one tick: it keeps its nose into the wind while it has a tail, its remaining wings
     * bend its path, and what it has lost rolls or spins it.
     *
     * @param motion
     *            its motion last tick; start with {@link CrashMotion#of}
     * @param attitude
     *            where it points now, in world space
     * @param mode
     *            how it comes down
     * @param lift
     *            the share of its lift it still has, as {@link #fallLift} gives it
     * @param gravity
     *            the pull of gravity in blocks a tick squared, 0.08 in the Overworld
     * @param mirrored
     *            which way it spins when nothing it has lost decides that: keep it fixed for one aircraft
     * @return its velocity and its turn this tick; apply the turn with {@link #rotate}
     */
    public static CrashMotion fall(CrashMotion motion, DriveableAttitude attitude, CrashMode mode, float lift, double gravity, boolean mirrored)
    {
        return AircraftCrashPhysics.step(motion, attitude, mode, lift, gravity, mirrored);
    }

    /**
     * Turns an attitude by a world-space rotation vector, as {@link CrashMotion#rotation()} gives one.
     *
     * @return the turned attitude
     */
    public static DriveableAttitude rotate(DriveableAttitude attitude, Vec3 rotation)
    {
        return AircraftCrashPhysics.rotate(attitude, rotation);
    }

    /**
     * Shows an aircraft going in: a fireball spread over its size, burning debris, a scorch mark and the noise of it, to
     * everyone near enough to see it. Does no damage of its own. Server side.
     *
     * @param level
     *            where it crashed
     * @param position
     *            where it struck, in world coordinates
     * @param radius
     *            the size of the fireball in blocks, about one and a half times the aircraft's own
     * @param water
     *            whether it went into water, which throws up a splash instead of scorching the ground
     */
    public static void crash(ServerLevel level, Vec3 position, float radius, boolean water)
    {
        AircraftCrashEffects.crash(level, position, radius, water, -1);
    }
}
