package com.flansmodultimate.common.driveables.physics;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    void draftUsesTheAuthoredKeelRatherThanTheEntityOrigin()
    {
        AABB core = new AABB(-2D, -3D, -1D, 2D, 1D, 1D);
        double bottom = MarineDraftPhysics.hullBottomY(core, 2D, point -> point.add(0D, 67D, 0D));
        assertEquals(61D, bottom, 1.0E-9D);
        assertEquals(0D, MarineDraftPhysics.verticalVelocity(0D, bottom, 64D, 3D, 0.25D), 1.0E-9D);
        assertTrue(MarineDraftPhysics.verticalVelocity(0D, 67D, 64D, 3D, 0.25D) < 0D, "using the origin as the keel would incorrectly sink this correctly floating hull");
    }

    @Test
    void aTiltedHullUsesItsLowestCorner()
    {
        AABB core = new AABB(-4D, -1D, -2D, 4D, 1D, 2D);
        double bottom = MarineDraftPhysics.hullBottomY(core, 1D, point -> new Vec3(point.y, 64D + point.x, point.z));
        assertEquals(60D, bottom, 1.0E-9D);
    }

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
    void aHullWithoutADraftSettlesOnItsWaterlineWithoutBobbing()
    {
        double depth = -0.2D;
        double velocity = -0.08D;
        double previousDepth = depth;
        int reversals = 0;
        double previousStep = 0D;
        for (int tick = 0; tick < 200; tick++)
        {
            velocity = MarineDraftPhysics.waterlineVelocity(velocity, depth, 0.05D);
            depth -= velocity;
            double step = depth - previousDepth;
            if (step * previousStep < -1.0E-12D)
                reversals++;
            previousStep = step;
            previousDepth = depth;
        }
        assertEquals(0D, depth, 1.0E-3D);
        assertTrue(reversals <= 1, "a hull at rest must not keep crossing the surface, reversed " + reversals + " times");
    }

    @Test
    void aHullWhoseFlightModelAppliesGravitySettlesOnItsWaterline()
    {
        // A plane-type ship: the legacy flight step takes 0.098 blocks per tick
        // off before flotation runs, and the capped response alone cannot carry it.
        double gravity = 0.098D;
        double ceiling = 0.05D;
        double carriedDepth = 0D;
        double carriedVelocity = 0D;
        double uncarriedDepth = 0D;
        double uncarriedVelocity = 0D;
        for (int tick = 0; tick < 400; tick++)
        {
            carriedVelocity = MarineDraftPhysics.waterlineVelocity(MarineDraftPhysics.carriedVelocity(carriedVelocity - gravity, gravity), carriedDepth, ceiling);
            carriedDepth -= carriedVelocity;
            uncarriedVelocity = MarineDraftPhysics.waterlineVelocity(uncarriedVelocity - gravity, uncarriedDepth, ceiling);
            uncarriedDepth -= uncarriedVelocity;
        }
        assertEquals(0D, carriedDepth, 1.0E-3D);
        assertTrue(uncarriedDepth > 10D, "weight left in must be what sank such hulls, depth was " + uncarriedDepth);
    }

    @Test
    void aDraftDeeperThanTheModelsHullIsCappedToIt()
    {
        // Rainfire5's HMS Jervis: 4.7 m researched draft, 2.835 block scaled core box.
        double capped = MarineDraftPhysics.modelDraft(4.7D, 2.835D);
        assertTrue(capped < 2.835D, "the deck must stay above water, draft was " + capped);
        assertEquals(2.835D * VehiclePhysicsConstants.MAX_DRAFT_HULL_FRACTION, capped, 1.0E-9D);
    }

    @Test
    void aModelDrawnToScaleKeepsItsDraft()
    {
        // The official S100: 1.7 m draft, 5.25 block core box.
        assertEquals(1.7D, MarineDraftPhysics.modelDraft(1.7D, 5.25D));
        assertEquals(1.7D, MarineDraftPhysics.modelDraft(1.7D, 0D), "an unknown hull leaves the draft alone");
        assertEquals(1.7D, MarineDraftPhysics.modelDraft(1.7D, Double.NaN));
    }

    @Test
    void onlyAPositiveFiniteWeightIsCarried()
    {
        assertEquals(-0.1D, MarineDraftPhysics.carriedVelocity(-0.1D, 0D));
        assertEquals(-0.1D, MarineDraftPhysics.carriedVelocity(-0.1D, -0.05D));
        assertEquals(-0.1D, MarineDraftPhysics.carriedVelocity(-0.1D, Double.NaN));
        assertEquals(0D, MarineDraftPhysics.carriedVelocity(-0.098D, 0.098D), 1.0E-12D);
    }

    @Test
    void theSurfaceProbeReachesTheDeepestAuthoredDraft()
    {
        assertTrue(VehiclePhysicsConstants.DRAFT_SURFACE_PROBE_BLOCKS >= 12);
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
    void aDeclaredDraftStillSettlesAHullWhoseDefinitionStatesNoBuoyancy()
    {
        // Buoyancy caps how fast a hull may rise. A boat that sets it to zero but
        // declares a draft would otherwise hang at whatever depth it was placed,
        // because the clamp left the restoring response no room to act at all.
        double noBuoyancy = MarineDraftPhysics.verticalVelocity(0D, 61D, 64D, 1.5D, 0D);
        assertTrue(noBuoyancy > 0D, "a declared draft must still reach its waterline, was " + noBuoyancy);
        assertTrue(noBuoyancy <= VehiclePhysicsConstants.MIN_DRAFT_RISE_BLOCKS_PER_TICK, "must stay within the minimum rise, was " + noBuoyancy);
        assertTrue(MarineDraftPhysics.verticalVelocity(0D, 61D, 64D, 1.5D, Double.NaN) > 0D);
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
