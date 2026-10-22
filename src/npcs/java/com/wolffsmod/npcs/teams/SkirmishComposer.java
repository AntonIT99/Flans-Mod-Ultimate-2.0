package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansModApi;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Function;

/**
 * Turns what a skirmish asks a faction to field into the force of each of its branches: loose infantry and squads go to
 * the branches that fill the infantry role, vehicles asked for by role to the branches whose vehicles fill it, both in
 * proportion to the branches' weights, and vehicles asked for by name to the branch that has them. Each branch keeps its
 * own classes, names, clone and crews. Without vehicles asked for, every branch fields the vehicles it lists. Pure
 * arithmetic, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SkirmishComposer
{
    /**
     * A branch as the composer sees it.
     *
     * @param roles
     *            the roles the branch fills: those set by hand, or else every role its classes and vehicles can fill
     * @param vehicleRoles
     *            the role of each vehicle type the branch lists
     */
    record BranchView(String team, Set<BotRole> roles, int weight, BotSettings.TeamForce force, Map<String, BotRole> vehicleRoles)
    {}

    /**
     * What a skirmish asks one faction to field.
     *
     * @param infantry
     *            loose infantry in all, or a negative number for what each branch lists
     * @param squads
     *            squads in all by template, or empty for what each branch lists
     * @param vehiclesByRole
     *            vehicles in all by role, picked from the vehicles the branches list
     * @param vehiclesByType
     *            vehicles in all by type; with any vehicles asked for, branches field only those
     */
    record Request(int infantry, Map<String, Integer> squads, Map<BotRole, Integer> vehiclesByRole, Map<String, Integer> vehiclesByType)
    {
        /** Everything the branches list, as they list it. */
        static final Request AS_LISTED = new Request(-1, Map.of(), Map.of(), Map.of());

        boolean asksForVehicles()
        {
            return !vehiclesByRole.isEmpty() || !vehiclesByType.isEmpty();
        }
    }

    /**
     * @param forces
     *            the force of each branch, by team short name
     * @param missingRoles
     *            roles asked for that no branch can fill
     * @param unknownTypes
     *            vehicle types asked for by name that make no vehicle NPC
     */
    record Result(Map<String, BotSettings.TeamForce> forces, Set<BotRole> missingRoles, Set<String> unknownTypes)
    {}

    /**
     * @param roleOfType
     *            the role of a vehicle type, for types asked for by name that no branch lists; empty for no vehicle
     */
    static Result compose(List<BranchView> branches, Request request, Function<String, Optional<BotRole>> roleOfType)
    {
        Map<String, BotSettings.TeamForce> forces = cleared(branches, request);
        List<BranchView> infantry = branches.stream().filter(branch -> branch.roles().contains(BotRole.INFANTRY)).toList();
        Set<BotRole> missing = EnumSet.noneOf(BotRole.class);
        if (request.infantry() > 0 || !request.squads().isEmpty())
        {
            if (infantry.isEmpty())
                missing.add(BotRole.INFANTRY);
            else
                composeInfantry(infantry, request, forces);
        }
        request.vehiclesByRole().forEach((role, count) ->
        {
            if (!composeRole(branches, role, count, forces))
                missing.add(role);
        });
        Set<String> unknown = new TreeSet<>();
        request.vehiclesByType().forEach((type, count) ->
        {
            if (!composeType(branches, type, count, roleOfType, forces))
                unknown.add(type);
        });
        return new Result(forces, missing, unknown);
    }

    /** A copy of each branch's force, without what the request asks for in its place. */
    private static Map<String, BotSettings.TeamForce> cleared(List<BranchView> branches, Request request)
    {
        Map<String, BotSettings.TeamForce> forces = new LinkedHashMap<>();
        for (BranchView branch : branches)
        {
            BotSettings.TeamForce force = BotSettings.TeamForce.load(branch.force().save());
            if (request.infantry() >= 0)
                force.setCount(0);
            if (!request.squads().isEmpty())
                force.getSquads().clear();
            if (request.asksForVehicles())
                force.getVehicles().clear();
            forces.put(branch.team(), force);
        }
        return forces;
    }

    private static void composeInfantry(List<BranchView> infantry, Request request, Map<String, BotSettings.TeamForce> forces)
    {
        int[] weights = infantry.stream().mapToInt(BranchView::weight).toArray();
        if (request.infantry() >= 0)
        {
            int[] shares = apportion(request.infantry(), weights);
            for (int i = 0; i < infantry.size(); i++)
                forces.get(infantry.get(i).team()).setCount(shares[i]);
        }
        request.squads().forEach((template, count) ->
        {
            int[] shares = apportion(count, weights);
            for (int i = 0; i < infantry.size(); i++)
                if (shares[i] > 0)
                    forces.get(infantry.get(i).team()).getSquads().add(new BotSettings.SquadEntry(template, shares[i], 0));
        });
    }

    /** @return whether a branch can field vehicles of the role */
    private static boolean composeRole(List<BranchView> branches, BotRole role, int count, Map<String, BotSettings.TeamForce> forces)
    {
        List<BranchView> filling = new ArrayList<>();
        List<List<BotSettings.VehicleEntry>> entries = new ArrayList<>();
        for (BranchView branch : branches)
        {
            List<BotSettings.VehicleEntry> ofRole = branch.force().getVehicles().stream().filter(entry -> branch.vehicleRoles().get(entry.type()) == role).toList();
            if (branch.roles().contains(role) && !ofRole.isEmpty())
            {
                filling.add(branch);
                entries.add(ofRole);
            }
        }
        if (filling.isEmpty())
            return false;
        int[] shares = apportion(count, filling.stream().mapToInt(BranchView::weight).toArray());
        for (int i = 0; i < filling.size(); i++)
        {
            // A branch's share is spread over its vehicles of the role, the first ones getting the remainder.
            List<BotSettings.VehicleEntry> ofRole = entries.get(i);
            int[] perEntry = apportion(shares[i], ofRole.stream().mapToInt(entry -> 1).toArray());
            for (int j = 0; j < ofRole.size(); j++)
                if (perEntry[j] > 0)
                    add(forces.get(filling.get(i).team()), ofRole.get(j), perEntry[j]);
        }
        return true;
    }

    /**
     * A type asked for by name goes to the branch that lists it, or else to the first that fills its role, or the first.
     *
     * @return whether the type makes a vehicle NPC
     */
    private static boolean composeType(List<BranchView> branches, String type, int count, Function<String, Optional<BotRole>> roleOfType, Map<String, BotSettings.TeamForce> forces)
    {
        if (branches.isEmpty() || count <= 0)
            return true;
        for (BranchView branch : branches)
        {
            Optional<BotSettings.VehicleEntry> listed = branch.force().getVehicles().stream().filter(entry -> entry.type().equals(type)).findFirst();
            if (listed.isPresent())
            {
                add(forces.get(branch.team()), listed.get(), count);
                return true;
            }
        }
        Optional<BotRole> role = roleOfType.apply(type);
        if (role.isEmpty())
            return false;
        BranchView branch = branches.stream().filter(candidate -> candidate.roles().contains(role.get())).findFirst().orElse(branches.get(0));
        add(forces.get(branch.team()), new BotSettings.VehicleEntry(type, count, 0, 0, 1, null, ""), count);
        return true;
    }

    /** Adds vehicles of a listed entry to a force, keeping the entry's budget, respawn, cost and cargo. */
    private static void add(BotSettings.TeamForce force, BotSettings.VehicleEntry listed, int count)
    {
        List<BotSettings.VehicleEntry> vehicles = force.getVehicles();
        int index = vehicles.stream().map(BotSettings.VehicleEntry::type).toList().indexOf(listed.type());
        int total = index < 0 ? count : vehicles.get(index).count() + count;
        BotSettings.VehicleEntry entry = new BotSettings.VehicleEntry(listed.type(), total, listed.budget(), listed.respawnSeconds(), listed.ticketCost(), null, listed.carry());
        if (index < 0)
            vehicles.add(entry);
        else
            vehicles.set(index, entry);
    }

    /** Splits a total by weights, largest remainders first, so the shares always add up to the total. */
    static int[] apportion(int total, int[] weights)
    {
        int[] shares = new int[weights.length];
        long sum = Arrays.stream(weights).mapToLong(weight -> Math.max(0, weight)).sum();
        if (total <= 0 || sum <= 0)
            return shares;
        double[] remainders = new double[weights.length];
        int given = 0;
        for (int i = 0; i < weights.length; i++)
        {
            double exact = (double) total * Math.max(0, weights[i]) / sum;
            shares[i] = (int) Math.floor(exact);
            remainders[i] = exact - shares[i];
            given += shares[i];
        }
        Integer[] order = new Integer[weights.length];
        for (int i = 0; i < order.length; i++)
            order[i] = i;
        // Ties go to the earlier branch, so the same request always gives the same force.
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> -remainders[i]).thenComparingInt(i -> i));
        for (int i = 0; given < total; i++, given++)
            shares[order[i % order.length]]++;
        return shares;
    }

    // --- Branches as the composer sees them ---

    /** Branches of a managed faction, with the roles they fill and the roles of the vehicles they list. */
    static List<BranchView> views(Collection<BattleFactions.Branch> branches)
    {
        List<BranchView> views = new ArrayList<>();
        for (BattleFactions.Branch branch : branches)
        {
            Map<String, BotRole> vehicleRoles = new LinkedHashMap<>();
            for (BotSettings.VehicleEntry entry : branch.getForce().getVehicles())
                roleOfType(entry.type()).ifPresent(role -> vehicleRoles.put(entry.type(), role));
            Set<BotRole> roles = EnumSet.noneOf(BotRole.class);
            if (branch.getRoles().isEmpty())
            {
                boolean soldiers = FlansModApi.getTeam(branch.getTeam()).map(team -> !team.getClasses().isEmpty()).orElse(false);
                if (soldiers)
                    roles.add(BotRole.INFANTRY);
                roles.addAll(vehicleRoles.values());
            }
            else
                roles.addAll(branch.getRoles());
            views.add(new BranchView(branch.getTeam(), roles, branch.getWeight(), branch.getForce(), vehicleRoles));
        }
        return views;
    }

    /** @return the role of the vehicle NPC a definition makes, or empty when it makes none */
    static Optional<BotRole> roleOfType(String type)
    {
        return FlanModelNpcs.type(type).map(model -> BotRole.of(model.getKind(), model.getInfoType()));
    }
}
