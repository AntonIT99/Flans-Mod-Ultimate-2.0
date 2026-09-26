package com.flansmodultimate.common;

import com.flansmodultimate.common.explosions.ExplosionScaling;
import com.flansmodultimate.common.explosions.ExplosionVisuals;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the explosion visuals to the rounds they were tuned against.
 * <p>
 * The crater radii here are the ones {@link ExplosionScaling} produces for each charge at the
 * default references, so these tests fail if the damage curves move underneath the visuals and
 * leave, say, a tank shell raising a demolition charge's smoke column.
 */
class ExplosionVisualsTest
{
    private static final double CRATER_REFERENCE = ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE;
    private static final double BLAST_REFERENCE = ModCommonConfig.DEFAULT_BLAST_RADIUS_REFERENCE;

    /** Filler masses in kg TNT equivalent, matching ExplosionScalingTest's reference set. */
    private static final float FIFTY_CAL_KG = 0.002F;
    private static final float TWENTY_MM_KG = 0.010F;
    private static final float EIGHTY_EIGHT_MM_KG = 1.0F;
    private static final float STURMTIGER_CHARGE_KG = 10.0F;

    private static float crater(float massKg)
    {
        return ExplosionScaling.craterRadius(CRATER_REFERENCE, massKg);
    }

    private static float blast(float massKg)
    {
        return ExplosionScaling.blastRadius(BLAST_REFERENCE, massKg);
    }

    @Test
    @DisplayName("Small-calibre HE clears well before its authored lifetime")
    void smallCalibreRoundsAreBrief()
    {
        // Both are the complaint this tuning answers: a few grams of filler should flash and go.
        assertTrue(ExplosionVisuals.lifetimeScale(crater(FIFTY_CAL_KG)) < 0.55F,
            ".50 cal should clear in about half its authored particle time");
        assertTrue(ExplosionVisuals.lifetimeScale(crater(TWENTY_MM_KG)) < 0.75F,
            "20 mm should still be brief");
    }

    @Test
    @DisplayName("A 20 mm round is visibly more than a .50 cal")
    void theTwoSmallRoundsAreToldApart()
    {
        // Linear interpolation over the crater range crushes both into the floor, which is what
        // the square root in intensity() exists to avoid.
        float fiftyCal = ExplosionVisuals.lifetimeScale(crater(FIFTY_CAL_KG));
        float twentyMm = ExplosionVisuals.lifetimeScale(crater(TWENTY_MM_KG));
        assertTrue(twentyMm > fiftyCal * 1.15F,
            "the 20 mm should read as a step up from the .50 cal, got " + fiftyCal + " vs " + twentyMm);
    }

    @Test
    @DisplayName("A 10 kg charge lingers longer than its particles were authored for")
    void aHeavyChargeIsPersistent()
    {
        float heavy = crater(STURMTIGER_CHARGE_KG);
        assertTrue(ExplosionVisuals.lifetimeScale(heavy) > 1.0F,
            "a demolition charge should outlast its authored particle time");
        assertTrue(ExplosionVisuals.countScale(heavy) > 2.5F,
            "a demolition charge should throw several times the authored particle count");
    }

    @ParameterizedTest(name = "{0} kg -> brighter, bigger, longer than anything lighter")
    @CsvSource({"0.002, 0.010", "0.010, 1.0", "1.0, 10.0"})
    @DisplayName("Every visual grows monotonically with the charge")
    void heavierChargesAreAlwaysMoreImpressive(float lighterKg, float heavierKg)
    {
        float lighter = crater(lighterKg);
        float heavier = crater(heavierKg);

        assertTrue(ExplosionVisuals.lifetimeScale(heavier) > ExplosionVisuals.lifetimeScale(lighter));
        assertTrue(ExplosionVisuals.countScale(heavier) > ExplosionVisuals.countScale(lighter));
        assertTrue(ExplosionVisuals.fireballScale(heavier) > ExplosionVisuals.fireballScale(lighter));
        assertTrue(ExplosionVisuals.fireballDurationTicks(heavier) > ExplosionVisuals.fireballDurationTicks(lighter));
        assertTrue(ExplosionVisuals.fireballCount(heavier) >= ExplosionVisuals.fireballCount(lighter));
    }

    @Test
    @DisplayName("A small round gets one fireball burst, a heavy charge a rolling one")
    void theFireballDurationSeparatesTheTwoEnds()
    {
        // A vanilla explosion puff lives 6-9 ticks, so a duration inside that leaves a single
        // burst with no follow-up waves at all - which is what "brief" has to mean in practice.
        assertTrue(ExplosionVisuals.fireballDurationTicks(crater(FIFTY_CAL_KG)) <= 6,
            ".50 cal should not outlive a single puff");
        assertTrue(ExplosionVisuals.fireballDurationTicks(crater(STURMTIGER_CHARGE_KG)) >= 60,
            "a 10 kg charge should keep burning for several seconds");
    }

