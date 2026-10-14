package com.flansmodultimate.common.teams;

import org.apache.commons.lang3.StringUtils;

/**
 * Where one spawn choice sits on the deployment map of a {@link TeamsMap}, as fractions of the map
 * area so the layout survives any window size.
 *
 * <p>
 * Keys are {@code hq_<team id>} for a headquarters and {@code objective_<capture point id>} for a
 * capture point.
 * </p>
 */
public record DeployLayoutEntry(String key, float x, float y)
{
    public static final int MAX_KEY_LENGTH = 64;
    public static final String HQ_PREFIX = "hq_";
    public static final String OBJECTIVE_PREFIX = "objective_";

    public DeployLayoutEntry
    {
        key = StringUtils.defaultString(key);
        x = clamp(x);
        y = clamp(y);
    }

    public boolean isValid()
    {
        return !key.isBlank() && key.length() <= MAX_KEY_LENGTH;
    }

    static float clamp(float value)
    {
        return Float.isFinite(value) ? Math.max(0F, Math.min(1F, value)) : 0F;
    }
}
