/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.types.EntitySoundSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.OptionalInt;
import java.util.UUID;

/** Server-controlled Flan engine audio for external living entities. No client classes are loaded by this API. */
@ApiStatus.Experimental
public final class FlansEntitySounds
{
    private FlansEntitySounds()
    {}

    /**
     * Starts one clip, following the source on clients that track it. Call on the server thread and gate repeats by
     * {@link EngineSound#repeatTicks()}; the caller owns the timer and a distinct instance ID for each clip.
     *
     * @param source
     *            live emitting entity
     * @param sound
     *            engine audio, with a registered sound event ID and a finite positive radius in blocks
     * @param instance
     *            cancellation identity for this clip
     * @return whether audio was sent; false for invalid or silent sources/sound IDs; clients ignore missing audio events
     */
    public static boolean playEngineSound(Entity source, EngineSound sound, UUID instance)
    {
        return playEngineSound(source, sound, instance, false);
    }

    /**
     * Starts one sound event clip, including namespaced sounds from other mods or resource packs. Server thread only.
     *
     * @param source
     *            live emitting entity
     * @param sound
     *            audio description; unqualified paths select the flansmod namespace
     * @param instance
     *            cancellation identity
     * @param variablePitch
     *            whether clients follow {@link IEngineSoundSource#flansmodultimateEnginePitch()} during this clip
     * @return whether audio was sent
     */
    public static boolean playEngineSound(Entity source, EngineSound sound, UUID instance, boolean variablePitch)
    {
        return EntitySoundSupport.play(source, sound, instance, variablePitch);
    }

    /**
     * Reads the resolved Flan sound asset's measured length, independent of authored type timer overrides. Either side after loading.
     *
     * @param sound
     *            namespaced sound event
     * @return ticks at pitch 1, or empty for unknown and non-Flan assets
     */
    public static OptionalInt getSoundLength(ResourceLocation sound)
    {
        return EntitySoundSupport.length(sound);
    }

    /** @return current server engine sound radius in blocks; read either side after common config synchronization */
    public static float getDefaultEngineRange()
    {
        return EntitySoundSupport.defaultRange();
    }

    /**
     * Stops a previously started clip throughout the source's dimension, including listeners the source moved away from.
     * Server thread only. Entity removal also stops audio automatically on clients tracking the source.
     *
     * @param source
     *            emitting entity in the original dimension
     * @param instance
     *            identity passed to {@link #playEngineSound}
     */
    public static void stopEngineSound(Entity source, UUID instance)
    {
        EntitySoundSupport.stop(source, instance);
    }
}
