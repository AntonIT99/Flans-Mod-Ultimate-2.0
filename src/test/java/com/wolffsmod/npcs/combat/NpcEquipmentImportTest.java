package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcEquipmentImport.Rhythm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcEquipmentImportTest
{
    @Test
    void aMagazineBecomesOneBurstFollowedByTheReload()
    {
        assertEquals(new Rhythm(60, 30, 2), NpcEquipmentImport.rhythm(2D, 60D, 30));
        // A reload shorter than the firing interval cannot make the pause shorter than a shot.
        assertEquals(new Rhythm(4, 8, 4), NpcEquipmentImport.rhythm(4D, 1D, 8));
        assertEquals(new Rhythm(50, NpcEquipmentImport.MAX_BURST, 1), NpcEquipmentImport.rhythm(0.4D, 49.2D, 250));
    }

    @Test
    void weaponsTooSlowForABurstOrHoldingOneRoundFireOneShotPerCycle()
    {
        // A bolt action fires slower than the longest burst delay the editor accepts.
        assertEquals(new Rhythm(40, 1, 0), NpcEquipmentImport.rhythm(40D, 70D, 5));
        // A single-shot launcher waits for whichever of its delay and reload is longer.
        assertEquals(new Rhythm(80, 1, 0), NpcEquipmentImport.rhythm(10D, 80D, 1));
        assertEquals(new Rhythm(25, 1, 0), NpcEquipmentImport.rhythm(25D, 0D, 1));
        assertEquals(new Rhythm(20, 1, 0), NpcEquipmentImport.rhythm(Double.NaN, Double.NaN, 0));
        assertEquals(new Rhythm(NpcEquipmentImport.MAX_DELAY, 1, 0), NpcEquipmentImport.rhythm(1.0E9D, 1.0E9D, 1));
    }

    @Test
    void protectionBecomesACustomNpcsResistanceBetweenNeutralAndImmune()
    {
        assertEquals(1F, NpcEquipmentImport.resistance(0F));
        assertEquals(1.4F, NpcEquipmentImport.resistance(0.4F));
        assertEquals(2F, NpcEquipmentImport.resistance(3F));
        assertEquals(1F, NpcEquipmentImport.resistance(-1F));
        assertEquals(1F, NpcEquipmentImport.resistance(Float.NaN));
    }
}
