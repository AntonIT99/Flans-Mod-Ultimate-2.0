package com.flansmodultimate.common.entity;

import net.minecraft.util.Mth;

/** Angular speed limit shared by player-controlled and sentry AA guns. */
public final class AAGunTraverse
{
    private AAGunTraverse() {}

    public static float yaw(float current, float target, float speedDegreesPerSecond)
    {
        if (speedDegreesPerSecond <= 0F)
            return Mth.wrapDegrees(target);
        float step = speedDegreesPerSecond / 20F;
        return Mth.wrapDegrees(current + Mth.clamp(Mth.wrapDegrees(target - current), -step, step));
    }

    public static float pitch(float current, float target, float speedDegreesPerSecond)
    {
        if (speedDegreesPerSecond <= 0F)
            return target;
        float step = speedDegreesPerSecond / 20F;
        return current + Mth.clamp(target - current, -step, step);
    }
}
