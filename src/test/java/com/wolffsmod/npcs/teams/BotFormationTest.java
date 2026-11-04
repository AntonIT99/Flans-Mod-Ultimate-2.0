package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class BotFormationTest
{
    private static final double SPACING = 24D;
    /** The squad comes up from the north, so its line astern trails back that way. */
    private static final Vec3 STATION = new Vec3(0D, 64D, 100D);
    private static final Vec3 FROM = new Vec3(0D, 64D, 0D);

    @Test
    void theLeaderOfALineLiesOnTheStationItself()
    {
        assertEquals(STATION, BotFormation.line(STATION, FROM, 0, SPACING));
    }

    @Test
    void everyShipAsternKeepsItsSpacingBehindTheOneAhead()
    {
        Vec3 second = BotFormation.line(STATION, FROM, 1, SPACING);
        Vec3 third = BotFormation.line(STATION, FROM, 2, SPACING);
        assertEquals(SPACING, STATION.distanceTo(second), 1.0E-6D);
        assertEquals(SPACING, second.distanceTo(third), 1.0E-6D);
        assertTrue(second.z < STATION.z, "the line trails back the way the squad came");
        assertEquals(0D, second.x, 1.0E-6D, "and stays on the squad's course");
    }

    @Test
    void shipsAreSpreadFarEnoughApartForTheirHullsNotToShareAStation()
    {
        // The spiral that spreads soldiers puts them within a few blocks, which no hull can use.
        Vec3 soldier = BotFormation.spot(STATION, 1, SPACING, java.util.List.of());
        assertTrue(STATION.distanceTo(soldier) < SPACING);
        assertEquals(SPACING, STATION.distanceTo(BotFormation.line(STATION, FROM, 1, SPACING)), 1.0E-6D);
    }

    @Test
    void aSquadAlreadyLyingOnItsStationRingsItInsteadOfStringingOut()
    {
        Vec3 first = BotFormation.line(STATION, STATION, 1, SPACING);
        Vec3 second = BotFormation.line(STATION, STATION, 2, SPACING);
        assertEquals(SPACING, STATION.distanceTo(first), 1.0E-6D);
        assertEquals(SPACING, STATION.distanceTo(second), 1.0E-6D);
        assertTrue(first.distanceTo(second) > 1D, "and no two of them share a spot");
    }

    @Test
    void aRingOpensOutOnceItHoldsAllTheHullsItCan()
    {
        double inner = STATION.distanceTo(BotFormation.line(STATION, STATION, 1, SPACING));
        double outer = STATION.distanceTo(BotFormation.line(STATION, STATION, 9, SPACING));
        assertTrue(outer > inner);
    }

    @Test
    void aLineKeepsTheStationsHeight()
    {
        assertEquals(STATION.y, BotFormation.line(STATION, FROM, 3, SPACING).y, 1.0E-6D);
    }
}
