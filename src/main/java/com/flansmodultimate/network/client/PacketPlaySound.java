package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
public class PacketPlaySound implements IClientPacket
{
    private float posX;
    private float posY;
    private float posZ;
    private float range;
    private String sound;
    private boolean distort;
    private boolean silenced;
    private boolean cancellable;
    private UUID instanceUUID;

    public PacketPlaySound(double x, double y, double z, double range, @Nullable String s, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        posX = (float) x;
        posY = (float) y;
        posZ = (float) z;
        this.range = (float) range;
        sound = Objects.requireNonNullElse(s, StringUtils.EMPTY);
        this.distort = distort;
        this.silenced = silenced;
        this.cancellable = cancellable;
        this.instanceUUID = instanceUUID;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(posX);
        data.writeFloat(posY);
        data.writeFloat(posZ);
        data.writeFloat(range);
        data.writeUtf(sound);
        data.writeBoolean(distort);
        data.writeBoolean(silenced);
        data.writeBoolean(cancellable);
        data.writeUtf(instanceUUID.toString());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        posX = data.readFloat();
        posY = data.readFloat();
        posZ = data.readFloat();
        range = data.readFloat();
        sound = data.readUtf();
        distort = data.readBoolean();
        silenced = data.readBoolean();
        cancellable = data.readBoolean();
        instanceUUID = UUID.fromString(data.readUtf());
    }

    @Override
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        if (sound.isBlank())
            return;

        RegistryObject<SoundEvent> event = FlansMod.getSoundEvent(sound).orElse(null);
        if (event == null || event.getId() == null)
        {
            FlansMod.log.debug("Could not play sound event {}", ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, sound));
            return;
        }

        float volume = silenced ? range / 2F : range;
        float pitchBase = distort ? (1.0F / (level.random.nextFloat() * 0.4F + 0.8F)) : 1.0F;
        float pitch = pitchBase * (silenced ? 2.0F : 1.0F);

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(event.getId(), SoundSource.MASTER, volume, pitch, level.random, false, 0, SoundInstance.Attenuation.LINEAR, posX, posY, posZ, false);

        if (cancellable)
            ModClient.getCancellableSounds().put(instanceUUID, soundInstance);

        Minecraft.getInstance().getSoundManager().play(soundInstance);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        PacketHandler.sendToAllAround(new PacketPlaySound(x, y, z, range, sound, distort, silenced, cancellable, instanceUUID), x, y, z, (float) range, dimension);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        PacketHandler.sendToAllAround(new PacketPlaySound(x, y, z, range, sound, distort, silenced, false, UUID.randomUUID()), x, y, z, (float) range, dimension);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(x, y, z, range, dimension, sound, distort, false);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort, silenced, cancellable, instanceUUID);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort, silenced);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, silenced, cancellable, instanceUUID);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort, boolean silenced)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, silenced);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, false);
    }
}
