package com.flansmodultimate.platform.client;

import com.flansmodultimate.FlansMod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/** Version boundary for HUD layers: Forge GUI overlays or NeoForge GUI layers. Client-only. */
public final class HudOverlayPlatform
{
    private HudOverlayPlatform() {}

    /** A HUD layer drawn with the frame partial tick and the scaled screen size. */
    @FunctionalInterface
    public interface HudLayer
    {
        void render(GuiGraphics graphics, float partialTick, int width, int height);
    }

    /** Registers layers relative to vanilla ones; ids are in the Flan's Mod Ultimate namespace. */
    public static final class Registrar
    {
        private final RegisterGuiLayersEvent event;

        private Registrar(RegisterGuiLayersEvent event)
        {
            this.event = event;
        }

        /** Above the full-screen camera overlays, such as the pumpkin helmet. */
        public void aboveCameraOverlays(String id, HudLayer layer)
        {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, id(id), wrap(layer));
        }

        public void aboveArmorLevel(String id, HudLayer layer)
        {
            event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, id(id), wrap(layer));
        }

        public void aboveHotbar(String id, HudLayer layer)
        {
            event.registerAbove(VanillaGuiLayers.HOTBAR, id(id), wrap(layer));
        }

        private static ResourceLocation id(String path)
        {
            return ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, path);
        }
    }

    public static Registrar registrar(RegisterGuiLayersEvent event)
    {
        return new Registrar(event);
    }

    private static LayeredDraw.Layer wrap(HudLayer layer)
    {
        return (graphics, deltaTracker) -> layer.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(true),
            graphics.guiWidth(), graphics.guiHeight());
    }

    /** Whether survival HUD elements, such as health and armor, are drawn. */
    public static boolean drawsSurvivalElements()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer();
    }

    /**
     * Top of the next free row above the left status bars, for a layer drawn right after the armor bar.
     * NeoForge layers no longer share Forge's left-height counter, so this uses the vanilla layout.
     */
    public static int leftStatusRowTop(int screenHeight, boolean vanillaArmorVisible)
    {
        return screenHeight - (vanillaArmorVisible ? 59 : 49);
    }

    /** Reserves the row returned by {@link #leftStatusRowTop}; the vanilla layout needs no bookkeeping. */
    public static void claimLeftStatusRow() {}
}
