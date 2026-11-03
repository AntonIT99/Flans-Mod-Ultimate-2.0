package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SquadTacticsTest
{
    private static final Vec3 OBJECTIVE = at(100D, 0D);
    private static final Vec3 HOME = at(-30D, 0D);
    private static final SquadTask TASK = new SquadTask(SquadTask.Kind.ATTACK, OBJECTIVE, 6D);

    private static Vec3 at(double x, double z)
    {
        return new Vec3(x, 64D, z);
    }

    private static SquadCombat.Member soldier(int id, double x, BotSettings.ClassRole role, boolean ready, boolean pinned)
    {
        return new SquadCombat.Member(new UUID(0L, id), at(x, 0D), 10, ready, false, false, pinned, role, true, OBJECTIVE);
    }

    private static List<SquadCombat.Member> group(double x)
    {
        return List.of(soldier(1, x, BotSettings.ClassRole.ASSAULT, true, false), soldier(2, x + 2D, BotSettings.ClassRole.RIFLEMAN, true, false),
            soldier(3, x + 4D, BotSettings.ClassRole.SUPPORT, true, false));
    }

    private static SquadTactics.Context context(double readiness, Vec3 threat)
    {
        return new SquadTactics.Context(TASK, OBJECTIVE, readiness, HOME, threat, false, null, List.of());
    }

    @Test
    void leaderWaitsForScatteredSquadButOneStuckMemberCannotHaltItForever()
    {
        SquadTactics tactics = new SquadTactics();
        var scattered = List.of(soldier(1, 0D, BotSettings.ClassRole.ASSAULT, true, false), soldier(2, -40D, BotSettings.ClassRole.RIFLEMAN, true, false),
            soldier(3, -45D, BotSettings.ClassRole.SUPPORT, true, false));
        tactics.update(context(1D, null), scattered, 0L);
        assertEquals(SquadTactics.Phase.REGROUP, tactics.getPhase());
        assertTrue(tactics.order(TASK, scattered.get(1).id()).spot().distanceTo(at(0D, 0D)) < 8D);
        tactics.update(context(1D, null), scattered, 200L);
        assertEquals(SquadTactics.Phase.APPROACH, tactics.getPhase());
        tactics.update(context(1D, null), scattered, 220L);
        assertEquals(SquadTactics.Phase.APPROACH, tactics.getPhase());
    }

    @Test
    void occupiedCoverCanAnchorRegrouping()
    {
        SquadTactics tactics = new SquadTactics();
        var members = List.of(soldier(1, 0D, BotSettings.ClassRole.ASSAULT, true, false), soldier(2, -40D, BotSettings.ClassRole.RIFLEMAN, true, false));
        var context = new SquadTactics.Context(TASK, OBJECTIVE, 1D, HOME, null, false, at(-5D, 4D), List.of());
        tactics.update(context, members, 0L);
        assertEquals(at(-5D, 4D), tactics.order(TASK, members.get(0).id()).spot());
    }

    @Test
    void assaultWaitsForSupportingFireAndHasABoundedFallback()
    {
        SquadTactics tactics = new SquadTactics();
        tactics.update(context(1D, OBJECTIVE), group(0D), 0L);
        assertEquals(SquadTactics.Phase.SUPPORT, tactics.getPhase());
        assertEquals(SquadTask.Kind.HOLD, tactics.order(TASK, group(0D).get(0).id()).kind());
        tactics.update(context(1D, OBJECTIVE), group(0D), 20L);
        assertEquals(SquadTactics.Phase.ASSAULT, tactics.getPhase());
        assertEquals(OBJECTIVE, tactics.order(TASK, group(0D).get(0).id()).spot());
        List<SquadCombat.Member> unable = List.of(soldier(1, 0D, BotSettings.ClassRole.ASSAULT, false, false), soldier(2, 2D, BotSettings.ClassRole.SUPPORT, false, false));
        SquadTactics fallback = new SquadTactics();
        fallback.update(context(1D, OBJECTIVE), unable, 0L);
        fallback.update(context(1D, OBJECTIVE), unable, 80L);
        assertEquals(SquadTactics.Phase.ASSAULT, fallback.getPhase());
    }

    @Test
    void damagedSquadWithdrawsWithARearGuardThenRegroupsBeforeReturning()
    {
        SquadTactics tactics = new SquadTactics();
        var members = group(0D);
        tactics.update(context(0.2D, OBJECTIVE), members, 0L);
        assertEquals(SquadTactics.Phase.WITHDRAW, tactics.getPhase());
        assertTrue(tactics.order(TASK, members.get(0).id()).retreat());
        assertFalse(tactics.order(TASK, members.get(2).id()).retreat());
        tactics.update(context(0.2D, OBJECTIVE), members, 60L);
        assertTrue(tactics.order(TASK, members.get(2).id()).retreat());
        tactics.update(context(0.6D, OBJECTIVE), group(-30D), 80L);
        assertEquals(SquadTactics.Phase.REGROUP, tactics.getPhase());
        tactics.update(context(0.6D, OBJECTIVE), group(-30D), 100L);
        assertEquals(SquadTactics.Phase.REGROUP, tactics.getPhase());
        tactics.update(context(0.6D, OBJECTIVE), group(-30D), 140L);
        assertNotEquals(SquadTactics.Phase.WITHDRAW, tactics.getPhase());
    }

    @Test
    void prolongedPinningRequestsSupportWithoutInventingEnemyContacts()
    {
        SquadTactics tactics = new SquadTactics();
        var members = List.of(soldier(1, 0D, BotSettings.ClassRole.ASSAULT, false, true), soldier(2, 2D, BotSettings.ClassRole.SUPPORT, false, true));
        tactics.update(context(0.8D, null), members, 0L);
        assertNull(tactics.supportTarget(0L));
        tactics.update(context(0.8D, null), members, 60L);
        assertEquals(SquadTactics.Report.PINNED, tactics.report(60L));
        assertEquals(OBJECTIVE, tactics.supportTarget(60L));
    }

    @Test
    void repeatedLeaderRouteFailureRequestsANewApproachThenTemporaryReassignment()
    {
        SquadTactics tactics = new SquadTactics();
        var members = group(0D);
        tactics.update(context(1D, null), members, 0L);
        tactics.blocked(members.get(1).id(), 20L);
        assertFalse(tactics.takeRetry(), "a straggler cannot reroute the whole squad");
        tactics.blocked(members.get(0).id(), 20L);
        assertTrue(tactics.takeRetry());
        assertFalse(tactics.takeRetry());
        assertNull(tactics.failedTarget(20L));
        tactics.blocked(members.get(0).id(), 40L);
        assertFalse(tactics.takeRetry(), "failure reports are rate limited");
        tactics.blocked(members.get(0).id(), 220L);
        assertEquals(OBJECTIVE, tactics.failedTarget(220L));
        assertNull(tactics.failedTarget(820L));
    }

    @Test
    void securedPointAssignsDistinctSectorsAndKeepsAssaultTroopsInsideTheCaptureZone()
    {
        SquadTactics tactics = new SquadTactics();
        var context = new SquadTactics.Context(TASK, OBJECTIVE, 1D, HOME, at(150D, 0D), true, null, List.of(at(115D, 0D), at(100D, 15D)));
        var members = group(100D);
        tactics.update(context, members, 0L);
        assertEquals(SquadTactics.Phase.SECURE, tactics.getPhase());
        assertEquals(SquadTactics.Report.SECURED, tactics.report(0L));
        Set<Vec3> positions = new HashSet<>();
        for (var member : members)
            positions.add(tactics.order(TASK, member.id()).spot());
        assertEquals(members.size(), positions.size());
        assertTrue(tactics.order(TASK, members.get(0).id()).spot().distanceTo(OBJECTIVE) < TASK.radius());
    }

    @Test
    void leaderReplacementAndPlayerOverrideDiscardStaleMovement()
    {
        SquadTactics tactics = new SquadTactics();
        var members = group(0D);
        tactics.update(context(1D, null), members, 0L);
        tactics.update(context(1D, null), members.subList(1, members.size()), 20L);
        assertNull(tactics.order(TASK, members.get(0).id()));
        tactics.blocked(members.get(0).id(), 40L);
        assertFalse(tactics.takeRetry());
        tactics.clear();
        assertNull(tactics.order(TASK, members.get(1).id()));
    }
}
