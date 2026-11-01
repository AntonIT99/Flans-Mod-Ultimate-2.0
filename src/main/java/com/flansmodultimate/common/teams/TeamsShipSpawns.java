package com.flansmodultimate.common.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Shared water placement for Teams ships and addon ship bots. Server thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsShipSpawns
{
    private static final int SEARCH_RADIUS = 64;

    /** Finds the nearest unobstructed water surface within 64 blocks, without loading chunks. */
    public static Optional<Vec3> find(ServerLevel level, Vec3 origin)
    {
        return new Search(origin).find(level);
    }

    private static final class Search
    {
        private final Vec3 origin;
        private final BlockPos min;
        private final BlockPos max;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        @Nullable
        private Vec3 nearest;
        private double distance = (double) SEARCH_RADIUS * SEARCH_RADIUS;

        private Search(Vec3 origin)
        {
            this.origin = origin;
            min = BlockPos.containing(origin.add(-SEARCH_RADIUS, -SEARCH_RADIUS - 1D, -SEARCH_RADIUS));
            max = BlockPos.containing(origin.add(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS));
        }

        private Optional<Vec3> find(ServerLevel level)
        {
            for (int chunkX = min.getX() >> 4; chunkX <= max.getX() >> 4; chunkX++)
            {
                for (int chunkZ = min.getZ() >> 4; chunkZ <= max.getZ() >> 4; chunkZ++)
                {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk != null)
                        scanChunk(level, chunk, chunkX, chunkZ);
                }
            }
            return Optional.ofNullable(nearest);
        }

        private void scanChunk(ServerLevel level, LevelChunk chunk, int chunkX, int chunkZ)
        {
            LevelChunkSection[] sections = chunk.getSections();
            for (int index = 0; index < sections.length; index++)
            {
                int bottom = (chunk.getMinSection() + index) << 4;
                LevelChunkSection section = sections[index];
                if (bottom <= max.getY() && bottom + 15 >= min.getY() && !section.hasOnlyAir() && section.maybeHas(state -> state.getFluidState().is(FluidTags.WATER)))
                    scanSection(level, section, chunkX, chunkZ, bottom);
            }
        }

        private void scanSection(ServerLevel level, LevelChunkSection section, int chunkX, int chunkZ, int bottom)
        {
            for (int x = Math.max(min.getX(), chunkX << 4); x <= Math.min(max.getX(), (chunkX << 4) + 15); x++)
            {
                for (int z = Math.max(min.getZ(), chunkZ << 4); z <= Math.min(max.getZ(), (chunkZ << 4) + 15); z++)
                {
                    for (int y = Math.max(min.getY(), bottom); y <= Math.min(max.getY(), bottom + 15); y++)
                        consider(level, section, x, y, z);
                }
            }
        }

        private void consider(ServerLevel level, LevelChunkSection section, int x, int y, int z)
        {
            double squared = origin.distanceToSqr(x + 0.5D, y + 1D, z + 0.5D);
            if (squared > distance || nearest != null && squared == distance)
                return;
            cursor.set(x, y, z);
            BlockState water = section.getBlockState(x & 15, y & 15, z & 15);
            if (!water.getFluidState().is(FluidTags.WATER) || !water.getCollisionShape(level, cursor).isEmpty())
                return;
            BlockPos above = cursor.above();
            BlockState surface = level.getBlockState(above);
            if (!surface.getFluidState().isEmpty() || !surface.getCollisionShape(level, above).isEmpty())
                return;
            nearest = Vec3.atBottomCenterOf(above);
            distance = squared;
        }
    }
}
