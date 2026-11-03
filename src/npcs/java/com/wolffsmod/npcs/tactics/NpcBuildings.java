package com.wolffsmod.npcs.tactics;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Recognizes the inside of buildings: a block with a roof over it and walls round most of it. NPCs that open doors prefer such
 * places as cover, fight from their windows and doorways, and follow enemies into them. Results are cached for a few seconds
 * per level because cover searches test the same blocks repeatedly. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBuildings
{
    /** Custom NPCs' door interaction value for opening doors. */
    public static final int DOORS_OPEN = 1;
    /** Highest a roof may be above the floor. */
    private static final int ROOF_HEIGHT = 6;
    /** Farthest a wall may be in each direction. */
    private static final int WALL_REACH = 6;
    /** Of the eight directions, how many must meet a wall. */
    private static final int WALLS_NEEDED = 6;
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final long CACHE_TICKS = 100L;
    private static final int CACHE_LIMIT = 8192;
    /** Packed position to the game time of the check, with the result in the lowest bit. */
    private static final Map<Level, Long2LongOpenHashMap> CACHE = new WeakHashMap<>();

    /** @return whether the NPC opens doors, so that it can get into buildings without breaking them */
    public static boolean entersBuildings(EntityNPCInterface npc)
    {
        return npc.ais.doorInteract == DOORS_OPEN;
    }

    public static boolean indoors(Entity entity)
    {
        return indoors(entity.level(), entity.blockPosition());
    }

    /**
     * @param stand
     *            the block a body stands in
     */
    public static boolean indoors(Level level, BlockPos stand)
    {
        long time = level.getGameTime();
        Long2LongOpenHashMap cache = CACHE.computeIfAbsent(level, ignored -> new Long2LongOpenHashMap());
        long key = stand.asLong();
        long known = cache.getOrDefault(key, Long.MIN_VALUE);
        if (known != Long.MIN_VALUE && time - (known >> 1) < CACHE_TICKS)
            return (known & 1L) != 0L;
        if (cache.size() >= CACHE_LIMIT)
            cache.clear();
        boolean result = measure(level, stand);
        cache.put(key, time << 1 | (result ? 1L : 0L));
        return result;
    }

    private static boolean measure(Level level, BlockPos stand)
    {
        if (!level.isLoaded(stand) || !roofed(level, stand))
            return false;
        int walls = 0;
        for (int index = 0; index < DIRECTIONS.length; index++)
        {
            if (wall(level, stand, DIRECTIONS[index]))
                walls++;
            // Stop as soon as the remaining directions can no longer reach the count.
            if (walls + DIRECTIONS.length - 1 - index < WALLS_NEEDED)
                return false;
        }
        return walls >= WALLS_NEEDED;
    }

    private static boolean roofed(Level level, BlockPos stand)
    {
        BlockPos.MutableBlockPos cursor = stand.mutable();
        for (int height = 2; height <= ROOF_HEIGHT; height++)
        {
            cursor.setY(stand.getY() + height);
            if (built(level, cursor))
                return true;
        }
        return false;
    }

    /** Checks at head height, so that windows count as walls and the open doorway as one gap. */
    private static boolean wall(Level level, BlockPos stand, int[] direction)
    {
        BlockPos.MutableBlockPos cursor = stand.mutable();
        for (int distance = 1; distance <= WALL_REACH; distance++)
        {
            cursor.set(stand.getX() + direction[0] * distance, stand.getY() + 1, stand.getZ() + direction[1] * distance);
            if (built(level, cursor))
                return true;
        }
        return false;
    }

    /** Glass, doors and other non-solid building blocks count; tree canopies do not. */
    private static boolean built(Level level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !(state.getBlock() instanceof LeavesBlock) && !state.getCollisionShape(level, pos).isEmpty() || state.getBlock() instanceof DoorBlock;
    }
}
