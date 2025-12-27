package com.flansmodultimate.client.render;

import org.lwjgl.opengl.GL11C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;

import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmissiveRenderType
{
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_GLOW = Util.memoize((texture, additive) ->
    {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard("emissive_additive_transparency", () -> {
                RenderSystem.enableBlend();
                if (additive)
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                else
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }))
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setLightmapState(new RenderStateShard.LightmapStateShard(false))
            .setOverlayState(new RenderStateShard.OverlayStateShard(true))
            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", GL11C.GL_LEQUAL))
            .createCompositeState(true);

        return RenderType.create(BooleanUtils.isTrue(additive) ? "entity_translucent_glow" : "entity_translucent_glow_additive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    });

    public static RenderType entityTranslucentGlow(ResourceLocation tex, boolean additive)
    {
        return ENTITY_TRANSLUCENT_GLOW.apply(tex, additive);
    }
}
