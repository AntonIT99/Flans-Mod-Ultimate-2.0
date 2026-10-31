package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssaultOvertimeTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void timeoutDoesNotMaskAnOvertimeOpportunityButRunningOutOfReinforcementsStillWins()
    {
        Team defenders = mock(Team.class);
        Team attackers = mock(Team.class);
        when(defenders.getShortName()).thenReturn("defenders");
        when(attackers.getShortName()).thenReturn("attackers");
        TeamsManager manager = mock(TeamsManager.class);
        TeamsRound round = new TeamsRound("map", "as", List.of("defenders", "attackers"), 10, 20);
        when(manager.getCurrentRound()).thenReturn(Optional.of(round));
        ITeamCapturePoint point = mock(ITeamCapturePoint.class);
        when(point.getCaptureState()).thenReturn(CaptureState.ownedBy(2));
        when(manager.getCapturePoints()).thenReturn(List.of(point));
        GameType assault = GameTypes.ASSAULT;
        try (var teams = mockStatic(Team.class))
        {
            teams.when(() -> Team.getTeam("attackers")).thenReturn(attackers);
            teams.when(() -> Team.getTeam("defenders")).thenReturn(defenders);
            when(manager.getRoundTimeLeftTicks()).thenReturn(0);
            assertTrue(assault.hasWinner(manager, defenders));
            assertFalse(assault.hasWinnerBeforeTimeLimit(manager, defenders));
            when(manager.getTeamScore(attackers)).thenReturn(20);
            assertTrue(assault.hasWinnerBeforeTimeLimit(manager, defenders));
            when(point.getCaptureState()).thenReturn(CaptureState.ownedBy(3));
            assertTrue(assault.hasWinnerBeforeTimeLimit(manager, attackers));
        }
    }
}
