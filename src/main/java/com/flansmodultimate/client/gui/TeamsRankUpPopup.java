package com.flansmodultimate.client.gui;

import com.flansmodultimate.client.sound.SoundHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * The rank-up notice of the Labjac fork: a panel that slides in at the top right of the screen
 * for a few seconds, showing the old and the new rank badge.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsRankUpPopup
{
    private static final long DURATION_MILLIS = 6000L;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;
    /** Pack sound of the Labjac fork; the vanilla level-up sound stands in where no pack has it. */
    private static final String SOUND = "levelup";

    private static long startMillis;
    private static int oldRank;
    private static int newRank;
    private static int prestige;

    public static void show(int from, int to, int prestigeLevel)
    {
        oldRank = from;
        newRank = to;
        prestige = prestigeLevel;
        startMillis = System.currentTimeMillis();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundHelper.getSoundEvent(SOUND).orElse(SoundEvents.PLAYER_LEVELUP), 1F, 1F));
    }

    public static void render(GuiGraphics graphics, int screenWidth)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (startMillis == 0L || minecraft.player == null || minecraft.options.hideGui)
            return;
        double age = (System.currentTimeMillis() - startMillis) / (double) DURATION_MILLIS;
        if (age < 0D || age > 1D)
        {
            startMillis = 0L;
            return;
        }

        // The easing of the vanilla advancement toast: in quickly, hold, out quickly.
        double hidden = age * 2D;
        if (hidden > 1D)
            hidden = 2D - hidden;
        hidden = Math.max(0D, 1D - hidden * 4D);
        hidden *= hidden;
        hidden *= hidden;

        int x = screenWidth - WIDTH;
        int y = -(int) (hidden * (HEIGHT + 4));
        Font font = minecraft.font;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xE0202020);
        graphics.renderOutline(x, y, WIDTH, HEIGHT, 0xFFFFFF55);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.rank_up"), x + 6, y + 6, 0xFFFF55, true);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 6F, y + 19F, 0F);
        graphics.pose().scale(0.8F, 0.8F, 1F);
        graphics.drawString(font, Component.translatable("gui.flansmod.teams.rank_up_detail", newRank), 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
        TeamsRankIcon.draw(graphics, oldRank, prestige, x + 100, y + 2, false);
        graphics.drawString(font, ">", x + 120, y + 6, 0xAAAAAA, true);
        TeamsRankIcon.draw(graphics, newRank, prestige, x + 132, y + 2, false);
    }
}
