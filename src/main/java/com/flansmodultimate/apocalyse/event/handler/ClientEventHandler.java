package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.client.ApocalypseClientState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientEventHandler
{
    /** Runs at the end of every client tick. */
    public static void onClientTick()
    {
        if (Minecraft.getInstance().isPaused())
            return;
        ApocalypseClientState.tick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ApocalypseClientState.reset();
    }
}
