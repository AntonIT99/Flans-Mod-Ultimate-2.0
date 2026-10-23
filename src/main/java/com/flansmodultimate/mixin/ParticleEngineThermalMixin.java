package com.flansmodultimate.mixin;

import com.flansmodultimate.client.particle.LegacyParticleRenderTypes;
import com.flansmodultimate.client.render.thermal.ThermalHotParticles;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;

/** Tracks hot particles for thermal sights and draws legacy smoke depth after its blended colour. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineThermalMixin
{
    @Unique
    private boolean flansmodultimateLegacyDepthPass;

    // ParticleRenderType has no end hook on 1.21. Capture the type at begin and replace only its mesh draw.
    @Redirect(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleRenderType;begin(Lcom/mojang/blaze3d/vertex/Tesselator;Lnet/minecraft/client/renderer/texture/TextureManager;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"))
    private BufferBuilder flansmodultimateBeginParticleBatch(ParticleRenderType type, Tesselator tesselator, TextureManager textures)
    {
        flansmodultimateLegacyDepthPass = LegacyParticleRenderTypes.usesDepthPass(type);
        return type.begin(tesselator, textures);
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void flansmodultimateDrawParticleBatch(MeshData mesh)
    {
        if (flansmodultimateLegacyDepthPass)
            LegacyParticleRenderTypes.drawWithDepth(mesh);
        else
            BufferUploader.drawWithShader(mesh);
    }

    /** Every particle passes through here, including those the mod adds directly by class. */
    @Inject(method = "add", at = @At("HEAD"))
    private void flansmodultimateTrackHotParticleByClass(Particle particle, CallbackInfo ci)
    {
        ThermalHotParticles.trackByClass(particle);
    }

    /** Particles created from a particle type are also matched by their type id. */
    // The handler mirrors the parameters of the injected method.
    @SuppressWarnings("java:S107")
    @Inject(method = "createParticle", at = @At("RETURN"))
    private void flansmodultimateTrackHotParticleByType(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir)
    {
        ThermalHotParticles.trackByType(cir.getReturnValue(), options);
    }
}
