package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackAnimationPhysicsTest
{
    @Test
    void travelFollowsCompletedDisplacementIncludingCoastingAndReverse()
    {
        assertEquals(0F, TrackAnimationPhysics.travelStep(0, 0, 1, 0));
        assertEquals(0.075F, TrackAnimationPhysics.travelStep(0.3, 0, 1, 0), 1E-6);
        assertEquals(0.15F, TrackAnimationPhysics.travelStep(0.6, 0, 1, 0), 1E-6);
        assertEquals(-0.15F, TrackAnimationPhysics.travelStep(-0.6, 0, 1, 0), 1E-6);
        assertEquals(0F, TrackAnimationPhysics.travelStep(0, 1, 1, 0), "Sideways sliding must not drive the belt");
        assertEquals(0.15F, TrackAnimationPhysics.travelStep(0, -0.6, 0, -0.5), 1E-6, "Yaw and a pitched forward vector must not change horizontal travel");
        assertEquals(0F, TrackAnimationPhysics.travelStep(1, 1, 0, 0));
    }

    @Test
    void aStoppedHullHasNoTrackMotion()
    {
        // No displacement or rotation: residual throttle, steering and network velocity cannot drive the belts.
        float travel = TrackAnimationPhysics.travelStep(0D, 0D, 1D, 0D);
        assertEquals(new TrackAnimationPhysics.Steps(0F, 0F), TrackAnimationPhysics.steps(travel, 0F, 2.25D, true, true));
    }

    @Test
    void pivotTurnsMoveTheBeltsInOppositeDirectionsAndScaleWithActualRotation()
    {
        TrackAnimationPhysics.Steps pivot = TrackAnimationPhysics.steps(0F, 18F / 20F, 2.25D, true, true);
        float expected = (float) (Math.toRadians(18D / 20D) * 2.25D / 2D * 0.25D);
        assertEquals(-expected, pivot.left(), 1E-6);
        assertEquals(expected, pivot.right(), 1E-6);
        TrackAnimationPhysics.Steps reverse = TrackAnimationPhysics.steps(0F, -18F / 20F, 2.25D, true, true);
        assertEquals(-pivot.left(), reverse.left());
        assertEquals(-pivot.right(), reverse.right());
        TrackAnimationPhysics.Steps halfTurn = TrackAnimationPhysics.steps(0F, 9F / 20F, 2.25D, true, true);
        assertEquals(pivot.right() / 2F, halfTurn.right(), 1E-6);
    }

    @Test
    void turningWhileCoastingCombinesTranslationAndRotation()
    {
        float travel = TrackAnimationPhysics.travelStep(0.3D, 0D, 1D, 0D);
        TrackAnimationPhysics.Steps pivot = TrackAnimationPhysics.steps(0F, 0.9F, 2.25D, true, true);
        TrackAnimationPhysics.Steps coasting = TrackAnimationPhysics.steps(travel, 0.9F, 2.25D, true, true);
        assertEquals(travel + pivot.left(), coasting.left(), 1E-6);
        assertEquals(travel + pivot.right(), coasting.right(), 1E-6);
    }

    @Test
    void destroyedBeltsStayStillAndTheSurvivingBeltCoversTheFullPivotWidth()
    {
        TrackAnimationPhysics.Steps pivot = TrackAnimationPhysics.steps(0F, 0.9F, 2.25D, true, true);
        TrackAnimationPhysics.Steps leftOnly = TrackAnimationPhysics.steps(0F, -0.9F, 2.25D, true, false);
        assertEquals(pivot.right() * 2F, leftOnly.left(), 1E-6);
        assertEquals(0F, leftOnly.right());
        TrackAnimationPhysics.Steps rightOnly = TrackAnimationPhysics.steps(0F, 0.9F, 2.25D, false, true);
        assertEquals(0F, rightOnly.left());
        assertEquals(pivot.right() * 2F, rightOnly.right(), 1E-6);
        assertEquals(new TrackAnimationPhysics.Steps(0F, 0F), TrackAnimationPhysics.steps(0.075F, 0.9F, 2.25D, false, false));
    }

    @Test
    void textureFramesCompleteFourCyclesPerLoopAndWrapInBothDirections()
    {
        for (int cycle = 0; cycle < 4; cycle++)
        {
            assertEquals(0F, TrackAnimationPhysics.framePhase(cycle * 0.25F), 1E-6);
            assertEquals(1F / 3F, TrackAnimationPhysics.framePhase(cycle * 0.25F + 1F / 12F), 1E-6);
            assertEquals(2F / 3F, TrackAnimationPhysics.framePhase(cycle * 0.25F + 1F / 6F), 1E-6);
        }
        assertEquals(0F, TrackAnimationPhysics.framePhase(1F));
        assertEquals(0.8F, TrackAnimationPhysics.framePhase(-0.05F), 1E-6);
        assertEquals(TrackAnimationPhysics.framePhase(0.95F), TrackAnimationPhysics.framePhase(-0.05F), 1E-6);
    }
}
