package com.flansmodultimate.common.driveables.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionVehicleDamageResolverTest
{
    @Test
    void grenadeKeepsFullChannelsForSoftTargetsButCannotDamageArmour()
    {
        ExplosionVehicleDamageResolver.DamageChannels soft = resolve(0F, 0.06F, 0.5D, 50F, 20F);
        assertEquals(50F, soft.blastDamage());
        assertEquals(20F, soft.fragmentationDamage());

        ExplosionVehicleDamageResolver.DamageChannels tank = resolve(26F, 0.06F, 0.5D, 50F, 20F);
        assertEquals(0F, tank.totalDamage());
    }

    @Test
    void smallHeBlocksFragmentationAndNormallyFailsLightArmourBlastThreshold()
    {
        ExplosionVehicleDamageResolver.DamageChannels result = resolve(13F, 0.01F, 0.3D, 80F, 30F);
        assertEquals(0F, result.fragmentationDamage());
        assertEquals(0F, result.blastDamage());
    }

    @Test
    void largeHeAndBombsCanExceedTheArmouredThreshold()
    {
        assertTrue(resolve(13F, 1F, 0.5D, 80F, 20F).blastDamage() > 70F);
        assertTrue(resolve(100F, 250F, 2D, 500F, 50F).blastDamage() > 400F);
    }

    @Test
    void missingExplosiveMassCannotPassArmouredGateButDoesNotAffectSoftTargets()
    {
        assertEquals(0F, resolve(40F, null, 0.5D, 80F, 20F).totalDamage());
        assertEquals(100F, resolve(0F, null, 0.5D, 80F, 20F).totalDamage());
    }

    @Test
    void zeroDistanceIsClampedAndPressureRemainsFinite()
    {
        double pressure = ExplosionVehicleDamageResolver.peakPressureKPa(250D, 0D, 0.5D);
        assertTrue(Double.isFinite(pressure));
        assertTrue(pressure > 0D);
    }

    private static ExplosionVehicleDamageResolver.DamageChannels resolve(float armour, Float mass,
                                                                           double distance, float blast,
                                                                           float fragmentation)
    {
        return ExplosionVehicleDamageResolver.resolve(armour, mass, distance, blast, fragmentation, 150D, 0.5D);
    }
}
