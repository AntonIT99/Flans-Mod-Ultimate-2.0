package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Two bounded doglegs around reported fighting on the way to a capture point. No world or navigation queries. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotBypass
{
    private static final double CORRIDOR = 32D;
    private static final double CLEARANCE = 40D;

    static List<Vec3> route(Vec3 from, SquadTask task, int lane, List<Vec3> contacts, List<Vec3> markers)
    {
        if (task.kind() != SquadTask.Kind.ATTACK)
            return List.of();
        Vec3 delta = task.target().subtract(from).multiply(1D, 0D, 1D);
        double distance = delta.length();
        if (distance < Math.max(40D, task.radius() * 2D))
            return List.of();
        Vec3 forward = delta.scale(1D / distance);
        Vec3 right = new Vec3(-forward.z, 0D, forward.x);
        double first = distance;
        double last = 0D;
        double leftWidth = CLEARANCE;
        double rightWidth = CLEARANCE;
        for (Vec3 contact : contacts)
        {
            Vec3 offset = contact.subtract(from).multiply(1D, 0D, 1D);
            double along = offset.dot(forward);
            double lateral = offset.dot(right);
            // Defenders at the destination must eventually be fought; bypass intervening fights instead.
            if (along < 0D || along >= distance - task.radius() - 12D || Math.abs(lateral) > CORRIDOR)
                continue;
            first = Math.min(first, along);
            last = Math.max(last, along);
            leftWidth = Math.max(leftWidth, CLEARANCE - lateral);
            rightWidth = Math.max(rightWidth, CLEARANCE + lateral);
        }
        if (first == distance)
            return List.of();
        double entry = Math.max(0D, first - CLEARANCE);
        double exit = Math.min(distance - task.radius() - 8D, last + CLEARANCE);
        List<Vec3> left = lane(from, forward, right.scale(-leftWidth), entry, exit, markers);
        List<Vec3> rightRoute = lane(from, forward, right.scale(rightWidth), entry, exit, markers);
        double leftCost = cost(from, task.target(), left, contacts);
        double rightCost = cost(from, task.target(), rightRoute, contacts);
        if (Math.abs(leftCost - rightCost) < 1D)
            return lane % 2 == 0 ? left : rightRoute;
        return leftCost < rightCost ? left : rightRoute;
    }

    private static List<Vec3> lane(Vec3 from, Vec3 forward, Vec3 offset, double entry, double exit, List<Vec3> markers)
    {
        return List.of(snap(from.add(forward.scale(entry)).add(offset), markers), snap(from.add(forward.scale(exit)).add(offset), markers));
    }

    private static Vec3 snap(Vec3 desired, List<Vec3> markers)
    {
        return markers.stream().filter(marker -> marker.distanceToSqr(desired) <= 64D).min(Comparator.comparingDouble(marker -> marker.distanceToSqr(desired))).orElse(desired);
    }

    private static double cost(Vec3 from, Vec3 target, List<Vec3> route, List<Vec3> contacts)
    {
        double cost = 0D;
        Vec3 start = from;
        for (Vec3 end : List.of(route.get(0), route.get(1), target))
        {
            cost += start.distanceTo(end);
            Vec3 segment = end.subtract(start).multiply(1D, 0D, 1D);
            for (Vec3 contact : contacts)
            {
                double fraction = segment.lengthSqr() < 1.0E-6D ? 0D : Math.max(0D, Math.min(1D, contact.subtract(start).dot(segment) / segment.lengthSqr()));
                double separation = contact.subtract(start.add(segment.scale(fraction))).horizontalDistance();
                cost += 4D * Math.max(0D, CORRIDOR - separation);
            }
            start = end;
        }
        return cost;
    }
}
