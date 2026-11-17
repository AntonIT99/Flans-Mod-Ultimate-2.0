package com.flansmodultimate.common.distant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistantSyncTest
{
    @Test
    void aStandInIsReportedFromJustInsideWhereItsTrackingEnds()
    {
        // Its own range ends first
        assertEquals(224D, DistantSync.standInTrackedRadius(400D, 256, 80));
        // The view distance ends first
        assertEquals(144D, DistantSync.standInTrackedRadius(176D, 256, 80));
        // Vanilla decides the range of an entity the mod does not manage
        assertEquals(48D, DistantSync.standInTrackedRadius(400D, -1, 80));
        assertEquals(0D, DistantSync.standInTrackedRadius(16D, 256, 80));
    }
}
