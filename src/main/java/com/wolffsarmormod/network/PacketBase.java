package com.wolffsarmormod.network;

import com.wolffsarmormod.ArmorMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public abstract class PacketBase
{
    /** Encode the packet into the buffer. */
    public abstract void encodeInto(FriendlyByteBuf data);

    /** Decode the packet from the buffer. */
    public abstract void decodeInto(FriendlyByteBuf data);

    /** Handle on server after decode. */
    public void handleServerSide(ServerPlayer player)
    {
        ArmorMod.log.warn("Recieved {} from {} on server!", getClass().getSimpleName(), player.getDisplayName().getString());
    }

    /** Handle on client after decode. */
    @OnlyIn(Dist.CLIENT)
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        ArmorMod.log.warn("Recieved {} on client!", getClass().getSimpleName());
    }
}
