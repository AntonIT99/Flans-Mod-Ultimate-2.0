package com.flansmodultimate.apocalyse.common.world;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The villages that were here before the wasteland was.
 *
 * <p>They sit on a fixed grid so they are spread evenly and reproducibly, and each one is a
 * well with a handful of collapsing cobblestone-and-plank houses and worked-out fields
 * around it — the ruined counterpart of the settlement generator 1.7.10 shipped.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseVillage
{
    /** One village per this many chunks along each axis. */
    private static final int GRID_SPACING = 20;
    /** Chunks at the far edge of a cell that are never chosen, keeping villages apart. */
    private static final int GRID_MARGIN = 4;
    private static final int MIN_BUILDINGS = 4;
    private static final int EXTRA_BUILDINGS = 4;
    /** Half-width of the area a village may occupy, in blocks. */
    private static final int VILLAGE_RADIUS = 20;
    /** Widest a plot can reach from its corner: a seven-block house or field plus a fence post. */
    private static final int MAX_PLOT_EXTENT = 8;

    private static final BlockState WALL = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState PATCH = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState BEAM = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState PATH = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** Builds the village anchored at this chunk, if one is. */
    public static void generate(WorldGenLevel level, ChunkPos chunkPos)
    {
        if (!isAnchor(level.getSeed(), chunkPos))
            return;

        RandomSource random = RandomSource.create(level.getSeed()
            ^ (chunkPos.x * 738297199L) ^ (chunkPos.z * 1128889L));
        BlockPos centre = ApocalypseWorldgen.surfacePos(level, chunkPos.getMinBlockX() + 8, chunkPos.getMinBlockZ() + 8);
        if (centre.getY() <= level.getMinBuildHeight() + 4 || centre.getY() >= level.getMaxBuildHeight() - 12)
            return;

        well(level, centre);
        crossroads(level, centre);

        int buildings = MIN_BUILDINGS + random.nextInt(EXTRA_BUILDINGS + 1);
        for (int index = 0; index < buildings; index++)
        {
            BlockPos plot = plotFor(level, centre, random);
            if (plot == null)
                continue;
            if (random.nextInt(4) == 0)
                field(level, random, plot);
            else
                ruinedHouse(level, random, plot);
        }
    }

    /**
     * Whether this chunk is the one chosen inside its grid cell.
     *
     * <p>Derived from the seed and the cell alone, so every chunk in the world agrees on
     * where the villages are without consulting any shared state.</p>
     */
    private static boolean isAnchor(long seed, ChunkPos pos)
    {
        int cellX = Math.floorDiv(pos.x, GRID_SPACING);
        int cellZ = Math.floorDiv(pos.z, GRID_SPACING);
        RandomSource random = RandomSource.create(seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x5EEDL);
        int offsetX = random.nextInt(GRID_SPACING - GRID_MARGIN);
        int offsetZ = random.nextInt(GRID_SPACING - GRID_MARGIN);
        return pos.x == cellX * GRID_SPACING + offsetX && pos.z == cellZ * GRID_SPACING + offsetZ;
    }

    /** A flat-enough building plot within the village, or null if none was found. */
    private static BlockPos plotFor(WorldGenLevel level, BlockPos centre, RandomSource random)
    {
        for (int attempt = 0; attempt < 12; attempt++)
        {
            // The whole plot, not just its corner, stays inside the village radius. That keeps
            // every block within a chunk of the anchor, the most a worldgen feature may write.
            int x = centre.getX() + random.nextInt(VILLAGE_RADIUS * 2 + 1 - MAX_PLOT_EXTENT) - VILLAGE_RADIUS;
            int z = centre.getZ() + random.nextInt(VILLAGE_RADIUS * 2 + 1 - MAX_PLOT_EXTENT) - VILLAGE_RADIUS;
            BlockPos plot = ApocalypseWorldgen.surfacePos(level, x, z);
            // Reject slopes: a ruin half-buried in a cliff reads as a bug, not as a ruin.
            if (Math.abs(plot.getY() - centre.getY()) <= 2)
                return plot;
        }
        return null;
    }

    /** A cobblestone well ringed by a gravel apron: the centre every village grew around. */
    private static void well(WorldGenLevel level, BlockPos centre)
    {
        for (int dx = -2; dx <= 2; dx++)
        {
            for (int dz = -2; dz <= 2; dz++)
            {
                BlockPos ground = new BlockPos(centre.getX() + dx, centre.getY() - 1, centre.getZ() + dz);
                boolean rim = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                ApocalypseWorldgen.setBlock(level, ground, rim ? WALL : PATH);
                for (int dy = 0; dy <= 2; dy++)
                    ApocalypseWorldgen.setBlock(level, ground.above(dy + 1), AIR);
            }
        }

        // The shaft: two blocks of water below the rim, open to the sky.
        ApocalypseWorldgen.setBlock(level, centre.below(), Blocks.WATER.defaultBlockState());
        ApocalypseWorldgen.setBlock(level, centre.below(2), Blocks.WATER.defaultBlockState());
        ApocalypseWorldgen.setBlock(level, centre.below(3), WALL);

        for (int dx = -1; dx <= 1; dx += 2)
        {
            for (int dz = -1; dz <= 1; dz += 2)
                ApocalypseWorldgen.setBlock(level, centre.offset(dx, 0, dz), Blocks.OAK_FENCE.defaultBlockState());
        }
    }

    /** Two gravel lanes meeting at the well, which the plots grow up around. */
    private static void crossroads(WorldGenLevel level, BlockPos centre)
    {
        for (int offset = -VILLAGE_RADIUS; offset <= VILLAGE_RADIUS; offset++)
        {
            layPath(level, centre.getX() + offset, centre.getZ());
            layPath(level, centre.getX(), centre.getZ() + offset);
        }
    }

    private static void layPath(WorldGenLevel level, int x, int z)
    {
        BlockPos surface = ApocalypseWorldgen.surfacePos(level, x, z);
        if (surface.getY() <= level.getMinBuildHeight() + 1)
            return;
        ApocalypseWorldgen.setBlock(level, surface.below(), PATH);
        if (!level.getBlockState(surface).isAir())
            ApocalypseWorldgen.setBlock(level, surface, AIR);
    }

    private static void ruinedHouse(WorldGenLevel level, RandomSource random, BlockPos origin)
    {
        int width = 5 + random.nextInt(3);
        int depth = 5 + random.nextInt(3);
        int height = 3 + random.nextInt(2);
        Direction doorway = Direction.Plane.HORIZONTAL.getRandomDirection(random);

        for (int dx = 0; dx < width; dx++)
        {
            for (int dz = 0; dz < depth; dz++)
            {
                BlockPos floor = origin.offset(dx, -1, dz);
                ApocalypseWorldgen.setBlock(level, floor, random.nextInt(6) == 0 ? PATCH : WALL);

                boolean edge = dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1;
                boolean corner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
                for (int dy = 0; dy < height; dy++)
                {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!edge)
                    {
                        ApocalypseWorldgen.setBlock(level, pos, AIR);
                        continue;
                    }
                    // Higher courses have fallen away more than lower ones.
                    boolean standing = corner || random.nextFloat() > 0.15F + 0.2F * dy;
                    ApocalypseWorldgen.setBlock(level, pos, standing ? (random.nextInt(5) == 0 ? PATCH : WALL) : AIR);
                }
                if (corner)
                    ApocalypseWorldgen.setBlock(level, origin.offset(dx, height - 1, dz), BEAM);
            }
        }

        cutDoorway(level, origin, width, depth, doorway);
        if (random.nextBoolean())
            collapsedRoof(level, random, origin, width, depth, height);
        furnish(level, random, origin, width, depth);
    }

    private static void cutDoorway(WorldGenLevel level, BlockPos origin, int width, int depth, Direction facing)
    {
        int x = switch (facing)
        {
            case WEST -> 0;
            case EAST -> width - 1;
            default -> width / 2;
        };
        int z = switch (facing)
        {
            case NORTH -> 0;
            case SOUTH -> depth - 1;
            default -> depth / 2;
        };
        ApocalypseWorldgen.setBlock(level, origin.offset(x, 0, z), AIR);
        ApocalypseWorldgen.setBlock(level, origin.offset(x, 1, z), AIR);
    }

    private static void collapsedRoof(WorldGenLevel level, RandomSource random, BlockPos origin, int width, int depth, int height)
    {
        for (int dx = 0; dx < width; dx++)
        {
            for (int dz = 0; dz < depth; dz++)
            {
                if (random.nextFloat() < 0.45F)
                    continue;
                ApocalypseWorldgen.setBlock(level, origin.offset(dx, height, dz), PATCH);
            }
        }
    }

    private static void furnish(WorldGenLevel level, RandomSource random, BlockPos origin, int width, int depth)
    {
        BlockPos inside = origin.offset(1 + random.nextInt(Math.max(1, width - 2)), 0, 1 + random.nextInt(Math.max(1, depth - 2)));
        if (random.nextInt(3) != 0)
            ApocalypseWorldgen.placeChest(level, random, inside);

        if (random.nextInt(6) != 0)
            return;
        BlockPos wall = origin.offset(1, 1, 0);
        ApocalypseWorldgen.flanBlock("flangunrack").ifPresent(block ->
            ApocalypseWorldgen.placeItemHolder(level, random, block, wall, Direction.SOUTH, true));
    }

    /** A plot that was still being worked when everyone left. */
    private static void field(WorldGenLevel level, RandomSource random, BlockPos origin)
    {
        int size = 5 + random.nextInt(3);
        for (int dx = 0; dx < size; dx++)
        {
            for (int dz = 0; dz < size; dz++)
            {
                BlockPos soil = origin.offset(dx, -1, dz);
                boolean channel = dx == size / 2;
                ApocalypseWorldgen.setBlock(level, soil, channel ? Blocks.WATER.defaultBlockState()
                    : random.nextInt(4) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.FARMLAND.defaultBlockState());
                BlockPos above = origin.offset(dx, 0, dz);
                if (channel || level.getBlockState(soil).is(Blocks.COARSE_DIRT))
                {
                    ApocalypseWorldgen.setBlock(level, above, AIR);
                    continue;
                }
                ApocalypseWorldgen.setBlock(level, above, random.nextBoolean() ? Blocks.WHEAT.defaultBlockState() : AIR);
            }
        }
        for (int dx = -1; dx <= size; dx++)
        {
            fencePost(level, origin.offset(dx, 0, -1));
            fencePost(level, origin.offset(dx, 0, size));
        }
        for (int dz = 0; dz < size; dz++)
        {
            fencePost(level, origin.offset(-1, 0, dz));
            fencePost(level, origin.offset(size, 0, dz));
        }
    }

    private static void fencePost(WorldGenLevel level, BlockPos pos)
    {
        if (!level.getBlockState(pos).isAir())
            return;
        ApocalypseWorldgen.setBlock(level, pos, Blocks.OAK_FENCE.defaultBlockState());
    }
}
