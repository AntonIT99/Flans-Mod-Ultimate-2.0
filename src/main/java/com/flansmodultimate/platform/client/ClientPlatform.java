package com.flansmodultimate.platform.client;

import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Version boundary for client frame timing and screen helpers. Client-only. */
public final class ClientPlatform
{
    private ClientPlatform() {}

    /** Interpolation factor between the previous and current game tick for the frame being rendered. */
    public static float partialTick()
    {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    /** Real time elapsed since the previous frame, in ticks. */
    public static float realtimeDeltaTicks()
    {
        return Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
    }

    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public static float partialTick(RenderLevelStageEvent event)
    {
        return event.getPartialTick().getGameTimeDeltaPartialTick(true);
    }

    /** Vertical mouse-wheel movement of the event. */
    public static double scrollDelta(InputEvent.MouseScrollingEvent event)
    {
        return event.getScrollDeltaY();
    }

    public static void hideNameTag(RenderNameTagEvent event)
    {
        event.setCanRender(TriState.FALSE);
    }
}
