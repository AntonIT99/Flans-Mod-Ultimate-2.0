package com.flansmodultimate.client.gui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeployMarkerLayoutTest
{
    @Test
    void vehiclesAtHeadquartersRemainSeparatelySelectable()
    {
        DeployMarkerLayout.Point headquarters = new DeployMarkerLayout.Point(100, 100);
        List<DeployMarkerLayout.Point> occupied = new ArrayList<>(List.of(headquarters));
        for (int vehicle = 0; vehicle < 8; vehicle++)
        {
            DeployMarkerLayout.Point placed = DeployMarkerLayout.separate(headquarters, occupied, 8, 8, 312, 204);
            assertTrue(occupied.stream().noneMatch(point -> Math.abs(point.x() - placed.x()) < 20 && Math.abs(point.y() - placed.y()) < 20), "Markers must have separate clickable areas");
            assertTrue(placed.x() >= 8 && placed.x() <= 312 && placed.y() >= 8 && placed.y() <= 204);
            occupied.add(placed);
        }
    }

    @Test
    void markersKeepTheirPositionWhenClearAndStayInsideTheMapAtAnEdge()
    {
        DeployMarkerLayout.Point corner = new DeployMarkerLayout.Point(8, 8);
        assertEquals(corner, DeployMarkerLayout.separate(corner, List.of(), 8, 8, 312, 204));
        DeployMarkerLayout.Point placed = DeployMarkerLayout.separate(corner, List.of(corner), 8, 8, 312, 204);
        assertNotEquals(corner, placed);
        assertTrue(placed.x() >= 8 && placed.y() >= 8);
        assertEquals(corner, DeployMarkerLayout.separate(corner, List.of(corner), 8, 8, 8, 8));
    }
}
