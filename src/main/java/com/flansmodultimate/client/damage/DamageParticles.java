package com.flansmodultimate.client.damage;

import com.flansmodultimate.client.particle.BlastPuffParticle;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.config.ModClientConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * The puffs damage is drawn with, and the one place they are spawned from.
 *
 * <p>
 * Every look is the same tinted cloud particle the explosions use, shaped per spawn, so smoke, steam, a fuel-vapour
 * trail, fire, sparks and hydraulic mist all come from one particle and all go through the shared distance, density
 * and per-tick budget of Flan's Mod particles.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DamageParticles
{
    /** How far ordinary damage effects are drawn at a distance scale of one. */
    static final double EFFECT_RANGE = 160D;
    /** How far a wreck's smoke column and a death's fireball are drawn: they mark the battlefield. */
    static final double LANDMARK_RANGE = 320D;
    /** Ticks a scorch mark stays on the ground before it has faded. */
    static final int SCORCH_TICKS = 2400;

    // spotless:off
    static final BlastPuffParticle.Look GREY_SMOKE = new BlastPuffParticle.Look(0.56F, 0.55F, 0.54F, 0.42F, 0.42F, 0.42F, 0.55F, 2.2F, 0.96F, 0.004F, 0F, false, true);
    static final BlastPuffParticle.Look BLACK_SMOKE = new BlastPuffParticle.Look(0.17F, 0.16F, 0.15F, 0.25F, 0.24F, 0.23F, 0.75F, 2.6F, 0.96F, 0.006F, 0F, false, true);
    static final BlastPuffParticle.Look OIL_SMOKE = new BlastPuffParticle.Look(0.09F, 0.08F, 0.07F, 0.19F, 0.18F, 0.17F, 0.85F, 3.0F, 0.965F, 0.007F, 0F, false, true);
    static final BlastPuffParticle.Look COLUMN_SMOKE = new BlastPuffParticle.Look(0.12F, 0.11F, 0.10F, 0.30F, 0.29F, 0.28F, 0.8F, 3.6F, 0.975F, 0.009F, 0F, false, false);
    static final BlastPuffParticle.Look THIN_SMOKE = new BlastPuffParticle.Look(0.48F, 0.47F, 0.46F, 0.55F, 0.55F, 0.55F, 0.35F, 2.4F, 0.97F, 0.005F, 0F, false, true);
    static final BlastPuffParticle.Look STEAM = new BlastPuffParticle.Look(0.95F, 0.95F, 0.97F, 0.86F, 0.87F, 0.9F, 0.5F, 2.5F, 0.94F, 0.012F, 0F, false, true);
    static final BlastPuffParticle.Look VAPOUR = new BlastPuffParticle.Look(0.93F, 0.93F, 0.95F, 0.9F, 0.9F, 0.93F, 0.32F, 1.6F, 0.9F, 0.0005F, 0F, false, false);
    static final BlastPuffParticle.Look FIRE = new BlastPuffParticle.Look(1.0F, 0.76F, 0.36F, 0.78F, 0.2F, 0.04F, 0.95F, 0.6F, 0.92F, 0.01F, 1F, true, false);
    static final BlastPuffParticle.Look FLASH = new BlastPuffParticle.Look(1.0F, 0.93F, 0.72F, 1.0F, 0.48F, 0.14F, 1F, 1.2F, 0.8F, 0F, 1F, true, false);
    static final BlastPuffParticle.Look SPARK = new BlastPuffParticle.Look(1.0F, 0.86F, 0.52F, 0.92F, 0.36F, 0.08F, 1F, -0.6F, 0.96F, -0.03F, 1F, true, true);
    static final BlastPuffParticle.Look ELECTRIC = new BlastPuffParticle.Look(0.76F, 0.86F, 1.0F, 0.42F, 0.56F, 1.0F, 1F, -0.7F, 0.85F, -0.02F, 1F, true, true);
    static final BlastPuffParticle.Look HYDRAULIC = new BlastPuffParticle.Look(0.56F, 0.33F, 0.13F, 0.4F, 0.23F, 0.08F, 0.6F, 1.0F, 0.9F, -0.012F, 0F, false, true);
    static final BlastPuffParticle.Look DUST = new BlastPuffParticle.Look(0.56F, 0.51F, 0.43F, 0.5F, 0.46F, 0.4F, 0.45F, 1.6F, 0.88F, 0.001F, 0F, false, true);
    static final BlastPuffParticle.Look OIL_SLICK = new BlastPuffParticle.Look(0.07F, 0.07F, 0.06F, 0.12F, 0.11F, 0.10F, 0.55F, 2.5F, 0.5F, 0F, 0F, false, true);
    static final BlastPuffParticle.Look SCORCH = new BlastPuffParticle.Look(0.05F, 0.045F, 0.04F, 0.09F, 0.085F, 0.08F, 0.8F, 0.15F, 0F, 0F, 0F, false, false);
    // spotless:on

    /** @return the share of damage effects the client wants, 0 when they are off */
    static double density()
    {
        ModClientConfig config = ModClientConfig.get();
        return config == null ? 1D : config.driveableDamageEffectDensity;
    }

    /** @return how many of something to emit this tick for a fractional rate: the whole part, plus one by chance */
    static int count(double rate, RandomSource random)
    {
        if (Double.isNaN(rate) || rate <= 0D)
            return 0;
        int whole = (int) rate;
        return whole + (random.nextDouble() < rate - whole ? 1 : 0);
    }

    /**
     * Spawns one puff, when the camera is close enough and the budget allows.
     *
     * @param landmark
     *            whether it is large enough to be drawn from afar without thinning
     */
    // Mirrors the particle constructor: look, position, velocity, size and lifetime.
    @SuppressWarnings("java:S107")
    static void puff(BlastPuffParticle.Look look, Vec3 at, double vx, double vy, double vz, float size, int lifetime, boolean landmark)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null)
            return;
        boolean allowed = landmark ? ParticleHelper.shouldSpawnLandmark(at.x, at.y, at.z, LANDMARK_RANGE) : ParticleHelper.shouldSpawnEffect(at.x, at.y, at.z, EFFECT_RANGE);
        if (!allowed)
            return;
        BlastPuffParticle particle = BlastPuffParticle.create(level, at.x, at.y, at.z, vx, vy, vz, look, size, lifetime);
        if (particle != null)
            minecraft.particleEngine.add(particle);
    }

    /** Spawns a rising underwater bubble under the same distance and budget rules. */
    static void bubble(Vec3 at, double rise)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && ParticleHelper.shouldSpawnEffect(at.x, at.y, at.z, EFFECT_RANGE))
            level.addParticle(ParticleTypes.BUBBLE, at.x, at.y, at.z, 0D, rise, 0D);
    }

    /** Lays a burnt patch flat on the ground at the given height, as a fire or a crash leaves. */
    static void scorchGround(double x, double groundY, double z, float radius)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || density() <= 0D || !ParticleHelper.shouldSpawnLandmark(x, groundY, z, LANDMARK_RANGE))
            return;
        BlastPuffParticle particle = BlastPuffParticle.createFlat(level, x, groundY + 0.02D, z, SCORCH, radius, SCORCH_TICKS);
        if (particle != null)
            minecraft.particleEngine.add(particle);
    }

    /** A spray of sparks thrown from a point, falling as they cool. */
    static void sparks(BlastPuffParticle.Look look, Vec3 at, int count, double speed, RandomSource random)
    {
        for (int i = 0; i < count; i++)
        {
            double vx = (random.nextDouble() - 0.5D) * 2D * speed;
            double vy = random.nextDouble() * speed * 1.2D;
            double vz = (random.nextDouble() - 0.5D) * 2D * speed;
            puff(look, at, vx, vy, vz, 0.08F + random.nextFloat() * 0.08F, 8 + random.nextInt(10), false);
        }
    }

    /** A burst of fire, smoke and flash where something has just blown up. */
    static void burst(Vec3 at, float size, RandomSource random)
    {
        puff(FLASH, at, 0D, 0D, 0D, 1.2F * size, 6, true);
        int fires = Math.max(4, Math.round(10 * size));
        for (int i = 0; i < fires; i++)
        {
            double spread = 0.12D * size;
            puff(FIRE, at, (random.nextDouble() - 0.5D) * spread, random.nextDouble() * spread * 1.4D, (random.nextDouble() - 0.5D) * spread, (0.5F + random.nextFloat() * 0.6F) * size,
                12 + random.nextInt(12), true);
        }
        int smokes = Math.max(3, Math.round(6 * size));
        for (int i = 0; i < smokes; i++)
        {
            double spread = 0.08D * size;
            puff(BLACK_SMOKE, at, (random.nextDouble() - 0.5D) * spread, 0.02D + random.nextDouble() * spread, (random.nextDouble() - 0.5D) * spread, (0.7F + random.nextFloat() * 0.6F) * size,
                60 + random.nextInt(40), true);
        }
        sparks(SPARK, at, Math.max(4, Math.round(8 * size)), 0.25D * Math.sqrt(size), random);
    }
}
