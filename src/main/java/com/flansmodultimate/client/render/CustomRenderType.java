package com.flansmodultimate.client.render;

import org.lwjgl.opengl.GL11C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
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
    private record TexDepthCullKey(ResourceLocation texture, boolean depthWrite, boolean cull) {}
    private record TexCullKey(ResourceLocation texture, boolean cull) {}

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

    private static final Function<TexDepthCullKey, RenderType> ENTITY_EMISSIVE_ALPHA = Util.memoize(key -> createEntityEmissive(key.texture(), key.depthWrite(), key.cull(), EMISSIVE_ALPHA_TRANSPARENCY, key.depthWrite() ? "entity_emissive_alpha" : "entity_emissive_alpha_no_depth_write"));
    private static final Function<TexDepthCullKey, RenderType> ENTITY_EMISSIVE_ADDITIVE = Util.memoize(key -> createEntityEmissive(key.texture(), key.depthWrite(), key.cull(), EMISSIVE_ADDITIVE_TRANSPARENCY, key.depthWrite() ? "entity_emissive_additive" : "entity_emissive_additive_no_depth_write"));
    private static final Function<TexCullKey, RenderType> ARMOR_CUTOUT = Util.memoize(key -> createArmorCutout(key.texture(), key.cull()));
    private static final Function<TexCullKey, RenderType> ARMOR_TRANSLUCENT = Util.memoize(key -> createArmorTranslucent(key.texture(), key.cull()));

    private static final RenderStateShard.LayeringStateShard ARMOR_VIEW_OFFSET_LAYERING =
        new RenderStateShard.LayeringStateShard(
            "armor_view_offset_layering",
            () -> {
                PoseStack poseStack = RenderSystem.getModelViewStack();
                poseStack.pushPose();
                poseStack.scale(0.99975586F, 0.99975586F, 0.99975586F);
                RenderSystem.applyModelViewMatrix();
            },
            () -> {
                PoseStack poseStack = RenderSystem.getModelViewStack();
                poseStack.popPose();
                RenderSystem.applyModelViewMatrix();
            }
        );

    private static RenderType createEntityEmissive(ResourceLocation texture, boolean depthWrite, boolean cull, RenderStateShard.TransparencyStateShard transparency, String debugName)
    {
        RenderStateShard.WriteMaskStateShard writeMask = new RenderStateShard.WriteMaskStateShard(true, depthWrite);
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(transparency)
            .setCullState(new RenderStateShard.CullStateShard(cull))
            .setLightmapState(new RenderStateShard.LightmapStateShard(true))
            .setOverlayState(new RenderStateShard.OverlayStateShard(true))
            .setWriteMaskState(writeMask)
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", GL11C.GL_LEQUAL))
            .createCompositeState(true);

        return RenderType.create(cull ? debugName + "_cull" : debugName, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, true, true, state);
    }

    private static RenderType createArmorCutout(ResourceLocation texture, boolean cull)
    {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorCutoutNoCullShader))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setCullState(new RenderStateShard.CullStateShard(cull))
            .setLightmapState(new RenderStateShard.LightmapStateShard(true))
            .setOverlayState(new RenderStateShard.OverlayStateShard(true))
            .setLayeringState(ARMOR_VIEW_OFFSET_LAYERING)
            .createCompositeState(true);

        return RenderType.create(cull ? "armor_cutout_cull" : "armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, state);
    }

    private static RenderType createArmorTranslucent(ResourceLocation texture, boolean cull)
    {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentShader))
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(EMISSIVE_ALPHA_TRANSPARENCY)
            .setCullState(new RenderStateShard.CullStateShard(cull))
            .setLightmapState(new RenderStateShard.LightmapStateShard(true))
            .setOverlayState(new RenderStateShard.OverlayStateShard(true))
            .setLayeringState(ARMOR_VIEW_OFFSET_LAYERING)
            .createCompositeState(true);

        return RenderType.create(cull ? "armor_translucent_cull" : "armor_translucent_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    }

    /** Emissive alpha-blended layer (writes depth) */
    public static RenderType entityEmissiveAlpha(ResourceLocation tex)
    {
        return entityEmissiveAlpha(tex, false);
    }

    public static RenderType entityEmissiveAlpha(ResourceLocation tex, boolean cull)
    {
        return ENTITY_EMISSIVE_ALPHA.apply(new TexDepthCullKey(tex, true, cull));
    }

    /** Emissive alpha-blended layer (does NOT write depth) */
    public static RenderType entityEmissiveAlphaNoDepthWrite(ResourceLocation tex)
    {
        return entityEmissiveAlphaNoDepthWrite(tex, false);
    }

    public static RenderType entityEmissiveAlphaNoDepthWrite(ResourceLocation tex, boolean cull)
    {
        return ENTITY_EMISSIVE_ALPHA.apply(new TexDepthCullKey(tex, false, cull));
    }

    /** Emissive additive layer (writes depth) */
    public static RenderType entityEmissiveAdditive(ResourceLocation tex)
    {
        return entityEmissiveAdditive(tex, false);
    }

    public static RenderType entityEmissiveAdditive(ResourceLocation tex, boolean cull)
    {
        return ENTITY_EMISSIVE_ADDITIVE.apply(new TexDepthCullKey(tex, true, cull));
    }

    /** Emissive additive layer (does NOT write depth) */
    public static RenderType entityEmissiveAdditiveNoDepthWrite(ResourceLocation tex)
    {
        return entityEmissiveAdditiveNoDepthWrite(tex, false);
    }

    public static RenderType entityEmissiveAdditiveNoDepthWrite(ResourceLocation tex, boolean cull)
    {
        return ENTITY_EMISSIVE_ADDITIVE.apply(new TexDepthCullKey(tex, false, cull));
    }

    public static RenderType armorCutout(ResourceLocation tex, boolean cull)
    {
        return ARMOR_CUTOUT.apply(new TexCullKey(tex, cull));
    }

    public static RenderType armorTranslucent(ResourceLocation tex, boolean cull)
    {
        return ARMOR_TRANSLUCENT.apply(new TexCullKey(tex, cull));
    }

    /** Armor layer render type with armor z-offset and entity-style partial alpha blending */
    public static RenderType armorTranslucentNoCull(ResourceLocation tex)
    {
        return armorTranslucent(tex, false);
    }
}
