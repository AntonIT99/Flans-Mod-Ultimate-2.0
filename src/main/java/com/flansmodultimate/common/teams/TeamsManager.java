package com.flansmodultimate.common.teams;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.ITeamsRespawnRule;
import com.flansmodultimate.api.ITeamsRound;
import com.flansmodultimate.api.TeamsScoreEntry;
import com.flansmodultimate.api.TeamsSpawner;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.network.client.teams.*;
import com.flansmodultimate.platform.world.ChunkTicketPlatform;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

/**
 * Authoritative Teams runtime. All mutation occurs on the logical server thread;
 * persisted data uses UUIDs and compact world SavedData rather than global files.
 */
public final class TeamsManager
{
    private static final String NBT_ENABLED = "enabled";
    private static final String NBT_ROUND_RUNNING = "round_running";
    private static final String NBT_CURRENT_ROUND = "current_round";
    private static final String NBT_ROTATION_INDEX = "rotation_index";
    private static final String NBT_TIME_LEFT = "time_left";
    private static final String NBT_ELAPSED = "elapsed";
    private static final String NBT_INTERMISSION = "intermission";
    private static final String NBT_INTERMISSION_VOTING_PHASE = "intermission_voting_phase";
    private static final String NBT_INTERMISSION_PHASE = "intermission_phase";
    private static final String NBT_SCORE_DISPLAY_TIME = "score_display_time";
    private static final String NBT_RANK_UPDATE_TIME = "rank_update_time";
    private static final String NBT_VOTING_TIME = "voting_time";
    private static final String NBT_MOTD = "motd";
    private static final String NBT_AUTO_BALANCE_INTERVAL = "auto_balance_interval";
    private static final String NBT_VOTE_OPTIONS = "vote_options";
    private static final String NBT_ID = "id";
    private static final String NBT_EXPLOSIONS = "explosions";
    private static final String NBT_BREAK_GLASS = "break_glass";
    private static final String NBT_BREAK_GUNS = "break_guns";
    private static final String NBT_DRIVEABLES_BREAK_BLOCKS = "driveables_break_blocks";
    private static final String NBT_BOMBS = "bombs";
    private static final String NBT_SHELLS = "shells";
    private static final String NBT_BULLETS = "bullets";
    private static final String NBT_ADVENTURE = "adventure";
    private static final String NBT_ARMOUR_DROPS = "armour_drops";
    private static final String NBT_FUEL = "fuel";
    private static final String NBT_VEHICLES_CAN_ZOOM = "vehicles_can_zoom";
    private static final String NBT_OVERRIDE_HUNGER = "override_hunger";
    private static final String NBT_BREAK_VEHICLES = "break_vehicles";
    private static final String NBT_PLACE_VEHICLES = "place_vehicles";
    private static final String NBT_WEAPON_DROPS = "weapon_drops";
    private static final String NBT_MG_LIFE = "mg_life";
    private static final String NBT_PLANE_LIFE = "plane_life";
    private static final String NBT_VEHICLE_LIFE = "vehicle_life";
    private static final String NBT_MECHA_LIFE = "mecha_life";
    private static final String NBT_AA_LIFE = "aa_life";
    private static final String NBT_VOTING = "voting";
    private static final String NBT_ROUNDS_GENERATOR = "rounds_generator";
    private static final String NBT_LOADOUT_POOL = "loadout_pool";
    private static final String NBT_EXPERIENCE_MULTIPLIER = "experience_multiplier";
    private static final String NBT_SCORES = "scores";
    private static final String NBT_QUEUED_ROUND = "queued_round";
    private static final String NBT_ROUND_INSTANCE = "round_instance";
    private static final String NBT_PACIFISM = "pacifism";
    private static final String NBT_MOVIE_PROP = "movie_prop";
    private static final String NBT_SEA_LEVEL = "sea_level";
    private static final String NBT_WINNER = "winner";
    private static final String NBT_ROUND_DECIDED = "round_decided";
    private static final String NBT_ALLIED_SPAWN_SHARING = "allied_spawn_sharing";
    /** Seconds a player may stay outside the bounds of the map before being killed, as in the Labjac fork. */
    private static final int OUT_OF_BOUNDS_GRACE_SECONDS = 20;
    /** Marks {@link #seaLevelOverride} as unset, leaving each dimension its own sea level. */
    private static final int NO_SEA_LEVEL_OVERRIDE = Integer.MIN_VALUE;
    private static final int DEFAULT_INTERMISSION_PHASE_TICKS = 200;
    private static final int DEFAULT_AUTO_BALANCE_INTERVAL_TICKS = 400;
    private static final int AUTO_BALANCE_WARNING_TICKS = 200;
    /** Delay between a Teams respawn and the automatic reload of the gun in the first hotbar slot. */
    private static final int RESPAWN_RELOAD_DELAY_TICKS = 5;
    public static final String DEFAULT_MOTD = "Welcome to the Teams server";

    public enum EnumWeaponDrop
    {
        NONE, DROPS, SMART_DROPS
    }

    /**
     * The stage the break between rounds is in.
     *
     * <p>
     * They run in order: the scoreboard, then the rank and reward screen where a loadout
     * pool is in play, then voting where it is enabled. Each stage has its own duration and
     * any of them can be turned off by setting that duration to zero.
     * </p>
     */
    public enum IntermissionPhase
    {
        NONE, SCORES, RANK_UPDATE, VOTING
    }

    private static TeamsManager instance;

    @Getter
    private boolean explosionsBreakBlocks = true;
    @Getter @Setter
    private boolean canBreakGlass = true;
    @Getter @Setter
    private boolean canBreakGuns = true;
    @Getter @Setter
    private boolean driveablesBreakBlocks = true;
    @Getter @Setter
    private boolean bombsEnabled = true;
    @Getter @Setter
    private boolean shellsEnabled = true;
    @Getter @Setter
    private boolean bulletsEnabled = true;
    @Getter
    private boolean forceAdventureMode = true;
    @Getter @Setter
    private boolean armourDrops = true;
    private boolean vehiclesNeedFuel = true;
    @Getter
    private boolean vehiclesCanZoom;
    @Getter @Setter
    private boolean overrideHunger = true;
    @Getter @Setter
    private boolean survivalCanBreakVehicles = true;
    @Getter @Setter
    private boolean survivalCanPlaceVehicles = true;
    @Getter @Setter
    private EnumWeaponDrop weaponDrops = EnumWeaponDrop.DROPS;
    @Getter @Setter
    private int mgLife;
    @Getter @Setter
    private int planeLife;
    @Getter @Setter
    private int vehicleLife;
    @Getter @Setter
    private int mechaLife;
    @Getter @Setter
    private int aaLife;
    @Getter
    private boolean voting;
    @Getter @Setter
    private boolean roundsGenerator;
    @Getter
    private String currentLoadoutPoolId = "";
    @Getter
    private float experienceMultiplier = 1F;
    /** Labjac "pacifism" rule: while on, nobody can fire a weapon or throw a grenade and driveables stand still. */
    @Getter
    private boolean pacifism;
    /** Labjac "movie prop" rule: driveables run at full throttle on their own and need no fuel, for filming. */
    @Getter
    private boolean movieProp;
    /**
     * Whether allied teams come back at each other's bases. A team without a base of its own on the
     * map does so whatever this says.
     */
    @Getter @Setter
    private boolean alliedSpawnSharing;
    private int seaLevelOverride = NO_SEA_LEVEL_OVERRIDE;

    @Nullable
    private MinecraftServer server;
    @Nullable
    private TeamsSavedData savedData;
    private final Map<UUID, ITeamBase> liveBases = new HashMap<>();
    private final Map<UUID, Integer> outOfBoundsSeconds = new HashMap<>();
    private Map<UUID, PlayerClassSkin> lastSyncedPlayerClassSkins = Map.of();
    private final Map<UUID, ITeamObject> liveObjects = new HashMap<>();
    private final Map<String, Integer> teamScores = new LinkedHashMap<>();
    @Getter
    private final RandomSource random = RandomSource.create();
    @Getter
    private boolean enabled = true;
    @Getter
    private boolean roundRunning;
    @Nullable
    private UUID currentRoundId;
    /** Identifies this playing of the round; a round of the rotation gets a new one every time it starts. */
    @Getter @Nullable
    private UUID roundInstanceId;
    /** Short name of the team that won the round that last ended, or blank for a draw. */
    @Getter
    private String lastWinnerId = "";
    /** Whether a round has ended and not been replaced yet, so that a blank winner means a draw. */
    @Getter
    private boolean lastRoundDecided;
    private int rotationIndex = -1;
    /** Round an operator picked to come next with {@code /teams admin setnext}; played once, then forgotten. */
    @Nullable
    private UUID queuedRoundId;
    @Getter
    private int roundTimeLeftTicks;
    @Getter
    private int roundElapsedTicks;
    @Getter
    private int intermissionTicks;
    @Getter
    private int scoreDisplayTimeTicks = DEFAULT_INTERMISSION_PHASE_TICKS;
    @Getter
    private int rankUpdateTimeTicks = DEFAULT_INTERMISSION_PHASE_TICKS;
    @Getter
    private int votingTimeTicks = DEFAULT_INTERMISSION_PHASE_TICKS;
    @Getter
    private int autoBalanceIntervalTicks = DEFAULT_AUTO_BALANCE_INTERVAL_TICKS;
    @Getter
    private IntermissionPhase intermissionPhase = IntermissionPhase.NONE;
    /** Greeting shown to players on the loadout landing page. */
    @Getter
    private String motd = DEFAULT_MOTD;
    private final List<UUID> voteOptionIds = new ArrayList<>();
    /** The API view of the playing in progress, made when first asked for. */
    @Nullable
    private TeamsLiveRound liveRound;
    /** Scoreboard rows addons show beside each team's players in the running round, by team short name. */
    private final Map<String, List<TeamsScoreEntry>> addonScoreEntries = new HashMap<>();

    // The manager registers itself as the server-wide instance when it is created.
    @SuppressWarnings("java:S3010")
    public TeamsManager()
    {
        instance = this;
        com.flansmodultimate.common.teams.GameType.bootstrap();
    }

    public static TeamsManager getInstance()
    {
        if (instance == null)
            instance = new TeamsManager();
        return instance;
    }

    public void attachServer(MinecraftServer server)
    {
        if (this.server == server && savedData != null)
            return;

        this.server = server;
        savedData = SavedDataPlatform.computeIfAbsent(server.overworld(), TeamsSavedData.ID, TeamsSavedData::new, TeamsSavedData::load);
        loadRuntime(savedData.runtime);
        if (roundRunning)
            updateActiveChunkTickets(true);
    }

    public void detachServer()
    {
        saveRuntime();
        liveBases.clear();
        liveObjects.clear();
        server = null;
        savedData = null;
    }

    public MinecraftServer getServer()
    {
        if (server == null)
            throw new IllegalStateException("TeamsManager is not attached to a server");
        return server;
    }

    /** Whether a server is attached and this is its thread, the only place the API may use the manager. */
    public boolean isServerThread()
    {
        return server != null && savedData != null && server.isSameThread();
    }

    /** The API view of the playing in progress, or empty when no round runs. */
    public Optional<TeamsLiveRound> getLiveRound()
    {
        TeamsRound round = getCurrentRound().orElse(null);
        TeamsMap map = round == null ? null : getMap(round.getMapId()).orElse(null);
        if (!roundRunning || round == null || map == null || roundInstanceId == null)
            return Optional.empty();
        if (liveRound == null || !liveRound.getInstanceId().equals(roundInstanceId) || liveRound.getRound() != round)
            liveRound = new TeamsLiveRound(this, round, roundInstanceId, map.getDimension());
        return Optional.of(liveRound);
    }

    /** Tells the addons that the playing in progress is over, before the manager moves on from it. */
    private void announceRoundEnded(@Nullable Team winner)
    {
        TeamsLiveRound ended = getLiveRound().orElse(null);
        if (ended == null)
            return;
        ended.markEnded();
        TeamsListeners.roundEnded(ended, winner);
    }

