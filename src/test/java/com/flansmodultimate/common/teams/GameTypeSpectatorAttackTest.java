package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerPlayer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameTypeSpectatorAttackTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void onlyMembersOfPlayingTeamsFight()
    {
        Team red = mock(Team.class);
        Team blue = mock(Team.class);
        assertTrue(GameType.mayFight(red, blue));
        assertFalse(GameType.mayFight(Team.SPECTATORS, blue));
        assertFalse(GameType.mayFight(red, Team.SPECTATORS));
        assertFalse(GameType.mayFight(null, blue));
        assertFalse(GameType.mayFight(red, null));
    }

    @Test
    void aSpectatorWhoKeptAWeaponCannotHurtAFighter()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            ServerPlayer fighter = mock(ServerPlayer.class);
            ServerPlayer enemy = mock(ServerPlayer.class);
            ServerPlayer watcher = mock(ServerPlayer.class);
            when(manager.getPlayerTeam(fighter)).thenReturn(mock(Team.class));
            when(manager.getPlayerTeam(enemy)).thenReturn(mock(Team.class));
            when(manager.getPlayerTeam(watcher)).thenReturn(Team.SPECTATORS);
            // Run the real rule without registering a second global team deathmatch game type.
            GameType game = mock(GameTypeTDM.class, CALLS_REAL_METHODS);

            assertTrue(game.canPlayerBeAttacked(fighter, enemy));
            assertFalse(game.canPlayerBeAttacked(watcher, enemy));
            assertFalse(game.canPlayerBeAttacked(fighter, watcher));
            // The rule for any two fighters hands two players to the same check.
            assertTrue(game.canBeAttacked(fighter, enemy));
            assertFalse(game.canBeAttacked(fighter, watcher));
        }
    }
}
