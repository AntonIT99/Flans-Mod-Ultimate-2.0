package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-projectile smoke allowances, independent of the client's shared particle budget. Client thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SmokeClouds
{
    private static final int MAX_CLOUDS = 32;
    private static final int MAX_PARTICLES_PER_TICK = 2;
    private static final int MAX_LIVE_PARTICLES = 96;
    private static final int MAX_LIFETIME = 60;
    private static final Map<Integer, Cloud> CLOUDS = new HashMap<>();
    private static ClientLevel currentLevel;

    /** A packet refreshes one source; additional packets in the same client tick cannot multiply its allowance. */
    public static void emit(int sourceId, Vec3 position, int requested, String particleType, float smokeRadius)
    {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || requested <= 0)
            return;

        Cloud cloud = CLOUDS.get(sourceId);
        if (cloud == null)
        {
            if (CLOUDS.size() >= MAX_CLOUDS)
                return;
            cloud = new Cloud();
            CLOUDS.put(sourceId, cloud);
        }

        int count = cloud.request(level.getGameTime(), requested);
        String visibleParticle = visibleParticle(particleType);
        float extent = visualRadius(smokeRadius);
        float radiusScale = extent / 5F;
        // Keep ordinary legacy/vanilla smoke dense enough to screen a grenade's surroundings with fewer puffs.
        float scale = (switch (visibleParticle)
        {
            case FlanParticles.CLOUD, FlanParticles.EXPLODE, FlanParticles.SMOKE, FlanParticles.LARGE_SMOKE -> 5F;
            default -> 1F;
        }) * radiusScale * 0.6F;
        for (int i = 0; i < count; i++)
        {
            // Puffs fill the whole smoke radius evenly, as a low dome, never another ballistic smoke shell.
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double radius = Math.sqrt(level.random.nextDouble()) * extent * 0.85D;
            double x = position.x + Math.cos(angle) * radius;
            double y = position.y + (0.1D + level.random.nextDouble() * 0.5D * (1D - radius / extent)) * extent;
            double z = position.z + Math.sin(angle) * radius;
            Particle particle = ParticleHelper.createFromString(visibleParticle, x, y, z, 0D, 0D, 0D, scale, 1F);
            if (particle != null)
            {
                particle.setLifetime(MAX_LIFETIME);
                cloud.track(particle);
            }
        }
    }

    /** Extreme gameplay radii must not become screen-filling particle quads. */
    static float visualRadius(float radius)
    {
        return Float.isFinite(radius) ? Mth.clamp(radius, 0.25F, 16F) : 5F;
    }

    /** Drops finished sources and releases every reference when changing or leaving a world. */
    public static void tick()
    {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level != null)
            CLOUDS.values().removeIf(cloud -> cloud.isFinished(level.getGameTime()));
    }

    @SuppressWarnings("java:S2696") // All state belongs to the single client world.
    private static void useLevel(ClientLevel level)
    {
        if (currentLevel != level)
        {
            CLOUDS.clear();
            currentLevel = level;
        }
    }

    /** Continuous smoke uses visible puffs, not controllers which would escape their source's allowance. */
    static String visibleParticle(String name)
    {
        String normalized = FlanParticles.resolve(name).orElse(name);
        return switch (normalized)
        {
            case FlanParticles.FM_BIG_SMOKE, FlanParticles.FM_SMOKER, FlanParticles.FM_SMOKER_1, FlanParticles.HUGE_EXPLOSION, FlanParticles.FM_FLARE, FlanParticles.FM_FLAME,
                FlanParticles.FM_DEBRIS_1, FlanParticles.FM_TRACER, FlanParticles.FM_TRACER_GREEN, FlanParticles.FM_TRACER_RED -> FlanParticles.FM_SMOKER;
            default -> normalized;
        };
    }

    static final class Cloud
    {
        private final List<Puff> particles = new ArrayList<>();
        private long emissionTick = Long.MIN_VALUE;
        private int attempts;

        int request(long tick, int requested)
        {
            prune(tick);
            if (emissionTick != tick)
            {
                emissionTick = tick;
                attempts = 0;
            }
            int count = Math.max(0, Math.min(Math.min(requested, MAX_PARTICLES_PER_TICK) - attempts, MAX_LIVE_PARTICLES - particles.size()));
            attempts += count;
            return count;
        }

        void track(Particle particle)
        {
            int lifetime = Math.max(1, Math.min(particle.getLifetime(), MAX_LIFETIME));
            particle.setLifetime(lifetime);
            particles.add(new Puff(particle, emissionTick + lifetime + 1));
        }

        boolean isFinished(long tick)
        {
            prune(tick);
            return particles.isEmpty() && tick - emissionTick > 2;
        }

        private void prune(long tick)
        {
            // Engine eviction and resource reload can drop a particle without calling remove() on it.
            particles.removeIf(puff -> !puff.particle().isAlive() || tick >= puff.expiresAt());
        }
    }

    private record Puff(Particle particle, long expiresAt)
    {}
}