    @Test
    @DisplayName("Blast and fragmentation visuals appear only once those envelopes are worth showing")
    void theEnvelopeVisualsAreGatedOnTheirOwnRadii()
    {
        assertEquals(0, ExplosionVisuals.shockwaveCount(blast(TWENTY_MM_KG)),
            "a 20 mm blast envelope is too small to read as a wave");
        assertTrue(ExplosionVisuals.shockwaveCount(blast(EIGHTY_EIGHT_MM_KG)) > 0,
            "an 88 mm shell reaches far enough past its crater to be worth drawing");
        assertTrue(ExplosionVisuals.shockwaveCount(blast(STURMTIGER_CHARGE_KG))
            > ExplosionVisuals.shockwaveCount(blast(EIGHTY_EIGHT_MM_KG)),
            "a heavier charge drives a wider wave");
    }

    @Test
    @DisplayName("Fragmentation spray scales with intensity, not just reach")
    void fragmentationSprayRespectsIntensity()
    {
        float radius = 30F;
        assertEquals(0, ExplosionVisuals.fragSparkCount(radius, 0F),
            "a charge that throws no fragments should spray none");
        assertTrue(ExplosionVisuals.fragSparkCount(radius, 4F) > ExplosionVisuals.fragSparkCount(radius, 0.8F),
            "a frag shell should spray more than a blast charge reaching the same distance");
        assertEquals(0, ExplosionVisuals.fragSparkCount(2F, 4F),
            "a spray inside the fireball would not be visible");
    }

    @Test
    @DisplayName("Only a demolition charge leaves a column standing")
    void theSmokeColumnMarksTheHeaviestCharges()
    {
        assertEquals(0, ExplosionVisuals.smokeColumnCount(crater(TWENTY_MM_KG)));
        assertEquals(0, ExplosionVisuals.smokeColumnCount(crater(EIGHTY_EIGHT_MM_KG)),
            "ordinary tank gunnery should not raise a column");
        assertTrue(ExplosionVisuals.smokeColumnCount(crater(STURMTIGER_CHARGE_KG)) > 0);
        assertEquals(0.2F, ExplosionVisuals.smokeColumnLifetimeScale(crater(STURMTIGER_CHARGE_KG)), 0.0001F,
            "an explosion should use only a brief tail of the smoke-screen particle");
    }

    @ParameterizedTest(name = "a crater radius of {0} produces no degenerate visuals")
    @ValueSource(floats = {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -1F, 0F, 1.0e9F})
    @DisplayName("Nonsensical and extreme radii stay bounded")
    void degenerateRadiiAreHandled(float craterRadius)
    {
        assertTrue(Float.isFinite(ExplosionVisuals.lifetimeScale(craterRadius)));
        assertTrue(ExplosionVisuals.lifetimeScale(craterRadius) > 0F);
        assertTrue(Float.isFinite(ExplosionVisuals.fireballScale(craterRadius)));
        assertTrue(ExplosionVisuals.fireballCount(craterRadius) >= 1);
        assertTrue(ExplosionVisuals.fireballDurationTicks(craterRadius) >= 1);
        assertTrue(ExplosionVisuals.smokeColumnCount(craterRadius) >= 0);
        assertFalse(ExplosionVisuals.scaledCount(20, craterRadius) > 120,
            "a scaled particle count must stay within its client budget");
    }

    @Test
    @DisplayName("The extra layers stay off for the rounds players fire by the hundred")
    void smallCalibreRoundsGetNoExtraLayers()
    {
        // These are the rounds the user was already happy with, and they are also the ones fired
        // most often, so nothing here may start costing a client particles.
        for (float massKg : new float[] {FIFTY_CAL_KG, TWENTY_MM_KG})
        {
            float crater = crater(massKg);
            for (ExplosionVisuals.Layer layer : ExplosionVisuals.LAYERS)
                assertEquals(0, layer.count(crater),
                    layer.particle() + " should not appear on a " + massKg + " kg round");
        }
    }

