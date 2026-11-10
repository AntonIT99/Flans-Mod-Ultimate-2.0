package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class PacketTeamsActionTest
{
    private static final String TEAM_ID = "militia";

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aTeamWithoutClassesIsConfirmedOnTheTermsOfAClassChoice()
    {
        Team team = mock(Team.class);
        when(team.getClasses()).thenReturn(List.of());
        ServerPlayer player = mock(ServerPlayer.class);
        try (var managers = mockStatic(TeamsManager.class); var teams = mockStatic(Team.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            teams.when(() -> Team.getTeam(TEAM_ID)).thenReturn(team);
            when(manager.selectTeam(player, team, false)).thenReturn(true);

            received(PacketTeamsAction.selectTeam(TEAM_ID)).handleServerSide(player, mock(ServerLevel.class));

            // Never the direct respawn, which would heal, resupply and move a fighter picking their own team again.
            verify(manager).confirmSelection(player);
            verify(manager, never()).respawnPlayer(any(), anyBoolean(), any());
            verify(manager).syncPlayer(player, PacketTeamsState.OpenScreen.CLOSE);
        }
    }

    @Test
    void aTeamWithClassesWaitsForTheClassChoice()
    {
        Team team = mock(Team.class);
        when(team.getClasses()).thenReturn(List.of(mock(PlayerClass.class)));
        ServerPlayer player = mock(ServerPlayer.class);
        try (var managers = mockStatic(TeamsManager.class); var teams = mockStatic(Team.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            teams.when(() -> Team.getTeam(TEAM_ID)).thenReturn(team);
            when(manager.selectTeam(player, team, false)).thenReturn(true);
            when(manager.getCurrentLoadoutPool()).thenReturn(Optional.empty());

            received(PacketTeamsAction.selectTeam(TEAM_ID)).handleServerSide(player, mock(ServerLevel.class));

            verify(manager, never()).confirmSelection(any());
            verify(manager, never()).respawnPlayer(any(), anyBoolean(), any());
            verify(manager).syncPlayer(player, PacketTeamsState.OpenScreen.CLASS_SELECT);
        }
    }

    @Test
    void leavingForTheSpectatorsIsConfirmedLikeAChangeOfSides()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.selectTeam(player, Team.SPECTATORS, false)).thenReturn(true);

            received(PacketTeamsAction.selectTeam(Team.SPECTATORS_ID)).handleServerSide(player, mock(ServerLevel.class));

            // Never the direct respawn, which let a fighter about to die step out and come back healed.
            verify(manager).confirmSelection(player);
            verify(manager, never()).respawnPlayer(any(), anyBoolean(), any());
        }
    }

    @Test
    void aRefusedTeamChangesNothing()
    {
        Team team = mock(Team.class);
        ServerPlayer player = mock(ServerPlayer.class);
        try (var managers = mockStatic(TeamsManager.class); var teams = mockStatic(Team.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            teams.when(() -> Team.getTeam(TEAM_ID)).thenReturn(team);

            received(PacketTeamsAction.selectTeam(TEAM_ID)).handleServerSide(player, mock(ServerLevel.class));

            verify(manager, never()).confirmSelection(any());
            verify(manager, never()).respawnPlayer(any(), anyBoolean(), any());
            verify(manager).syncPlayer(player, PacketTeamsState.OpenScreen.TEAM_SELECT);
        }
    }

    /** The packet as the server reads it off the wire. */
    private static PacketTeamsAction received(PacketTeamsAction sent)
    {
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        PacketTeamsAction packet = new PacketTeamsAction();
        try
        {
            sent.encodeInto(buffer);
            packet.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
        return packet;
    }
}
