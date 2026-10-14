package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.types.Team;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The living team players standing on a capture point this tick, grouped by side id. Allied teams
 * count as one, so they take a point together instead of contesting it against each other.
 */
public final class CaptureScan
{
    private final Map<Integer, List<ServerPlayer>> playersByTeam = new TreeMap<>();
    private int totalPlayers;

    /**
     * Counts everyone inside the upright cylinder around the point: within its radius on the
     * ground plane and no further than that above or below it.
     */
    public static CaptureScan of(ITeamCapturePoint point, ServerLevel level, TeamsRound round)
    {
        CaptureScan scan = new CaptureScan();
        Vec3 centre = point.getTeamObjectPosition();
        double radius = point.getCaptureRadius();
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players())
        {
            double dx = player.getX() - centre.x;
            double dz = player.getZ() - centre.z;
            boolean onPoint = player.isAlive() && !player.isSpectator() && dx * dx + dz * dz <= radiusSquared && Math.abs(player.getY() - centre.y) <= radius;
            Team team = onPoint ? PlayerData.getInstance(player).getTeam() : null;
            int sideId = round.getSideId(team);
            if (sideId >= TeamsRound.FIRST_TEAM_ID)
                scan.add(sideId, player);
        }
        return scan;
    }

    private void add(int teamId, ServerPlayer player)
    {
        playersByTeam.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(player);
        totalPlayers++;
    }

    public int count(int teamId)
    {
        List<ServerPlayer> players = playersByTeam.get(teamId);
        return players == null ? 0 : players.size();
    }

    public List<ServerPlayer> players(int teamId)
    {
        return List.copyOf(playersByTeam.getOrDefault(teamId, List.of()));
    }

    public boolean isEmpty()
    {
        return totalPlayers == 0;
    }

    public boolean isContested()
    {
        return playersByTeam.size() > 1;
    }

    /** The one side on the point, or 0 when it is empty or shared. */
    public int onlyTeamId()
    {
        return playersByTeam.size() == 1 ? playersByTeam.keySet().iterator().next() : 0;
    }

    /** How many players each side has on the point, by side id in round order. */
    public Map<Integer, Integer> counts()
    {
        Map<Integer, Integer> counts = new TreeMap<>();
        playersByTeam.forEach((sideId, players) -> counts.put(sideId, players.size()));
        return counts;
    }
}
