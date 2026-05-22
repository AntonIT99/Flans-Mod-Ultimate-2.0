package com.flansmodultimate.hooks.server;

import com.flansmodultimate.hooks.IClientPlayerHooks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ClientPlayerHooksNoop implements IClientPlayerHooks
{
    @Override
    public boolean isLocalPlayer(Entity entity)
    {
        return false;
    }

    @Override
    public boolean isLocalPlayerWithinSqr(Entity entity, double distanceSq)
    {
        return false;
    }

    @Override
    public void swingIfLocalPlayer(Player player, InteractionHand hand)
    {
        /* no-op */
    }
}
