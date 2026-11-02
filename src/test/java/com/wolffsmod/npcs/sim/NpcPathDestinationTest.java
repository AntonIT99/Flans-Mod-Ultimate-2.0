package com.wolffsmod.npcs.sim;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NpcPathDestinationTest
{
    @Test
    void aQueuedPursuitCannotBecomeARetreatPath()
    {
        Set<BlockPos> pursue = Set.of(new BlockPos(20, 64, 0));
        assertFalse(NpcPathWorkers.sameDestination(pursue, Set.of(new BlockPos(-12, 64, 0)), true));
        assertFalse(NpcPathWorkers.sameDestination(pursue, Set.of(new BlockPos(18, 64, 0)), true));
        assertTrue(NpcPathWorkers.sameDestination(pursue, pursue, true));
    }

    @Test
    void aMovingQuarryDoesNotStarveOrdinaryAsynchronousPursuit()
    {
        Set<BlockPos> pursue = Set.of(new BlockPos(20, 64, 0));
        assertTrue(NpcPathWorkers.sameDestination(pursue, Set.of(new BlockPos(22, 64, 0)), false));
        assertFalse(NpcPathWorkers.sameDestination(pursue, Set.of(new BlockPos(30, 64, 0)), false));
    }
}
