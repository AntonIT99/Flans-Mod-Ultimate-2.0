package com.flansmodultimate.client.model;

import com.flansmod.common.vector.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A twin or quad mount fired from one point spreads its shots over the model's
 * barrels, round wherever the type puts that point, so a pack trusted as written
 * keeps its muzzle and only gains the barrels either side of it.
 */
class MuzzleMeasurementsBarrelSpreadTest
{
    /** The Manus Flakpanzer IV's four barrels, in type-file pixels. */
    private static final List<Vector3f> FLAKVIERLING = List.of(
        new Vector3f(37F, 38.5F, 6F), new Vector3f(37F, 42.5F, 6F),
        new Vector3f(37F, 42.5F, -6F), new Vector3f(37F, 38.5F, -6F));

    private static void assertVector(Vector3f expected, Vector3f actual)
    {
        assertEquals(expected.x, actual.x, 1.0E-4F);
        assertEquals(expected.y, actual.y, 1.0E-4F);
        assertEquals(expected.z, actual.z, 1.0E-4F);
    }

    @Test
    void aPointForTheWholeMountSpreadsRoundTheMiddleOfItsBarrels()
    {
        // BarrelPosition 0 40 0: at the turret pivot, between all four barrels.
        List<Vector3f> offsets = MuzzleMeasurements.barrelSpread(FLAKVIERLING, new Vector3f(0F, 40F, 0F));

        assertEquals(4, offsets.size());
        assertVector(new Vector3f(0F, -2F, 6F), offsets.get(0));
        assertVector(new Vector3f(0F, 2F, -6F), offsets.get(2));
    }

    @Test
    void aPointOnOneBarrelIsThatBarrel()
    {
        List<Vector3f> twin = List.of(new Vector3f(104F, 5.9F, -2.5F), new Vector3f(104F, 5.9F, 2.5F));

        // Authored on the left gun, and short of the muzzle, as a trusted pack may be.
        List<Vector3f> offsets = MuzzleMeasurements.barrelSpread(twin, new Vector3f(90F, 6F, -2.4F));

        assertVector(new Vector3f(0F, 0F, 0F), offsets.get(0));
        assertVector(new Vector3f(0F, 0F, 5F), offsets.get(1));
    }

    @Test
    void aSingleBarrelHasNothingToSpread()
    {
        assertTrue(MuzzleMeasurements.barrelSpread(List.of(new Vector3f(10F, 20F, 0F)), new Vector3f()).isEmpty());
        assertTrue(MuzzleMeasurements.barrelSpread(List.of(), new Vector3f()).isEmpty());
    }
}
