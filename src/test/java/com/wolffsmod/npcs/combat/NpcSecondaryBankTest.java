package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcSecondaryBankTest
{
    private static final int BOTH = NpcEquipment.PRIMARY_ARMED | NpcEquipment.SECONDARY_ARMED;

    @Test
    void theSwitchChoosesTheGroupWhileBothHaveWeapons()
    {
        assertFalse(NpcEquipment.secondaryBank(false, BOTH));
        assertTrue(NpcEquipment.secondaryBank(true, BOTH));
    }

    @Test
    void aBomberWhoseOnlyGroupIsItsBombsDropsThemWithTheSwitchOff()
    {
        assertTrue(NpcEquipment.secondaryBank(false, NpcEquipment.SECONDARY_ARMED));
        assertFalse(NpcEquipment.secondaryBank(true, NpcEquipment.PRIMARY_ARMED));
    }

    @Test
    void aModelWithoutWeaponsKeepsWhatTheSwitchSays()
    {
        assertFalse(NpcEquipment.secondaryBank(false, 0));
        assertTrue(NpcEquipment.secondaryBank(true, 0));
    }
}
