package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcAmmunitionTest
{
    @Test
    void theHardestHittingWeaponIsPickedAndTheFirstOfEquals()
    {
        assertEquals(2, NpcAmmunition.best(List.of(1D, 4D, 7D, 7D, 3D), Double::doubleValue, 1D));
        assertEquals(0, NpcAmmunition.best(List.of(5D), Double::doubleValue, 1D));
    }

    @Test
    void nothingIsPickedWhenNoItemBeatsAnEmptyHand()
    {
        assertEquals(-1, NpcAmmunition.best(List.of(1D, 0D, 1D), Double::doubleValue, 1D));
        assertEquals(-1, NpcAmmunition.best(List.of(), Double::doubleValue, 1D));
    }
}
