package com.flansmodultimate.client.damage;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PieceGroundingTest
{
    private static final PieceGrounding.Surface FLOOR = (point, ceiling) -> ceiling >= 0D ? 0D : Double.NaN;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void tiltedTrackLiesDownInsteadOfStoppingOnItsLowestCorner()
    {
        Vector3f[] track = box(2D, 0.4D, 0.2D);
        Quaternionf rotation = new Quaternionf().rotateY(0.7F).rotateZ(0.8F).rotateX(0.3F);
        Vec3 pivot = supported(track, rotation, FLOOR);
        double initialHeight = pivot.y;
        for (int tick = 0; tick < 160; tick++)
        {
            PieceGrounding.Settlement settled = PieceGrounding.settle(track, rotation, pivot, FLOOR);
            assertTrue(settled.height() <= pivot.y + 1.0E-6D, "Settling must lower gravitational potential");
            rotation.set(settled.rotation());
            pivot = new Vec3(0D, settled.height(), 0D);
            assertAboveSurface(track, rotation, pivot, FLOOR);
            if (!settled.moved())
                break;
        }
        assertTrue(pivot.y < initialHeight * 0.5D);
        assertTrue(pivot.y < 0.41D, "The track must rest on a face, rather than balancing diagonally");
        assertFalse(PieceGrounding.settle(track, rotation, pivot, FLOOR).moved());
    }

    @Test
    void supportComesFromTerrainUnderThePartRatherThanUnderThePivot()
    {
        Vector3f[] track = box(2D, 0.4D, 0.2D);
        PieceGrounding.Surface ledge = (point, ceiling) -> Math.abs(point.x) > 1D && ceiling >= 1D ? 1D : Double.NaN;
        Quaternionf rotation = new Quaternionf();
        Vec3 pivot = new Vec3(0D, 2D, 0D);
        assertEquals(1.4D, PieceGrounding.supportHeight(track, rotation, pivot, rotation, pivot, ledge), 1.0E-6D);
    }

    @Test
    void aFastFallAndRotationAreBothIncludedInGroundContact()
    {
        Vector3f[] track = box(2D, 0.4D, 0.2D);
        Quaternionf oldRotation = new Quaternionf();
        Quaternionf rotated = new Quaternionf().rotateZ(0.6F);
        Vec3 previous = new Vec3(0D, 5D, 0D);
        Vec3 next = new Vec3(0D, -3D, 0D);
        double support = PieceGrounding.supportHeight(track, rotated, next, oldRotation, previous, FLOOR);
        assertTrue(support > 1D, "The newly rotated corner determines the landing height");
        assertAboveSurface(track, rotated, new Vec3(0D, support, 0D), FLOOR);
    }

    @Test
    void aPieceCanSettleAlongASlopeWithoutPenetratingIt()
    {
        Vector3f[] track = box(2D, 0.4D, 0.2D);
        PieceGrounding.Surface slope = (point, ceiling) -> point.x * 0.25D <= ceiling ? point.x * 0.25D : Double.NaN;
        Quaternionf rotation = new Quaternionf().rotateZ(0.7F);
        Vec3 pivot = supported(track, rotation, slope);
        for (int tick = 0; tick < 100; tick++)
        {
            PieceGrounding.Settlement settled = PieceGrounding.settle(track, rotation, pivot, slope);
            rotation.set(settled.rotation());
            pivot = new Vec3(0D, settled.height(), 0D);
            assertAboveSurface(track, rotation, pivot, slope);
            if (!settled.moved())
                break;
        }
        Vector3f lengthAxis = rotation.transform(new Vector3f(1F, 0F, 0F));
        assertEquals(0.25D, lengthAxis.y / lengthAxis.x, 0.003D);
    }

    @Test
    void missingGroundAndMissingGeometryHaveSafeFallbacks()
    {
        Vector3f[] empty = new Vector3f[0];
        Quaternionf rotation = new Quaternionf();
        assertEquals(0D, PieceGrounding.supportHeight(empty, rotation, Vec3.ZERO, rotation, Vec3.ZERO, FLOOR));
        assertFalse(PieceGrounding.settle(empty, rotation, Vec3.ZERO, FLOOR).moved());
        PieceGrounding.Surface voidBelow = (point, ceiling) -> Double.NaN;
        assertFalse(Double.isFinite(PieceGrounding.supportHeight(box(1D, 1D, 1D), rotation, Vec3.ZERO, rotation, Vec3.ZERO, voidBelow)));
        assertFalse(PieceGrounding.settle(box(1D, 1D, 1D), rotation, Vec3.ZERO, voidBelow).moved());
    }

    @Test
    void stairAndSlabContactsUseTheActualShapeAtTheSamplePoint()
    {
        VoxelShape stair = Shapes.or(Shapes.box(0D, 0D, 0D, 1D, 0.5D, 1D), Shapes.box(0.5D, 0.5D, 0D, 1D, 1D, 1D));
        assertEquals(0.5D, DetachedPieces.surfaceHeight(stair, 0.25D, 0.5D, 2D));
        assertEquals(1D, DetachedPieces.surfaceHeight(stair, 0.75D, 0.5D, 2D));
        assertEquals(0.5D, DetachedPieces.surfaceHeight(Shapes.box(0D, 0D, 0D, 1D, 0.5D, 1D), 0.5D, 0.5D, 0.6D));
        assertTrue(Double.isNaN(DetachedPieces.surfaceHeight(Shapes.box(0.5D, 0D, 0D, 1D, 1D, 1D), 0.25D, 0.5D, 2D)));
        assertTrue(Double.isNaN(DetachedPieces.surfaceHeight(Shapes.block(), 0.5D, 0.5D, 0.5D)), "An overhead block must not pull a piece up");
    }

    @Test
    void initialOverlapCanBeRecoveredWithoutSnappingToAnOverheadBlock()
    {
        ClientLevel level = mock(ClientLevel.class);
        BlockState solid = mock(BlockState.class);
        BlockState air = mock(BlockState.class);
        when(level.getMinBuildHeight()).thenReturn(-64);
        when(solid.getCollisionShape(eq(level), any(BlockPos.class))).thenReturn(Shapes.block());
        when(air.getCollisionShape(eq(level), any(BlockPos.class))).thenReturn(Shapes.empty());
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(invocation ->
        {
            int y = invocation.<BlockPos>getArgument(0).getY();
            return y == -1 || y == 1 ? solid : air;
        });
        assertEquals(0D, DetachedPieces.groundBelow(level, new Vec3(0.5D, -0.05D, 0.5D)));
        assertEquals(0D, DetachedPieces.groundBelow(level, new Vec3(0.5D, 0.1D, 0.5D)));
        Vector3f[] track = box(2D, 0.4D, 0.2D);
        Quaternionf rotation = new Quaternionf();
        Vec3 pivot = new Vec3(0D, 0.35D, 0D);
        double support = PieceGrounding.supportHeight(track, rotation, pivot, rotation, pivot, (point, ceiling) -> DetachedPieces.groundBelow(level, point));
        assertEquals(0.4D, support, 1.0E-6D);
    }

    private static Vec3 supported(Vector3f[] points, Quaternionf rotation, PieceGrounding.Surface surface)
    {
        Vec3 above = new Vec3(0D, 5D, 0D);
        return new Vec3(0D, PieceGrounding.supportHeight(points, rotation, above, rotation, above, surface), 0D);
    }

    private static void assertAboveSurface(Vector3f[] points, Quaternionf rotation, Vec3 pivot, PieceGrounding.Surface surface)
    {
        for (Vector3f point : points)
        {
            Vector3f rotated = rotation.transform(new Vector3f(point));
            Vec3 at = pivot.add(rotated.x, rotated.y, rotated.z);
            double ground = surface.height(at, Double.POSITIVE_INFINITY);
            if (Double.isFinite(ground))
                assertTrue(at.y >= ground - 1.0E-5D, "Every part corner must stay above its supporting terrain");
        }
    }

    private static Vector3f[] box(double x, double y, double z)
    {
        Vector3f[] points = new Vector3f[8];
        for (int corner = 0; corner < points.length; corner++)
            points[corner] = new Vector3f((float) ((corner & 1) == 0 ? -x : x), (float) ((corner & 2) == 0 ? -y : y), (float) ((corner & 4) == 0 ? -z : z));
        return points;
    }
}
