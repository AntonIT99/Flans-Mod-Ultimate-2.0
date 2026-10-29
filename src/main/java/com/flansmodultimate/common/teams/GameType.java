package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

@Getter
public abstract class GameType
{
    /**
     * How many teams a round of a game type takes and how many sides they may form. Teams on the
     * same side are allied; a game type that needs as many sides as teams has no alliances.
     */
    public record TeamLimits(int minTeams, int maxTeams, int minSides, int maxSides)
    {
        /** Exactly this many teams, each on its own side. */
        public static TeamLimits exactly(int teams)
        {
            return new TeamLimits(teams, teams, teams, teams);
        }

        /** Two to {@link TeamsRound#MAX_TEAMS} teams forming two to {@code maxSides} sides. */
        public static TeamLimits upToSides(int maxSides)
        {
            return new TeamLimits(2, TeamsRound.MAX_TEAMS, 2, maxSides);
        }

        /** Why these teams and sides cannot play, or null when they can. */
        @Nullable
        public String describeRefusal(int teams, int sides)
        {
            if (teams < minTeams || teams > maxTeams)
                return "requires " + describe(minTeams, maxTeams, "team");
            if (sides < minSides || sides > maxSides)
                return "requires " + describe(minSides, maxSides, "side");
            return null;
        }

        /** The limits in words, as the game type list shows them. */
        public String describe()
        {
            String teams = describe(minTeams, maxTeams, "team");
            return minSides == minTeams && maxSides == maxTeams ? teams : teams + " on " + describe(minSides, maxSides, "side");
        }

        private static String describe(int minimum, int maximum, String noun)
        {
            String plural = maximum == 1 ? noun : noun + "s";
            return minimum == maximum ? minimum + " " + plural : minimum + " to " + maximum + " " + plural;
        }
    }

    private static final Map<String, GameType> TYPES = new LinkedHashMap<>();

    private final String id;
    private final String name;
    private final TeamLimits teamLimits;
    /** Whether the rounds generator may pick this game type. */
    private final boolean allowedForRoundsGenerator;

    protected GameType(String id, String name, TeamLimits teamLimits, boolean allowedForRoundsGenerator)
    {
        this.id = id.toLowerCase(Locale.ROOT);
        this.name = name;
        this.teamLimits = teamLimits;
        this.allowedForRoundsGenerator = allowedForRoundsGenerator;
        if (TYPES.putIfAbsent(this.id, this) != null)
            throw new IllegalStateException("Duplicate game type: " + id);
    }

    public void roundStarted(TeamsManager manager)
    {}

    public void roundEnded(TeamsManager manager)
    {}

    public void tick(TeamsManager manager)
    {}

    public boolean canPlayerBeAttacked(ServerPlayer victim, ServerPlayer attacker)
    {
        TeamsManager manager = TeamsManager.getInstance();
        Team victimTeam = manager.getPlayerTeam(victim);
        Team attackerTeam = manager.getPlayerTeam(attacker);
        return victimTeam != Team.SPECTATORS && attackerTeam != null && victimTeam != null && (!manager.areAllied(victimTeam, attackerTeam) || isFriendlyFireEnabled());
    }

    /**
     * Whether one fighter may hurt another, either of them a player or a combatant. Two players are judged by
     * {@link #canPlayerBeAttacked}; otherwise by the same rule, with combatants on their team.
     */
    public boolean canBeAttacked(LivingEntity victim, LivingEntity attacker)
    {
        if (victim instanceof ServerPlayer victimPlayer && attacker instanceof ServerPlayer attackerPlayer)
            return canPlayerBeAttacked(victimPlayer, attackerPlayer);
        Team victimTeam = TeamsCombatants.getTeam(victim);
        Team attackerTeam = TeamsCombatants.getTeam(attacker);
        return victimTeam != Team.SPECTATORS && attackerTeam != null && victimTeam != null && (!TeamsManager.getInstance().areAllied(victimTeam, attackerTeam) || isFriendlyFireEnabled());
    }

    public boolean playerAttacked(ServerPlayer player, DamageSource source)
    {
        LivingEntity attacker = TeamsCombatants.getAttacker(source);
        return attacker == null ? getPlayerTeam(player) != Team.SPECTATORS : canBeAttacked(player, attacker);
    }

    /** Whether a combatant of the round takes this damage; harm from outside the round always lands. */
    public boolean combatantAttacked(LivingEntity combatant, DamageSource source)
    {
        LivingEntity attacker = TeamsCombatants.getAttacker(source);
        return attacker == null || canBeAttacked(combatant, attacker);
    }

    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        PlayerData victimData = PlayerData.getInstance(victim);
        victimData.setDeaths(victimData.getDeaths() + 1);
        PlayerStats victimStats = manager.getStats(victim);
        victimStats.recordDeath();
        manager.getCurrentLoadoutPool().ifPresent(pool -> manager.awardExperience(victim, pool.getExperienceForDeath()));

