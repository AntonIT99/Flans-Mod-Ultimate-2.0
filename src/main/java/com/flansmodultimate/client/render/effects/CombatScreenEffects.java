package com.flansmodultimate.client.render.effects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Independent explosion and suppression envelopes. Only render offsets change; player aim never does. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CombatScreenEffects
{
    private static long explosionStarted = -1L;
    private static float explosionStrength;
    private static long suppressionUpdated = -1L;
    private static float suppressionStrength;
    private static float suppressionSide;

    public static void addExplosion(float strength, long now, double durationSeconds)
    {
        if (!Float.isFinite(strength) || strength <= 0F)
            return;
        float remaining = explosionLevel(now, durationSeconds);
        explosionStrength = Math.min(100F, Math.max(remaining, strength));
        explosionStarted = now;
    }

    public static float explosionLevel(long now, double durationSeconds)
    {
        if (explosionStarted < 0L || !Double.isFinite(durationSeconds) || durationSeconds <= 0D)
            return 0F;
        double progress = Math.max(0D, (now - explosionStarted) / (durationSeconds * 1000D));
        return (float) (explosionStrength * Math.max(0D, 1D - progress));
    }

    public static void addSuppression(float strength, float side, long now, double durationSeconds)
    {
        if (!Float.isFinite(strength) || !Float.isFinite(side) || strength <= 0F)
            return;
        suppressionStrength = Math.min(1F, suppressionLevel(now, durationSeconds) + strength);
        suppressionSide = Math.max(-1F, Math.min(1F, side));
        suppressionUpdated = now;
    }

    public static float suppressionLevel(long now, double durationSeconds)
    {
        if (suppressionUpdated < 0L || !Double.isFinite(durationSeconds) || durationSeconds <= 0D)
            return 0F;
        double elapsed = Math.max(0D, (now - suppressionUpdated) / (durationSeconds * 1000D));
        return (float) Math.max(0D, suppressionStrength - elapsed);
    }

    public static float suppressionRoll(long now, double durationSeconds)
    {
        float level = suppressionLevel(now, durationSeconds);
        return level * (suppressionSide * 0.8F + (float) Math.sin(now * 0.027D) * 0.2F);
    }

    public static void resetExplosion()
    {
        explosionStarted = -1L;
        explosionStrength = 0F;
    }

    public static void resetSuppression()
    {
        suppressionUpdated = -1L;
        suppressionStrength = 0F;
        suppressionSide = 0F;
    }

    public static void reset()
    {
        resetExplosion();
        resetSuppression();
    }
}
