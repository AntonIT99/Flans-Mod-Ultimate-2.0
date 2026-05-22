package com.flansmodultimate.hooks.client;

import com.flansmodultimate.client.SoundHelper;
import com.flansmodultimate.hooks.IClientSoundHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientSoundHooksImpl implements IClientSoundHooks
{
    public void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, @Nullable Player player)
    {
        SoundHelper.playSound(sound, pos, range, distort, silenced, cancellable, instanceUUID, player == null || player != Minecraft.getInstance().player);
    }

    public void cancelSound(UUID instanceUUID)
    {
        SoundHelper.cancelSound(instanceUUID);
    }
}
