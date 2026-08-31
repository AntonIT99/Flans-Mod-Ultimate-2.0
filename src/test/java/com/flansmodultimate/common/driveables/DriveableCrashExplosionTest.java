package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableCrashExplosionTest
{
    @Test
    void anEmptyTankLeavesWreckageRatherThanAFireball()
    {
        assertFalse(DriveableCrashExplosion.evaluate(4F, 1F, 0F).happens());
        assertFalse(DriveableCrashExplosion.evaluate(4F, 1F, 0.01F).happens());
    }

    @Test
    void aTypeWithoutADeathExplosionRadiusNeverBlows()
    {
        assertFalse(DriveableCrashExplosion.evaluate(0F, 1F, 1F).happens());
    }

    @Test
    void moreFuelMakesABiggerFireball()
    {
        float low = DriveableCrashExplosion.evaluate(4F, 1F, 0.1F).radius();
        float full = DriveableCrashExplosion.evaluate(4F, 1F, 1F).radius();

        assertTrue(low > 0F);
        assertTrue(full > low);
    }

    @Test
    void aWorseImpactMakesABiggerFireball()
    {
        float marginal = DriveableCrashExplosion.evaluate(4F, 0.75F, 1F).radius();
        float severe = DriveableCrashExplosion.evaluate(4F, 1F, 1F).radius();

        assertTrue(severe > marginal);
    }

    @Test
    void theBlastNeverExceedsTheConfiguredRadius()
    {
        assertTrue(DriveableCrashExplosion.evaluate(4F, 1F, 1F).radius() <= 4F);
        assertTrue(DriveableCrashExplosion.evaluate(4F, 2F, 2F).radius() <= 4F);
    }

    @Test
    void theFireballReadsLargerThanItBites()
    {
        DriveableCrashExplosion.Blast blast = DriveableCrashExplosion.evaluate(4F, 1F, 1F);

        assertTrue(blast.visualRadius() > blast.radius());
    }

    @Test
    void aCrashThatDoesNotBlowHasNothingToShow()
    {
        assertEquals(0F, DriveableCrashExplosion.evaluate(4F, 1F, 0F).visualRadius());
    }

    @Test
    void onlyAWellFuelledCrashSetsTheGroundAlight()
    {
        assertEquals(0F, DriveableCrashExplosion.evaluate(4F, 1F, 0.1F).fireRadius());
        assertTrue(DriveableCrashExplosion.evaluate(4F, 1F, 0.9F).fireRadius() > 0F);
    }
}
