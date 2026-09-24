package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.event.handler.ClientEventHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge client game-bus subscribers for events whose type, phase or accessors differ between loaders.
 * They adapt the event and call the shared handlers.
 */
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientGameEvents
{
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            ClientEventHandler.onClientTickStart();
            return;
        }
        ClientEventHandler.onClientTick();
        com.flansmodultimate.apocalyse.event.handler.ClientEventHandler.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            ClientEventHandler.onRenderTick();
    }

    /** Crosshair pre: the hit marker replaces the vanilla crosshair when it should be hidden. */
    @SubscribeEvent
    public static void onPreRenderGuiOverlay(RenderGuiOverlayEvent.Pre event)
    {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && ClientEventHandler.hidesCrosshair())
        {
            ClientEventHandler.renderCrosshairHitMarker(event.getGuiGraphics(), event.getPartialTick());
            event.setCanceled(true);
        }
    }

    /** Crosshair post: the hit marker is drawn over the vanilla crosshair. */
    @SubscribeEvent
    public static void onPostRenderGuiOverlay(RenderGuiOverlayEvent.Post event)
    {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type())
            ClientEventHandler.renderCrosshairHitMarker(event.getGuiGraphics(), event.getPartialTick());
    }
}
