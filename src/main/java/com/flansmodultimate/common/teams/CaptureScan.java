package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The living fighters standing on a capture point this tick, grouped by side id: the players of the
 * round, and the combatants addons assigned to its teams. Each weighs on the point with its capture
 * weight, a player with 1. Allied teams count as one, so they take a point together instead of
 * contesting it against each other.
 */
public final class CaptureScan
{
    private final Map<Integer, Float> weightByTeam = new TreeMap<>();
    private final Map<Integer, List<ServerPlayer>> playersByTeam = new TreeMap<>();

    /**
     * Weighs everyone inside the upright cylinder around the point: within its radius on the
     * ground plane and no further than that above or below it.
     */
    public static CaptureScan of(ITeamCapturePoint point, ServerLevel level, TeamsRound round)
    {
        TeamsManager manager = TeamsManager.getInstance();
        CaptureScan scan = new CaptureScan();
        Vec3 centre = point.getTeamObjectPosition();
        double radius = point.getCaptureRadius();
        double radiusSquared = radius * radius;
        AABB cylinderBounds = new AABB(centre, centre).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, cylinderBounds, Entity::isAlive))
        {
            double dx = entity.getX() - centre.x;
            double dz = entity.getZ() - centre.z;
            if (dx * dx + dz * dz > radiusSquared || Math.abs(entity.getY() - centre.y) > radius)
                continue;
            float weight = TeamsCombatants.getCaptureWeight(manager, entity);
            Team team = weight > 0F ? TeamsCombatants.getTeam(entity) : null;
            int sideId = round.getSideId(team);
            if (sideId >= TeamsRound.FIRST_TEAM_ID)
                scan.add(sideId, entity, weight);
        }
        return scan;
    }

    private void add(int sideId, LivingEntity entity, float weight)
    {
        weightByTeam.merge(sideId, weight, Float::sum);
        if (entity instanceof ServerPlayer player)
            playersByTeam.computeIfAbsent(sideId, ignored -> new ArrayList<>()).add(player);
    }

    /** The weight a side has on the point, which for players alone is their number. */
    public float count(int teamId)
    {
        return weightByTeam.getOrDefault(teamId, 0F);
    }

    /** The players of a side on the point, who share its rewards; combatants are weighed but not rewarded. */
    public List<ServerPlayer> players(int teamId)
    {
        return List.copyOf(playersByTeam.getOrDefault(teamId, List.of()));
    }

    public boolean isEmpty()
    {
        return weightByTeam.isEmpty();
    }

    public boolean isContested()
    {
        return weightByTeam.size() > 1;
    }

    /** The one side on the point, or 0 when it is empty or shared. */
    public int onlyTeamId()
    {
        return weightByTeam.size() == 1 ? weightByTeam.keySet().iterator().next() : 0;
    }

    /** How much weight each side has on the point, by side id in round order. */
    public Map<Integer, Float> counts()
    {
        return Collections.unmodifiableMap(new TreeMap<>(weightByTeam));
    }
}
