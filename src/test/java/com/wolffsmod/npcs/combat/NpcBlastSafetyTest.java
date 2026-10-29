package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.projectiles.DetonationRadii;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcBlastSafetyTest
{
    private static final double MARGIN = 4D;
    private static final double RANGE = 64D;

    @Test
    void theReachOfARoundIsTheFurthestOfItsEffectsWithFragmentsCountedInPart()
    {
        assertEquals(6D, NpcBlastSafety.reach(new DetonationRadii(6D, 3D, 4D, 2D)));
        assertEquals(7D, NpcBlastSafety.reach(new DetonationRadii(3D, 7D, 4D, 2D)));
        assertEquals(9D, NpcBlastSafety.reach(new DetonationRadii(3D, 7D, 4D, 9D)), "fire reaches furthest");
        // Fragments that far off seldom hit, so only part of their radius is kept out of.
        assertEquals(20D * NpcBlastSafety.FRAGMENT_SHARE, NpcBlastSafety.reach(new DetonationRadii(3D, 7D, 20D, 2D)));
    }

    @Test
    void aRoundThatDoesNotGoOffKeepsNoStandoffAtAll()
    {
        assertEquals(0D, NpcBlastSafety.reach(new DetonationRadii(0D, 0D, 0D, 0D)));
        assertEquals(0D, NpcBlastSafety.standoff(0D, MARGIN, RANGE));
        assertEquals(0D, NpcBlastSafety.standoff(-1D, MARGIN, RANGE), "nothing is read as no blast either");
    }

    @Test
    void theStandoffIsTheRoundsReachPlusTheMargin()
    {
        assertEquals(10D, NpcBlastSafety.standoff(6D, MARGIN, RANGE));
        assertEquals(6D, NpcBlastSafety.standoff(6D, 0D, RANGE));
        assertEquals(6D, NpcBlastSafety.standoff(6D, -2D, RANGE), "a margin below zero adds nothing");
    }

    @Test
    void aBlastThatOutreachesTheFiringRangeStillLeavesRoomToFire()
    {
        double capped = RANGE * NpcBlastSafety.MAX_RANGE_SHARE;
        assertEquals(capped, NpcBlastSafety.standoff(80D, MARGIN, RANGE));
        assertEquals(capped, NpcBlastSafety.standoff(RANGE, MARGIN, RANGE));
        assertEquals(0D, NpcBlastSafety.standoff(6D, MARGIN, 0D), "with no range at all it holds nothing back");
    }
}
