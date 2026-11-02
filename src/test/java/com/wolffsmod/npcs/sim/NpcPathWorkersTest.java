package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.mixin.PathFinderAccessor;
import noppes.npcs.ai.NpcWalkNodeEvaluator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcPathWorkersTest
{
    @Test
    void urgentRequestsOvertakeRecentWorkWithoutStarvingOlderWork()
    {
        Runnable task = () ->
        {
        };
        NpcPathWorkers.Work old = new NpcPathWorkers.Work(task, false);
        for (int index = 0; index < 40; index++)
            new NpcPathWorkers.Work(task, false);
        NpcPathWorkers.Work urgent = new NpcPathWorkers.Work(task, true);
        NpcPathWorkers.Work recent = new NpcPathWorkers.Work(task, false);
        PriorityQueue<NpcPathWorkers.Work> queue = new PriorityQueue<>();
        queue.add(recent);
        queue.add(urgent);
        queue.add(old);
        assertSame(old, queue.remove());
        assertSame(urgent, queue.remove());
        assertSame(recent, queue.remove());
    }

    @Test
    void ordinaryBacklogsLeaveUrgentCapacityAndEvenUrgentWorkHasAHardQueueLimit()
    {
        assertTrue(NpcPathWorkers.queueAllows(223, false));
        assertFalse(NpcPathWorkers.queueAllows(224, false));
        assertTrue(NpcPathWorkers.queueAllows(224, true));
        assertTrue(NpcPathWorkers.queueAllows(255, true));
        assertFalse(NpcPathWorkers.queueAllows(256, true));
    }

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void groundWorkerOwnsItsCachesWhileNavigationKeepsItsRegion() throws ReflectiveOperationException
    {
        NpcWalkNodeEvaluator original = new NpcWalkNodeEvaluator();
        original.setCanPassDoors(true);
        original.setCanOpenDoors(true);
        original.setCanFloat(true);
        original.setCanWalkOverFences(true);
        PathNavigationRegion region = mock(PathNavigationRegion.class);
        Mob mob = mock(Mob.class);
        PathFinder worker = NpcPathWorkers.workerFinder(finder(original), region, mob);
        assertNotNull(worker);
        NodeEvaluator isolated = (NodeEvaluator) field(PathFinder.class, worker, "nodeEvaluator");
        assertEquals(NpcWalkNodeEvaluator.class, isolated.getClass());
        assertNotSame(original, isolated);
        assertTrue(isolated.canPassDoors());
        assertTrue(isolated.canOpenDoors());
        assertTrue(isolated.canFloat());
        assertTrue(isolated.canWalkOverFences());
        assertEquals(321, field(PathFinder.class, worker, "maxVisitedNodes"));
        assertNotSame(field(WalkNodeEvaluator.class, original, "pathTypesByPosCacheByMob"), field(WalkNodeEvaluator.class, isolated, "pathTypesByPosCacheByMob"));
        assertNotSame(field(NodeEvaluator.class, original, "nodes"), field(NodeEvaluator.class, isolated, "nodes"));
        // 1.21 keeps the region in the evaluator's pathfinding context, which Custom NPCs' done() retains.
        assertSame(region, ((PathfindingContext) field(NodeEvaluator.class, original, "currentContext")).level());
        assertNull(field(NodeEvaluator.class, isolated, "currentContext"));
        verify(mob).onPathfindingStart();
        verify(mob).onPathfindingDone();
    }

    @Test
    void flyingWorkerAlsoHasAnIndependentEvaluator() throws ReflectiveOperationException
    {
        FlyNodeEvaluator original = new FlyNodeEvaluator();
        PathFinder worker = NpcPathWorkers.workerFinder(finder(original), mock(PathNavigationRegion.class), mock(Mob.class));
        assertNotNull(worker);
        Object isolated = field(PathFinder.class, worker, "nodeEvaluator");
        assertEquals(FlyNodeEvaluator.class, isolated.getClass());
        assertNotSame(original, isolated);
        assertFalse(((NodeEvaluator) isolated).canFloat());
    }

    @Test
    void unknownEvaluatorsRemainOnTheServerThread()
    {
        NodeEvaluator custom = mock(NodeEvaluator.class);
        assertNull(NpcPathWorkers.workerFinder(finder(custom), mock(PathNavigationRegion.class), mock(Mob.class)));
        verifyNoInteractions(custom);
    }

    private static PathFinder finder(NodeEvaluator evaluator)
    {
        PathFinder finder = mock(PathFinder.class, withSettings().extraInterfaces(PathFinderAccessor.class));
        PathFinderAccessor access = (PathFinderAccessor) finder;
        when(access.wolffsmodnpcsNodeEvaluator()).thenReturn(evaluator);
        when(access.wolffsmodnpcsMaxVisitedNodes()).thenReturn(321);
        return finder;
    }

    private static Object field(Class<?> owner, Object instance, String name) throws ReflectiveOperationException
    {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }
}
