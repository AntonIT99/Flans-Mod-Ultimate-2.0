package com.flansmodultimate.common.raytracing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LagCompensationTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks Forge's NetworkEvent transformer; vanilla registries initialize first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
    }

    @Test
    void playerShotsRewindByPingAndClientInterpolation()
    {
        assertEquals(LagCompensation.CLIENT_INTERPOLATION_TICKS, LagCompensation.rewindTicks(0, 0, 50));
        assertEquals(2 + LagCompensation.CLIENT_INTERPOLATION_TICKS, LagCompensation.rewindTicks(100, 0, 50));
        assertEquals(1 + 2 + LagCompensation.CLIENT_INTERPOLATION_TICKS, LagCompensation.rewindTicks(100, 1, 50));
        // A divisor of 0 ignores the ping
        assertEquals(LagCompensation.CLIENT_INTERPOLATION_TICKS, LagCompensation.rewindTicks(400, 0, 0));
    }

    @Test
    void entityHistoryReturnsBoxesNearestTheRewoundTimeFirst()
    {
        Level level = mock(Level.class);
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.level()).thenReturn(level);
        for (long time = 100; time <= 110; time++)
        {
            when(level.getGameTime()).thenReturn(time);
            when(entity.getBoundingBox()).thenReturn(new AABB(time, 0D, 0D, time + 1D, 2D, 1D));
            EntityPositionHistory.record(entity);
        }

        List<AABB> boxes = LagCompensation.entityBoxes(entity, 111L, 5);
        assertEquals(3, boxes.size());
        assertEquals(106D, boxes.get(0).minX);
        assertEquals(List.of(105D, 107D), boxes.subList(1, 3).stream().map(box -> box.minX).sorted().toList());

        // Shots aimed on the server test the entity where it is now
        assertTrue(LagCompensation.entityBoxes(entity, 111L, 0).isEmpty());
        // Nothing recorded that long ago
        assertTrue(LagCompensation.entityBoxes(entity, 111L, 40).isEmpty());
    }

    @Test
    void fencesStopShotsWhereTheyAreDrawnAndPlantsLetThemThrough()
    {
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        assertEquals(1.5D, fence.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).max(Direction.Axis.Y), 1e-6);
        assertEquals(1D, Raytracer.shotShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, fence).max(Direction.Axis.Y), 1e-6);
        assertEquals(Shapes.empty(), Raytracer.shotShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Blocks.GRASS.defaultBlockState()));

        BlockGetter world = mock(BlockGetter.class, CALLS_REAL_METHODS);
        doAnswer(call -> BlockPos.ZERO.equals(call.getArgument(0)) ? fence : Blocks.AIR.defaultBlockState()).when(world).getBlockState(any());

        // Fired across the fence just above its post, inside the collision box that reaches higher
        assertEquals(HitResult.Type.MISS, Raytracer.clipBlocks(world, new Vec3(-2D, 1.25D, 0.5D), new Vec3(3D, 1.25D, 0.5D)).getType());
        assertEquals(HitResult.Type.BLOCK, Raytracer.clipBlocks(world, new Vec3(-2D, 0.5D, 0.5D), new Vec3(3D, 0.5D, 0.5D)).getType());
    }
}
