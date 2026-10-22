package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutePlannerTest
{
    private static Vec3 at(double x, double z)
    {
        return new Vec3(x, 64D, z);
    }

    @Test
    void aNearGoalIsReachedStraight()
    {
        assertEquals(List.of(at(30D, 0D)), RoutePlanner.route(at(0D, 0D), at(30D, 0D), List.of(at(15D, 50D)), RoutePlanner.LINK_DISTANCE));
    }

    @Test
    void aFarGoalIsReachedThroughTheShortestChainOfRoutePoints()
    {
        // Two chains lead to the goal; the one round the north is shorter.
        List<Vec3> nodes = List.of(at(50D, 20D), at(100D, 20D), at(150D, 20D), at(50D, -60D), at(100D, -60D), at(150D, -60D));
        List<Vec3> route = RoutePlanner.route(at(0D, 0D), at(200D, 0D), nodes, RoutePlanner.LINK_DISTANCE);
        assertEquals(List.of(at(50D, 20D), at(100D, 20D), at(150D, 20D), at(200D, 0D)), route);
    }

    @Test
    void anUnreachableGoalIsHeadedForStraight()
    {
        assertEquals(List.of(at(500D, 0D)), RoutePlanner.route(at(0D, 0D), at(500D, 0D), List.of(at(50D, 0D)), RoutePlanner.LINK_DISTANCE));
    }

    @Test
    void routePointsAreTakenInOrderAndNeverWalkedBackTo()
    {
        List<Vec3> route = List.of(at(50D, 0D), at(100D, 0D), at(150D, 0D));
        assertEquals(0, RoutePlanner.advance(at(0D, 0D), route, 0, 6D));
        assertEquals(1, RoutePlanner.advance(at(48D, 0D), route, 0, 6D));
        assertEquals(1, RoutePlanner.advance(at(20D, 0D), route, 1, 6D), "a point already passed is not gone back to");
        assertEquals(2, RoutePlanner.advance(at(150D, 0D), route, 2, 6D), "the last point stays the last");
    }

    @Test
    void stepsAreShortenedToAStretchAtATime()
    {
        assertEquals(at(RoutePlanner.STEP, 0D), RoutePlanner.towards(at(0D, 0D), at(100D, 0D)));
        assertEquals(at(10D, 0D), RoutePlanner.towards(at(0D, 0D), at(10D, 0D)));
    }

    @Test
    void detoursAlternateSidesAndWidenUpToALimit()
    {
        Vec3 from = at(0D, 0D);
        Vec3 heading = at(100D, 0D);
        Vec3 first = RoutePlanner.detour(from, heading, 0);
        Vec3 second = RoutePlanner.detour(from, heading, 1);
        assertTrue(first.x > 0D && second.x > 0D, "a detour still leads ahead");
        assertTrue(first.z * second.z < 0D, "the second try goes round the other side");
        assertEquals(Math.abs(first.z), Math.abs(second.z), 1.0E-9D);
        assertTrue(Math.abs(RoutePlanner.detour(from, heading, 2).z) > Math.abs(first.z), "later tries go wider");
        assertEquals(Math.abs(RoutePlanner.detour(from, heading, 40).z), Math.abs(RoutePlanner.detour(from, heading, 60).z), 1.0E-9D, "the width has a limit");
    }

    @Test
    void aDetourWithoutHeadingStillGoesSomewhere()
    {
        assertNotEquals(at(0D, 0D), RoutePlanner.detour(at(0D, 0D), at(0D, 0D), 0));
    }
}
