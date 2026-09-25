package com.flansmodultimate.platform.client;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/** Version boundary for client frame timing and screen helpers. Client-only. */
public final class ClientPlatform
{
    private ClientPlatform() {}

    /** Interpolation factor between the previous and current game tick for the frame being rendered. */
    public static float partialTick()
    {
        return Minecraft.getInstance().getFrameTime();
    }

    /** Real time elapsed since the previous frame, in ticks. */
    public static float realtimeDeltaTicks()
    {
        return Minecraft.getInstance().getDeltaFrameTime();
    }

    public static void renderBackground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        screen.renderBackground(graphics);
    }

    public static float partialTick(RenderLevelStageEvent event)
    {
        return event.getPartialTick();
    }

    /** Vertical mouse-wheel movement of the event. */
    public static double scrollDelta(InputEvent.MouseScrollingEvent event)
    {
        return event.getScrollDelta();
    }

    public static void hideNameTag(RenderNameTagEvent event)
    {
        event.setResult(Event.Result.DENY);
    }

    /** The item or block id of a baked model location, without its variant. */
    public static ResourceLocation modelItemId(ResourceLocation location)
    {
        return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath());
    }
}
