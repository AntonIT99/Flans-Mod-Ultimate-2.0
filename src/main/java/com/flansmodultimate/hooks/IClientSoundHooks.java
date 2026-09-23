package com.flansmodultimate.hooks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface IClientSoundHooks
{
    void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, @Nullable Player player);

    /** Keeps one named looping sound playing on an entity, replacing it when the sound changes and stopping it when there is none. */
    void setLoopingEntitySound(Entity source, String channel, @Nullable String sound, float range, float pitchRange);

    /** Plays a sound once, following the entity that emits it rather than staying where it started. */
    void playEntitySound(Entity source, @Nullable String sound, float range);

    void cancelSound(UUID instanceUUID);
}
