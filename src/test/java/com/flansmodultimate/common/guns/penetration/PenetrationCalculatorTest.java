package com.flansmodultimate.common.guns.penetration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PenetrationCalculatorTest
{
    @Test
    void authoredPenetrationIsUsedDirectly()
    {
        assertEquals(100F, PenetrationCalculator.currentPenetrationMm(100F));
    }

    @Test
    void invalidInputsNeverCreateNan()
    {
        assertEquals(0F, PenetrationCalculator.currentPenetrationMm(Float.NaN));
        assertEquals(0F, PenetrationCalculator.currentPenetrationMm(Float.POSITIVE_INFINITY));
        assertEquals(0F, PenetrationCalculator.currentPenetrationMm(-1F));
    }

    @Test
    void missingPenetrationBlocksArmourButNotUnarmouredParts()
    {
        assertFalse(PenetrationCalculator.resolve(null, 10F).penetrated());
        assertTrue(PenetrationCalculator.resolve(null, 0F).penetrated());
    }

    @Test
    void equalitySucceedsAndJustBelowFails()
    {
        assertTrue(PenetrationCalculator.resolve(100F, 100F).penetrated());
        assertFalse(PenetrationCalculator.resolve(99.99F, 100F).penetrated());
    }
}
