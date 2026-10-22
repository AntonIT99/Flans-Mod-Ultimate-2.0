package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SkirmishComposerTest
{
    private static final Map<String, BotRole> ROLES = Map.of("tiger", BotRole.ARMOR, "panzer4", BotRole.ARMOR, "flak88", BotRole.AA, "bf109", BotRole.PLANE, "sherman", BotRole.ARMOR);

    private static BotSettings.VehicleEntry entry(String type, int count)
    {
        return new BotSettings.VehicleEntry(type, count, 0, 45, 2, null, "");
    }

    private static SkirmishComposer.BranchView branch(String team, int weight, Set<BotRole> roles, int infantry, BotSettings.VehicleEntry... vehicles)
    {
        BotSettings.TeamForce force = new BotSettings.TeamForce();
        force.setCount(infantry);
        force.setNameList("german_names");
        force.getSquads().add(new BotSettings.SquadEntry("grenadiers", 1, 0));
        Map<String, BotRole> vehicleRoles = new LinkedHashMap<>();
        for (BotSettings.VehicleEntry vehicle : vehicles)
        {
            force.getVehicles().add(vehicle);
            vehicleRoles.put(vehicle.type(), ROLES.get(vehicle.type()));
        }
        return new SkirmishComposer.BranchView(team, roles, weight, force, vehicleRoles);
    }

    /** Wehrmacht: infantry and tanks, weight 3. Afrikakorps: infantry and tanks, weight 1. Luftwaffe: planes and AA. */
    private static List<SkirmishComposer.BranchView> germany()
    {
        return List.of(branch("wehrmacht", 3, EnumSet.of(BotRole.INFANTRY, BotRole.ARMOR), 10, entry("tiger", 2), entry("panzer4", 4)),
            branch("afrikakorps", 1, EnumSet.of(BotRole.INFANTRY, BotRole.ARMOR), 6, entry("panzer4", 1)),
            branch("luftwaffe", 1, EnumSet.of(BotRole.PLANE, BotRole.AA), 0, entry("bf109", 2), entry("flak88", 1)));
    }

    private static SkirmishComposer.Result compose(SkirmishComposer.Request request)
    {
        return SkirmishComposer.compose(germany(), request, type -> Optional.ofNullable(ROLES.get(type)));
    }

    private static int vehicles(SkirmishComposer.Result result, String team, String type)
    {
        return result.forces().get(team).getVehicles().stream().filter(entry -> entry.type().equals(type)).mapToInt(BotSettings.VehicleEntry::count).sum();
    }

    @Test
    void withNothingAskedEveryBranchFieldsWhatItLists()
    {
        SkirmishComposer.Result result = compose(SkirmishComposer.Request.AS_LISTED);
        assertEquals(10, result.forces().get("wehrmacht").getCount());
        assertEquals(6, result.forces().get("afrikakorps").getCount());
        assertEquals(2, vehicles(result, "wehrmacht", "tiger"));
        assertEquals(2, vehicles(result, "luftwaffe", "bf109"));
        assertTrue(result.missingRoles().isEmpty());
    }

    @Test
    void infantryIsSharedByTheInfantryBranchesByWeight()
    {
        SkirmishComposer.Result result = compose(new SkirmishComposer.Request(16, Map.of(), Map.of(), Map.of()));
        assertEquals(12, result.forces().get("wehrmacht").getCount());
        assertEquals(4, result.forces().get("afrikakorps").getCount());
        assertEquals(0, result.forces().get("luftwaffe").getCount(), "a branch that fills no infantry role fields no soldiers");
        assertEquals(2, vehicles(result, "wehrmacht", "tiger"), "vehicles not asked for stay as listed");
    }

    @Test
    void sharesAlwaysAddUpToTheTotal()
    {
        assertArrayEquals(new int[]{4, 3, 3}, SkirmishComposer.apportion(10, new int[]{1, 1, 1}));
        assertArrayEquals(new int[]{1, 0}, SkirmishComposer.apportion(1, new int[]{1, 1}), "ties go to the earlier branch");
        assertArrayEquals(new int[]{0, 0}, SkirmishComposer.apportion(5, new int[]{0, 0}));
        assertEquals(7, Arrays.stream(SkirmishComposer.apportion(7, new int[]{3, 1, 2})).sum());
    }

    @Test
    void squadsAskedForReplaceTheListedOnesAndAreSharedByWeight()
    {
        SkirmishComposer.Result result = compose(new SkirmishComposer.Request(-1, Map.of("panzergrenadiere", 4), Map.of(), Map.of()));
        assertEquals(List.of(new BotSettings.SquadEntry("panzergrenadiere", 3, 0)), result.forces().get("wehrmacht").getSquads());
        assertEquals(List.of(new BotSettings.SquadEntry("panzergrenadiere", 1, 0)), result.forces().get("afrikakorps").getSquads());
        assertEquals(10, result.forces().get("wehrmacht").getCount(), "infantry not asked for stays as listed");
    }

    @Test
    void vehiclesAskedForByRoleComeFromTheBranchesThatListThem()
    {
        SkirmishComposer.Result result = compose(new SkirmishComposer.Request(-1, Map.of(), Map.of(BotRole.ARMOR, 8, BotRole.AA, 1), Map.of()));
        assertEquals(6, vehicles(result, "wehrmacht", "tiger") + vehicles(result, "wehrmacht", "panzer4"));
        assertEquals(3, vehicles(result, "wehrmacht", "tiger"), "a branch's share is spread over its vehicles of the role");
        assertEquals(2, vehicles(result, "afrikakorps", "panzer4"));
        assertEquals(1, vehicles(result, "luftwaffe", "flak88"));
        assertEquals(0, vehicles(result, "luftwaffe", "bf109"), "with vehicles asked for, branches field only those");
        BotSettings.VehicleEntry tiger = result.forces().get("wehrmacht").getVehicles().stream().filter(entry -> entry.type().equals("tiger")).findFirst().orElseThrow();
        assertEquals(45, tiger.respawnSeconds(), "a vehicle keeps what its listed entry says about respawn and cost");
        assertEquals(2, tiger.ticketCost());
    }

    @Test
    void rolesNoBranchFillsAreReported()
    {
        SkirmishComposer.Result result = compose(new SkirmishComposer.Request(-1, Map.of(), Map.of(BotRole.NAVAL, 2, BotRole.HELICOPTER, 1), Map.of()));
        assertEquals(EnumSet.of(BotRole.NAVAL, BotRole.HELICOPTER), result.missingRoles());
    }

    @Test
    void aBranchSetToOtherRolesDoesNotFieldItsListedVehicles()
    {
        List<SkirmishComposer.BranchView> branches = List.of(branch("wehrmacht", 1, EnumSet.of(BotRole.INFANTRY), 10, entry("tiger", 2)));
        SkirmishComposer.Result result = SkirmishComposer.compose(branches, new SkirmishComposer.Request(-1, Map.of(), Map.of(BotRole.ARMOR, 2), Map.of()), type -> Optional.empty());
        assertEquals(EnumSet.of(BotRole.ARMOR), result.missingRoles());
    }

    @Test
    void vehiclesAskedForByNameGoToTheBranchThatListsThemOrFillsTheirRole()
    {
        SkirmishComposer.Result result = compose(new SkirmishComposer.Request(-1, Map.of(), Map.of(), Map.of("panzer4", 3, "sherman", 2, "nothing", 1)));
        assertEquals(3, vehicles(result, "wehrmacht", "panzer4"), "the first branch listing a type gets it");
        assertEquals(0, vehicles(result, "afrikakorps", "panzer4"));
        assertEquals(2, vehicles(result, "wehrmacht", "sherman"), "an unlisted type goes to the first branch filling its role");
        assertEquals(Set.of("nothing"), result.unknownTypes());
    }

    @Test
    void branchesKeepTheirOwnSetupAndTheListedForceIsLeftAlone()
    {
        List<SkirmishComposer.BranchView> branches = germany();
        SkirmishComposer.Result result = SkirmishComposer.compose(branches, new SkirmishComposer.Request(20, Map.of(), Map.of(BotRole.ARMOR, 1), Map.of()), type -> Optional.empty());
        assertEquals("german_names", result.forces().get("wehrmacht").getNameList());
        assertEquals(10, branches.get(0).force().getCount(), "composing never changes the branch itself");
        assertEquals(2, branches.get(0).force().getVehicles().size());
    }
}
