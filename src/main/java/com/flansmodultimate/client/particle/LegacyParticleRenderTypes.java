package com.flansmodultimate.client.particle;

import com.flansmodultimate.FlansModTextures;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Objects;

public final class LegacyParticleRenderTypes
{
    private static final ResourceLocation PARTICLE_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/particles.png");
    public static final ParticleRenderType TRANSLUCENT = create("FLAN_LEGACY_TRANSLUCENT", false);
    public static final ParticleRenderType PREMULTIPLIED = create("FLAN_LEGACY_PREMULTIPLIED", true);
    public static final ParticleRenderType TERRAIN = new ParticleRenderType()
    {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager)
        {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
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

    /** Whether this batch needs the legacy smoke depth pass after its colour pass. */
    public static boolean usesDepthPass(ParticleRenderType type)
    {
        return type == TRANSLUCENT || type == PREMULTIPLIED || type == TERRAIN || type == FLARE;
    }

    /** 1.21 removed ParticleRenderType.end; ParticleEngine's draw hook supplies the completed mesh instead. */
    public static void drawWithDepth(MeshData mesh)
    {
        BufferUploader.reset();
        VertexBuffer buffer = mesh.drawState().format().getImmediateDrawVertexBuffer();
        buffer.bind();
        try
        {
            buffer.upload(mesh);
            // ParticleEngine selects its shader before beginning each batch.
            ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());
            // Keep overlapping puffs blended, then prevent later clouds and weather from drawing over them.
            buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(true);
            buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
        }
        finally
        {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            VertexBuffer.unbind();
        }
    }

    /** Premultiplied blending like {@link #PREMULTIPLIED}, but bound to a single texture file. */
    private static ParticleRenderType createForTexture(String name, ResourceLocation texture)
    {
        return new ParticleRenderType()
        {
            @Override
            public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager)
            {
                RenderSystem.depthMask(false);
                RenderSystem.setShaderTexture(0, texture);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
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
            public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager)
            {
                RenderSystem.depthMask(false);
                RenderSystem.setShaderTexture(0, PARTICLE_ATLAS);
                RenderSystem.enableBlend();
                if (premultiplied)
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                else
                    RenderSystem.defaultBlendFunc();
                return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public String toString()
            {
                return name;
            }
        };
    }
}
