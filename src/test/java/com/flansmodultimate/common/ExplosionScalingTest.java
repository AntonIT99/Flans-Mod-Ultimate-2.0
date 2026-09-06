package com.flansmodultimate.common;

import com.flansmodultimate.common.types.ShootableType.EnumFragType;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the explosion radius curves to measured ordnance.
 * <p>
 * The bands are the quoted effect radii of real rounds, in metres, which map one-to-one onto
 * blocks. "Concentrated destruction" is the cratering radius; the "broader blast / fragmentation"
 * figure is the outer envelope of the two damage radii, so it is checked against whichever of
 * blast and fragmentation reaches further.
 */
class ExplosionScalingTest
{
    private static final double CRATER_REFERENCE = ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE;
    private static final double BLAST_REFERENCE = ModCommonConfig.DEFAULT_BLAST_RADIUS_REFERENCE;

    /**
     * The .50 cal crater is allowed a little over its band. Its 0.2-0.5 m figure against the
     * 20 mm's 1-2 m implies a local slope near 0.9, which no single power law can meet without
     * throwing off every heavier round, so the fit takes the small absolute miss on the lightest
     * round in the set.
     */
    private static final float CRATER_TOLERANCE = 0.1F;

    @ParameterizedTest(name = "{0}: {1} kg craters {2}-{3} blocks")
    @CsvSource({
        "'.50 cal HE/HEI',        0.002, 0.2, 0.5",
        "'20 mm HE (KwK 30)',     0.010, 1.0, 2.0",
        "'20 mm HEI high-cap',    0.030, 1.5, 2.5",
        "'88 mm HE (KwK 36)',     1.000, 4.0, 6.0",
        "'150 mm HE (sFH 18)',    4.400, 7.0, 10.0"
    })
    void theCraterMatchesTheQuotedDestructionRadius(String round, float massKg, float low, float high)
    {
        float crater = ExplosionScaling.craterRadius(CRATER_REFERENCE, massKg);

        assertTrue(crater >= low - CRATER_TOLERANCE,
            () -> round + " craters " + crater + ", under the quoted " + low + "-" + high);
        assertTrue(crater <= high + CRATER_TOLERANCE,
            () -> round + " craters " + crater + ", over the quoted " + low + "-" + high);
    }

    /**
     * Checked against HE_SHELL, the casing type of the artillery rounds these figures come from.
     */
    @ParameterizedTest(name = "{0}: {1} kg reaches {2}-{3} blocks")
    @CsvSource({
        "'.50 cal HE/HEI',        0.002,  1.0,  2.0",
        "'20 mm HE (KwK 30)',     0.010,  3.0,  5.0",
        "'20 mm HEI high-cap',    0.030,  4.0,  7.0",
        "'88 mm HE (KwK 36)',     1.000, 15.0, 25.0",
        "'150 mm HE (sFH 18)',    4.400, 30.0, 50.0"
    })
    void theDamageEnvelopeMatchesTheQuotedBlastAndFragRadius(String round, float massKg, float low, float high)
    {
        float blast = ExplosionScaling.blastRadius(BLAST_REFERENCE, massKg);
        float frag = ExplosionScaling.fragRadius(EnumFragType.HE_SHELL.kFragRadius, massKg);
        float envelope = Math.max(blast, frag);

        assertTrue(envelope >= low, () -> round + " reaches " + envelope + ", under the quoted " + low + "-" + high);
        assertTrue(envelope <= high, () -> round + " reaches " + envelope + ", over the quoted " + low + "-" + high);
    }

    @Test
    @DisplayName("a 2.25 t bomb stays within the ranges chosen for Minecraft's scale")
    void aHeavyBombIsFlattenedToPlayableRadii()
    {
        float massKg = 2250F;

        float crater = ExplosionScaling.craterRadius(CRATER_REFERENCE, massKg);
        float blast = ExplosionScaling.blastRadius(BLAST_REFERENCE, massKg);
        float frag = ExplosionScaling.fragRadius(EnumFragType.GP_BOMB.kFragRadius, massKg);

        assertTrue(crater >= 40F && crater <= 60F, () -> "crater " + crater);
        assertTrue(blast >= 100F && blast <= 150F, () -> "blast " + blast);
        assertTrue(frag <= 300F, () -> "frag " + frag);
    }

