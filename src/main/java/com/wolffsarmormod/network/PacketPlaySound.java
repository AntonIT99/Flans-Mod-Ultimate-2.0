package com.wolffsarmormod.network;

import com.wolffsarmormod.ArmorMod;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Objects;

@NoArgsConstructor
public class PacketPlaySound extends PacketBase
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

        RegistryObject<SoundEvent> event = ArmorMod.getSoundEvent(sound).orElse(null);
        if (event == null)
        {
            ArmorMod.log.debug("Could not play sound event {}", ResourceLocation.fromNamespaceAndPath(ArmorMod.FLANSMOD_ID, sound));
            return;
        }

        float volume = silenced ? ArmorMod.SOUND_VOLUME / 2F : ArmorMod.SOUND_VOLUME;
        float pitchBase = distort ? (1.0F / (level.random.nextFloat() * 0.4F + 0.8F)) : 1.0F;
        float pitch = pitchBase * (silenced ? 2.0F : 1.0F);

        level.playLocalSound(posX, posY, posZ, event.get(), SoundSource.PLAYERS, volume, pitch, false);
    }

    public static void sendSoundPacket(Player player, double range, String sound, boolean distort)
    {
        sendSoundPacket(player.getX(), player.getY(), player.getZ(), range, player.level().dimension(), sound, distort, false);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort)
    {
        sendSoundPacket(x, y, z, range, dimension, sound, distort, false);
    }

    public static void sendSoundPacket(double x, double y, double z, double range, ResourceKey<Level> dimension, String sound, boolean distort, boolean silenced)
    {
        PacketHandler.sendToAllAround(new PacketPlaySound(x, y, z, sound, distort, silenced), x, y, z, (float)range, dimension);
    }
}
