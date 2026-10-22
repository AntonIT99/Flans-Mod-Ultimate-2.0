package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * The bots of one battle, such as a playing of a Teams round: its slots, squads, spending and spawn queue. Its bots are
 * tagged with its instance. Server thread.
 */
@Getter
final class BotBattle
{
    private final UUID instance;
    private final Battleground ground;
    @Setter
    private BotSettings settings;
    /** Custom NPCs faction id of each team, by short name. */
    private final Map<String, Integer> factions;
    private final Map<Integer, BotSlot> slots = new LinkedHashMap<>();
    private final Map<Integer, BotSquad> squads = new LinkedHashMap<>();
    private final Map<UUID, Integer> slotOfEntity = new HashMap<>();
    private final Deque<Integer> queue = new ArrayDeque<>();
    /** Tickets each side has left, by side; absent while no ticket limit is set. */
    private final Map<Integer, Integer> tickets = new HashMap<>();
    /** Loose infantry spawns made by each team. */
    private final Map<String, Integer> infantrySpawns = new HashMap<>();
    /** Spawns made of each vehicle entry, by team and entry index. */
    private final Map<String, Map<Integer, Integer>> vehicleSpawns = new HashMap<>();
    /** Squads raised for each squad entry, by team and entry index. */
    private final Map<String, Map<Integer, Integer>> squadDeployments = new HashMap<>();
    /** The player commanding each side, by side; a side without one is commanded by its AI alone. */
    private final Map<Integer, UUID> commanders = new HashMap<>();
    /** Sides that have fielded anyone, the only ones that can be worn out. */
    private final Set<Integer> sidesThatFought = new HashSet<>();
    /** Players who took the field in this playing; joining again later costs them a ticket. */
    private final Set<UUID> fieldedPlayers = new HashSet<>();
    /** Where each player leading a squad chose to come back after dying. */
    private final Map<UUID, BotLeaders.SpawnChoice> spawnChoices = new HashMap<>();
    @Setter
    private int nextSlot = 1;
    @Setter
    private int nextSquad = 1;
    /** Whether slots made now belong to the force the round started with, whose first spawn is free. */
    @Setter
    private boolean opening = true;

    BotBattle(UUID instance, Battleground ground, BotSettings settings, Map<String, Integer> factions)
    {
        this.instance = instance;
        this.ground = ground;
        this.settings = settings;
        this.factions = new HashMap<>(factions);
    }

    BotSlot newSlot(String team, BotSlot.Source source, int entry)
    {
        BotSlot slot = new BotSlot(nextSlot++, team, source, entry, opening);
        slots.put(slot.getId(), slot);
        return slot;
    }

    BotSquad newSquad(String team, BotRole role, BotSlot.Source source, int entry, String template)
    {
        BotSquad squad = new BotSquad(nextSquad++, team, role, source, entry, template);
        squads.put(squad.getId(), squad);
        return squad;
    }

    Optional<ITeam> team(String shortName)
    {
        return ground.team(shortName);
    }

    int sideOf(String team)
    {
        return team(team).map(ground::sideOf).orElse(-1);
    }

    /**
     * @return the side a player fights on: in a round, the side of the team they play, as the round sees it; in any other
     *         battle, the side of the faction they belong to. Empty for a player who does not fight.
     */
    OptionalInt sideOfPlayer(Player player)
    {
        if (ground.round().isPresent())
            return FlansTeams.getTeam(player).map(team -> OptionalInt.of(ground.sideOf(team))).orElse(OptionalInt.empty());
        int side = ground.sideOf(player);
        return side < 0 ? OptionalInt.empty() : OptionalInt.of(side);
    }

    int count(Map<String, Map<Integer, Integer>> counts, String team, int entry)
    {
        return counts.getOrDefault(team, Map.of()).getOrDefault(entry, 0);
    }

    void increment(Map<String, Map<Integer, Integer>> counts, String team, int entry)
    {
        counts.computeIfAbsent(team, ignored -> new HashMap<>()).merge(entry, 1, Integer::sum);
    }
}
