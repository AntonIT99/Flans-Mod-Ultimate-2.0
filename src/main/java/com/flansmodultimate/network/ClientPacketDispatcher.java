package com.flansmodultimate.network;

import com.flansmodultimate.FlansMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class ClientPacketDispatcher
{
    private ClientPacketDispatcher()
    {
    }

    public static void dispatch(IClientPacket packet)
    {
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            packet.handleClientSide();
        }
        else
        {
            FlansMod.log.warn("Received client packet on server: {}", packet.getClass().getSimpleName());
        }
    }
}