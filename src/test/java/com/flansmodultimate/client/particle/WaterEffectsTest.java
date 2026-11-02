package com.flansmodultimate.client.particle;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WaterEffectsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void shallowImpactsReachTheSurfaceButDeepExplosionsDoNot()
    {
        Level level = mock(Level.class);
        FluidState water = mock(FluidState.class);
        when(water.is(FluidTags.WATER)).thenReturn(true);
        FluidState air = mock(FluidState.class);
        when(level.hasChunkAt(any(BlockPos.class))).thenReturn(true);
        when(level.getFluidState(any(BlockPos.class))).thenAnswer(invocation -> ((BlockPos) invocation.getArgument(0)).getY() < 64 ? water : air);
        assertEquals(64D, WaterEffects.surface(level, new Vec3(0.5D, 62.5D, 0.5D), 3));
        assertTrue(Double.isNaN(WaterEffects.surface(level, new Vec3(0.5D, 50.5D, 0.5D), 6)));
        assertTrue(Double.isNaN(WaterEffects.surface(level, new Vec3(0.5D, 65.5D, 0.5D), 6)));
    }

    @Test
    void missingChunksAreNeverReadOrLoaded()
    {
        Level level = mock(Level.class);
        assertTrue(Double.isNaN(WaterEffects.surface(level, Vec3.ZERO, 6)));
        verify(level, never()).getFluidState(any(BlockPos.class));
    }
}
