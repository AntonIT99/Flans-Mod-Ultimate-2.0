package com.flansmodultimate.hooks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface IClientPlayerHooks
{
    boolean isLocalPlayer(Entity entity);

    boolean isLocalPlayerWithinSqr(Entity entity, double distanceSq);

    void swingIfLocalPlayer(Player player, InteractionHand hand);
}
