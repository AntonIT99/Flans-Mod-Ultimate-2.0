package com.flansmodultimate.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public interface IClientPacket extends IPacket
{
    /** Handle on client after decode. */
    @OnlyIn(Dist.CLIENT)
    void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level);
}
