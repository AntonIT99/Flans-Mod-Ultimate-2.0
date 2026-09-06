package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.gui.GuiGraphics;

/** Draws the teams rank badges from the shared rank sheet, mirroring the legacy DrawRankIcon helper. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsRankIcon
{
    private static final int SHEET_WIDTH = 512;
    private static final int SHEET_HEIGHT = 256;
    private static final int ICON_SIZE = 16;
    private static final int RANKS_PER_ROW = SHEET_WIDTH / ICON_SIZE;
    private static final int PRESTIGE_ROWS = SHEET_HEIGHT / ICON_SIZE;

    /** Draws the badge for the given rank, scaled to double size when requested. */
    public static void draw(GuiGraphics graphics, int rank, int prestige, int x, int y, boolean doubleSize)
    {
        int column = Math.floorMod(rank, RANKS_PER_ROW);
        int row = Math.floorMod(prestige, PRESTIGE_ROWS);
        int size = doubleSize ? ICON_SIZE * 2 : ICON_SIZE;
        graphics.blit(FlansMod.TEXTURE_GUI_TEAMSRANKS, x, y, size, size,
            column * ICON_SIZE, row * ICON_SIZE, ICON_SIZE, ICON_SIZE, SHEET_WIDTH, SHEET_HEIGHT);
    }
}
