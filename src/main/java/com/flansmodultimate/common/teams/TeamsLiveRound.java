package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsLiveRound;
import com.flansmodultimate.api.ITeamsRound;
import com.flansmodultimate.api.TeamsCapturePoint;
import com.flansmodultimate.api.TeamsFlag;
import com.flansmodultimate.api.TeamsSpawner;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.types.Team;
import lombok.Getter;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/** The API view of one playing of a round, reading the manager as the round goes on. Server thread. */
public final class TeamsLiveRound implements ITeamsLiveRound
{
    private final TeamsManager manager;
    private final TeamsRound round;
    @Getter
    private final UUID instanceId;
    @Getter
    private final ResourceKey<Level> dimension;
    /** Set when the end of the round is announced, which may come before the manager has moved on. */
    private boolean ended;

    TeamsLiveRound(TeamsManager manager, TeamsRound round, UUID instanceId, ResourceKey<Level> dimension)
    {
        this.manager = manager;
        this.round = round;
        this.instanceId = instanceId;
        this.dimension = dimension;
    }

    void markEnded()
    {
        ended = true;
    }

    @Override
    public ITeamsRound getRound()
    {
        return round;
    }

    @Override
    public boolean isRunning()
    {
        return !ended && isCurrent() && manager.isRoundRunning();
    }

    /** Whether the manager still has this playing set up, which lasts through the break after it. */
    private boolean isCurrent()
    {
        return instanceId.equals(manager.getRoundInstanceId()) && manager.getCurrentRound().filter(round::equals).isPresent();
    }

    @Override
    public int getScore(int side)
    {
        List<Integer> sideIds = round.getSideIds();
        return side < 0 || side >= sideIds.size() || !isCurrent() ? 0 : manager.getTeamScore(round.getSideLeader(sideIds.get(side)));
    }

    @Override
    public int getTicksLeft()
    {
        return isCurrent() ? manager.getRoundTimeLeftTicks() : 0;
    }

    @Override
    public int getTicksElapsed()
    {
        return isCurrent() ? manager.getRoundElapsedTicks() : 0;
    }

    @Override
    public OptionalInt getAttackingSide()
    {
        GameType type = round.getGametype();
        return type == null || type.getObjectiveMode() != ObjectiveMode.ASSAULT ? OptionalInt.empty() : OptionalInt.of(round.getSideIndex(type.getAttackingTeamId(round)));
    }

    @Override
    public OptionalInt getDefendingSide()
    {
        GameType type = round.getGametype();
        return type == null || type.getObjectiveMode() != ObjectiveMode.ASSAULT ? OptionalInt.empty() : OptionalInt.of(round.getSideIndex(type.getDefendingTeamId(round)));
    }

    @Override
    public boolean mayCapture(int side)
    {
        List<Integer> sideIds = round.getSideIds();
        return side < 0 || side >= sideIds.size() || round.mayCapture(sideIds.get(side));
    }

    @Override
    public List<TeamsCapturePoint> getCapturePoints()
    {
        GameType type = round.getGametype();
        if (!isCurrent() || type == null || type.getObjectiveMode() == ObjectiveMode.NONE)
            return List.of();
        return manager.getCapturePoints().stream().map(this::toApi).toList();
    }

    @Override
    public List<TeamsFlag> getFlags()
    {
        if (!isCurrent() || !(round.getGametype() instanceof GameTypeCTF))
            return List.of();
        List<TeamsFlag> flags = new ArrayList<>();
        for (Flag flag : manager.getFlags())
        {
            Flagpole base = flag.getBase();
            Vec3 home = base == null ? flag.position() : new Vec3(base.getX(), base.getY() + 2D, base.getZ());
            flags.add(new TeamsFlag(flag.getObjectId(), manager.getTeamForBase(base), flag.position(), home, flag.isHome(), flag.getCarrierId()));
        }
        return List.copyOf(flags);
    }

    @Override
    public List<Vec3> getSpawnPositions(ITeam team)
    {
        int teamId = team instanceof Team internal ? round.getTeamId(internal) : 0;
        return !isCurrent() || teamId < TeamsRound.FIRST_TEAM_ID ? List.of() : manager.getSpawnPositions(teamId, false);
    }

    @Override
    public List<TeamsSpawner> getSpawners()
    {
        return isCurrent() ? manager.getSpawners() : List.of();
    }

    /** A capture point as the API reports it, with side ids turned into side indices. */
    TeamsCapturePoint toApi(ITeamCapturePoint point)
    {
        CaptureState state = point.getCaptureState();
        return new TeamsCapturePoint(point.getObjectId(), point.getSymbol(), point.getBaseName(), point.getDimension(), point.getTeamObjectPosition(), point.getCaptureRadius(),
            round.getSideIndex(state.ownerTeamId()), round.getSideIndex(state.capturingTeamId()), state.progress(), state.contested(), !manager.getSpawnPositions(point).isEmpty());
    }
}
