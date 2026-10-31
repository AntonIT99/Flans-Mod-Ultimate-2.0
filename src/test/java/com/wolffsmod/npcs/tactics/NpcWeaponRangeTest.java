package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcWeaponRangeTest
{
    @Test
    void aTighterSpreadReachesFurtherWithinTheLimitOfItsRole()
    {
        // A Thompson's 0.40 degrees, about 2.8 spread units.
        assertEquals(71, NpcWeaponRange.range(2.8D, 1, WeaponRole.GENERAL));
        // A Kar98k's 0.15 degrees would reach 190 blocks, beyond what an unmagnified rifle is given.
        assertEquals(NpcWeaponRange.limit(WeaponRole.GENERAL), NpcWeaponRange.range(1.05D, 1, WeaponRole.GENERAL));
        // The same spread behind a scope reaches its full distance.
        assertEquals(190, NpcWeaponRange.range(1.05D, 1, WeaponRole.MARKSMAN));
        assertEquals(NpcWeaponRange.limit(WeaponRole.MARKSMAN), NpcWeaponRange.range(0D, 1, WeaponRole.MARKSMAN), "a weapon without spread");
    }

    @Test
    void aWideSpreadStillReachesTheShortestRange()
    {
        assertEquals(NpcWeaponRange.MIN_RANGE, NpcWeaponRange.range(60D, 1, WeaponRole.GENERAL));
    }

    @Test
    void severalPelletsReachFurtherThanOneOfTheirSpread()
    {
        int single = NpcWeaponRange.range(14D, 1, WeaponRole.CLOSE);
        int pellets = NpcWeaponRange.range(14D, 8, WeaponRole.CLOSE);
        assertTrue(pellets > single, pellets + " > " + single);
        assertTrue(pellets <= NpcWeaponRange.limit(WeaponRole.CLOSE));
    }

    @Test
    void worseAimShortensTheRange()
    {
        // Custom NPCs accuracy 95 and 81, as an Elite and a Regular Teams bot hold a Kar98k.
        int elite = NpcWeaponRange.range(1D, 1, WeaponRole.GENERAL);
        int regular = NpcWeaponRange.range(3.8D, 1, WeaponRole.GENERAL);
        assertTrue(regular < elite, regular + " < " + elite);
    }

    @Test
    void everyRoleReachesAtLeastTheShortestRange()
    {
        for (WeaponRole role : WeaponRole.values())
            assertTrue(NpcWeaponRange.limit(role) >= NpcWeaponRange.MIN_RANGE, role.name());
    }
}
