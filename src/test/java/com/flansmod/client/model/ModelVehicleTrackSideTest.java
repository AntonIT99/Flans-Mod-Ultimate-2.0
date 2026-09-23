package com.flansmod.client.model;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Track meshes and link points are drawn mirrored against the lateral axis the
 * part boxes use, so the two sides agree only when their lateral offsets have
 * opposite signs. Type files that name the sides the other way round have to be
 * detected here, or destroying one track would hide the other.
 */
class ModelVehicleTrackSideTest
{
    @Test
    void oppositeLateralSignsMeanTheNamesAlreadyAgree()
    {
        assertFalse(ModelVehicle.sidesSwapped(-21F, 1.0F), "left mesh mirrors the left box");
        assertFalse(ModelVehicle.sidesSwapped(21F, -1.0F));
    }

    @Test
    void matchingLateralSignsMeanTheTypeFileNamesTheSidesTheOtherWayRound()
    {
        assertTrue(ModelVehicle.sidesSwapped(-21F, -1.0F));
        assertTrue(ModelVehicle.sidesSwapped(21F, 1.0F));
    }

    @Test
    void anUnmeasurableSideKeepsTheAuthoredNames()
    {
        assertFalse(ModelVehicle.sidesSwapped(null, 1.0F));
        assertFalse(ModelVehicle.sidesSwapped(21F, null));
        assertFalse(ModelVehicle.sidesSwapped(21F, 0F), "a shared centre line cannot decide a side");
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
