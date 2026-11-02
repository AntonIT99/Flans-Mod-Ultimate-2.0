package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.ITeam;
import org.junit.jupiter.api.Test;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotVehicleToggleTest
{
    @Test
    void disablingVehiclesRetiresOnlyVehicleSlotsAndBlocksQueuedSpawns()
    {
        BotSettings settings = new BotSettings();
        settings.setVehiclesEnabled(false);
        BotSettings.TeamForce force = settings.team("red");
        force.setCount(1);
        force.getVehicles().add(new BotSettings.VehicleEntry("tank", 1, 0, 0, 1, null, ""));
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn("red");
        Battleground ground = mock(Battleground.class);
        when(ground.teams()).thenReturn(List.of(team));
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, settings, Map.of());
        BotSlot infantry = battle.newSlot("red", BotSlot.Source.INFANTRY, 0);
        BotSlot vehicle = battle.newSlot("red", BotSlot.Source.VEHICLE, 0);
        vehicle.setRole("tank");
        vehicle.setQueued(true);
        battle.getQueue().add(vehicle.getId());
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of());

        assertFalse(BotPopulation.canSpawn(battle, vehicle));
        BotPopulation.upkeep(server, battle, 100L, new Random(1));
        assertTrue(vehicle.isRetired());
        assertFalse(infantry.isRetired());
        assertTrue(BotPopulation.canSpawn(battle, infantry));
        assertEquals(1, force.getVehicles().size());

        settings.setVehiclesEnabled(true);
        BotSlot replacement = battle.newSlot("red", BotSlot.Source.VEHICLE, 0);
        assertTrue(BotPopulation.canSpawn(battle, replacement));
    }
}
