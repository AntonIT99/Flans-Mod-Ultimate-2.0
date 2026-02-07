package com.flansmodultimate.network;

import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface IServerPacket extends IPacket
{
    /** Handle on server after decode. */
    void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level);
}
