package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.driveables.WeaponTriggerState;
import com.flansmodultimate.api.equipment.MechaMeleeProperties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Pure decisions shared by the driver's melee, traversal and trigger paths. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MechaCombat
{
    static double meleeReach(ISeat seat)
    {
        double first = seat.getDriverMeleeProperties(false).map(MechaMeleeProperties::reach).orElse(Double.POSITIVE_INFINITY);
        double second = seat.getDriverMeleeProperties(true).map(MechaMeleeProperties::reach).orElse(Double.POSITIVE_INFINITY);
        double reach = Math.min(first, second);
        return Double.isFinite(reach) ? reach : 0D;
    }

    static double approach(double reach, double vertical)
    {
        return Math.max(0.25D, Math.sqrt(Math.max(0D, reach * reach - vertical * vertical)) * 0.8D);
    }

    static boolean ascend(double rise, double step, double jump, boolean grounded, boolean blocked, boolean rocket)
    {
        return grounded && blocked || rise > step + 0.1D && (rocket || grounded && rise <= jump + 0.2D);
    }

    static boolean trigger(boolean ready, boolean held, WeaponTriggerState state)
    {
        return ready && !(held && state.needsRelease()) && (held || state.mode() != WeaponTriggerState.Mode.BURST || !state.burstActive());
    }
}
