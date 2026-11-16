package com.flansmodultimate.client.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundHelperTest
{
    @Test
    void followingSoundFadesOutWhereTheViewDistanceDropsItsSource()
    {
        // A 512-block plane engine on a server with a 10-chunk view distance: the client loses the plane at 160 blocks.
        assertEquals(160F, SoundHelper.audibleRange(512F, 512, 10));
        assertEquals(10F, SoundHelper.getVolumeFromRange(SoundHelper.audibleRange(512F, 512, 10), false));
    }

    @Test
    void followingSoundFadesOutAtTheSourceTrackingRange()
    {
        assertEquals(256F, SoundHelper.audibleRange(512F, 256, 32));
    }

    @Test
    void shortSoundKeepsItsOwnRange()
    {
        assertEquals(64F, SoundHelper.audibleRange(64F, 512, 10));
    }
}
