package com.wolffsarmormod.network;

import com.wolffsarmormod.ArmorMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public abstract class PacketBase
{
    /** Encode the packet into the buffer. */
    public abstract void encodeInto(FriendlyByteBuf buf);

    /** Decode the packet from the buffer. */
    public abstract void decodeInto(FriendlyByteBuf buf);

    /** Handle on server after decode. */
    public void handleServerSide(ServerPlayer player)
    {
        ArmorMod.log.warn("Recieved {} on server!", getClass().getSimpleName());
    }

    /** Handle on client after decode. */
    @OnlyIn(Dist.CLIENT)
    public void handleClientSide(Minecraft mc)
    {
        ArmorMod.log.warn("Recieved {} on client!", getClass().getSimpleName());
    }

    // UTF helpers (replace ByteBufUtils)
    public static void writeUTF(FriendlyByteBuf buf, String s)
    {
        buf.writeUtf(s);
    }

    public static String readUTF(FriendlyByteBuf buf)
    {
        return buf.readUtf();
    }
}
