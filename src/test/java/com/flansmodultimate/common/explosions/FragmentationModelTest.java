package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.types.ShootableType.EnumFragType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentationModelTest
{
    @Test
    void m67CanWoundAtItsCasualtyRadiusButNotAtLongRange()
    {
        var m67 = FragmentationModel.create(EnumFragType.HIGH_FRAG, 0.205D, 0D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL);
        assertTrue(m67.damage(m67.peakDamage(), 1D, 5D, 1D) >= 20D);
        assertTrue(m67.damage(m67.peakDamage(), 1D, 15D, 1D) > 1D);
        assertTrue(m67.damage(m67.peakDamage(), 1D, 15D, 1D) < 20D);
        assertEquals(0D, m67.damage(m67.peakDamage(), 1D, 100D, 1D));
    }

    @Test
    void claymoreConcentratesFragmentsInItsForwardFan()
    {
        var claymore = FragmentationModel.create(EnumFragType.PREFORMED, 0.934D, 0D, 0D, 700D,
            FragmentationModel.Pattern.FORWARD_FAN);
        var fan = claymore.pattern();
        assertTrue(fan.distribution(50D, 1.5D, 0D, 1D, 0D, 0D) > 0D);
        assertEquals(0D, fan.distribution(0D, 1.5D, 50D, 1D, 0D, 0D));
        assertEquals(0D, fan.distribution(-50D, 1.5D, 0D, 1D, 0D, 0D));
        assertTrue(claymore.damage(claymore.peakDamage(), 1D, 50D, 1D) >= 20D);
        assertEquals(0D, claymore.damage(claymore.peakDamage(), 1D, 50D, 0D));
    }

    @Test
    void aheadUsesProjectileSpeedAndForwardCone()
    {
        var slow = FragmentationModel.create(EnumFragType.PREFORMED, 0.001D, 750D, 0D, 152D,
            FragmentationModel.Pattern.FORWARD_CONE);
        var ahead = FragmentationModel.create(EnumFragType.PREFORMED, 0.001D, 750D, 0D, 152D,
            FragmentationModel.Pattern.FORWARD_CONE, 1050D);
        assertTrue(ahead.energyLength() > slow.energyLength());
        assertTrue(ahead.damage(ahead.peakDamage(), 1D, 25D, 1D) >= 20D);
        assertEquals(0D, ahead.pattern().distribution(0D, 0D, -25D, 0D, 0D, 1D));
    }

    @Test
    void coverReducesHitProbabilityAndDamage()
    {
        var shell = FragmentationModel.create(EnumFragType.HE_SHELL, 1D, 9400D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL);
        assertTrue(shell.damage(shell.peakDamage(), 0.25D, 15D, 1D)
            < shell.damage(shell.peakDamage(), 1D, 15D, 1D));
        assertEquals(0D, shell.damage(shell.peakDamage(), 0D, 15D, 1D));
    }

    @Test
    void sMineBandNeedsHeightForNearbyStandingTarget()
    {
        var band = FragmentationModel.Pattern.HORIZONTAL_BAND;
        assertEquals(0D, band.distribution(5D, 1.6D, 0D, 0D, 0D, 0D));
        assertEquals(1D, band.distribution(5D, 0.6D, 0D, 0D, 0D, 0D));
    }
}
