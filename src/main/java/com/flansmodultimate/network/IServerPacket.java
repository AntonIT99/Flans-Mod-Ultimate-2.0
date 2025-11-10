package com.flansmodultimate.network;

import net.minecraft.server.level.ServerPlayer;

public interface IServerPacket extends IPacket
{
    /** Handle on server after decode. */
    void handleServerSide(ServerPlayer player);
}
