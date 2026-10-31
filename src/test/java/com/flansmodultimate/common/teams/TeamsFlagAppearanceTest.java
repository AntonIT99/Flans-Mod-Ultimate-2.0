package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsFlagAppearanceTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void idleOwnershipRequiresAgreementOnThePolesMap()
    {
        TeamsManager manager = mock(TeamsManager.class);
        ITeamBase base = mock(ITeamBase.class);
        Team red = mock(Team.class);
        Team blue = mock(Team.class);
        TeamsRound first = mock(TeamsRound.class);
        TeamsRound second = mock(TeamsRound.class);
        when(base.getMapId()).thenReturn("harbour");
        when(base.getOwnerId()).thenReturn(2);
        when(first.getMapId()).thenReturn("harbour");
        when(second.getMapId()).thenReturn("other");
        when(first.getTeam(0)).thenReturn(red);
        when(second.getTeam(0)).thenReturn(blue);
        when(manager.getCurrentRound()).thenReturn(Optional.empty());
        when(manager.getRounds()).thenReturn(List.of(first, second));
        when(manager.getDisplayTeamForBase(base)).thenCallRealMethod();

        assertSame(red, manager.getDisplayTeamForBase(base));
        when(second.getMapId()).thenReturn("harbour");
        assertNull(manager.getDisplayTeamForBase(base));
        when(second.getTeam(0)).thenReturn(red);
        assertSame(red, manager.getDisplayTeamForBase(base));
        when(manager.getCurrentRound()).thenReturn(Optional.of(first));
        when(second.getTeam(0)).thenReturn(blue);
        assertSame(red, manager.getDisplayTeamForBase(base));
        when(base.getOwnerId()).thenReturn(0);
        assertNull(manager.getDisplayTeamForBase(base));
    }
}
