package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.ITeamsLiveRound;
import com.flansmodultimate.api.teams.TeamsCapturePoint;
import com.flansmodultimate.api.teams.TeamsFlag;
import com.flansmodultimate.api.teams.TeamsSpawner;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** The battle of a playing of a Teams round: its sides, bases, objectives and flags are the round's. Server thread. */
record RoundBattleground(ITeamsLiveRound live) implements Battleground
{
    @Override
    public ResourceKey<Level> dimension()
    {
        return live.getDimension();
    }

    @Override
    public List<ITeam> teams()
    {
        return live.getRound().getTeams();
    }

    @Override
    public int sideCount()
    {
        return live.getRound().getSideCount();
    }

    @Override
    public int sideOf(ITeam team)
    {
        return live.getRound().getSideOf(team);
    }

    @Override
    public int sideOf(Entity entity)
    {
        return FlansTeams.getTeam(entity).map(this::sideOf).orElse(-1);
    }

    @Override
    public List<ITeam> teamsOfSide(int side)
    {
        return live.getRound().getTeamsOfSide(side);
    }

    /** Allied teams share a side in a round, so every other side is an enemy. */
    @Override
    public boolean hostile(int side, int other)
    {
        return side != other;
    }

    @Override
    public boolean areEnemies(Entity first, Entity second)
    {
        return FlansTeams.areEnemies(first, second);
    }

    @Override
    public OptionalInt attackingSide()
    {
        return live.getAttackingSide();
    }

    @Override
    public boolean mayCapture(int side)
    {
        return live.mayCapture(side);
    }

    @Override
    public List<TeamsCapturePoint> capturePoints()
    {
        return live.getCapturePoints();
    }

    @Override
    public List<TeamsFlag> flags()
    {
        return live.getFlags();
    }

    @Override
    public List<Vec3> spawnPositions(ITeam team)
    {
        return live.getSpawnPositions(team);
    }

    @Override
    public List<TeamsSpawner> spawners()
    {
        return live.getSpawners();
    }

    @Override
    public Optional<String> mapId()
    {
        return Optional.of(live.getRound().getMapId());
    }

    @Override
    public Optional<ITeamsLiveRound> round()
    {
        return Optional.of(live);
    }
}