    /** Sets the scoreboard rows an addon shows beside a team's players in the running round. */
    public boolean setScoreboardEntries(Team team, List<TeamsScoreEntry> entries)
    {
        if (!roundRunning || getRoundTeamIndex(team) < 0)
            return false;
        if (entries.isEmpty())
            addonScoreEntries.remove(team.getShortName());
        else
            addonScoreEntries.put(team.getShortName(), List.copyOf(entries));
        return true;
    }

    /** The scoreboard rows addons show beside a team's players. */
    public List<TeamsScoreEntry> getScoreboardEntries(Team team)
    {
        return addonScoreEntries.getOrDefault(team.getShortName(), List.of());
    }

    /** Saves an addon's data with a round of the rotation. */
    public boolean setRoundExtensionData(ITeamsRound round, ResourceLocation key, CompoundTag data)
    {
        if (savedData == null || !(round instanceof TeamsRound internal) || !savedData.rounds.contains(internal))
            return false;
        internal.setExtensionData(key, data);
        markDirty();
        return true;
    }

    public List<TeamsRound> getVoteOptions()
    {
        if (savedData == null)
            return List.of();
        return voteOptionIds.stream().map(id -> savedData.rounds.stream().filter(round -> round.getId().equals(id)).findFirst().orElse(null)).filter(java.util.Objects::nonNull).toList();
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (!enabled)
            stopRound();
        saveRuntime();
    }

    public void setVoting(boolean voting)
    {
        this.voting = voting;
        saveRuntime();
    }

    public void setScoreDisplayTimeSeconds(int seconds)
    {
        scoreDisplayTimeTicks = secondsToTicks(seconds);
        saveRuntime();
    }

    public void setRankUpdateTimeSeconds(int seconds)
    {
        rankUpdateTimeTicks = secondsToTicks(seconds);
        saveRuntime();
    }

    public void setVotingTimeSeconds(int seconds)
    {
        votingTimeTicks = secondsToTicks(seconds);
        saveRuntime();
    }

    /** Sets the greeting; a blank value restores the default rather than clearing it. */
    public void setMotd(@Nullable String value)
    {
        motd = value == null || value.isBlank() ? DEFAULT_MOTD : value.strip();
        saveRuntime();
        if (server == null)
            return;
        server.getPlayerList().getPlayers().forEach(player -> syncLoadouts(player, PacketLoadoutState.OpenScreen.NONE, 0, ""));
    }

    public void setAutoBalanceIntervalSeconds(int seconds)
    {
        autoBalanceIntervalTicks = secondsToTicks(seconds);
        saveRuntime();
    }

    private static int secondsToTicks(int seconds)
    {
        return Math.multiplyExact(seconds, 20);
    }

    public void setExplosionsBreakBlocks(boolean explosionsBreakBlocks)
    {
        this.explosionsBreakBlocks = explosionsBreakBlocks;
        saveRuntime();
    }

    public void setForceAdventureMode(boolean forceAdventureMode)
    {
        this.forceAdventureMode = forceAdventureMode;
        saveRuntime();
    }

    public void setVehiclesNeedFuel(boolean vehiclesNeedFuel)
    {
        this.vehiclesNeedFuel = vehiclesNeedFuel;
        saveRuntime();
    }

    /** Movie prop mode lifts the fuel requirement along with driving the vehicles. */
    public boolean isVehiclesNeedFuel()
    {
        return vehiclesNeedFuel && !movieProp;
    }

    public void setPacifism(boolean pacifism)
    {
        this.pacifism = pacifism;
        saveRuntime();
    }

    public void setMovieProp(boolean movieProp)
    {
        this.movieProp = movieProp;
        saveRuntime();
    }

    /** Overrides the sea level depth charges measure their depth from; null restores that of each dimension. */
    public void setSeaLevelOverride(@Nullable Integer seaLevel)
    {
        seaLevelOverride = seaLevel == null ? NO_SEA_LEVEL_OVERRIDE : seaLevel;
        saveRuntime();
    }

    /** The sea level of a dimension, unless an operator set one with {@code /teams seaLevel}. */
    public int getSeaLevel(Level level)
    {
        return seaLevelOverride == NO_SEA_LEVEL_OVERRIDE ? level.getSeaLevel() : seaLevelOverride;
    }

    public void setVehiclesCanZoom(boolean vehiclesCanZoom)
    {
        this.vehiclesCanZoom = vehiclesCanZoom;
        saveRuntime();
        syncAll(PacketTeamsState.OpenScreen.NONE);
    }

    public Optional<TeamsRound> getCurrentRound()
    {
        if (savedData == null || currentRoundId == null)
            return Optional.empty();
        return savedData.rounds.stream().filter(round -> currentRoundId.equals(round.getId())).findFirst();
    }

    public Optional<com.flansmodultimate.common.teams.GameType> getCurrentGameType()
    {
        return getCurrentRound().map(TeamsRound::getGametype);
    }

    public Optional<LoadoutPool> getCurrentLoadoutPool()
    {
        return Optional.ofNullable(LoadoutPool.get(currentLoadoutPoolId));
    }

    public boolean setCurrentLoadoutPool(@Nullable String id)
    {
        if (id == null || id.isBlank() || "none".equalsIgnoreCase(id))
        {
            currentLoadoutPoolId = "";
            saveRuntime();
            return true;
        }
        LoadoutPool pool = LoadoutPool.get(id);
        if (pool == null)
            return false;
        currentLoadoutPoolId = pool.getShortName();
        saveRuntime();
        return true;
    }

    public void setExperienceMultiplier(float multiplier)
    {
        experienceMultiplier = Math.max(0F, Math.min(100F, multiplier));
        saveRuntime();
    }

    public Collection<TeamsMap> getMaps()
    {
        return savedData == null ? List.of() : java.util.Collections.unmodifiableCollection(savedData.maps.values());
    }

    public List<TeamsRound> getRounds()
    {
        return savedData == null ? List.of() : java.util.Collections.unmodifiableList(savedData.rounds);
    }

    public Optional<TeamsMap> getMap(String id)
    {
        return savedData == null || id == null ? Optional.empty() : Optional.ofNullable(savedData.maps.get(id.toLowerCase(java.util.Locale.ROOT)));
    }

    public TeamsMap addMap(String id, String name, ServerLevel level)
    {
        return addMap(id, name, level, null);
    }

    /** Adds a map, optionally with the playing area its players have to stay inside. */
    public TeamsMap addMap(String id, String name, ServerLevel level, @Nullable TeamsMap.Bounds bounds)
    {
        ensureData();
        TeamsMap map = new TeamsMap(id, name, level.dimension());
        map.setBounds(bounds);

        if (savedData != null && savedData.maps.putIfAbsent(map.getShortName(), map) != null)
            throw new IllegalArgumentException("A map named '" + map.getShortName() + "' already exists");

        markDirty();
        return map;
    }

    public boolean renameMap(String id, String name)
    {
        Optional<TeamsMap> map = getMap(id);
        map.ifPresent(found -> found.setName(name));
        return afterMapChange(map.isPresent());
    }

    /** Sets or, with null, removes the playing area of a map. */
    public boolean setMapBounds(String id, @Nullable TeamsMap.Bounds bounds)
    {
        Optional<TeamsMap> map = getMap(id);
        map.ifPresent(found -> found.setBounds(bounds));
        return afterMapChange(map.isPresent());
    }

    /** Sets the deployment image of a map by resource name. Fails for an unknown map or an unsafe name. */
    public boolean setMapImage(String id, @Nullable String image)
    {
        return afterMapChange(getMap(id).map(map -> map.setDeployImage(image)).orElse(false));
    }

    /**
     * Stores the deployment layout an operator arranged for a map.
     *
     * @return whether the map exists
     */
    public boolean saveDeployLayout(String mapId, Collection<DeployLayoutEntry> entries, TeamsMap.ImageCrop crop)
    {
        Optional<TeamsMap> map = getMap(mapId);
        map.ifPresent(found ->
        {
            found.replaceDeployLayout(entries);
            found.setDeployImageCrop(crop.minU(), crop.minV(), crop.maxU(), crop.maxV());
        });
        return afterMapChange(map.isPresent());
    }

    private boolean afterMapChange(boolean changed)
    {
        if (changed)
        {
            markDirty();
            syncAll(PacketTeamsState.OpenScreen.NONE);
        }
        return changed;
    }

    public boolean removeMap(String id)
    {
        ensureData();
        String normalizedId = id.toLowerCase(java.util.Locale.ROOT);
        if (getCurrentRound().map(round -> round.getMapId().equals(normalizedId)).orElse(false))
            stopRound();

        TeamsMap removed = Objects.requireNonNull(savedData).maps.remove(normalizedId);
        if (removed == null)
            return false;

        savedData.rounds.removeIf(round -> round.getMapId().equals(removed.getShortName()));
        markDirty();
        return true;
    }

    /**
     * Adds a round to the rotation.
     *
     * @param sides
     *            the side of each team, parallel to {@code teamIds}; teams given the same number are allied.
     *            Empty for a round without alliances.
     */
    public TeamsRound addRound(String mapId, String gameTypeId, List<String> teamIds, List<Integer> sides, int minutes, int scoreLimit)
    {
        ensureData();
        TeamsMap map = getMap(mapId).orElseThrow(() -> new IllegalArgumentException("Unknown map: " + mapId));
        com.flansmodultimate.common.teams.GameType type = Optional.ofNullable(com.flansmodultimate.common.teams.GameType.get(gameTypeId))
            .orElseThrow(() -> new IllegalArgumentException("Unknown game type: " + gameTypeId));

        if (!sides.isEmpty() && sides.size() != teamIds.size())
            throw new IllegalArgumentException("Every team needs a side");

        Set<String> seen = new HashSet<>();
        for (String teamId : teamIds)
        {
            Team team = Team.getTeam(teamId);
            if (team == null || team == Team.SPECTATORS)
                throw new IllegalArgumentException("Unknown team: " + teamId);
            // A team listed twice could end up fighting itself, or on two sides at once.
            if (!seen.add(team.getShortName()))
                throw new IllegalArgumentException("Team listed twice: " + teamId);
        }

        TeamsRound round = new TeamsRound(map.getShortName(), type.getId(), teamIds, sides, minutes, scoreLimit);
        String refusal = type.getTeamLimits().describeRefusal(teamIds.size(), round.getSideCount());
        if (refusal != null)
            throw new IllegalArgumentException(type.getName() + " " + refusal);
        Objects.requireNonNull(savedData).rounds.add(round);
        markDirty();

        return round;
    }

    /**
     * Makes a Domination or Conquest round of the rotation one of attackers against defenders, or one every side captures
     * in again.
     *
     * @param side
     *            the attacking side by its position in round order, from 0, or -1 for none
     * @return why the round cannot be played so, or null once it is set
     */
    @Nullable
    public String setRoundAttackingSide(int index, int side)
    {
        ensureData();
        List<TeamsRound> rounds = Objects.requireNonNull(savedData).rounds;
        if (index < 0 || index >= rounds.size())
            return "Invalid round index";
        TeamsRound round = rounds.get(index);
        if (side >= 0 && !round.canBeAttack())
            return "Only Domination and Conquest rounds can be played attacker against defender";
        if (side >= round.getSideCount())
            return "The round has " + round.getSideCount() + " sides";
        round.setAttackingSide(side);
        markDirty();
        return null;
    }

    public boolean removeRound(int index)
    {
        ensureData();
        if (index < 0 || index >= Objects.requireNonNull(savedData).rounds.size())
            return false;
        TeamsRound removed = Objects.requireNonNull(savedData).rounds.remove(index);
        if (removed.getId().equals(currentRoundId))
            stopRound();
        rotationIndex = Math.min(rotationIndex, savedData.rounds.size() - 1);
        markDirty();
        return true;
    }

    /** Number of rounds the generator keeps queued up, as in 1.7.10. */
    private static final int GENERATED_ROUND_TARGET = 4;

