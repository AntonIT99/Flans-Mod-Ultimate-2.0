package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientPlayerHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ClientPlayerHooksImpl implements IClientPlayerHooks
{
    @Override
    public boolean isLocalPlayer(Entity entity)
    {
        return Minecraft.getInstance().player == entity;
    }

    @Override
    public boolean isLocalPlayerWithinSqr(Entity entity, double distanceSq)
    {
        Player p = Minecraft.getInstance().player;
        return p != null && entity.distanceToSqr(p) <= distanceSq;
    }

    @Override
    public void swingIfLocalPlayer(Player player, InteractionHand hand)
    {
        if (Minecraft.getInstance().player == player)
            player.swing(hand, true);
    }
}
