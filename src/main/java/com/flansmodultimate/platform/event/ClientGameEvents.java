package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.event.handler.ClientEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * NeoForge client game-bus subscribers for events whose type, phase or accessors differ between loaders.
 * They adapt the event and call the shared handlers.
 */
@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents
{
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTickStart(ClientTickEvent.Pre event)
    {
        ClientEventHandler.onClientTickStart();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        ClientEventHandler.onClientTick();
        com.flansmodultimate.apocalyse.event.handler.ClientEventHandler.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Post event)
    {
        ClientEventHandler.onRenderTick();
    }

    /** Crosshair pre: the hit marker replaces the vanilla crosshair when it should be hidden. */
    @SubscribeEvent
    public static void onPreRenderGuiLayer(RenderGuiLayerEvent.Pre event)
    {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && ClientEventHandler.hidesCrosshair())
        {
            ClientEventHandler.renderCrosshairHitMarker(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
            event.setCanceled(true);
        }
    }

    /** Crosshair post: the hit marker is drawn over the vanilla crosshair. */
    @SubscribeEvent
    public static void onPostRenderGuiLayer(RenderGuiLayerEvent.Post event)
    {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR))
            ClientEventHandler.renderCrosshairHitMarker(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }

    /** 1.20.1 changes the third-person camera distance with DriveableCameraMixin instead. */
    @SubscribeEvent
    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event)
    {
        if (!(event.getCamera().getEntity() instanceof Player player))
            return;

        var controllable = KeyInputHandler.resolveControllable(player);
        if (controllable == null)
            return;

        float requestedDistance = controllable.getCameraDistance();
        if (Float.isFinite(requestedDistance))
            event.setDistance(Mth.clamp(requestedDistance, 1F, 64F));
    }
}
