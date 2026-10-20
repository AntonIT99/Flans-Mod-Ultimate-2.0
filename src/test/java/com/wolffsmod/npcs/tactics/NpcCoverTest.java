package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NpcCoverTest
{
    @Test
    void coverIsSearchedRingByRingWithEveryBlockOfARingOnce()
    {
        BlockPos origin = new BlockPos(10, 64, -3);
        assertEquals(List.of(origin.below(), origin, origin.above()), NpcCover.ring(origin, 0));
        for (int ring = 1; ring <= 3; ring++)
        {
            List<BlockPos> blocks = NpcCover.ring(origin, ring);
            assertEquals(blocks.size(), new HashSet<>(blocks).size(), "ring " + ring);
            assertEquals(8 * ring * 3, blocks.size(), "ring " + ring);
            for (BlockPos block : blocks)
                assertEquals(ring, Math.max(Math.abs(block.getX() - origin.getX()), Math.abs(block.getZ() - origin.getZ())));
        }
    }
}
