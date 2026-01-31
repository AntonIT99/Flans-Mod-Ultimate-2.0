package com.flansmodultimate.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class ClientPacketDispatcher
{
    public static void dispatch(IClientPacket packet)
    {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level != null && player != null)
            packet.handleClientSide(player, level);
    }
}
