/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import org.jetbrains.annotations.ApiStatus;

/**
 * The roles Flan's Mod Ultimate gives the sounds its own entities make, each with a server-configured radius in blocks.
 *
 * <p>
 * The radius decides both how far the sound is sent and how loud it is: a sound of 16 blocks or more carries its radius
 * divided by 16 as its volume, so it fades out linearly at its edge. An entity addon that plays the counterpart of one
 * of these sounds reads its radius through {@link FlansEntitySounds#getRange} and plays the sound with
 * {@link FlansEntitySounds#playClip}, and then sounds like the mod's own entities whatever the server configured.
 * </p>
 */
@ApiStatus.Experimental
public enum SoundRange
{
    /** A gun being fired, including an entity's mounted or held weapon ({@code gunFireSoundRange}). */
    GUN_FIRE,
    /** A gun's idle, warm-up and cocking sounds ({@code gunActionSoundRange}). */
    GUN_ACTION,
    /** A weapon being reloaded ({@code reloadSoundRange}). */
    RELOAD,
    /** A melee swing, hit or shield block ({@code meleeSoundRange}). */
    MELEE,
    /** A grenade or other throwable leaving the hand ({@code grenadeThrowSoundRange}). */
    THROW,
    /** A round striking an entity or a block ({@code bulletHitSoundRange}). */
    BULLET_HIT,
    /** An explosion going off ({@code explosionSoundRange}). */
    EXPLOSION,
    /** A driveable's engine, idle and movement sounds ({@code vehicleSoundRange}). */
    VEHICLE,
    /** A mecha tool or a rocket pack ({@code vehicleUtilitySoundRange}). */
    VEHICLE_UTILITY
}
