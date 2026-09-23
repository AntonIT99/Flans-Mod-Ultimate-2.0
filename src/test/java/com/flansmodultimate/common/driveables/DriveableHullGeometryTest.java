package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableHullGeometryTest
{
    private static final double EPSILON = 1.0E-6D;
    private static final double HALF_WIDTH = 0.3D;
    private static final double HEIGHT = 1.8D;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final double ORIGIN_Y = 10D;
    /** Deck top sits one block above the driveable origin; world X spans [-1.5, 1.5] and Z [-2, 2] at yaw 0. */
    private static final double DECK_TOP = ORIGIN_Y + 1D;

    @Test
    void fallingPlayerLandsFlushOnDeckAtAnyHeading()
    {
        // The driveable basis is mirrored, which once flipped every deck normal
        // downward and let players fall straight through.
        for (float yaw : new float[] {0F, 33F, 90F, 180F, -127F})
        {
            DriveableHullGeometry geometry = posed(yaw, 0F, deck(-2F, 4F));

            assertEquals(-0.05D, clipPlayer(geometry, Y, 0.3D, DECK_TOP + 0.05D, 0.2D, -0.08D), EPSILON);
            assertEquals(-0.08D, clipPlayer(geometry, Y, 0.3D, DECK_TOP + 0.5D, 0.2D, -0.08D), EPSILON);
        }
    }

    @Test
    void fastFallCannotTunnelThroughDeck()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));

        assertEquals(-3D, clipPlayer(geometry, Y, 0D, DECK_TOP + 3D, 0D, -6D), EPSILON);
    }

    @Test
    void tiltedDeckStopsFallAtItsSurface()
    {
        float pitch = 10F;
        DriveableHullGeometry geometry = posed(0F, pitch, deck(-2F, 4F));
        double radians = Math.toRadians(pitch);
        // The deck normal is (0, cos, sin); the box corner at -Z touches it first.
        double contactFeet = ORIGIN_Y + (1D + Math.sin(radians) * HALF_WIDTH) / Math.cos(radians);
        double feet = ORIGIN_Y + 2D;

        assertEquals(contactFeet - feet, clipPlayer(geometry, Y, 0D, feet, 0D, -3D), EPSILON);
    }

    @Test
    void playerRestingOnDeckWalksFreelyAcrossSeams()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 2F), deck(0F, 2F));

        for (double feet : new double[] {DECK_TOP, DECK_TOP - 5.0E-7D})
        {
            assertEquals(1D, clipPlayer(geometry, Z, 0D, feet, -0.5D, 1D), EPSILON);
            assertEquals(-0.4D, clipPlayer(geometry, X, 0D, feet, -0.5D, -0.4D), EPSILON);
            assertEquals(0D, clipPlayer(geometry, Y, 0D, feet, -0.5D, -0.08D), EPSILON);
        }
    }

    @Test
    void hullSideStopsWalkingPlayerFlush()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));

        assertEquals(-0.2D, clipPlayer(geometry, X, 2D, ORIGIN_Y + 0.2D, 0D, -0.5D), EPSILON);
        assertEquals(0.5D, clipPlayer(geometry, X, 2D, ORIGIN_Y + 0.2D, 0D, 0.5D), EPSILON);
    }

    @Test
    void deeplyEmbeddedPlayerCanEscape()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));

        assertEquals(0.3D, clipPlayer(geometry, X, 0D, ORIGIN_Y + 0.1D, 0D, 0.3D), EPSILON);
        assertEquals(-0.08D, clipPlayer(geometry, Y, 0D, ORIGIN_Y + 0.1D, 0D, -0.08D), EPSILON);
        assertEquals(0.5D, clipPlayer(geometry, Y, 0D, ORIGIN_Y + 0.1D, 0D, 0.5D), EPSILON);
    }

    @Test
    void shallowlySunkPlayerStopsSinkingButCanRise()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));

        assertEquals(0D, clipPlayer(geometry, Y, 0D, DECK_TOP - 0.1D, 0D, -0.08D), EPSILON);
        assertEquals(0.2D, clipPlayer(geometry, Y, 0D, DECK_TOP - 0.1D, 0D, 0.2D), EPSILON);
    }

    @Test
    void destroyedPartsDoNotCollide()
    {
        DriveableHullGeometry geometry = new DriveableHullGeometry(DriveableCollisionProfile.of(List.of(deck(-2F, 4F))));
        geometry.update(0D, ORIGIN_Y, 0D, 0F, 0F, 0F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> false, false);

        assertFalse(geometry.hasActiveShapes());
        assertEquals(-0.08D, clipPlayer(geometry, Y, 0D, DECK_TOP + 0.01D, 0D, -0.08D), EPSILON);
    }

    @Test
    void standingPlayerIsCarriedWithTranslatingDeck()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));
        pose(geometry, 0.5D, ORIGIN_Y + 0.25D, -0.2D, 0F, 0F, true);
        int support = supportUnderPlayer(geometry, 0.2D, DECK_TOP, 0.1D);
        double[] carried = new double[3];

        assertTrue(support >= 0);
        assertTrue(geometry.carryPoint(support, 0.2D, DECK_TOP, 0.1D, carried));
        assertEquals(0.7D, carried[0], EPSILON);
        assertEquals(DECK_TOP + 0.25D, carried[1], EPSILON);
        assertEquals(-0.1D, carried[2], EPSILON);
        assertEquals(0F, geometry.carryYaw(support), 1.0E-4F);
    }

    @Test
    void standingPlayerTurnsWithYawingDeck()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));
        pose(geometry, 0D, ORIGIN_Y, 0D, 10F, 0F, true);
        int support = supportUnderPlayer(geometry, 1D, DECK_TOP, 0.5D);
        double[] carried = new double[3];

        assertTrue(support >= 0);
        assertTrue(geometry.carryPoint(support, 1D, DECK_TOP, 0.5D, carried));
        float yaw = geometry.carryYaw(support);
        assertEquals(10F, yaw, 1.0E-3F);
        // Vanilla yaw turns a horizontal offset (x, z) towards (-sin, cos).
        double length = Math.hypot(1D, 0.5D);
        double heading = Math.atan2(-1D, 0.5D) + Math.toRadians(yaw);
        assertEquals(-Math.sin(heading) * length, carried[0], EPSILON);
        assertEquals(DECK_TOP, carried[1], EPSILON);
        assertEquals(Math.cos(heading) * length, carried[2], EPSILON);
    }

    @Test
    void parkedDeckKeepsCarryingInPlace()
    {
        DriveableHullGeometry geometry = posed(25F, 0F, deck(-2F, 4F));
        pose(geometry, 0D, ORIGIN_Y, 0D, 25F, 0F, true);
        int support = supportUnderPlayer(geometry, 0.4D, DECK_TOP, -0.3D);
        double[] carried = new double[3];

        assertTrue(support >= 0);
        assertTrue(geometry.carryPoint(support, 0.4D, DECK_TOP, -0.3D, carried));
        assertEquals(0.4D, carried[0], EPSILON);
        assertEquals(DECK_TOP, carried[1], EPSILON);
        assertEquals(-0.3D, carried[2], EPSILON);
    }

    @Test
    void playerAgainstHullWallIsNotSupported()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));
        pose(geometry, 0D, ORIGIN_Y, 0D, 0F, 0F, true);

        assertEquals(-1, supportUnderPlayer(geometry, 1.79D, ORIGIN_Y + 0.2D, 0D));
    }

    @Test
    void teleportedDeckDoesNotCarry()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));
        pose(geometry, 0.5D, ORIGIN_Y, 0D, 0F, 0F, false);

        assertEquals(-1, supportUnderPlayer(geometry, 0D, DECK_TOP, 0D));
    }

    @Test
    void penetrationPushesOutThroughNearestFace()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));
        double[] push = new double[4];

        assertTrue(findPlayerPenetration(geometry, 0D, DECK_TOP - 0.1D, 0D, false, push));
        assertArrayEquals(new double[] {0D, 1D, 0D, 0.1D}, push, EPSILON);

        assertTrue(findPlayerPenetration(geometry, 1.7D, ORIGIN_Y + 0.2D, 0D, false, push));
        assertArrayEquals(new double[] {1D, 0D, 0D, 0.1D}, push, EPSILON);
        assertTrue(findPlayerPenetration(geometry, 1.7D, ORIGIN_Y + 0.2D, 0D, true, push));
        assertArrayEquals(new double[] {0D, 1D, 0D, 0.8D}, push, EPSILON);

        assertFalse(findPlayerPenetration(geometry, 0D, DECK_TOP, 0D, false, push));
    }

    @Test
    void touchingIsNotIntersecting()
    {
        DriveableHullGeometry geometry = posed(0F, 0F, deck(-2F, 4F));

        assertFalse(geometry.intersects(-HALF_WIDTH, DECK_TOP, -HALF_WIDTH, HALF_WIDTH, DECK_TOP + HEIGHT, HALF_WIDTH));
        assertTrue(geometry.intersects(-HALF_WIDTH, DECK_TOP - 0.55D, -HALF_WIDTH, HALF_WIDTH, DECK_TOP + HEIGHT,
            HALF_WIDTH));
    }

    private static DriveableCollisionProfile.Shape deck(float localX, float length)
    {
        return DriveableCollisionProfile.compilePartBox(EnumDriveablePart.CORE,
            CollisionBox.inWorldUnits(100F, localX, 0F, -1.5F, length, 1F, 3F, 5F, 0F));
    }

    private static DriveableHullGeometry posed(float yaw, float pitch, DriveableCollisionProfile.Shape... shapes)
    {
        DriveableHullGeometry geometry = new DriveableHullGeometry(DriveableCollisionProfile.of(List.of(shapes)));
        pose(geometry, 0D, ORIGIN_Y, 0D, yaw, pitch, false);
        return geometry;
    }

    private static void pose(DriveableHullGeometry geometry, double x, double y, double z, float yaw, float pitch,
                             boolean continuous)
    {
        geometry.update(x, y, z, yaw, pitch, 0F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true, continuous);
    }

    private static double clipPlayer(DriveableHullGeometry geometry, int axis, double x, double feet, double z,
                                     double desired)
    {
        return geometry.clip(axis, x - HALF_WIDTH, feet, z - HALF_WIDTH, x + HALF_WIDTH, feet + HEIGHT,
            z + HALF_WIDTH, desired);
    }

    private static int supportUnderPlayer(DriveableHullGeometry geometry, double x, double feet, double z)
    {
        return geometry.findSupport(x - HALF_WIDTH, feet, z - HALF_WIDTH, x + HALF_WIDTH, feet + HEIGHT,
            z + HALF_WIDTH);
    }

    private static boolean findPlayerPenetration(DriveableHullGeometry geometry, double x, double feet, double z,
                                                 boolean upwardOnly, double[] out)
    {
        return geometry.findPenetration(x - HALF_WIDTH, feet, z - HALF_WIDTH, x + HALF_WIDTH, feet + HEIGHT,
            z + HALF_WIDTH, upwardOnly, out);
    }
}
