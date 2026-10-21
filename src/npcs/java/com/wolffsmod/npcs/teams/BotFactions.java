package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsRound;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;

import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Custom NPCs factions bots fight in: one hidden faction per Flan team, hostile to the factions of the teams on the
 * other sides of the round being played. While a round runs, the team a player fights for decides how these factions
 * treat the player, instead of the player's faction points. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotFactions
{
    private static final String NAME_PREFIX = "Flan Teams ";

    /** How a bot faction treats a player. */
    public enum Stance
    {
        HOSTILE, FRIENDLY, NEUTRAL
    }

    /** The team of each bot faction in the round being played, by faction id. */
    private static final Map<Integer, String> TEAM_OF_FACTION = new ConcurrentHashMap<>();

    /**
     * Makes or updates the faction of every team of the round and sets which ones are hostile to each other.
     *
     * @return the faction id of each team, by short name
     */
    public static Map<String, Integer> setUp(ITeamsRound round)
    {
        FactionController controller = FactionController.instance;
        TEAM_OF_FACTION.clear();
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (ITeam team : round.getTeams())
        {
            Faction faction = controller.getFactionFromName(NAME_PREFIX + team.getShortName());
            if (faction == null)
                faction = (Faction) controller.create(NAME_PREFIX + team.getShortName(), team.getTeamColour());
            faction.color = team.getTeamColour();
            faction.hideFaction = true;
            faction.getsAttacked = false;
            ids.put(team.getShortName(), faction.id);
            TEAM_OF_FACTION.put(faction.id, team.getShortName());
        }
        for (ITeam team : round.getTeams())
        {
            Faction faction = controller.getFaction(ids.get(team.getShortName()));
            faction.attackFactions.clear();
            for (ITeam other : round.getTeams())
                if (!round.areTeamsAllied(team, other))
                    faction.attackFactions.add(ids.get(other.getShortName()));
            controller.saveFaction(faction);
        }
        return ids;
    }

    /** Forgets the factions of a round that ended; they keep their saved names for the next one. */
    public static void tearDown()
    {
        TEAM_OF_FACTION.clear();
    }

    /**
     * How a bot faction treats a player in the round being played: hostile to the other sides, friendly to its own, and
     * neutral to anyone not fighting. Empty for factions that are not a bot faction, and on clients, where Custom NPCs'
     * own rules apply.
     */
    public static Optional<Stance> stance(int factionId, Player player)
    {
        if (player.level().isClientSide)
            return Optional.empty();
        String team = TEAM_OF_FACTION.get(factionId);
        if (team == null)
            return Optional.empty();
        Optional<ITeam> playerTeam = FlansTeams.getTeam(player);
        Optional<ITeam> factionTeam = FlansModApi.getTeam(team);
        if (playerTeam.isEmpty() || factionTeam.isEmpty())
            return Optional.of(Stance.NEUTRAL);
        boolean allied = FlansTeams.getRunningRound().map(round -> round.getRound().areTeamsAllied(playerTeam.get(), factionTeam.get())).orElse(false);
        return Optional.of(allied ? Stance.FRIENDLY : Stance.HOSTILE);
    }
}
