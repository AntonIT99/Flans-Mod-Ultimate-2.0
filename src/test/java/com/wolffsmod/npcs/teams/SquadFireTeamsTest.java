package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SquadFireTeamsTest
{
    private static SquadCombat.Member member(int id, BotSettings.ClassRole role, int target, double enemyX, boolean ready, boolean moving)
    {
        return new SquadCombat.Member(new UUID(0L, id), new Vec3(id, 64D, 0D), target, ready, moving, false, false, role, true, new Vec3(enemyX, 64D, 0D));
    }

    private static List<SquadCombat.Member> group()
    {
        return List.of(member(1, BotSettings.ClassRole.ASSAULT, 10, 60D, true, false), member(2, BotSettings.ClassRole.RIFLEMAN, 11, 65D, true, false),
            member(3, BotSettings.ClassRole.RIFLEMAN, 12, 70D, true, false), member(4, BotSettings.ClassRole.SUPPORT, 13, 68D, true, false));
    }

    @Test
    void assaultElementMovesTogetherWhileSupportCoversDifferentEnemiesInTheSamePosition()
    {
        SquadFireTeams teams = new SquadFireTeams();
        var members = group();
        assertEquals(false, teams.covers(members.get(0), members, 0L));
        assertEquals(false, teams.covers(members.get(1), members, 0L));
        assertEquals(true, teams.covers(members.get(2), members, 0L));
        assertEquals(true, teams.covers(members.get(3), members, 0L));
    }

    @Test
    void supportElementFollowsAfterTheFirstElementFinishesABound()
    {
        SquadFireTeams teams = new SquadFireTeams();
        var members = group();
        teams.covers(members.get(0), members, 0L);
        var moving = List.of(member(1, BotSettings.ClassRole.ASSAULT, 10, 60D, false, true), member(2, BotSettings.ClassRole.RIFLEMAN, 11, 65D, false, true), members.get(2), members.get(3));
        assertEquals(true, teams.covers(members.get(3), moving, 5L));
        assertEquals(false, teams.covers(members.get(3), members, 10L));
        assertEquals(true, teams.covers(members.get(0), members, 10L));
    }

    @Test
    void absentCoveringFireAndStalledMoversNeverHoldBothElements()
    {
        SquadFireTeams teams = new SquadFireTeams();
        var members = group();
        teams.covers(members.get(0), members, 0L);
        assertEquals(false, teams.covers(members.get(3), members, 10L), "an element that never starts yields the next move");
        var empty = List.of(member(1, BotSettings.ClassRole.ASSAULT, 10, 60D, false, false), member(2, BotSettings.ClassRole.RIFLEMAN, 11, 65D, false, false),
            member(3, BotSettings.ClassRole.RIFLEMAN, 12, 70D, false, false), member(4, BotSettings.ClassRole.SUPPORT, 13, 68D, false, false));
        for (var member : empty)
            assertEquals(false, teams.covers(member, empty, 20L));
    }

    @Test
    void smallOrUnrelatedGroupsKeepTheExistingPairCoordination()
    {
        SquadFireTeams teams = new SquadFireTeams();
        var members = group();
        assertNull(teams.covers(members.get(0), members.subList(0, 2), 0L));
        var remote = member(4, BotSettings.ClassRole.SUPPORT, 14, 200D, true, false);
        assertNull(teams.covers(remote, List.of(members.get(0), members.get(1), members.get(2), remote), 5L));
    }
}
