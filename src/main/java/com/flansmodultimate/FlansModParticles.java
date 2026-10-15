package com.flansmodultimate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.particles.SimpleParticleType;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModParticles
{
    public static final Supplier<? extends SimpleParticleType> afterburnParticle = FlansModRegistries.particleRegistry.register("afterburn", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> blastPuffParticle = FlansModRegistries.particleRegistry.register("blast_puff", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> bigSmokeParticle = FlansModRegistries.particleRegistry.register("big_smoke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> debris1Particle = FlansModRegistries.particleRegistry.register("debris_1", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> explodeParticle = FlansModRegistries.particleRegistry.register("explode", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fireExplosionParticle = FlansModRegistries.particleRegistry.register("fire_explosion",
        () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> flareParticle = FlansModRegistries.particleRegistry.register("flare", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> flashParticle = FlansModRegistries.particleRegistry.register("flash", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmFlameParticle = FlansModRegistries.particleRegistry.register("fm_flame", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmMuzzleFlashParticle = FlansModRegistries.particleRegistry.register("fm_muzzle_flash",
        () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmSmokeParticle = FlansModRegistries.particleRegistry.register("fm_smoke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmTracerParticle = FlansModRegistries.particleRegistry.register("fm_tracer", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmTracerGreenParticle = FlansModRegistries.particleRegistry.register("fm_tracer_green",
        () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmTracerRedParticle = FlansModRegistries.particleRegistry.register("fm_tracer_red",
        () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> rocketExhaustParticle = FlansModRegistries.particleRegistry.register("rocket_exhaust",
        () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> smokeBurstParticle = FlansModRegistries.particleRegistry.register("smoke_burst",
        () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> smokeGrenadeParticle = FlansModRegistries.particleRegistry.register("smoke_grenade",
        () -> new SimpleParticleType(false));

    /** Forces supplier registration without resolving any registry entries. */
    static void initialize()
    {
        // no-op
    }
}
