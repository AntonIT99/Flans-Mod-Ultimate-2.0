package com.flansmodultimate.common;

import com.flansmodultimate.common.explosions.ExplosionScaling;
import com.flansmodultimate.common.explosions.FragmentationModel;
import com.flansmodultimate.common.types.ShootableType.EnumFragType;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionScalingTest
{
    private static final double CRATER = ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE;
    private static final double BLAST = ModCommonConfig.DEFAULT_BLAST_RADIUS_REFERENCE;

    @ParameterizedTest
    @CsvSource({
        "'.50 cal HE/HEI', 0.002, 0.2, 0.5",
        "'20 mm HE', 0.010, 1.0, 2.0",
        "'88 mm HE', 1.000, 4.0, 6.0",
        "'150 mm HE', 4.400, 7.0, 10.0"
    })
    void craterScaleMatchesOrdnance(String round, float charge, float low, float high)
    {
        float radius = ExplosionScaling.craterRadius(CRATER, charge);
        assertTrue(radius >= low - 0.1F && radius <= high + 0.1F, round + ": " + radius);
    }

    @ParameterizedTest
    @CsvSource({
        "'.50 cal HE/HEI', 0.002, 1.0, 2.0",
        "'20 mm HE', 0.010, 3.0, 5.0",
        "'88 mm HE', 1.000, 15.0, 25.0",
        "'150 mm HE', 4.400, 30.0, 50.0"
    })
    void blastScaleMatchesOrdnance(String round, float charge, float low, float high)
    {
        float radius = ExplosionScaling.blastRadius(BLAST, charge);
        assertTrue(radius >= low && radius <= high, round + ": " + radius);
    }

    @Test
    void heavyChargeStillHasPlayableBlastAndCrater()
    {
        assertTrue(ExplosionScaling.craterRadius(CRATER, 2250F) < 85F);
        assertTrue(ExplosionScaling.blastRadius(BLAST, 2250F) < 150F);
        assertTrue(ExplosionScaling.blastRadius(BLAST, 2250F) > 100F);
    }

    @Test
    void blastAndCraterAreMonotonicAndContinuous()
    {
        float[] masses = {0.0004F, 0.002F, 0.03F, 1F, 4.4F, 5F, 20F, 300F, 2250F, 11021F};
        for (int i = 1; i < masses.length; i++)
        {
            assertTrue(ExplosionScaling.craterRadius(CRATER, masses[i])
                > ExplosionScaling.craterRadius(CRATER, masses[i - 1]));
            assertTrue(ExplosionScaling.blastRadius(BLAST, masses[i])
                > ExplosionScaling.blastRadius(BLAST, masses[i - 1]));
        }
        float knee = ExplosionScaling.FLATTEN_KNEE_MASS_KG;
        assertEquals(ExplosionScaling.blastRadius(BLAST, knee),
            ExplosionScaling.blastRadius(BLAST, Math.nextDown(knee)), 0.001F);
        assertEquals(ExplosionScaling.blastRadius(BLAST, knee),
            ExplosionScaling.blastRadius(BLAST, Math.nextUp(knee)), 0.001F);
    }

    @Test
    void legacyCraterInverseRoundTrips()
    {
        for (float radius : new float[] {0.5F, 2F, 5.5F, 9.98F, 20F, 49F, 120F})
            assertEquals(radius, ExplosionScaling.craterRadius(CRATER,
                ExplosionScaling.chargeForCraterRadius(radius, CRATER)), radius * 0.01F);
    }

    @Test
    void invalidInputsHaveNoEffect()
    {
        assertEquals(0F, ExplosionScaling.craterRadius(CRATER, 0F));
        assertEquals(0F, ExplosionScaling.blastRadius(BLAST, -1F));
        assertEquals(0F, ExplosionScaling.chargeForCraterRadius(0F, CRATER));
        assertEquals(0F, ExplosionScaling.blastDamage(80D, 0F));
        assertEquals(0F, ExplosionScaling.fragPeakDamage(Float.NaN));
        assertEquals(0D, FragmentationModel.create(EnumFragType.STD_FRAG, 0D, 0D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL).queryRadius());
    }

    @Test
    void moreMetalCreatesMoreFragmentsButPeakIsAPropertyOfTheCasing()
    {
        var light = FragmentationModel.create(EnumFragType.HE_SHELL, 1D, 2000D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL);
        var heavy = FragmentationModel.create(EnumFragType.HE_SHELL, 1D, 9000D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL);
        assertTrue(heavy.fragmentCount() > light.fragmentCount());
        assertTrue(heavy.queryRadius() > light.queryRadius());
        assertEquals(light.peakDamage(), heavy.peakDamage());
    }

    @Test
    void chargeChangesFragmentEnergyWithoutArtificialRadiusPowerLaw()
    {
        var weak = FragmentationModel.create(EnumFragType.HE_SHELL, 0.1D, 1000D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL);
        var strong = FragmentationModel.create(EnumFragType.HE_SHELL, 0.5D, 1000D, 0D, 0D,
            FragmentationModel.Pattern.RADIAL);
        assertTrue(strong.energyLength() > weak.energyLength());
        assertEquals(weak.peakDamage(), strong.peakDamage());
    }
}
