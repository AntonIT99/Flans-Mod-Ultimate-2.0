package com.flansmodultimate.common.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** How many players of a team may use a class limited by {@code ClassLimit} or {@code ClassLimitPercent}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassLimits
{
    public static final int UNLIMITED = Integer.MAX_VALUE;

    /**
     * The number of slots a class has on a team of {@code teamSize} players.
     *
     * <p>
     * A negative limit means that limit is not set. When both are set the smaller wins. The
     * percentage is rounded up, so a class limited to a share of the team is never closed to a
     * team that has players at all.
     * </p>
     *
     * @param classLimit
     *            fixed number of slots, or negative for none
     * @param classLimitPercent
     *            share of the team in percent, or negative for none
     * @param teamSize
     *            players on the team, counting the one who is choosing
     */
    public static int capacity(int classLimit, float classLimitPercent, int teamSize)
    {
        int capacity = UNLIMITED;
        if (classLimit >= 0)
            capacity = classLimit;
        if (classLimitPercent >= 0F)
            capacity = Math.min(capacity, (int) Math.ceil(Math.max(0, teamSize) * Math.min(100F, classLimitPercent) / 100F));
        return capacity;
    }

    public static boolean hasSlot(int capacity, int used)
    {
        return capacity == UNLIMITED || used < capacity;
    }
}
