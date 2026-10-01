package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackAnimationPhysicsTest
{
    @Test
    void travelFollowsActualForwardSpeedIncludingCoastingAndReverse()
    {
        assertEquals(0F, TrackAnimationPhysics.travelStep(0, 0, 1, 0));
        assertEquals(0.075F, TrackAnimationPhysics.travelStep(0.3, 0, 1, 0), 1E-6);
        assertEquals(0.15F, TrackAnimationPhysics.travelStep(0.6, 0, 1, 0), 1E-6);
        assertEquals(-0.15F, TrackAnimationPhysics.travelStep(-0.6, 0, 1, 0), 1E-6);
        assertEquals(0F, TrackAnimationPhysics.travelStep(0, 1, 1, 0), "Sideways sliding must not drive the belt");
        assertEquals(0.15F, TrackAnimationPhysics.travelStep(0, -0.6, 0, -0.5), 1E-6,
            "Yaw and a pitched forward vector must not change horizontal travel");
        assertEquals(0F, TrackAnimationPhysics.travelStep(1, 1, 0, 0));
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
