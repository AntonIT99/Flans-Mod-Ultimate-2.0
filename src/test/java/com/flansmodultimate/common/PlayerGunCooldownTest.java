package com.flansmodultimate.common;

import com.flansmodultimate.common.guns.ShotCooldown;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerGunCooldownTest
{
    @Test
    void fractionalReloadCompletesOnTheSameRoundedUpTickAsTheAmmoPlan()
    {
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        data.doGunReload(InteractionHand.MAIN_HAND, 2.4F);
        data.tickGunCooldowns();
        data.tickGunCooldowns();
        assertTrue(data.isReloading(InteractionHand.MAIN_HAND));
        assertTrue(data.getShootTime(InteractionHand.MAIN_HAND) > 0F);
        data.tickGunCooldowns();
        assertTrue(data.getShootTime(InteractionHand.MAIN_HAND) <= 0F);
        assertTrue(data.getShootTime(InteractionHand.OFF_HAND) <= 0F);
        assertFalse(data.isReloading(InteractionHand.MAIN_HAND));
    }

    @Test
    void switchingExpiresAfterItsOwnDurationDespiteAnOrdinaryTimerReset()
    {
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        data.beginSecondaryModeSwitch(InteractionHand.OFF_HAND, 3);
        data.setShootTime(InteractionHand.OFF_HAND, 0F);
        data.tickGunCooldowns();
        data.tickGunCooldowns();
        assertTrue(data.isSecondaryModeSwitching(InteractionHand.OFF_HAND));
        assertEquals(1F, data.getShootTime(InteractionHand.OFF_HAND));
        data.tickGunCooldowns();
        assertFalse(data.isSecondaryModeSwitching(InteractionHand.OFF_HAND));
        assertEquals(0F, data.getShootTime(InteractionHand.OFF_HAND));
    }

    @Test
    void highRateFiringRetainsTheFractionNeededForSeveralShotsPerTick()
    {
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        data.setShootTime(InteractionHand.MAIN_HAND, 0.2F);
        data.tickGunCooldowns();
        float cooldown = data.getShootTime(InteractionHand.MAIN_HAND);
        assertEquals(-0.8F, cooldown, 1E-6F);
        int shots = 0;
        while (ShotCooldown.isReady(cooldown))
        {
            cooldown = ShotCooldown.charge(cooldown, 0.2F);
            shots++;
        }
        assertEquals(5, shots);
    }

    @Test
    void anInstantModeSwitchDoesNotResetTheFiringRemainder()
    {
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        data.setShootTime(InteractionHand.MAIN_HAND, -0.4F);
        data.beginSecondaryModeSwitch(InteractionHand.MAIN_HAND, 0);
        assertEquals(-0.4F, data.getShootTime(InteractionHand.MAIN_HAND));
        assertFalse(data.isSecondaryModeSwitching(InteractionHand.MAIN_HAND));
    }
}
