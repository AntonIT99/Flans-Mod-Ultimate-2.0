package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotSettingsTextTest
{
    @Test
    void classWeightsRoundTripAndDefaultToOne()
    {
        Map<String, Integer> weights = BotSettingsText.parseClassWeights("rifleman:3, mg medic:0");
        assertEquals(Map.of("rifleman", 3, "mg", 1, "medic", 0), weights);
        assertEquals(weights, BotSettingsText.parseClassWeights(BotSettingsText.formatClassWeights(weights)));
    }

    @Test
    void squadsKeepTheirBudgets()
    {
        List<BotSettings.SquadEntry> squads = BotSettingsText.parseSquads("Grenadiers:4:6, recon");
        assertEquals(List.of(new BotSettings.SquadEntry("grenadiers", 4, 6), new BotSettings.SquadEntry("recon", 1, 0)), squads);
        assertEquals("grenadiers:4:6, recon:1", BotSettingsText.formatSquads(squads));
    }

    @Test
    void vehiclesKeepTheSpawnerTheyHadAndCarryASquad()
    {
        UUID spawner = UUID.randomUUID();
        List<BotSettings.VehicleEntry> before = List.of(new BotSettings.VehicleEntry("tiger", 2, 0, 0, 1, spawner, ""));
        List<BotSettings.VehicleEntry> vehicles = BotSettingsText.parseVehicles("tiger:3:6:90:5, opel:2:0:0:1:Grenadiers, sherman", before);
        assertEquals(new BotSettings.VehicleEntry("tiger", 3, 6, 90, 5, spawner, ""), vehicles.get(0));
        assertEquals(new BotSettings.VehicleEntry("opel", 2, 0, 0, 1, null, "grenadiers"), vehicles.get(1));
        assertEquals(new BotSettings.VehicleEntry("sherman", 1, 0, 0, 1, null, ""), vehicles.get(2));
        assertEquals(vehicles, BotSettingsText.parseVehicles(BotSettingsText.formatVehicles(vehicles), vehicles));
    }

    @Test
    void textThatIsNoNumberTakesTheFallback()
    {
        assertEquals(7, BotSettingsText.parseNumber("seven", 7));
        assertEquals(12, BotSettingsText.parseNumber(" 12 ", 7));
        assertEquals(0.5F, BotSettingsText.parseDecimal("0.5", 1F));
        assertEquals(1F, BotSettingsText.parseDecimal("NaN", 1F));
        assertTrue(BotSettingsText.parseClassWeights("  ").isEmpty());
    }
}
