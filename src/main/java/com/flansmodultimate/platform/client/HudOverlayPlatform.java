package com.flansmodultimate.platform.client;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;

/** Version boundary for HUD layers: Forge GUI overlays or NeoForge GUI layers. Client-only. */
public final class HudOverlayPlatform
{
    private static final int STATUS_ROW_HEIGHT = 10;

    /** The Forge GUI of the overlay being rendered, for left status-bar bookkeeping. */
    @Nullable
    private static ForgeGui currentGui;

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
        private final RegisterGuiOverlaysEvent event;

        private Registrar(RegisterGuiOverlaysEvent event)
        {
            this.event = event;
        }

        /** Above the full-screen camera overlays, such as the pumpkin helmet. */
        public void aboveCameraOverlays(String id, HudLayer layer)
        {
            event.registerAbove(VanillaGuiOverlay.HELMET.id(), id, wrap(layer));
        }

        public void aboveArmorLevel(String id, HudLayer layer)
        {
            event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(), id, wrap(layer));
        }

        public void aboveHotbar(String id, HudLayer layer)
        {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), id, wrap(layer));
        }
    }

    public static Registrar registrar(RegisterGuiOverlaysEvent event)
    {
        return new Registrar(event);
    }

    private static IGuiOverlay wrap(HudLayer layer)
    {
        return (gui, graphics, partialTick, width, height) -> {
            currentGui = gui;
            layer.render(graphics, partialTick, width, height);
        };
    }

    /** Whether survival HUD elements, such as health and armor, are drawn. */
    public static boolean drawsSurvivalElements()
    {
        return currentGui != null && currentGui.shouldDrawSurvivalElements();
    }

    /** Top of the next free row above the left status bars, for a layer drawn right after the armor bar. */
    public static int leftStatusRowTop(int screenHeight, boolean vanillaArmorVisible)
    {
        int leftHeight = currentGui != null ? currentGui.leftHeight : 49;
        return screenHeight - leftHeight + (vanillaArmorVisible ? 0 : STATUS_ROW_HEIGHT);
    }

    /** Reserves the row returned by {@link #leftStatusRowTop} so later status bars stack above it. */
    public static void claimLeftStatusRow()
    {
        if (currentGui != null)
            currentGui.leftHeight += STATUS_ROW_HEIGHT;
    }
}