    /**
     * Tops the rotation back up to {@link #GENERATED_ROUND_TARGET} rounds by
     * picking random maps, game types and teams. Only teams declaring
     * {@code AllowedForRoundsGenerator True} and game types that opt in are
     * eligible. Does nothing unless the generator is switched on.
     */
    private void generateRounds()
    {
        ensureData();
        if (!roundsGenerator || Objects.requireNonNull(savedData).rounds.size() >= GENERATED_ROUND_TARGET)
            return;

        List<Team> allowedTeams = Team.values().stream().filter(Team::isAllowedForRoundsGenerator).filter(team -> team != Team.SPECTATORS).toList();
        List<com.flansmodultimate.common.teams.GameType> allowedGameTypes = com.flansmodultimate.common.teams.GameType.values().stream()
            .filter(com.flansmodultimate.common.teams.GameType::isAllowedForRoundsGenerator).toList();
        List<TeamsMap> maps = List.copyOf(getMaps());
        if (allowedTeams.isEmpty() || allowedGameTypes.isEmpty() || maps.isEmpty())
            return;

        int missing = GENERATED_ROUND_TARGET - savedData.rounds.size();
        for (int i = 0; i < missing; i++)
        {
            TeamsMap map = maps.get(random.nextInt(maps.size()));
            // A game type is left out when it needs more distinct teams than the generator may use.
            List<com.flansmodultimate.common.teams.GameType> playable = allowedGameTypes.stream().filter(type -> type.isPlayableOn(map) && type.getTeamLimits().minTeams() <= allowedTeams.size())
                .toList();
            if (playable.isEmpty())
                continue;
            com.flansmodultimate.common.teams.GameType gameType = playable.get(random.nextInt(playable.size()));
            // Generated rounds stay at the fewest teams the game type takes, each on its own side.
            int teamCount = gameType.getTeamLimits().minTeams();
            List<Team> candidates = new ArrayList<>(allowedTeams);
            List<String> teamIds = new ArrayList<>();
            for (int team = 0; team < teamCount; team++)
                teamIds.add(candidates.remove(random.nextInt(candidates.size())).getShortName());

            addRound(map.getShortName(), gameType.getId(), teamIds, List.of(), 10 + random.nextInt(10), generatedScoreLimit(gameType));
        }
    }

    private static int generatedScoreLimit(com.flansmodultimate.common.teams.GameType gameType)
    {
        // Tickets in the objective modes: reinforcements for the attackers of an Assault, score to reach otherwise.
        if (gameType.getObjectiveMode() != ObjectiveMode.NONE)
            return gameType.getObjectiveMode() == ObjectiveMode.ASSAULT ? 100 : 300;
        if (gameType instanceof GameTypeCTF)
            return 5;
        if (gameType instanceof GameTypeTDM)
            return 30;
        if (gameType instanceof GameTypeDM)
            return 20;
        return 10;
    }

    public boolean startRound(int index)
    {
        ensureData();
        generateRounds();
        if (!enabled || index < 0 || index >= Objects.requireNonNull(savedData).rounds.size())
            return false;
        TeamsRound next = Objects.requireNonNull(savedData).rounds.get(index);
        if (next.getGametype() == null || getMap(next.getMapId()).isEmpty())
            return false;

        announceRoundEnded(null);
        getCurrentGameType().ifPresent(type -> type.roundEnded(this));
        updateActiveChunkTickets(false);
        liveBases.values().forEach(ITeamBase::roundCleanup);
        getCurrentGameType().ifPresent(type -> type.roundCleanup(this));
        rotationIndex = index;
        currentRoundId = next.getId();
        roundInstanceId = UUID.randomUUID();
        lastWinnerId = "";
        lastRoundDecided = false;
        outOfBoundsSeconds.clear();
        roundTimeLeftTicks = next.getTimeLimitTicks();
        roundElapsedTicks = 0;
        intermissionTicks = 0;
        intermissionPhase = IntermissionPhase.NONE;
        roundRunning = true;
        voteOptionIds.clear();
        resetScores();
        addonScoreEntries.clear();
        savedData.rounds.forEach(round ->
        {
            if (round == next)
                round.markPlayed();
            else
                round.markSkipped();
        });
        liveBases.values().stream().filter(this::isBaseInCurrentMap).forEach(ITeamBase::startRound);
        updateActiveChunkTickets(true);

        for (ServerPlayer player : getServer().getPlayerList().getPlayers())
        {
            PlayerData data = PlayerData.getInstance(player);
            data.setScore(0);
            data.setKills(0);
            data.setDeaths(0);
            data.setZombieScore(0);
            data.setVote(0);
            Team selected = data.getNewTeam();
            if (getRoundTeamIndex(selected) < 0 && selected != Team.SPECTATORS)
                selectTeam(player, Team.SPECTATORS, true);
            respawnPlayer(player, false);
        }
        next.getGametype().roundStarted(this);
        getLiveRound().ifPresent(TeamsListeners::roundStarted);
        broadcast(Component.literal("Starting " + next.getGametype().getName() + " on " + getMap(next.getMapId()).orElseThrow().getName()));
        saveRuntime();
        getServer().getPlayerList().getPlayers().forEach(player ->
        {
            // The break's own screens belong to the break: take down anything still showing
            // results before the new round's team selection goes up.
            syncLoadouts(player, PacketLoadoutState.OpenScreen.CLOSE, 0, "");
            syncPlayer(player, getPlayerTeam(player) == null || getPlayerTeam(player) == Team.SPECTATORS ? PacketTeamsState.OpenScreen.TEAM_SELECT : PacketTeamsState.OpenScreen.CLOSE);
        });
        return true;
    }

    public boolean startNextRound()
    {
        ensureData();
        generateRounds();
        if (Objects.requireNonNull(savedData).rounds.isEmpty())
            return false;
        int queued = queuedRoundIndex();
        queuedRoundId = null;
        return startRound(queued >= 0 ? queued : (rotationIndex + 1) % savedData.rounds.size());
    }

    /**
     * Makes the round at {@code index} the next one played, whether the current round ends on its own,
     * an operator skips to the next one, or a vote would otherwise have picked another.
     */
    public boolean queueNextRound(int index)
    {
        ensureData();
        if (index < 0 || index >= Objects.requireNonNull(savedData).rounds.size())
            return false;
        queuedRoundId = savedData.rounds.get(index).getId();
        saveRuntime();
        return true;
    }

    /** The rotation index of the queued round, or -1 when none is queued or it no longer exists. */
    public int queuedRoundIndex()
    {
        if (queuedRoundId == null || savedData == null)
            return -1;
        for (int i = 0; i < savedData.rounds.size(); i++)
            if (savedData.rounds.get(i).getId().equals(queuedRoundId))
                return i;
        return -1;
    }

    /** Changes the score limit of the round being played, as the legacy {@code scorelimit} game type variable did. */
    public boolean setCurrentScoreLimit(int scoreLimit)
    {
        Optional<TeamsRound> round = getCurrentRound();
        if (round.isEmpty() || scoreLimit < 1)
            return false;
        round.get().setScoreLimit(scoreLimit);
        markDirty();
        syncAll(PacketTeamsState.OpenScreen.NONE);
        return true;
    }

    /** Persists the Teams rules after a caller changed some of them through their setters. */
    public void saveSettings()
    {
        saveRuntime();
        syncAll(PacketTeamsState.OpenScreen.NONE);
    }

    /** Lag compensation, configured in the common config so the legacy BltSS settings survive restarts. */
    public int getBulletSnapshotMin()
    {
        return ModCommonConfig.bulletSnapshotMin();
    }

    public int getBulletSnapshotDivisor()
    {
        return ModCommonConfig.bulletSnapshotDivisor();
    }

    public void stopRound()
    {
        announceRoundEnded(null);
        getCurrentGameType().ifPresent(type -> type.roundEnded(this));
        updateActiveChunkTickets(false);
        liveBases.values().forEach(ITeamBase::roundCleanup);
        getCurrentGameType().ifPresent(type -> type.roundCleanup(this));
        roundRunning = false;
        currentRoundId = null;
        roundInstanceId = null;
        lastWinnerId = "";
        lastRoundDecided = false;
        outOfBoundsSeconds.clear();
        roundTimeLeftTicks = 0;
        roundElapsedTicks = 0;
        intermissionTicks = 0;
        intermissionPhase = IntermissionPhase.NONE;
        voteOptionIds.clear();
        resetScores();
        addonScoreEntries.clear();
        saveRuntime();
        syncAll(PacketTeamsState.OpenScreen.CLOSE);
    }

    public void tick()
    {
        if (server == null || savedData == null)
            return;
        if (server.getTickCount() % 40 == 0)
            syncAll(PacketTeamsState.OpenScreen.NONE);
        if (server.getTickCount() % 20 == 0)
        {
            server.getPlayerList().getPlayers().forEach(player -> getStats(player).addPlayTime(20));
            syncPlayerClassSkins(false);
            markDirty();
        }
        if (!enabled)
            return;
        if (intermissionTicks > 0)
        {
            if (--intermissionTicks == 0)
                advanceIntermission();
            else if (intermissionTicks % 20 == 0)
                saveRuntime();
            return;
        }
        if (!roundRunning)
            return;

        roundElapsedTicks++;
        roundTimeLeftTicks = Math.max(0, roundTimeLeftTicks - 1);
        getCurrentGameType().ifPresent(type -> type.tick(this));
        enforceMapBounds();
        int autoBalancePhase = roundElapsedTicks % autoBalanceIntervalTicks;
        if (autoBalancePhase == autoBalanceIntervalTicks - AUTO_BALANCE_WARNING_TICKS && needsAutoBalance())
            broadcast(Component.literal("Autobalancing teams in 10 seconds..."));
        if (autoBalancePhase == 0)
            autoBalanceIfNeeded();

        boolean winner = getCurrentRound().stream().flatMap(round -> round.getTeamIds().stream()).map(Team::getTeam).filter(java.util.Objects::nonNull)
            .anyMatch(team -> getCurrentGameType().map(type -> type.hasWinner(this, team)).orElse(false));
        if (winner || roundTimeLeftTicks <= 0)
            finishRound();
        else if (roundElapsedTicks % 20 == 0)
            saveRuntime();
    }

    /** Lets a game type restart the clock, as Assault does each time an objective falls. */
    public void setRoundTimeLeftTicks(int ticks)
    {
        roundTimeLeftTicks = Math.max(0, ticks);
        saveRuntime();
        syncAll(PacketTeamsState.OpenScreen.NONE);
    }

    /**
     * Warns the players of the round who are outside the playing area of the map, once a second,
     * and kills the ones who stayed out for the whole grace period.
     */
    private void enforceMapBounds()
    {
        if (roundElapsedTicks % 20 != 0)
            return;
        TeamsMap map = getCurrentRound().flatMap(round -> getMap(round.getMapId())).orElse(null);
        TeamsMap.Bounds bounds = map == null ? null : map.getBounds();
        if (bounds == null)
        {
            outOfBoundsSeconds.clear();
            return;
        }
        for (ServerPlayer player : getServer().getPlayerList().getPlayers())
        {
            boolean playing = getRoundTeamIndex(getPlayerTeam(player)) >= 0 && player.isAlive() && !player.getAbilities().instabuild && !player.isSpectator();
            boolean inside = player.level().dimension().equals(map.getDimension()) && bounds.contains(player.position());
            if (playing && !inside)
                countOutOfBounds(player);
            else
                outOfBoundsSeconds.remove(player.getUUID());
        }
    }

    /** Counts one more second outside the playing area for a player, warning them until the grace period is spent. */
    private void countOutOfBounds(ServerPlayer player)
    {
        int seconds = outOfBoundsSeconds.merge(player.getUUID(), 1, Integer::sum);
        if (seconds <= OUT_OF_BOUNDS_GRACE_SECONDS)
        {
            player.displayClientMessage(Component.translatable("message.flansmodultimate.teams.out_of_bounds", OUT_OF_BOUNDS_GRACE_SECONDS - seconds), true);
            return;
        }
        outOfBoundsSeconds.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.out_of_bounds_killed"));
        // A rider is put out of their seat first, because damage to anyone in a driveable is suppressed.
        player.stopRiding();
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
    }

