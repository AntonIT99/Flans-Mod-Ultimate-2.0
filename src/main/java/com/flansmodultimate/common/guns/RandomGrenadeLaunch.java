package com.flansmodultimate.common.guns;

import net.minecraft.util.RandomSource;

/** Sampled once by the server: an upward, rolled debris launch with the fork's randomized speed. */
public record RandomGrenadeLaunch(float yaw, float pitch, float roll, float speedScale)
{
    public static RandomGrenadeLaunch sample(RandomSource random)
    {
        return new RandomGrenadeLaunch(random.nextFloat() * 360F, -66F - random.nextFloat() * 22F, 90F, random.nextFloat() * 1.5F);
    }
}
