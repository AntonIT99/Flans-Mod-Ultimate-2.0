package com.flansmodultimate.common.driveables.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleSquashDamageTest
{
    /** About 1.5 t, the calibration case. */
    private static final double CAR_KG = 1500D;
    private static final double TANK_KG = 50000D;
    /** 60 km/h is 16.67 m/s, which is 0.8333 blocks a tick. */
    private static final double SIXTY_KMH = 60D / 3.6D / 20D;

    @Test
    void aStationaryOrReversingHullRunsNothingOver()
    {
        assertEquals(0D, VehicleSquashDamage.damage(CAR_KG, 0D));
        assertEquals(0D, VehicleSquashDamage.damage(CAR_KG, -0.5D));
        assertEquals(0D, VehicleSquashDamage.damage(CAR_KG, Double.NaN));
    }

    @Test
    void aCarAtSixtyKilometresAnHourAccountsForAPlayer()
    {
        // The calibration point: a person struck by a car at 60 km/h is unlikely
        // to survive, so this is where the curve reaches twenty hitpoints.
        assertEquals(20D, VehicleSquashDamage.damage(CAR_KG, SIXTY_KMH), 0.5D);
    }

    @Test
    void speedEntersSquaredRatherThanLinearly()
    {
        double once = VehicleSquashDamage.damage(CAR_KG, 0.4D);
        double twice = VehicleSquashDamage.damage(CAR_KG, 0.8D);

        assertEquals(4D, twice / once, 1.0E-6D);
    }

    @Test
    void aLightHullHurtsFarLessThanAHeavyOneAtTheSameSpeed()
    {
        double light = VehicleSquashDamage.damage(60D, 0.5D);
        double heavy = VehicleSquashDamage.damage(TANK_KG, 0.5D);

        assertTrue(light < heavy * 0.5D, "a 60 kg hull should deliver well under half what a tank does");
    }

    @Test
    void massSaturatesOnceTheHullNoLongerSlowsDown()
    {
        // Both are far heavier than the body they strike, so the body absorbs
        // what its own mass dictates and the difference nearly vanishes. This is
        // the physics, not a clamp.
        double lorry = VehicleSquashDamage.damage(5000D, 0.5D);
        double tank = VehicleSquashDamage.damage(TANK_KG, 0.5D);

        assertEquals(lorry, tank, tank * 0.05D);
    }

    @Test
    void absorbedEnergyIsCarriedByTheReducedMassOfThePair()
    {
        // 80 kg body, 80 kg hull: the reduced mass is half the body, so the body
        // takes half of what an immovable hull of the same speed would give it.
        double equalMass = VehicleSquashDamage.absorbedKilojoules(80D, 0.5D);
        double immovable = VehicleSquashDamage.absorbedKilojoules(1.0E9D, 0.5D);

        assertEquals(0.5D, equalMass / immovable, 1.0E-3D);
    }

    @Test
    void anAbsurdSpeedIsBoundedRatherThanOverflowing()
    {
        assertEquals(100D, VehicleSquashDamage.damage(TANK_KG, 10D));
        assertEquals(100D, VehicleSquashDamage.damage(TANK_KG, Double.MAX_VALUE));
    }

    @Test
    void aHullWithNoUsableMassKeepsTheLegacyFlatCurve()
    {
        assertEquals(8D, VehicleSquashDamage.damage(0D, 0.5D), 1.0E-9D);
        assertEquals(8D, VehicleSquashDamage.damage(Double.NaN, 0.5D), 1.0E-9D);
        assertEquals(40D, VehicleSquashDamage.legacyDamage(10D), 1.0E-9D);
        assertEquals(0D, VehicleSquashDamage.legacyDamage(0D));
    }
}
