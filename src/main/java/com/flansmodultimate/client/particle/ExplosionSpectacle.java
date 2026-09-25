package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.explosions.ExplosionVisuals;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of a detonation that happen <em>over time</em> rather than all at once.
 * <p>
 * Everything else an explosion shows is emitted in the tick it goes off, which is right for a
 * flash or a spray of fragments but leaves a heavy charge looking like one very large burst.
 * What sells a big explosion is sequence: the ground dust rolling outwards ring after ring, the
 * fireball lifting off and cooling to smoke as it climbs, and for the heaviest charges the top of
 * that stem spreading into a cap. This class stages those layers across the following ticks, all
 * sized by {@link ExplosionVisuals} so they switch on one after another as the charge grows.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExplosionSpectacle
{
    /** Staged explosions playing at once, so a barrage cannot stack unbounded work. */
    private static final int MAX_ACTIVE = 12;
    /** Ticks between successive rings of the dust skirt. */
    private static final int SKIRT_WAVE_INTERVAL_TICKS = 2;
    /** Ticks over which the cap spreads once the stem has reached its height. */
    private static final int CAP_TICKS = 4;

    /** Earth-coloured dust that every ground colour is blended with, so a lawn does not throw green dust. */
    private static final float DUST_BASE_R = 0.60F;
    private static final float DUST_BASE_G = 0.56F;
    private static final float DUST_BASE_B = 0.50F;
    private static final float GROUND_COLOUR_SHARE = 0.45F;

    private static final BlastPuffParticle.Look AFTERGLOW = new BlastPuffParticle.Look(
        1.0F, 0.78F, 0.45F, 0.85F, 0.28F, 0.06F, 0.9F, 0.7F, 1.0F, 0F, 1.0F, true, false);

    private static final List<Staged> ACTIVE = new ArrayList<>();

    /** Plays the staged layers of one detonation. Called from the explosion packet on the client. */
    public static void spawn(Vec3 position, float craterRadius, float blastRadius, boolean groundBurst, boolean fiery)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        spawnAfterglow(level, position, craterRadius);

        Staged staged = Staged.of(level, position, craterRadius, blastRadius, groundBurst, fiery);
        if (staged != null && ACTIVE.size() < MAX_ACTIVE && !staged.tick(level))
            ACTIVE.add(staged);
    }

    /** Advances every staged explosion. Driven from the client tick. */
    public static void tick()
    {
        if (ACTIVE.isEmpty())
            return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            ACTIVE.clear();
            return;
        }
        ACTIVE.removeIf(staged -> staged.tick(level));
    }

    /**
     * A warm glow under the white flash. The flash is the detonation; this is the fireball's light
     * in the instant after it, and it is what makes the flash read as fire rather than a camera bulb.
     */
    private static void spawnAfterglow(ClientLevel level, Vec3 position, float craterRadius)
    {
        float scale = ExplosionVisuals.afterglowScale(craterRadius);
        if (scale <= 0F)
            return;

        int lifetime = 5 + Mth.ceil(8F * ExplosionVisuals.intensity(craterRadius));
        emit(level, ExplosionVisuals.landmarkRange(craterRadius), position.x, position.y, position.z, 0D, 0D, 0D, AFTERGLOW, scale * 0.6F, lifetime);
    }

    private static void emit(ClientLevel level, float range, double x, double y, double z, double vx, double vy, double vz,
                             BlastPuffParticle.Look look, float size, int lifetime)
    {
        if (!ParticleHelper.shouldSpawnLandmark(x, y, z, range))
            return;
        BlastPuffParticle particle = BlastPuffParticle.create(level, x, y, z, vx, vy, vz, look, size, lifetime);
        if (particle != null)
            Minecraft.getInstance().particleEngine.add(particle);
    }

    /**
     * One burst of the animated fire-explosion sprite, drawn {@code size} blocks from its centre to
     * its edge. It is created through the engine's own provider so it animates like any other.
     */
    private static void emitFire(float range, double x, double y, double z, float size)
    {
        if (!ParticleHelper.shouldSpawnLandmark(x, y, z, range))
            return;
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(
            FlansMod.fireExplosionParticle.get(), x, y, z, 0D, 0D, 0D);
        if (particle != null)
            particle.scale(size / FireExplosionParticle.BASE_SIZE);
    }

    /**
     * The colour of dust thrown up from this block: its own map colour, pulled toward bare earth.
     * Anything growing on dirt throws dirt, so a meadow gives brown dust and snow or sand keep
     * their own colour.
     */
    private static float[] dustColour(BlockState ground, ClientLevel level, BlockPos pos)
    {
        BlockState source = ground.is(BlockTags.DIRT) ? Blocks.DIRT.defaultBlockState() : ground;
        MapColor mapColour = source.getMapColor(level, pos);
        if (mapColour == MapColor.NONE)
            return new float[] {DUST_BASE_R, DUST_BASE_G, DUST_BASE_B};

        int rgb = mapColour.col;
        return new float[] {
            Mth.lerp(GROUND_COLOUR_SHARE, DUST_BASE_R, ((rgb >> 16) & 0xFF) / 255F),
            Mth.lerp(GROUND_COLOUR_SHARE, DUST_BASE_G, ((rgb >> 8) & 0xFF) / 255F),
            Mth.lerp(GROUND_COLOUR_SHARE, DUST_BASE_B, (rgb & 0xFF) / 255F)
        };
    }

    /** One detonation's staged layers, advanced a tick at a time. */
    private static final class Staged
    {
        private final Vec3 position;
        private final float sizingRadius;
        private final float landmarkRange;
        private final float lifetimeScale;

        /** Height of the ground surface the skirt rolls across, or NaN for an air burst. */
        private final double skirtY;
        private final BlastPuffParticle.Look dustLook;
        private final int skirtWaves;
        private final int skirtPuffs;
        private final float skirtReach;

        private final int stemSteps;
        private final float stemHeight;
        private final int stemPuffs;

        /** Steps from the foot of the stem, up to this one, that burn with animated fire. */
        private final int hotStemSteps;

        private final int capPuffs;
        private final float capRadius;

        private final int duration;
        private int age;

        private Staged(Vec3 position, float craterRadius, double skirtY, float[] dust, float blastRadius, boolean fiery)
        {
            this.position = position;
            this.sizingRadius = ExplosionVisuals.stagedSizingRadius(craterRadius);
            this.landmarkRange = ExplosionVisuals.landmarkRange(craterRadius);
            this.lifetimeScale = ExplosionVisuals.lifetimeScale(craterRadius);
            this.skirtY = skirtY;
            this.dustLook = new BlastPuffParticle.Look(dust[0], dust[1], dust[2],
                dust[0] * 0.85F, dust[1] * 0.85F, dust[2] * 0.85F,
                0.8F, 1.6F, 0.9F, 0.0015F, 0F, false, true);

            boolean hasGround = !Double.isNaN(skirtY);
            this.skirtWaves = hasGround ? ExplosionVisuals.dustSkirtWaves(craterRadius) : 0;
            this.skirtPuffs = ExplosionVisuals.dustSkirtPuffsPerWave(craterRadius);
            this.skirtReach = ExplosionVisuals.dustSkirtReach(craterRadius, blastRadius);

            this.stemSteps = ExplosionVisuals.fireballStemSteps(craterRadius);
            this.stemHeight = ExplosionVisuals.fireballStemHeight(craterRadius);
            this.stemPuffs = ExplosionVisuals.fireballStemPuffsPerStep(craterRadius);
            this.hotStemSteps = Mth.ceil(stemSteps * ExplosionVisuals.hotStemShare(fiery));

            this.capPuffs = ExplosionVisuals.mushroomCapPuffs(craterRadius);
            this.capRadius = ExplosionVisuals.mushroomCapRadius(craterRadius);

            int skirtEnd = skirtWaves * SKIRT_WAVE_INTERVAL_TICKS;
            int stemEnd = stemSteps + (capPuffs > 0 ? CAP_TICKS : 0);
            this.duration = Math.max(skirtEnd, stemEnd);
        }

        /** The staged layers of this detonation, or {@code null} when it is too small to have any. */
        private static Staged of(ClientLevel level, Vec3 position, float craterRadius, float blastRadius, boolean groundBurst, boolean fiery)
        {
            if (ExplosionVisuals.dustSkirtWaves(craterRadius) <= 0 && ExplosionVisuals.fireballStemSteps(craterRadius) <= 0)
                return null;

            // A skirt needs ground close enough below for the blast to scour it. A charge high in
            // the air has nothing to throw up, and pretending otherwise would lay dust in the sky.
            double skirtY = Double.NaN;
            float[] dust = {DUST_BASE_R, DUST_BASE_G, DUST_BASE_B};
            int depth = groundBurst ? 1 : Math.max(1, Mth.ceil(ExplosionVisuals.stagedSizingRadius(craterRadius)));
            BlockPos.MutableBlockPos cursor = BlockPos.containing(position).mutable();
            for (int i = 0; i <= depth; i++)
            {
                BlockState state = level.getBlockState(cursor);
                if (!state.getCollisionShape(level, cursor).isEmpty())
                {
                    skirtY = cursor.getY() + 1.2D;
                    dust = dustColour(state, level, cursor.immutable());
                    break;
                }
                cursor.move(0, -1, 0);
            }
            return new Staged(position, craterRadius, skirtY, dust, blastRadius, fiery);
        }

        /** @return true once every layer has played out and this should be dropped */
        private boolean tick(ClientLevel level)
        {
            RandomSource random = level.random;

            if (age % SKIRT_WAVE_INTERVAL_TICKS == 0 && age / SKIRT_WAVE_INTERVAL_TICKS < skirtWaves)
                emitSkirtWave(level, random, age / SKIRT_WAVE_INTERVAL_TICKS);
            if (age < stemSteps)
                emitStemStep(level, random, age);
            if (capPuffs > 0 && age >= stemSteps && age < stemSteps + CAP_TICKS)
                emitCapPart(level, random);

            return ++age > duration;
        }

        /**
         * One ring of the skirt. Each ring starts further out than the last and is pushed outwards,
         * so together they read as a single front of dust travelling away from the blast.
         */
        private void emitSkirtWave(ClientLevel level, RandomSource random, int wave)
        {
            float progress = skirtWaves <= 1 ? 1F : wave / (float) (skirtWaves - 1);
            double inner = sizingRadius * 0.4D;
            double ring = Mth.lerp(progress, inner, skirtReach * 0.85D);
            double push = Math.max(0.05D, (skirtReach - ring) * 0.06D);
            float size = sizingRadius * 0.3F * (1F + 0.6F * progress);
            int lifetime = Math.max(20, Math.round((35F + 45F * progress) * lifetimeScale));

            for (int i = 0; i < skirtPuffs; i++)
            {
                double angle = (i + random.nextDouble()) / skirtPuffs * Mth.TWO_PI;
                double distance = ring * (0.85D + random.nextDouble() * 0.3D);
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double speed = push * (0.7D + random.nextDouble() * 0.6D);

                emit(level, landmarkRange, position.x + dx * distance, skirtY + random.nextDouble() * size * 0.3D, position.z + dz * distance,
                    dx * speed, 0.01D + random.nextDouble() * 0.03D, dz * speed,
                    dustLook, size * (0.7F + random.nextFloat() * 0.6F), lifetime + random.nextInt(20));
            }
        }

        /**
         * One step of the fireball stem. The first steps are hot, glowing and orange; each one after
         * is laid higher and a shade darker, so the column is seen lifting off and cooling into
         * smoke. The climb decelerates toward the top the way a rising fireball does.
         */
        private void emitStemStep(ClientLevel level, RandomSource random, int step)
        {
            float progress = (step + 1) / (float) stemSteps;
            double height = stemHeight * (1D - (1D - progress) * (1D - progress));
            double width = sizingRadius * 0.35D * (1D - 0.3D * progress);
            float size = sizingRadius * 0.4F * (1F + 0.3F * progress);
            int lifetime = Math.max(30, Math.round((70F + 70F * progress) * lifetimeScale));
            BlastPuffParticle.Look look = stemLook(progress);

            for (int i = 0; i < stemPuffs; i++)
            {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double distance = width * Math.sqrt(random.nextDouble());
                emit(level, landmarkRange,
                    position.x + Math.cos(angle) * distance,
                    position.y + height + random.nextGaussian() * width * 0.3D,
                    position.z + Math.sin(angle) * distance,
                    random.nextGaussian() * 0.02D, 0.03D + random.nextDouble() * 0.04D, random.nextGaussian() * 0.02D,
                    look, size * (0.8F + random.nextFloat() * 0.4F), lifetime + random.nextInt(30));
            }

            // The foot of the stem burns. The fire sprite plays out in about half a second, so it is
            // laid over the lasting puffs rather than instead of them: the fire rolls up the lower
            // column as it forms, and the puffs are what is left standing once it has burnt out.
            if (step < hotStemSteps)
            {
                for (int i = 0; i < stemPuffs; i++)
                {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double distance = width * 0.8D * Math.sqrt(random.nextDouble());
                    emitFire(landmarkRange,
                        position.x + Math.cos(angle) * distance,
                        position.y + height + random.nextGaussian() * width * 0.25D,
                        position.z + Math.sin(angle) * distance,
                        size * (0.9F + random.nextFloat() * 0.4F));
                }
            }
        }

        private static BlastPuffParticle.Look stemLook(float progress)
        {
            // Hot orange at the foot, near-black smoke at the top.
            float r = Mth.lerp(progress, 1.0F, 0.22F);
            float g = Mth.lerp(progress, 0.55F, 0.19F);
            float b = Mth.lerp(progress, 0.18F, 0.16F);
            return new BlastPuffParticle.Look(r, g, b, 0.14F, 0.13F, 0.12F,
                0.9F, 0.8F, 0.93F, 0.003F, 0.4F * (1F - progress), false, false);
        }

        /**
         * A slice of the mushroom cap. Puffs spread outwards from the top of the stem and curl
         * slightly down at the rim; the core stays hot for a moment, the rim is already smoke.
         */
        private void emitCapPart(ClientLevel level, RandomSource random)
        {
            int count = Mth.ceil(capPuffs / (float) CAP_TICKS);
            double top = position.y + stemHeight;
            float size = sizingRadius * 0.55F;
            int lifetime = Math.max(60, Math.round(150F * lifetimeScale));

            for (int i = 0; i < count; i++)
            {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double share = Math.sqrt(random.nextDouble());
                double distance = capRadius * share * 0.5D;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double outward = capRadius * 0.035D * (0.6D + random.nextDouble() * 0.8D);

                BlastPuffParticle.Look look = share < 0.4D
                    ? new BlastPuffParticle.Look(0.95F, 0.5F, 0.18F, 0.16F, 0.14F, 0.12F,
                        0.9F, 1.0F, 0.94F, 0.002F, 0.3F, false, false)
                    : new BlastPuffParticle.Look(0.26F, 0.22F, 0.2F, 0.13F, 0.12F, 0.11F,
                        0.85F, 1.2F, 0.94F, 0.001F, 0F, false, false);

                emit(level, landmarkRange,
                    position.x + dx * distance,
                    top + random.nextGaussian() * size * 0.4D,
                    position.z + dz * distance,
                    dx * outward, -0.02D * share + 0.01D, dz * outward,
                    look, size * (0.8F + random.nextFloat() * 0.5F), lifetime + random.nextInt(40));
            }
        }
    }
}
