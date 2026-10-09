package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class EpicShipPhysicsTest
{
    private static final EpicShipPhysics.PartHealth HEALTHY = new EpicShipPhysics.PartHealth(100F, 100F);
    private static final EpicShipPhysics.PartHealth MISSING = new EpicShipPhysics.PartHealth(0F, 0F);

    @Test
    void absentCompartmentsAndBuoyancyDoNotSinkLegacyPacks()
    {
        EpicShipPhysics.Damage damage = EpicShipPhysics.damage(MISSING, MISSING, MISSING, MISSING, MISSING, MISSING);
        assertFalse(damage.sinking());
        assertEquals(1F, damage.buoyancyRatio());
        assertEquals(0.07D, EpicShipPhysics.verticalVelocity(-0.2D, 0.07D, true, damage));
    }

    @Test
    void invalidAndOverfullHealthCannotPoisonFlotation()
    {
        assertEquals(1F, new EpicShipPhysics.PartHealth(Float.NaN, 100F).ratio());
        assertEquals(1F, new EpicShipPhysics.PartHealth(0F, Float.NaN).ratio());
        assertEquals(1F, new EpicShipPhysics.PartHealth(200F, 100F).ratio());
        assertEquals(0F, new EpicShipPhysics.PartHealth(-1F, 100F).ratio());
    }

    @Test
    void eachDestroyedDefinedCompartmentStartsSinking()
    {
        for (int lost = 1; lost <= 5; lost++)
            assertTrue(damage(1F, lost).sinking(), "compartment " + lost);
        assertFalse(damage(0.1F, -1).sinking());
        assertTrue(damage(0.09F, -1).sinking());
    }

    @Test
    void sinkingOverridesEvenStrongHealthyDraftLift()
    {
        EpicShipPhysics.Damage lostBow = damage(1F, 1);
        double velocity = 0D;
        for (int tick = 0; tick < 100; tick++)
            velocity = EpicShipPhysics.verticalVelocity(velocity, 0.25D, true, lostBow);
        assertEquals(-0.009D, velocity, 1.0E-8D);
        assertTrue(EpicShipPhysics.verticalVelocity(0D, 0.25D, false, lostBow) < 0D);
    }

    @Test
    void lostBuoyancyUsesTheForksDeepSinkingFormula()
    {
        EpicShipPhysics.Damage damage = damage(0F, -1);
        double velocity = 0D;
        for (int tick = 0; tick < 100; tick++)
            velocity = EpicShipPhysics.verticalVelocity(velocity, 0.25D, true, damage);
        assertEquals(-0.15D, velocity, 1.0E-8D);
    }

    @Test
    void partialBuoyancyDamageReducesLiftAndPropulsion()
    {
        EpicShipPhysics.Damage half = damage(0.5F, -1);
        assertEquals(0.05D, EpicShipPhysics.verticalVelocity(0D, 0.1D, false, half), 1.0E-9D);
        assertEquals(0.5F, EpicShipPhysics.throttleLimit(half, true, 0D));
        assertEquals(0.3F, EpicShipPhysics.throttleLimit(half, false, 0D));
        assertEquals(0F, EpicShipPhysics.throttleLimit(damage(0.09F, -1), true, 1.01D));
    }

    @Test
    void damageProducesDirectionalListingInDegreesWithLegacyCaps()
    {
        assertTrue(EpicShipPhysics.roll(0F, damage(1F, 3)) > 0F);
        assertTrue(EpicShipPhysics.roll(0F, damage(1F, 4)) < 0F);
        assertTrue(EpicShipPhysics.pitch(0F, damage(1F, 1)) > 0F);
        assertTrue(EpicShipPhysics.pitch(0F, damage(1F, 2)) < 0F);
        float roll = 0F;
        float pitch = 0F;
        for (int tick = 0; tick < 10000; tick++)
        {
            roll = EpicShipPhysics.roll(roll, damage(1F, 3));
            pitch = EpicShipPhysics.pitch(pitch, damage(1F, 1));
        }
        assertEquals(Math.toDegrees(1.5D), roll, 1.0E-4D);
        assertEquals(Math.toDegrees(0.6D), pitch, 1.0E-4D);
    }

    @Test
    void repairedShipsReturnToLevelAndNoLongerQualifyForDestruction()
    {
        EpicShipPhysics.Damage healthy = damage(1F, -1);
        assertEquals(8.5F, EpicShipPhysics.roll(10F, healthy));
        assertEquals(-8.5F, EpicShipPhysics.pitch(-10F, healthy));
        assertFalse(EpicShipPhysics.shouldDestroy(healthy, 20D));
    }

    @Test
    void sinkingWrecksUseActualDepthAndRequireAValidSurface()
    {
        EpicShipPhysics.Damage sinking = damage(1F, 5);
        assertFalse(EpicShipPhysics.shouldDestroy(sinking, 15D));
        assertTrue(EpicShipPhysics.shouldDestroy(sinking, 15.01D));
        assertFalse(EpicShipPhysics.shouldDestroy(sinking, Double.NaN));
        assertFalse(EpicShipPhysics.shouldDestroy(sinking, Double.POSITIVE_INFINITY));
    }

    @Test
    void allThreeWideDeepWaterProbesAreRequired()
    {
        Vec3 origin = new Vec3(0D, 64D, 0D);
        assertTrue(EpicShipPhysics.atSea(origin, point -> point.y <= 60D));
        assertFalse(EpicShipPhysics.atSea(origin, point -> point.x == 0D && point.z == 0D));
        assertFalse(EpicShipPhysics.atSea(origin, point -> point.y >= 60D));
    }

    private static EpicShipPhysics.Damage damage(float buoyancyRatio, int lost)
    {
        EpicShipPhysics.PartHealth[] parts = {new EpicShipPhysics.PartHealth(100F * buoyancyRatio, 100F), HEALTHY, HEALTHY, HEALTHY, HEALTHY, HEALTHY};
        if (lost >= 0)
            parts[lost] = new EpicShipPhysics.PartHealth(0F, 100F);
        return EpicShipPhysics.damage(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }
}
