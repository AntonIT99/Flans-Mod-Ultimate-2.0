package com.flansmodultimate.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

/** Geometry and strength of server-authored near misses, independent of projectile hit detection. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NearMissFeedback
{
    /** Closest point on the travelled segment, including its endpoints. */
    public static Vec3 closestPoint(Vec3 start, Vec3 end, Vec3 eye)
    {
        Vec3 travel = end.subtract(start);
        double lengthSquared = travel.lengthSqr();
        if (lengthSquared < 1E-12D)
            return start;
        double progress = Math.max(0D, Math.min(1D, eye.subtract(start).dot(travel) / lengthSquared));
        return start.add(travel.scale(progress));
    }

    /** Labjac's damage/proximity impulse, with pack and server multipliers and a bounded ceiling. */
    public static float intensity(float damage, double distance, double range, double multiplier)
    {
        if (!Float.isFinite(damage) || !Double.isFinite(distance) || !Double.isFinite(range) || !Double.isFinite(multiplier) || range <= 0D || distance < 0D || distance > range || multiplier <= 0D)
            return 0F;
        double damageStrength = Math.min(1D, Math.sqrt(Math.max(0F, damage) / 40D));
        double proximity = Math.max(0.2D, 1D - distance / range);
        return (float) Math.min(1D, (0.08D + damageStrength * proximity * 0.70D) * multiplier);
    }
}
