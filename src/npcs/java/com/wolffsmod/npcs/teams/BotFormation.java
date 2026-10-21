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
        double distance = Math.min(Math.max(1D, radius * 0.8D), FIRST_RING + RING_STEP * Math.sqrt(spiralIndex));
        double angle = spiralIndex * GOLDEN_ANGLE;
        return target.add(Math.cos(angle) * distance, 0D, Math.sin(angle) * distance);
    }
}
