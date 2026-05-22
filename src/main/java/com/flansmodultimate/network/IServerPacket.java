package com.flansmodultimate.network;

import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

public interface IServerPacket extends IPacket
{
    void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel world);
}