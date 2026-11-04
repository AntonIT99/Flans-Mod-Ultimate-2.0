package com.flansmodultimate.common;

import com.flansmodultimate.common.explosions.ExplosionScaling;
import com.flansmodultimate.common.explosions.ExplosionVisuals;
import com.flansmodultimate.config.ModCommonConfig;
import net.minecraft.util.Mth;
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
    @CsvSource({"125, 29.170, 12, 43.755", "500, 46.304, 15, 69.456", "2250, 76.447, 19, 96"})
    void heavyChargesHaveCompactBriefHotCores(float mass, float diameter, int ticks, float height)
    {
        float radius = radius(mass);
        assertEquals(diameter, ExplosionVisuals.fireballDiameter(radius), 0.05F);
        assertEquals(ticks, ExplosionVisuals.fireballDurationTicks(radius));
        assertEquals(height, ExplosionVisuals.cloudHeight(radius), 0.05F);
        // Four-block base sprite plus the full +/- 15% centre scatter fits the envelope.
        assertEquals(diameter, ExplosionVisuals.fireballScale(radius) * 4F + ExplosionVisuals.fireballDiameter(radius) * 0.3F, 0.02F);
    }

    @Test
    void smallRoundsKeepThe21LookAndMediumShellsSitBetween()
    {
        float twentyMm = radius(0.01F);
        assertEquals(1F, ExplosionVisuals.legacyWeight(twentyMm));
        assertEquals(0.8F, ExplosionVisuals.fireballScale(twentyMm), 0.01F);
        assertEquals(1, ExplosionVisuals.fireballCount(twentyMm));
        assertEquals(9, ExplosionVisuals.fireballDurationTicks(twentyMm));
        assertEquals(2.0F, ExplosionVisuals.flashScale(twentyMm), 0.01F);

        float hummel = radius(5.1F);
        float weight = ExplosionVisuals.legacyWeight(hummel);
        assertTrue(weight > 0.4F && weight < 0.6F, "a 150 mm shell should sit about halfway between both looks");
        float legacyScale = 0.3F + hummel * 0.5F;
        float chargeScale = ExplosionVisuals.fireballDiameter(hummel) * 0.175F;
        assertTrue(ExplosionVisuals.fireballScale(hummel) > chargeScale && ExplosionVisuals.fireballScale(hummel) < legacyScale);
        assertTrue(ExplosionVisuals.fireballDurationTicks(hummel) < 1 + Mth.ceil(hummel * 7.5F), "the 2.1 fireball of a 150 mm shell burnt for almost four seconds");
        assertTrue(ExplosionVisuals.fireballScale(hummel) < (chargeScale + legacyScale) / 2F, "the fireball should stay nearer the compact charge-scaled size");
        assertTrue(ExplosionVisuals.stagedSizingRadius(hummel) > ExplosionVisuals.cloudHeight(hummel) / 5F, "the smoke should keep much of its 2.1 volume");
        assertTrue(ExplosionVisuals.LAYERS.get(0).count(hummel) > 0, "a 150 mm shell should throw up the 2.1 smoke burst");

        assertEquals(0F, ExplosionVisuals.legacyWeight(radius(125F)), "bombs use the charge-scaled look alone");
    }

    @Test
    void knownChargeIgnoresCraterBalanceAndLegacyRadiusStillWorks()
    {
        float normal = ExplosionVisuals.visualRadius(29F, 125F);
        assertEquals(normal, ExplosionVisuals.visualRadius(100F, 125F));
        assertEquals(normal, ExplosionVisuals.visualRadius(0F, 125F));
        assertEquals(29.17F, ExplosionVisuals.fireballDiameter(normal), 0.05F);
        assertEquals(8F, ExplosionVisuals.visualRadius(8F, 0F));
        assertEquals(8F, ExplosionVisuals.visualRadius(8F, Float.NaN));
        assertEquals(0F, ExplosionVisuals.visualRadius(Float.NaN, 0F));
    }

    @Test
    void bombSmokeRisesOverSecondsAndDoesNotKeepBurning()
    {
        float bomb = radius(125F);
        assertEquals(73, ExplosionVisuals.cloudRiseTicks(bomb), 1, "a bomb column should climb in under four seconds");
        assertEquals(43.755F, ExplosionVisuals.fireballStemHeight(bomb), 0.05F);
        assertEquals(8.751F, ExplosionVisuals.stagedSizingRadius(bomb), 0.01F);
        assertEquals(0F, ExplosionVisuals.hotStemShare(false));
        assertEquals(12, ExplosionVisuals.fireballHotTicks(bomb, false));
        assertEquals(24, ExplosionVisuals.fireballHotTicks(bomb, true));
        assertTrue(ExplosionVisuals.mushroomCapRadius(bomb) < ExplosionVisuals.fireballStemHeight(bomb) / 2F);
        assertTrue(ExplosionVisuals.fireballStemSteps(bomb) * ExplosionVisuals.fireballStemPuffsPerStep(bomb) <= 144);
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
        assertEquals(bomb, ExplosionVisuals.dustSkirtReach(bomb, 100F), 0.01F, "dust should reach the crater edge");
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
        assertTrue(ExplosionVisuals.fireballDurationTicks(radius) >= 1);
        assertTrue(ExplosionVisuals.fireballDurationTicks(radius) <= 200);
        assertTrue(ExplosionVisuals.fireballCount(radius) <= 16);
        assertTrue(ExplosionVisuals.cloudHeight(radius) <= 96F);
        assertTrue(ExplosionVisuals.cloudRiseTicks(radius) <= 240);
        assertTrue(ExplosionVisuals.dustSkirtWaves(radius) * ExplosionVisuals.dustSkirtPuffsPerWave(radius) <= 144);
        assertTrue(ExplosionVisuals.mushroomCapPuffs(radius) <= 48);
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
