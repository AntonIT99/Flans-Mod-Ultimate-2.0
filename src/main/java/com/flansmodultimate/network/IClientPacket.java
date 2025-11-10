package com.flansmodultimate.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public interface IClientPacket extends IPacket
{
    /** Handle on client after decode. */
    @OnlyIn(Dist.CLIENT)
    void handleClientSide(LocalPlayer player, ClientLevel level);
}
