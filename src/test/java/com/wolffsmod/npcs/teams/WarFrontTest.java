package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

class WarFrontTest
{
    private static final int GERMANY = 1;
    private static final int BRITAIN = 2;
    private static final int ITALY = 3;
    /** Germany and Italy are allies; both fight Britain. */
    private static final BiPredicate<Integer, Integer> AT_WAR = (faction, other) -> !faction.equals(other) && (faction == BRITAIN || other == BRITAIN);

    /** Germany holds x 0 to 2, Britain x 3 to 5, on rows z 0 and 1; Italy holds x 0 to 2 on row z 2. */
    private static Map<Long, Integer> territory()
    {
        Map<Long, Integer> owners = new HashMap<>();
        for (int z = 0; z <= 1; z++)
        {
            for (int x = 0; x <= 5; x++)
                owners.put(ChunkPos.asLong(x, z), x <= 2 ? GERMANY : BRITAIN);
        }
        for (int x = 0; x <= 2; x++)
            owners.put(ChunkPos.asLong(x, 2), ITALY);
        return owners;
    }

    @Test
    void bordersAreWhereFactionsAtWarMeetEachOnce()
    {
        List<WarFront.Border> borders = WarFront.borders(territory(), AT_WAR);
        assertEquals(2, borders.size(), "allies Germany and Italy share no border; Germany and Britain meet on two rows");
        for (WarFront.Border border : borders)
            assertEquals(1, Math.abs(ChunkPos.getX(border.first()) - ChunkPos.getX(border.second())));
    }

    @Test
    void theNearestBorderWithinReachIsFound()
    {
        List<WarFront.Border> borders = WarFront.borders(territory(), AT_WAR);
        assertTrue(WarFront.nearest(borders, 2, 1, 2).isPresent());
        assertTrue(WarFront.nearest(borders, 30, 30, 4).isEmpty(), "a border far away is not near");
    }

    @Test
    void afterTakingAChunkTheFightGoesOnStraightAhead()
    {
        Map<Long, Integer> owners = territory();
        WarFront.Border taken = new WarFront.Border(ChunkPos.asLong(2, 0), GERMANY, ChunkPos.asLong(3, 0), BRITAIN);
        owners.put(taken.second(), GERMANY);
        WarFront.Border onward = WarFront.onward(owners, AT_WAR, taken, 4).orElseThrow();
        assertEquals(ChunkPos.asLong(3, 0), onward.first());
        assertEquals(ChunkPos.asLong(4, 0), onward.second(), "the chunk straight ahead comes first");
        assertEquals(GERMANY, onward.firstFaction());
        assertEquals(BRITAIN, onward.secondFaction());
    }

    @Test
    void withNothingAheadTheFightMovesToTheNearestBorder()
    {
        Map<Long, Integer> owners = new HashMap<>();
        owners.put(ChunkPos.asLong(0, 0), GERMANY);
        owners.put(ChunkPos.asLong(1, 0), GERMANY);
        owners.put(ChunkPos.asLong(1, 2), GERMANY);
        owners.put(ChunkPos.asLong(2, 2), BRITAIN);
        WarFront.Border taken = new WarFront.Border(ChunkPos.asLong(0, 0), GERMANY, ChunkPos.asLong(1, 0), BRITAIN);
        WarFront.Border onward = WarFront.onward(owners, AT_WAR, taken, 4).orElseThrow();
        assertEquals(GERMANY, onward.firstFaction(), "the border is seen from the side that took the chunk");
        assertEquals(ChunkPos.asLong(2, 2), onward.second());
        assertTrue(WarFront.onward(owners, AT_WAR, taken, 0).isEmpty(), "a border too far away ends the fight");
    }

    @Test
    void aSideGainsGroundWhileItStandsAloneInTheOthersChunk()
    {
        assertEquals(0, WarFront.pressing(3, 0, 0, 2));
        assertEquals(1, WarFront.pressing(0, 4, 2, 0));
        assertEquals(-1, WarFront.pressing(3, 1, 0, 2), "a chunk still held by its own side is not taken");
        assertEquals(-1, WarFront.pressing(0, 0, 0, 0));
    }
}