        LivingEntity attacker = TeamsCombatants.getAttacker(source);
        if (attacker == null || attacker == victim)
        {
            victimData.setScore(victimData.getScore() - 1);
            return;
        }
        if (attacker instanceof ServerPlayer player)
            creditPlayerKill(manager, player, victim);
    }

    /**
     * A combatant of the round died. Here the player who killed it is credited as for killing a player; game types
     * with team scores also score the kill and the loss for the teams.
     *
     * @param victimTeam
     *            the team the combatant fought for
     */
    // Base implementation of a hook; overriding subclasses use the parameter.
    @SuppressWarnings("java:S1172")
    public void combatantKilled(TeamsManager manager, LivingEntity victim, Team victimTeam, DamageSource source)
    {
        if (TeamsCombatants.getAttacker(source) instanceof ServerPlayer player)
            creditPlayerKill(manager, player, victim);
    }

    /** Gives a player the score, kill, statistics and experience of killing an enemy, player or combatant. */
    protected static void creditPlayerKill(TeamsManager manager, ServerPlayer attacker, LivingEntity victim)
    {
        PlayerData attackerData = PlayerData.getInstance(attacker);
        attackerData.setScore(attackerData.getScore() + 1);
        attackerData.setKills(attackerData.getKills() + 1);
        PlayerStats attackerStats = manager.getStats(attacker);
        attackerStats.recordKill(attacker.distanceTo(victim));
        // A combatant has no rank of its own, so the fallback reward goes by distance alone.
        int victimRank = victim instanceof ServerPlayer player ? manager.getStats(player).getRank() : 0;
        int xp = manager.getCurrentLoadoutPool().map(pool -> pool.getExperienceForKill() + Math.max(0, attackerStats.getKillstreak() - 1) * pool.getExperienceForKillstreakBonus())
            .orElse(victimRank * 2 + Math.max(1, (int) (attacker.distanceTo(victim) / 10D)));
        manager.awardExperience(attacker, xp);
    }

    /**
     * A player changed sides during a running round, having already played for {@code from}.
     *
     * <p>
     * Called before the defection is applied, so both teams are still the ones the round
     * has been scored against.
     * </p>
     */
    public void playerDefected(TeamsManager manager, ServerPlayer player, Team from, Team to)
    {}

    /** A player picked a class they will respawn with, without changing teams. */
    public void playerChoseNewClass(TeamsManager manager, ServerPlayer player, PlayerClass playerClass)
    {}

    public void flagClicked(TeamsManager manager, ServerPlayer player, Flag flag)
    {}

    public void baseClicked(TeamsManager manager, ServerPlayer player, Flagpole base)
    {}

    /** Called after {@link #roundEnded} once the round's bases have been reset, to clear per-round state. */
    public void roundCleanup(TeamsManager manager)
    {}

    /** The teams a player may join through the team menu. Forced assignments bypass this. */
    // Base implementation of a hook; overriding subclasses use the parameter.
    @SuppressWarnings("java:S1172")
    public List<Team> getTeamsCanSpawnAs(TeamsManager manager, TeamsRound round, ServerPlayer player)
    {
        return round.getTeamIds().stream().map(Team::getTeam).filter(Objects::nonNull).toList();
    }

    /** A player logged in while this game type's round was running. */
    public void playerJoined(TeamsManager manager, ServerPlayer player)
    {}

    /** A player is logging out while this game type's round is running. */
    public void playerQuit(TeamsManager manager, ServerPlayer player)
    {}

    /** A player was just placed at their spawn point with their kit. */
    public void playerRespawned(TeamsManager manager, ServerPlayer player)
    {}

    /** A player picked a team from the team menu, before the choice is stored. */
    public void playerChoseTeam(TeamsManager manager, ServerPlayer player, @Nullable Team oldTeam, Team newTeam)
    {}

    /** A player who was not yet in play took the field. */
    public void playerEnteredTheGame(TeamsManager manager, ServerPlayer player, Team team, @Nullable PlayerClass playerClass)
    {}

    /** Something tried to damage a base. Bases stay invulnerable; this only reports the attempt. */
    public void baseAttacked(TeamsManager manager, ITeamBase base, DamageSource source)
    {}

    /** Something tried to damage a team object such as a flag. It stays invulnerable. */
    public void objectAttacked(TeamsManager manager, ITeamObject object, DamageSource source)
    {}

    /** Any living entity other than a player died; players go through {@link #playerKilled}. */
    public void entityKilled(TeamsManager manager, Entity entity, DamageSource source)
    {}

    // Base implementation of a hook; overriding subclasses use the parameter.
    @SuppressWarnings("java:S1172")
    public boolean canPlayerPickup(TeamsManager manager, ServerPlayer player, ItemStack stack)
    {
        return true;
    }

    @Nullable
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        return manager.findSpawnPoint(player, false).orElse(null);
    }

    public boolean hasWinner(TeamsManager manager, Team team)
    {
        return manager.getTeamScore(team) >= manager.getCurrentRound().map(TeamsRound::getScoreLimit).orElse(Integer.MAX_VALUE);
    }

    /**
     * The team that won the round that just ended, or null for a draw. Its allies won with it, so
     * the winner of an alliance is reported as the first team of that side.
     *
     * <p>
     * A side that met the winning condition takes it; when the clock ran out instead, the side
     * ahead on score does, and level scores are a draw.
     * </p>
     */
    @Nullable
    public Team getWinner(TeamsManager manager)
    {
        // Allies share a score, so comparing every team would call any alliance a tie with itself.
        List<Team> teams = manager.getCurrentRound().stream().flatMap(round -> round.getSideIds().stream().map(round::getSideLeader)).filter(Objects::nonNull).toList();
        for (Team team : teams)
            if (hasWinner(manager, team))
                return team;
        Team best = null;
        boolean tied = false;
        for (Team team : teams)
        {
            if (best == null || manager.getTeamScore(team) > manager.getTeamScore(best))
            {
                best = team;
                tied = false;
            }
            else if (manager.getTeamScore(team) == manager.getTeamScore(best))
                tied = true;
        }
        return tied ? null : best;
    }

    /** How this game type uses capture points; {@link ObjectiveMode#NONE} for the ones that do not. */
    public ObjectiveMode getObjectiveMode()
    {
        return ObjectiveMode.NONE;
    }

    /** The capture points of the running round as clients see them, in the order they are fought over. */
    // Base implementation of a hook; overriding subclasses use the parameter.
    @SuppressWarnings("java:S1172")
    public List<ObjectiveSnapshot> createObjectives(TeamsManager manager)
    {
        return List.of();
    }

    /** Side id shown on the left of the objective HUD: the attackers where a mode has them. */
    public int getAttackingTeamId(TeamsRound round)
    {
        return sideId(round, 0);
    }

    /** Side id shown on the right of the objective HUD: the defenders where a mode has them. */
    public int getDefendingTeamId(TeamsRound round)
    {
        return sideId(round, 1);
    }

    /** The side id of the side at {@code index} in round order, or 0 when the round has fewer sides. */
    protected static int sideId(TeamsRound round, int index)
    {
        List<Integer> sideIds = round.getSideIds();
        return index < sideIds.size() ? sideIds.get(index) : 0;
    }

    /** Seconds a dead player of the given round team waits on the deployment map before choosing a spawn. */
    // Base implementation of a hook; overriding subclasses use the parameters.
    @SuppressWarnings("java:S1172")
    public int getDeployDelaySeconds(TeamsManager manager, int teamId)
    {
        return 0;
    }

    /**
     * Records where a player asked to come back: at a capture point, or at headquarters for null.
     *
     * @return whether this game type lets players choose
     */
    // Base implementation of a hook; overriding subclasses use the parameters.
    @SuppressWarnings("java:S1172")
    public boolean selectSpawn(TeamsManager manager, ServerPlayer player, @Nullable UUID capturePointId)
    {
        return false;
    }

    /** Whether a round of this game type makes sense on the map, which the rounds generator respects. */
    // Base implementation of a hook; overriding subclasses use the parameter.
    @SuppressWarnings("java:S1172")
    public boolean isPlayableOn(TeamsMap map)
    {
        return true;
    }

    public boolean isFriendlyFireEnabled()
    {
        return false;
    }

    public boolean isAutoBalanceEnabled()
    {
        return true;
    }

    public boolean isScoreboardSortedByTeam()
    {
        return true;
    }

    /** Whether the end of a round names the best player of every team, rather than one across the whole field. */
    public boolean hasMvpPerTeam()
    {
        return true;
    }

    public boolean showZombieScore()
    {
        return false;
    }

    // Base implementation of a hook; overriding subclasses use the parameter.
    @SuppressWarnings("java:S1172")
    public boolean setVariable(String variable, String value)
    {
        return false;
    }

    /**
     * The variables {@link #setVariable} takes for this game type, by the name it takes each under, with its current
     * value written as it would be given. Overrides add theirs to what the game type they extend returns.
     */
    public Map<String, String> getVariables()
    {
        return new LinkedHashMap<>();
    }

    public void loadSettings(CompoundTag tag)
    {}

    public void saveSettings(CompoundTag tag)
    {}

    @Nullable
    protected Team getPlayerTeam(ServerPlayer player)
    {
        return TeamsManager.getInstance().getPlayerTeam(player);
    }

    /** Adds {@code points} to a player's score and to their team's score. */
    public static void givePoints(ServerPlayer player, int points)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.setScore(data.getScore() + points);
        Team team = data.getTeam();
        if (team != null && team != Team.SPECTATORS)
            TeamsManager.getInstance().addTeamScore(team, points);
    }

    public static void bootstrap()
    {
        GameTypes.bootstrap();
    }

    @Nullable
    public static GameType get(@Nullable String id)
    {
        bootstrap();
        return id == null ? null : TYPES.get(id.toLowerCase(Locale.ROOT));
    }

    public static Collection<GameType> values()
    {
        bootstrap();
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
