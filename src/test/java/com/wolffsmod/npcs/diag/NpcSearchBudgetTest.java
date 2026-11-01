package com.wolffsmod.npcs.diag;

import org.junit.jupiter.api.Test;

import static com.wolffsmod.npcs.diag.NpcBudgets.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcSearchBudgetTest
{
    @Test
    void anAllowanceAtItsCeilingMeansNoLimit()
    {
        assertEquals(0, limitOf(MAX_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES));
        assertEquals(0, limitOf(MAX_AUTO_TARGET_SEARCHES + 1D, MAX_AUTO_TARGET_SEARCHES));
        assertEquals(100, limitOf(100.9D, MAX_AUTO_TARGET_SEARCHES));
    }

    @Test
    void slowTicksCutTheAllowanceBackAndFastOnesLetItGrow()
    {
        double over = steer(100D, 60D, 45D, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        assertTrue(over < 100D, "a tick over the target must cut the allowance back");
        double under = steer(100D, 30D, 45D, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        assertTrue(under > 100D, "a tick under the target must let the allowance grow");
        // Growth is gentler than the cutback, so a server at its limit settles rather than oscillating widely.
        assertTrue(under - 100D < 100D - over);
    }

    @Test
    void theAllowanceNeverStarvesEveryNpcHoweverLongTheTicksAre()
    {
        double allowance = MAX_AUTO_TARGET_SEARCHES;
        for (int tick = 0; tick < 1000; tick++)
            allowance = steer(allowance, 200D, 45D, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        assertEquals(MIN_AUTO_TARGET_SEARCHES, allowance);
        assertEquals(MIN_AUTO_TARGET_SEARCHES, limitOf(allowance, MAX_AUTO_TARGET_SEARCHES));
    }

    @Test
    void theAllowanceClimbsBackToNoLimitOnceTheServerRecovers()
    {
        double allowance = MIN_AUTO_PATH_SEARCHES;
        for (int tick = 0; tick < 1000; tick++)
            allowance = steer(allowance, 10D, 45D, MIN_AUTO_PATH_SEARCHES, MAX_AUTO_PATH_SEARCHES);
        assertEquals(MAX_AUTO_PATH_SEARCHES, allowance);
        assertEquals(0, limitOf(allowance, MAX_AUTO_PATH_SEARCHES));
    }
}
