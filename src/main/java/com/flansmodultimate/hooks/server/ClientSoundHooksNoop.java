package com.flansmodultimate.hooks.server;

import com.flansmodultimate.hooks.IClientSoundHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientSoundHooksNoop implements IClientSoundHooks
{
    public void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        /* no-op */
    }

    public void cancelSound(UUID instanceUUID)
    {
        /* no-op */
    }
}
