package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotApproachTest
{
    private static final Vec3 START = new Vec3(0D, 64D, 0D);
    private static final SquadTask ATTACK = new SquadTask(SquadTask.Kind.ATTACK, new Vec3(100D, 64D, 0D), 6D);

    @Test
    void leaderFailureRetriesAnotherApproachAndPlayerOrdersCanClearIt()
    {
        BotApproach approach = approach(1, List.of());
        Vec3 initial = approach.waypoint(ATTACK);
        approach.retry(ATTACK, START, 1, List.of(), 20L);
        assertNotEquals(initial, approach.waypoint(ATTACK));
        approach.clear();
        assertNull(approach.waypoint(ATTACK));
    }

    @Test
    void sharedObjectiveHasDirectLeftAndRightApproaches()
    {
        BotApproach direct = approach(0, List.of());
        BotApproach left = approach(1, List.of());
        BotApproach right = approach(2, List.of());
        assertNull(direct.waypoint(ATTACK));
        assertTrue(left.waypoint(ATTACK).z > 0D);
        assertTrue(right.waypoint(ATTACK).z < 0D);
        assertEquals(left.waypoint(ATTACK).x, right.waypoint(ATTACK).x);
        assertTrue(left.waypoint(ATTACK).x > START.x && left.waypoint(ATTACK).x < ATTACK.target().x);
    }

    @Test
    void authoredMarkerOnAssignedFlankIsPreferred()
    {
        Vec3 marker = new Vec3(55D, 64D, 20D);
        assertEquals(marker, approach(1, List.of(new Vec3(60D, 64D, -16D), marker)).waypoint(ATTACK));
    }

    @Test
    void replanningKeepsTheApproachDespiteLeaderMovementAndOtherSquadLosses()
    {
        BotApproach approach = approach(1, List.of());
        Vec3 waypoint = approach.waypoint(ATTACK);
        approach.update(ATTACK, START.add(20D, 0D, 0D), 0, List.of(), 20L, true);
        assertEquals(waypoint, approach.waypoint(ATTACK));
        SquadTask moved = new SquadTask(ATTACK.kind(), ATTACK.target().add(3D, 0D, 0D), ATTACK.radius());
        approach.update(moved, START, 1, List.of(), 40L, true);
        assertEquals(waypoint, approach.waypoint(moved));
    }

    @Test
    void reachedOrTimedOutApproachesNeverRestartForTheSameObjective()
    {
        BotApproach reached = approach(1, List.of());
        reached.update(ATTACK, reached.waypoint(ATTACK), 1, List.of(), 20L, true);
        reached.update(ATTACK, START, 1, List.of(), 40L, true);
        assertNull(reached.waypoint(ATTACK));
        BotApproach expired = approach(1, List.of());
        expired.update(ATTACK, START, 1, List.of(), 600L, true);
        expired.update(ATTACK, START, 1, List.of(), 620L, true);
        assertNull(expired.waypoint(ATTACK));
    }

    @Test
    void changingObjectiveReplacesTheOldApproach()
    {
        BotApproach approach = approach(1, List.of());
        SquadTask changed = new SquadTask(ATTACK.kind(), new Vec3(0D, 64D, 100D), 6D);
        approach.update(changed, START, 1, List.of(), 20L, true);
        assertNull(approach.waypoint(ATTACK));
        assertTrue(approach.waypoint(changed).x < 0D);
    }

    @Test
    void guardingCloseObjectivesAndFlagCarriersUseDirectOrders()
    {
        BotApproach approach = new BotApproach();
        for (SquadTask task : List.of(new SquadTask(SquadTask.Kind.DEFEND, ATTACK.target(), 6D), new SquadTask(SquadTask.Kind.ATTACK, START.add(20D, 0D, 0D), 6D)))
        {
            approach.update(task, START, 1, List.of(), 0L, true);
            assertNull(approach.waypoint(task));
        }
        approach.update(ATTACK, START, 1, List.of(), 0L, false);
        assertNull(approach.waypoint(ATTACK));
    }

    private static BotApproach approach(int lane, List<Vec3> markers)
    {
        BotApproach approach = new BotApproach();
        approach.update(ATTACK, START, lane, markers, 0L, true);
        return approach;
    }
}
