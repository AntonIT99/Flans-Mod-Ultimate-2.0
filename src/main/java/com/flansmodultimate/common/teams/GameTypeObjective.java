package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * What Domination, Conquest and Assault have in common: two teams fighting over the capture
 * points of the map. Kills are scored as in Team Deathmatch; subclasses decide how points are
 * taken, what holding them is worth and where players come back.
 */
public abstract class GameTypeObjective extends GameTypeTDM
{
    /** Round team id of the first team of the round. */
    protected static final int FIRST_TEAM_ID = 2;
    /** Round team id of the second team of the round. */
    protected static final int SECOND_TEAM_ID = 3;
    /** Stored as a spawn choice to mean the team's headquarters rather than any capture point. */
    protected static final UUID HEADQUARTERS = new UUID(0L, 0L);
    /** Pack sounds the Labjac fork plays at a point that changes hands; silent where no pack provides them. */
    private static final String SOUND_POINT_TAKEN = "CapturePoint_Taken";
    private static final String SOUND_POINT_LOST = "CapturePoint_Lost";
    private static final double SOUND_RANGE = 64D;
    /** Capture progress has to move this far before clients are told again. */
    private static final float SYNC_PROGRESS_STEP = 0.02F;
    /** Experience and score for everyone who was standing on a point when their team took it. */
    private static final int CAPTURE_EXPERIENCE = 25;
    private static final int CAPTURE_SCORE = 5;
    private static final String SETTING_AUTO_BALANCE = "auto_balance";

    private final String settingsPrefix;
    private final int defaultCaptureTicks;
    private boolean friendlyFire;
    private boolean autoBalance = true;
    /** Ticks one player needs to take a neutral point alone. */
    protected int captureTicks;
    private final Map<UUID, UUID> preferredSpawns = new HashMap<>();
    private final Map<UUID, CaptureState> lastSynced = new HashMap<>();

    protected GameTypeObjective(String id, String name, int defaultCaptureTicks)
    {
        super(id, name, 2);
        settingsPrefix = id.toLowerCase(Locale.ROOT) + "_";
        this.defaultCaptureTicks = defaultCaptureTicks;
        captureTicks = defaultCaptureTicks;
    }

    /** The state a point starts the round in, given all the points of the map in play order. */
    protected abstract CaptureState initialState(List<ITeamCapturePoint> points, int index);

    /** Advances every point by one tick. Called once all of them carry the state of this round. */
    protected abstract void tickObjectives(TeamsManager manager, TeamsRound round, ServerLevel level, List<ITeamCapturePoint> points);

    @Override
    public final void tick(TeamsManager manager)
    {
        TeamsRound round = manager.getCurrentRound().orElse(null);
        ServerLevel level = manager.getCurrentLevel();
        if (round == null || level == null || round.getTeamIds().size() < 2)
            return;

        List<ITeamCapturePoint> points = manager.getCapturePoints();
        UUID roundId = manager.getRoundInstanceId();
        for (int i = 0; i < points.size(); i++)
        {
            ITeamCapturePoint point = points.get(i);
            if (!Objects.equals(roundId, point.getRoundId()))
            {
                point.setCaptureState(initialState(points, i));
                point.setRoundId(roundId);
            }
        }
        tickObjectives(manager, round, level, points);
        syncChangedObjectives(manager, points);
    }

    /** Tells clients about the points as soon as one changes hands or its progress moved visibly. */
    private void syncChangedObjectives(TeamsManager manager, List<ITeamCapturePoint> points)
    {
        boolean changed = lastSynced.size() != points.size();
        for (ITeamCapturePoint point : points)
            changed |= needsSync(lastSynced.get(point.getObjectId()), point.getCaptureState());
        if (!changed)
            return;
        lastSynced.clear();
        points.forEach(point -> lastSynced.put(point.getObjectId(), point.getCaptureState()));
        manager.syncObjectives();
    }

    private static boolean needsSync(@Nullable CaptureState previous, CaptureState current)
    {
        if (previous == null || previous.ownerTeamId() != current.ownerTeamId() || previous.capturingTeamId() != current.capturingTeamId() || previous.contested() != current.contested())
            return true;
        if (previous.progress() == current.progress())
            return false;
        // Reaching either end is always reported, so a bar never stops just short of full or empty.
        return Math.abs(previous.progress() - current.progress()) >= SYNC_PROGRESS_STEP || current.progress() <= 0F || current.progress() >= 1F;
    }

    @Override
    public void roundStarted(TeamsManager manager)
    {
        // A round begins with nothing remembered from the last one, exactly as cleaning up leaves it.
        roundCleanup(manager);
    }

    @Override
    public void roundCleanup(TeamsManager manager)
    {
        preferredSpawns.clear();
        lastSynced.clear();
    }

    @Override
    public void playerQuit(TeamsManager manager, ServerPlayer player)
    {
        preferredSpawns.remove(player.getUUID());
    }

    @Override
    public List<ObjectiveSnapshot> createObjectives(TeamsManager manager)
    {
        return manager.getCapturePoints().stream().map(point -> ObjectiveSnapshot.of(point, !manager.getSpawnPositions(point).isEmpty())).toList();
    }

    @Override
    public boolean isPlayableOn(TeamsMap map)
    {
        return map.hasCapturePoints();
    }