    /** Ends the running round at once with the side of {@code winner} as the winner, as an addon decided. */
    public boolean declareWinner(Team winner)
    {
        if (!roundRunning || getRoundTeamIndex(winner) < 0)
            return false;
        finishRound(winner);
        return true;
    }

    private void finishRound()
    {
        finishRound(null);
    }

    /**
     * @param declaredWinner
     *            the winner an addon declared, or null for the game type to decide
     */
    private void finishRound(@Nullable Team declaredWinner)
    {
        if (!roundRunning)
            return;
        // Decided before anything is cleaned up, while the scores and capture points still tell who won.
        Team winner = declaredWinner == null ? getCurrentGameType().map(type -> type.getWinner(this)).orElse(null) : declaredWinner;
        lastWinnerId = winner == null ? "" : winner.getShortName();
        lastRoundDecided = true;
        // Announced while the round is still set up, so listeners can read how it ended.
        announceRoundEnded(lastWinnerId.isEmpty() ? null : Team.getTeam(lastWinnerId));
        outOfBoundsSeconds.clear();
        roundRunning = false;
        getCurrentGameType().ifPresent(type -> type.roundEnded(this));
        awardRoundStats();
        voteOptionIds.clear();
        intermissionPhase = IntermissionPhase.NONE;
        broadcast(Component.literal("Round over. " + nextRoundNotice()));
        beginScoreDisplay();
    }

    /** Says when play resumes, counting every stage of the break that is switched on. */
    private String nextRoundNotice()
    {
        int beforeVoting = scoreDisplayTimeTicks + rankUpdateTicksForRound();
        if (voting)
            return "Voting begins in " + beforeVoting / 20 + " seconds.";
        return "Next round starts in " + beforeVoting / 20 + " seconds.";
    }

    /** The rank stage only applies while a loadout pool is giving out ranks and rewards. */
    private int rankUpdateTicksForRound()
    {
        return getCurrentLoadoutPool().isPresent() ? rankUpdateTimeTicks : 0;
    }

    private void beginScoreDisplay()
    {
        intermissionPhase = IntermissionPhase.SCORES;
        intermissionTicks = scoreDisplayTimeTicks;
        saveRuntime();
        if (intermissionTicks == 0)
        {
            advanceIntermission();
            return;
        }
        syncAll(PacketTeamsState.OpenScreen.SCOREBOARD);
    }

    private void beginRankUpdate()
    {
        intermissionPhase = IntermissionPhase.RANK_UPDATE;
        intermissionTicks = rankUpdateTicksForRound();
        saveRuntime();
        if (intermissionTicks == 0)
        {
            advanceIntermission();
            return;
        }
        getServer().getPlayerList().getPlayers().forEach(player -> syncLoadouts(player, PacketLoadoutState.OpenScreen.MISSION_RESULTS, 0, ""));
    }

    /** Steps to the next stage of the break, skipping any that is switched off. */
    private void advanceIntermission()
    {
        switch (intermissionPhase)
        {
            case NONE, SCORES -> {
                if (rankUpdateTicksForRound() > 0)
                    beginRankUpdate();
                else if (voting)
                    beginVoting();
                else
                    startNextRound();
            }
            case RANK_UPDATE -> {
                if (voting)
                    beginVoting();
                else
                    startNextRound();
            }
            case VOTING -> startVotedRound();
        }
    }

    private void beginVoting()
    {
        intermissionPhase = IntermissionPhase.VOTING;
        pickVoteOptions();
        intermissionTicks = votingTimeTicks;
        broadcast(Component.literal("Vote for the next round with /teams vote <number>."));
        List<TeamsRound> options = getVoteOptions();
        for (int i = 0; i < options.size(); i++)
        {
            TeamsRound option = options.get(i);
            broadcast(Component.literal((i + 1) + ". " + option.getGameTypeId() + " @ " + option.getMapId()));
        }
        saveRuntime();
        if (intermissionTicks == 0)
            startVotedRound();
        else
            syncAll(PacketTeamsState.OpenScreen.VOTING);
    }

    private void pickVoteOptions()
    {
        voteOptionIds.clear();
        if (savedData == null)
            return;
        savedData.rounds.stream().filter(round -> !round.getId().equals(currentRoundId)).sorted().limit(5).map(TeamsRound::getId).forEach(voteOptionIds::add);
        if (voteOptionIds.isEmpty() && currentRoundId != null)
            voteOptionIds.add(currentRoundId);
        getServer().getPlayerList().getPlayers().forEach(player -> PlayerData.getInstance(player).setVote(0));
    }

    public boolean castVote(ServerPlayer player, int option)
    {
        if (intermissionPhase != IntermissionPhase.VOTING || intermissionTicks <= 0 || option < 1 || option > voteOptionIds.size())
            return false;
        PlayerData.getInstance(player).setVote(option);
        syncAll(PacketTeamsState.OpenScreen.NONE);
        return true;
    }

    private void startVotedRound()
    {
        if (queuedRoundIndex() >= 0)
        {
            voteOptionIds.clear();
            startNextRound();
            return;
        }
        int[] votes = new int[voteOptionIds.size()];
        for (ServerPlayer player : getServer().getPlayerList().getPlayers())
        {
            int vote = PlayerData.getInstance(player).getVote();
            if (vote > 0 && vote <= votes.length)
                votes[vote - 1]++;
        }
        int winner = 0;
        for (int i = 1; i < votes.length; i++)
            if (votes[i] > votes[winner])
                winner = i;
        UUID chosen = voteOptionIds.get(winner);
        voteOptionIds.clear();
        for (int i = 0; i < Objects.requireNonNull(savedData).rounds.size(); i++)
        {
            if (savedData.rounds.get(i).getId().equals(chosen))
            {
                startRound(i);
                return;
            }
        }
        startNextRound();
    }

    private void awardRoundStats()
    {
        for (ServerPlayer player : getServer().getPlayerList().getPlayers())
            if (getPlayerTeam(player) != Team.SPECTATORS)
                getStats(player).recordRound();
        getCurrentRound().ifPresent(round ->
        {
            for (String id : round.getTeamIds())
            {
                getPlayersOnTeam(Team.getTeam(id)).stream().max(Comparator.comparingInt(player -> PlayerData.getInstance(player).getScore())).ifPresent(player ->
                {
                    getStats(player).recordMvp();
                    awardExperience(player, 250);
                });
            }
        });
        markDirty();
    }

    public boolean selectTeam(ServerPlayer player, @Nullable Team team, boolean force)
    {
        if (team == null)
            team = Team.SPECTATORS;
        if (team != Team.SPECTATORS && getRoundTeamIndex(team) < 0)
            return false;
        if (!force && team != Team.SPECTATORS && wouldUnbalance(player, team))
            return false;
        if (!force && team != Team.SPECTATORS && !canChooseTeam(player, team))
            return false;
        PlayerData data = PlayerData.getInstance(player);
        if (!force)
        {
            Team chosen = team;
            getCurrentGameType().ifPresent(type -> type.playerChoseTeam(this, player, data.getTeam(), chosen));
        }
        data.setBuilder(false);
        data.setNewTeam(team);
        if (force || !player.isAlive())
            data.setTeam(team);
        if (team == Team.SPECTATORS)
        {
            data.setNewPlayerClass(null);
            data.setPlayerClass(null);
        }
        getStats(player).setSelection(team.getShortName(), data.getNewPlayerClass() == null ? "" : data.getNewPlayerClass().getShortName());
        markDirty();
        return true;
    }

    /** Whether the running game type lets this player pick {@code team} from the team menu. */
    public boolean canChooseTeam(ServerPlayer player, Team team)
    {
        Optional<TeamsRound> round = getCurrentRound();
        Optional<com.flansmodultimate.common.teams.GameType> type = getCurrentGameType();
        return round.isEmpty() || type.isEmpty() || type.get().getTeamsCanSpawnAs(this, round.get(), player).contains(team);
    }

    public boolean selectBuilder(ServerPlayer player)
    {
        if (!player.hasPermissions(2))
            return false;
        PlayerData data = PlayerData.getInstance(player);
        data.setBuilder(true);
        data.setTeam(null);
        data.setNewTeam(null);
        data.setPlayerClass(null);
        data.setNewPlayerClass(null);
        player.setGameMode(GameType.CREATIVE);
        getStats(player).setSelection("", "");
        markDirty();
        return true;
    }

    public boolean selectClass(ServerPlayer player, @Nullable PlayerClass playerClass)
    {
        PlayerData data = PlayerData.getInstance(player);
        Team team = data.getNewTeam();
        if (playerClass == null || team == null || describeClassRefusal(player, playerClass) != null)
            return false;
        data.setNewPlayerClass(playerClass);
        getStats(player).setSelection(team.getShortName(), playerClass.getShortName());
        markDirty();
        return true;
    }

    /**
     * Why a player may not take a class on the team they picked, or null when they may.
     *
     * <p>
     * A class can be closed by the unlock rank, by the prestige level it requires, or because
     * its {@code ClassLimit} / {@code ClassLimitPercent} slots on that team are all taken.
     * </p>
     */
    @Nullable
    public Component describeClassRefusal(ServerPlayer player, @Nullable PlayerClass playerClass)
    {
        Team team = PlayerData.getInstance(player).getNewTeam();
        if (playerClass == null || team == null || team == Team.SPECTATORS || !team.getClasses().contains(playerClass))
            return Component.translatable("message.flansmodultimate.teams.class_unavailable");
        PlayerStats stats = getStats(player);
        if (stats.getRank() < playerClass.getUnlockLevel())
            return Component.translatable("message.flansmodultimate.teams.class_rank", playerClass.getName(), playerClass.getUnlockLevel());
        if (stats.getPrestige() < playerClass.getRequiredPrestigeLevel())
            return Component.translatable("message.flansmodultimate.teams.class_prestige", playerClass.getName(), playerClass.getRequiredPrestigeLevel());
        int capacity = getClassCapacity(team, playerClass);
        int used = countPlayersUsingClass(team, playerClass, player);
        if (!ClassLimits.hasSlot(capacity, used))
            return Component.translatable("message.flansmodultimate.teams.class_full", playerClass.getName(), used, capacity);
        return null;
    }

    /** Slots a limited class has on a team at its present size; {@link ClassLimits#UNLIMITED} when it has no limit. */
    public int getClassCapacity(Team team, PlayerClass playerClass)
    {
        return ClassLimits.capacity(playerClass.getClassLimit(), playerClass.getClassLimitPercent(), getPlayersChoosing(team).size());
    }

    /**
     * Players of a team using a class, or waiting to respawn with it. Counting the pending choice
     * keeps repeated selections from getting past a limit; {@code except} is left out so that a
     * player keeping their own class does not count against themselves.
     */
    public int countPlayersUsingClass(Team team, PlayerClass playerClass, @Nullable ServerPlayer except)
    {
        int count = 0;
        for (ServerPlayer member : getPlayersChoosing(team))
        {
            PlayerData data = PlayerData.getInstance(member);
            PlayerClass effective = data.getNewPlayerClass() != null ? data.getNewPlayerClass() : data.getPlayerClass();
            if (member != except && effective == playerClass)
                count++;
        }
        return count;
    }

    /** Everyone on a team or about to join it, which is the team size class limits are measured against. */
    private List<ServerPlayer> getPlayersChoosing(Team team)
    {
        return server == null ? List.of() : server.getPlayerList().getPlayers().stream().filter(player -> team.equals(PlayerData.getInstance(player).getNewTeam())).toList();
    }

    /** The rank at which a player may trade their progress for a prestige level. */
    public int getPrestigeRank()
    {
        return getCurrentLoadoutPool().map(LoadoutPool::getMaxLevel).orElse(PlayerStats.DEFAULT_PRESTIGE_RANK);
    }

    public boolean gainPrestige(ServerPlayer player)
    {
        if (!getStats(player).gainPrestige(getPrestigeRank()))
            return false;
        markDirty();
        syncPlayer(player, PacketTeamsState.OpenScreen.NONE);
        return true;
    }

