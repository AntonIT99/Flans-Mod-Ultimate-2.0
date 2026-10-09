package com.flansmodultimate.client.render.effects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GunScreenShakeTest
{
    @AfterEach
    void reset()
    {
        GunScreenShake.reset();
    }

    @Test
    void idleViewIsUntouched()
    {
        GunScreenShake.tick();
        assertEquals(0F, GunScreenShake.fovOffset(1F));
        assertEquals(0F, GunScreenShake.pitchOffset(1F));
    }

    @Test
    void cleanShotNarrowsTheViewByATenthPerPoint()
    {
        GunScreenShake.addShot(false, 1F, 0F);
        GunScreenShake.tick();
        assertEquals(-0.5F, GunScreenShake.fovOffset(1F), 1e-5F);
        assertEquals(0F, GunScreenShake.fovOffset(0F), 1e-5F);
    }

    @Test
    void sustainedStyleGrowsWithSustainedFire()
    {
        GunScreenShake.addShot(true, 1F, 0F);
        GunScreenShake.tick();
        // One shot fills the sustained meter to 10, scaling the punch by 1 + 10/70
        assertEquals(-0.5F * (1F + 10F / 70F), GunScreenShake.fovOffset(1F), 1e-5F);
    }

    @Test
    void punchRecoversAndEnds()
    {
        GunScreenShake.addShot(false, 1F, 0F);
        for (int i = 0; i < 10; i++)
            GunScreenShake.tick();
        assertEquals(0F, GunScreenShake.fovOffset(1F));
    }

    @Test
    void shakeBudgetIsCapped()
    {
        for (int i = 0; i < 100; i++)
            GunScreenShake.addShot(false, 10F, 0F);
        GunScreenShake.tick();
        assertEquals(-10F, GunScreenShake.fovOffset(1F), 1e-5F);
    }

    @Test
    void cameraKickLooksUpAndDecays()
    {
        GunScreenShake.addShot(false, 0F, 1F);
        GunScreenShake.tick();
        assertEquals(-GunScreenShake.CAMERA_KICK_DEGREES, GunScreenShake.pitchOffset(0F), 1e-5F);
        assertEquals(-GunScreenShake.CAMERA_KICK_DEGREES * 0.2F, GunScreenShake.pitchOffset(1F), 1e-5F);
    }

    @Test
    void recoveryMatchesTheTruncatedLegacyTable()
    {
        assertEquals(5, GunScreenShake.recoveryStep(-100));
        assertEquals(4, GunScreenShake.recoveryStep(-85));
        assertEquals(4, GunScreenShake.recoveryStep(-75));
        assertEquals(3, GunScreenShake.recoveryStep(-55));
        assertEquals(2, GunScreenShake.recoveryStep(-35));
        assertEquals(1, GunScreenShake.recoveryStep(-20));
        assertEquals(1, GunScreenShake.recoveryStep(-1));
    }

    @Test
    void invalidValuesAreIgnored()
    {
        GunScreenShake.addShot(false, Float.NaN, Float.POSITIVE_INFINITY);
        GunScreenShake.tick();
        assertEquals(0F, GunScreenShake.fovOffset(1F));
        assertEquals(0F, GunScreenShake.pitchOffset(1F));
    }
}
