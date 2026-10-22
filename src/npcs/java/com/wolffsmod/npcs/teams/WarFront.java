package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Where the territories of factions at war meet. Territory is held chunk by chunk; two side by side chunks of factions at
 * war make a border, and a war fights over its borders. Pure arithmetic, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class WarFront
{
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Two side by side chunks of factions at war, as chunk position keys.
     *
     * @param first
     *            the chunk of the first faction
     * @param second
     *            the chunk of the second faction, beside the first
     */
    record Border(long first, int firstFaction, long second, int secondFaction)
    {
        /** @return the border seen from the other side */
        Border reversed()
        {
            return new Border(second, secondFaction, first, firstFaction);
        }
    }

    /** @return every border between chunks of factions at war, each once */
    static List<Border> borders(Map<Long, Integer> owners, BiPredicate<Integer, Integer> atWar)
    {
        List<Border> borders = new ArrayList<>();
        for (Map.Entry<Long, Integer> chunk : owners.entrySet())
        {
            int x = ChunkPos.getX(chunk.getKey());
            int z = ChunkPos.getZ(chunk.getKey());
            // Only the neighbours east and south, so every pair is found once.
            for (int[] step : new int[][]{{1, 0}, {0, 1}})
            {
                long beside = ChunkPos.asLong(x + step[0], z + step[1]);
                Integer other = owners.get(beside);
                if (other != null && atWar.test(chunk.getValue(), other))
                    borders.add(new Border(chunk.getKey(), chunk.getValue(), beside, other));
            }
        }
        return borders;
    }

    /** @return the border nearest a chunk, no farther than some chunks from it, or empty for none */
    static Optional<Border> nearest(List<Border> borders, int chunkX, int chunkZ, int within)
    {
        Border best = null;
        double bestDistance = (double) within * within;
        for (Border border : borders)
        {
            double distance = distanceSqr(border, chunkX, chunkZ);
            if (distance <= bestDistance)
            {
                best = border;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The border a fight moves on to once the first faction of a border took the second's chunk: on from that chunk to a
     * chunk beside it of a faction at war with the taker, straight ahead first; failing that, the border nearest the taken
     * chunk within some chunks. The border is seen from the taker's side.
     *
     * @param owners
     *            the territory with the chunk taken already
     */
    static Optional<Border> onward(Map<Long, Integer> owners, BiPredicate<Integer, Integer> atWar, Border taken, int within)
    {
        int fromX = ChunkPos.getX(taken.first());
        int fromZ = ChunkPos.getZ(taken.first());
        int x = ChunkPos.getX(taken.second());
        int z = ChunkPos.getZ(taken.second());
        int aheadX = x + (x - fromX);
        int aheadZ = z + (z - fromZ);
        int taker = taken.firstFaction();
        Optional<Border> ahead = borderTo(owners, atWar, taken.second(), taker, aheadX, aheadZ);
        if (ahead.isPresent())
            return ahead;
        for (int[] step : NEIGHBOURS)
        {
            Optional<Border> beside = borderTo(owners, atWar, taken.second(), taker, x + step[0], z + step[1]);
            if (beside.isPresent())
                return beside;
        }
        return nearest(borders(owners, atWar), x, z, within).map(border -> border.firstFaction() == taker ? border : border.reversed());
    }

    private static Optional<Border> borderTo(Map<Long, Integer> owners, BiPredicate<Integer, Integer> atWar, long from, int taker, int x, int z)
    {
        long to = ChunkPos.asLong(x, z);
        Integer owner = owners.get(to);
        return owner != null && atWar.test(taker, owner) ? Optional.of(new Border(from, taker, to, owner)) : Optional.empty();
    }

    /** @return the square of the distance from a chunk to the middle of a border, in chunks */
    static double distanceSqr(Border border, int chunkX, int chunkZ)
    {
        double x = (ChunkPos.getX(border.first()) + ChunkPos.getX(border.second())) / 2D - chunkX;
        double z = (ChunkPos.getZ(border.first()) + ChunkPos.getZ(border.second())) / 2D - chunkZ;
        return x * x + z * z;
    }

    /**
     * Who gains on a contested chunk pair as the fight goes: the first side presses on while it stands in the second's
     * chunk and nobody of the second does; and the other way round.
     *
     * @return 0 for the first side pressing on, 1 for the second, or -1 for neither
     */
    static int pressing(int firstInSecondsChunk, int secondInSecondsChunk, int secondInFirstsChunk, int firstInFirstsChunk)
    {
        if (firstInSecondsChunk > 0 && secondInSecondsChunk == 0)
            return 0;
        if (secondInFirstsChunk > 0 && firstInFirstsChunk == 0)
            return 1;
        return -1;
    }
}
