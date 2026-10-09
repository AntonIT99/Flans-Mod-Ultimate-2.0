package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.thermal.ThermalHotParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets thermal sights find the particles that should look hot, as they are created. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineThermalMixin
{
    /** Every particle passes through here, including those the mod adds directly by class. */
    @Inject(method = "add", at = @At("HEAD"))
    private void flansmodultimateTrackHotParticleByClass(Particle particle, CallbackInfo ci)
    {
        ThermalHotParticles.trackByClass(particle);
    }

    /** Particles created from a particle type are also matched by their type id. */
    @Inject(method = "createParticle", at = @At("RETURN"))
    private void flansmodultimateTrackHotParticleByType(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir)
    {
        ThermalHotParticles.trackByType(cir.getReturnValue(), options);
    }
}
