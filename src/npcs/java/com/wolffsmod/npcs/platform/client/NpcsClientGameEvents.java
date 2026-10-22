package com.wolffsmod.npcs.platform.client;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.client.BotKeys;
import com.wolffsmod.npcs.client.BotWaypoints;
import com.wolffsmod.npcs.client.ClientBenchmark;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Version boundary for the module's client game events: hands NeoForge's events to loader-neutral handlers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = NpcsMod.MOD_ID, value = Dist.CLIENT)
public final class NpcsClientGameEvents
{
    @SubscribeEvent
    public static void frameEnd(RenderFrameEvent.Post event)
    {
        ClientBenchmark.frame();
    }

    @SubscribeEvent
    public static void clientTickEnd(ClientTickEvent.Post event)
    {
        BotKeys.tick();
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            BotWaypoints.render(event.getPoseStack(), event.getCamera());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        BotWaypoints.clear();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event)
    {
        ClientBenchmark.registerCommands(event.getDispatcher());
    }
}
