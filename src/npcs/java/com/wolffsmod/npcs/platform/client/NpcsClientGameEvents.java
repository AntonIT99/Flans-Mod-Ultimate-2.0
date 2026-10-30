package com.wolffsmod.npcs.platform.client;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.client.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Minecraft;

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
        GunnerSeatInput.tick();
        SeatChangeInput.tick();
        NpcSeatMarkers.tick();
        NpcDebugLabels.tick();
    }

    @SubscribeEvent
    public static void interactionKey(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (event.isUseItem() && SeatBoardingInput.use(event.getHand()))
        {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        // The attack button fires the seat gun the player mans, so their hand neither swings nor strikes.
        Minecraft minecraft = Minecraft.getInstance();
        if (event.isAttack() && minecraft.player != null && GunnerSeatInput.mans(minecraft.player))
        {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            BotWaypoints.render(event.getPoseStack(), event.getCamera());
            NpcMuzzleFlashRenderer.render(event.getPoseStack(), event.getCamera(), partialTick);
            NpcDebugLabels.render(event.getPoseStack(), event.getCamera(), partialTick);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        BotWaypoints.clear();
        GunnerSeatInput.clear();
        NpcMuzzleFlashRenderer.clear();
        NpcDebugLabels.clear();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event)
    {
        ClientBenchmark.registerCommands(event.getDispatcher());
    }
}
