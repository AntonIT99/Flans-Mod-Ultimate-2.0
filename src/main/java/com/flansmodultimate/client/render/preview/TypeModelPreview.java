package com.flansmodultimate.client.render.preview;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.WorldModelBoundsCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.*;

/** Static previews of AA gun and driveable models, drawn without an entity. Render thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeModelPreview
{
    private static final Map<InfoType, Optional<AABB>> bounds = new HashMap<>();

    /** Measures the same transformed geometry used by the static preview. Render thread only. */
    public static Optional<AABB> bounds(InfoType type)
    {
        return bounds.computeIfAbsent(type, definition ->
        {
            var collector = new WorldModelBoundsCollector();
            render(definition, new PoseStack(), collector, 0, 0, 1, 1, 1, 1);
            return collector.bounds();
        });
    }

    public static void clearBounds()
    {
        bounds.clear();
    }

    public static RenderType renderType(InfoType type, ResourceLocation texture)
    {
        return EnumRenderPass.DEFAULT.getRenderType(texture, ModClientConfig.get().useTranslucentRendering(type), ModClientConfig.get().useCullingRendering(type));
    }

    /**
     * Draws the model around the pose origin, facing -Z like a vanilla entity model, with Y up.
     * Legacy models face model X, and plane models the opposite way to the others.
     *
     * @return false when the type has no previewable model
     */
    public static boolean render(InfoType type, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        if (!(type instanceof DriveableType) && !(type instanceof AAGunType))
            return false;
        IModelBase model = ModelCache.getOrLoadTypeModel(type);
        if (model == null)
            return false;

        int colour = type.getColour();
        red *= (colour >> 16 & 255) / 255F;
        green *= (colour >> 8 & 255) / 255F;
        blue *= (colour & 255) / 255F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(type instanceof PlaneType ? -90F : 90F));
        boolean rendered = false;
        if (model instanceof ModelDriveable driveableModel && type instanceof DriveableType driveableType)
        {
            // Same transforms as the driveable item and entity renderers.
            LegacyTransformApplier.applyModelTransform(model, type, poseStack);
            float modelScale = type.getModelScale();
            poseStack.scale(modelScale, modelScale, modelScale);
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
                driveableModel.render(driveableType, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, 1F, renderPass);
            rendered = true;
        }
        else if (model instanceof ModelAAGun aaGunModel && type instanceof AAGunType aaGunType)
        {
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
                aaGunModel.render(aaGunType, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, type.getModelScale(), renderPass);
            rendered = true;
        }
        poseStack.popPose();
        return rendered;
    }
}
