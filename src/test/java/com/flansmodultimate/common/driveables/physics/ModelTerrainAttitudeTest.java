package com.flansmodultimate.common.driveables.physics;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.api.driveables.TerrainAttitude;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelTerrainAttitudeTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void uphillDownhillAndCrossSlopesFollowTheHullHeading()
    {
        Harness harness = new Harness(0D, 0.2D);
        float slope = (float) Math.toDegrees(Math.atan(0.2D));
        assertEquals(-slope, harness.settle(0F, 1D).pitch(), 0.03F);
        assertEquals(slope, harness.settle(180F, 1D).pitch(), 0.03F);
        assertEquals(0F, harness.settle(90F, 1D).pitch(), 0.03F);
        assertEquals(slope, harness.settle(90F, 1D).bank(), 0.03F);
        assertEquals(-slope, harness.settle(-90F, 1D).bank(), 0.03F);
    }

    @Test
    void partialBlockHeightsAndScaledModelsKeepTheSameGrade()
    {
        Harness harness = new Harness(0D, 0.125D);
        float slope = (float) Math.toDegrees(Math.atan(0.125D));
        for (double scale : new double[]{0.5D, 1D, 2D})
            assertEquals(-slope, harness.settle(0F, scale).pitch(), 0.03F);
    }

    @Test
    void suspensionBoundsChangesAndSettlesBackToFlatGround()
    {
        Harness harness = new Harness(0D, 0.4D);
        TerrainAttitude first = harness.sample(TerrainAttitude.LEVEL, 0F, 1D);
        assertTrue(first.pitch() < 0F);
        assertTrue(first.pitch() >= -3F);
        TerrainAttitude current = harness.settle(0F, 1D);
        harness.longitudinalSlope = 0D;
        for (int tick = 0; tick < 100; tick++)
            current = harness.sample(current, 0F, 1D);
        assertEquals(0F, current.pitch(), 0.001F);
        assertEquals(0F, current.bank(), 0.001F);
    }

    @Test
    void missingOrUnloadedSupportsRetainTiltAndDoNotLoadChunks()
    {
        Harness harness = new Harness(0D, 0D);
        TerrainAttitude tilted = new TerrainAttitude(-10F, 8F);
        when(harness.level.isLoaded(any())).thenReturn(false);
        assertEquals(tilted, harness.sample(tilted, 0F, 1D));
        verify(harness.level, never()).clip(any());
        when(harness.level.isLoaded(any())).thenReturn(true);
        doReturn(BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO)).when(harness.level).clip(any());
        assertEquals(tilted, harness.sample(tilted, 0F, 1D));
    }

    @Test
    void liftPutsTheWheelsOnTheGroundBelowTheEntity()
    {
        Harness harness = new Harness(0D, 0D);
        harness.groundHeight = -0.75D;
        TerrainAttitude current = harness.settle(0F, 1D);
        assertEquals(-0.75F, current.lift(), 0.01F);
        assertEquals(0F, current.pitch(), 0.01F);
    }

    @Test
    void supportAtOnlyOneEndEasesLevelInsteadOfFreezing()
    {
        Harness harness = new Harness(0D, 0D);
        harness.rearMissing = true;
        TerrainAttitude current = new TerrainAttitude(-10F, 0F);
        for (int tick = 0; tick < 100; tick++)
            current = harness.sample(current, 0F, 1D);
        assertEquals(0F, current.pitch(), 0.01F);
    }

    @Test
    void buriedWheelsStillFindTheTopOfAStep()
    {
        Harness harness = new Harness(0D, 0D);
        harness.groundHeight = 1.3D;
        // Above the old step + 0.1 buried-wheel cutoff, still within the downward probe.
        assertTrue(harness.sample(TerrainAttitude.LEVEL, 0F, 1D).lift() > 0F);
    }

    @Test
    void bodyContactWithoutAnyWheelSupportEasesLevel()
    {
        Harness harness = new Harness(0D, 0D);
        when(harness.entity.onGround()).thenReturn(true);
        doReturn(BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO)).when(harness.level).clip(any());
        TerrainAttitude current = new TerrainAttitude(-10F, 8F, -0.5F);
        for (int tick = 0; tick < 100; tick++)
            current = harness.sample(current, 0F, 1D);
        assertEquals(0F, current.pitch(), 0.01F);
        assertEquals(0F, current.bank(), 0.01F);
        assertEquals(0F, current.lift(), 0.01F);
    }

    @Test
    void movingVehiclesProbeAheadWithoutChangingTheSlope()
    {
        Harness harness = new Harness(0D, 0.2D);
        when(harness.entity.getDeltaMovement()).thenReturn(new Vec3(0D, 0D, 0.5D));
        TerrainAttitude settled = harness.settle(0F, 1D);
        assertEquals(-Math.toDegrees(Math.atan(0.2D)), settled.pitch(), 0.03D);
        assertEquals(0.1F, settled.lift(), 0.01F);
    }

    @Test
    void rollPermissionAndDefinitionsWithoutWheelsAreRespected()
    {
        Harness harness = new Harness(0.2D, 0.1D);
        when(harness.type.isCanRoll()).thenReturn(false);
        TerrainAttitude current = harness.settle(0F, 1D);
        assertTrue(current.pitch() < 0F);
        assertEquals(0F, current.bank());
        when(harness.type.getWheelPositions()).thenReturn(List.of());
        assertEquals(TerrainAttitude.LEVEL, harness.sample(current, 0F, 1D));
        assertEquals(TerrainAttitude.LEVEL, FlansEntityTypes.sampleTerrainAttitude(harness.entity, harness.type, point -> point, Double.NaN, current));
    }

    private static final class Harness
    {
        private final Level level = mock(Level.class);
        private final Entity entity = mock(Entity.class);
        private final VehicleType type = mock(VehicleType.class);
        private final double lateralSlope;
        private double longitudinalSlope;
        private double groundHeight;
        private boolean rearMissing;

        private Harness(double lateral, double longitudinal)
        {
            when(entity.getDeltaMovement()).thenReturn(Vec3.ZERO);
            lateralSlope = lateral;
            longitudinalSlope = longitudinal;
            when(entity.level()).thenReturn(level);
            when(level.isLoaded(any())).thenReturn(true);
            when(type.getModelScale()).thenReturn(1F);
            when(type.getWheelSpringStrength()).thenReturn(0.5F);
            when(type.getWheelStepHeight()).thenReturn(1F);
            when(type.isCanRoll()).thenReturn(true);
            when(type.getWheelPositions()).thenReturn(List.of(wheel(2F, 1F), wheel(2F, -1F), wheel(-2F, 1F), wheel(-2F, -1F)));
            when(level.clip(any())).thenAnswer(call ->
            {
                ClipContext ray = call.getArgument(0);
                Vec3 start = ray.getFrom();
                Vec3 end = ray.getTo();
                Vec3 surface = new Vec3(start.x, groundHeight + lateralSlope * start.x + longitudinalSlope * start.z, start.z);
                // At yaw 0 the nose points to +Z.
                if (rearMissing && start.z < 0D || surface.y > start.y || surface.y < end.y)
                    return BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end));
                return new BlockHitResult(surface, Direction.UP, BlockPos.containing(surface), false);
            });
        }

        private static DriveablePosition wheel(float forward, float right)
        {
            return new DriveablePosition(new Vector3f(forward, 0F, right), null);
        }

        private TerrainAttitude sample(TerrainAttitude current, float yaw, double scale)
        {
            return FlansEntityTypes.sampleTerrainAttitude(entity, type, point -> point.zRot((float) Math.toRadians(current.bank())).xRot((float) Math.toRadians(current.pitch())).scale(scale)
                .yRot((float) Math.toRadians(180F - yaw)).add(0D, current.lift(), 0D), scale, current);
        }

        private TerrainAttitude settle(float yaw, double scale)
        {
            TerrainAttitude current = TerrainAttitude.LEVEL;
            for (int tick = 0; tick < 100; tick++)
                current = sample(current, yaw, scale);
            return current;
        }
    }
}
