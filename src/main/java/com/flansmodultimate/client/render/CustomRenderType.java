package com.flansmodultimate.client.render;

import org.lwjgl.opengl.GL11C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomRenderType
{
    private record TexDepthKey(ResourceLocation texture, boolean depthWrite) {}

    /** Standard alpha blending */
    private static final RenderStateShard.TransparencyStateShard EMISSIVE_ALPHA_TRANSPARENCY =
        new RenderStateShard.TransparencyStateShard(
            "emissive_alpha_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
        );

    /** Additive blending */
    private static final RenderStateShard.TransparencyStateShard EMISSIVE_ADDITIVE_TRANSPARENCY =
        new RenderStateShard.TransparencyStateShard(
            "emissive_additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
        );

    private static final Function<TexDepthKey, RenderType> ENTITY_EMISSIVE_ALPHA = Util.memoize(key -> createEntityEmissive(key.texture(), key.depthWrite(), EMISSIVE_ALPHA_TRANSPARENCY, key.depthWrite() ? "entity_emissive_alpha" : "entity_emissive_alpha_no_depth_write"));
    private static final Function<TexDepthKey, RenderType> ENTITY_EMISSIVE_ADDITIVE = Util.memoize(key -> createEntityEmissive(key.texture(), key.depthWrite(), EMISSIVE_ADDITIVE_TRANSPARENCY, key.depthWrite() ? "entity_emissive_additive" : "entity_emissive_additive_no_depth_write"));

    private static RenderType createEntityEmissive(ResourceLocation texture, boolean depthWrite, RenderStateShard.TransparencyStateShard transparency, String debugName)
    {
        RenderStateShard.WriteMaskStateShard writeMask = new RenderStateShard.WriteMaskStateShard(true, depthWrite);
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(transparency)
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setLightmapState(new RenderStateShard.LightmapStateShard(true))
            .setOverlayState(new RenderStateShard.OverlayStateShard(true))
            .setWriteMaskState(writeMask)
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", GL11C.GL_LEQUAL))
            .createCompositeState(true);

        return RenderType.create(debugName, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, true, true, state);
    }

    /** Emissive alpha-blended layer (writes depth) */
    public static RenderType entityEmissiveAlpha(ResourceLocation tex)
    {
        return ENTITY_EMISSIVE_ALPHA.apply(new TexDepthKey(tex, true));
    }

    /** Emissive alpha-blended layer (does NOT write depth) */
    public static RenderType entityEmissiveAlphaNoDepthWrite(ResourceLocation tex)
    {
        return ENTITY_EMISSIVE_ALPHA.apply(new TexDepthKey(tex, false));
    }

    /** Emissive additive layer (writes depth) */
    public static RenderType entityEmissiveAdditive(ResourceLocation tex)
    {
        return ENTITY_EMISSIVE_ADDITIVE.apply(new TexDepthKey(tex, true));
    }

    /** Emissive additive layer (does NOT write depth) */
    public static RenderType entityEmissiveAdditiveNoDepthWrite(ResourceLocation tex)
    {
        return ENTITY_EMISSIVE_ADDITIVE.apply(new TexDepthKey(tex, false));
    }
}
