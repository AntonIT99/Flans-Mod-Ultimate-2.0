package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.common.types.LoadoutPool;
import com.flansmodultimate.common.types.Team;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * The command centre's section for the Teams game modes: the round being played, the rotation, the maps, the rules and
 * the server settings, with the teams, game types and loadout pools to choose from. The keys are shared with the
 * screen, which reads the section. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsSection
{
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "teams");

    public static final String ENABLED = "enabled";
    public static final String PLAYERS = "players";
    public static final String ROUND = "round";
    public static final String CURRENT = "current";
    public static final String NEXT = "next";
    public static final String QUEUED = "queued";
    public static final String ROUNDS = "rounds";
    public static final String MAPS = "maps";
    public static final String GAME_TYPES = "game_types";
    public static final String TEAMS = "team_list";
    public static final String POOLS = "loadout_pools";
    public static final String RULES = "rules";
    public static final String SETTINGS = "settings";
    public static final String VARIABLES = "variables";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String COLOUR = "colour";
    public static final String MAP = "map";
    public static final String GAME_TYPE = "game_type";
    public static final String SIDES = "sides";
    public static final String SIDE = "side";
    public static final String SCORE = "score";
    public static final String MINUTES = "minutes";
    public static final String SCORE_LIMIT = "score_limit";
    public static final String ATTACKERS = "attackers";
    public static final String CAN_ATTACK = "can_attack";
    public static final String TIME_LEFT = "time_left";
    public static final String PHASE = "phase";
    public static final String INTERMISSION_LEFT = "intermission_left";
    public static final String DIMENSION = "dimension";
    public static final String BOUNDS = "bounds";
    public static final String BASES = "bases";
    public static final String CAPTURE_POINTS = "capture_points";
    public static final String IMAGE = "image";
    public static final String PLAYABLE = "playable";
    public static final String MIN_TEAMS = "min_teams";
    public static final String MAX_TEAMS = "max_teams";
    public static final String MIN_SIDES = "min_sides";
    public static final String MAX_SIDES = "max_sides";
    public static final String DEFAULT_SCORE = "default_score";
    public static final String WEAPON_DROPS = "weaponDrops";
    public static final String SEA_LEVEL = "seaLevel";

    /**
     * The boolean rules, by the {@code /teams} command that sets each; {@code noHunger} in the screen is the legacy
     * {@code overrideHunger}.
     */
    public static final List<String> SWITCHES = List.of("explosions", "canBreakGlass", "canBreakGuns", "vehiclesBreakBlocks", "survivalCanBreakVehicles", "survivalCanPlaceVehicles", "forceAdventure",
        "pacifism", "bombs", "shells", "bullets", "armourDrops", "overrideHunger", "alliedSpawns", "fuelNeeded", "vehiclesCanZoom", "movieProp");
    /** How long abandoned driveables last, by the {@code /teams} command that sets each, in seconds. */
    public static final List<String> LIFETIMES = List.of("mgLife", "planeLife", "vehicleLife", "mechaLife", "aaLife");

    public static final String VOTING = "voting";
    public static final String GENERATOR = "roundsGenerator";
    public static final String SCORE_DISPLAY = "scoreDisplayTime";
    public static final String RANK_UPDATE = "rankUpdateTime";
    public static final String VOTING_TIME = "votingTime";
    public static final String AUTOBALANCE = "autobalancetime";
    public static final String MOTD = "motd";
    public static final String XP = "xpmultiplier";
    public static final String POOL = "loadoutpool";
    public static final String SNAPSHOT_MIN = "bltss_min";
    public static final String SNAPSHOT_DIVISOR = "bltss_divisor";

    static CompoundTag build(ServerPlayer player)
    {
        TeamsManager manager = TeamsManager.getInstance();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(ENABLED, manager.isEnabled());
        tag.putInt(PLAYERS, player.server.getPlayerCount());
        List<TeamsRound> rounds = manager.getRounds();
        tag.putInt(CURRENT, manager.getCurrentRound().map(rounds::indexOf).orElse(-1));
        tag.putInt(NEXT, manager.nextRoundIndex());
        tag.putInt(QUEUED, manager.queuedRoundIndex());
        manager.getCurrentRound().ifPresent(round -> tag.put(ROUND, liveRound(manager, round)));
        ListTag roundList = new ListTag();
        rounds.forEach(round -> roundList.add(round(round)));
        tag.put(ROUNDS, roundList);
        ListTag maps = new ListTag();
        manager.getMaps().forEach(map -> maps.add(map(map)));
        tag.put(MAPS, maps);
        ListTag types = new ListTag();
        GameType.values().forEach(type -> types.add(gameType(type)));
        tag.put(GAME_TYPES, types);
        ListTag teams = new ListTag();
        Team.values().stream().filter(team -> team != Team.SPECTATORS).forEach(team -> teams.add(team(team)));
        tag.put(TEAMS, teams);
        ListTag pools = new ListTag();
        LoadoutPool.values().forEach(pool ->
        {
            CompoundTag entry = new CompoundTag();
            entry.putString(ID, pool.getShortName());
            entry.putString(NAME, pool.getName());
            pools.add(entry);
        });
        tag.put(POOLS, pools);
        tag.put(RULES, rules(manager));
        tag.put(SETTINGS, settings(manager));
        CompoundTag variables = new CompoundTag();
        manager.getCurrentGameType().ifPresent(type -> type.getVariables().forEach(variables::putString));
        tag.put(VARIABLES, variables);
        return tag;
    }

    private static CompoundTag liveRound(TeamsManager manager, TeamsRound round)
    {
        CompoundTag tag = round(round);
        tag.putInt(TIME_LEFT, manager.getRoundTimeLeftTicks());
        tag.putString(PHASE, manager.getIntermissionPhase().name());
        tag.putInt(INTERMISSION_LEFT, manager.getIntermissionTicks());
        ListTag teams = new ListTag();
        for (int i = 0; i < round.getTeamIds().size(); i++)
        {
            Team team = round.getTeam(i);
            if (team == null)
                continue;
            CompoundTag entry = team(team);
            entry.putInt(SIDE, round.getSides().get(i));
            entry.putInt(SCORE, manager.getTeamScore(team));
            entry.putInt(PLAYERS, manager.countPlayersOnRoundTeam(i));
            teams.add(entry);
        }
        tag.put(TEAMS, teams);
        return tag;
    }

    private static CompoundTag round(TeamsRound round)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID, round.getId().toString());
        tag.putString(MAP, round.getMapId());
        tag.putString(GAME_TYPE, round.getGameTypeId());
        ListTag teams = new ListTag();
        round.getTeamIds().forEach(team -> teams.add(StringTag.valueOf(team)));
        tag.put(TEAMS, teams);
        tag.putIntArray(SIDES, round.getSides());
        tag.putInt(MINUTES, round.getTimeLimitMinutes());
        tag.putInt(SCORE_LIMIT, round.getScoreLimit());
        tag.putInt(ATTACKERS, round.getAttackingSide().orElse(-1));
        tag.putBoolean(CAN_ATTACK, round.canBeAttack());
        return tag;
    }

    private static CompoundTag map(TeamsMap map)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID, map.getShortName());
        tag.putString(NAME, map.getName());
        tag.putString(DIMENSION, map.getDimension().location().toString());
        TeamsMap.Bounds bounds = map.getBounds();
        if (bounds != null)
            tag.putIntArray(BOUNDS, new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        tag.putInt(BASES, map.getBases().size());
        tag.putBoolean(CAPTURE_POINTS, map.hasCapturePoints());
        tag.putString(IMAGE, map.getDeployImage());
        ListTag playable = new ListTag();
        GameType.values().stream().filter(type -> type.isPlayableOn(map)).forEach(type -> playable.add(StringTag.valueOf(type.getId())));
        tag.put(PLAYABLE, playable);
        return tag;
    }

    private static CompoundTag gameType(GameType type)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID, type.getId());
        tag.putString(NAME, type.getName());
        GameType.TeamLimits limits = type.getTeamLimits();
        tag.putInt(MIN_TEAMS, limits.minTeams());
        tag.putInt(MAX_TEAMS, limits.maxTeams());
        tag.putInt(MIN_SIDES, limits.minSides());
        tag.putInt(MAX_SIDES, limits.maxSides());
        tag.putInt(DEFAULT_SCORE, TeamsManager.defaultScoreLimit(type));
        tag.putBoolean(CAN_ATTACK, type.getObjectiveMode() == ObjectiveMode.DOMINATION || type.getObjectiveMode() == ObjectiveMode.CONQUEST);
        return tag;
    }

    private static CompoundTag team(ITeam team)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(ID, team.getShortName());
        tag.putString(NAME, team.getName());
        tag.putInt(COLOUR, team.getTeamColour());
        return tag;
    }

    private static CompoundTag rules(TeamsManager manager)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("explosions", manager.isExplosionsBreakBlocks());
        tag.putBoolean("canBreakGlass", manager.isCanBreakGlass());
        tag.putBoolean("canBreakGuns", manager.isCanBreakGuns());
        tag.putBoolean("vehiclesBreakBlocks", manager.isDriveablesBreakBlocks());
        tag.putBoolean("survivalCanBreakVehicles", manager.isSurvivalCanBreakVehicles());
        tag.putBoolean("survivalCanPlaceVehicles", manager.isSurvivalCanPlaceVehicles());
        tag.putBoolean("forceAdventure", manager.isForceAdventureMode());
        tag.putBoolean("pacifism", manager.isPacifism());
        tag.putBoolean("bombs", manager.isBombsEnabled());
        tag.putBoolean("shells", manager.isShellsEnabled());
        tag.putBoolean("bullets", manager.isBulletsEnabled());
        tag.putBoolean("armourDrops", manager.isArmourDrops());
        tag.putBoolean("overrideHunger", manager.isOverrideHunger());
        tag.putBoolean("alliedSpawns", manager.isAlliedSpawnSharing());
        tag.putBoolean("fuelNeeded", manager.isVehiclesNeedFuelSetting());
        tag.putBoolean("vehiclesCanZoom", manager.isVehiclesCanZoom());
        tag.putBoolean("movieProp", manager.isMovieProp());
        tag.putString(WEAPON_DROPS, switch (manager.getWeaponDrops())
        {
            case NONE -> "off";
            case DROPS -> "on";
            case SMART_DROPS -> "smart";
        });
        tag.putInt("mgLife", manager.getMgLife());
        tag.putInt("planeLife", manager.getPlaneLife());
        tag.putInt("vehicleLife", manager.getVehicleLife());
        tag.putInt("mechaLife", manager.getMechaLife());
        tag.putInt("aaLife", manager.getAaLife());
        manager.getSeaLevelOverride().ifPresent(level -> tag.putInt(SEA_LEVEL, level));
        return tag;
    }

    private static CompoundTag settings(TeamsManager manager)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(VOTING, manager.isVoting());
        tag.putBoolean(GENERATOR, manager.isRoundsGenerator());
        tag.putInt(SCORE_DISPLAY, manager.getScoreDisplayTimeTicks() / 20);
        tag.putInt(RANK_UPDATE, manager.getRankUpdateTimeTicks() / 20);
        tag.putInt(VOTING_TIME, manager.getVotingTimeTicks() / 20);
        tag.putInt(AUTOBALANCE, manager.getAutoBalanceIntervalTicks() / 20);
        tag.putString(MOTD, manager.getMotd());
        tag.putFloat(XP, manager.getExperienceMultiplier());
        tag.putString(POOL, manager.getCurrentLoadoutPoolId());
        tag.putInt(SNAPSHOT_MIN, manager.getBulletSnapshotMin());
        tag.putInt(SNAPSHOT_DIVISOR, manager.getBulletSnapshotDivisor());
        return tag;
    }
}
