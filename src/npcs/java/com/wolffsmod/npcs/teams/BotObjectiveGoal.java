package com.wolffsmod.npcs.teams;

import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Walks, drives or flies a bot to where its squad's task puts it, while it has nobody to fight. Long ways are strung
 * through the map's route markers and covered a stretch at a time, which keeps each path search short; a fight on the
 * way takes over, and the bot carries on once it is over.
 */
public final class BotObjectiveGoal extends Goal
{
    /** Ticks between checks whether the bot has somewhere to go. */
    private static final int CHECK_INTERVAL = 10;
    /** Ticks between new path searches on the way. */
    private static final int REPATH_INTERVAL = 20;
    /** Blocks within which a route point counts as reached. */
    private static final double ROUTE_REACHED = 6D;
    /** Blocks a spot may move before the route to it is planned again. */
    private static final double REPLAN_DISTANCE = 4D;

    private final EntityNPCInterface npc;
    @Nullable
    private BotCommander.Order order;
    private List<Vec3> route = List.of();
    @Nullable
    private Vec3 routedTo;
    private int routeIndex;
    private int cooldown;
    private int repath;

    public BotObjectiveGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (--cooldown > 0)
            return false;
        cooldown = CHECK_INTERVAL;
        if (npc.level().isClientSide || !BotTag.isBot(npc) || !free())
            return false;
        order = TeamsBots.orderFor(npc).orElse(null);
        return order != null && away(order);
    }

    @Override
    public boolean canContinueToUse()
    {
        return order != null && free() && horizontalDistance(order.spot()) > order.arrival() * 0.5D;
    }

    @Override
    public void start()
    {
        repath = 0;
    }

    @Override
    public void tick()
    {
        if (--repath > 0 && !npc.getNavigation().isDone())
            return;
        repath = REPATH_INTERVAL;
        order = TeamsBots.orderFor(npc).orElse(order);
        if (order == null)
            return;
        Vec3 position = npc.position();
        if (routedTo == null || routedTo.distanceTo(order.spot()) > REPLAN_DISTANCE)
        {
            routedTo = order.spot();
            route = RoutePlanner.route(position, order.spot(), routePoints(), RoutePlanner.LINK_DISTANCE);
            routeIndex = 0;
        }
        routeIndex = RoutePlanner.advance(position, route, routeIndex, ROUTE_REACHED);
        Vec3 step = RoutePlanner.towards(position, route.get(routeIndex));
        double y = step.y;
        if (!order.role().flies() && order.role() != BotRole.NAVAL)
            y = npc.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(step.x), (int) Math.floor(step.z));
        npc.getNavigation().moveTo(step.x, y, step.z, 1D);
    }

    @Override
    public void stop()
    {
        npc.getNavigation().stop();
        route = List.of();
        routedTo = null;
    }

    /** Whether the bot is free to move: alive, not fighting and not riding. */
    private boolean free()
    {
        return npc.isAlive() && !npc.isKilled() && npc.getTarget() == null && !npc.isPassenger();
    }

    private boolean away(BotCommander.Order current)
    {
        return horizontalDistance(current.spot()) > current.arrival();
    }

    /** Blocks to a spot on the ground plane: an aircraft is there when it circles above it at any height. */
    private double horizontalDistance(Vec3 spot)
    {
        double dx = spot.x - npc.getX();
        double dz = spot.z - npc.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private List<Vec3> routePoints()
    {
        return TeamsBots.current().filter(round -> npc.getServer() != null).map(round -> BotMarkers.get(npc.getServer()).positions(round.getLive().getRound().getMapId(), BotMarkers.Type.ROUTE))
            .orElse(List.of());
    }
}
