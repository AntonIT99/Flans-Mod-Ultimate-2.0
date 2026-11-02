package com.wolffsmod.npcs.tactics;

import net.minecraft.world.phys.Vec3;

/** A movement order retained during combat, including the area to remain in once reached. Server thread. */
public record NpcCombatIntent(Vec3 destination, double arrival, Vec3 centre, double radius, boolean ordered)
{
    public NpcCombatIntent(Vec3 destination, double arrival, Vec3 centre, double radius)
    {
        this(destination, arrival, centre, radius, true);
    }

    public NpcCombatIntent(Vec3 destination, double arrival)
    {
        this(destination, arrival, destination, arrival);
    }

    public boolean reached(Vec3 position)
    {
        return distance(position, destination) <= arrival;
    }

    public double progress(Vec3 from, Vec3 to)
    {
        return Math.max(0D, distance(from, destination) - arrival) - Math.max(0D, distance(to, destination) - arrival);
    }

    /** Once inside an assigned area, voluntary combat movement stays inside it. */
    public boolean permits(Vec3 from, Vec3 to)
    {
        return !ordered || distance(from, centre) > radius || distance(to, centre) <= radius;
    }

    public Vec3 step(Vec3 from, double length)
    {
        Vec3 direction = destination.subtract(from).multiply(1D, 0D, 1D);
        return from.add(direction.normalize().scale(Math.min(length, Math.max(0D, direction.length() - arrival))));
    }

    private static double distance(Vec3 first, Vec3 second)
    {
        return first.subtract(second).horizontalDistance();
    }
}
