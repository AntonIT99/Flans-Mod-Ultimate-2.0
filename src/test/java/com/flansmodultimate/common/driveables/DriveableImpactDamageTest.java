package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriveableImpactDamageTest
{
    @Test
    void softBlocksAndSlowContactDoNothing()
    {
        assertEquals(0F, DriveableImpactDamage.blockStrikeHealthFraction(0.2F, 4D));
        assertEquals(0F, DriveableImpactDamage.blockStrikeHealthFraction(1.5F, 0.1D));
    }

    @Test
    void unbreakableBlocksAreInertJustAsInLegacy()
    {
        assertEquals(0F, DriveableImpactDamage.blockStrikeHealthFraction(-1F, 4D));
    }

    @Test
    void harderBlocksAndHigherSpeedsHurtMore()
    {
        float dirt = DriveableImpactDamage.blockStrikeHealthFraction(0.5F, 2D);
        float stone = DriveableImpactDamage.blockStrikeHealthFraction(1.5F, 2D);
        float fastStone = DriveableImpactDamage.blockStrikeHealthFraction(1.5F, 4D);

        assertTrue(dirt > 0F);
        assertTrue(stone > dirt);
        assertTrue(fastStone > stone);
    }

    @Test
    void oneStrikeCannotDestroyAHealthyPart()
    {
        assertTrue(DriveableImpactDamage.blockStrikeHealthFraction(50F, 8D) < 1F);
    }
}
