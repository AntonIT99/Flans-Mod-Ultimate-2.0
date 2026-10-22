/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.EntityTypePropertySupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Read-only AA and driveable property inspection for addons, without creating a live vehicle. */
@ApiStatus.Experimental
public final class FlansEntityTypes
{
    private FlansEntityTypes()
    {}

    /**
     * Inspects movement-engine audio independently of living-entity stat inheritance. Either side after content loading.
     * Mecha stomps retain their footstep cadence and are not engine loops.
     *
     * @param type
     *            selected driveable definition
     * @return engine sound, effective range and clip duration, or empty for missing audio, mechas and unsupported types
     */
    public static Optional<EngineSound> getEngineSound(IContentType type)
    {
        return EntityTypePropertySupport.engineSound(type);
    }

    /**
     * Inspects a vehicle/plane's idle engine loop, using its idle, start, then movement audio fallback. Either side after content loading.
     *
     * @param type
     *            selected definition
     * @return audio with effective engine range and measured or authored repeat duration, or empty for unsupported/missing audio
     */
    public static Optional<EngineSound> getEngineIdleSound(IContentType type)
    {
        return EntityTypePropertySupport.engineIdleSound(type);
    }

    /**
     * Evaluates the authored engine pitch curve using normalized movement instead of driveable throttle. Either side after content loading.
     *
     * @param type
     *            selected vehicle/plane definition, or an unsupported type to use the generic vehicle curve
     * @param movement
     *            fraction of normal movement speed, clamped to 0–1; reverse uses its positive magnitude
     * @return pitch multiplier clamped to the sound engine's 0.5–2 range
     */
    public static float getEnginePitch(IContentType type, float movement)
    {
        return EntityTypePropertySupport.enginePitch(type, movement);
    }

    /**
     * Read on either logical side after content loading; this does not mutate gameplay state.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @param secondary
     *            whether to inspect the secondary weapon bank
     * @return available type properties, or empty for an unsupported definition
     */
    public static Optional<EntityTypeProperties> getProperties(IContentType type, boolean secondary)
    {
        return getProperties(type, secondary, true);
    }

    /**
     * Reads the same type properties with explicit control of mounted gun resolution, on either side after content loading.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @param secondary
     *            whether to inspect the secondary weapon bank
     * @param mountedWeapons
     *            whether authored gun mounts supply weapon defaults; false uses the platform itself
     * @return available type properties, or empty for an unsupported definition
     */
    public static Optional<EntityTypeProperties> getProperties(IContentType type, boolean secondary, boolean mountedWeapons)
    {
        return EntityTypePropertySupport.read(type, secondary, mountedWeapons);
    }

    /**
     * Reads how the definition alternates between firing and reloading with the given ammunition, by the rules a placed
     * AA gun or a driveable bank follows. Either side after content loading; does not mutate gameplay state.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @param secondary
     *            whether to inspect the secondary weapon bank; ignored for AA guns
     * @param ammunition
     *            ammunition being fired, whose rounds per item and reload time multiplier apply; empty counts one round
     * @return the reload cycle, or empty for an unsupported definition or a bank without a weapon
     */
    public static Optional<ReloadCycle> getReloadCycle(IContentType type, boolean secondary, ItemStack ammunition)
    {
        return EntityTypePropertySupport.reloadCycle(type, secondary, ammunition);
    }

    /**
     * Describes the firing and reloading rhythm of a seat's own gun with this ammunition, as a crewed driveable keeps it:
     * the gun's cadence, and a reload after each item of ammunition, never shorter than the pause between shots. Either
     * side after content loading; does not mutate gameplay state.
     *
     * @param type
     *            vehicle, plane or mecha definition
     * @param seat
     *            seat index, as {@link WeaponMount#seat()}
     * @param ammunition
     *            ammunition being fired, whose rounds per item set the volleys; empty counts one round
     * @return the reload cycle, or empty for another definition or a seat without a gun
     */
    public static Optional<ReloadCycle> getSeatReloadCycle(IContentType type, int seat, ItemStack ammunition)
    {
        return EntityTypePropertySupport.seatReloadCycle(type, seat, ammunition);
    }

    /**
     * Lists what a definition can aim: every seat of a driveable, the driver first, or the single gun of an AA gun.
     * Either side after content loading.
     *
     * @param type
     *            selected AA gun, mecha, plane or vehicle definition
     * @return immutable mounts ordered by seat index; empty for an unsupported definition
     */
    public static List<WeaponMount> getMounts(IContentType type)
    {
        return EntityTypePropertySupport.mounts(type);
    }

    /** @return the definition of a vehicle, plane or mecha entity, or empty for any other entity; either side */
    public static Optional<IDriveableType> getDriveableType(Entity entity)
    {
        return entity instanceof Driveable driveable ? Optional.ofNullable(driveable.getConfigType()) : Optional.empty();
    }

    /**
     * @return the share of its hit points a driveable has left over all of its parts, from 0 to 1, or empty for any other
     *         entity; server thread
     */
    public static OptionalDouble getHealthShare(Entity entity)
    {
        return entity instanceof Driveable driveable ? driveable.getHealthShare() : OptionalDouble.empty();
    }

    /**
     * Repairs a driveable as a repair tool does, for someone who is no player: the damaged part nearest a point gets hit
     * points back. Server thread.
     *
     * @param from
     *            where the repair is made from, such as the repairer's eyes
     * @param amount
     *            hit points given back
     * @return whether a part was repaired; false for another entity, a driveable without damage or a destroyed one
     */
    public static boolean repair(Entity entity, Vec3 from, int amount)
    {
        return entity instanceof Driveable driveable && driveable.repairNearestPart(from, amount);
    }

    /**
     * Places a vehicle, plane or mecha of a definition in the level, as placing its item would, with every part worn down
     * to a share of its hit points. It has no fuel and no ammunition. Server thread.
     *
     * @param type
     *            a vehicle, plane or mecha definition
     * @param position
     *            where the driveable's origin goes, in world coordinates
     * @param yaw
     *            the heading it faces, in degrees as Minecraft measures yaw
     * @param health
     *            the share of its hit points every part keeps, from above 0 to 1; a part is never destroyed by it
     * @return the driveable, or empty for another definition, on a client, or where it does not fit
     */
    public static Optional<Entity> spawnDriveable(ServerLevel level, IContentType type, Vec3 position, float yaw, float health)
    {
        if (!(type.getItem() instanceof DriveableItem<?, ?> item))
            return Optional.empty();
        Driveable driveable = item.spawnDriveable(level, position.x, position.y, position.z, yaw, null, ItemStack.EMPTY);
        if (driveable == null)
            return Optional.empty();
        driveable.wearDown(health);
        return Optional.of(driveable);
    }
}
