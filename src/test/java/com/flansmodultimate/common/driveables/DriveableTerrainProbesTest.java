package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class DriveableTerrainProbesTest
{
    /** A wall filling everything at x >= 10. */
    private static final double WALL_X = 10D;
    private static final Predicate<AABB> IN_WALL = box -> box.maxX > WALL_X;
    private static final BiFunction<AABB, Vec3, Vec3> COLLIDE_WITH_WALL = (box, motion) ->
        motion.x > 0D && box.maxX + motion.x > WALL_X
            ? new Vec3(Math.max(0D, WALL_X - box.maxX), motion.y, motion.z) : motion;

    @Test
    void aProbeAtTheNoseStopsTheWholeDriveableAtTheWall()
    {
        AABB nose = box(9.5D);
        AABB tail = box(2D);
        Vec3 clamped = DriveableTerrainProbes.clampHorizontal(new Vec3(1D, -0.1D, 0D), List.of(nose, tail), IN_WALL, COLLIDE_WITH_WALL);
        assertEquals(0.5D, clamped.x, 1.0E-9D);
        assertEquals(-0.1D, clamped.y, 1.0E-9D, "vertical motion is left to the body and suspension");
        assertTrue(DriveableTerrainProbes.blocked(new Vec3(1D, -0.1D, 0D), clamped));
    }

    @Test
    void motionAlongTheWallIsKeptSoTheDriveableSlides()
    {
        Vec3 clamped = DriveableTerrainProbes.clampHorizontal(new Vec3(1D, 0D, 0.7D), List.of(box(9.5D)), IN_WALL, COLLIDE_WITH_WALL);
        assertEquals(0.5D, clamped.x, 1.0E-9D);
        assertEquals(0.7D, clamped.z, 1.0E-9D);
    }

    @Test
    void movingAwayFromTheWallIsNeverLimited()
    {
        Vec3 requested = new Vec3(-1D, 0D, 0D);
        assertSame(requested, DriveableTerrainProbes.clampHorizontal(requested, List.of(box(9.5D)), IN_WALL, COLLIDE_WITH_WALL));
    }

    @Test
    void aProbeAlreadyInsideTerrainCannotTrapTheDriveable()
    {
        Vec3 requested = new Vec3(1D, 0D, 0D);
        Vec3 clamped = DriveableTerrainProbes.clampHorizontal(requested, List.of(box(10.5D)), IN_WALL, COLLIDE_WITH_WALL);
        assertSame(requested, clamped);
        assertFalse(DriveableTerrainProbes.blocked(requested, clamped));
        assertEquals(1, DriveableTerrainProbes.countInTerrain(List.of(box(10.5D), box(2D)), IN_WALL));
    }

    @Test
    void wheelProbesStartAboveTheClimbableStep()
    {
        AABB probe = DriveableTerrainProbes.wheelProbe(new Vec3(0D, 64D, 0D), DriveableTerrainProbes.stepLift(1D));
        assertEquals(65.05D, probe.minY, 1.0E-9D, "a one block ledge stays climbable");
        assertEquals(0.6D, probe.getXsize(), 1.0E-9D);
        assertEquals(64D + DriveableTerrainProbes.MIN_STEP_LIFT + DriveableTerrainProbes.STEP_CLEARANCE,
            DriveableTerrainProbes.wheelProbe(new Vec3(0D, 64D, 0D), DriveableTerrainProbes.stepLift(0D)).minY, 1.0E-9D,
            "slabs never stop a driveable that cannot step");
    }

    @Test
    void seatProbesNeverReachBelowTheStep()
    {
        assertEquals(66.2D, DriveableTerrainProbes.seatProbe(new Vec3(0D, 66D, 0D), 65.05D).minY, 1.0E-9D);
        assertEquals(65.05D, DriveableTerrainProbes.seatProbe(new Vec3(0D, 64.3D, 0D), 65.05D).minY, 1.0E-9D);
    }

    private static AABB box(double maxX)
    {
        return new AABB(maxX - 0.6D, 65D, -0.3D, maxX, 66D, 0.3D);
    }
}
