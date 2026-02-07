package com.flansmodultimate.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IClientPacket extends IPacket
{
    /** Handle on client after decode. */
    @OnlyIn(Dist.CLIENT)
    void handleClientSide(@NotNull Player player, @NotNull Level level);
}
