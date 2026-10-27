package com.flansmodultimate.common.driveables.collision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HullCarryTest
{
    @Test
    void theViewCatchesUpWithACarriedTurnOverTheFramesOfOneTick()
    {
        HullCarry carry = new HullCarry();
        carry.carried(100L, 7, 0D, 0D, 0D, 6F);
        assertEquals(6F, carry.viewYawLag(7, 0F), 1.0E-6F);
        assertEquals(3F, carry.viewYawLag(7, 0.5F), 1.0E-6F);
        assertEquals(0F, carry.viewYawLag(7, 1F), 1.0E-6F);
        // A tick without a carry leaves nothing to catch up with.
        assertEquals(0F, carry.viewYawLag(8, 0F), 1.0E-6F);
    }

    @Test
    void turnsAddUpWithinATickButNotAcrossTicks()
    {
        HullCarry carry = new HullCarry();
        carry.carried(100L, 7, 0D, 0D, 0D, 2F);
        carry.carried(100L, 7, 0D, 0D, 0D, 3F);
        assertEquals(5F, carry.viewYawLag(7, 0F), 1.0E-6F);
        carry.carried(101L, 8, 0D, 0D, 0D, 4F);
        assertEquals(4F, carry.viewYawLag(8, 0F), 1.0E-6F);
    }
}
