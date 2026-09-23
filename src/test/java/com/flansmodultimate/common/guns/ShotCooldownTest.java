package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShotCooldownTest
{
    /** Fires a weapon for a number of ticks the way every firing loop in the mod does. */
    private static int shotsOverTicks(float delay, int ticks)
    {
        float cooldown = 0F;
        int shots = 0;
        for (int tick = 0; tick < ticks; tick++)
        {
            while (ShotCooldown.isReady(cooldown))
            {
                shots++;
                cooldown = ShotCooldown.charge(cooldown, delay);
            }
            cooldown = ShotCooldown.tick(cooldown);
        }
        return shots;
    }

    /**
     * Shots fired across {@code ticks} ticks of sustained fire, ignoring the start:
     * a weapon that begins cold gets its first shot away immediately, which would
     * otherwise show up as one extra shot in every measurement.
     */
    private static int sustainedShots(float delay, int ticks)
    {
        int warmUp = 100;
        return shotsOverTicks(delay, warmUp + ticks) - shotsOverTicks(delay, warmUp);
    }

    @Test
    void wholeTickDelayFiresOncePerDelay()
    {
        assertEquals(20, sustainedShots(1F, 20));
        assertEquals(10, sustainedShots(2F, 20));
    }

    @Test
    void subTickDelayPutsSeveralShotsIntoOneTick()
    {
        // 2400 RPM is half a tick per shot, so twenty ticks carry forty shots.
        assertEquals(40, sustainedShots(0.5F, 20));
        assertEquals(80, sustainedShots(0.25F, 20));
    }

    @Test
    void rateThatDoesNotDivideIntoTicksDoesNotDrift()
    {
        // 800 RPM is 1.5 ticks per shot: sixty ticks must carry forty shots, which
        // only holds if the leftover half tick is carried instead of rounded away.
        assertEquals(40, sustainedShots(1.5F, 60));
        // Rounding 1.5 up to 2 ticks, as the driveable banks used to, would give 30.
        assertEquals(400, sustainedShots(1.5F, 600));
    }

    @Test
    void coldWeaponFiresItsFirstShotWithoutWaiting()
    {
        assertEquals(1, shotsOverTicks(20F, 1));
    }

    @Test
    void idleWeaponDoesNotBankShots()
    {
        float cooldown = ShotCooldown.charge(0F, 0.5F);
        for (int tick = 0; tick < 100; tick++)
            cooldown = ShotCooldown.tick(cooldown);
        // A spent cooldown settles in (-1, 0] however long the weapon rests, so
        // resuming fire can never unload a hundred ticks' worth of shots at once.
        assertTrue(cooldown > -1F && cooldown <= 0F);
    }

    @Test
    void delayIsHeldAboveZeroSoFiringLoopsTerminate()
    {
        assertEquals(ShotCooldown.MIN_DELAY, ShotCooldown.clampDelay(0F));
        assertEquals(ShotCooldown.MIN_DELAY, ShotCooldown.clampDelay(-5F));
        assertTrue(ShotCooldown.charge(0F, 0F) > 0F);
    }

    @Test
    void readinessTracksTheSignOfTheCooldown()
    {
        assertTrue(ShotCooldown.isReady(0F));
        assertTrue(ShotCooldown.isReady(-0.5F));
        assertFalse(ShotCooldown.isReady(0.25F));
    }

    @Test
    void displayTicksRoundsAPartTickUpAndNeverBelowZero()
    {
        assertEquals(0, ShotCooldown.displayTicks(0F));
        assertEquals(0, ShotCooldown.displayTicks(-0.75F));
        assertEquals(1, ShotCooldown.displayTicks(0.25F));
        assertEquals(2, ShotCooldown.displayTicks(1.5F));
    }
}
