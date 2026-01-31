package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketRequestPlaySound;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundHelper
{
    private static final List<PendingSound> pendingSounds = new ArrayList<>();

    private static final class PendingSound
    {
        int ticksLeft;
        Runnable action;

        PendingSound(int ticksLeft, Runnable action)
        {
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    public static void tickClient()
    {
        if (pendingSounds.isEmpty())
            return;

        for (PendingSound p : pendingSounds)
        {
            if (--p.ticksLeft <= 0)
            {
                pendingSounds.remove(p);
                p.action.run();
            }
        }
    }

    public static void playLocalAndBroadcast(@Nullable String sound, Vec3 pos, float range)
    {
        if (StringUtils.isBlank(sound))
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null)
            return;

        RegistryObject<SoundEvent> event = FlansMod.getSoundEvent(sound).orElse(null);
        if (event == null || event.getId() == null)
        {
            FlansMod.log.debug("Could not play sound event {}", ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, sound));
            return;
        }

        float volume = getVolumeFromRange(range, false);

        level.playLocalSound(pos.x, pos.y, pos.z, event.get(), SoundSource.PLAYERS, volume, 1F, false);
        PacketHandler.sendToServer(new PacketRequestPlaySound(pos, range, sound));
    }

    public static void playDelayedLocalAndBroadcast(@Nullable String sound, Vec3 pos, float range, int delayTicks)
    {
        pendingSounds.add(new PendingSound(delayTicks, () -> playLocalAndBroadcast(sound, pos, range)));
    }

    public static float getVolumeFromRange(float range, boolean silenced)
    {
        return silenced ? range / 32F : range / 16F;
    }
}
