package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public final class LegacyParticleRenderTypes
{
    public static final ParticleRenderType TRANSLUCENT = create("FLAN_LEGACY_TRANSLUCENT", false);
    public static final ParticleRenderType PREMULTIPLIED = create("FLAN_LEGACY_PREMULTIPLIED", true);
    public static final ParticleRenderType TERRAIN = new ParticleRenderType()
    {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager)
        {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator)
        {
            tesselator.end();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString()
        {
            return "FLAN_LEGACY_TERRAIN";
        }
    };

    /** Countermeasure flares kept their own texture instead of a particle atlas sprite. */
    public static final ParticleRenderType FLARE = createForTexture("FLAN_LEGACY_FLARE", FlansMod.TEXTURE_GUI_FLARE);

    private LegacyParticleRenderTypes() {}

    /** Premultiplied blending like {@link #PREMULTIPLIED}, but bound to a single texture file. */
    private static ParticleRenderType createForTexture(String name, ResourceLocation texture)
    {
        return new ParticleRenderType()
        {
            @Override
            public void begin(BufferBuilder builder, TextureManager textureManager)
            {
                RenderSystem.depthMask(false);
                RenderSystem.setShaderTexture(0, texture);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public void end(Tesselator tesselator)
            {
                tesselator.end();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(true);
            }

            @Override
            public String toString()
            {
                return name;
            }
        };
    }

    private static ParticleRenderType create(String name, boolean premultiplied)
    {
        return new ParticleRenderType()
        {
            @Override
            public void begin(BufferBuilder builder, TextureManager textureManager)
            {
                RenderSystem.depthMask(false);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
                RenderSystem.enableBlend();
                if (premultiplied)
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                else
                    RenderSystem.defaultBlendFunc();
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public void end(Tesselator tesselator)
            {
                tesselator.end();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(true);
            }

            @Override
            public String toString()
            {
                return name;
            }
        };
    }
}
