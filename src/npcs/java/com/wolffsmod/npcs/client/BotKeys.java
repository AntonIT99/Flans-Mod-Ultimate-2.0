package com.wolffsmod.npcs.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wolffsmod.npcs.network.BotOrderPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The module's key bindings. Client side. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotKeys
{
    private static final String CATEGORY = "key.categories.wolffsmodnpcs";

    /** Opens the squad order screen, which asks the server for the squads the player may order. */
    public static final KeyMapping SQUAD_ORDERS = new KeyMapping("key.wolffsmodnpcs.squad_orders", InputConstants.KEY_P, CATEGORY);

    /** Once a client tick, outside screens. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        while (SQUAD_ORDERS.consumeClick())
        {
            if (minecraft.player != null && minecraft.screen == null)
                NpcNetwork.sendToServer(BotOrderPacket.refresh());
        }
    }
}
