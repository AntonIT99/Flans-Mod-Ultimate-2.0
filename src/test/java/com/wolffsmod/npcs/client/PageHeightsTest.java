package com.wolffsmod.npcs.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageHeightsTest
{
    /**
     * A page drawing one row too many used to run over the hint line at the bottom of the tab rather than being cut off, so
     * the rows and the hint were printed on top of each other. Giving a page another row is cheap, so the budget is checked
     * here instead of in the game.
     */
    @Test
    void noPageReachesPastTheHintLine()
    {
        assertEquals(List.of(), GuiWolffsMod.overflowingPages());
    }
}
