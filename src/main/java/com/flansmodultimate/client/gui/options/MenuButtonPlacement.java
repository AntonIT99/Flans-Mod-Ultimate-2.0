package com.flansmodultimate.client.gui.options;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Works out where the mod's options button goes in a vanilla menu, from the buttons the menu already has.
 * Both the pause menu and the options screen are centered layouts other mods add rows to, so the button
 * takes the place of the last full width row, which moves down to make room, rather than sitting at fixed
 * coordinates. When the menu already reaches the bottom of the screen, the button retreats into the bottom
 * left corner instead of pushing anything off screen.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MenuButtonPlacement
{
    public static final int BUTTON_HEIGHT = 20;
    /** A widget row, as the vanilla grid layouts space them. */
    public static final int ROW_HEIGHT = 24;
    private static final int SCREEN_MARGIN = 2;
    private static final int CORNER_MARGIN = 4;
    private static final int CORNER_WIDTH = 100;

    /** A widget's bounds, in the screen's scaled coordinates. */
    public record Rect(int x, int y, int width, int height)
    {
        public int bottom()
        {
            return y + height;
        }
    }

    /**
     * @param x            left edge of the button to add
     * @param y            top edge of the button to add
     * @param width        width of the button to add
     * @param shiftFromY   every widget at or below this y moves down by {@code shiftBy} to make room;
     *                     {@link #NO_SHIFT} when nothing has to move
     * @param shiftBy      how far those widgets move down
     */
    public record Placement(int x, int y, int width, int shiftFromY, int shiftBy)
    {
        public int height()
        {
            return BUTTON_HEIGHT;
        }
    }

    public static final int NO_SHIFT = Integer.MAX_VALUE;

    /**
     * @param buttons the bounds of the buttons already on the menu, excluding labels and other decoration
     * @return where to put the options button, or null when the menu has no buttons to anchor it to
     */
    @Nullable
    public static Placement compute(List<Rect> buttons, int screenWidth, int screenHeight)
    {
        if (buttons.isEmpty())
            return null;

        Rect lastFullWidthRow = buttons.get(0);
        int lowestBottom = lastFullWidthRow.bottom();
        for (Rect button : buttons)
        {
            // The widest row is the full width column the vanilla grids center everything else on, and the
            // lowest one of those is the row the menu ends with: Disconnect, or Done
            if (button.width() > lastFullWidthRow.width()
                || button.width() == lastFullWidthRow.width() && button.y() > lastFullWidthRow.y())
                lastFullWidthRow = button;
            lowestBottom = Math.max(lowestBottom, button.bottom());
        }

        if (lowestBottom + ROW_HEIGHT <= screenHeight - SCREEN_MARGIN)
            return new Placement(lastFullWidthRow.x(), lastFullWidthRow.y(), lastFullWidthRow.width(), lastFullWidthRow.y(), ROW_HEIGHT);

        int cornerWidth = Math.min(CORNER_WIDTH, Math.max(0, screenWidth - 2 * CORNER_MARGIN));
        return new Placement(CORNER_MARGIN, screenHeight - BUTTON_HEIGHT - CORNER_MARGIN, cornerWidth, NO_SHIFT, 0);
    }
}
