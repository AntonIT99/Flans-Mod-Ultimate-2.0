package com.flansmodultimate.client.render.effects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The camera kick a nearby driveable's main or coaxial gun gives this client, from the Labjac
 * Edition's {@code FancyScreenShake}.
 *
 * <p>
 * A kick pitches the view up, peaking halfway through and fading out, with a fast sideways
 * tremor on top. A new kick replaces the running one only when it is at least as strong, so a
 * coaxial gun firing between main gun shots does not cut the bigger kick short. The offsets are
 * a pure function of the time, which keeps them smooth at any frame rate and leaves the player's
 * real aim untouched.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VehicleScreenShake
{
    /** The longest a single kick lasts, whatever the content pack asks for. */
    static final long MAX_DURATION_MS = 2000L;

    private static long startedAt = -1L;
    private static long durationMs;
    private static float intensity;

    /** Starts a kick of the given strength lasting the given number of seconds. */
    public static void add(float newIntensity, float durationSeconds, long now)
    {
        // Written as negated comparisons so that NaN is rejected too
        if (!(newIntensity > 0F) || !(durationSeconds > 0F))
            return;
        if (isActive(now) && newIntensity < intensity)
            return;
        startedAt = now;
        durationMs = Math.max(1L, Math.min(MAX_DURATION_MS, Math.round(durationSeconds * 1000D)));
        intensity = newIntensity;
    }

    /** Degrees to add to the camera pitch; negative values look up. */
    public static float pitchOffset(long now)
    {
        float progress = progress(now);
        if (progress < 0F)
            return 0F;
        float fade = 1F - progress;
        float pulse = (float) Math.sin(progress * Math.PI);
        return -intensity * (0.08F + 0.32F * pulse) * fade;
    }

    /**
     * Degrees to add to the camera roll. The 1.7.10 original fed this tremor into the view bobbing,
     * which turned it into a roll three times its size.
     */
    public static float rollOffset(long now)
    {
        float progress = progress(now);
        if (progress < 0F)
            return 0F;
        float seconds = (now - startedAt) * 0.001F;
        return (float) Math.sin(seconds * 71F) * intensity * 0.18F * (1F - progress);
    }

    /** Ends any running kick. */
    public static void reset()
    {
        startedAt = -1L;
        durationMs = 0L;
        intensity = 0F;
    }

    private static boolean isActive(long now)
    {
        return progress(now) >= 0F;
    }

    /** How far through the running kick we are, from 0 to just below 1, or -1 when none is running. */
    private static float progress(long now)
    {
        if (startedAt < 0L || durationMs <= 0L || now < startedAt)
            return -1F;
        float progress = (float) (now - startedAt) / durationMs;
        return progress < 1F ? progress : -1F;
    }
}
