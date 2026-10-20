package com.flansmodultimate.common.guns;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.common.PlayerData;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GunHandlingTimingTest
{
    // Constant inputs deliberately exercise every eligibility/visibility branch.
    @SuppressWarnings("ConstantValue")
    @Test
    void defaultTacticalBonusNeedsLiveMagazineAmmoRatherThanAnInternalOrSingleRoundLoad()
    {
        assertTrue(GunHandlingTiming.tacticalReload(true, 1, 30, -1F));
        assertFalse(GunHandlingTiming.tacticalReload(false, 1, 30, 50F));
        assertFalse(GunHandlingTiming.tacticalReload(true, 6, 1, -1F));
        assertFalse(GunHandlingTiming.tacticalReload(true, 1, 1, -1F));
        assertTrue(GunHandlingTiming.tacticalReload(true, 6, 1, 40F));
    }

    @Test
    void emptyExplicitAndDisabledReloadsSelectTheirOwnDuration()
    {
        assertEquals(64F, GunHandlingTiming.reloadTime(80F, -1F, 0.8F, true));
        assertEquals(50F, GunHandlingTiming.reloadTime(80F, 50F, 0.8F, true));
        assertEquals(80F, GunHandlingTiming.reloadTime(80F, 50F, 0.8F, false));
        assertEquals(80F, GunHandlingTiming.reloadTime(80F, 0F, 0.8F, true));
        assertEquals(80F, GunHandlingTiming.reloadTime(80F, -1F, 1F, true));
    }

    @Test
    void fractionalSwitchDurationsRoundUpAndRejectNonfiniteValues()
    {
        assertEquals(9, GunHandlingTiming.switchTicks(8.1F));
        assertEquals(0, GunHandlingTiming.switchTicks(-1F));
        assertEquals(0, GunHandlingTiming.switchTicks(Float.NaN));
        assertEquals(1200, GunHandlingTiming.switchTicks(10000F));
    }

    // Constant inputs deliberately exercise every eligibility/visibility branch.
    @SuppressWarnings("ConstantValue")
    @Test
    void secondaryOnlyAttachmentsRemainVisibleThroughBothTransitions()
    {
        assertTrue(GunHandlingTiming.attachmentVisible(false, false, false));
        assertFalse(GunHandlingTiming.attachmentVisible(true, false, false));
        assertTrue(GunHandlingTiming.attachmentVisible(true, true, false));
        assertTrue(GunHandlingTiming.attachmentVisible(true, false, true));
        assertEquals(0F, GunHandlingTiming.switchMovement(8, 8, 0F));
        assertEquals(1F, GunHandlingTiming.switchMovement(4, 8, 0F));
        assertEquals(0F, GunHandlingTiming.switchMovement(0, 8, 0F));
    }

    @Test
    void modeSwitchCooldownSurvivesShootTimerResetAndClearsTheOldBurstInOnlyItsOwnHand()
    {
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        data.setBurstRoundsRemaining(InteractionHand.MAIN_HAND, 3);
        data.setShootTime(InteractionHand.OFF_HAND, 2F);
        data.beginSecondaryModeSwitch(InteractionHand.MAIN_HAND, 8);
        data.setShootTime(InteractionHand.MAIN_HAND, 0F);
        assertEquals(8F, data.getShootTime(InteractionHand.MAIN_HAND));
        assertEquals(0, data.getBurstRoundsRemaining(InteractionHand.MAIN_HAND));
        assertEquals(2F, data.getShootTime(InteractionHand.OFF_HAND));
        assertTrue(data.isSecondaryModeSwitching(InteractionHand.MAIN_HAND));
        assertFalse(data.isSecondaryModeSwitching(InteractionHand.OFF_HAND));
    }

    @Test
    void delayedReloadOverlapsOnlyTheRemainingShotCooldown()
    {
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        // A 300-tick shot cooldown has already elapsed when fire-to-reload is requested.
        data.setShootTime(InteractionHand.MAIN_HAND, 0F);
        data.doGunReload(InteractionHand.MAIN_HAND, 80F);
        assertEquals(80F, data.getShootTime(InteractionHand.MAIN_HAND));
        assertEquals(80F, data.getShootTime(InteractionHand.OFF_HAND));

        // Reloading immediately cannot bypass the original shot cooldown or the other hand's longer wait.
        data.setShootTime(InteractionHand.MAIN_HAND, 299F);
        data.setShootTime(InteractionHand.OFF_HAND, 400F);
        data.doGunReload(InteractionHand.MAIN_HAND, 80F);
        assertEquals(299F, data.getShootTime(InteractionHand.MAIN_HAND));
        assertEquals(400F, data.getShootTime(InteractionHand.OFF_HAND));
    }

    @Test
    void switchPresentationExpiresWithoutChangingAimOrReloadState()
    {
        GunAnimations animations = new GunAnimations();
        animations.beginSecondaryModeSwitch(3);
        animations.update();
        assertEquals(2, animations.getSecondarySwitchRemaining());
        animations.update();
        animations.update();
        animations.update();
        assertEquals(0, animations.getSecondarySwitchRemaining());
        assertFalse(animations.isReloading());
        assertEquals(0F, animations.getGunRecoil());
    }
}
