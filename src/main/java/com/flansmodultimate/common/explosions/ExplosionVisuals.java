package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

import java.util.List;

/**
 * Bounded conventional-explosion visuals, using a charge-normalized reference radius.
 * {@link #visualRadius(float, float)} separates cloud dimensions from crater balance settings;
 * legacy radius-only explosions retain an approximate charge fallback. Blast and fragmentation
 * cues still follow their actual gameplay envelopes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExplosionVisuals
{
    public static final float BRIEF_CRATER_RADIUS = 0.5F;
    public static final float FULL_CRATER_RADIUS = 40.0F;
    private static final float MIN_LIFETIME_SCALE = 0.4F;
    private static final float MAX_LIFETIME_SCALE = 1.3F;
    private static final float FRAG_LIFETIME_SHARE = 0.65F;
    private static final float MAX_COUNT_SCALE = 2.0F;
    private static final int MAX_SCALED_COUNT = 120;
    public static final float MIN_SHOCKWAVE_BLAST_RADIUS = 6.0F;
    private static final float BLAST_RADIUS_PER_SHOCKWAVE = 1.2F;
    private static final int MAX_SHOCKWAVE_PARTICLES = 48;
    public static final float MIN_FRAG_SPRAY_RADIUS = 4.0F;
    private static final float SPARKS_PER_FRAG_AREA = 1.0F;
    public static final int DEFAULT_MAX_FRAG_SPARKS = 512;
    public static final float MIN_SMOKE_COLUMN_CRATER_RADIUS = 6.0F;
    private static final float CRATER_RADIUS_PER_COLUMN = 1.5F;
    private static final int MAX_SMOKE_COLUMN_PARTICLES = 10;
    public static final float MIN_AFTERGLOW_CRATER_RADIUS = 1.5F;
    public static final float MIN_DUST_SKIRT_CRATER_RADIUS = 3.0F;
    public static final float MIN_FIREBALL_STEM_CRATER_RADIUS = MIN_SMOKE_COLUMN_CRATER_RADIUS;
    public static final float MIN_MUSHROOM_CAP_CRATER_RADIUS = 16.0F;
    private static final float LANDMARK_RANGE_PER_CRATER = 16.0F;
    public static final float MAX_LANDMARK_RANGE = 256.0F;

    public static final List<Layer> LAYERS = List.of(new Layer(FlanParticles.LARGE_EXPLODE, 3F, 0.25F, 4, 0.18F, 0.005F, 0.2F, 0.7F),
        new Layer(FlanParticles.FM_FLAME, 2F, 0.5F, 12, 0.15F, 0.015F, 0.35F, 0.35F), new Layer(FlanParticles.CLOUD, 3F, 0.8F, 20, 0.35F, 0.025F, 0.45F, 1F),
        new Layer(FlanParticles.FM_AFTERBURN, 4F, 0.5F, 12, 0.15F, 0.08F, 0.55F, 0.5F), new Layer(FlanParticles.LARGE_SMOKE, 6F, 0.6F, 16, 0.25F, 0.015F, 0.6F, 1F));

    /** Known filler mass takes precedence over gameplay crater sizing; legacy radii remain usable. */
    public static float visualRadius(float craterRadius, float chargeKg)
    {
        if (Float.isFinite(chargeKg) && chargeKg > 0F)
            return ExplosionScaling.craterRadius(ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE, chargeKg);
        return Float.isFinite(craterRadius) ? Math.max(0F, craterRadius) : 0F;
    }

    private static float charge(float radius)
    {
        // Bound before inversion to avoid overflow for extreme legacy radii.
        return ExplosionScaling.chargeForCraterRadius(Mth.clamp(Float.isFinite(radius) ? radius : 0F, 0F, 256F), ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE);
    }

    /** Whole hot-burst diameter, including sprite scatter; one block represents roughly one metre. */
    public static float fireballDiameter(float radius)
    {
        return Mth.clamp(3.5F * (float) Math.cbrt(charge(radius)), 0.35F, 96F);
    }

    /** Presentation target for rising smoke, independent of the hot gas and damage envelopes. */
    public static float cloudHeight(float radius)
    {
        return Math.min(80F, 5F * (float) Math.cbrt(charge(radius)));
    }

    /** Cloud ascent takes several seconds instead of assembling a finished stem in under one. */
    public static int cloudRiseTicks(float radius)
    {
        return Mth.clamp(Mth.ceil(40F + cloudHeight(radius) * 3.2F), 40, 240);
    }

    public static float intensity(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= BRIEF_CRATER_RADIUS)
            return 0F;

        float t = (craterRadius - BRIEF_CRATER_RADIUS) / (FULL_CRATER_RADIUS - BRIEF_CRATER_RADIUS);
        return Mth.sqrt(Mth.clamp(t, 0F, 1F));
    }

    public static float lifetimeScale(float craterRadius)
    {
        return Mth.lerp(intensity(craterRadius), MIN_LIFETIME_SCALE, MAX_LIFETIME_SCALE);
    }

    public static float fragLifetimeScale(float craterRadius)
    {
        return lifetimeScale(craterRadius) * FRAG_LIFETIME_SHARE;
    }

    public static float countScale(float craterRadius)
    {
        return Mth.lerp(intensity(craterRadius), 1.0F, MAX_COUNT_SCALE);
    }

    public static int scaledCount(int authoredCount, float craterRadius)
    {
        if (authoredCount <= 0)
            return 0;
        return Math.min(MAX_SCALED_COUNT, Mth.ceil(authoredCount * countScale(craterRadius)));
    }

    /** Four-block-wide base sprite occupies 70% of the envelope; centre scatter supplies the rest. */
    public static float fireballScale(float craterRadius)
    {
        return fireballDiameter(craterRadius) * 0.175F;
    }

    public static int fireballCount(float craterRadius)
    {
        return Mth.clamp(Mth.ceil(fireballDiameter(craterRadius) / 3F), 1, 8);
    }

    public static int fireballDurationTicks(float craterRadius)
    {
        return Mth.clamp(Mth.ceil(5D * Math.pow(charge(craterRadius), 1D / 6D)), 3, 40);
    }

    public static float flashScale(float craterRadius)
    {
        return Float.isFinite(craterRadius) && craterRadius > 0F ? fireballDiameter(craterRadius) * 0.5F : 0F;
    }

    public static int shockwaveCount(float blastRadius)
    {
        if (!Float.isFinite(blastRadius) || blastRadius < MIN_SHOCKWAVE_BLAST_RADIUS)
            return 0;
        return Mth.clamp(Mth.ceil(blastRadius / BLAST_RADIUS_PER_SHOCKWAVE), 1, MAX_SHOCKWAVE_PARTICLES);
    }

    public static int fragSparkCount(float fragRadius, float fragIntensity, double effectiveFragments, double sparkMultiplier, int maxFragSparks)
    {
        if (!Float.isFinite(fragRadius) || fragRadius < MIN_FRAG_SPRAY_RADIUS || !Float.isFinite(fragIntensity) || fragIntensity <= 0F || !Double.isFinite(effectiveFragments)
            || effectiveFragments < 1D || !Double.isFinite(sparkMultiplier) || sparkMultiplier <= 0D || maxFragSparks <= 0)
            return 0;

        double sparks = (double) fragRadius * fragRadius * SPARKS_PER_FRAG_AREA * Math.sqrt(fragIntensity);
        double availableFragments = Math.floor(effectiveFragments);
        double scaledSparks = Math.ceil(Math.min(Math.ceil(sparks), availableFragments) * sparkMultiplier);
        return (int) Math.min(Math.min(scaledSparks, availableFragments), maxFragSparks);
    }

    public static int smokeColumnCount(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius < MIN_SMOKE_COLUMN_CRATER_RADIUS)
            return 0;
        return Mth.clamp(Mth.ceil(craterRadius / CRATER_RADIUS_PER_COLUMN), 1, MAX_SMOKE_COLUMN_PARTICLES);
    }

    public static float stagedSizingRadius(float craterRadius)
    {
        return cloudHeight(craterRadius) / 5F;
    }

    public static float landmarkRange(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= 0F)
            return 0F;
        return Math.min(craterRadius * LANDMARK_RANGE_PER_CRATER, MAX_LANDMARK_RANGE);
    }

    public static int fireballHotTicks(float craterRadius, boolean fiery)
    {
        if (!fiery && (!Float.isFinite(craterRadius) || craterRadius < MIN_AFTERGLOW_CRATER_RADIUS))
            return 0;
        return fireballDurationTicks(craterRadius) * (fiery ? 2 : 1);
    }

    public static float hotStemShare(boolean fiery)
    {
        return fiery ? 0.15F : 0F;
    }

    public static float afterglowScale(float craterRadius)
    {
        return Float.isFinite(craterRadius) && craterRadius >= MIN_AFTERGLOW_CRATER_RADIUS ? fireballDiameter(craterRadius) * 0.22F : 0F;
    }

    public static int dustSkirtWaves(float craterRadius)
    {
        return !Float.isFinite(craterRadius) || craterRadius < MIN_DUST_SKIRT_CRATER_RADIUS ? 0 : Mth.clamp(Mth.ceil(stagedSizingRadius(craterRadius)), 2, 6);
    }

    public static int dustSkirtPuffsPerWave(float craterRadius)
    {
        return dustSkirtWaves(craterRadius) == 0 ? 0 : Mth.clamp(Mth.ceil(stagedSizingRadius(craterRadius) * 4F), 8, 24);
    }

    public static float dustSkirtReach(float craterRadius, float blastRadius)
    {
        if (dustSkirtWaves(craterRadius) == 0)
            return 0F;
        float reach = Math.min(48F, cloudHeight(craterRadius) * 0.6F);
        return Float.isFinite(blastRadius) && blastRadius > 0F ? Math.min(reach, blastRadius) : reach;
    }

    public static int fireballStemSteps(float craterRadius)
    {
        return !Float.isFinite(craterRadius) || craterRadius < MIN_FIREBALL_STEM_CRATER_RADIUS ? 0 : Mth.clamp(Mth.ceil(cloudHeight(craterRadius) / 2F), 4, 18);
    }

    public static float fireballStemHeight(float craterRadius)
    {
        return fireballStemSteps(craterRadius) == 0 ? 0F : cloudHeight(craterRadius);
    }

    public static int fireballStemPuffsPerStep(float craterRadius)
    {
        return fireballStemSteps(craterRadius) == 0 ? 0 : Mth.clamp(Mth.ceil(stagedSizingRadius(craterRadius) / 2F), 2, 4);
    }

    public static int mushroomCapPuffs(float craterRadius)
    {
        return !Float.isFinite(craterRadius) || craterRadius < MIN_MUSHROOM_CAP_CRATER_RADIUS ? 0 : Mth.clamp(Mth.ceil(stagedSizingRadius(craterRadius) * 3F), 8, 32);
    }

    public static float mushroomCapRadius(float craterRadius)
    {
        return mushroomCapPuffs(craterRadius) == 0 ? 0F : cloudHeight(craterRadius) * 0.18F;
    }

    public record Layer(String particle, float minCraterRadius, float countPerRadius, int maxCount, float spreadShare, float speedShare, float upwardBias, float lifetimeShare)
    {
        public int count(float craterRadius)
        {
            if (!Float.isFinite(craterRadius) || craterRadius < minCraterRadius)
                return 0;
            return Mth.clamp(Mth.ceil(craterRadius * countPerRadius), 1, maxCount);
        }
    }

}
