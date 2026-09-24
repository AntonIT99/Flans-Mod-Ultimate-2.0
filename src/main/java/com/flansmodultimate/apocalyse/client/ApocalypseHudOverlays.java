package com.flansmodultimate.apocalyse.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Counts the apocalypse in, once an AI-chip mecha has armed it. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseHudOverlays
{
    private static final int TOP_MARGIN = 8;
    /** Below this many seconds the warning pulses. */
    private static final int URGENT_SECONDS = 10;
    private static final int CALM_COLOUR = 0xFFD54F;
    private static final int URGENT_COLOUR = 0xFF5252;

    public static final LayeredDraw.Layer COUNTDOWN = (graphics, deltaTracker) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || !ApocalypseClientState.isCountingDown())
            return;

        int ticks = ApocalypseClientState.getCountdownTicks();
        int seconds = Mth.positiveCeilDiv(ticks, 20);
        Component text = Component.translatable("hud.flansmodultimate.apocalypse_countdown", seconds);

        Font font = minecraft.font;
        int x = (graphics.guiWidth() - font.width(text)) / 2;
        graphics.drawString(font, text, x, TOP_MARGIN, colourFor(seconds, ticks), true);
    };

    private static int colourFor(int seconds, int ticks)
    {
        if (seconds > URGENT_SECONDS)
            return CALM_COLOUR;
        // One flash per second in the last stretch, so the warning cannot be read past.
        return ticks % 20 < 10 ? URGENT_COLOUR : CALM_COLOUR;
    }
}
