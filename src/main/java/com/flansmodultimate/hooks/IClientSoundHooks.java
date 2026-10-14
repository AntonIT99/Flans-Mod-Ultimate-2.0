package com.flansmodultimate.hooks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface IClientSoundHooks
{
    // Preserve the existing sound transport contract, whose flags map directly to packet fields.
    @SuppressWarnings("java:S107")
    void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, @Nullable Entity emitter);

    /** Keeps one named looping sound playing on an entity, replacing it when the sound changes and stopping it when there is none. */
    void setLoopingEntitySound(Entity source, String channel, @Nullable String sound, float range, boolean varyPitch);

    /** Plays a sound once, following the entity that emits it rather than staying where it started. */
    void playEntitySound(Entity source, @Nullable String sound, float range);

    void cancelSound(UUID instanceUUID);

    /** Starts a following clip with optional entity-driven engine pitch. Server implementations retain the no-op sound path. */
    // Extend the existing packet bridge without changing its callers or dedicated-server no-op path.
    @SuppressWarnings("java:S107")
    default void playEngineClip(String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instance, @Nullable Entity emitter, boolean variablePitch)
    {
        playSound(sound, pos, range, distort, silenced, cancellable, instance, emitter);
    }
}