    /** Rewards the players who took a point and tells everyone about it. */
    protected void pointCaptured(TeamsManager manager, TeamsRound round, ITeamCapturePoint point, CaptureScan scan, int teamId, String messageKey)
    {
        for (ServerPlayer player : scan.players(teamId))
        {
            PlayerData data = PlayerData.getInstance(player);
            data.setScore(data.getScore() + CAPTURE_SCORE);
            manager.awardExperience(player, CAPTURE_EXPERIENCE);
        }
        Team team = round.getTeam(teamId - FIRST_TEAM_ID);
        if (team != null)
            manager.broadcast(Component.translatable(messageKey, team.getDisplayComponent(), pointLabel(point)));
        manager.playSoundAt(point, SOUND_POINT_TAKEN, SOUND_RANGE);
    }

    protected void pointNeutralized(TeamsManager manager, ITeamCapturePoint point)
    {
        manager.broadcast(Component.translatable("message.flansmodultimate.teams.point_neutralized", pointLabel(point)));
        manager.playSoundAt(point, SOUND_POINT_LOST, SOUND_RANGE);
    }

    private static Component pointLabel(ITeamCapturePoint point)
    {
        return Component.literal("[" + point.getSymbol() + "] " + point.getBaseName()).withStyle(ChatFormatting.YELLOW);
    }

    /** Gives a point to the team ahead for each point it holds more than the other, as tickets bleed away. */
    protected static void drainTickets(TeamsManager manager, TeamsRound round, List<ITeamCapturePoint> points)
    {
        int first = 0;
        int second = 0;
        for (ITeamCapturePoint point : points)
        {
            if (point.getCaptureState().isHeldBy(FIRST_TEAM_ID))
                first++;
            else if (point.getCaptureState().isHeldBy(SECOND_TEAM_ID))
                second++;
        }
        Team ahead = round.getTeam(first > second ? 0 : 1);
        if (first != second && ahead != null)
            manager.addTeamScore(ahead, Math.abs(first - second));
    }

    // --- Spawning ---

    @Override
    public boolean selectSpawn(TeamsManager manager, ServerPlayer player, @Nullable UUID capturePointId)
    {
        if (!getObjectiveMode().usesDeployment())
            return false;
        preferredSpawns.put(player.getUUID(), capturePointId == null ? HEADQUARTERS : capturePointId);
        return true;
    }

    /** The spawn a player last asked for: a capture point id, {@link #HEADQUARTERS}, or null for no choice yet. */
    @Nullable
    protected UUID preferredSpawn(ServerPlayer player)
    {
        return preferredSpawns.get(player.getUUID());
    }

    protected void forgetPreferredSpawn(ServerPlayer player)
    {
        preferredSpawns.remove(player.getUUID());
    }

    /** A spawn at one of the bases the team started the round with, never at a capture point. */
    @Nullable
    protected Vec3 headquartersSpawn(TeamsManager manager, ServerPlayer player, int teamId)
    {
        return manager.findHeadquartersSpawn(teamId).or(() -> manager.findSpawnPoint(player, false)).orElse(null);
    }

    /** A random player spawner connected to the point, or null when it has none. */
    @Nullable
    protected static Vec3 spawnAt(TeamsManager manager, ITeamCapturePoint point)
    {
        List<Vec3> spawns = manager.getSpawnPositions(point);
        return spawns.isEmpty() ? null : spawns.get(manager.getRandom().nextInt(spawns.size()));
    }

    protected static int teamIdOf(TeamsManager manager, ServerPlayer player)
    {
        return manager.getCurrentRound().map(round -> round.getTeamId(PlayerData.getInstance(player).getNewTeam())).orElse(0);
    }

    // --- Settings ---

    @Override
    public boolean isFriendlyFireEnabled()
    {
        return friendlyFire;
    }

    @Override
    public boolean isAutoBalanceEnabled()
    {
        return autoBalance;
    }

    @Override
    public boolean setVariable(String variable, String value)
    {
        switch (variable.toLowerCase(Locale.ROOT))
        {
            case "friendlyfire" -> friendlyFire = Boolean.parseBoolean(value);
            case "autobalance" -> autoBalance = Boolean.parseBoolean(value);
            case "captureticks" -> captureTicks = Math.max(1, Integer.parseInt(value));
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void loadSettings(CompoundTag tag)
    {
        friendlyFire = tag.getBoolean(key("friendly_fire"));
        autoBalance = !tag.contains(key(SETTING_AUTO_BALANCE)) || tag.getBoolean(key(SETTING_AUTO_BALANCE));
        captureTicks = readInt(tag, "capture_ticks", defaultCaptureTicks, 1);
    }

    @Override
    public void saveSettings(CompoundTag tag)
    {
        tag.putBoolean(key("friendly_fire"), friendlyFire);
        tag.putBoolean(key(SETTING_AUTO_BALANCE), autoBalance);
        tag.putInt(key("capture_ticks"), captureTicks);
    }

    /** Settings are saved under a prefix of their own, so each objective mode keeps separate values. */
    protected final String key(String name)
    {
        return settingsPrefix + name;
    }

    protected final int readInt(CompoundTag tag, String name, int fallback, int minimum)
    {
        return tag.contains(key(name)) ? Math.max(minimum, tag.getInt(key(name))) : fallback;
    }
}
