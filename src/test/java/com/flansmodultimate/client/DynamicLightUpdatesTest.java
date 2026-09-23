package com.flansmodultimate.client;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamicLightUpdatesTest
{
    @Test
    void refreshesAtClientIntervalsAndImmediatelyAfterWorldReset()
    {
        for (int interval : new int[] {10, 20})
        {
            DynamicLightUpdates updates = new DynamicLightUpdates();
            int refreshes = 0;
            for (int tick = 0; tick < 100; tick++)
                if (updates.tick(interval)) ++refreshes;
            assertEquals(100 / interval, refreshes);
            updates.reset();
            assertTrue(updates.tick(interval));
            assertFalse(updates.tick(interval));
        }
    }

    @Test
    void overlappingSourcesUseTheBrightestLightRegardlessOfOrder()
    {
        for (int[] levels : new int[][] {{12, 15}, {15, 12}})
        {
            DynamicLightUpdates updates = new DynamicLightUpdates();
            TestWorld world = new TestWorld();
            Long2ByteMap requested = new Long2ByteOpenHashMap();
            for (int level : levels)
                DynamicLightUpdates.request(requested, 1, level);
            updates.apply(requested, world);
            assertEquals(15, world.lightAt(1));
            assertEquals(1, world.writes);
            updates.apply(light(1, 12), world);
            assertEquals(12, world.lightAt(1), "The dimmer source remains when the bright source moves away");
        }
    }

    @Test
    void stationaryLightsCauseNoFurtherBlockWritesAndCanDimOrMove()
    {
        DynamicLightUpdates updates = new DynamicLightUpdates();
        TestWorld world = new TestWorld();
        updates.apply(light(1, 15), world);
        assertEquals(1, world.writes);
        for (int tick = 0; tick < 100; tick++)
            updates.apply(light(1, 15), world);
        assertEquals(1, world.writes);

        updates.apply(light(1, 12), world);
        assertEquals(12, world.lightAt(1));
        assertEquals(2, world.writes);
        updates.apply(light(2, 12), world);
        assertEquals(0, world.lightAt(1));
        assertEquals(12, world.lightAt(2));
        assertEquals(4, world.writes);
        updates.apply(new Long2ByteOpenHashMap(), world);
        assertEquals(0, world.lightAt(2));
        assertEquals(5, world.writes);
    }

    @Test
    void doesNotOverwriteServerChangesOrAdoptExistingLights()
    {
        DynamicLightUpdates updates = new DynamicLightUpdates();
        TestWorld world = new TestWorld();
        world.blocks.put(1L, 10);
        updates.apply(light(1, 15), world);
        assertEquals(0, world.writes);
        updates.apply(new Long2ByteOpenHashMap(), world);
        assertEquals(10, world.lightAt(1));

        updates.apply(light(2, 15), world);
        world.blocks.put(2L, -1);
        updates.apply(light(2, 15), world);
        updates.apply(new Long2ByteOpenHashMap(), world);
        assertEquals(-1, world.lightAt(2));
        assertEquals(1, world.writes);

        updates.apply(light(3, 15), world);
        world.blocks.put(3L, 8);
        updates.apply(new Long2ByteOpenHashMap(), world);
        assertEquals(8, world.lightAt(3));
        assertEquals(2, world.writes);
    }

    @Test
    void reappliesALightRemovedByAChunkUpdateAndForgetsPreviousWorldOwnership()
    {
        DynamicLightUpdates updates = new DynamicLightUpdates();
        TestWorld world = new TestWorld();
        updates.apply(light(1, 15), world);
        world.blocks.remove(1L);
        updates.apply(light(1, 15), world);
        assertEquals(15, world.lightAt(1));
        assertEquals(2, world.writes);
        updates.reset();
        updates.apply(new Long2ByteOpenHashMap(), world);
        assertEquals(2, world.writes);
    }

    private static Long2ByteMap light(long position, int level)
    {
        Long2ByteMap result = new Long2ByteOpenHashMap();
        result.put(position, (byte) level);
        return result;
    }

    private static final class TestWorld implements DynamicLightUpdates.Access
    {
        private final Map<Long, Integer> blocks = new HashMap<>();
        private int writes;

        @Override public int lightAt(long position) { return blocks.getOrDefault(position, 0); }
        @Override public void setLight(long position, int light) { blocks.put(position, light); ++writes; }
    }
}
