package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Where each member of a squad stands around its task's target: spread on a spiral so that no two share a spot, or on
 * the garrison and sniper posts near a defended objective. Pure arithmetic, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotFormation
{
    /** The golden angle, which spreads spiral points evenly. */
    private static final double GOLDEN_ANGLE = Math.PI * (3D - Math.sqrt(5D));
    private static final double FIRST_RING = 2D;
    private static final double RING_STEP = 1.5D;
    /** Hulls a ring around a station holds before the next ring is laid a spacing farther out. */
    private static final double RING_HULLS = 6D;

    /**
     * @param index
     *            the member's place in its squad, from 0
     * @param radius
     *            the radius the squad spreads over
     * @param posts
     *            posts the members take first, in order
     * @return the member's spot
     */
    public static Vec3 spot(Vec3 target, int index, double radius, List<Vec3> posts)
    {
        if (index < posts.size())
            return posts.get(index);
        int spiralIndex = index - posts.size();
        if (spiralIndex == 0 && posts.isEmpty())
            return target;
        double distance = Math.min(Math.max(0D, radius * 0.8D), FIRST_RING + RING_STEP * Math.sqrt(spiralIndex));
        double angle = spiralIndex * GOLDEN_ANGLE;
        return target.add(Math.cos(angle) * distance, 0D, Math.sin(angle) * distance);
    }

    /**
     * Where a ship of a squad takes station: in line ahead, each hull a spacing astern of the one in front along the
     * course the squad steams in on. Hulls are many blocks long, so they cannot share a station the way soldiers share a
     * rally point, and a line astern is the one arrangement in which none of them crosses another. A squad already lying
     * on its station has no course to string out along, so its hulls ring the station a spacing apart instead.
     *
     * @param from
     *            where the squad comes from, which gives the line its direction
     * @param index
     *            the ship's place in its squad, from 0; the leader lies on the station itself
     * @param spacing
     *            blocks between one hull and the next
     * @return the ship's station
     */
    public static Vec3 line(Vec3 target, Vec3 from, int index, double spacing)
    {
        if (index <= 0)
            return target;
        Vec3 astern = target.subtract(from).multiply(1D, 0D, 1D);
        if (astern.lengthSqr() < 1.0E-4D)
        {
            double angle = index * GOLDEN_ANGLE;
            double distance = spacing * (1D + (index - 1) / RING_HULLS);
            return target.add(Math.cos(angle) * distance, 0D, Math.sin(angle) * distance);
        }
        return target.subtract(astern.normalize().scale(spacing * index));
    }
}
