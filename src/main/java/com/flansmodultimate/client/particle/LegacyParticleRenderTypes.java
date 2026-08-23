package com.flansmodultimate.client.particle;

import com.flansmodultimate.client.render.CustomRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;

@SuppressWarnings("deprecation")
public final class LegacyParticleRenderTypes
{
    public static final SingleQuadParticle.Layer TRANSLUCENT = SingleQuadParticle.Layer.TRANSLUCENT;
    public static final SingleQuadParticle.Layer TERRAIN = SingleQuadParticle.Layer.TRANSLUCENT_TERRAIN;
    public static final SingleQuadParticle.Layer PREMULTIPLIED = new SingleQuadParticle.Layer(
        true, TextureAtlas.LOCATION_PARTICLES, CustomRenderType.particlePremultipliedPipeline());

    private LegacyParticleRenderTypes() {}

}
