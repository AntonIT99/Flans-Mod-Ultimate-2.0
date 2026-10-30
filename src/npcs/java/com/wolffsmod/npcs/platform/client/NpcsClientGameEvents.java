package com.wolffsmod.npcs.platform.client;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.client.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;

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
        if (event.phase != TickEvent.Phase.END)
            return;
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
            BotWaypoints.render(event.getPoseStack(), event.getCamera());
            NpcMuzzleFlashRenderer.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
            NpcDebugLabels.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
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
