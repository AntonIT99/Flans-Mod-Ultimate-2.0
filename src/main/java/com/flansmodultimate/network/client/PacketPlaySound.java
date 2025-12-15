package com.flansmodultimate.network.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

@NoArgsConstructor
public class PacketPlaySound implements IClientPacket
{
    private float posX;
    private float posY;
    private float posZ;
    private String sound;
    private boolean distort;
    private boolean silenced;

    public PacketPlaySound(double x, double y, double z, @Nullable String s)
    {
        this(x, y, z, s, false);
    }

    public PacketPlaySound(double x, double y, double z, @Nullable String s, boolean distort)
    {
        this(x, y, z, s, distort, false);
    }

    public PacketPlaySound(double x, double y, double z, @Nullable String s, boolean distort, boolean silenced)
    {
        posX = (float) x;
        posY = (float) y;
        posZ = (float) z;
        sound = Objects.requireNonNullElse(s, StringUtils.EMPTY);
        this.distort = distort;
        this.silenced = silenced;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(posX);
        data.writeFloat(posY);
        data.writeFloat(posZ);
        data.writeUtf(sound);
        data.writeBoolean(distort);
        data.writeBoolean(silenced);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        posX = data.readFloat();
        posY = data.readFloat();
        posZ = data.readFloat();
        sound = data.readUtf();
        distort = data.readBoolean();
        silenced = data.readBoolean();
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        if (sound.isBlank())
            return;

        RegistryObject<SoundEvent> event = FlansMod.getSoundEvent(sound).orElse(null);
        if (event == null)
        {
            FlansMod.log.debug("Could not play sound event {}", ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, sound));
            return;
        }

        float volume = silenced ? FlansMod.SOUND_VOLUME / 2F : FlansMod.SOUND_VOLUME;
        float pitchBase = distort ? (1.0F / (level.random.nextFloat() * 0.4F + 0.8F)) : 1.0F;
        float pitch = pitchBase * (silenced ? 2.0F : 1.0F);

        level.playLocalSound(posX, posY, posZ, event.get(), SoundSource.PLAYERS, volume, pitch, false);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        PacketHandler.sendToAllAround(new PacketPlaySound(x, y, z, sound, distort, silenced), x, y, z, (float)range, dimension);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(x, y, z, range, dimension, sound, distort, false);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort);
    }

    public static void sendSoundPacket(Vec3 position, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        sendSoundPacket(position.x, position.y, position.z, range, dimension, sound, distort, silenced);
    }

    public static void sendSoundPacket(Entity entity, double range, String sound, boolean distort)
    {
        sendSoundPacket(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension(), sound, distort, false);
    }
}
