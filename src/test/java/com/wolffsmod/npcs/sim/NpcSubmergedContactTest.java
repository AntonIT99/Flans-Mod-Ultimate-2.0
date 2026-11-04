package com.wolffsmod.npcs.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcSubmergedContactTest
{
    private static final double SURFACE_RANGE = 160D;

    @Test
    void aDeeperHullHasToComeCloserBeforeItIsNoticed()
    {
        double shallow = NpcSubmergedContact.contactRange(SURFACE_RANGE, 2);
        double deep = NpcSubmergedContact.contactRange(SURFACE_RANGE, 10);
        assertTrue(shallow < SURFACE_RANGE, "even a shallow boat is harder to pick up than one on the surface");
        assertTrue(deep < shallow);
    }

    @Test
    void aHullAtPeriscopeDepthIsStillSeenWellOff()
    {
        double periscope = NpcSubmergedContact.contactRange(SURFACE_RANGE, NpcSubmarine.PERISCOPE_DEPTH);
        assertEquals(SURFACE_RANGE - NpcSubmarine.PERISCOPE_DEPTH * NpcSubmergedContact.RANGE_PER_DEPTH, periscope, 1.0E-9D);
        assertTrue(periscope > SURFACE_RANGE * 0.5D, "coming up to fight costs it most of its cover");
    }

    @Test
    void aSubmarineHoweverDeepIsStillNoticedCloseAboard()
    {
        assertEquals(NpcSubmergedContact.MIN_CONTACT, NpcSubmergedContact.contactRange(SURFACE_RANGE, 64));
        assertEquals(NpcSubmergedContact.MIN_CONTACT, NpcSubmergedContact.contactRange(0D, 1));
    }

    @Test
    void aShortSightedSearcherNeverGainsRangeFromTheSubmarineBeingDeep()
    {
        assertTrue(NpcSubmergedContact.contactRange(20D, 1) <= 20D);
    }
}
