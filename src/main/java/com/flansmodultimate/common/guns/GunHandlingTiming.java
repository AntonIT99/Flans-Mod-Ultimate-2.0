package com.flansmodultimate.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/** Shared reload, mode-switch and attachment-presentation rules. All times are ticks. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunHandlingTiming
{
    public static boolean tacticalReload(boolean hasLiveRounds, int slots, int magazineCapacity, float authoredTime)
    {
        return hasLiveRounds && (authoredTime > 0F || (slots == 1 && magazineCapacity > 1));
    }

    public static float reloadTime(float emptyTime, float tacticalTime, float multiplier, boolean tactical)
    {
        if (!tactical || tacticalTime == 0F)
            return emptyTime;
        return tacticalTime > 0F ? tacticalTime : emptyTime * multiplier;
    }

    /**
     * Ticks a gun waits after emptying a magazine of the given size. A single-round gun
     * reloads after every shot, so the shorter of its reload and its cadence sets the gap
     * between shots; a larger magazine never reloads faster than the gun fires.
     */
    public static float reloadWait(float reloadTime, float shootDelay, int magazineRounds)
    {
        return magazineRounds <= 1 ? Math.min(reloadTime, shootDelay) : Math.max(reloadTime, shootDelay);
    }

    public static int switchTicks(float ticks)
    {
        return Float.isFinite(ticks) ? Mth.ceil(Mth.clamp(ticks, 0F, 1200F)) : 0;
    }

    public static boolean attachmentVisible(boolean modeOnly, boolean secondary, boolean switching)
    {
        return !modeOnly || secondary || switching;
    }

    public static float switchMovement(int remaining, int length, float partialTick)
    {
        if (remaining <= 0 || length <= 0)
            return 0F;
        float progress = Mth.clamp((length - remaining + partialTick) / length, 0F, 1F);
        return (float) Math.sin(Math.PI * progress);
    }
}
