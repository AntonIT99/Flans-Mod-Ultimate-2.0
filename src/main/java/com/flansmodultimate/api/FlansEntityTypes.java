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
