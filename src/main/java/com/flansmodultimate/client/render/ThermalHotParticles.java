package com.flansmodultimate.client.render;

import com.flansmodultimate.client.particle.AfterburnParticle;
import com.flansmodultimate.client.particle.BigSmokeParticle;
import com.flansmodultimate.client.particle.BlastPuffParticle;
import com.flansmodultimate.client.particle.Debris1Particle;
import com.flansmodultimate.client.particle.FireExplosionParticle;
import com.flansmodultimate.client.particle.FlareParticle;
import com.flansmodultimate.client.particle.FlashParticle;
import com.flansmodultimate.client.particle.FmFlameParticle;
import com.flansmodultimate.client.particle.FmMuzzleFlashParticle;
import com.flansmodultimate.client.particle.FmSmokeParticle;
import com.flansmodultimate.client.particle.FmTracerParticle;
import com.flansmodultimate.client.particle.LegacyExplodeParticle;
import com.flansmodultimate.client.particle.RocketExhaustParticle;
import com.flansmodultimate.client.particle.SmokeBurstParticle;
import com.flansmodultimate.client.particle.TracerBeamParticle;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.mixin.ParticleAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The particles thermal sights show hot, from the Labjac Edition: explosions, fire, muzzle flashes, sparks,
 * tracers, exhaust and Flan's own blast, debris and smoke particles, plus any particle types listed in the
 * {@code additionalThermalHotParticles} client setting.
 *
 * <p>
 * Particles are classified once, when they are created, and drawn into the thermal heat mask as camera-facing
 * squares of their own size, depth tested against the scene. Smoke grenades and other ordinary smoke stay cold
 * so they still hide what is behind them.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThermalHotParticles
{
    /** The mod's particles that may be added directly, without a particle type. */
    private static final Set<Class<?>> HOT_CLASSES = Set.of(AfterburnParticle.class, BigSmokeParticle.class, BlastPuffParticle.class, Debris1Particle.class, FireExplosionParticle.class,
        FlareParticle.class, FlashParticle.class, FmFlameParticle.class, FmMuzzleFlashParticle.class, FmSmokeParticle.class, FmTracerParticle.class, LegacyExplodeParticle.class,
        RocketExhaustParticle.class, SmokeBurstParticle.class, TracerBeamParticle.class);
    static final int MAX_TRACKED = 8192;
    static final int MAX_DRAWN = 4096;
    private static final double MAX_DISTANCE_SQ = 256D * 256D;
    private static final Vector3f[] CORNERS = {new Vector3f(-1F, -1F, 0F), new Vector3f(-1F, 1F, 0F), new Vector3f(1F, 1F, 0F), new Vector3f(1F, -1F, 0F)};

    private static final Set<Particle> tracked = Collections.newSetFromMap(new WeakHashMap<>());

    public static void trackByClass(Particle particle)
    {
        if (particle != null && HOT_CLASSES.contains(particle.getClass()))
            track(particle);
    }

    public static void trackByType(@Nullable Particle particle, ParticleOptions options)
    {
        if (particle == null || options == null)
            return;
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (id == null)
            return;
        ModClientConfig config = ModClientConfig.get();
        if (ThermalHotParticleIds.isHot(id.toString(), config == null ? List.of() : config.additionalThermalHotParticles))
            track(particle);
    }

    private static void track(Particle particle)
    {
        if (tracked.size() >= MAX_TRACKED)
            tracked.removeIf(p -> !p.isAlive());
        if (tracked.size() < MAX_TRACKED)
            tracked.add(particle);
    }

    /** Draws every visible hot particle into the heat mask. */
    public static void renderMask(PoseStack pose, VertexConsumer mask, Vec3 camera, Quaternionf cameraRotation, Frustum frustum, float partialTick)
    {
        Matrix4f matrix = pose.last().pose();
        int drawn = 0;
        Iterator<Particle> iterator = tracked.iterator();
        while (iterator.hasNext() && drawn < MAX_DRAWN)
        {
            Particle particle = iterator.next();
            if (!particle.isAlive())
            {
                iterator.remove();
                continue;
            }
            if (particle instanceof TracerBeamParticle beam)
            {
                if (beam.renderHeat(mask, matrix, camera, frustum))
                    drawn++;
                continue;
            }
            ParticleAccessor position = (ParticleAccessor) particle;
            double x = Mth.lerp(partialTick, position.flansmodultimateXo(), position.flansmodultimateX());
            double y = Mth.lerp(partialTick, position.flansmodultimateYo(), position.flansmodultimateY());
            double z = Mth.lerp(partialTick, position.flansmodultimateZo(), position.flansmodultimateZ());
            float size = particle instanceof SingleQuadParticle quad ? quad.getQuadSize(partialTick) : 0.1F;
            if (!(size > 0F) || camera.distanceToSqr(x, y, z) > MAX_DISTANCE_SQ || !frustum.isVisible(new AABB(x, y, z, x, y, z).inflate(size)))
                continue;
            billboard(mask, matrix, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), size, cameraRotation);
            drawn++;
        }
    }

    /** Forgets every tracked particle, for example when leaving a world. */
    public static void reset()
    {
        tracked.clear();
    }

    private static void billboard(VertexConsumer mask, Matrix4f matrix, float x, float y, float z, float size, Quaternionf rotation)
    {
        Vector3f[] corners = new Vector3f[4];
        for (int i = 0; i < 4; i++)
            corners[i] = rotation.transform(new Vector3f(CORNERS[i])).mul(size).add(x, y, z);
        HeatMaskQuads.quad(mask, matrix, corners[0], corners[1], corners[2], corners[3]);
    }
}
