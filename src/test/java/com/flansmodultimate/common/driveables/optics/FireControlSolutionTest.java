package com.flansmodultimate.common.driveables.optics;

import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FireControlSolutionTest
{
    private static final ContentPack PACK = new ContentPack("test", Path.of("build", "test-packs", "fire-control"));
    private static final WeaponBallistics SHELL = new WeaponBallistics(10D, 0.05D, 1D, false);

    @Test
    void elevatedShotLandsOnTheLasedPoint()
    {
        float correction = FireControlSolution.elevationCorrection(800D, 0F, SHELL, 20F);
        assertTrue(correction > 0F);
        assertEquals(0D, FireControlSolution.heightAt(800D, correction, SHELL), 0.05D);
        // Uphill and downhill lines of sight are solved along the line, not the horizon
        float uphill = FireControlSolution.elevationCorrection(800D, 5F, SHELL, 20F);
        double horizontal = 800D * Math.cos(Math.toRadians(5D));
        assertEquals(800D * Math.sin(Math.toRadians(5D)), FireControlSolution.heightAt(horizontal, 5F + uphill, SHELL), 0.05D);
    }

    @Test
    void dragNeedsMoreElevationThanGravityAlone()
    {
        WeaponBallistics dragged = new WeaponBallistics(10D, 0.05D, 0.99D, false);
        assertTrue(FireControlSolution.elevationCorrection(800D, 0F, dragged, 20F) > FireControlSolution.elevationCorrection(800D, 0F, SHELL, 20F));
        WeaponBallistics quadratic = new WeaponBallistics(10D, 0.05D, 1D, false, 0.0005D);
        assertTrue(FireControlSolution.elevationCorrection(800D, 0F, quadratic, 20F) > FireControlSolution.elevationCorrection(800D, 0F, SHELL, 20F));
    }

    @Test
    void correctionStopsAtItsLimitAndSkipsWhatCannotBeSolved()
    {
        assertEquals(1F, FireControlSolution.elevationCorrection(4000D, 0F, SHELL, 1F), 1E-6F);
        assertEquals(0F, FireControlSolution.elevationCorrection(-1D, 0F, SHELL, 20F));
        assertEquals(0F, FireControlSolution.elevationCorrection(800D, 0F, new WeaponBallistics(0D, 0.05D, 1D, false), 20F));
        assertEquals(0F, FireControlSolution.elevationCorrection(800D, 0F, new WeaponBallistics(10D, 0.05D, 1D, true), 20F));
        assertEquals(0F, FireControlSolution.elevationCorrection(800D, 0F, SHELL, 0F));
    }

    @Test
    void readsLabjacFireControlKeys()
    {
        VehicleType defaults = new VehicleType();
        defaults.load(new TypeFile("fcsDefault", EnumType.VEHICLE, PACK, List.of("ShortName fcsDefault")));
        assertFalse(defaults.isFireControlAdjustsBarrel());
        assertEquals(400F, defaults.getFireControlMaxRange());
        assertEquals(20F, defaults.getFireControlMaxCorrection());

        VehicleType tank = new VehicleType();
        tank.load(new TypeFile("fcsTank", EnumType.VEHICLE, PACK,
            List.of("ShortName fcsTank", "FCSMode AdjustingBarrel", "FCSMaxRange 4000", "FCSBarrelMaxCorrection 8", "FCSDropScale 0.5")));
        assertTrue(tank.isFireControlAdjustsBarrel());
        assertEquals(4000F, tank.getFireControlMaxRange());
        assertEquals(8F, tank.getFireControlMaxCorrection());
        assertEquals(0.5F, tank.getFireControlDropScale());

        VehicleType sight = new VehicleType();
        sight.load(new TypeFile("fcsSight", EnumType.VEHICLE, PACK, List.of("ShortName fcsSight", "FCSMode AdjustingSight")));
        assertFalse(sight.isFireControlAdjustsBarrel());
    }
}
