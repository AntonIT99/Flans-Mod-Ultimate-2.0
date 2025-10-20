package com.wolffsarmormod.network;

import com.wolffsarmormod.ArmorMod;
import lombok.NoArgsConstructor;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketPlaySound extends PacketBase
{
    private float posX;
    private float posY;
    private float posZ;
    private String sound;
    private boolean distort;
    private boolean silenced;

    public PacketPlaySound(double x, double y, double z, String s)
    {
        this(x, y, z, s, false);
    }

    public PacketPlaySound(double x, double y, double z, String s, boolean distort)
    {
        this(x, y, z, s, distort, false);
    }

    public PacketPlaySound(double x, double y, double z, String s, boolean distort, boolean silenced)
    {
        posX = (float) x;
        posY = (float) y;
        posZ = (float) z;
        sound = s;
        this.distort = distort;
        this.silenced = silenced;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(posX);
        data.writeFloat(posY);
        data.writeFloat(posZ);
        writeUTF(data, sound);
        data.writeBoolean(distort);
        data.writeBoolean(silenced);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        posX = data.readFloat();
        posY = data.readFloat();
        posZ = data.readFloat();
        sound = readUTF(data);
        distort = data.readBoolean();
        silenced = data.readBoolean();
    }

    @Override
    public void handleClientSide(Minecraft mc)
    {
        if (mc.level == null)
            return;

        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath(ArmorMod.FLANSMOD_ID, sound));
        if (event == null)
            return;

        float volume = silenced ? 2.0F : 4.0F;
        RandomSource rng = mc.level.random;
        float pitchBase = distort ? (1.0F / (rng.nextFloat() * 0.4F + 0.8F)) : 1.0F;
        float pitch = pitchBase * (silenced ? 2.0F : 1.0F);

        mc.level.playLocalSound(posX, posY, posZ, event, SoundSource.PLAYERS, volume, pitch, false);
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
