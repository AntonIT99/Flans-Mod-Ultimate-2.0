package com.flansmodultimate.compat.sable;

import com.flansmodultimate.common.raytracing.Raytracer.BlockContact;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import dev.ryanhcode.sable.companion.math.Pose3d;
import org.junit.jupiter.api.Test;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SableShotClipperTest
{
    @Test
    void rotatedPanelKeepsItsBlockAddressAndCompetesWithTerrainInWorldSpace() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        var panel = new BlockPos(1000, 0, 1000);
        var level = mock(BlockGetter.class, CALLS_REAL_METHODS);
        doAnswer(call -> panel.equals(call.getArgument(0)) ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState()).when(level).getBlockState(any());
        var pose = new Pose3d();
        pose.rotationPoint().set(1000, 0, 1000);
        pose.position().set(10, 0, 30);
        pose.orientation().rotateY(Math.PI / 2);
        Vec3 from = new Vec3(10.5, 0.5, 35);
        Vec3 to = new Vec3(10.5, 0.5, 25);
        var wallPosition = new Vec3(10.5, 0.5, 27);
        var wall = new BlockContact(new BlockHitResult(wallPosition, Direction.SOUTH, BlockPos.containing(wallPosition), false), wallPosition, new Vec3(0, 0, 1));

        var first = SableShotClipper.clipStructure(level, from, to, pose, pos -> true, wall);
        assertEquals(panel, first.local().getBlockPos());
        assertEquals(0D, first.position().distanceTo(new Vec3(10.5, 0.5, 30)), 1e-7);
        assertEquals(0D, first.normal().distanceTo(new Vec3(0, 0, 1)), 1e-7);
        assertSame(wall, SableShotClipper.clipStructure(level, first.position(), to, pose, pos -> !panel.equals(pos), wall));

        pose.position().z = 26;
        assertSame(wall, SableShotClipper.clipStructure(level, from, to, pose, pos -> true, wall));
        pose.position().x += 2;
        assertSame(wall, SableShotClipper.clipStructure(level, from, to, pose, pos -> true, wall));
    }
}
