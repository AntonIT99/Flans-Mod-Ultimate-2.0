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
    public static final Supplier<? extends SimpleParticleType> fireExplosionParticle = FlansModRegistries.particleRegistry.register("fire_explosion", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> groundBloodParticle = FlansModRegistries.particleRegistry.register("ground_blood", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> flareParticle = FlansModRegistries.particleRegistry.register("flare", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> flashParticle = FlansModRegistries.particleRegistry.register("flash", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmFlameParticle = FlansModRegistries.particleRegistry.register("fm_flame", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmMuzzleFlashParticle = FlansModRegistries.particleRegistry.register("fm_muzzle_flash", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmSmokeParticle = FlansModRegistries.particleRegistry.register("fm_smoke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmTracerParticle = FlansModRegistries.particleRegistry.register("fm_tracer", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmTracerGreenParticle = FlansModRegistries.particleRegistry.register("fm_tracer_green", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> fmTracerRedParticle = FlansModRegistries.particleRegistry.register("fm_tracer_red", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> rocketExhaustParticle = FlansModRegistries.particleRegistry.register("rocket_exhaust", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> smokeBurstParticle = FlansModRegistries.particleRegistry.register("smoke_burst", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> smokeGrenadeParticle = FlansModRegistries.particleRegistry.register("smoke_grenade", () -> new SimpleParticleType(false));

    public static final Supplier<? extends SimpleParticleType> legacyBloodParticle = FlansModRegistries.particleRegistry.register("blood", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyOverkillParticle = FlansModRegistries.particleRegistry.register("overkill", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyTankDeathParticle = FlansModRegistries.particleRegistry.register("tank_death", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyShipDeathParticle = FlansModRegistries.particleRegistry.register("ship_death", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyNukeParticle = FlansModRegistries.particleRegistry.register("fm_nuke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacySmokeShellParticle = FlansModRegistries.particleRegistry.register("smoke_shell", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyMustardParticle = FlansModRegistries.particleRegistry.register("smoke_shell_mustard", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyChlorineParticle = FlansModRegistries.particleRegistry.register("smoke_shell_chlorine", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyWaterParticle = FlansModRegistries.particleRegistry.register("water_smoke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyWaterMiniParticle = FlansModRegistries.particleRegistry.register("water_smoke_mini", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyShipSmokeParticle = FlansModRegistries.particleRegistry.register("ship_smoke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyGunSmokeParticle = FlansModRegistries.particleRegistry.register("gun_smoke", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyLockedParticle = FlansModRegistries.particleRegistry.register("locked", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyFlasherParticle = FlansModRegistries.particleRegistry.register("flasher", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyNuFlashParticle = FlansModRegistries.particleRegistry.register("nu_flash", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyNuMuzzleParticle = FlansModRegistries.particleRegistry.register("nu_muzzle", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyNuSparkParticle = FlansModRegistries.particleRegistry.register("nu_spark", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyCasingParticle = FlansModRegistries.particleRegistry.register("casing", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> legacyTurretTestParticle = FlansModRegistries.particleRegistry.register("turret_test", () -> new SimpleParticleType(false));
    public static final Supplier<? extends SimpleParticleType> classicMuzzleParticle = FlansModRegistries.particleRegistry.register("classic_muzzle", () -> new SimpleParticleType(false));

    /** Forces supplier registration without resolving any registry entries. */
    static void initialize()
    {
        // no-op
    }
}
