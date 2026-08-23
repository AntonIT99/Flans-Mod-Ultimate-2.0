package com.flansmodultimate.network;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IClientPacket extends IPacket
{
    /** Handle on client after decode. */
    void handleClientSide(@NotNull Player player, @NotNull Level level);
}
