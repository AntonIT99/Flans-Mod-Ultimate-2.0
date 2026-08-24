package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.List;
import java.util.function.Function;

/** Render pipelines used by imported legacy models. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CustomRenderType
{
    private record TexDepthCullKey(Identifier texture, boolean depthWrite, boolean cull) {}
    private record TexCullKey(Identifier texture, boolean cull) {}

    private static final BlendFunction LEGACY_ADDITIVE = new BlendFunction(
        SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);

    private static final RenderPipeline EMISSIVE_ALPHA_DEPTH = emissivePipeline("emissive_alpha_depth", BlendFunction.TRANSLUCENT, true, false);
    private static final RenderPipeline EMISSIVE_ALPHA_DEPTH_CULL = emissivePipeline("emissive_alpha_depth_cull", BlendFunction.TRANSLUCENT, true, true);
    private static final RenderPipeline EMISSIVE_ALPHA_NO_DEPTH = emissivePipeline("emissive_alpha_no_depth", BlendFunction.TRANSLUCENT, false, false);
    private static final RenderPipeline EMISSIVE_ALPHA_NO_DEPTH_CULL = emissivePipeline("emissive_alpha_no_depth_cull", BlendFunction.TRANSLUCENT, false, true);
    private static final RenderPipeline EMISSIVE_ADDITIVE_DEPTH = emissivePipeline("emissive_additive_depth", LEGACY_ADDITIVE, true, false);
    private static final RenderPipeline EMISSIVE_ADDITIVE_DEPTH_CULL = emissivePipeline("emissive_additive_depth_cull", LEGACY_ADDITIVE, true, true);
    private static final RenderPipeline EMISSIVE_ADDITIVE_NO_DEPTH = emissivePipeline("emissive_additive_no_depth", LEGACY_ADDITIVE, false, false);
    private static final RenderPipeline EMISSIVE_ADDITIVE_NO_DEPTH_CULL = emissivePipeline("emissive_additive_no_depth_cull", LEGACY_ADDITIVE, false, true);
    private static final RenderPipeline ENTITY_TRANSLUCENT_CULL = entityTranslucentCullPipeline();
    private static final RenderPipeline ARMOR_CUTOUT_CULL = armorPipeline("armor_cutout_cull", false);
    private static final RenderPipeline ARMOR_TRANSLUCENT_CULL = armorPipeline("armor_translucent_cull", true);
    private static final RenderPipeline PARTICLE_PREMULTIPLIED = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "pipeline/particle_premultiplied"))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
        .build();

    private static final List<RenderPipeline> PIPELINES = List.of(
        EMISSIVE_ALPHA_DEPTH, EMISSIVE_ALPHA_DEPTH_CULL,
        EMISSIVE_ALPHA_NO_DEPTH, EMISSIVE_ALPHA_NO_DEPTH_CULL,
        EMISSIVE_ADDITIVE_DEPTH, EMISSIVE_ADDITIVE_DEPTH_CULL,
        EMISSIVE_ADDITIVE_NO_DEPTH, EMISSIVE_ADDITIVE_NO_DEPTH_CULL,
        ENTITY_TRANSLUCENT_CULL, ARMOR_CUTOUT_CULL, ARMOR_TRANSLUCENT_CULL,
        PARTICLE_PREMULTIPLIED);

    private static final Function<TexDepthCullKey, RenderType> ENTITY_EMISSIVE_ALPHA = Util.memoize(key ->
        createEntityType("entity_emissive_alpha", key.texture(), selectEmissivePipeline(key, false), false, false, true));
    private static final Function<TexDepthCullKey, RenderType> ENTITY_EMISSIVE_ADDITIVE = Util.memoize(key ->
        createEntityType("entity_emissive_additive", key.texture(), selectEmissivePipeline(key, true), false, false, true));
    private static final Function<TexCullKey, RenderType> ENTITY_TRANSLUCENT_UNSORTED = Util.memoize(key ->
        createEntityType("entity_translucent_unsorted", key.texture(), key.cull() ? ENTITY_TRANSLUCENT_CULL : RenderPipelines.ENTITY_TRANSLUCENT, true, true, false));
    private static final Function<TexCullKey, RenderType> ENTITY_TRANSLUCENT_SORTED = Util.memoize(key ->
        createEntityType("entity_translucent", key.texture(), key.cull() ? ENTITY_TRANSLUCENT_CULL : RenderPipelines.ENTITY_TRANSLUCENT, true, true, true));
    private static final Function<TexCullKey, RenderType> ARMOR_CUTOUT = Util.memoize(key ->
        key.cull() ? createArmorType("armor_cutout_cull", key.texture(), ARMOR_CUTOUT_CULL, false) : RenderTypes.armorCutoutNoCull(key.texture()));
    private static final Function<TexCullKey, RenderType> ARMOR_TRANSLUCENT = Util.memoize(key ->
        key.cull() ? createArmorType("armor_translucent_cull", key.texture(), ARMOR_TRANSLUCENT_CULL, true) : RenderTypes.armorTranslucent(key.texture()));
    private static final Function<TexCullKey, RenderType> ARMOR_TRANSLUCENT_UNSORTED = Util.memoize(key ->
        key.cull() ? createArmorType("armor_translucent_unsorted_cull", key.texture(), ARMOR_TRANSLUCENT_CULL, false)
            : createArmorType("armor_translucent_unsorted", key.texture(), RenderPipelines.ARMOR_TRANSLUCENT, false));

    public static void registerPipelines(RegisterRenderPipelinesEvent event)
    {
        PIPELINES.forEach(event::registerPipeline);
    }

    private static RenderPipeline emissivePipeline(String name, BlendFunction blend, boolean depthWrite, boolean cull)
    {
        // Match the 1.20/1.21 position_color_tex_lightmap path: alpha-tested, but unaffected by
        // cardinal lighting or entity overlays. The render type remains sorted for blending.
        return RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "pipeline/" + name))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withShaderDefine("NO_OVERLAY")
            .withColorTargetState(new ColorTargetState(blend))
            .withCull(cull)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, depthWrite))
            .build();
    }

    private static RenderPipeline entityTranslucentCullPipeline()
    {
        return RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "pipeline/entity_translucent_cull"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    }

    private static RenderPipeline armorPipeline(String name, boolean translucent)
    {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "pipeline/" + name))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("PER_FACE_LIGHTING");
        if (translucent)
            builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        return builder.build();
    }

    private static RenderPipeline selectEmissivePipeline(TexDepthCullKey key, boolean additive)
    {
        if (additive)
            return key.depthWrite() ? (key.cull() ? EMISSIVE_ADDITIVE_DEPTH_CULL : EMISSIVE_ADDITIVE_DEPTH)
                : (key.cull() ? EMISSIVE_ADDITIVE_NO_DEPTH_CULL : EMISSIVE_ADDITIVE_NO_DEPTH);
        return key.depthWrite() ? (key.cull() ? EMISSIVE_ALPHA_DEPTH_CULL : EMISSIVE_ALPHA_DEPTH)
            : (key.cull() ? EMISSIVE_ALPHA_NO_DEPTH_CULL : EMISSIVE_ALPHA_NO_DEPTH);
    }

    private static RenderType createEntityType(String name, Identifier texture, RenderPipeline pipeline,
                                               boolean useLightmap, boolean useOverlay, boolean sortOnUpload)
    {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        if (useLightmap)
            setup.useLightmap();
        if (useOverlay)
            setup.useOverlay();
        if (sortOnUpload)
            setup.sortOnUpload();
        return RenderType.create(name, setup.createRenderSetup());
    }

    private static RenderType createArmorType(String name, Identifier texture, RenderPipeline pipeline, boolean sortOnUpload)
    {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        if (sortOnUpload)
            setup.sortOnUpload();
        return RenderType.create(name, setup.createRenderSetup());
    }

    public static RenderType entityEmissiveAlpha(Identifier texture) { return entityEmissiveAlpha(texture, false); }
    public static RenderType entityEmissiveAlpha(Identifier texture, boolean cull) { return ENTITY_EMISSIVE_ALPHA.apply(new TexDepthCullKey(texture, true, cull)); }
    public static RenderType entityEmissiveAlphaNoDepthWrite(Identifier texture) { return entityEmissiveAlphaNoDepthWrite(texture, false); }
    public static RenderType entityEmissiveAlphaNoDepthWrite(Identifier texture, boolean cull) { return ENTITY_EMISSIVE_ALPHA.apply(new TexDepthCullKey(texture, false, cull)); }
    public static RenderType entityEmissiveAdditive(Identifier texture) { return entityEmissiveAdditive(texture, false); }
    public static RenderType entityEmissiveAdditive(Identifier texture, boolean cull) { return ENTITY_EMISSIVE_ADDITIVE.apply(new TexDepthCullKey(texture, true, cull)); }
    public static RenderType entityEmissiveAdditiveNoDepthWrite(Identifier texture) { return entityEmissiveAdditiveNoDepthWrite(texture, false); }
    public static RenderType entityEmissiveAdditiveNoDepthWrite(Identifier texture, boolean cull) { return ENTITY_EMISSIVE_ADDITIVE.apply(new TexDepthCullKey(texture, false, cull)); }
    public static RenderType entityTranslucentUnsorted(Identifier texture, boolean cull) { return ENTITY_TRANSLUCENT_UNSORTED.apply(new TexCullKey(texture, cull)); }
    public static RenderType entityTranslucent(Identifier texture, boolean cull) { return ENTITY_TRANSLUCENT_SORTED.apply(new TexCullKey(texture, cull)); }
    public static RenderType armorCutout(Identifier texture, boolean cull) { return ARMOR_CUTOUT.apply(new TexCullKey(texture, cull)); }
    public static RenderType armorTranslucent(Identifier texture, boolean cull) { return ARMOR_TRANSLUCENT.apply(new TexCullKey(texture, cull)); }
    public static RenderType armorTranslucentUnsorted(Identifier texture, boolean cull) { return ARMOR_TRANSLUCENT_UNSORTED.apply(new TexCullKey(texture, cull)); }
    public static RenderType armorTranslucentNoCull(Identifier texture) { return armorTranslucent(texture, false); }
    public static RenderPipeline particlePremultipliedPipeline() { return PARTICLE_PREMULTIPLIED; }
}
