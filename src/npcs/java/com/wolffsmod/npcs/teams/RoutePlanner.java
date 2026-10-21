package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Strings a path across a map through the route markers operators placed, so bots go round obstacles a straight line
 * would run into. Two points are linked when they are no farther apart than the link distance. Pure arithmetic, any
 * thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoutePlanner
{
    /** Blocks within which route points, and the ends of a route, link up. */
    public static final double LINK_DISTANCE = 64D;
    /** Blocks a bot walks towards a point before it heads for the next one. */
    public static final double STEP = 24D;

    /**
     * @param nodes
     *            the route points of the map
     * @return the points to pass in order, ending with {@code to}; just {@code to} when it is close or when no chain of
     *         route points reaches it
     */
    public static List<Vec3> route(Vec3 from, Vec3 to, List<Vec3> nodes, double link)
    {
        if (from.distanceTo(to) <= link || nodes.isEmpty())
            return List.of(to);
        List<Vec3> points = new ArrayList<>(nodes.size() + 2);
        points.add(from);
        points.addAll(nodes);
        points.add(to);
        int goal = points.size() - 1;

        double[] distance = new double[points.size()];
        int[] previous = new int[points.size()];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(previous, -1);
        distance[0] = 0D;
        PriorityQueue<Integer> open = new PriorityQueue<>(Comparator.comparingDouble(index -> distance[index]));
        open.add(0);
        boolean[] closed = new boolean[points.size()];
        while (!open.isEmpty() && !closed[goal])
        {
            int current = open.poll();
            // A point queued again at a shorter distance is settled the first time it comes out.
            if (!closed[current])
            {
                closed[current] = true;
                relax(points, current, link, closed, distance, previous, open);
            }
        }
        if (previous[goal] < 0)
            return List.of(to);
        LinkedList<Vec3> path = new LinkedList<>();
        for (int index = goal; index > 0; index = previous[index])
            path.addFirst(points.get(index));
        return List.copyOf(path);
    }

    /** Shortens the known way to every unsettled point linked to a settled one through it. */
    private static void relax(List<Vec3> points, int current, double link, boolean[] closed, double[] distance, int[] previous, PriorityQueue<Integer> open)
    {
        for (int next = 0; next < points.size(); next++)
        {
            double step = points.get(current).distanceTo(points.get(next));
            if (!closed[next] && step <= link && distance[current] + step < distance[next])
            {
                distance[next] = distance[current] + step;
                previous[next] = current;
                open.add(next);
            }
        }
    }

    /**
     * @param index
     *            the route point the bot was heading for
     * @return the route point the bot heads for now: the next one once it came within {@code reached} blocks of it, the
     *         last one staying the last
     */
    public static int advance(Vec3 from, List<Vec3> route, int index, double reached)
    {
        int current = Math.max(0, Math.min(index, route.size() - 1));
        while (current < route.size() - 1 && from.distanceTo(route.get(current)) <= reached)
            current++;
        return current;
    }

    /** @return the point a bot walks to next on its way to a target: the target, or {@link #STEP} blocks towards it */
    public static Vec3 towards(Vec3 from, Vec3 target)
    {
        Vec3 offset = target.subtract(from);
        double length = offset.length();
        return length <= STEP ? target : from.add(offset.scale(STEP / length));
    }
}
