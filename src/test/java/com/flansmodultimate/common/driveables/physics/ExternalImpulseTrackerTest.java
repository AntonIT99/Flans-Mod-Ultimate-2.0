package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The tracker must scale exactly the velocity an entity did not give itself,
 * exactly once, and never what its own simulation or a collision already
 * weighed against mass.
 */
class ExternalImpulseTrackerTest
{
    private static final double REFERENCE_KG = 100D;
    private static final double TANK_KG = 64_000D;
    private static final double JEEP_KG = 1_100D;

    @Test
    void theFirstVelocitySeenIsTakenAsTheEntitysOwn()
    {
        ExternalImpulseTracker tracker = new ExternalImpulseTracker();
        Vec3 loaded = new Vec3(0.3D, 0D, 0D);
        assertSame(loaded, tracker.absorb(loaded, TANK_KG, REFERENCE_KG));
    }

    @Test
    void motionTheEntitySettledItselfIsNeverScaled()
    {
        ExternalImpulseTracker tracker = primed();
        tracker.settle(new Vec3(0.4D, 0D, 0D));
        Vec3 own = new Vec3(0.4D, 0D, 0D);
        assertSame(own, tracker.absorb(own, TANK_KG, REFERENCE_KG));
    }

    @Test
    void anOutsidePushIsWeighedAgainstMass()
    {
        ExternalImpulseTracker tracker = primed();
        tracker.settle(new Vec3(0.1D, 0D, 0D));
        Vec3 result = tracker.absorb(new Vec3(1.1D, 0D, 0D), JEEP_KG, REFERENCE_KG);
        assertEquals(0.1D + REFERENCE_KG / JEEP_KG, result.x, 1.0E-9D);
    }

    @Test
    void aPushTooSmallToMoveTheMassIsDiscarded()
    {
        ExternalImpulseTracker tracker = primed();
        tracker.settle(new Vec3(0.1D, 0D, 0D));
        Vec3 result = tracker.absorb(new Vec3(1.1D, 0.2D, 0D), TANK_KG, REFERENCE_KG);
        assertEquals(0.1D, result.x, 1.0E-12D);
        assertEquals(0D, result.y, 1.0E-12D);
    }

    @Test
    void aPushIsWeighedOnlyOnceEvenWithoutAnotherSettle()
    {
        ExternalImpulseTracker tracker = primed();
        Vec3 first = tracker.absorb(new Vec3(1D, 0D, 0D), JEEP_KG, REFERENCE_KG);
        Vec3 second = tracker.absorb(first, JEEP_KG, REFERENCE_KG);
        assertEquals(first.x, second.x);
    }

    @Test
    void anImpulseAlreadyWeighedAgainstMassIsNotScaledAgain()
    {
        ExternalImpulseTracker tracker = primed();
        tracker.addResolvedImpulse(new Vec3(0.05D, 0D, 0D));
        Vec3 result = tracker.absorb(new Vec3(0.05D, 0D, 0D), TANK_KG, REFERENCE_KG);
        assertEquals(0.05D, result.x, 1.0E-12D);
    }

    private static ExternalImpulseTracker primed()
    {
        ExternalImpulseTracker tracker = new ExternalImpulseTracker();
        tracker.absorb(Vec3.ZERO, TANK_KG, REFERENCE_KG);
        return tracker;
    }
}
