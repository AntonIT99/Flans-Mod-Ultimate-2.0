package com.flansmodultimate.common;

import com.flansmodultimate.common.explosions.ExplosionScaling;
import com.flansmodultimate.common.explosions.ExplosionVisuals;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionVisualsTest
{
    private static float radius(float massKg)
    {
        return ExplosionScaling.craterRadius(ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE, massKg);
    }

    @ParameterizedTest
    @CsvSource({"0.01, 0.754, 3, 1.077", "0.039, 1.187, 3, 1.696", "0.686, 3.087, 5, 4.410", "1.38, 3.897, 6, 5.567", "10, 7.541, 8, 10.772", "125, 17.5, 12, 25", "500, 27.779, 15, 39.685",
        "2250, 45.863, 19, 65.519"})
    void representativeChargesHaveCompactBriefHotCores(float mass, float diameter, int ticks, float height)
    {
        float radius = radius(mass);
        assertEquals(diameter, ExplosionVisuals.fireballDiameter(radius), 0.02F);
        assertEquals(ticks, ExplosionVisuals.fireballDurationTicks(radius));
        assertEquals(height, ExplosionVisuals.cloudHeight(radius), 0.02F);
        // Four-block base sprite plus the full +/- 15% centre scatter fits the envelope.
        assertEquals(diameter, ExplosionVisuals.fireballScale(radius) * 4F + ExplosionVisuals.fireballDiameter(radius) * 0.3F, 0.02F);
    }

    @Test
    void knownChargeIgnoresCraterBalanceAndLegacyRadiusStillWorks()
    {
        float normal = ExplosionVisuals.visualRadius(29F, 125F);
        assertEquals(normal, ExplosionVisuals.visualRadius(100F, 125F));
        assertEquals(normal, ExplosionVisuals.visualRadius(0F, 125F));
        assertEquals(17.5F, ExplosionVisuals.fireballDiameter(normal), 0.01F);
        assertEquals(8F, ExplosionVisuals.visualRadius(8F, 0F));
        assertEquals(8F, ExplosionVisuals.visualRadius(8F, Float.NaN));
        assertEquals(0F, ExplosionVisuals.visualRadius(Float.NaN, 0F));
    }

    @Test
    void bombSmokeRisesOverSecondsAndDoesNotKeepBurning()
    {
        float bomb = radius(125F);
        assertEquals(120, ExplosionVisuals.cloudRiseTicks(bomb));
        assertEquals(25F, ExplosionVisuals.fireballStemHeight(bomb), 0.01F);
        assertEquals(5F, ExplosionVisuals.stagedSizingRadius(bomb), 0.01F);
        assertEquals(0F, ExplosionVisuals.hotStemShare(false));
        assertEquals(12, ExplosionVisuals.fireballHotTicks(bomb, false));
        assertEquals(24, ExplosionVisuals.fireballHotTicks(bomb, true));
        assertTrue(ExplosionVisuals.mushroomCapRadius(bomb) < 5F);
        assertTrue(ExplosionVisuals.fireballStemSteps(bomb) * ExplosionVisuals.fireballStemPuffsPerStep(bomb) <= 72);
    }

    @Test
    void smallCalibreAndTankShellsDoNotRaiseTallColumns()
    {
        for (float mass : new float[]{0.002F, 0.01F, 0.039F, 0.686F, 0.9F})
        {
            assertEquals(0, ExplosionVisuals.fireballStemSteps(radius(mass)));
            assertEquals(0, ExplosionVisuals.mushroomCapPuffs(radius(mass)));
        }
        for (float mass : new float[]{0.002F, 0.01F})
        {
            assertEquals(0, ExplosionVisuals.fireballHotTicks(radius(mass), false));
            for (ExplosionVisuals.Layer layer : ExplosionVisuals.LAYERS)
                assertEquals(0, layer.count(radius(mass)));
        }
    }

    @Test
    void dustAndFragmentCuesStayInsideTheirOwnBudgets()
    {
        float bomb = radius(125F);
        assertEquals(1F, ExplosionVisuals.dustSkirtReach(bomb, 1F));
        assertEquals(15F, ExplosionVisuals.dustSkirtReach(bomb, 100F), 0.01F);
        assertEquals(0, ExplosionVisuals.scaledCount(0, bomb));
        assertTrue(ExplosionVisuals.scaledCount(1000, bomb) <= 120);
        assertEquals(0, ExplosionVisuals.shockwaveCount(2F));
        assertTrue(ExplosionVisuals.shockwaveCount(30F) > 0);
    }

    @ParameterizedTest
    @ValueSource(floats = {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -1F, 0F, 1.0e9F})
    void invalidAndExtremeInputsStayFiniteAndBounded(float radius)
    {
        assertTrue(Float.isFinite(ExplosionVisuals.fireballDiameter(radius)));
        assertTrue(ExplosionVisuals.fireballDiameter(radius) <= 96F);
        assertTrue(ExplosionVisuals.fireballDurationTicks(radius) >= 3);
        assertTrue(ExplosionVisuals.fireballDurationTicks(radius) <= 40);
        assertTrue(ExplosionVisuals.fireballCount(radius) <= 8);
        assertTrue(ExplosionVisuals.cloudHeight(radius) <= 80F);
        assertTrue(ExplosionVisuals.cloudRiseTicks(radius) <= 240);
        assertTrue(ExplosionVisuals.dustSkirtWaves(radius) * ExplosionVisuals.dustSkirtPuffsPerWave(radius) <= 144);
        assertTrue(ExplosionVisuals.mushroomCapPuffs(radius) <= 32);
    }

    @Test
    @DisplayName("Fragmentation spray scales with intensity, not just reach")
    void fragmentationSprayRespectsIntensity()
    {
        float radius = 30F;
        assertEquals(0, ExplosionVisuals.fragSparkCount(radius, 0F, 5000D, 0.25D, 2048), "a charge that throws no fragments should spray none");
        assertTrue(ExplosionVisuals.fragSparkCount(radius, 4F, 5000D, 0.25D, 2048) > ExplosionVisuals.fragSparkCount(radius, 0.8F, 5000D, 0.25D, 2048),
            "a frag shell should spray more than a blast charge reaching the same distance");
        assertEquals(0, ExplosionVisuals.fragSparkCount(2F, 4F, 5000D, 0.25D, 2048), "a spray inside the fireball would not be visible");
        assertTrue(ExplosionVisuals.fragSparkCount(4F, 1F, 5000D, 0.25D, 2048) > 0, "a grenade-sized fragmentation envelope should have a visible spray");
        assertEquals(ExplosionVisuals.DEFAULT_MAX_FRAG_SPARKS, ExplosionVisuals.fragSparkCount(1000F, 4F, 5000D, 0.25D, ExplosionVisuals.DEFAULT_MAX_FRAG_SPARKS),
            "even an extreme burst must stay within the particle cap");
        assertEquals(450, ExplosionVisuals.fragSparkCount(30F, 4F, 5000D, 0.25D, 2048), "the larger fragmentation envelope should produce a dense spray");
        assertEquals(120, ExplosionVisuals.fragSparkCount(30F, 4F, 5000D, 0.25D, 120), "the client cap should limit an individual explosion");
        assertEquals(0, ExplosionVisuals.fragSparkCount(30F, 4F, 5000D, 0.25D, 0), "the client should be able to disable fragmentation sparks");
        assertEquals(11, ExplosionVisuals.fragSparkCount(30F, 4F, 42.9D, 0.25D, 2048), "the multiplier applies after limiting sparks to effective fragments");
        assertEquals(42, ExplosionVisuals.fragSparkCount(30F, 4F, 42.9D, 4D, 2048), "sparks must never exceed the integer number of effective fragments");
        assertEquals(0, ExplosionVisuals.fragSparkCount(30F, 4F, 0.9D, 0.25D, 2048), "fewer than one effective fragment cannot produce a spark");
        assertEquals(0, ExplosionVisuals.fragSparkCount(30F, 4F, 5000D, 0D, 2048), "zero multiplier disables fragmentation sparks");
    }

}
