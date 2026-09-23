package com.flansmodultimate.client.gui.options;

import com.flansmodultimate.client.gui.options.MenuButtonPlacement.Placement;
import com.flansmodultimate.client.gui.options.MenuButtonPlacement.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuButtonPlacementTest
{
    /** The vanilla 1.20.1 pause menu, as Forge extends it with its own full width Mods row. */
    private static List<Rect> pauseMenu(int top)
    {
        return List.of(
            new Rect(138, top, 204, 20),
            new Rect(138, top + 24, 98, 20),
            new Rect(244, top + 24, 98, 20),
            new Rect(138, top + 48, 98, 20),
            new Rect(244, top + 48, 98, 20),
            new Rect(138, top + 72, 204, 20),
            new Rect(138, top + 96, 204, 20));
    }

    /** The vanilla options screen: pairs of narrow buttons above a wider Done. */
    private static List<Rect> optionsScreen(int top)
    {
        return List.of(
            new Rect(155, top, 150, 20),
            new Rect(315, top, 150, 20),
            new Rect(155, top + 24, 150, 20),
            new Rect(315, top + 24, 150, 20),
            new Rect(140, top + 50, 200, 20));
    }

    @Test
    void buttonTakesTheSlotOfTheLastFullWidthRow()
    {
        Placement placement = MenuButtonPlacement.compute(pauseMenu(46), 480, 270);

        assertNotNull(placement);
        assertEquals(138, placement.x());
        assertEquals(204, placement.width());
        assertEquals(46 + 96, placement.y());
        // That row, and anything below it, moves down by one row to make space
        assertEquals(46 + 96, placement.shiftFromY());
        assertEquals(MenuButtonPlacement.ROW_HEIGHT, placement.shiftBy());
    }

    @Test
    void theOptionsScreenAnchorsOnItsDoneButton()
    {
        Placement placement = MenuButtonPlacement.compute(optionsScreen(40), 480, 270);

        assertNotNull(placement);
        assertEquals(140, placement.x());
        assertEquals(200, placement.width());
        assertEquals(40 + 50, placement.y());
        assertEquals(40 + 50, placement.shiftFromY());
    }

    @Test
    void buttonRetreatsToTheCornerWhenTheMenuReachesTheBottom()
    {
        Placement placement = MenuButtonPlacement.compute(pauseMenu(46), 480, 170);

        assertNotNull(placement);
        assertEquals(4, placement.x());
        assertEquals(100, placement.width());
        assertEquals(170 - MenuButtonPlacement.BUTTON_HEIGHT - 4, placement.y());
        // Nothing may move: there is no room left for another row
        assertEquals(MenuButtonPlacement.NO_SHIFT, placement.shiftFromY());
        assertEquals(0, placement.shiftBy());
    }

    @Test
    void screensWithoutAMenuGetNoButton()
    {
        assertNull(MenuButtonPlacement.compute(List.of(), 480, 270));
    }
}
