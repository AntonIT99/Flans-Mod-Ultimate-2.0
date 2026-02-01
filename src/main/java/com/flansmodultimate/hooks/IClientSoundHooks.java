package com.flansmodultimate.hooks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface IClientSoundHooks
{
    void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID);

    void cancelSound(UUID instanceUUID);
}
