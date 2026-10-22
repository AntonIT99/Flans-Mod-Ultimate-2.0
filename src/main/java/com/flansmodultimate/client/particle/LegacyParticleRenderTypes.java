package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansModTextures;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

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
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator)
        {
            drawWithDepth(tesselator);
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString()
        {
            return "FLAN_LEGACY_TERRAIN";
        }
    };

    /** Countermeasure flares kept their own texture instead of a particle atlas sprite. */
    public static final ParticleRenderType FLARE = createForTexture("FLAN_LEGACY_FLARE", FlansModTextures.TEXTURE_GUI_FLARE);

    private LegacyParticleRenderTypes()
    {}

    /**
     * Draws the batch's colour without writing depth, so overlapping puffs blend in any order, then draws it again into
     * depth only. Clouds and weather render after particles, and without that depth they were drawn over the smoke.
     */
    private static void drawWithDepth(Tesselator tesselator)
    {
        BufferBuilder.RenderedBuffer rendered = tesselator.getBuilder().end();
        if (rendered.isEmpty())
        {
            rendered.release();
            return;
        }

        // Bind the immediate buffer ourselves so it can be drawn twice; reset() makes BufferUploader rebind afterwards.
        BufferUploader.reset();
        VertexBuffer buffer = rendered.drawState().format().getImmediateDrawVertexBuffer();
        buffer.bind();
        buffer.upload(rendered);
        buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        RenderSystem.colorMask(true, true, true, true);
        VertexBuffer.unbind();
    }

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
                drawWithDepth(tesselator);
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
                drawWithDepth(tesselator);
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
