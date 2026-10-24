package com.wolffsmod.npcs.platform.client;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.client.BotKeys;
import com.wolffsmod.npcs.client.BotWaypoints;
import com.wolffsmod.npcs.client.ClientBenchmark;
import com.wolffsmod.npcs.client.NpcMuzzleFlashRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Version boundary for the module's client game events: hands Forge's events to loader-neutral handlers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = NpcsMod.MOD_ID, value = Dist.CLIENT)
public final class NpcsClientGameEvents
{
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            ClientBenchmark.frame();
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            BotKeys.tick();
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            BotWaypoints.render(event.getPoseStack(), event.getCamera());
            NpcMuzzleFlashRenderer.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        BotWaypoints.clear();
        NpcMuzzleFlashRenderer.clear();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event)
    {
        ClientBenchmark.registerCommands(event.getDispatcher());
    }
}
