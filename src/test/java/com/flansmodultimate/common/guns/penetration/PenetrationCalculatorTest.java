package com.flansmodultimate.common.guns.penetration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PenetrationCalculatorTest
{
    @Test
    void pAtReferenceVelocityEqualsP100()
    {
        assertEquals(100F, PenetrationCalculator.currentPenetrationMm(100F, 700D, 700D, 1.43D), 1.0E-4F);
    }

    @Test
    void slowerReducesAndFasterIncreasesPenetration()
    {
        assertTrue(PenetrationCalculator.currentPenetrationMm(100F, 600D, 700D, 1.43D) < 100F);
        assertTrue(PenetrationCalculator.currentPenetrationMm(100F, 800D, 700D, 1.43D) > 100F);
    }

    @Test
    void invalidInputsNeverCreateNan()
    {
        float result = PenetrationCalculator.currentPenetrationMm(100F, Double.NaN, 700D, 1.43D);
        assertEquals(0F, result);
        assertTrue(Float.isFinite(result));
    }

    @Test
    void missingPenetrationBlocksArmourButNotUnarmouredParts()
    {
        assertFalse(PenetrationCalculator.resolve(null, 700D, 700D, 1.43D, 10F).penetrated());
        assertTrue(PenetrationCalculator.resolve(null, 700D, 700D, 1.43D, 0F).penetrated());
    }

    @Test
    void equalitySucceedsAndJustBelowFails()
    {
        assertTrue(PenetrationCalculator.resolve(100F, 700D, 700D, 1.43D, 100F).penetrated());
        assertFalse(PenetrationCalculator.resolve(99.99F, 700D, 700D, 1.43D, 100F).penetrated());
    }

    @Test
    void definitionTimeReferenceVelocityUsesDragOnlyOnce()
    {
        assertEquals(100F, PenetrationCalculator.referenceVelocityAt100m(5F, 1F), 1.0E-4F);
        assertTrue(PenetrationCalculator.referenceVelocityAt100m(5F, 0.99F) < 100F);
        assertEquals(0F, PenetrationCalculator.referenceVelocityAt100m(0F, 0.99F));
    }
}
