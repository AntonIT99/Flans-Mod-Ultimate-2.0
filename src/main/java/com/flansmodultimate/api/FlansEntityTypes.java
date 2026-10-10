/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.types.EntityTypePropertySupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

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
}
