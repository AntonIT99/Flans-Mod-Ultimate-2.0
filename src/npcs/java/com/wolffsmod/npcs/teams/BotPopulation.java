package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.IPlayerClass;
import com.flansmodultimate.api.ITeam;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Keeps each team's force at what the round's settings ask for, and decides which fallen bots may come back: slots are
 * added and retired as the settings and the players change, squads are raised and lost, and every spawn is weighed
 * against the unit budgets and the side's tickets. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotPopulation
{
    /** Brings every team's force in line with the settings, then queues the fallen bots that may come back. */
    static void upkeep(MinecraftServer server, BotRound round, long now, RandomGenerator random)
    {
        BotSettings settings = round.getSettings();
        if (!settings.isEnabled())
        {
            round.getSlots().values().forEach(slot -> slot.setRetired(true));
            round.getSquads().values().forEach(squad -> squad.setDisbanded(true));
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ITeam team : round.getLive().getRound().getTeams())
            counts.put(team.getShortName(), settings.team(team.getShortName()).getCount());
        Map<String, Integer> targets = rubberBand(round, ForcePlanner.infantryTargets(settings.getMode(), settings.getTotal(), counts, playersPerTeam(server)));
        for (ITeam team : round.getLive().getRound().getTeams())
        {
            BotSettings.TeamForce force = settings.team(team.getShortName());
            keepInfantry(round, team, force, targets.getOrDefault(team.getShortName(), 0), random);
            keepSquads(round, team, force, random);
            keepVehicles(round, team, force);
        }
        round.setOpening(false);
        for (BotSlot slot : round.getSlots().values())
        {
            if (!slot.isRetired() && slot.getEntity() == null && !slot.isQueued() && now >= slot.getRespawnAt() && canSpawn(round, slot))
            {
                slot.setQueued(true);
                round.getQueue().add(slot.getId());
            }
        }
    }

    /** Players fighting for each team, by short name. */
    static Map<String, Integer> playersPerTeam(MinecraftServer server)
    {
        Map<String, Integer> players = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            FlansTeams.getTeam(player).ifPresent(team -> players.merge(team.getShortName(), 1, Integer::sum));
        return players;
    }

    private static void keepInfantry(BotRound round, ITeam team, BotSettings.TeamForce force, int target, RandomGenerator random)
    {
        List<BotSlot> active = active(round, team.getShortName(), BotSlot.Source.INFANTRY, -1);
        for (int i = active.size(); i < target; i++)
        {
            BotSlot slot = round.newSlot(team.getShortName(), BotSlot.Source.INFANTRY, -1);
            slot.setRole(pickClass(team, force, random));
            BotSquad squad = looseSquad(round, team.getShortName());
            squad.getSlots().add(slot.getId());
            slot.setSquad(squad.getId());
        }
        retireExtra(active, target);
    }

    /** The loose infantry squad of a team that still has room, raised when none has. */
    private static BotSquad looseSquad(BotRound round, String team)
    {
        int size = round.getSettings().getSquadSize();
        return round.getSquads().values().stream()
            .filter(squad -> !squad.isDisbanded() && squad.getTeam().equals(team) && squad.getSource() == BotSlot.Source.INFANTRY && liveMembers(round, squad) < size).findFirst()
            .orElseGet(() -> round.newSquad(team, BotRole.INFANTRY, BotSlot.Source.INFANTRY, -1, ""));
    }

    private static long liveMembers(BotRound round, BotSquad squad)
    {
        return squad.getSlots().stream().map(round.getSlots()::get).filter(slot -> slot != null && !slot.isRetired()).count();
    }

    /**
     * Raises the squads of each squad entry. A squad of an entry without a budget gets its fallen members back one by one;
     * one of an entry with a budget fights until it is wiped out, and the next squad takes its place while the budget
     * lasts.
     */
    private static void keepSquads(BotRound round, ITeam team, BotSettings.TeamForce force, RandomGenerator random)
    {
        String name = team.getShortName();
        for (BotSquad squad : round.getSquads().values())
        {
            boolean outdated = squad.getSource() == BotSlot.Source.SQUAD && squad.getTeam().equals(name)
                && (squad.getEntry() >= force.getSquads().size() || !force.getSquads().get(squad.getEntry()).template().equals(squad.getTemplate()));
            if (outdated)
                disband(round, squad);
        }
        for (int entry = 0; entry < force.getSquads().size(); entry++)
        {
            int index = entry;
            SquadTemplates.get().find(force.getSquads().get(entry).template()).ifPresent(template -> keepSquadEntry(round, team, force, index, template, random));
        }
    }

    private static void keepSquadEntry(BotRound round, ITeam team, BotSettings.TeamForce force, int entry, SquadTemplate template, RandomGenerator random)
    {
        String name = team.getShortName();
        BotSettings.SquadEntry setting = force.getSquads().get(entry);
        if (setting.budget() > 0)
            squadsOf(round, name, entry).stream().filter(squad -> wipedOut(round, squad)).forEach(squad -> disband(round, squad));
        List<BotSquad> squads = squadsOf(round, name, entry);
        int raised = squads.size();
        while (raised < setting.count() && (setting.budget() == 0 || round.count(round.getSquadDeployments(), name, entry) < setting.budget()))
        {
            raise(round, team, force, entry, template, random);
            raised++;
        }
        for (int i = setting.count(); i < squads.size(); i++)
            disband(round, squads.get(i));
    }

    private static void raise(BotRound round, ITeam team, BotSettings.TeamForce force, int entry, SquadTemplate template, RandomGenerator random)
    {
        BotSquad squad = round.newSquad(team.getShortName(), BotRole.INFANTRY, BotSlot.Source.SQUAD, entry, template.id());
        Set<String> offered = new HashSet<>();
        team.getClasses().forEach(playerClass -> offered.add(playerClass.getShortName()));
        for (String playerClass : template.roster())
        {
            BotSlot slot = round.newSlot(team.getShortName(), BotSlot.Source.SQUAD, entry);
            slot.setRole(offered.contains(playerClass) ? playerClass : pickClass(team, force, random));
            slot.setSquad(squad.getId());
            squad.getSlots().add(slot.getId());
        }
        round.increment(round.getSquadDeployments(), team.getShortName(), entry);
    }

    private static void keepVehicles(BotRound round, ITeam team, BotSettings.TeamForce force)
    {
        String name = team.getShortName();
        for (BotSlot slot : round.getSlots().values())
        {
            boolean outdated = slot.getSource() == BotSlot.Source.VEHICLE && slot.getTeam().equals(name)
                && (slot.getEntry() >= force.getVehicles().size() || !force.getVehicles().get(slot.getEntry()).type().equals(slot.getRole()));
            if (outdated)
                slot.setRetired(true);
        }
        for (int entry = 0; entry < force.getVehicles().size(); entry++)
        {
            BotSettings.VehicleEntry setting = force.getVehicles().get(entry);
            Optional<BotRole> role = FlanModelNpcs.type(setting.type()).map(type -> BotRole.of(type.getKind(), type.getInfoType()));
            if (role.isEmpty())
                continue;
            List<BotSlot> active = active(round, name, BotSlot.Source.VEHICLE, entry);
            for (int i = active.size(); i < setting.count(); i++)
            {
                BotSlot slot = round.newSlot(name, BotSlot.Source.VEHICLE, entry);
                slot.setRole(setting.type());
                BotSquad squad = round.newSquad(name, role.get(), BotSlot.Source.VEHICLE, entry, "");
                squad.getSlots().add(slot.getId());
                slot.setSquad(squad.getId());
            }
            retireExtra(active, setting.count());
        }
    }

    private static List<BotSlot> active(BotRound round, String team, BotSlot.Source source, int entry)
    {
        return round.getSlots().values().stream().filter(slot -> !slot.isRetired() && slot.getTeam().equals(team) && slot.getSource() == source && slot.getEntry() == entry).toList();
    }

    private static List<BotSquad> squadsOf(BotRound round, String team, int entry)
    {
        return round.getSquads().values().stream().filter(squad -> !squad.isDisbanded() && squad.getTeam().equals(team) && squad.getSource() == BotSlot.Source.SQUAD && squad.getEntry() == entry)
            .toList();
    }

    /** Retires the slots beyond a target, the fallen ones first, then the newest. */
    private static void retireExtra(List<BotSlot> active, int target)
    {
        if (active.size() <= target)
            return;
        List<BotSlot> order = new ArrayList<>(active);
        order.sort(Comparator.comparing((BotSlot slot) -> slot.getEntity() != null).thenComparing(Comparator.comparingInt(BotSlot::getId).reversed()));
        for (int i = 0; i < active.size() - target; i++)
            order.get(i).setRetired(true);
    }

    private static void disband(BotRound round, BotSquad squad)
    {
        squad.setDisbanded(true);
        squad.getSlots().stream().map(round.getSlots()::get).filter(Objects::nonNull).forEach(slot -> slot.setRetired(true));
    }

    /** Whether every member of a squad has fallen and none is about to come back. */
    private static boolean wipedOut(BotRound round, BotSquad squad)
    {
        return squad.getSlots().stream().map(round.getSlots()::get).filter(Objects::nonNull).allMatch(slot -> slot.getSpawns() > 0 && slot.getEntity() == null && !slot.isQueued());
    }

    /** A class of the team by the settings' weights; every class weighs the same while none is listed. */
    static String pickClass(ITeam team, BotSettings.TeamForce force, RandomGenerator random)
    {
        List<? extends IPlayerClass> classes = team.getClasses();
        if (classes.isEmpty())
            return "";
        int[] weights = new int[classes.size()];
        int sum = 0;
        for (int i = 0; i < classes.size(); i++)
        {
            weights[i] = force.getClassWeights().isEmpty() ? 1 : force.getClassWeights().getOrDefault(classes.get(i).getShortName(), 0);
            sum += weights[i];
        }
        if (sum <= 0)
            return classes.get(random.nextInt(classes.size())).getShortName();
        int ticket = random.nextInt(sum);
        for (int i = 0; i < classes.size(); i++)
        {
            ticket -= weights[i];
            if (ticket < 0)
                return classes.get(i).getShortName();
        }
        return classes.get(classes.size() - 1).getShortName();
    }

    // --- Spending ---

    /** Whether the budgets and tickets let a slot spawn now. */
    static boolean canSpawn(BotRound round, BotSlot slot)
    {
        if (slot.isRetired())
            return false;
        BotSettings.TeamForce force = round.getSettings().team(slot.getTeam());
        switch (slot.getSource())
        {
            case INFANTRY -> {
                if (force.getBudget() > 0 && round.getInfantrySpawns().getOrDefault(slot.getTeam(), 0) >= force.getBudget())
                    return false;
            }
            case CARGO -> {
                // Transports bring cargo to the front; it never comes back on its own.
                return false;
            }
            case SQUAD -> {
                // A squad with a budget is lost as a whole: its members do not come back one by one.
                boolean budgeted = slot.getEntry() < force.getSquads().size() && force.getSquads().get(slot.getEntry()).budget() > 0;
                if (budgeted && slot.getSpawns() > 0)
                    return false;
            }
            case VEHICLE -> {
                if (slot.getEntry() >= force.getVehicles().size())
                    return false;
                int budget = force.getVehicles().get(slot.getEntry()).budget();
                if (budget > 0 && round.count(round.getVehicleSpawns(), slot.getTeam(), slot.getEntry()) >= budget)
                    return false;
            }
        }
        return canAfford(round, slot);
    }

    /** Whether the side has the tickets a spawn of the slot costs. */
    static boolean canAfford(BotRound round, BotSlot slot)
    {
        int cost = ticketCost(round, slot);
        return round.getSettings().getTickets() <= 0 || cost <= 0 || ticketsLeft(round, round.sideOf(slot.getTeam())) >= cost;
    }

    /** Gives the sides that trail on score extra infantry, as the round's rubber band says; never in Assault, scored by losses. */
    private static Map<String, Integer> rubberBand(BotRound round, Map<String, Integer> targets)
    {
        int percent = round.getSettings().getRubberBand();
        if (percent <= 0 || round.getLive().getAttackingSide().isPresent())
            return targets;
        Map<String, Integer> sides = new HashMap<>();
        targets.keySet().forEach(team -> sides.put(team, round.sideOf(team)));
        Map<Integer, Integer> scores = new HashMap<>();
        for (int side = 0; side < round.getLive().getRound().getSideCount(); side++)
            scores.put(side, round.getLive().getScore(side));
        return ForcePlanner.rubberBand(targets, sides, scores, round.getLive().getRound().getScoreLimit(), percent);
    }

    /** Records a spawn against the budgets and the side's tickets. */
    static void charge(BotRound round, BotSlot slot)
    {
        int cost = ticketCost(round, slot);
        int side = round.sideOf(slot.getTeam());
        if (round.getSettings().getTickets() > 0 && cost > 0)
            round.getTickets().put(side, ticketsLeft(round, side) - cost);
        if (slot.getSource() == BotSlot.Source.INFANTRY)
            round.getInfantrySpawns().merge(slot.getTeam(), 1, Integer::sum);
        else if (slot.getSource() == BotSlot.Source.VEHICLE)
            round.increment(round.getVehicleSpawns(), slot.getTeam(), slot.getEntry());
        round.getSidesThatFought().add(side);
    }

    /** Tickets a spawn costs: none for the first spawn of the force the round started with. */
    static int ticketCost(BotRound round, BotSlot slot)
    {
        if (slot.isInitial() && slot.getSpawns() == 0)
            return 0;
        if (slot.getSource() != BotSlot.Source.VEHICLE)
            return 1;
        List<BotSettings.VehicleEntry> vehicles = round.getSettings().team(slot.getTeam()).getVehicles();
        return slot.getEntry() < vehicles.size() ? vehicles.get(slot.getEntry()).ticketCost() : 1;
    }

    static int ticketsLeft(BotRound round, int side)
    {
        return round.getTickets().computeIfAbsent(side, ignored -> round.getSettings().getTickets());
    }

    /** Whether the side can still put a bot in the field, now or once a respawn timer runs out. */
    static boolean canStillField(BotRound round, int side)
    {
        boolean slotsLeft = round.getSlots().values().stream().anyMatch(slot -> round.sideOf(slot.getTeam()) == side && (slot.getEntity() != null || canSpawn(round, slot)));
        if (slotsLeft)
            return true;
        // A squad entry with budget left raises another squad as soon as the last one is lost.
        for (ITeam team : round.getLive().getRound().getTeamsOfSide(side))
        {
            List<BotSettings.SquadEntry> entries = round.getSettings().team(team.getShortName()).getSquads();
            for (int entry = 0; entry < entries.size(); entry++)
            {
                BotSettings.SquadEntry setting = entries.get(entry);
                if (setting.count() > 0 && (setting.budget() == 0 || round.count(round.getSquadDeployments(), team.getShortName(), entry) < setting.budget()))
                    return true;
            }
        }
        return false;
    }
}
