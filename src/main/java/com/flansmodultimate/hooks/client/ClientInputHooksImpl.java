package com.flansmodultimate.hooks.client;

import com.flansmodultimate.hooks.IClientInputHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class ClientInputHooksImpl implements IClientInputHooks
{
    @Override
    public void swingIfLocalPlayer(Player player, InteractionHand hand)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == player) {
            player.swing(hand, true);
        }
    }
}