    /**
     * Records where a dead player asked to come back on the deployment map.
     *
     * @param capturePointId
     *            the capture point, or null for headquarters
     * @return whether the running game type lets players choose
     */
    public boolean selectSpawn(ServerPlayer player, @Nullable UUID capturePointId)
    {
        return roundRunning && getCurrentGameType().map(type -> type.selectSpawn(this, player, capturePointId)).orElse(false);
    }

    /**
     * Puts a player's pending team and class choice into effect.
     *
     * <p>
     * What that costs depends on what changed. A player who has not taken the field yet
     * simply spawns. Someone changing class on the team they are already fighting for keeps
     * playing and gets the new kit at their next spawn, so the class menu cannot be used as a
     * free resupply. Changing sides mid-round is a defection: it is announced and the player
     * dies where they stand, coming back on their new team.
     * </p>
     */
    public void confirmSelection(ServerPlayer player)
    {
        PlayerData data = PlayerData.getInstance(player);
        Team current = data.getTeam();
        Team next = data.getNewTeam();

        // Waiting to respawn already: the respawn hook applies the choice when they come back.
        if (!player.isAlive())
            return;

        // Not in play yet, so there is nothing to interrupt: take the field.
        if (!roundRunning || current == null || next == null || current == Team.SPECTATORS)
        {
            respawnPlayer(player, true, ITeamsRespawnRule.Reason.JOINED);
            Team entered = data.getTeam();
            if (roundRunning && entered != null && entered != Team.SPECTATORS)
                getCurrentGameType().ifPresent(type -> type.playerEnteredTheGame(this, player, entered, data.getPlayerClass()));
            return;
        }

        if (current == next)
        {
            announcePendingKit(player, data);
            return;
        }

        broadcast(Component.translatable("message.flansmodultimate.teams.switched_team", player.getScoreboardName(), next.getName()));
        getCurrentGameType().ifPresent(type -> type.playerDefected(this, player, current, next));

        // Dying is what makes the change take hold: the respawn hook then applies the pending
        // selection and places the player at their new team's spawn. A rider is put out of
        // their seat first, because damage to anyone in a driveable is suppressed.
        player.stopRiding();
        if (!player.getAbilities().instabuild)
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        // Creative players, and anyone a game type shields from damage, change over directly.
        if (player.isAlive())
            respawnPlayer(player, true, ITeamsRespawnRule.Reason.CHANGED_TEAM);
    }

    /**
     * Tells a player still fighting for the same team that their new kit is queued.
     *
     * <p>
     * It deliberately does not take effect now: applying it here would turn the class and
     * loadout menus into a full heal and resupply in the middle of a firefight.
     * </p>
     */
    private void announcePendingKit(ServerPlayer player, PlayerData data)
    {
        PlayerClass playerClass = data.getNewPlayerClass();
        if (playerClass != null && playerClass != data.getPlayerClass())
        {
            getCurrentGameType().ifPresent(type -> type.playerChoseNewClass(this, player, playerClass));
            player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.class_on_respawn", playerClass.getName()));
            return;
        }
        if (getCurrentLoadoutPool().isPresent())
            player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.loadout_on_respawn"));
    }

    /**
     * Whether joining {@code requested} would leave the sides of the round uneven. Balance is kept
     * between sides only, so a player is always free to move to another team of their own side.
     */
    private boolean wouldUnbalance(ServerPlayer player, Team requested)
    {
        Optional<com.flansmodultimate.common.teams.GameType> type = getCurrentGameType();
        TeamsRound round = getCurrentRound().orElse(null);
        if (round == null || type.isEmpty() || !type.get().isAutoBalanceEnabled())
            return false;
        PlayerData data = PlayerData.getInstance(player);
        if (round.areAllied(requested, data.getTeam()) || round.areAllied(requested, data.getNewTeam()))
            return false;
        int minimum = round.getSideIds().stream().mapToInt(sideId -> getPlayersOnSide(round, sideId).size()).min().orElse(0);
        return getPlayersOnSide(round, round.getSideId(requested)).size() > minimum;
    }

    /** Moves the lowest scoring player of the largest side to the smallest team of the smallest side. */
    private void autoBalanceIfNeeded()
    {
        TeamsRound round = getCurrentRound().orElse(null);
        if (round == null || !needsAutoBalance())
            return;

        List<Integer> sideIds = round.getSideIds();
        int largest = sideIds.stream().max(Comparator.comparingInt(sideId -> getPlayersOnSide(round, sideId).size())).orElseThrow();
        int smallest = sideIds.stream().min(Comparator.comparingInt(sideId -> getPlayersOnSide(round, sideId).size())).orElseThrow();
        Team destination = round.getTeamsOnSide(smallest).stream().min(Comparator.comparingInt(team -> getPlayersOnTeam(team).size())).orElse(null);
        if (destination == null)
            return;

        getPlayersOnSide(round, largest).stream().min(Comparator.comparingInt(player -> PlayerData.getInstance(player).getScore())).ifPresent(player ->
        {
            selectTeam(player, destination, true);
            PlayerData.getInstance(player).setPlayerMovedByAutobalancer(true);
            respawnPlayer(player, false);
            player.sendSystemMessage(Component.literal("You were moved to balance the teams"));
        });
    }

    private boolean needsAutoBalance()
    {
        TeamsRound round = getCurrentRound().orElse(null);
        if (round == null || getCurrentGameType().map(type -> !type.isAutoBalanceEnabled()).orElse(true))
            return false;
        List<Integer> sideIds = round.getSideIds();
        if (sideIds.size() < 2)
            return false;
        int largest = sideIds.stream().mapToInt(sideId -> getPlayersOnSide(round, sideId).size()).max().orElse(0);
        int smallest = sideIds.stream().mapToInt(sideId -> getPlayersOnSide(round, sideId).size()).min().orElse(0);
        return largest - smallest > 1;
    }

    /** Everyone playing for any team of a side. */
    private List<ServerPlayer> getPlayersOnSide(TeamsRound round, int sideId)
    {
        return round.getTeamsOnSide(sideId).stream().flatMap(team -> getPlayersOnTeam(team).stream()).toList();
    }

    public void playerLoggedIn(ServerPlayer player)
    {
        PlayerStats stats = getStats(player);
        Team team = Team.getTeam(stats.getSelectedTeam());
        PlayerClass playerClass = PlayerClass.getPlayerClass(stats.getSelectedClass());
        PlayerData data = PlayerData.getInstance(player);
        data.setTeam(team == null ? Team.SPECTATORS : team);
        data.setNewTeam(data.getTeam());
        if (playerClass != null && data.getTeam().getClasses().contains(playerClass))
        {
            data.setPlayerClass(playerClass);
            data.setNewPlayerClass(playerClass);
        }
        // The joining client starts with no assignments at all, so resend them in full
        syncPlayerClassSkins(true);
        if (roundRunning && (data.getTeam() == null || data.getTeam() == Team.SPECTATORS) && getCurrentLoadoutPool().isPresent())
            syncLoadouts(player, PacketLoadoutState.OpenScreen.HUB, 0, "");
        else
            syncPlayer(player, roundRunning && (data.getTeam() == null || data.getTeam() == Team.SPECTATORS) ? PacketTeamsState.OpenScreen.TEAM_SELECT : PacketTeamsState.OpenScreen.NONE);
        if (roundRunning)
            getCurrentGameType().ifPresent(type -> type.playerJoined(this, player));
    }

    public void playerLoggedOut(ServerPlayer player)
    {
        if (roundRunning)
            getCurrentGameType().ifPresent(type -> type.playerQuit(this, player));
        dropFlag(player);
        PlayerData.removeServerData(player.getUUID());
        markDirty();
    }

    public void playerDied(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source)
    {
        getCurrentGameType().ifPresent(type -> type.playerKilled(this, player, source));
        dropFlag(player);
        announceCombatantKilled(player, source);
    }

    public void respawnPlayer(ServerPlayer player, boolean immediate)
    {
        respawnPlayer(player, immediate, null);
    }

