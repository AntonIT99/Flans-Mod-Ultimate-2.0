package com.flansmodultimate.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.sound.EntitySoundInstance;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketRequestPlaySound;
import com.flansmodultimate.platform.registry.RegistryEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundHelper
{
    private static final Map<UUID, SoundInstance> cancellableSounds = new HashMap<>();

    private static final List<PendingSound> pendingSounds = new ArrayList<>();

    /** Looping sounds attached to an entity, keyed by entity id and channel name. */
    private static final Map<String, EntitySoundInstance> loopingEntitySounds = new HashMap<>();

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
        // Collect first: running an action while iterating could queue another delayed sound.
        List<PendingSound> dueSounds = new ArrayList<>();
        Iterator<PendingSound> iterator = pendingSounds.iterator();
        while (iterator.hasNext())
        {
            PendingSound pendingSound = iterator.next();
            if (--pendingSound.ticksLeft <= 0)
            {
                iterator.remove();
                dueSounds.add(pendingSound);
            }
        }
        dueSounds.forEach(pendingSound -> pendingSound.action.run());

        cancellableSounds.values().removeIf(soundInstance -> !Minecraft.getInstance().getSoundManager().isActive(soundInstance));

        // The sound engine drops a sound once it becomes inaudible, and refuses to start one that is
        // already out of range. Forgetting those lets the owner start the loop again as it comes back
        // into earshot, instead of staying silent for good after driving away once.
        loopingEntitySounds.values().removeIf(soundInstance -> soundInstance.isStopped()
            || soundInstance.isSourceGone()
            || !Minecraft.getInstance().getSoundManager().isActive(soundInstance));
    }

    /**
     * Keeps one named looping sound playing on an entity, started and stopped as the sound changes.
     * <p>
     * Calling this every tick with the same sound keeps the running sound untouched, so the sound
     * engine loops it without a gap. Passing a different sound replaces it, and passing none stops it.
     *
     * @param source  the entity the sound follows
     * @param channel names the looping sound on that entity, so an entity can run several at once
     * @param sound   the sound to loop, or {@code null} or blank to stop whatever is playing
     * @param range   how far the sound carries
     */
    public static void setLoopingEntitySound(Entity source, String channel, @Nullable String sound, float range, float pitchRange)
    {
        String key = source.getId() + ":" + channel;
        EntitySoundInstance playing = loopingEntitySounds.get(key);

        if (StringUtils.isBlank(sound) || source.isRemoved())
        {
            if (playing != null)
            {
                playing.requestStop();
                Minecraft.getInstance().getSoundManager().stop(playing);
                loopingEntitySounds.remove(key);
            }
            return;
        }

        if (playing != null && playing.isSound(sound) && !playing.isStopped())
        {
            playing.setPitchRange(pitchRange);
            return;
        }

        if (playing != null)
        {
            playing.requestStop();
            Minecraft.getInstance().getSoundManager().stop(playing);
            loopingEntitySounds.remove(key);
        }

        playEntitySound(source, sound, range, true, pitchRange).ifPresent(soundInstance -> loopingEntitySounds.put(key, soundInstance));
    }

    /** Plays a sound once, following the entity that emits it rather than staying where it started. */
    public static void playEntitySound(Entity source, @Nullable String sound, float range)
    {
        playEntitySound(source, sound, range, false, 0F);
    }

    private static Optional<EntitySoundInstance> playEntitySound(Entity source, @Nullable String sound, float range,
                                                                  boolean looping, float pitchRange)
    {
        return getSoundEvent(sound).map(soundEvent -> {
            EntitySoundInstance soundInstance = new EntitySoundInstance(soundEvent, source, range, looping, pitchRange);
            Minecraft.getInstance().getSoundManager().play(soundInstance);
            return soundInstance;
        });
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

    public static void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, boolean relativeToListener)
    {
        if (StringUtils.isBlank(sound))
            return;

        getSoundEvent(sound).ifPresent(soundEvent -> {
            RandomSource r = RandomSource.create(instanceUUID.getMostSignificantBits() ^ instanceUUID.getLeastSignificantBits());
            float volume = SoundHelper.getVolumeFromRange(range, silenced);
            float pitchBase = distort ? (1.0F / (r.nextFloat() * 0.4F + 0.8F)) : 1.0F;
            float pitch = pitchBase * (silenced ? 2.0F : 1.0F);
            Vec3 soundPosition = relativeToListener ? Vec3.ZERO : pos;

            SimpleSoundInstance soundInstance = new SimpleSoundInstance(soundEvent.getLocation(), SoundSource.PLAYERS, volume, pitch, r, false, 0, SoundInstance.Attenuation.LINEAR, soundPosition.x, soundPosition.y, soundPosition.z, relativeToListener);

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

        RegistryEntry<SoundEvent> soundEvent = FlansMod.getSoundEvent(sound).orElse(null);
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
