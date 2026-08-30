package com.flansmodultimate.common.driveables;

import java.util.function.IntPredicate;

/** Deterministic cyclic seat selection shared by every driveable kind. */
public final class SeatCycle
{
    private SeatCycle() {}

    public static int nextAvailable(int current, int seatCount, IntPredicate available)
    {
        if (seatCount < 2 || available == null)
            return -1;
        int start = Math.floorMod(current, seatCount);
        for (int offset = 1; offset < seatCount; offset++)
        {
            int candidate = Math.floorMod(start + offset, seatCount);
            if (available.test(candidate))
                return candidate;
        }
        return -1;
    }
}
