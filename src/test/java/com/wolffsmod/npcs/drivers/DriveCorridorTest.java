package com.wolffsmod.npcs.drivers;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class DriveCorridorTest
{
    /** A house from x 4 to 7 and z 2 to 7, two blocks high on ground at y 64. */
    private static final DriveCorridor.Solid HOUSE = (x, y, z) -> x >= 4 && x <= 7 && z >= 2 && z <= 7 && y >= 65 && y <= 66;
    private static final DriveCorridor.Solid OPEN = (x, y, z) -> false;

    @Test
    void aHullDrivesStraightAcrossOpenGround()
    {
        assertTrue(DriveCorridor.clear(new Vec3(0.5D, 64D, 0.5D), new Vec3(10.5D, 64D, 10.5D), 4D, OPEN));
    }

    @Test
    void cuttingTheCornerOfAHouseRunsIntoItsWall()
    {
        // The path goes round the house's corner; the straight way to a point beyond it does not.
        assertFalse(DriveCorridor.clear(new Vec3(2D, 64D, 9D), new Vec3(9D, 64D, 0D), 2D, HOUSE));
    }

    @Test
    void aWideHullBrushesAWallANarrowOnePasses()
    {
        // Driving along the house's side, two and a half blocks off its wall.
        Vec3 from = new Vec3(0.5D, 64D, 10.5D);
        Vec3 to = new Vec3(12.5D, 64D, 10.5D);
        assertTrue(DriveCorridor.clear(from, to, 2D, HOUSE));
        assertFalse(DriveCorridor.clear(from, to, 7D, HOUSE));
    }

    @Test
    void aWallFaceTheHullOnlyTouchesDoesNotBlockIt()
    {
        // The hull's side lies exactly along the house's wall at z 8.
        assertTrue(DriveCorridor.clear(new Vec3(0.5D, 64D, 9D), new Vec3(12.5D, 64D, 9D), 2D, HOUSE));
    }

    @Test
    void aSingleStepUpIsDrivenOverButAWallIsNot()
    {
        DriveCorridor.Solid step = (x, y, z) -> x >= 5 && y <= 64;
        // The ground rises by one block at x 5, where the path rises with it.
        assertTrue(DriveCorridor.clear(new Vec3(0.5D, 64D, 0.5D), new Vec3(8.5D, 65D, 0.5D), 2D, step));
        DriveCorridor.Solid wall = (x, y, z) -> x == 5 && y >= 65 && y <= 66;
        assertFalse(DriveCorridor.clear(new Vec3(0.5D, 64D, 0.5D), new Vec3(8.5D, 64D, 0.5D), 2D, wall));
    }

    @Test
    void whatTheHullStandsOnOrOverhangsHighAboveDoesNotBlockIt()
    {
        DriveCorridor.Solid groundAndRoof = (x, y, z) -> y <= 64 || y >= 68;
        assertTrue(DriveCorridor.clear(new Vec3(0.5D, 64D, 0.5D), new Vec3(8.5D, 64D, 4.5D), 3D, groundAndRoof));
    }
}
