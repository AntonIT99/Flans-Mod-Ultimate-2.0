package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The conversion between real-world speed and Minecraft speed is the foundation
 * the whole system rests on, so it is pinned exactly. One block is one metre and
 * twenty ticks are one second, which makes the divisor 72.
 */
class VehiclePhysicsUnitsTest
{
    private static final double EPSILON = 1.0E-9D;

    @Test
    void kmhConvertsToBlocksPerTickThroughSeventyTwo()
    {
        assertEquals(1D, VehiclePhysicsUnits.kmhToBlocksPerTick(72D, 1D), EPSILON);
        assertEquals(5D, VehiclePhysicsUnits.kmhToBlocksPerTick(360D, 1D), EPSILON);
        assertEquals(10D, VehiclePhysicsUnits.kmhToBlocksPerTick(720D, 1D), EPSILON);
    }

    @Test
    void speedScaleMultipliesTheConvertedSpeedAndNothingElse()
    {
        assertEquals(5D, VehiclePhysicsUnits.kmhToBlocksPerTick(720D, 0.5D), EPSILON);
        assertEquals(2.5D, VehiclePhysicsUnits.kmhToBlocksPerTick(720D, 0.25D), EPSILON);
        assertEquals(7.5D, VehiclePhysicsUnits.kmhToBlocksPerTick(720D, 0.75D), EPSILON);
    }

    @Test
    void conversionRoundTrips()
    {
        assertEquals(635D, VehiclePhysicsUnits.blocksPerTickToKmh(
            VehiclePhysicsUnits.kmhToBlocksPerTick(635D, 1D)), 1.0E-6D);
    }

    @Test
    void invalidSpeedsBecomeZeroRatherThanPoisoningPhysics()
    {
        assertEquals(0D, VehiclePhysicsUnits.kmhToBlocksPerTick(Double.NaN, 1D), EPSILON);
        assertEquals(0D, VehiclePhysicsUnits.kmhToBlocksPerTick(Double.POSITIVE_INFINITY, 1D), EPSILON);
        assertEquals(0D, VehiclePhysicsUnits.kmhToBlocksPerTick(-100D, 1D), EPSILON);
        assertEquals(0D, VehiclePhysicsUnits.kmhToBlocksPerTick(0D, 1D), EPSILON);
    }

    @Test
    void invalidScalesFallBackToFullSpeedInsteadOfCollapsingToZero()
    {
        assertEquals(1D, VehiclePhysicsUnits.kmhToBlocksPerTick(72D, Double.NaN), EPSILON);
        assertEquals(1D, VehiclePhysicsUnits.kmhToBlocksPerTick(72D, 0D), EPSILON);
        assertEquals(1D, VehiclePhysicsUnits.kmhToBlocksPerTick(72D, -2D), EPSILON);
    }

    @Test
    void metresPerSecondConvertsThroughTheTickRate()
    {
        assertEquals(1D, VehiclePhysicsUnits.metresPerSecondToBlocksPerTick(20D, 1D), EPSILON);
        assertEquals(0.5D, VehiclePhysicsUnits.metresPerSecondToBlocksPerTick(20D, 0.5D), EPSILON);
        assertEquals(20D, VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(1D), EPSILON);
    }

    @Test
    void accelerationConvertsThroughTheSquaredTickRate()
    {
        assertEquals(1D / 400D,
            VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(1D), EPSILON);
    }

    @Test
    void derivedRatiosMatchTheirDefinitions()
    {
        // 993 kW over 2890 kg, the Spitfire figures used in the documentation.
        assertEquals(0.3436F, VehiclePhysicsUnits.powerToWeight(993F, 2890F), 1.0E-4F);
        // 2890 kg over 22.48 m^2.
        assertEquals(128.56F, VehiclePhysicsUnits.wingLoading(2890F, 22.48F), 1.0E-2F);
        // 1 kN lifting 100 kg is a thrust-to-weight just above 1.
        assertEquals(1.0197F, VehiclePhysicsUnits.thrustToWeight(1F, 100F), 1.0E-4F);
    }

    @Test
    void derivedRatiosRefuseUnusableInputsInsteadOfDividingByZero()
    {
        assertEquals(0F, VehiclePhysicsUnits.powerToWeight(993F, 0F));
        assertEquals(0F, VehiclePhysicsUnits.powerToWeight(-1F, 2890F));
        assertEquals(0F, VehiclePhysicsUnits.wingLoading(2890F, 0F));
        assertEquals(0F, VehiclePhysicsUnits.wingLoading(Float.NaN, 22F));
        assertEquals(0F, VehiclePhysicsUnits.thrustToWeight(50F, Float.POSITIVE_INFINITY));
    }

    @Test
    void usablePositiveRejectsEveryDegenerateValue()
    {
        assertTrue(VehiclePhysicsUnits.isUsablePositive(0.001F));
        assertFalse(VehiclePhysicsUnits.isUsablePositive(0F));
        assertFalse(VehiclePhysicsUnits.isUsablePositive(-1F));
        assertFalse(VehiclePhysicsUnits.isUsablePositive(Float.NaN));
        assertFalse(VehiclePhysicsUnits.isUsablePositive(Float.POSITIVE_INFINITY));
        assertFalse(VehiclePhysicsUnits.isUsablePositive((Float) null));
    }
}
