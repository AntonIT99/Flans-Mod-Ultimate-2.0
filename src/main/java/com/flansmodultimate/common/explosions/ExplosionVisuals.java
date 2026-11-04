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
    public static final float FULL_CRATER_RADIUS = 24.0F;
    private static final float MIN_LIFETIME_SCALE = 0.4F;
    private static final float MAX_LIFETIME_SCALE = 1.45F;
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
    /**
     * Small rounds keep the 2.1 crater-driven look in full up to this crater radius; past it they
     * blend towards the charge-scaled sizing, which heavy charges use alone from
     * {@link #CHARGE_SCALED_CRATER_RADIUS}. A 150 mm shell sits about halfway.
     */
    public static final float LEGACY_LOOK_CRATER_RADIUS = 2.0F;
    public static final float CHARGE_SCALED_CRATER_RADIUS = 18.0F;
    private static final float LEGACY_FULL_CRATER_RADIUS = 12.0F;
    private static final float LEGACY_MAX_LIFETIME_SCALE = 1.6F;
    private static final float LEGACY_MAX_COUNT_SCALE = 3.0F;
    private static final float LEGACY_HOT_FIREBALL_SHARE = 0.4F;

    // The fire layers keep one size; the smoke layers grow back towards their 2.1 volume on small and medium shells.
    public static final List<Layer> LAYERS = List.of(new Layer(FlanParticles.FM_SMOKE_BURST, 3F, 0F, 0, 0F, 0.5F, 8, 0.55F, 0.01F, 0.2F, 1F),
        new Layer(FlanParticles.LARGE_EXPLODE, 3F, 0.25F, 4, 0.18F, 0.25F, 4, 0.18F, 0.005F, 0.2F, 0.7F), new Layer(FlanParticles.FM_FLAME, 2F, 0.5F, 18, 0.15F, 0.5F, 18, 0.15F, 0.015F, 0.35F, 0.35F),
        new Layer(FlanParticles.CLOUD, 3F, 0.8F, 20, 0.5F, 1.2F, 28, 0.75F, 0.025F, 0.45F, 1F), new Layer(FlanParticles.FM_AFTERBURN, 4F, 0.5F, 12, 0.15F, 0.5F, 12, 0.15F, 0.08F, 0.55F, 0.5F),
        new Layer(FlanParticles.LARGE_SMOKE, 6F, 0.6F, 16, 0.45F, 1.0F, 24, 0.8F, 0.015F, 0.6F, 1F));

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
        return ExplosionScaling.chargeForCraterRadius(boundedRadius(radius), ModCommonConfig.DEFAULT_CRATER_RADIUS_REFERENCE);
    }

    private static float boundedRadius(float radius)
    {
        return Mth.clamp(Float.isFinite(radius) ? radius : 0F, 0F, 256F);
    }

    /**
     * Whole hot-burst diameter, including sprite scatter; one block represents roughly one metre.
     * The crater is the floor, so a blast never looks smaller than the hole it digs.
     */
    public static float fireballDiameter(float radius)
    {
        return Mth.clamp(Math.max(4.5F * (float) Math.cbrt(charge(radius)), boundedRadius(radius)), 0.5F, 96F);
    }

    /**
     * How much of the 2.1 crater-driven look an explosion keeps: one for small rounds, falling to zero
     * for heavy charges, so the two sizings never jump at a threshold.
     */
    public static float legacyWeight(float radius)
    {
        float r = boundedRadius(radius);
        return Mth.clamp((CHARGE_SCALED_CRATER_RADIUS - r) / (CHARGE_SCALED_CRATER_RADIUS - LEGACY_LOOK_CRATER_RADIUS), 0F, 1F);
    }

    private static float blend(float radius, float chargeScaled, float legacy)
    {
        return Mth.lerp(legacyWeight(radius), chargeScaled, legacy);
    }

    /** As {@link #blend}, but the fireball leaves the 2.1 size sooner than the smoke does: a medium shell keeps 2.1 smoke volume around a compact fireball. */
    private static float fireballBlend(float radius, float chargeScaled, float legacy)
    {
        float weight = legacyWeight(radius);
        return Mth.lerp(weight * weight, chargeScaled, legacy);
    }

    /**
     * Presentation target for rising smoke, independent of the hot gas and damage envelopes, but at
     * least half again the crater radius; medium charges lean towards the 2.1 stem height.
     */
    public static float cloudHeight(float radius)
    {
        float chargeScaled = Math.min(96F, Math.max(8F * (float) Math.cbrt(charge(radius)), 1.5F * boundedRadius(radius)));
        return blend(radius, chargeScaled, Math.min(96F, 2.4F * boundedRadius(radius)));
    }

    /** The column climbs in a few seconds, then keeps billowing as its puffs drift up. */
    public static int cloudRiseTicks(float radius)
    {
        return Mth.clamp(Mth.ceil(12F + cloudHeight(radius) * 0.7F), 16, 80);
    }

    public static float intensity(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= BRIEF_CRATER_RADIUS)
            return 0F;

        float t = (craterRadius - BRIEF_CRATER_RADIUS) / (FULL_CRATER_RADIUS - BRIEF_CRATER_RADIUS);
        return Mth.sqrt(Mth.clamp(t, 0F, 1F));
    }

    private static float legacyIntensity(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= BRIEF_CRATER_RADIUS)
            return 0F;
        return Mth.sqrt(Mth.clamp((craterRadius - BRIEF_CRATER_RADIUS) / (LEGACY_FULL_CRATER_RADIUS - BRIEF_CRATER_RADIUS), 0F, 1F));
    }

    public static float lifetimeScale(float craterRadius)
    {
        return blend(craterRadius, Mth.lerp(intensity(craterRadius), MIN_LIFETIME_SCALE, MAX_LIFETIME_SCALE),
            Mth.lerp(legacyIntensity(craterRadius), MIN_LIFETIME_SCALE, LEGACY_MAX_LIFETIME_SCALE));
    }

    public static float fragLifetimeScale(float craterRadius)
    {
        return lifetimeScale(craterRadius) * FRAG_LIFETIME_SHARE;
    }

    public static float countScale(float craterRadius)
    {
        return blend(craterRadius, Mth.lerp(intensity(craterRadius), 1.0F, MAX_COUNT_SCALE), Mth.lerp(legacyIntensity(craterRadius), 1.0F, LEGACY_MAX_COUNT_SCALE));
    }

    public static int scaledCount(int authoredCount, float craterRadius)
    {
        if (authoredCount <= 0)
            return 0;
        return Math.min(MAX_SCALED_COUNT, Mth.ceil(authoredCount * countScale(craterRadius)));
    }

    /**
     * Quad size of each fireball puff. Charge-scaled, a four-block-wide base sprite occupies 70% of the
     * envelope and centre scatter supplies the rest; small rounds keep the 2.1 crater-linear puff.
     */
    public static float fireballScale(float craterRadius)
    {
        float legacy = Mth.clamp(0.3F + boundedRadius(craterRadius) * 0.5F, 0.45F, 64F);
        return fireballBlend(craterRadius, fireballDiameter(craterRadius) * 0.175F, legacy);
    }

    /** How far the fireball puffs are scattered from the centre, per axis. */
    public static float fireballSpread(float craterRadius)
    {
        return fireballBlend(craterRadius, fireballDiameter(craterRadius) * 0.15F, boundedRadius(craterRadius) * 0.3F);
    }

    public static int fireballCount(float craterRadius)
    {
        int chargeScaled = Mth.clamp(Mth.ceil(fireballDiameter(craterRadius) / 3F), 1, 8);
        int legacy = Mth.clamp(Mth.ceil(boundedRadius(craterRadius) / 2F), 1, 16);
        return Math.round(fireballBlend(craterRadius, chargeScaled, legacy));
    }

    public static int fireballDurationTicks(float craterRadius)
    {
        int chargeScaled = Mth.clamp(Mth.ceil(5D * Math.pow(charge(craterRadius), 1D / 6D)), 3, 40);
        int legacy = Mth.clamp(1 + Mth.ceil(boundedRadius(craterRadius) * 7.5F), 1, 200);
        return Math.max(1, Math.round(fireballBlend(craterRadius, chargeScaled, legacy)));
    }

    public static float flashScale(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= 0F)
            return 0F;
        return fireballBlend(craterRadius, fireballDiameter(craterRadius) * 0.5F, Mth.clamp(0.5F + boundedRadius(craterRadius) * 1.5F, 0.9F, 30F));
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
        return blend(craterRadius, cloudHeight(craterRadius) / 5F, Math.min(boundedRadius(craterRadius), 40F));
    }

    public static float landmarkRange(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= 0F)
            return 0F;
        return (float) Math.min(craterRadius * LANDMARK_RANGE_PER_CRATER, ModCommonConfig.explosionParticleRange());
    }

    public static int fireballHotTicks(float craterRadius, boolean fiery)
    {
        if (!fiery && (!Float.isFinite(craterRadius) || craterRadius < MIN_AFTERGLOW_CRATER_RADIUS))
            return 0;
        int duration = fireballDurationTicks(craterRadius);
        if (fiery)
            return Math.round(fireballBlend(craterRadius, duration * 2F, duration));
        // 2.1 burnt only the opening share of a small round's fireball before it cooled to the grey puff.
        return Math.max(1, Mth.ceil(duration * fireballBlend(craterRadius, 1F, LEGACY_HOT_FIREBALL_SHARE)));
    }

    public static float hotStemShare(boolean fiery)
    {
        return fiery ? 0.15F : 0F;
    }

    public static float afterglowScale(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius < MIN_AFTERGLOW_CRATER_RADIUS)
            return 0F;
        return fireballBlend(craterRadius, fireballDiameter(craterRadius) * 0.22F, flashScale(craterRadius) * 1.4F);
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
        // Dust rolls out at least to the crater edge.
        float reach = Math.min(48F, Math.max(cloudHeight(craterRadius) * 0.6F, boundedRadius(craterRadius)));
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
        return fireballStemSteps(craterRadius) == 0 ? 0 : Mth.clamp(Mth.ceil(stagedSizingRadius(craterRadius) / 1.5F), 3, 8);
    }

    public static int mushroomCapPuffs(float craterRadius)
    {
        return !Float.isFinite(craterRadius) || craterRadius < MIN_MUSHROOM_CAP_CRATER_RADIUS ? 0 : Mth.clamp(Mth.ceil(stagedSizingRadius(craterRadius) * 4F), 8, 48);
    }

    public static float mushroomCapRadius(float craterRadius)
    {
        return mushroomCapPuffs(craterRadius) == 0 ? 0F : cloudHeight(craterRadius) * 0.3F;
    }

    /**
     * One extra particle layer. The count and spread are given for the charge-scaled look and for the
     * 2.1 look, and blended by {@link #legacyWeight}; the spread is a share of the fireball diameter
     * in the first and of the crater radius in the second.
     */
    @SuppressWarnings("java:S107") // A plain tuning table row.
    public record Layer(String particle, float minCraterRadius, float countPerRadius, int maxCount, float spreadShare, float legacyCountPerRadius, int legacyMaxCount,
        float legacySpreadShare, float speedShare, float upwardBias, float lifetimeShare)
    {
        public int count(float craterRadius)
        {
            if (!Float.isFinite(craterRadius) || craterRadius < minCraterRadius)
                return 0;
            float perRadius = blend(craterRadius, countPerRadius, legacyCountPerRadius);
            int max = Math.round(blend(craterRadius, maxCount, legacyMaxCount));
            if (perRadius <= 0F || max <= 0)
                return 0;
            return Mth.clamp(Mth.ceil(craterRadius * perRadius), 1, max);
        }

        /** How far this layer is scattered from the centre. */
        public float spread(float craterRadius)
        {
            return blend(craterRadius, fireballDiameter(craterRadius) * spreadShare, boundedRadius(craterRadius) * legacySpreadShare);
        }
    }

}