    @Test
    @DisplayName("A 10 kg charge gets every extra layer, a tank shell only some")
    void theExtraLayersSwitchOnProgressively()
    {
        float shell = crater(EIGHTY_EIGHT_MM_KG);
        float charge = crater(STURMTIGER_CHARGE_KG);

        long onForShell = ExplosionVisuals.LAYERS.stream().filter(l -> l.count(shell) > 0).count();
        long onForCharge = ExplosionVisuals.LAYERS.stream().filter(l -> l.count(charge) > 0).count();

        assertEquals(ExplosionVisuals.LAYERS.size(), onForCharge,
            "a demolition charge should get the full set of particle types");
        assertTrue(onForShell > 0 && onForShell < onForCharge,
            "an 88 mm shell should get some variety but not all of it, got " + onForShell);
    }

    @Test
    @DisplayName("The extra layers bring genuine variety, not one sprite repeated")
    void theExtraLayersAreDistinctParticleTypes()
    {
        assertEquals(ExplosionVisuals.LAYERS.size(),
            ExplosionVisuals.LAYERS.stream().map(ExplosionVisuals.Layer::particle).distinct().count(),
            "each layer should contribute a different particle type");

        for (ExplosionVisuals.Layer layer : ExplosionVisuals.LAYERS)
            assertTrue(FlanParticles.resolve(layer.particle()).isPresent(),
                layer.particle() + " is not a particle the client can resolve");
    }

    @ParameterizedTest(name = "a crater radius of {0} keeps every extra layer bounded")
    @ValueSource(floats = {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -1F, 0F, 1.0e9F})
    @DisplayName("Extreme radii cannot make the extra layers run away")
    void theExtraLayersStayBounded(float craterRadius)
    {
        for (ExplosionVisuals.Layer layer : ExplosionVisuals.LAYERS)
        {
            int count = layer.count(craterRadius);
            assertTrue(count >= 0 && count <= layer.maxCount(),
                layer.particle() + " produced " + count + " particles");
        }
    }

    @Test
    @DisplayName("The staged layers stay off for the rounds players fire by the hundred")
    void smallCalibreRoundsGetNoStagedLayers()
    {
        for (float massKg : new float[] {FIFTY_CAL_KG, TWENTY_MM_KG})
        {
            float crater = crater(massKg);
            assertEquals(0F, ExplosionVisuals.afterglowScale(crater), massKg + " kg should only flash white");
            assertEquals(0, ExplosionVisuals.dustSkirtWaves(crater), massKg + " kg should not roll out a skirt");
            assertEquals(0, ExplosionVisuals.fireballStemSteps(crater), massKg + " kg should not raise a stem");
            assertEquals(0, ExplosionVisuals.mushroomCapPuffs(crater), massKg + " kg should not mushroom");
        }
    }

    @Test
    @DisplayName("A tank shell rolls out dust, a 10 kg charge rises and mushrooms")
    void theStagedLayersSwitchOnProgressively()
    {
        float shell = crater(EIGHTY_EIGHT_MM_KG);
        float charge = crater(STURMTIGER_CHARGE_KG);

        assertTrue(ExplosionVisuals.afterglowScale(shell) > 0F);
        assertTrue(ExplosionVisuals.dustSkirtWaves(shell) > 0, "an 88 mm shell should scour the ground");
        assertEquals(0, ExplosionVisuals.fireballStemSteps(shell), "ordinary tank gunnery should not raise a stem");
        assertEquals(0, ExplosionVisuals.mushroomCapPuffs(shell));

        assertTrue(ExplosionVisuals.dustSkirtWaves(charge) > ExplosionVisuals.dustSkirtWaves(shell));
        assertTrue(ExplosionVisuals.fireballStemSteps(charge) > 0, "a demolition charge should raise a stem");
        assertTrue(ExplosionVisuals.mushroomCapPuffs(charge) > 0, "a demolition charge should mushroom");
        assertTrue(ExplosionVisuals.mushroomCapRadius(charge) < ExplosionVisuals.fireballStemHeight(charge),
            "the cap should be narrower than the stem is tall");
    }

    @Test
    @DisplayName("The dust skirt never claims a reach the blast does not have")
    void theDustSkirtStopsAtTheBlastRadius()
    {
        float charge = crater(STURMTIGER_CHARGE_KG);
        assertTrue(ExplosionVisuals.dustSkirtReach(charge, blast(STURMTIGER_CHARGE_KG)) <= blast(STURMTIGER_CHARGE_KG));
        assertTrue(ExplosionVisuals.dustSkirtReach(charge, 1F) >= charge,
            "the skirt should still clear the crater itself");
    }

