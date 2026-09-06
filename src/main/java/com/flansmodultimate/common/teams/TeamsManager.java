package com.flansmodultimate.common.teams;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.LoadoutPool;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.RewardBox;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.client.PacketTeamsState;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    
    public enum EnumWeaponDrop
    { 
        NONE, 
        DROPS, 
        SMART_DROPS 
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
    @Getter
    private boolean vehiclesNeedFuel = true;
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
    @Getter @Setter 
    private int bulletSnapshotMin;
    @Getter @Setter 
    private int bulletSnapshotDivisor = 50;
    @Getter 
    private boolean voting;
    @Getter @Setter 
    private boolean roundsGenerator;
    @Getter 
    private String currentLoadoutPoolId = "";
    @Getter 
    private float experienceMultiplier = 1F;

    @Nullable 
    private MinecraftServer server;
    @Nullable 
    private TeamsSavedData savedData;
    private final Map<UUID, Flagpole> liveBases = new HashMap<>();
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
    private int rotationIndex = -1;
    @Getter
    private int roundTimeLeftTicks;
    @Getter
    private int roundElapsedTicks;
    @Getter
    private int intermissionTicks;
    private final List<UUID> voteOptionIds = new ArrayList<>();

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
        savedData = server.overworld().getDataStorage().computeIfAbsent(TeamsSavedData::load, TeamsSavedData::new, TeamsSavedData.ID);
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

    public List<TeamsRound> getVoteOptions()
    {
        if (savedData == null)
            return List.of();
        return voteOptionIds.stream()
            .map(id -> savedData.rounds.stream().filter(round -> round.getId().equals(id)).findFirst().orElse(null))
            .filter(java.util.Objects::nonNull).toList();
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
        if (pool == null) return false;
        currentLoadoutPoolId = pool.getOriginalShortName();
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
        ensureData();
        TeamsMap map = new TeamsMap(id, name, level.dimension());

        if (savedData != null && savedData.maps.putIfAbsent(map.getShortName(), map) != null)
            throw new IllegalArgumentException("A map named '" + map.getShortName() + "' already exists");

        markDirty();
        return map;
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

    public TeamsRound addRound(String mapId, String gameTypeId, List<String> teamIds, int minutes, int scoreLimit)
    {
        ensureData();
        TeamsMap map = getMap(mapId).orElseThrow(() -> new IllegalArgumentException("Unknown map: " + mapId));
        com.flansmodultimate.common.teams.GameType type = Optional.ofNullable(com.flansmodultimate.common.teams.GameType.get(gameTypeId))
            .orElseThrow(() -> new IllegalArgumentException("Unknown game type: " + gameTypeId));

        if (teamIds.size() != type.getRequiredTeams())
            throw new IllegalArgumentException(type.getName() + " requires " + type.getRequiredTeams() + " team(s)");

        for (String teamId : teamIds)
            if (Team.getTeam(teamId) == null) throw new IllegalArgumentException("Unknown team: " + teamId);

        TeamsRound round = new TeamsRound(map.getShortName(), type.getId(), teamIds, minutes, scoreLimit);
        Objects.requireNonNull(savedData).rounds.add(round);
        markDirty();

        return round;
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

    public boolean startRound(int index)
    {
        ensureData();
        if (!enabled || index < 0 || index >= Objects.requireNonNull(savedData).rounds.size())
            return false;
        TeamsRound next = Objects.requireNonNull(savedData).rounds.get(index);
        if (next.getGametype() == null || getMap(next.getMapId()).isEmpty())
            return false;

        getCurrentGameType().ifPresent(type -> type.roundEnded(this));
        updateActiveChunkTickets(false);
        liveBases.values().forEach(ITeamBase::roundCleanup);
        rotationIndex = index;
        currentRoundId = next.getId();
        roundTimeLeftTicks = next.getTimeLimitTicks();
        roundElapsedTicks = 0;
        intermissionTicks = 0;
        roundRunning = true;
        voteOptionIds.clear();
        resetScores();
        savedData.rounds.forEach(round -> { if (round == next) round.markPlayed(); else round.markSkipped(); });
        liveBases.values().stream().filter(this::isBaseInCurrentMap).forEach(ITeamBase::startRound);
        updateActiveChunkTickets(true);

        for (ServerPlayer player : getServer().getPlayerList().getPlayers())
        {
            PlayerData data = PlayerData.getInstance(player);
            data.setScore(0); data.setKills(0); data.setDeaths(0); data.setZombieScore(0); data.setVote(0);
            Team selected = data.getNewTeam();
            if (getRoundTeamIndex(selected) < 0 && selected != Team.SPECTATORS)
                selectTeam(player, Team.SPECTATORS, true);
            respawnPlayer(player, false);
        }
        next.getGametype().roundStarted(this);
        broadcast(Component.literal("Starting " + next.getGametype().getName() + " on " + getMap(next.getMapId()).orElseThrow().getName()));
        saveRuntime();
        getServer().getPlayerList().getPlayers().forEach(player ->
            syncPlayer(player, getPlayerTeam(player) == null || getPlayerTeam(player) == Team.SPECTATORS
                ? PacketTeamsState.OpenScreen.TEAM_SELECT : PacketTeamsState.OpenScreen.CLOSE));
        return true;
    }

    public boolean startNextRound()
    {
        ensureData();
        if (Objects.requireNonNull(savedData).rounds.isEmpty())
            return false;
        return startRound((rotationIndex + 1) % savedData.rounds.size());
    }

    public void stopRound()
    {
        getCurrentGameType().ifPresent(type -> type.roundEnded(this));
        updateActiveChunkTickets(false);
        liveBases.values().forEach(ITeamBase::roundCleanup);
        roundRunning = false;
        currentRoundId = null;
        roundTimeLeftTicks = 0;
        roundElapsedTicks = 0;
        intermissionTicks = 0;
        voteOptionIds.clear();
        resetScores();
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
            markDirty();
        }
        if (!enabled)
            return;
        if (intermissionTicks > 0)
        {
            if (--intermissionTicks == 0)
            {
                if (voteOptionIds.isEmpty()) startNextRound(); else startVotedRound();
            }
            return;
        }
        if (!roundRunning)
            return;

        roundElapsedTicks++;
        roundTimeLeftTicks = Math.max(0, roundTimeLeftTicks - 1);
        getCurrentGameType().ifPresent(type -> type.tick(this));
        if (roundElapsedTicks % 200 == 0)
            autoBalanceIfNeeded();

        boolean winner = getCurrentRound().stream().flatMap(round -> round.getTeamIds().stream())
            .map(Team::getTeam).filter(java.util.Objects::nonNull)
            .anyMatch(team -> getCurrentGameType().map(type -> type.hasWinner(this, team)).orElse(false));
        if (winner || roundTimeLeftTicks <= 0)
            finishRound();
        else if (roundElapsedTicks % 20 == 0)
            saveRuntime();
    }

    private void finishRound()
    {
        if (!roundRunning)
            return;
        roundRunning = false;
        getCurrentGameType().ifPresent(type -> type.roundEnded(this));
        awardRoundStats();
        if (voting)
        {
            pickVoteOptions();
            broadcast(Component.literal("Round over. Vote with /teams vote <number>."));
            List<TeamsRound> options = getVoteOptions();
            for (int i = 0; i < options.size(); i++)
            {
                TeamsRound option = options.get(i);
                broadcast(Component.literal((i + 1) + ". " + option.getGameTypeId() + " @ " + option.getMapId()));
            }
            intermissionTicks = 20 * 20;
        }
        else
        {
            broadcast(Component.literal("Round over. Next round starts in 10 seconds."));
            intermissionTicks = 200;
        }
        saveRuntime();
        if (!voting && getCurrentLoadoutPool().isPresent())
            getServer().getPlayerList().getPlayers().forEach(player -> syncLoadouts(player, PacketLoadoutState.OpenScreen.MISSION_RESULTS, 0, ""));
        else
            syncAll(voting ? PacketTeamsState.OpenScreen.VOTING : PacketTeamsState.OpenScreen.SCOREBOARD);
    }

    private void pickVoteOptions()
    {
        voteOptionIds.clear();
        if (savedData == null)
            return;
        savedData.rounds.stream().filter(round -> !round.getId().equals(currentRoundId)).sorted().limit(5)
            .map(TeamsRound::getId).forEach(voteOptionIds::add);
        if (voteOptionIds.isEmpty() && currentRoundId != null)
            voteOptionIds.add(currentRoundId);
        getServer().getPlayerList().getPlayers().forEach(player -> PlayerData.getInstance(player).setVote(0));
    }

    public boolean castVote(ServerPlayer player, int option)
    {
        if (intermissionTicks <= 0 || option < 1 || option > voteOptionIds.size())
            return false;
        PlayerData.getInstance(player).setVote(option);
        syncAll(PacketTeamsState.OpenScreen.NONE);
        return true;
    }

    private void startVotedRound()
    {
        int[] votes = new int[voteOptionIds.size()];
        for (ServerPlayer player : getServer().getPlayerList().getPlayers())
        {
            int vote = PlayerData.getInstance(player).getVote();
            if (vote > 0 && vote <= votes.length)
                votes[vote - 1]++;
        }
        int winner = 0;
        for (int i = 1; i < votes.length; i++)
            if (votes[i] > votes[winner]) winner = i;
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
            if (getPlayerTeam(player) != Team.SPECTATORS) getStats(player).recordRound();
        getCurrentRound().ifPresent(round -> {
            for (String id : round.getTeamIds())
            {
                getPlayersOnTeam(Team.getTeam(id)).stream()
                    .max(Comparator.comparingInt(player -> PlayerData.getInstance(player).getScore()))
                    .ifPresent(player -> { getStats(player).recordMvp(); awardExperience(player, 250); });
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
        if (!force && team != Team.SPECTATORS && wouldUnbalance(team))
            return false;
        PlayerData data = PlayerData.getInstance(player);
        data.setBuilder(false);
        data.setNewTeam(team);
        if (force || !player.isAlive())
            data.setTeam(team);
        if (team == Team.SPECTATORS)
        {
            data.setNewPlayerClass(null);
            data.setPlayerClass(null);
        }
        getStats(player).setSelection(team.getOriginalShortName(), data.getNewPlayerClass() == null ? "" : data.getNewPlayerClass().getOriginalShortName());
        markDirty();
        return true;
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
        if (playerClass == null || team == null || team == Team.SPECTATORS || !team.getClasses().contains(playerClass))
            return false;
        if (getStats(player).getRank() < playerClass.getUnlockLevel())
            return false;
        data.setNewPlayerClass(playerClass);
        getStats(player).setSelection(team.getOriginalShortName(), playerClass.getOriginalShortName());
        markDirty();
        return true;
    }

    private boolean wouldUnbalance(Team requested)
    {
        Optional<com.flansmodultimate.common.teams.GameType> type = getCurrentGameType();
        if (type.isEmpty() || !type.get().isAutoBalanceEnabled())
            return false;
        int requestedCount = getPlayersOnTeam(requested).size();
        int minimum = getCurrentRound().stream().flatMap(round -> round.getTeamIds().stream())
            .map(Team::getTeam).filter(java.util.Objects::nonNull).mapToInt(team -> getPlayersOnTeam(team).size()).min().orElse(0);
        return requestedCount > minimum;
    }

    private void autoBalanceIfNeeded()
    {
        if (getCurrentGameType().map(type -> !type.isAutoBalanceEnabled()).orElse(true))
            return;

        List<Team> teams = getCurrentRound().stream().flatMap(round -> round.getTeamIds().stream())
            .map(Team::getTeam).filter(java.util.Objects::nonNull).toList();

        if (teams.size() < 2)
            return;

        Team largest = teams.stream().max(Comparator.comparingInt(team -> getPlayersOnTeam(team).size())).orElse(null);
        Team smallest = teams.stream().min(Comparator.comparingInt(team -> getPlayersOnTeam(team).size())).orElse(null);

        if (getPlayersOnTeam(largest).size() - getPlayersOnTeam(smallest).size() <= 1)
            return;

        getPlayersOnTeam(largest).stream().min(Comparator.comparingInt(player -> PlayerData.getInstance(player).getScore())).ifPresent(player -> {
            selectTeam(player, smallest, true);
            PlayerData.getInstance(player).setPlayerMovedByAutobalancer(true);
            respawnPlayer(player, false);
            player.sendSystemMessage(Component.literal("You were moved to balance the teams"));
        });
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
        if (roundRunning && (data.getTeam() == null || data.getTeam() == Team.SPECTATORS) && getCurrentLoadoutPool().isPresent())
            syncLoadouts(player, PacketLoadoutState.OpenScreen.HUB, 0, "");
        else
            syncPlayer(player, roundRunning && (data.getTeam() == null || data.getTeam() == Team.SPECTATORS)
                ? PacketTeamsState.OpenScreen.TEAM_SELECT : PacketTeamsState.OpenScreen.NONE);
    }

    public void playerLoggedOut(ServerPlayer player)
    {
        dropFlag(player);
        PlayerData.removeServerData(player.getUUID());
        markDirty();
    }

    public void playerDied(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source)
    {
        getCurrentGameType().ifPresent(type -> type.playerKilled(this, player, source));
        dropFlag(player);
    }

    public void respawnPlayer(ServerPlayer player, boolean immediate)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.applyPendingTeamSelection();
        if (!roundRunning || data.getTeam() == null)
            return;
        if (forceAdventureMode)
            player.setGameMode(data.getTeam() == Team.SPECTATORS ? GameType.SPECTATOR : GameType.ADVENTURE);
        else if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && data.getTeam() != Team.SPECTATORS)
            player.setGameMode(GameType.SURVIVAL);
        if (data.getTeam() != Team.SPECTATORS)
            applyLoadout(player);
        getCurrentGameType().map(type -> type.getSpawnPoint(this, player)).ifPresent(position -> {
            TeamsMap map = getCurrentRound().flatMap(round -> getMap(round.getMapId())).orElse(null);
            ServerLevel level = map == null ? null : getServer().getLevel(map.getDimension());
            if (level != null)
                player.teleportTo(level, position.x, position.y, position.z, player.getYRot(), player.getXRot());
        });
        if (immediate)
            player.setHealth(player.getMaxHealth());
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
            if (stack.isEmpty())
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
                        if (!player.getInventory().add(extra.copy())) player.drop(extra.copy(), false);
                }
            }
        }
        else if (playerClass != null)
        {
            for (ItemStack stack : playerClass.createStartingItems())
                if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
        }
        player.getInventory().setChanged();
    }

    public void awardExperience(ServerPlayer player, int baseAmount)
    {
        if (baseAmount <= 0)
            return;

        PlayerStats stats = getStats(player);
        LoadoutPool pool = getCurrentLoadoutPool().orElse(null);
        int amount = Math.round(baseAmount * experienceMultiplier);
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
        markDirty();
    }

    public boolean grantRewardBox(ServerPlayer player, String boxId, RewardBoxInstance.Origin origin)
    {
        RewardBox box = RewardBox.get(boxId);
        if (box == null) return false;
        getStats(player).addRewardBox(box.getOriginalShortName(), origin);
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
        TeamsRound round = getCurrentRound().orElse(null);
        TeamsMap map = round == null ? null : getMap(round.getMapId()).orElse(null);
        if (map == null)
            return Optional.empty();

        int teamId = round.getTeamId(PlayerData.getInstance(player).getNewTeam());

        List<ITeamObject> choices = liveObjects.values().stream()
            .filter(ITeamObject::isSpawnPoint)
            .filter(object -> object.getDimension().equals(map.getDimension()))
            .filter(object -> {
                Flagpole base = object.getBaseId() == null ? null : liveBases.get(object.getBaseId());
                return base != null && base.getMapId().equals(map.getShortName()) && (anyTeam || base.getOwnerId() == teamId);
            }).toList();

        if (!choices.isEmpty())
            return Optional.of(choices.get(random.nextInt(choices.size())).getTeamObjectPosition().add(0D, 0.1D, 0D));

        List<Flagpole> bases = liveBases.values().stream().filter(base -> base.getMapId().equals(map.getShortName()))
            .filter(base -> anyTeam || base.getOwnerId() == teamId).toList();

        return bases.isEmpty() ? Optional.empty() : Optional.of(bases.get(random.nextInt(bases.size())).position().add(0D, 1D, 0D));
    }

    public Team getPlayerTeam(Player player)
    {
        return PlayerData.getInstance(player).getTeam();
    }

    public int getRoundTeamIndex(@Nullable Team team) {
        return getCurrentRound().map(round -> round.getTeamIds().indexOf(team == null ? "" : team.getOriginalShortName())).orElse(-1);
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

    public int getTeamScore(@Nullable Team team)
    {
        return team == null ? 0 : teamScores.getOrDefault(team.getOriginalShortName(), 0);
    }

    public void addTeamScore(Team team, int amount)
    {
        teamScores.merge(team.getOriginalShortName(), amount, Integer::sum);
        saveRuntime();
    }

    public void resetScores()
    {
        teamScores.clear(); getCurrentRound().ifPresent(round -> round.getTeamIds().forEach(id -> teamScores.put(id, 0)));
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

    public void registerBase(Flagpole base)
    {
        liveBases.put(base.getUUID(), base);
        liveObjects.put(base.getUUID(), base);
        getMap(base.getMapId()).ifPresent(map -> { map.addBase(base.getUUID(), BlockPos.containing(base.position())); markDirty(); });
        if (roundRunning && isBaseInCurrentMap(base))
            base.startRound();
    }

    public void unregisterBase(UUID id)
    {
        liveBases.remove(id);
        liveObjects.remove(id);
    }

    public void registerObject(ITeamObject object) { liveObjects.put(object.getObjectId(), object); }

    public void unregisterObject(UUID id) { liveObjects.remove(id); }

    public Optional<Flagpole> getBase(UUID id) { return Optional.ofNullable(liveBases.get(id)); }

    public Optional<ITeamObject> getObject(UUID id) { return Optional.ofNullable(liveObjects.get(id)); }

    public void assignBaseToMap(ITeamBase base, TeamsMap map)
    {
        getMap(base.getMapId()).ifPresent(old -> old.removeBase(base.getObjectId()));
        base.setMapId(map.getShortName());
        map.addBase(base.getObjectId(), BlockPos.containing(base.getTeamObjectPosition()));
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
        {
            TeamsMap map = getMap(base.getMapId()).orElse(null);
            if (map != null)
            {
                if (isBaseInCurrentMap(base) && server != null)
                {
                    ServerLevel level = server.getLevel(map.getDimension());
                    if (level != null)
                    {
                        ChunkPos chunk = new ChunkPos(BlockPos.containing(base.getTeamObjectPosition()));
                        ForgeChunkManager.forceChunk(level, FlansMod.MOD_ID, base.getObjectId(), chunk.x, chunk.z, false, true);
                    }
                }
                map.removeBase(base.getObjectId());
            }
            for (UUID childId : List.copyOf(base.getObjectIds()))
                getObject(childId).ifPresent(child -> child.setBaseId(null));
            unregisterBase(base.getObjectId());
        }
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
    public Flag getFlagCarriedBy(ServerPlayer player)
    {
        return liveBases.values().stream().map(Flagpole::getFlag).filter(java.util.Objects::nonNull)
            .filter(flag -> flag.isCarriedBy(player)).findFirst().orElse(null);
    }

    private void dropFlag(ServerPlayer player)
    {
        Flag flag = getFlagCarriedBy(player);
        if (flag != null)
        {
            int returnTicks = getCurrentGameType().filter(GameTypeCTF.class::isInstance).map(GameTypeCTF.class::cast)
                .map(GameTypeCTF::getFlagReturnTimeSeconds).orElse(30) * 20;
            flag.drop(returnTicks);
        }
    }

    public void broadcast(Component message)
    {
        if (server != null)
            server.getPlayerList().broadcastSystemMessage(message, false);
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

    public void markPlayerDataDirty() { markDirty(); }

    public void syncAll(PacketTeamsState.OpenScreen openScreen)
    {
        if (server != null && savedData != null)
            server.getPlayerList().getPlayers().forEach(player -> syncPlayer(player, openScreen));
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
        map.getBasePositions().forEach((owner, position) -> {
            ChunkPos chunk = new ChunkPos(position);
            ForgeChunkManager.forceChunk(level, FlansMod.MOD_ID, owner, chunk.x, chunk.z, add, true);
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

        enabled = !tag.contains(NBT_ENABLED) || tag.getBoolean(NBT_ENABLED);
        roundRunning = tag.getBoolean(NBT_ROUND_RUNNING);
        currentRoundId = tag.hasUUID(NBT_CURRENT_ROUND) ? tag.getUUID(NBT_CURRENT_ROUND) : null;
        rotationIndex = tag.getInt(NBT_ROTATION_INDEX);
        roundTimeLeftTicks = tag.getInt(NBT_TIME_LEFT);
        roundElapsedTicks = tag.getInt(NBT_ELAPSED);
        intermissionTicks = tag.getInt(NBT_INTERMISSION);
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

        mgLife = tag.contains(NBT_MG_LIFE) ? tag.getInt(NBT_MG_LIFE) : mgLife; planeLife = tag.contains(NBT_PLANE_LIFE) ? tag.getInt(NBT_PLANE_LIFE) : planeLife;
        vehicleLife = tag.contains(NBT_VEHICLE_LIFE) ? tag.getInt(NBT_VEHICLE_LIFE) : vehicleLife; mechaLife = tag.contains(NBT_MECHA_LIFE) ? tag.getInt(NBT_MECHA_LIFE) : mechaLife;
        aaLife = tag.contains(NBT_AA_LIFE) ? tag.getInt(NBT_AA_LIFE) : aaLife; voting = tag.contains(NBT_VOTING) && tag.getBoolean(NBT_VOTING);
        roundsGenerator = tag.contains(NBT_ROUNDS_GENERATOR) && tag.getBoolean(NBT_ROUNDS_GENERATOR); currentLoadoutPoolId = tag.getString(NBT_LOADOUT_POOL);
        experienceMultiplier = tag.contains(NBT_EXPERIENCE_MULTIPLIER) ? Math.max(0F, tag.getFloat(NBT_EXPERIENCE_MULTIPLIER)) : 1F;

        for (String key : tag.getCompound(NBT_SCORES).getAllKeys())
            teamScores.put(key, tag.getCompound(NBT_SCORES).getInt(key));

        for (com.flansmodultimate.common.teams.GameType type : com.flansmodultimate.common.teams.GameType.values())
            type.loadSettings(tag);
    }

    private void saveRuntime()
    {
        if (savedData == null)
            return;

        CompoundTag tag = savedData.runtime;
        tag.putBoolean(NBT_ENABLED, enabled); tag.putBoolean(NBT_ROUND_RUNNING, roundRunning);

        if (currentRoundId == null)
            tag.remove(NBT_CURRENT_ROUND);
        else
            tag.putUUID(NBT_CURRENT_ROUND, currentRoundId);

        tag.putInt(NBT_ROTATION_INDEX, rotationIndex);
        tag.putInt(NBT_TIME_LEFT, roundTimeLeftTicks);
        tag.putInt(NBT_ELAPSED, roundElapsedTicks);
        tag.putInt(NBT_INTERMISSION, intermissionTicks);
        ListTag voteOptions = new ListTag();

        for (UUID id : voteOptionIds)
        {
            CompoundTag option = new CompoundTag();
            option.putUUID(NBT_ID, id);
            voteOptions.add(option);
        }

        tag.put(NBT_VOTE_OPTIONS, voteOptions); tag.putBoolean(NBT_EXPLOSIONS, explosionsBreakBlocks); tag.putBoolean(NBT_BREAK_GLASS, canBreakGlass);
        tag.putBoolean(NBT_BREAK_GUNS, canBreakGuns); tag.putBoolean(NBT_DRIVEABLES_BREAK_BLOCKS, driveablesBreakBlocks); tag.putBoolean(NBT_BOMBS, bombsEnabled);
        tag.putBoolean(NBT_SHELLS, shellsEnabled); tag.putBoolean(NBT_BULLETS, bulletsEnabled); tag.putBoolean(NBT_ADVENTURE, forceAdventureMode);
        tag.putBoolean(NBT_ARMOUR_DROPS, armourDrops); tag.putBoolean(NBT_FUEL, vehiclesNeedFuel); tag.putBoolean(NBT_OVERRIDE_HUNGER, overrideHunger);
        tag.putBoolean(NBT_BREAK_VEHICLES, survivalCanBreakVehicles); tag.putBoolean(NBT_PLACE_VEHICLES, survivalCanPlaceVehicles);
        tag.putInt(NBT_WEAPON_DROPS, weaponDrops.ordinal()); tag.putInt(NBT_MG_LIFE, mgLife); tag.putInt(NBT_PLANE_LIFE, planeLife);
        tag.putInt(NBT_VEHICLE_LIFE, vehicleLife); tag.putInt(NBT_MECHA_LIFE, mechaLife); tag.putInt(NBT_AA_LIFE, aaLife);
        tag.putBoolean(NBT_VOTING, voting); tag.putBoolean(NBT_ROUNDS_GENERATOR, roundsGenerator); tag.putString(NBT_LOADOUT_POOL, currentLoadoutPoolId);
        tag.putFloat(NBT_EXPERIENCE_MULTIPLIER, experienceMultiplier);

        CompoundTag scores = new CompoundTag();
        teamScores.forEach(scores::putInt);
        tag.put(NBT_SCORES, scores);

        for (com.flansmodultimate.common.teams.GameType type : com.flansmodultimate.common.teams.GameType.values())
            type.saveSettings(tag);

        markDirty();
    }
}
