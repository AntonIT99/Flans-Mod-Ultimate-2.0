package com.flansmodultimate.client.gui.options;

import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTooltipTest
{
    @Test
    void tooltipMovedAboveTopIsBroughtBackOnScreen()
    {
        Vector2ic position = SettingsTooltip.boundedPosition(418, 235, 170, 180, new Vector2i(172, -122));
        assertEquals(172, position.x());
        assertEquals(6, position.y());
    }

    @Test
    void bottomAndRightEdgesIncludePadding()
    {
        Vector2ic position = SettingsTooltip.boundedPosition(418, 235, 170, 180, new Vector2i(400, 220));
        assertEquals(242, position.x());
        assertEquals(47, position.y());
    }

    @Test
    void validPlacementIsPreserved()
    {
        Vector2ic position = SettingsTooltip.boundedPosition(960, 540, 170, 180, new Vector2i(500, 220));
        assertEquals(new Vector2i(500, 220), position);
    }

    @Test
    void pagesFitAtDifferentGuiScales()
    {
        for (int height : new int[]{180, 235, 270, 360, 540, 1080})
        {
            int contentHeight = SettingsTooltip.lineCapacity(height) * 10 + 2;
            assertTrue(contentHeight + 12 <= height, "Tooltip page exceeds GUI height " + height);
        }
    }
}
