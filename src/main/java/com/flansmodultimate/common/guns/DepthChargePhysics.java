package com.flansmodultimate.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

/** Depth-charge motion and water-only activation, independent of the firing entity. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DepthChargePhysics
{
    public static boolean shouldDetonate(boolean inWater, double y, int seaLevel, float activationDepth)
    {
        return inWater && Double.isFinite(y) && Float.isFinite(activationDepth) && activationDepth >= 0F && seaLevel - y > activationDepth;
    }

    public static Vec3 velocity(Vec3 previous, boolean inWater, double gravity, float drag)
    {
        Vec3 next = previous.scale(drag).add(0D, -gravity, 0D);
        if (inWater)
            return new Vec3(0D, next.y, 0D);
        return next.y > 0.1D ? new Vec3(next.x, next.y * 0.9D, next.z) : next;
    }
}
