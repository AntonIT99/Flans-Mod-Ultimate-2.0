package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcGunSteadinessTest
{
    /** Ticks a vehicle stands to shoot, and the fewest it then drives, as {@link NpcGunSteadiness} alternates them. */
    private static final int FIRE_TICKS = 50;
    private static final int MOVE_TICKS = 40;

    private static int standingTicks(NpcGunSteadiness gunnery, NpcWeaponOptions options, boolean bears, int ticks)
    {
        int standing = 0;
        for (int tick = 0; tick < ticks; tick++)
            if (gunnery.haltsToFire(options, bears))
                standing++;
        return standing;
    }

    @Test
    void aVehicleWithNothingToShootAtNeverStands()
    {
        assertEquals(0, standingTicks(new NpcGunSteadiness(), new NpcWeaponOptions(), false, 200));
    }

    @Test
    void aVehicleStandsToShootAndThenDrivesOnWhetherOrNotItsTargetIsDown()
    {
        NpcGunSteadiness gunnery = new NpcGunSteadiness();
        NpcWeaponOptions options = new NpcWeaponOptions();
        assertEquals(FIRE_TICKS, standingTicks(gunnery, options, true, FIRE_TICKS), "stands for its whole firing halt");
        assertEquals(0, standingTicks(gunnery, options, true, MOVE_TICKS), "then drives on even with the target still up");
        assertTrue(gunnery.haltsToFire(options, true), "and may stand again once it has covered some ground");
    }

    @Test
    void aVehicleThatLosesItsShotStopsStandingAtOnce()
    {
        NpcGunSteadiness gunnery = new NpcGunSteadiness();
        NpcWeaponOptions options = new NpcWeaponOptions();
        assertTrue(gunnery.haltsToFire(options, true));
        assertFalse(gunnery.haltsToFire(options, false));
        assertEquals(FIRE_TICKS, standingTicks(gunnery, options, true, FIRE_TICKS), "a fresh halt, rather than the rest of the last one");
    }

    @Test
    void stabilisedGunsNeitherStandNorScatter()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.GUN_STABILISER, true);
        assertEquals(0, standingTicks(new NpcGunSteadiness(), options, true, 200));
    }

    @Test
    void theRuleCanBeTurnedOffAltogether()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.UNSTEADY_GUNS, false);
        assertEquals(0, standingTicks(new NpcGunSteadiness(), options, true, 200));
    }
}
