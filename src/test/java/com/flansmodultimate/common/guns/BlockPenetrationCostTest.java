package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPenetrationCostTest
{
    @Test
    void unbreakableBlockCannotIncreasePenetration()
    {
        float remaining = 0F - ShootingHelper.blockPenetrationCost(-1F, 1F);

        assertEquals(Float.NEGATIVE_INFINITY, remaining);
        assertTrue(remaining <= 0F);
    }

    @Test
    void ordinaryBlockKeepsItsHardnessCost()
    {
        assertEquals(3F, ShootingHelper.blockPenetrationCost(1.5F, 1F));
    }
}
