package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.types.Team;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** The living team players standing on a capture point this tick, grouped by round team id. */
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
            int teamId = round.getTeamId(team);
            if (teamId > 1)
                scan.add(teamId, player);
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

    /** The one team on the point, or 0 when it is empty or shared. */
    public int onlyTeamId()
    {
        return playersByTeam.size() == 1 ? playersByTeam.keySet().iterator().next() : 0;
    }
}
