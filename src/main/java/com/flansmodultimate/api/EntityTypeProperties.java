/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Read-only properties of an AA gun or driveable, available on either side after content loading.
 * Empty values have no suitable type property; consumers retain their own defaults.
 *
 * @param health
 *            maximum total hull HP, including resolved health scaling
 * @param targetRange
 *            authored AA targeting radius, in blocks
 * @param walkingSpeed
 *            unloaded mecha movement speed in blocks per tick, with a unit-speed engine
 * @param meleeReach
 *            mecha reach in blocks
 * @param worksUnderWater
 *            whether the driveable operates submerged
 * @param takesFallDamage
 *            whether the driveable's fall damage is enabled
 * @param shootDelay
 *            selected bank's interval in ticks; not its magazine reload time
 * @param sounds
 *            declared ambient and movement sounds, as resource identifiers
 * @param weapons
 *            selected bank's distinct weapon definitions, in muzzle order
 */
@ApiStatus.Experimental
public record EntityTypeProperties(OptionalDouble health, OptionalDouble targetRange, OptionalDouble walkingSpeed, OptionalDouble meleeReach, Optional<Boolean> worksUnderWater,
    Optional<Boolean> takesFallDamage, OptionalDouble shootDelay, Map<Sound, String> sounds, List<Weapon> weapons)
{
    /** Makes the returned collections immutable. */
    public EntityTypeProperties
    {
        sounds = Map.copyOf(sounds);
        weapons = List.copyOf(weapons);
    }

    /** Sound roles with a direct living-entity counterpart. */
    @ApiStatus.Experimental
    public enum Sound
    {
        /** Declared idle sound; an empty declaration does not replace a consumer's default. */
        IDLE,
        /** Engine movement sound, or the mecha's stomp sound. */
        STEP
    }

    /**
     * Weapon defaults before ammunition, stance or attachments modify them.
     *
     * @param name
     *            content shortname identifying this weapon
     * @param damage
     *            weapon damage, including platform damage multipliers
     * @param spread
     *            angular spread in the same units as {@link ProjectileParameters#spread()}
     * @param speed
     *            optional launch speed in blocks per tick; AA guns leave this to ammunition or the caller
     * @param projectiles
     *            pellets per weapon trigger, excluding separate muzzles
     * @param firingSound
     *            content firing sound; empty when none is declared
     */
    @ApiStatus.Experimental
    public record Weapon(String name, double damage, double spread, OptionalDouble speed, int projectiles, String firingSound)
    {}
}
