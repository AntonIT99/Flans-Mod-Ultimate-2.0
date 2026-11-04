package com.wolffsmod.npcs.sim;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** How the watercraft path filter spreads its depth samples over a hull's footprint. */
class NpcWaterDepthSamplingTest
{
    @Test
    void aNarrowHullIsStillTestedColumnByColumn()
    {
        assertEquals(1, NpcMobility.samplesPerSide(1));
        assertEquals(2, NpcMobility.samplesPerSide(2));
        assertEquals(3, NpcMobility.samplesPerSide(3));
    }

    @Test
    void aWideHullCostsABoundedNumberOfSamplesRatherThanTheSquareOfItsWidth()
    {
        assertEquals(5, NpcMobility.samplesPerSide(9));
        assertEquals(5, NpcMobility.samplesPerSide(40), "a battleship costs no more than a destroyer");
        assertTrue(NpcMobility.samplesPerSide(40) * NpcMobility.samplesPerSide(40) < 40 * 40);
    }

    @Test
    void theSamplesSpanTheWholeFootprintFromItsFirstColumnToItsLast()
    {
        int footprint = 9;
        int samples = NpcMobility.samplesPerSide(footprint);
        assertEquals(0, NpcMobility.sampleOffset(0, samples, footprint), "the first corner");
        assertEquals(footprint - 1, NpcMobility.sampleOffset(samples - 1, samples, footprint), "the far corner");
        Set<Integer> columns = new HashSet<>();
        for (int sample = 0; sample < samples; sample++)
        {
            int column = NpcMobility.sampleOffset(sample, samples, footprint);
            assertTrue(column >= 0 && column < footprint, "every sample falls inside the footprint");
            assertTrue(columns.add(column), "no two samples share a column");
        }
    }

    @Test
    void aSingleSampleIsTakenUnderTheMiddleOfTheHull()
    {
        assertEquals(0, NpcMobility.sampleOffset(0, 1, 1));
        assertEquals(1, NpcMobility.sampleOffset(0, 1, 3));
    }

    @Test
    void everyColumnOfANarrowFootprintIsCovered()
    {
        int footprint = 3;
        int samples = NpcMobility.samplesPerSide(footprint);
        for (int column = 0; column < footprint; column++)
        {
            int sample = column;
            assertEquals(column, NpcMobility.sampleOffset(sample, samples, footprint));
        }
    }
}