    @ParameterizedTest(name = "a crater radius of {0} keeps every staged layer bounded")
    @ValueSource(floats = {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -1F, 0F, 1.0e9F})
    @DisplayName("Extreme radii cannot make the staged layers run away")
    void theStagedLayersStayBounded(float craterRadius)
    {
        assertTrue(Float.isFinite(ExplosionVisuals.afterglowScale(craterRadius)));
        assertTrue(ExplosionVisuals.dustSkirtWaves(craterRadius) <= 8);
        assertTrue(ExplosionVisuals.dustSkirtPuffsPerWave(craterRadius) <= 40);
        assertTrue(ExplosionVisuals.dustSkirtReach(craterRadius, craterRadius) <= 64F);
        assertTrue(ExplosionVisuals.fireballStemSteps(craterRadius) <= 18);
        assertTrue(ExplosionVisuals.fireballStemPuffsPerStep(craterRadius) <= 6);
        assertTrue(ExplosionVisuals.fireballStemHeight(craterRadius) <= 96F);
        assertTrue(ExplosionVisuals.mushroomCapPuffs(craterRadius) <= 64);
    }

    @Test
    @DisplayName("Puffs stop growing where the column and cap stop growing")
    void stagedPuffSizesStayInProportionWithTheCappedShapes()
    {
        float charge = crater(STURMTIGER_CHARGE_KG);
        assertEquals(charge, ExplosionVisuals.stagedSizingRadius(charge), 0.0001F,
            "ordinary charges should size their puffs from their own crater");

        // A legacy ExplosionRadius of 200 under a 256 block cap: the stem is already at its ceiling,
        // so the puffs drawing it must be too.
        float extreme = 200F;
        assertEquals(ExplosionVisuals.fireballStemHeight(extreme), ExplosionVisuals.fireballStemHeight(40F), 0.0001F);
        assertEquals(ExplosionVisuals.stagedSizingRadius(40F), ExplosionVisuals.stagedSizingRadius(extreme), 0.0001F,
            "puffs should stop growing at the crater where the stem stops growing");
    }

    @Test
    @DisplayName("Heavy charges are drawn from further away, but never beyond the packet range")
    void theLandmarkRangeGrowsWithTheChargeUpToThePacketRange()
    {
        assertTrue(ExplosionVisuals.landmarkRange(crater(STURMTIGER_CHARGE_KG))
            > ExplosionVisuals.landmarkRange(crater(EIGHTY_EIGHT_MM_KG)));
        assertTrue(ExplosionVisuals.landmarkRange(crater(STURMTIGER_CHARGE_KG)) > 128F,
            "a demolition charge's column should outlast the default particle distance");
        assertEquals(ExplosionVisuals.MAX_LANDMARK_RANGE, ExplosionVisuals.landmarkRange(1.0e9F), 0.0001F);
        assertEquals(0F, ExplosionVisuals.landmarkRange(Float.NaN));
    }

    @Test
    @DisplayName("Small-calibre HE keeps its grey pop; heavier HE burns first, then cools")
    void ordinaryFireballsBurnBeforeTheyCool()
    {
        for (float massKg : new float[] {FIFTY_CAL_KG, TWENTY_MM_KG})
            assertEquals(0, ExplosionVisuals.fireballHotTicks(crater(massKg), false),
                massKg + " kg should not change its fireball");

        float shell = crater(EIGHTY_EIGHT_MM_KG);
        int hot = ExplosionVisuals.fireballHotTicks(shell, false);
        assertTrue(hot >= 1, "the opening burst of a shell should be fire");
        assertTrue(hot < ExplosionVisuals.fireballDurationTicks(shell), "a shell's fireball should cool to smoke");
    }

    @ParameterizedTest(name = "a fire explosion of {0} kg burns for its whole fireball")
    @ValueSource(floats = {0.002F, 0.06F, 1.0F, 10.0F})
    @DisplayName("A fire explosion never cools to the grey puff")
    void fireExplosionsBurnThroughout(float massKg)
    {
        float crater = crater(massKg);
        assertEquals(ExplosionVisuals.fireballDurationTicks(crater), ExplosionVisuals.fireballHotTicks(crater, true));
    }

    @Test
    @DisplayName("A fire explosion's stem burns further up than an ordinary one's")
    void fireExplosionsBurnHigherUpTheStem()
    {
        assertTrue(ExplosionVisuals.hotStemShare(true) > ExplosionVisuals.hotStemShare(false));
        assertTrue(ExplosionVisuals.hotStemShare(false) > 0F && ExplosionVisuals.hotStemShare(true) <= 1F);
    }

    @Test
    @DisplayName("An unauthored particle count stays unauthored")
    void aZeroCountIsNotInvented()
    {
        assertEquals(0, ExplosionVisuals.scaledCount(0, crater(STURMTIGER_CHARGE_KG)),
            "a pack that asked for no flares should not be given any");
    }
}
