package com.flansmodultimate.client;

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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundHelper
{
    private static final Map<UUID, SoundInstance> cancellableSounds = new HashMap<>();

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

        cancellableSounds.values().removeIf(soundInstance -> !Minecraft.getInstance().getSoundManager().isActive(soundInstance));
    }

    public static void playSoundLocalAndBroadcast(@Nullable String sound, Vec3 pos, float range)
    {
        if (StringUtils.isBlank(sound))
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null)
            return;

        getSoundEvent(sound).ifPresent(soundEvent -> {
            float volume = getVolumeFromRange(range, false);

            level.playLocalSound(pos.x, pos.y, pos.z, soundEvent, SoundSource.PLAYERS, volume, 1F, false);
            PacketHandler.sendToServer(new PacketRequestPlaySound(pos, range, sound));
        });
    }

    public static void playSoundDelayedLocalAndBroadcast(@Nullable String sound, Vec3 pos, float range, int delayTicks)
    {
        pendingSounds.add(new PendingSound(delayTicks, () -> playSoundLocalAndBroadcast(sound, pos, range)));
    }

    public static void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        if (StringUtils.isBlank(sound))
            return;

        getSoundEvent(sound).ifPresent(soundEvent -> {
            RandomSource r = RandomSource.create(instanceUUID.getMostSignificantBits() ^ instanceUUID.getLeastSignificantBits());
            float volume = SoundHelper.getVolumeFromRange(range, silenced);
            float pitchBase = distort ? (1.0F / (r.nextFloat() * 0.4F + 0.8F)) : 1.0F;
            float pitch = pitchBase * (silenced ? 2.0F : 1.0F);

            SimpleSoundInstance soundInstance = new SimpleSoundInstance(soundEvent.getLocation(), SoundSource.PLAYERS, volume, pitch, r, false, 0, SoundInstance.Attenuation.LINEAR, pos.x, pos.y, pos.z, false);

            if (cancellable)
                cancellableSounds.put(instanceUUID, soundInstance);

            Minecraft.getInstance().getSoundManager().play(soundInstance);
        });
    }

    public static void cancelSound(UUID instanceUUID)
    {
        if (cancellableSounds.containsKey(instanceUUID))
        {
            Minecraft.getInstance().getSoundManager().stop(cancellableSounds.get(instanceUUID));
            cancellableSounds.remove(instanceUUID);
        }
    }

    public static Optional<SoundEvent> getSoundEvent(@Nullable String sound)
    {
        if (StringUtils.isBlank(sound))
            return Optional.empty();

        RegistryObject<SoundEvent> soundEvent = FlansMod.getSoundEvent(sound).orElse(null);
        if (soundEvent == null || soundEvent.getId() == null)
        {
            FlansMod.log.debug("Could not play sound event {}", ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, sound));
            return Optional.empty();
        }
        return Optional.of(soundEvent.get());
    }

    public static float getVolumeFromRange(float range, boolean silenced)
    {
        return silenced ? range / 32F : range / 16F;
    }
}
