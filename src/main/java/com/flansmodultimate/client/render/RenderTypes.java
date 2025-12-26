package com.flansmodultimate.client.render;

import org.lwjgl.opengl.GL11C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

public class RenderTypes
{
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_EMISSIVE_GLOW_ADDITIVE = Util.memoize((texture, depthWrite) ->
    {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard("emissive_additive_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }))
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setLightmapState(new RenderStateShard.LightmapStateShard(true))
            .setOverlayState(new RenderStateShard.OverlayStateShard(true))
            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, depthWrite))
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", GL11C.GL_LEQUAL))
            .createCompositeState(true);

        return RenderType.create(depthWrite ? "entity_emissive_glow_additive_depthwrite" : "entity_emissive_glow_additive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    });

    public static RenderType emissiveGlowAdditive(ResourceLocation tex)
    {
        return ENTITY_EMISSIVE_GLOW_ADDITIVE.apply(tex, false);
    }

    public static RenderType emissiveGlowAdditiveDepthWrite(ResourceLocation tex)
    {
        return ENTITY_EMISSIVE_GLOW_ADDITIVE.apply(tex, true);
    }
}
