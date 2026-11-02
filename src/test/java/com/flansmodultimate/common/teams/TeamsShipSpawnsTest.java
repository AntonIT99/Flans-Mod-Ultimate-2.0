package com.flansmodultimate.common.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsShipSpawnsTest
{
    private static final Vec3 ORIGIN = new Vec3(0.5D, 8D, 0.5D);

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void choosesNearestSurfaceInThreeDimensionsIncludingBelowTheOrigin()
    {
        World world = new World();
        world.water(5, 7, 0);
        world.water(1, 0, 0);
        world.water(2, 6, 0);
        assertEquals(new Vec3(2.5D, 7D, 0.5D), TeamsShipSpawns.find(world.level, ORIGIN).orElseThrow());
    }

    @Test
    void includes64BlocksButRejectsMoreDistantWater()
    {
        World world = new World();
        world.water(65, 7, 0);
        assertTrue(TeamsShipSpawns.find(world.level, ORIGIN).isEmpty());
        world.water(64, 7, 0);
        assertEquals(new Vec3(64.5D, 8D, 0.5D), TeamsShipSpawns.find(world.level, ORIGIN).orElseThrow());
        world.water(64, 8, 0);
        assertTrue(TeamsShipSpawns.find(world.level, ORIGIN).isEmpty());
    }

    @Test
    void rejectsCoveredWaterAndWaterloggedSolidBlocks()
    {
        World world = new World();
        world.water(0, 7, 0);
        world.blocks.put(new BlockPos(0, 8, 0), world.solid(false));
        world.blocks.put(new BlockPos(1, 7, 0), world.solid(true));
        assertTrue(TeamsShipSpawns.find(world.level, ORIGIN).isEmpty());
    }

    @Test
    void usesTopOfWaterColumnAndDoesNotLoadMissingChunks()
    {
        World world = new World();
        world.water(0, 6, 0);
        world.water(0, 7, 0);
        assertEquals(ORIGIN, TeamsShipSpawns.find(world.level, ORIGIN).orElseThrow());
        verify(world.level, never()).getChunk(anyInt(), anyInt());
        verify(world.chunks, never()).getChunk(anyInt(), anyInt(), any(), anyBoolean());
    }

    @Test
    void noLoadedWaterMeansNoSpawn()
    {
        World world = new World();
        assertTrue(TeamsShipSpawns.find(world.level, ORIGIN).isEmpty());
        verify(world.level, never()).getBlockState(any());
    }

    @Test
    void searchesNegativeChunkCoordinatesAndUsesASphereRatherThanACube()
    {
        World world = new World();
        world.water(-50, 7, -50);
        assertTrue(TeamsShipSpawns.find(world.level, ORIGIN).isEmpty());
        world.water(-2, 7, -3);
        assertEquals(new Vec3(-1.5D, 8D, -2.5D), TeamsShipSpawns.find(world.level, ORIGIN).orElseThrow());
    }

    private static final class World
    {
        private final ServerLevel level = mock(ServerLevel.class);
        private final ServerChunkCache chunks = mock(ServerChunkCache.class);
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private final Map<Long, LevelChunk> loaded = new HashMap<>();
        private final BlockState air = state(false);
        private final BlockState water = state(true);

        private World()
        {
            when(level.getChunkSource()).thenReturn(chunks);
            when(chunks.getChunkNow(anyInt(), anyInt())).thenAnswer(call -> loaded.get(ChunkPos.asLong(call.getArgument(0), call.getArgument(1))));
            when(level.getBlockState(any())).thenAnswer(call -> blocks.getOrDefault(call.getArgument(0, BlockPos.class), air));
        }

        private void water(int x, int y, int z)
        {
            blocks.put(new BlockPos(x, y, z), water);
            loaded.computeIfAbsent(ChunkPos.asLong(x >> 4, z >> 4), key ->
            {
                LevelChunk chunk = mock(LevelChunk.class);
                LevelChunkSection section = mock(LevelChunkSection.class);
                when(chunk.getSections()).thenReturn(new LevelChunkSection[]{section});
                when(section.maybeHas(any())).thenReturn(true);
                when(section.getBlockState(anyInt(), anyInt(), anyInt()))
                    .thenAnswer(call -> blocks.getOrDefault(new BlockPos(((x >> 4) << 4) + (int) call.getArgument(0), call.getArgument(1), ((z >> 4) << 4) + (int) call.getArgument(2)), air));
                return chunk;
            });
        }

        private BlockState solid(boolean waterlogged)
        {
            BlockState state = state(waterlogged);
            when(state.getCollisionShape(eq(level), any())).thenReturn(Shapes.block());
            return state;
        }

        private BlockState state(boolean wet)
        {
            BlockState state = mock(BlockState.class);
            FluidState fluid = mock(FluidState.class);
            when(state.getFluidState()).thenReturn(fluid);
            when(fluid.is(FluidTags.WATER)).thenReturn(wet);
            when(fluid.isEmpty()).thenReturn(!wet);
            when(state.getCollisionShape(eq(level), any())).thenReturn(Shapes.empty());
            return state;
        }
    }
}
