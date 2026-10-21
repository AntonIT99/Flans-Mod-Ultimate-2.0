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
    /** Blocks a bot has to move between two path searches to count as getting somewhere. */
    private static final double MOVED = 0.75D;
    /** Path searches in a row without getting anywhere after which the bot tries another way. */
    private static final int STALLED_SEARCHES = 3;

    private final EntityNPCInterface npc;
    @Nullable
    private BotCommander.Order order;
    private List<Vec3> route = List.of();
    @Nullable
    private Vec3 routedTo;
    private int routeIndex;
    private int cooldown;
    private int repath;
    /** Where the bot stood at the last path search, to tell whether it got anywhere since. */
    @Nullable
    private Vec3 lastPosition;
    /** Path searches in a row the bot barely moved over. */
    private int stalled;
    /** Ways round an obstacle tried since the bot last made progress; each tries another side and farther out. */
    private int detours;

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
        lastPosition = null;
        stalled = 0;
        detours = 0;
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
        if (stuck(position))
        {
            // Skip a route point it cannot reach, then try round the obstacle, on alternate sides and farther out.
            if (routeIndex < route.size() - 1)
                routeIndex++;
            step = RoutePlanner.detour(position, route.get(routeIndex), detours++);
        }
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

    /**
     * Whether the bot has hardly moved over the last few path searches. Aircraft never count as stuck: they circle on
     * their own.
     */
    private boolean stuck(Vec3 position)
    {
        boolean moved = lastPosition == null || lastPosition.distanceTo(position) >= MOVED;
        lastPosition = position;
        if (moved || order == null || order.role().flies())
        {
            stalled = 0;
            if (moved)
                detours = 0;
            return false;
        }
        return ++stalled >= STALLED_SEARCHES;
    }

    /** Whether the bot is free to move: alive, not fighting, not riding and not pinned down by fire. */
    private boolean free()
    {
        return npc.isAlive() && !npc.isKilled() && npc.getTarget() == null && !npc.isPassenger() && !BotSuppression.isPinned(npc);
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
