package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcFlightPathsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aPlaneFliesStraightToItsNearestGoalWhileOnlyAHelicopterThatSeesItsGoalDoes()
    {
        EntityNPCInterface npc = aircraft();
        BlockPos near = new BlockPos(10, 64, 0);
        BlockPos far = new BlockPos(100, 64, 0);
        try (var flight = mockStatic(NpcFlight.class); var sight = mockStatic(NpcSight.class))
        {
            flight.when(() -> NpcFlight.type(npc)).thenReturn(Flight.PLANE);
            Path path = NpcFlightPaths.direct(npc, Set.of(far, near));
            assertNotNull(path);
            assertEquals(2, path.getNodeCount());
            assertEquals(new BlockPos(0, 80, 0), path.getNodePos(0));
            assertEquals(near, path.getTarget());
            assertTrue(path.canReach());
            // Navigation replaces nodes of the path it follows, so the path holds a list it may change.
            assertDoesNotThrow(() -> path.replaceNode(1, path.getNode(0)));

            flight.when(() -> NpcFlight.type(npc)).thenReturn(Flight.HELICOPTER);
            sight.when(() -> NpcSight.clear(any(Level.class), any(Vec3.class), any(Vec3.class), eq(0), eq(npc))).thenReturn(true);
            assertNotNull(NpcFlightPaths.direct(npc, Set.of(near)));
            sight.when(() -> NpcSight.clear(any(Level.class), any(Vec3.class), any(Vec3.class), eq(0), eq(npc))).thenReturn(false);
            assertNull(NpcFlightPaths.direct(npc, Set.of(near)));

            flight.when(() -> NpcFlight.type(npc)).thenReturn(Flight.NORMAL);
            assertNull(NpcFlightPaths.direct(npc, Set.of(near)));
            assertFalse(NpcFlightPaths.limitsBody(npc));
        }
    }

    @Test
    void aWreckOrAnEmptyGoalGetsNoStraightPath()
    {
        EntityNPCInterface npc = aircraft();
        try (var flight = mockStatic(NpcFlight.class))
        {
            flight.when(() -> NpcFlight.type(npc)).thenReturn(Flight.PLANE);
            assertTrue(NpcFlightPaths.limitsBody(npc));
            assertNull(NpcFlightPaths.direct(npc, Set.of()));
            when(npc.isKilled()).thenReturn(true);
            assertNull(NpcFlightPaths.direct(npc, Set.of(BlockPos.ZERO)));
        }
    }

    private static EntityNPCInterface aircraft()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        when(npc.position()).thenReturn(new Vec3(0.5D, 80D, 0.5D));
        when(npc.blockPosition()).thenReturn(new BlockPos(0, 80, 0));
        when(npc.getEyePosition()).thenReturn(new Vec3(0.5D, 81D, 0.5D));
        return npc;
    }
}
