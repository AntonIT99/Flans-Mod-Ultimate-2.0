package com.flansmodultimate.hooks.server;

import com.flansmodultimate.hooks.IClientInputHooks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class ClientInputHooksNoop implements IClientInputHooks
{
    @Override
    public void swingIfLocalPlayer(Player player, InteractionHand hand) { /* no-op */ }
}
