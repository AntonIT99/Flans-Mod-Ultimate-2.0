package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two independently usable overrides that change motion directly. Both must
 * be completely inert when their parameter is absent.
 */
class GroundSlopeAndDraftPhysicsTest
{
    // -------------------------------------------------------------- slope

    @Test
    void noSlopeLimitMeansNoEffectAtAnyPitch()
    {
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 1F, 0F));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 1F, -10F));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 1F, Float.NaN));
    }

    @Test
    void gentleClimbsAreUnaffected()
    {
        // Full propulsion up to 70% of the limit.
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(20F, 1F, 35F));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(24.4F, 1F, 35F));
    }

    @Test
    void propulsionRampsDownSmoothlyRatherThanCuttingOut()
    {
        float atFalloff = GroundSlopePhysics.propulsionFactor(24.5F, 1F, 35F);
        float midway = GroundSlopePhysics.propulsionFactor(30F, 1F, 35F);
        float nearLimit = GroundSlopePhysics.propulsionFactor(34.9F, 1F, 35F);
        assertTrue(atFalloff > midway, "must fall monotonically");
        assertTrue(midway > nearLimit);
        assertTrue(nearLimit > 0F, "a residual keeps the ramp continuous");
        assertEquals(0F, GroundSlopePhysics.propulsionFactor(35F, 1F, 35F));
        assertEquals(0F, GroundSlopePhysics.propulsionFactor(60F, 1F, 35F));
    }

    @Test
    void descendingAndStandingStillAreNeverLimited()
    {
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(-45F, 1F, 35F), "driving downhill is free");
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 0F, 35F), "no demand, no limit");
    }

    @Test
    void reversingUpTheSameSlopeIsLimitedToo()
    {
        // Nose-down while reversing is still climbing.
        assertEquals(0F, GroundSlopePhysics.propulsionFactor(-40F, -1F, 35F));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(40F, -1F, 35F));
    }

    // -------------------------------------------------------------- draft

    @Test
    void aHullSittingTooDeepIsPushedUpAndOneTooHighSettles()
    {
        // Surface at Y=64, draft 1.5, so the hull bottom belongs at Y=62.5.
        double tooDeep = MarineDraftPhysics.verticalVelocity(0D, 61D, 64D, 1.5D, 0.25D);
        double tooHigh = MarineDraftPhysics.verticalVelocity(0D, 63.5D, 64D, 1.5D, 0.25D);
        assertTrue(tooDeep > 0D, "a submerged hull must rise");
        assertTrue(tooHigh < 0D, "a hull riding high must settle");
    }

    @Test
    void aHullAtItsDraftIsHeldThere()
    {
        assertEquals(0D, MarineDraftPhysics.verticalVelocity(0D, 62.5D, 64D, 1.5D, 0.25D), 1.0E-9D);
    }

    @Test
    void theResponseIsBoundedByTheLegacyBuoyancyCeiling()
    {
        double veryDeep = MarineDraftPhysics.verticalVelocity(0D, 10D, 64D, 1.5D, 0.25D);
        assertTrue(veryDeep <= 0.25D, "must not exceed the legacy buoyancy clamp, was " + veryDeep);
        assertTrue(veryDeep > 0D);
    }

    @Test
    void theApproachIsDampedRatherThanInstant()
    {
        double first = MarineDraftPhysics.verticalVelocity(-0.5D, 61D, 64D, 1.5D, 0.25D);
        assertTrue(first > -0.5D, "must move toward the target");
        assertTrue(first < 0.25D, "must not jump straight to the target in one tick");
    }

    @Test
    void invalidDraftLeavesTheVelocityUntouched()
    {
        assertEquals(-0.3D, MarineDraftPhysics.verticalVelocity(-0.3D, 61D, 64D, 0D, 0.25D));
        assertEquals(-0.3D, MarineDraftPhysics.verticalVelocity(-0.3D, 61D, Double.NaN, 1.5D, 0.25D));
        assertEquals(0D, MarineDraftPhysics.verticalVelocity(Double.NaN, 61D, 64D, 1.5D, 0.25D));
    }
}
