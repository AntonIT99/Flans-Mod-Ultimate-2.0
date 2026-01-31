package com.flansmodultimate.hooks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public interface IClientInputHooks
{
    void swingIfLocalPlayer(Player player, InteractionHand hand);
}
