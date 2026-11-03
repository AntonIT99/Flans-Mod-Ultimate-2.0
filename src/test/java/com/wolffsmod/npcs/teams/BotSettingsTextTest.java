package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.crew.CrewPost;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotSettingsTextTest
{
    @Test
    void aircraftRolesSurviveAllocationEditingAndNbtRoundTrips()
    {
        var role = com.wolffsmod.npcs.sim.NpcTuning.AircraftRole.BOMBER;
        BotSettings.VehicleEntry original = new BotSettings.VehicleEntry("bomber", 2, 4, 15, 3, null, "", List.of(0)).withAircraftRole(role);
        var parsed = BotSettingsText.parseVehicles(BotSettingsText.formatVehicles(List.of(original)), List.of());
        assertEquals(original, parsed.get(0));
        assertEquals(role, BotSettingsText.parseVehicles("bomber:3", List.of(original)).get(0).aircraftRole());
        assertEquals(role, BotSettings.VehicleEntry.load(original.save()).aircraftRole());
        CompoundTag legacy = original.save();
        legacy.remove("aircraft_role");
        assertEquals(com.wolffsmod.npcs.sim.NpcTuning.AircraftRole.AUTO, BotSettings.VehicleEntry.load(legacy).aircraftRole());
    }

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
    void omittedPaintjobsAllowAllForNewVehiclesAndPreserveExistingSelections()
    {
        BotSettings.VehicleEntry defaultOnly = new BotSettings.VehicleEntry("tiger", 1, 0, 0, 1, null, "", List.of(0));
        BotSettings.VehicleEntry selected = new BotSettings.VehicleEntry("opel", 1, 0, 0, 1, null, "", List.of(2, 4));
        List<BotSettings.VehicleEntry> vehicles = BotSettingsText.parseVehicles("tiger, opel, sherman", List.of(defaultOnly, selected));
        assertEquals(List.of(0), vehicles.get(0).paintjobs());
        assertEquals(List.of(2, 4), vehicles.get(1).paintjobs());
        assertTrue(vehicles.get(2).paintjobs().isEmpty(), "New allocations allow every paintjob");
        assertEquals(vehicles, BotSettingsText.parseVehicles(BotSettingsText.formatVehicles(vehicles), List.of()));
    }

    @Test
    void explicitPaintjobsReplaceThePreviousSelection()
    {
        BotSettings.VehicleEntry previous = new BotSettings.VehicleEntry("tiger", 1, 0, 0, 1, null, "", List.of(0));
        assertTrue(BotSettingsText.parseVehicles("tiger:1:0:0:1::*", List.of(previous)).get(0).paintjobs().isEmpty());
        assertEquals(List.of(2, 4), BotSettingsText.parseVehicles("tiger:1:0:0:1::2|4", List.of(previous)).get(0).paintjobs());
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

    @Test
    void classRolesReadWithOrWithoutTheUnderscore()
    {
        Map<String, BotSettings.ClassRole> roles = BotSettingsText.parseRoles("medic_ger:medic, panzerschreck:antitank, mg:support, odd:pilot");
        assertEquals(BotSettings.ClassRole.MEDIC, roles.get("medic_ger"));
        assertEquals(BotSettings.ClassRole.ANTI_TANK, roles.get("panzerschreck"));
        assertEquals(BotSettings.ClassRole.SUPPORT, roles.get("mg"));
        assertFalse(roles.containsKey("odd"), "a role that is not known is left out");
        assertEquals(roles, BotSettingsText.parseRoles(BotSettingsText.formatRoles(roles)));
    }

    @Test
    void crewClassesShowAnyForAClassPickedByWeight()
    {
        BotSettings.TeamForce force = new BotSettings.TeamForce();
        force.setCrewClass(CrewPost.DRIVER, "tank_crewman");
        force.setCrewClass(CrewPost.GUNNER, BotSettingsText.parseCrewClass("Any"));
        force.setCrewClass(CrewPost.PASSENGER, BotSettingsText.parseCrewClass(" none "));
        assertEquals("driver:tank_crewman, gunner:any, passenger:none", BotSettingsText.formatCrew(force));
        assertEquals("", force.crewClass(CrewPost.GUNNER));
    }
}
