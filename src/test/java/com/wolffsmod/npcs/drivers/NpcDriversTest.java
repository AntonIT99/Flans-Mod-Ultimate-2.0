package com.wolffsmod.npcs.drivers;

import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Claim;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcDriversTest
{
    @Test
    void anNpcTakesDriveablesNobodyOwnsOrItsSidesUnlessItIsToldOtherwise()
    {
        assertEquals(Claim.ALLIED, new NpcTuning().claim());
        assertTrue(NpcDrivers.mayTake(Claim.ALLIED, false, false, false));
        assertTrue(NpcDrivers.mayTake(Claim.ALLIED, false, true, true));
        assertFalse(NpcDrivers.mayTake(Claim.ALLIED, false, true, false));
        assertTrue(NpcDrivers.mayTake(Claim.ANY, false, true, false));
        assertTrue(NpcDrivers.mayTake(Claim.UNOWNED, false, false, false));
        assertFalse(NpcDrivers.mayTake(Claim.UNOWNED, false, true, true));
    }

    @Test
    @SuppressWarnings("ConstantValue") // The rule never takes a locked driveable, which is what this asserts.
    void aDriveableLockedWithAKeyIsNeverTaken()
    {
        for (Claim claim : Claim.values())
            assertFalse(NpcDrivers.mayTake(claim, true, false, true), claim.name());
    }
}
