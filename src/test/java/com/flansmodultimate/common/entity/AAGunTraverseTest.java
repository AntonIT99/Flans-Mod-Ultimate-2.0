package com.flansmodultimate.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AAGunTraverseTest
{
    @Test
    void yawTakesTheShortestPathAcrossTheAngleWrap()
    {
        assertEquals(172F, AAGunTraverse.yaw(170F, -170F, 40F), 1.0E-5F);
        assertEquals(-170F, AAGunTraverse.yaw(170F, -170F, 0F), 1.0E-5F);
    }

    @Test
    void pitchCannotMoveFasterThanTheConfiguredRate()
    {
        assertEquals(-2F, AAGunTraverse.pitch(0F, -75F, 40F), 1.0E-5F);
        assertEquals(-75F, AAGunTraverse.pitch(0F, -75F, 0F), 1.0E-5F);
    }
}
