package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import org.junit.jupiter.api.Test;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotTeamSquadSizeTest
{
    @Test
    void oldRoundDefaultsAndTeamOverridesSurviveCopies()
    {
        BotSettings settings = new BotSettings();
        settings.setSquadSize(8);
        settings.team("red").setSquadSize(3);
        BotSettings loaded = BotSettings.load(settings.save());
        assertEquals(3, loaded.squadSize("red"));
        assertEquals(8, loaded.squadSize("blue"));
        loaded.setSquadSize(5);
        assertEquals(3, loaded.squadSize("red"));
        assertEquals(5, loaded.squadSize("blue"));
        BotSettings copy = BotPresets.carryOver(settings.save(), List.of("red", "blue"), List.of("green", "gold"));
        assertEquals(3, copy.squadSize("green"));
        assertEquals(8, copy.squadSize("gold"));
        copy.team("green").setSquadSize(0);
        assertEquals(8, BotSettings.load(copy.save()).squadSize("green"));
    }

    @Test
    void teamsFormAndResizeLooseSquadsIndependently()
    {
        BotSettings settings = new BotSettings();
        settings.setSquadSize(6);
        settings.team("red").setCount(6);
        settings.team("red").setSquadSize(2);
        settings.team("blue").setCount(6);
        IPlayerClass playerClass = mock(IPlayerClass.class);
        when(playerClass.getShortName()).thenReturn("rifleman");
        ITeam red = team("red", playerClass);
        ITeam blue = team("blue", playerClass);
        Battleground ground = mock(Battleground.class);
        when(ground.teams()).thenReturn(List.of(red, blue));
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of());
        BotPopulation.upkeep(server, battle, 100L, new Random(1));
        assertEquals(List.of(2, 2, 2), sizes(battle, "red"));
        assertEquals(List.of(6), sizes(battle, "blue"));
        settings.team("blue").setSquadSize(3);
        BotPopulation.upkeep(server, battle, 101L, new Random(1));
        assertEquals(List.of(2, 2, 2), sizes(battle, "red"));
        assertEquals(List.of(3, 3), sizes(battle, "blue"));
    }

    private static ITeam team(String name, IPlayerClass playerClass)
    {
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn(name);
        doReturn(List.of(playerClass)).when(team).getClasses();
        return team;
    }

    private static List<Integer> sizes(BotBattle battle, String team)
    {
        return battle.getSquads().values().stream().filter(squad -> !squad.isDisbanded() && squad.getTeam().equals(team) && squad.getSource() == BotSlot.Source.INFANTRY)
            .map(squad -> squad.getSlots().size()).filter(size -> size > 0).sorted().toList();
    }
}
