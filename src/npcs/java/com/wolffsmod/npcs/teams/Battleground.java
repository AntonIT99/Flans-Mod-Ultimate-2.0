package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.*;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Where a battle is fought and who fights it: its sides and the teams on them, where their bots come in, and what there is
 * to fight over. The bots of a battle ask it, never the Teams round directly, so the same bots can fight a battle that is
 * no round. Server thread.
 */
interface Battleground
{
    ResourceKey<Level> dimension();

    /** @return the teams whose bots fight, in order */
    List<ITeam> teams();

    default Optional<ITeam> team(String shortName)
    {
        return teams().stream().filter(team -> team.getShortName().equals(shortName)).findFirst();
    }

    int sideCount();

    /** @return the side a team fights on, or -1 when it does not fight here */
    int sideOf(ITeam team);

    /** @return the side an entity fights on, a player's or a combatant's, or -1 for anyone who does not fight here */
    int sideOf(Entity entity);

    /** @return the teams on a side, in order */
    List<ITeam> teamsOfSide(int side);

    /** Whether two sides fight each other. */
    boolean hostile(int side, int other);

    /** Whether both entities fight here, on sides hostile to each other. */
    boolean areEnemies(Entity first, Entity second);

    /** @return the side that attacks while the other defends, or empty when every side may take objectives */
    OptionalInt attackingSide();

    boolean mayCapture(int side);

    List<TeamsCapturePoint> capturePoints();

    List<TeamsFlag> flags();

    /** @return where members of a team may come in right now */
    List<Vec3> spawnPositions(ITeam team);

    List<TeamsSpawner> spawners();

    /** @return the map whose markers the bots use, or empty for a battle on no map */
    Optional<String> mapId();

    /** @return the Teams round this battle is, or empty for a battle fought outside one */
    Optional<ITeamsLiveRound> round();
}
