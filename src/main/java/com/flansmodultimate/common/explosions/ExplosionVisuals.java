package com.flansmodultimate.common.explosions;

import com.flansmodultimate.common.FlanParticles;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

import java.util.List;

/**
 * How big an explosion <em>looks</em>, derived from how big it actually is.
 * <p>
 * The damage model already knows the three radii that matter - the crater it carves, the
 * overpressure envelope and how far fragments reach - so the visuals are shaped from those rather
 * than from a content pack's flat particle counts, which do not scale with the charge at all. One
 * class decides the whole budget so the layers stay in proportion to each other: a round whose
 * fireball is small should not also be the one leaving a smoke column standing.
 * <p>
 * The governing idea is that small-calibre HE is <em>brief</em>. A .50 cal or a 20 mm round puts a
 * few grams of filler into the air; it flashes, throws a little dirt and is over inside half a
 * second. Everything here therefore keys off {@link #intensity(float)}, which is near zero for
 * those rounds and only reaches one for a charge in the tens of kilograms. At the bottom of that
 * curve particles live a fraction of their authored lifetime and the extra layers switch off
 * entirely; at the top they live longer than authored, there are several times as many of them, and
 * the blast and fragmentation envelopes get visuals of their own so the radii that hurt you are
 * also the radii you can see.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExplosionVisuals
{
    /**
     * Crater radius at or below which an explosion is as brief as it gets. Sits just under the
     * ~0.5 block crater of a .50 cal HE round, so the lightest HE in the game is at the floor.
     */
    public static final float BRIEF_CRATER_RADIUS = 0.5F;
    /**
     * Crater radius at which an explosion is as impressive as it gets. Chosen as the ~12 block
     * crater of a 10 kg charge, so a Sturmtiger round sits exactly at the top of the curve and
     * anything heavier is already saturated rather than escalating without limit.
     */
    public static final float FULL_CRATER_RADIUS = 12.0F;

    /** Share of its authored lifetime a particle keeps at the brief end. */
    private static final float MIN_LIFETIME_SCALE = 0.25F;
    /** Multiple of its authored lifetime a particle gets at the impressive end. */
    private static final float MAX_LIFETIME_SCALE = 1.6F;
    /**
     * Fragments arrive all at once, so their sparks are shorter-lived than the rest of the
     * explosion however big the charge is.
     */
    private static final float FRAG_LIFETIME_SHARE = 0.45F;

    /** Fireball quad size per block of crater radius - the linear term. */
    private static final float FIREBALL_SCALE_PER_RADIUS = 0.5F;
    /** A sub-block crater still gets a visible puff, just a small one. */
    private static final float MIN_FIREBALL_SCALE = 0.3F;
    /**
     * Ceiling on the linear term. At the default maxExplosionRadius of 128 the fireball scales
     * linearly over the whole range and only just reaches this, so the clamp bites only when a
     * server raises that config well past its default rather than shaping ordinary explosions.
     */
    private static final float MAX_FIREBALL_SCALE = 64.0F;
    /** One fireball sprite per this many blocks of crater radius. */
    private static final float RADIUS_PER_FIREBALL = 2.0F;
    private static final int MAX_FIREBALLS = 16;
    /**
     * Ticks the fireball keeps burning, per block of crater radius. Unlike the rest of the budget
     * this is linear in the radius rather than going through {@link #intensity(float)}, because it
     * has a threshold to clear rather than a look to hit: a vanilla explosion puff lives six to
     * nine ticks, and a duration under that leaves a single burst with no follow-up waves at all.
     * Linear growth keeps the sub-block craters safely beneath it, where the square-root curve
     * would lift them just over and give a .50 cal round a rolling fireball.
     */
    private static final float FIREBALL_DURATION_PER_RADIUS = 7.5F;
    private static final int MAX_FIREBALL_DURATION_TICKS = 200;

    /** Most a heavy charge multiplies the authored flare and debris counts by. */
    private static final float MAX_COUNT_SCALE = 3.0F;
    /** Bound on what the count multiplier may cost a client, per layer. */
    private static final int MAX_SCALED_COUNT = 120;

    /** A single bright flash, sized from the crater. */
    private static final float FLASH_SCALE_PER_RADIUS = 1.5F;
    private static final float MIN_FLASH_SCALE = 0.6F;
    private static final float MAX_FLASH_SCALE = 30.0F;

    /**
     * Below this blast radius the overpressure envelope is too small to read as a wave, and a ring
     * drawn across it would just be noise around the fireball. Set above the ~3.5 block blast of a
     * 20 mm round so only shells and charges throw one.
     */
    public static final float MIN_SHOCKWAVE_BLAST_RADIUS = 6.0F;
    /** One shockwave particle per this many blocks of blast radius. */
    private static final float BLAST_RADIUS_PER_SHOCKWAVE = 1.2F;
    private static final int MAX_SHOCKWAVE_PARTICLES = 48;

    /**
     * Below this fragmentation radius the spray would be lost inside the fireball. Set above the
     * ~3.5 block fragmentation radius of a 20 mm round for the same reason as the shockwave.
     */
    public static final float MIN_FRAG_SPRAY_RADIUS = 6.0F;
    /** Sparks per block of fragmentation radius, before the intensity term. */
    private static final float SPARKS_PER_FRAG_RADIUS = 0.8F;
    private static final int MAX_FRAG_SPARKS = 64;

    /**
     * Below this crater radius nothing is thrown high enough to leave a column standing. Set above
     * the ~5.5 block crater of an 88 mm shell, so a column marks a demolition charge rather than
     * ordinary tank gunnery.
     */
    public static final float MIN_SMOKE_COLUMN_CRATER_RADIUS = 6.0F;
    /** One column source per this many blocks of crater radius. */
    private static final float CRATER_RADIUS_PER_COLUMN = 1.5F;
    /** Column sources are long-lived emitters, so very few of them go a long way. */
    private static final int MAX_SMOKE_COLUMN_PARTICLES = 10;
    /** Explosion columns use only a brief tail of the smoke-screen particle's authored lifetime. */
    private static final float MAX_SMOKE_COLUMN_LIFETIME_SCALE = 0.2F;

    /**
     * Where this explosion sits between brief and impressive, 0 to 1.
     * <p>
     * The square root is what makes the small end usable. Linearly, every round lighter than an
     * 88 mm shell is crushed into the bottom few percent of the range and a 20 mm looks exactly
     * like a .50 cal; the curve spreads the light rounds apart where the player can actually see
     * the difference, and costs only a little resolution at the top where everything is already
     * large.
     */
    public static float intensity(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= BRIEF_CRATER_RADIUS)
            return 0F;

        float t = (craterRadius - BRIEF_CRATER_RADIUS) / (FULL_CRATER_RADIUS - BRIEF_CRATER_RADIUS);
        return Mth.sqrt(Mth.clamp(t, 0F, 1F));
    }

    /**
     * What to multiply every explosion particle's authored lifetime by. Below one the explosion
     * clears quickly, which is the whole point for small-calibre HE; above one it lingers.
     */
    public static float lifetimeScale(float craterRadius)
    {
        return Mth.lerp(intensity(craterRadius), MIN_LIFETIME_SCALE, MAX_LIFETIME_SCALE);
    }

    /** Lifetime multiplier for fragmentation sparks, which are briefer than the rest. */
    public static float fragLifetimeScale(float craterRadius)
    {
        return lifetimeScale(craterRadius) * FRAG_LIFETIME_SHARE;
    }

    /** What to multiply a content pack's authored flare and debris counts by. */
    public static float countScale(float craterRadius)
    {
        return Mth.lerp(intensity(craterRadius), 1.0F, MAX_COUNT_SCALE);
    }

    /** An authored particle count grown for the charge, bounded so a barrage cannot run away. */
    public static int scaledCount(int authoredCount, float craterRadius)
    {
        if (authoredCount <= 0)
            return 0;
        return Math.min(MAX_SCALED_COUNT, Mth.ceil(authoredCount * countScale(craterRadius)));
    }

    /** Quad size of each fireball puff. */
    public static float fireballScale(float craterRadius)
    {
        // Mth.clamp lets a NaN through as NaN, which would reach the particle engine as a quad of
        // no size at all, so a radius that is not a number is treated as the smallest puff.
        if (!Float.isFinite(craterRadius))
            return MIN_FIREBALL_SCALE;
        return Mth.clamp(craterRadius * FIREBALL_SCALE_PER_RADIUS, MIN_FIREBALL_SCALE, MAX_FIREBALL_SCALE);
    }

    /** How many fireball puffs are scattered through the crater at once. */
    public static int fireballCount(float craterRadius)
    {
        if (!Float.isFinite(craterRadius))
            return 1;
        return Mth.clamp(Mth.ceil(craterRadius / RADIUS_PER_FIREBALL), 1, MAX_FIREBALLS);
    }

    /**
     * How long the fireball keeps burning. The duration is achieved by replacing puffs as they
     * expire rather than by stretching any one of them, so each still animates at its own speed,
     * and a duration shorter than one puff's life therefore means a single burst and nothing after.
     */
    public static int fireballDurationTicks(float craterRadius)
    {
        if (!Float.isFinite(craterRadius))
            return 1;
        return Mth.clamp(Mth.ceil(craterRadius * FIREBALL_DURATION_PER_RADIUS), 1, MAX_FIREBALL_DURATION_TICKS);
    }

    /** Quad size of the single detonation flash. */
    public static float flashScale(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= 0F)
            return 0F;
        return Mth.clamp(craterRadius * FLASH_SCALE_PER_RADIUS, MIN_FLASH_SCALE, MAX_FLASH_SCALE);
    }

    /** Particles in the expanding overpressure ring, or zero when the blast is too small to show one. */
    public static int shockwaveCount(float blastRadius)
    {
        if (!Float.isFinite(blastRadius) || blastRadius < MIN_SHOCKWAVE_BLAST_RADIUS)
            return 0;
        return Mth.clamp(Mth.ceil(blastRadius / BLAST_RADIUS_PER_SHOCKWAVE), 1, MAX_SHOCKWAVE_PARTICLES);
    }

    /**
     * Sparks thrown out along the fragmentation envelope. Scales with the fragmentation intensity
     * as well as the radius, so a thin-walled blast charge sprays far less than a frag shell
     * reaching the same distance.
     */
    public static int fragSparkCount(float fragRadius, float fragIntensity)
    {
        if (!Float.isFinite(fragRadius) || fragRadius < MIN_FRAG_SPRAY_RADIUS
            || !Float.isFinite(fragIntensity) || fragIntensity <= 0F)
            return 0;

        float sparks = fragRadius * SPARKS_PER_FRAG_RADIUS * Mth.sqrt(fragIntensity);
        return Mth.clamp(Mth.ceil(sparks), 1, MAX_FRAG_SPARKS);
    }

    /** Sources of the rising smoke column, or zero when the crater is too small to raise one. */
    public static int smokeColumnCount(float craterRadius)
    {
        if (!Float.isFinite(craterRadius) || craterRadius < MIN_SMOKE_COLUMN_CRATER_RADIUS)
            return 0;
        return Mth.clamp(Mth.ceil(craterRadius / CRATER_RADIUS_PER_COLUMN), 1, MAX_SMOKE_COLUMN_PARTICLES);
    }

    /** Lifetime scale for explosion columns; direct smoker effects retain their full lifetime. */
    public static float smokeColumnLifetimeScale(float craterRadius)
    {
        return Math.min(lifetimeScale(craterRadius), MAX_SMOKE_COLUMN_LIFETIME_SCALE);
    }

    /**
     * One of the extra particle types a large detonation gets on top of the classic fireball.
     * <p>
     * The fireball itself is the right look and is left alone; what a heavy charge was missing was
     * variety around it. A single sprite repeated a hundred times reads as one flat effect however
     * many of it there are, whereas dust, flame, embers and rolling smoke at different sizes,
     * speeds and lifetimes give the eye separate things to follow and make the same explosion read
     * as much bigger.
     * <p>
     * Each layer has its own {@code minCraterRadius}, so they switch on one after another as the
     * charge grows rather than all at once. That is both a look - an 88 mm shell gets dust and
     * flame, a demolition charge gets everything - and the performance guard, since the small-calibre
     * rounds that players fire by the hundred never reach the first threshold at all.
     *
     * @param particle        particle to emit, resolved through {@link FlanParticles}
     * @param minCraterRadius crater radius below which this layer does not appear
     * @param countPerRadius  particles per block of crater radius
     * @param maxCount        bound on this layer's cost to a client
     * @param spreadShare     how far particles are scattered, as a share of the crater radius.
     *                        Several of these particle types overwrite their own velocity, so the
     *                        spread rather than the speed is what shapes them.
     * @param speedShare      outward speed in blocks per tick, per block of crater radius
     * @param upwardBias      0 throws particles evenly in every direction, 1 straight up
     * @param lifetimeShare   multiplies the explosion's own lifetime scale for this layer
     */
    public record Layer(String particle, float minCraterRadius, float countPerRadius, int maxCount,
                        float spreadShare, float speedShare, float upwardBias, float lifetimeShare)
    {
        /** How many of this layer a given crater gets, or zero when it is below the threshold. */
        public int count(float craterRadius)
        {
            if (!Float.isFinite(craterRadius) || craterRadius < minCraterRadius)
                return 0;
            return Mth.clamp(Mth.ceil(craterRadius * countPerRadius), 1, maxCount);
        }
    }

    /**
     * The extra layers, in the order they are emitted. Order matters because the client drops
     * particles once its per-tick budget is spent, so the layers that carry the most of the look
     * come first and the decorative tail is what gets thinned during a barrage.
     */
    public static final List<Layer> LAYERS = List.of(
        // A few big, instant dust puffs at the seat of the blast. Short-lived, and the widest
        // single thing on screen in the first few ticks.
        new Layer(FlanParticles.FM_SMOKE_BURST, 3.0F, 0.5F, 8, 0.55F, 0.01F, 0.20F, 1.0F),
        // Flame through the body of the fireball, so it has something burning inside it rather
        // than only an expanding grey shell.
        new Layer(FlanParticles.FM_FLAME, 2.0F, 1.5F, 32, 0.45F, 0.06F, 0.35F, 1.0F),
        // Soft dust billowing up around the crater rim, which is what gives the base of a big
        // explosion its volume.
        new Layer(FlanParticles.CLOUD, 3.0F, 1.2F, 28, 0.75F, 0.05F, 0.45F, 1.0F),
        // Embers thrown clear on ballistic arcs. These are the part that sells scale, because
        // the eye can follow an individual one all the way out and back down.
        new Layer(FlanParticles.FM_AFTERBURN, 4.0F, 1.0F, 24, 0.30F, 0.12F, 0.55F, 1.0F),
        // Rolling smoke between the fireball and the column, drifting upwards as it thins.
        new Layer(FlanParticles.LARGE_SMOKE, 5.0F, 1.0F, 24, 0.80F, 0.04F, 0.60F, 1.0F),
        // Incandescent ejecta popping out of the crater. This one sets its own upward velocity and
        // ignores anything asked of it, so it is placed across the crater instead - biased upwards
        // only enough to keep it from being scattered inside the ground, where it would stick.
        new Layer(FlanParticles.LAVA, 6.0F, 0.5F, 12, 0.50F, 0.0F, 0.30F, 0.7F)
    );
}
