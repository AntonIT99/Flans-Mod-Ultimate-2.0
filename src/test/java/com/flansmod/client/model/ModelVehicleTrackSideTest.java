package com.flansmod.client.model;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Static track meshes are flipped at construction, while procedural link points
 * retain their authored lateral sign. Resolve each against the part boxes so
 * destroying one track hides the track that was hit.
 */
class ModelVehicleTrackSideTest
{
    @Test
    void matchingLateralSignsMeanTheNamesAlreadyAgree()
    {
        assertFalse(ModelVehicle.sidesSwapped(-21F, -1.0F));
        assertFalse(ModelVehicle.sidesSwapped(21F, 1.0F));
    }

    @Test
    void oppositeLateralSignsMeanTheTypeFileNamesTheSidesTheOtherWayRound()
    {
        assertTrue(ModelVehicle.sidesSwapped(-21F, 1.0F));
        assertTrue(ModelVehicle.sidesSwapped(21F, -1.0F));
    }

    @Test
    void anUnmeasurableSideKeepsTheAuthoredNames()
    {
        assertFalse(ModelVehicle.sidesSwapped(null, 1.0F));
        assertFalse(ModelVehicle.sidesSwapped(21F, null));
        assertFalse(ModelVehicle.sidesSwapped(21F, 0F), "a shared centre line cannot decide a side");
    }

    @Test
    void proceduralLinkPointsResolveAgainstTheirPhysicalSide()
    {
        // Tiger: LeftLinkPoint z=25, RightLinkPoint z=-25; left/right box centres are -30/+30 pixels.
        boolean swapped = ModelVehicle.pathSidesSwapped(50F, -60F / 16F);
        assertTrue(swapped);
        assertEquals(EnumDriveablePart.RIGHT_TRACK, ModelVehicle.trackPart(true, swapped));
        assertEquals(EnumDriveablePart.LEFT_TRACK, ModelVehicle.trackPart(false, swapped));
        assertFalse(ModelVehicle.pathSidesSwapped(50F, 3.125F));
        assertFalse(ModelVehicle.pathSidesSwapped(null, 3.125F));
    }

    @Test
    void theResolvedPartFollowsTheDrawnSide()
    {
        assertEquals(EnumDriveablePart.LEFT_TRACK, ModelVehicle.trackPart(true, false));
        assertEquals(EnumDriveablePart.RIGHT_TRACK, ModelVehicle.trackPart(false, false));
        assertEquals(EnumDriveablePart.RIGHT_TRACK, ModelVehicle.trackPart(true, true));
        assertEquals(EnumDriveablePart.LEFT_TRACK, ModelVehicle.trackPart(false, true));
    }
}