    /**
     * Places a player for the round: in the field with their loadout at a spawn point of their team, or as a spectator.
     *
     * @param reason
     *            why the player takes the field, for the respawn rules of addons to decide on; null places the player
     *            without asking them, as when a round starts or the auto-balancer moves someone
     */
    public void respawnPlayer(ServerPlayer player, boolean immediate, @Nullable ITeamsRespawnRule.Reason reason)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.applyPendingTeamSelection();
        if (!roundRunning || data.getTeam() == null)
            return;
        if (reason != null && data.getTeam() != Team.SPECTATORS && !mayRespawn(player, data.getTeam(), reason))
        {
            // Held back by an addon's rule: the player stays on their team, watching until the round ends.
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.respawn_denied"));
            return;
        }
        if (forceAdventureMode)
            player.setGameMode(data.getTeam() == Team.SPECTATORS ? GameType.SPECTATOR : GameType.ADVENTURE);
        else if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && data.getTeam() != Team.SPECTATORS)
            player.setGameMode(GameType.SURVIVAL);
        if (data.getTeam() != Team.SPECTATORS)
        {
            applyLoadout(player);
            // As in 1.7.10, respawned players hold their first hotbar slot and have that gun reloaded
            player.getInventory().selected = 0;
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
            data.setRespawnReloadTicks(RESPAWN_RELOAD_DELAY_TICKS);
        }
        spawnPosition(player, data.getTeam(), reason).ifPresent(position ->
        {
            TeamsMap map = getCurrentRound().flatMap(round -> getMap(round.getMapId())).orElse(null);
            ServerLevel level = map == null ? null : getServer().getLevel(map.getDimension());
            if (level != null)
                player.teleportTo(level, position.x, position.y, position.z, player.getYRot(), player.getXRot());
        });
        if (immediate)
            player.setHealth(player.getMaxHealth());
        getCurrentGameType().ifPresent(type -> type.playerRespawned(this, player));
    }

    /** Where a player appears: where an addon's spawn provider puts them, or at the game type's spawn point. */
    private Optional<Vec3> spawnPosition(ServerPlayer player, Team team, @Nullable ITeamsRespawnRule.Reason reason)
    {
        if (reason != null && team != Team.SPECTATORS)
        {
            Optional<Vec3> offered = getLiveRound().flatMap(round -> TeamsListeners.spawnPosition(round, player, team, reason));
            if (offered.isPresent())
                return offered;
        }
        return getCurrentGameType().map(type -> type.getSpawnPoint(this, player));
    }

    private boolean mayRespawn(ServerPlayer player, Team team, ITeamsRespawnRule.Reason reason)
    {
        return getLiveRound().map(round -> TeamsListeners.mayRespawn(round, player, team, reason)).orElse(true);
    }

    /**
     * Reports a non-player death to the running game type, as the 1.12.2 death handler did. A combatant of the round
     * dies as a fighter of its team; any other entity is only reported.
     */
    public void entityDied(Entity entity, net.minecraft.world.damagesource.DamageSource source)
    {
        if (!roundRunning)
            return;
        Team team = TeamsCombatants.getCombatantTeam(this, entity);
        if (team != null && entity instanceof LivingEntity combatant)
        {
            getCurrentGameType().ifPresent(type -> type.combatantKilled(this, combatant, team, source));
            dropFlag(combatant);
            announceCombatantKilled(combatant, source);
        }
        else
            getCurrentGameType().ifPresent(type -> type.entityKilled(this, entity, source));
    }

    private void announceCombatantKilled(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source)
    {
        if (TeamsCombatants.getPlayingTeam(this, victim) == null)
            return;
        LivingEntity killer = TeamsCombatants.getAttacker(source);
        getLiveRound().ifPresent(round -> TeamsListeners.combatantKilled(round, victim, killer == victim ? null : killer, source));
    }

    /** Reports an attempt to damage an invulnerable base or team object to the running game type. */
    public void teamEntityAttacked(Object target, net.minecraft.world.damagesource.DamageSource source)
    {
        if (!roundRunning)
            return;
        getCurrentGameType().ifPresent(type ->
        {
            if (target instanceof ITeamBase base && isBaseInCurrentMap(base))
                type.baseAttacked(this, base, source);
            else if (target instanceof ITeamObject object)
                type.objectAttacked(this, object, source);
        });
    }

    private void applyLoadout(ServerPlayer player)
    {
        PlayerData data = PlayerData.getInstance(player);
        Team team = data.getTeam();
        PlayerClass playerClass = data.getPlayerClass();
        data.setReloadedAfterRespawn(false);
        player.getInventory().clearContent();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET))
        {
            ItemStack stack = playerClass == null ? ItemStack.EMPTY : playerClass.getArmour(slot);
            if (stack.isEmpty() && (playerClass == null || playerClass.usesTeamArmour(slot)))
                stack = team.getArmour(slot);
            player.setItemSlot(slot, stack.copy());
        }
        LoadoutPool pool = getCurrentLoadoutPool().orElse(null);
        if (pool != null)
        {
            PlayerLoadout loadout = getStats(player).getSelectedLoadout(pool);
            ItemStack armour = loadout.get(LoadoutSlot.ARMOUR);
            if (!armour.isEmpty())
                player.setItemSlot(EquipmentSlot.CHEST, armour.copy());
            for (LoadoutSlot loadoutSlot : LoadoutSlot.values())
            {
                if (loadoutSlot == LoadoutSlot.ARMOUR)
                    continue;
                ItemStack stack = loadout.get(loadoutSlot);
                if (!stack.isEmpty() && !player.getInventory().add(stack.copy()))
                    player.drop(stack.copy(), false);
                if (stack.getItem() instanceof IFlanItem<?> flanItem)
                {
                    for (ItemStack extra : pool.createExtraItems(loadoutSlot, flanItem.getConfigType()))
                        if (!player.getInventory().add(extra.copy()))
                            player.drop(extra.copy(), false);
                }
            }
        }
        else if (playerClass != null)
        {
            Map<Integer, ItemStack> layout = playerClass.createStartingInventory();
            if (!layout.isEmpty())
                layout.forEach((slot, stack) -> player.getInventory().setItem(slot, stack.copy()));
            else
                for (ItemStack stack : playerClass.createStartingItems())
                    if (!player.getInventory().add(stack.copy()))
                        player.drop(stack.copy(), false);
        }
        if (pool != null || playerClass == null || playerClass.createStartingInventory().isEmpty())
            equipMeleeOnlyLoadout(player);
        player.getInventory().setChanged();
    }

    /** Places a melee gun in the main slot when the kit has no non-melee gun. */
    private void equipMeleeOnlyLoadout(ServerPlayer player)
    {
        var inventory = player.getInventory();
        int meleeSlot = -1;
        for (int slot = 0; slot < inventory.items.size(); slot++)
        {
            ItemStack stack = inventory.items.get(slot);
            if (!(stack.getItem() instanceof GunItem gun))
                continue;
            if (!gun.getConfigType().getPrimaryFunction().isMelee())
                return;
            if (meleeSlot < 0)
                meleeSlot = slot;
        }
        if (meleeSlot > 0)
        {
            ItemStack melee = inventory.items.get(meleeSlot);
            inventory.items.set(meleeSlot, inventory.items.get(0));
            inventory.items.set(0, melee);
        }
    }

    public void awardExperience(ServerPlayer player, int baseAmount)
    {
        if (baseAmount <= 0)
            return;

        PlayerStats stats = getStats(player);
        LoadoutPool pool = getCurrentLoadoutPool().orElse(null);
        int amount = Math.round(baseAmount * experienceMultiplier);
        int rankBefore = stats.getRank();
        if (pool == null)
            stats.addExperience(amount);
        else
        {
            for (int rank : stats.addExperience(amount, pool))
            {
                for (String boxId : pool.getRewardsForRank(rank))
                    stats.addRewardBox(boxId, RewardBoxInstance.Origin.LEVEL_UP);
                player.sendSystemMessage(Component.literal("Rank up! You reached rank " + rank));
            }
        }
        if (stats.getRank() > rankBefore)
            PacketHandler.sendTo(new PacketRankUp(rankBefore, stats.getRank(), stats.getPrestige()), player);
        markDirty();
    }

    public boolean grantRewardBox(ServerPlayer player, String boxId, RewardBoxInstance.Origin origin)
    {
        RewardBox box = RewardBox.get(boxId);
        if (box == null)
            return false;
        getStats(player).addRewardBox(box.getShortName(), origin);
        markDirty();
        return true;
    }

    @Nullable
    public RewardBox.Reward openRewardBox(ServerPlayer player, UUID instanceId)
    {
        PlayerStats stats = getStats(player);

        RewardBoxInstance rewardBoxInstance = stats.getRewardBox(instanceId).orElse(null);
        if (rewardBoxInstance == null || rewardBoxInstance.isOpened())
            return null;

        RewardBox box = RewardBox.get(rewardBoxInstance.boxId());
        if (box == null)
            return null;

        RewardBox.Reward reward = box.choose(random, stats::ownsReward);
        if (reward == null || !stats.markRewardBoxOpened(instanceId, reward.key()))
            return null;

        markDirty();
        return reward;
    }

    public Optional<Vec3> findSpawnPoint(ServerPlayer player, boolean anyTeam)
    {
        int teamId = getCurrentRound().map(round -> round.getTeamId(PlayerData.getInstance(player).getNewTeam())).orElse(0);
        List<Vec3> choices = getSpawnPositions(teamId, anyTeam);
        return choices.isEmpty() ? Optional.empty() : Optional.of(choices.get(random.nextInt(choices.size())));
    }

    /**
     * Every place a member of a round team may come back: the player spawners of the bases it may use or, when the map
     * has none of those, the bases themselves.
     *
     * @param anyTeam
     *            whether the bases of every team count
     */
    public List<Vec3> getSpawnPositions(int teamId, boolean anyTeam)
    {
        TeamsRound round = getCurrentRound().orElse(null);
        TeamsMap map = round == null ? null : getMap(round.getMapId()).orElse(null);
        if (map == null)
            return List.of();

        Predicate<ITeamBase> usable = anyTeam ? base -> true : spawnBases(round, map, teamId);

        List<Vec3> choices = liveObjects.values().stream().filter(ITeamObject::isSpawnPoint).filter(object -> object.getDimension().equals(map.getDimension())).filter(object ->
        {
            ITeamBase base = object.getBaseId() == null ? null : liveBases.get(object.getBaseId());
            return base != null && base.getMapId().equals(map.getShortName()) && usable.test(base);
        }).map(object -> object.getTeamObjectPosition().add(0D, 0.1D, 0D)).toList();

        if (!choices.isEmpty())
            return choices;

        return liveBases.values().stream().filter(base -> base.getMapId().equals(map.getShortName())).filter(usable).map(base -> base.getTeamObjectPosition().add(0D, 1D, 0D)).toList();
    }

    /** The loaded spawners connected to the bases of the map being played, as the API reports them. */
    public List<TeamsSpawner> getSpawners()
    {
        TeamsRound round = getCurrentRound().orElse(null);
        if (round == null)
            return List.of();
        List<TeamsSpawner> spawners = new ArrayList<>();
        for (ITeamObject object : liveObjects.values())
        {
            ITeamBase base = object.getBaseId() == null ? null : liveBases.get(object.getBaseId());
            if (!(object instanceof TeamSpawnerBlockEntity spawner) || base == null || !isBaseInCurrentMap(base))
                continue;
            TeamsSpawner.Kind kind = switch (spawner.getMode())
            {
                case PLAYER -> TeamsSpawner.Kind.PLAYER;
                case ITEM -> TeamsSpawner.Kind.ITEM;
                case VEHICLE -> TeamsSpawner.Kind.VEHICLE;
            };
            spawners.add(new TeamsSpawner(spawner.getObjectId(), kind, spawner.getDimension(), spawner.getBlockPos(), base.getObjectId(), getTeamForBase(base)));
        }
        return List.copyOf(spawners);
    }

    /**
     * The bases of a map a team may come back at.
     *
     * <p>
     * Those are its own, and the capture points its side holds, since allies take those together.
     * With {@link #alliedSpawnSharing} on, the bases of its allies count as well. A team that was
     * given no base of its own on the map shares those of its allies whatever the setting, so an
     * alliance can be played on a map that only has one headquarters per side.
     * </p>
     */
    private Predicate<ITeamBase> spawnBases(TeamsRound round, TeamsMap map, int teamId)
    {
        int sideId = round.getSideId(teamId);
        boolean hasOwnBase = liveBases.values().stream().anyMatch(base -> !(base instanceof ITeamCapturePoint) && base.getMapId().equals(map.getShortName()) && base.getOwnerId() == teamId);
        if (alliedSpawnSharing || !hasOwnBase)
            return base -> base.getOwnerId() >= TeamsRound.FIRST_TEAM_ID && round.getSideId(base.getOwnerId()) == sideId;
        return base -> base.getOwnerId() == teamId || base instanceof ITeamCapturePoint && base.getOwnerId() == sideId;
    }

    /** Where the player spawners connected to a base put players. Empty for a base without any. */
    public List<Vec3> getSpawnPositions(ITeamBase base)
    {
        return liveObjects.values().stream().filter(ITeamObject::isSpawnPoint).filter(object -> base.getObjectId().equals(object.getBaseId()))
            .filter(object -> object.getDimension().equals(base.getDimension())).map(object -> object.getTeamObjectPosition().add(0D, 0.1D, 0D)).toList();
    }

    /**
     * A spawn at one of the bases a team holds that is not a capture point: its headquarters in
     * the objective game types. Falls back to the base itself when it has no player spawner.
     */
    public Optional<Vec3> findHeadquartersSpawn(int teamId)
    {
        TeamsRound round = getCurrentRound().orElse(null);
        TeamsMap map = round == null ? null : getMap(round.getMapId()).orElse(null);
        if (map == null)
            return Optional.empty();
        List<ITeamBase> bases = liveBases.values().stream().filter(base -> !(base instanceof ITeamCapturePoint)).filter(base -> base.getMapId().equals(map.getShortName()))
            .filter(spawnBases(round, map, teamId)).toList();
        List<Vec3> spawns = bases.stream().flatMap(base -> getSpawnPositions(base).stream()).toList();
        if (!spawns.isEmpty())
            return Optional.of(spawns.get(random.nextInt(spawns.size())));
        return bases.isEmpty() ? Optional.empty() : Optional.of(bases.get(random.nextInt(bases.size())).getTeamObjectPosition().add(0D, 1D, 0D));
    }

    /** The loaded capture points of the map being played, in play order: by letter, then by id. */
    public List<ITeamCapturePoint> getCapturePoints()
    {
        String mapId = getCurrentRound().map(TeamsRound::getMapId).orElse(null);
        if (mapId == null)
            return List.of();
        return liveBases.values().stream().filter(ITeamCapturePoint.class::isInstance).map(ITeamCapturePoint.class::cast).filter(point -> mapId.equals(point.getMapId()))
            .sorted(Comparator.comparingInt(ITeamCapturePoint::getSymbol).thenComparing(ITeamCapturePoint::getObjectId)).toList();
    }

    /** The level the map being played is in, or null when no round is set up or its dimension is gone. */
    @Nullable
    public ServerLevel getCurrentLevel()
    {
        TeamsMap map = getCurrentRound().flatMap(round -> getMap(round.getMapId())).orElse(null);
        return map == null || server == null ? null : server.getLevel(map.getDimension());
    }

    /** Plays a content-pack sound at a team object for everyone in range; silent when no pack provides it. */
    public void playSoundAt(ITeamObject object, String sound, double range)
    {
        String name = ResourceUtils.sanitize(sound);
        if (FlansMod.getSoundEvent(name).isEmpty())
            return;
        Vec3 position = object.getTeamObjectPosition();
        PacketHandler.sendToAllAround(new PacketPlaySound(position, range, name, false, false, null), position, range, object.getDimension());
    }

    /** Sends the capture points of the running round to everyone, between the full state updates. */
    public void syncObjectives()
    {
        if (server == null || server.getPlayerList().getPlayers().isEmpty())
            return;
        getCurrentGameType().ifPresent(type -> PacketHandler.sendToAll(new PacketTeamsObjectives(type.createObjectives(this))));
    }

    public Team getPlayerTeam(Player player)
    {
        return PlayerData.getInstance(player).getTeam();
    }

    public int getRoundTeamIndex(@Nullable Team team)
    {
        return getCurrentRound().map(round -> round.getTeamIds().indexOf(team == null ? "" : team.getShortName())).orElse(-1);
    }

    public List<ServerPlayer> getPlayersOnRoundTeam(int index)
    {
        return getCurrentRound().map(round -> getPlayersOnTeam(round.getTeam(index))).orElse(List.of());
    }

    public int countPlayersOnRoundTeam(int index)
    {
        return getPlayersOnRoundTeam(index).size();
    }

    public List<ServerPlayer> getPlayersOnTeam(@Nullable Team team)
    {
        return server == null || team == null ? List.of() : server.getPlayerList().getPlayers().stream().filter(player -> team.equals(getPlayerTeam(player))).toList();
    }

    /**
     * Whether two teams are on the same side: the same team, or allies in the round being played.
     * Spectators and players without a team are on nobody's side.
     */
    public boolean areAllied(@Nullable Team first, @Nullable Team second)
    {
        if (first == null || second == null || first == Team.SPECTATORS || second == Team.SPECTATORS)
            return false;
        return first == second || getCurrentRound().map(round -> round.areAllied(first, second)).orElse(false);
    }

    /** The score of the side a team fights for, which all its allies share. */
    public int getTeamScore(@Nullable Team team)
    {
        return team == null ? 0 : teamScores.getOrDefault(scoreKey(team), 0);
    }

    /** Adds to the score of the side a team fights for. */
    public void addTeamScore(Team team, int amount)
    {
        teamScores.merge(scoreKey(team), amount, Integer::sum);
        saveRuntime();
    }

    /** Allies share one score, kept under the first team of their side. */
    private String scoreKey(Team team)
    {
        return getCurrentRound().map(round -> round.getSideLeader(round.getSideId(team))).orElse(team).getShortName();
    }

    public void resetScores()
    {
        teamScores.clear();
        getCurrentRound().ifPresent(round -> round.getTeamIds().forEach(id -> teamScores.put(id, 0)));
    }

    public PlayerStats getStats(ServerPlayer player)
    {
        ensureData();
        PlayerStats stats = Objects.requireNonNull(savedData).stats.computeIfAbsent(player.getUUID(), id -> new PlayerStats(id, player.getScoreboardName()));
        stats.setLastKnownName(player.getScoreboardName());
        return stats;
    }

    public Collection<PlayerStats> getAllStats()
    {
        return savedData == null ? List.of() : savedData.getStats();
    }

    public void registerBase(ITeamBase base)
    {
        liveBases.put(base.getObjectId(), base);
        liveObjects.put(base.getObjectId(), base);
        getMap(base.getMapId()).ifPresent(map ->
        {
            map.addBase(base.getObjectId(), BlockPos.containing(base.getTeamObjectPosition()));
            map.markCapturePoint(base.getObjectId(), base instanceof ITeamCapturePoint);
            markDirty();
        });
        if (roundRunning && isBaseInCurrentMap(base))
            base.startRound();
    }

    public void unregisterBase(UUID id)
    {
        liveBases.remove(id);
        liveObjects.remove(id);
    }

    public void registerObject(ITeamObject object)
    {
        liveObjects.put(object.getObjectId(), object);
    }

    public void unregisterObject(UUID id)
    {
        liveObjects.remove(id);
    }

    public Optional<ITeamBase> getBase(UUID id)
    {
        return Optional.ofNullable(liveBases.get(id));
    }

    public Optional<ITeamObject> getObject(UUID id)
    {
        return Optional.ofNullable(liveObjects.get(id));
    }

    public void assignBaseToMap(ITeamBase base, TeamsMap map)
    {
        getMap(base.getMapId()).ifPresent(old -> old.removeBase(base.getObjectId()));
        base.setMapId(map.getShortName());
        map.addBase(base.getObjectId(), BlockPos.containing(base.getTeamObjectPosition()));
        map.markCapturePoint(base.getObjectId(), base instanceof ITeamCapturePoint);
        markDirty();
    }

    public void connectObject(ITeamBase base, ITeamObject object)
    {
        UUID oldBase = object.getBaseId();
        if (oldBase != null)
            getBase(oldBase).ifPresent(previous -> previous.removeObject(object.getObjectId()));
        object.setBaseId(base.getObjectId());
        base.addObject(object.getObjectId());
        markDirty();
    }

    public void destroyObject(ITeamObject object)
    {
        if (object instanceof ITeamBase base)
            releaseBase(base);
        else
        {
            UUID baseId = object.getBaseId();
            if (baseId != null)
                getBase(baseId).ifPresent(base -> base.removeObject(object.getObjectId()));
            unregisterObject(object.getObjectId());
        }
        object.destroyTeamObject();
        markDirty();
    }

    /**
     * Forgets a base that is going away for good: its map drops it, its chunk ticket is released
     * and the objects connected to it are left without a base. Does nothing for a base already
     * released, so removing the block or entity afterwards is harmless.
     */
    public void releaseBase(ITeamBase base)
    {
        if (!liveBases.containsKey(base.getObjectId()))
            return;
        TeamsMap map = getMap(base.getMapId()).orElse(null);
        if (map != null)
        {
            if (isBaseInCurrentMap(base) && server != null)
            {
                ServerLevel level = server.getLevel(map.getDimension());
                if (level != null)
                {
                    ChunkPos chunk = new ChunkPos(BlockPos.containing(base.getTeamObjectPosition()));
                    ChunkTicketPlatform.force(level, base.getObjectId(), chunk.x, chunk.z, false, true);
                }
            }
            map.removeBase(base.getObjectId());
        }
        for (UUID childId : List.copyOf(base.getObjectIds()))
            getObject(childId).ifPresent(child -> child.setBaseId(null));
        unregisterBase(base.getObjectId());
        markDirty();
    }

    public boolean isBaseInCurrentMap(ITeamBase base)
    {
        return getCurrentRound().map(round -> round.getMapId().equals(base.getMapId())).orElse(false);
    }

    @Nullable
    public Team getTeamForBase(@Nullable ITeamBase base)
    {
        return base == null ? null : getCurrentRound().map(round -> round.getTeam(base.getOwnerId() - 2)).orElse(null);
    }

    @Nullable
    public Flag getFlagCarriedBy(Entity carrier)
    {
        return liveBases.values().stream().map(ITeamBase::getFlag).filter(java.util.Objects::nonNull).filter(flag -> flag.isCarriedBy(carrier)).findFirst().orElse(null);
    }

    /** The flags of the bases of the map being played. */
    public List<Flag> getFlags()
    {
        return liveBases.values().stream().filter(this::isBaseInCurrentMap).map(ITeamBase::getFlag).filter(java.util.Objects::nonNull).toList();
    }

    private void dropFlag(Entity carrier)
    {
        Flag flag = getFlagCarriedBy(carrier);
        if (flag != null)
        {
            int returnTicks = getCurrentGameType().filter(GameTypeCTF.class::isInstance).map(GameTypeCTF.class::cast).map(GameTypeCTF::getFlagReturnTimeSeconds).orElse(30) * 20;
            flag.drop(returnTicks);
        }
    }

    public void broadcast(Component message)
    {
        if (server != null)
            server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /**
     * Tells every client which player class each player wears, so that classes with a
     * SkinOverride can be drawn on them. Only sent when the assignment actually changed.
     */
    public void syncPlayerClassSkins(boolean force)
    {
        if (server == null)
            return;
        Map<UUID, PlayerClassSkin> current = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            PlayerData data = PlayerData.getInstance(player);
            PlayerClass playerClass = data.getPlayerClass();
            if (playerClass != null && data.getPlayerClassSkin() >= 0)
            {
                var variant = playerClass.getSkinOverrideVariants().get(data.getPlayerClassSkin());
                var texture = variant.texture() == null ? InfoType.loadSkinTexture(variant.name(), playerClass) : variant.texture();
                current.put(player.getUUID(), new PlayerClassSkin(playerClass.getShortName(), data.getPlayerClassSkin(), texture.toString()));
            }
        }
        if (!force && current.equals(lastSyncedPlayerClassSkins))
            return;
        lastSyncedPlayerClassSkins = current;
        PacketHandler.sendToAll(new PacketPlayerClassSkins(current));
    }

    public void syncPlayer(ServerPlayer player, PacketTeamsState.OpenScreen openScreen)
    {
        if (server != null && savedData != null)
            PacketHandler.sendTo(PacketTeamsState.create(this, player, openScreen), player);
    }

    public void syncLoadouts(ServerPlayer player, PacketLoadoutState.OpenScreen openScreen, int editLoadout, String revealedReward)
    {
        if (server != null && savedData != null)
        {
            PacketHandler.sendTo(PacketLoadoutState.create(this, player, openScreen, editLoadout, revealedReward), player);
            markDirty();
        }
    }

    public void markPlayerDataDirty()
    {
        markDirty();
    }

    public void syncAll(PacketTeamsState.OpenScreen openScreen)
    {
        if (server == null || savedData == null || server.getPlayerList().getPlayers().isEmpty())
            return;
        PacketTeamsState.SharedScoreboard shared = PacketTeamsState.createSharedScoreboard(this);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            PacketHandler.sendTo(PacketTeamsState.create(this, player, openScreen, shared), player);
    }

    private void updateActiveChunkTickets(boolean add)
    {
        if (server == null)
            return;
        TeamsMap map = getCurrentRound().flatMap(round -> getMap(round.getMapId())).orElse(null);
        if (map == null)
            return;
        ServerLevel level = server.getLevel(map.getDimension());
        if (level == null)
            return;
        map.getBasePositions().forEach((owner, position) ->
        {
            ChunkPos chunk = new ChunkPos(position);
            ChunkTicketPlatform.force(level, owner, chunk.x, chunk.z, add, true);
        });
    }

    public void applyArenaPreset()
    {
        explosionsBreakBlocks = driveablesBreakBlocks = vehiclesNeedFuel = armourDrops = false;
        bombsEnabled = shellsEnabled = bulletsEnabled = forceAdventureMode = overrideHunger = canBreakGuns = true;
        canBreakGlass = false;
        weaponDrops = EnumWeaponDrop.SMART_DROPS;
        mgLife = planeLife = vehicleLife = mechaLife = aaLife = 120;
        saveRuntime();
    }

    public void applySurvivalPreset()
    {
        explosionsBreakBlocks = driveablesBreakBlocks = bombsEnabled = shellsEnabled = bulletsEnabled = canBreakGuns = canBreakGlass = true;
        survivalCanBreakVehicles = survivalCanPlaceVehicles = armourDrops = vehiclesNeedFuel = true;
        forceAdventureMode = overrideHunger = false;
        weaponDrops = EnumWeaponDrop.DROPS;
        mgLife = planeLife = vehicleLife = mechaLife = aaLife = 0;
        saveRuntime();
    }

    private void ensureData()
    {
        if (savedData == null)
            throw new IllegalStateException("Teams data is unavailable before server start");
    }

    private void markDirty()
    {
        if (savedData != null)
            savedData.setDirty();
    }

    private void loadRuntime(CompoundTag tag)
    {
        if (tag.isEmpty())
            return;

        queuedRoundId = tag.hasUUID(NBT_QUEUED_ROUND) ? tag.getUUID(NBT_QUEUED_ROUND) : null;
        roundInstanceId = tag.hasUUID(NBT_ROUND_INSTANCE) ? tag.getUUID(NBT_ROUND_INSTANCE) : null;
        lastWinnerId = tag.getString(NBT_WINNER);
        lastRoundDecided = tag.getBoolean(NBT_ROUND_DECIDED);
        pacifism = tag.getBoolean(NBT_PACIFISM);
        movieProp = tag.getBoolean(NBT_MOVIE_PROP);
        alliedSpawnSharing = tag.getBoolean(NBT_ALLIED_SPAWN_SHARING);
        seaLevelOverride = tag.contains(NBT_SEA_LEVEL) ? tag.getInt(NBT_SEA_LEVEL) : NO_SEA_LEVEL_OVERRIDE;
        enabled = !tag.contains(NBT_ENABLED) || tag.getBoolean(NBT_ENABLED);
        roundRunning = tag.getBoolean(NBT_ROUND_RUNNING);
        currentRoundId = tag.hasUUID(NBT_CURRENT_ROUND) ? tag.getUUID(NBT_CURRENT_ROUND) : null;
        rotationIndex = tag.getInt(NBT_ROTATION_INDEX);
        roundTimeLeftTicks = tag.getInt(NBT_TIME_LEFT);
        roundElapsedTicks = tag.getInt(NBT_ELAPSED);
        intermissionTicks = tag.getInt(NBT_INTERMISSION);
        intermissionPhase = readIntermissionPhase(tag);
        scoreDisplayTimeTicks = tag.contains(NBT_SCORE_DISPLAY_TIME) ? Math.max(0, tag.getInt(NBT_SCORE_DISPLAY_TIME)) : DEFAULT_INTERMISSION_PHASE_TICKS;
        rankUpdateTimeTicks = tag.contains(NBT_RANK_UPDATE_TIME) ? Math.max(0, tag.getInt(NBT_RANK_UPDATE_TIME)) : DEFAULT_INTERMISSION_PHASE_TICKS;
        votingTimeTicks = tag.contains(NBT_VOTING_TIME) ? Math.max(0, tag.getInt(NBT_VOTING_TIME)) : DEFAULT_INTERMISSION_PHASE_TICKS;
        motd = tag.contains(NBT_MOTD) ? tag.getString(NBT_MOTD) : DEFAULT_MOTD;
        autoBalanceIntervalTicks = tag.contains(NBT_AUTO_BALANCE_INTERVAL) ? Math.max(AUTO_BALANCE_WARNING_TICKS + 20, tag.getInt(NBT_AUTO_BALANCE_INTERVAL)) : DEFAULT_AUTO_BALANCE_INTERVAL_TICKS;
        voteOptionIds.clear();

        for (Tag value : tag.getList(NBT_VOTE_OPTIONS, Tag.TAG_COMPOUND))
        {
            CompoundTag option = (CompoundTag) value;
            if (option.hasUUID(NBT_ID))
                voteOptionIds.add(option.getUUID(NBT_ID));
        }

        if (tag.contains(NBT_EXPLOSIONS))
            explosionsBreakBlocks = tag.getBoolean(NBT_EXPLOSIONS);
        if (tag.contains(NBT_BREAK_GLASS))
            canBreakGlass = tag.getBoolean(NBT_BREAK_GLASS);
        if (tag.contains(NBT_BREAK_GUNS))
            canBreakGuns = tag.getBoolean(NBT_BREAK_GUNS);
        if (tag.contains(NBT_DRIVEABLES_BREAK_BLOCKS))
            driveablesBreakBlocks = tag.getBoolean(NBT_DRIVEABLES_BREAK_BLOCKS);
        if (tag.contains(NBT_BOMBS))
            bombsEnabled = tag.getBoolean(NBT_BOMBS);
        if (tag.contains(NBT_SHELLS))
            shellsEnabled = tag.getBoolean(NBT_SHELLS);
        if (tag.contains(NBT_BULLETS))
            bulletsEnabled = tag.getBoolean(NBT_BULLETS);
        if (tag.contains(NBT_ADVENTURE))
            forceAdventureMode = tag.getBoolean(NBT_ADVENTURE);
        if (tag.contains(NBT_ARMOUR_DROPS))
            armourDrops = tag.getBoolean(NBT_ARMOUR_DROPS);
        if (tag.contains(NBT_FUEL))
            vehiclesNeedFuel = tag.getBoolean(NBT_FUEL);
        vehiclesCanZoom = tag.contains(NBT_VEHICLES_CAN_ZOOM) && tag.getBoolean(NBT_VEHICLES_CAN_ZOOM);
        if (tag.contains(NBT_OVERRIDE_HUNGER))
            overrideHunger = tag.getBoolean(NBT_OVERRIDE_HUNGER);
        if (tag.contains(NBT_BREAK_VEHICLES))
            survivalCanBreakVehicles = tag.getBoolean(NBT_BREAK_VEHICLES);
        if (tag.contains(NBT_PLACE_VEHICLES))
            survivalCanPlaceVehicles = tag.getBoolean(NBT_PLACE_VEHICLES);
        if (tag.contains(NBT_WEAPON_DROPS))
        {
            int ordinal = tag.getInt(NBT_WEAPON_DROPS);
            weaponDrops = EnumWeaponDrop.values()[Math.max(0, Math.min(EnumWeaponDrop.values().length - 1, ordinal))];
        }

        mgLife = tag.contains(NBT_MG_LIFE) ? tag.getInt(NBT_MG_LIFE) : mgLife;
        planeLife = tag.contains(NBT_PLANE_LIFE) ? tag.getInt(NBT_PLANE_LIFE) : planeLife;
        vehicleLife = tag.contains(NBT_VEHICLE_LIFE) ? tag.getInt(NBT_VEHICLE_LIFE) : vehicleLife;
        mechaLife = tag.contains(NBT_MECHA_LIFE) ? tag.getInt(NBT_MECHA_LIFE) : mechaLife;
        aaLife = tag.contains(NBT_AA_LIFE) ? tag.getInt(NBT_AA_LIFE) : aaLife;
        voting = tag.contains(NBT_VOTING) && tag.getBoolean(NBT_VOTING);
        roundsGenerator = tag.contains(NBT_ROUNDS_GENERATOR) && tag.getBoolean(NBT_ROUNDS_GENERATOR);
        currentLoadoutPoolId = tag.getString(NBT_LOADOUT_POOL);
        experienceMultiplier = tag.contains(NBT_EXPERIENCE_MULTIPLIER) ? Math.max(0F, tag.getFloat(NBT_EXPERIENCE_MULTIPLIER)) : 1F;

        for (String key : tag.getCompound(NBT_SCORES).getAllKeys())
            teamScores.put(key, tag.getCompound(NBT_SCORES).getInt(key));

        for (com.flansmodultimate.common.teams.GameType type : com.flansmodultimate.common.teams.GameType.values())
            type.loadSettings(tag);
    }

    /**
     * Reads the intermission stage, understanding worlds saved before the stage was split.
     *
     * <p>
     * Those only recorded whether voting had started, so a break in progress resumes at
     * voting or at the scoreboard rather than restarting the whole sequence.
     * </p>
     */
    private static IntermissionPhase readIntermissionPhase(CompoundTag tag)
    {
        if (tag.contains(NBT_INTERMISSION_PHASE, Tag.TAG_STRING))
        {
            String name = tag.getString(NBT_INTERMISSION_PHASE);
            for (IntermissionPhase phase : IntermissionPhase.values())
                if (phase.name().equals(name))
                    return phase;
        }
        boolean legacyVoting = tag.contains(NBT_INTERMISSION_VOTING_PHASE) ? tag.getBoolean(NBT_INTERMISSION_VOTING_PHASE) : !tag.getList(NBT_VOTE_OPTIONS, Tag.TAG_COMPOUND).isEmpty();
        if (legacyVoting)
            return IntermissionPhase.VOTING;
        return tag.getInt(NBT_INTERMISSION) > 0 ? IntermissionPhase.SCORES : IntermissionPhase.NONE;
    }

    private void saveRuntime()
    {
        if (savedData == null)
            return;

        CompoundTag tag = savedData.runtime;
        tag.putBoolean(NBT_ENABLED, enabled);
        tag.putBoolean(NBT_ROUND_RUNNING, roundRunning);

        if (currentRoundId == null)
            tag.remove(NBT_CURRENT_ROUND);
        else
            tag.putUUID(NBT_CURRENT_ROUND, currentRoundId);

        tag.putInt(NBT_ROTATION_INDEX, rotationIndex);
        if (queuedRoundId == null)
            tag.remove(NBT_QUEUED_ROUND);
        else
            tag.putUUID(NBT_QUEUED_ROUND, queuedRoundId);
        if (roundInstanceId == null)
            tag.remove(NBT_ROUND_INSTANCE);
        else
            tag.putUUID(NBT_ROUND_INSTANCE, roundInstanceId);
        tag.putString(NBT_WINNER, lastWinnerId);
        tag.putBoolean(NBT_ROUND_DECIDED, lastRoundDecided);
        tag.putBoolean(NBT_PACIFISM, pacifism);
        tag.putBoolean(NBT_MOVIE_PROP, movieProp);
        tag.putBoolean(NBT_ALLIED_SPAWN_SHARING, alliedSpawnSharing);
        if (seaLevelOverride == NO_SEA_LEVEL_OVERRIDE)
            tag.remove(NBT_SEA_LEVEL);
        else
            tag.putInt(NBT_SEA_LEVEL, seaLevelOverride);
        tag.putInt(NBT_TIME_LEFT, roundTimeLeftTicks);
        tag.putInt(NBT_ELAPSED, roundElapsedTicks);
        tag.putInt(NBT_INTERMISSION, intermissionTicks);
        tag.putString(NBT_INTERMISSION_PHASE, intermissionPhase.name());
        tag.putInt(NBT_SCORE_DISPLAY_TIME, scoreDisplayTimeTicks);
        tag.putInt(NBT_RANK_UPDATE_TIME, rankUpdateTimeTicks);
        tag.putInt(NBT_VOTING_TIME, votingTimeTicks);
        tag.putString(NBT_MOTD, motd);
        tag.putInt(NBT_AUTO_BALANCE_INTERVAL, autoBalanceIntervalTicks);
        ListTag voteOptions = new ListTag();

        for (UUID id : voteOptionIds)
        {
            CompoundTag option = new CompoundTag();
            option.putUUID(NBT_ID, id);
            voteOptions.add(option);
        }

        tag.put(NBT_VOTE_OPTIONS, voteOptions);
        tag.putBoolean(NBT_EXPLOSIONS, explosionsBreakBlocks);
        tag.putBoolean(NBT_BREAK_GLASS, canBreakGlass);
        tag.putBoolean(NBT_BREAK_GUNS, canBreakGuns);
        tag.putBoolean(NBT_DRIVEABLES_BREAK_BLOCKS, driveablesBreakBlocks);
        tag.putBoolean(NBT_BOMBS, bombsEnabled);
        tag.putBoolean(NBT_SHELLS, shellsEnabled);
        tag.putBoolean(NBT_BULLETS, bulletsEnabled);
        tag.putBoolean(NBT_ADVENTURE, forceAdventureMode);
        tag.putBoolean(NBT_ARMOUR_DROPS, armourDrops);
        tag.putBoolean(NBT_FUEL, vehiclesNeedFuel);
        tag.putBoolean(NBT_VEHICLES_CAN_ZOOM, vehiclesCanZoom);
        tag.putBoolean(NBT_OVERRIDE_HUNGER, overrideHunger);
        tag.putBoolean(NBT_BREAK_VEHICLES, survivalCanBreakVehicles);
        tag.putBoolean(NBT_PLACE_VEHICLES, survivalCanPlaceVehicles);
        tag.putInt(NBT_WEAPON_DROPS, weaponDrops.ordinal());
        tag.putInt(NBT_MG_LIFE, mgLife);
        tag.putInt(NBT_PLANE_LIFE, planeLife);
        tag.putInt(NBT_VEHICLE_LIFE, vehicleLife);
        tag.putInt(NBT_MECHA_LIFE, mechaLife);
        tag.putInt(NBT_AA_LIFE, aaLife);
        tag.putBoolean(NBT_VOTING, voting);
        tag.putBoolean(NBT_ROUNDS_GENERATOR, roundsGenerator);
        tag.putString(NBT_LOADOUT_POOL, currentLoadoutPoolId);
        tag.putFloat(NBT_EXPERIENCE_MULTIPLIER, experienceMultiplier);

        CompoundTag scores = new CompoundTag();
        teamScores.forEach(scores::putInt);
        tag.put(NBT_SCORES, scores);

        for (com.flansmodultimate.common.teams.GameType type : com.flansmodultimate.common.teams.GameType.values())
            type.saveSettings(tag);

        markDirty();
    }
}
