package com.flansmodultimate.common.driveables;

import lombok.Getter;

/** Shared heat of a driveable's mounted weapons, advanced only on the server. */
@Getter
public final class VehicleWeaponHeat
{
    private int heat;
    private int lockoutTicks;

    public void tick(int limit, int coolingBonus)
    {
        if (limit <= 0)
        {
            heat = 0;
            lockoutTicks = 0;
            return;
        }
        heat = (int) Math.max(0L, (long) heat - 1L - Math.max(0, coolingBonus));
        lockoutTicks = Math.max(0, lockoutTicks - 1);
    }

    public boolean locked(int limit)
    {
        return limit > 0 && (lockoutTicks > 0 || heat > limit);
    }

    /** Adds five heat per fired muzzle and reports the start of a new lockout. */
    public boolean fired(int rounds, int limit, int penalty)
    {
        if (limit <= 0 || rounds <= 0 || locked(limit))
            return false;
        heat = (int) Math.min(Integer.MAX_VALUE, (long) heat + 5L * rounds);
        if (heat <= limit)
            return false;
        lockoutTicks = Math.max(0, penalty);
        return true;
    }
}
