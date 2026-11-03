package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcGunBurstTest
{
    @Test
    void anAircraftsQuickGunsFireInBursts()
    {
        assertTrue(NpcDriverGuns.burstDiscipline(true, 1D).bursts());
        assertTrue(NpcDriverGuns.burstDiscipline(true, 2D).bursts());
    }

    @Test
    void aGunThatAlreadyPausesBetweenRoundsPacesItself()
    {
        assertFalse(NpcDriverGuns.burstDiscipline(true, 20D).bursts());
    }

    @Test
    void groundAndNavalMountsKeepTheCadenceTheirDefinitionStates()
    {
        assertFalse(NpcDriverGuns.burstDiscipline(false, 1D).bursts());
        assertFalse(NpcDriverGuns.burstDiscipline(false, 20D).bursts());
    }
}