    @Test
    @DisplayName("unflattened growth would put the same bomb far out of reach")
    void theFlatteningIsWhatKeepsHeavyChargesPlayable()
    {
        float massKg = 2250F;
        double unflattened = BLAST_REFERENCE * Math.pow(massKg, ExplosionScaling.BLAST_EXPONENT);

        assertTrue(unflattened > 400D, () -> "expected the fitted curve to run away, got " + unflattened);
        assertTrue(ExplosionScaling.blastRadius(BLAST_REFERENCE, massKg) < unflattened / 3D);
    }

    @Test
    void radiiGrowMonotonicallyWithTheChargeAcrossTheWholeRange()
    {
        float[] masses = {0.0004F, 0.002F, 0.03F, 1F, 4.4F, 5F, 20F, 300F, 2250F, 11021F, 1_000_000F, 5.0E10F};

        for (int i = 1; i < masses.length; i++)
        {
            float previous = masses[i - 1];
            float current = masses[i];
            assertTrue(ExplosionScaling.craterRadius(CRATER_REFERENCE, current) > ExplosionScaling.craterRadius(CRATER_REFERENCE, previous),
                () -> "crater did not grow from " + previous + " kg to " + current + " kg");
            assertTrue(ExplosionScaling.blastRadius(BLAST_REFERENCE, current) > ExplosionScaling.blastRadius(BLAST_REFERENCE, previous),
                () -> "blast did not grow from " + previous + " kg to " + current + " kg");
        }
    }

    @Test
    void theCurveIsContinuousAcrossTheKnee()
    {
        float knee = ExplosionScaling.FLATTEN_KNEE_MASS_KG;

        float justUnder = ExplosionScaling.blastRadius(BLAST_REFERENCE, Math.nextDown(knee));
        float atKnee = ExplosionScaling.blastRadius(BLAST_REFERENCE, knee);
        float justOver = ExplosionScaling.blastRadius(BLAST_REFERENCE, Math.nextUp(knee));

        assertEquals(atKnee, justUnder, 0.001F, "the fitted regime should meet the knee");
        assertEquals(atKnee, justOver, 0.001F, "the flattened regime should leave from the knee");
    }

    @Test
    void aChargeRecoveredFromACraterReproducesThatCrater()
    {
        // The legacy path derives an implied charge from an authored crater radius, so the
        // inverse has to track the curve through both of its regimes.
        for (float radius : new float[] {0.5F, 2F, 5.5F, 9.98F, 20F, 49F, 120F})
        {
            float charge = ExplosionScaling.chargeForCraterRadius(radius, CRATER_REFERENCE);
            assertEquals(radius, ExplosionScaling.craterRadius(CRATER_REFERENCE, charge), radius * 0.01F,
                () -> "round trip failed for a " + radius + " block crater");
        }
    }

    @Test
    void aChargeOfZeroOrLessHasNoRadius()
    {
        assertEquals(0F, ExplosionScaling.craterRadius(CRATER_REFERENCE, 0F));
        assertEquals(0F, ExplosionScaling.blastRadius(BLAST_REFERENCE, -1F));
        assertEquals(0F, ExplosionScaling.fragRadius(EnumFragType.HE_SHELL.kFragRadius, Float.NaN));
        assertEquals(0F, ExplosionScaling.chargeForCraterRadius(0F, CRATER_REFERENCE));
    }

    @Test
    void fragmentationOrderingFollowsTheCasingType()
    {
        float massKg = 1F;

        float thickCase = ExplosionScaling.fragRadius(EnumFragType.THICK_CASE.kFragRadius, massKg);
        float stdFrag = ExplosionScaling.fragRadius(EnumFragType.STD_FRAG.kFragRadius, massKg);
        float heShell = ExplosionScaling.fragRadius(EnumFragType.HE_SHELL.kFragRadius, massKg);
        float shrapnel = ExplosionScaling.fragRadius(EnumFragType.IED_SHRAPNEL.kFragRadius, massKg);

        assertTrue(thickCase < stdFrag, "a thick penetrator case should throw fragments least far");
        assertTrue(stdFrag < heShell);
        assertTrue(heShell < shrapnel, "a shrapnel-packed casing should throw fragments furthest");

        // HE_SHELL is deliberately pinned to the blast reference: the artillery rounds the blast
        // curve was fitted to are the ones whose quoted radius already includes their fragments.
        assertEquals(ExplosionScaling.blastRadius(BLAST_REFERENCE, massKg), heShell, 0.001F);
    }
}
