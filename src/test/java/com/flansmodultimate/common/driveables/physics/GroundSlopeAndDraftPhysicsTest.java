package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure motion helpers for derived uphill response and authored marine draft.
 */
class GroundSlopeAndDraftPhysicsTest
{
    // -------------------------------------------------------------- slope

    @Test
    void invalidProfileInputsLeavePropulsionUntouched()
    {
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 1F, 0F, EnumDriveType.RWD));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 1F, Float.NaN, EnumDriveType.RWD));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 1F, 0.03F, null));
    }

    @Test
    void gentleClimbsAreUnaffected()
    {
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(20F, 1F, 0.03F, EnumDriveType.RWD));
    }

    @Test
    void propulsionRampsDownSmoothlyRatherThanCuttingOut()
    {
        float atFalloff = GroundSlopePhysics.propulsionFactor(23F, 1F, 0.03F, EnumDriveType.RWD);
        float midway = GroundSlopePhysics.propulsionFactor(32F, 1F, 0.03F, EnumDriveType.RWD);
        float steep = GroundSlopePhysics.propulsionFactor(60F, 1F, 0.03F, EnumDriveType.RWD);
        assertTrue(atFalloff > midway, "must fall monotonically");
        assertTrue(midway > steep);
        assertEquals(0.2F, steep, 1.0E-6F, "steep terrain retains crawl propulsion");
    }

    @Test
    void powerToWeightAndDriveLayoutDetermineClimbingCapability()
    {
        float weakRwd = GroundSlopePhysics.propulsionFactor(30F, 1F, 0.015F, EnumDriveType.RWD);
        float strongRwd = GroundSlopePhysics.propulsionFactor(30F, 1F, 0.06F, EnumDriveType.RWD);
        float strongTracked = GroundSlopePhysics.propulsionFactor(30F, 1F, 0.06F, EnumDriveType.TRACKED);
        assertTrue(strongRwd > weakRwd);
        assertTrue(strongTracked >= strongRwd);
    }

    @Test
    void descendingAndStandingStillAreNeverLimited()
    {
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(-45F, 1F, 0.03F, EnumDriveType.RWD));
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(45F, 0F, 0.03F, EnumDriveType.RWD));
    }

    @Test
    void reversingUpTheSameSlopeIsLimitedToo()
    {
        // Nose-down while reversing is still climbing.
        assertTrue(GroundSlopePhysics.propulsionFactor(-40F, -1F, 0.03F, EnumDriveType.RWD) < 1F);
        assertEquals(1F, GroundSlopePhysics.propulsionFactor(40F, -1F, 0.03F, EnumDriveType.RWD));
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
