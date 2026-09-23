package com.flansmodultimate.client.render.item;

import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableItemRenderer
{
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (!(stack.getItem() instanceof DriveableItem<?, ?> driveableItem)
            || !(driveableItem.useCustomRenderer(context)))
        {
            ICustomItemRenderer.renderItemFallback(stack, context, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        DriveableType type = driveableItem.getConfigType();
        if (!(ModelCache.getOrLoadTypeModel(type) instanceof ModelDriveable model))
        {
            ICustomItemRenderer.renderItemFallback(stack, context, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        Paintjob paintjob = type.getPaintjob(stack);
        ResourceLocation texture = paintjob != null && paintjob.getTexture() != null ? paintjob.getTexture() : type.getTexture();
        int color = type.getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);

        poseStack.pushPose();
        // Restore the frame the ported 1.7.10 numbers below were written for.
        apply(context, poseStack);
        applyDisplayTransform(context, type, poseStack);
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);

        // ModelScale was a root transform in the legacy entity renderers. Apply it
        // to the complete preview hierarchy as well: attachment translations and
        // procedural tank-track paths otherwise remain unscaled while their mesh
        // parts are scaled, which distorts driveables in hand.
        float modelScale = type.getModelScale();
        poseStack.scale(modelScale, modelScale, modelScale);
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
        {
            model.render(type, poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay, red, green, blue, 1F, 1F, renderPass);
        }
        poseStack.popPose();
    }

    public static void apply(ItemDisplayContext context, PoseStack poseStack)
    {
        switch (context)
        {
            case FIRST_PERSON_RIGHT_HAND -> firstPerson(poseStack, 1F);
            case FIRST_PERSON_LEFT_HAND -> firstPerson(poseStack, -1F);
            case THIRD_PERSON_RIGHT_HAND -> thirdPerson(poseStack, 1F);
            case THIRD_PERSON_LEFT_HAND -> thirdPerson(poseStack, -1F);
            default ->
            {
                // Ground, GUI and item frames were never routed through the
                // hand renderers, so they have no legacy hand frame to restore.
            }
        }
    }

    private static void firstPerson(PoseStack poseStack, float side)
    {
        poseStack.translate(0.14F * side, -0.13F, -0.18F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45F * side));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        poseStack.translate(-0.5F, 0.5F, 1F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30F));
        poseStack.mulPose(Axis.YP.rotationDegrees(60F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0F));
        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);
    }

    private static void thirdPerson(PoseStack poseStack, float side)
    {
        poseStack.translate(-side / 16F, -0.125F, 0.625F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        poseStack.translate(-0.0625F, 0.4375F, 0.0625F);
        poseStack.translate(0.25F, 0.1875F, -0.1875F);
        poseStack.scale(0.375F, 0.375F, 0.375F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(60F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(20F));
        poseStack.translate(-0.5F, 0.1F, 1F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30F));
        poseStack.mulPose(Axis.YP.rotationDegrees(60F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0F));
        float scale = 1.2F;
        poseStack.scale(scale, scale, scale);
    }

    private static void applyDisplayTransform(ItemDisplayContext context, DriveableType type, PoseStack poseStack)
    {
        float scale = switch (context)
        {
            case GROUND -> 1.5F;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 1F;
            case FIXED -> 0.9F;
            case GUI -> 0.65F;
            default -> 0.5F;
        };

        switch (context)
        {
            case GROUND -> poseStack.mulPose(Axis.YP.rotationDegrees(45F));
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(15F));
                poseStack.mulPose(Axis.XP.rotationDegrees(15F));
                poseStack.mulPose(Axis.YP.rotationDegrees(type instanceof PlaneType ? 90F : 270F));
                poseStack.translate(0F, type instanceof PlaneType ? 0.2F : type instanceof MechaType ? 0.1F : 0.15F,
                    type instanceof PlaneType ? 0.4F : -0.4F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND ->
            {
                poseStack.mulPose(Axis.ZP.rotationDegrees(25F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-5F));
                poseStack.translate(0.15F, type instanceof MechaType ? 0.35F : 0.45F, -0.6F);
                if (type instanceof PlaneType)
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F));
                else if (type instanceof MechaType)
                    poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            }
            case GUI ->
            {
                poseStack.mulPose(Axis.XP.rotationDegrees(22.5F));
                poseStack.mulPose(Axis.YP.rotationDegrees(type instanceof PlaneType ? 135F : -45F));
                poseStack.translate(0F, -0.05F, 0F);
            }
            case FIXED -> poseStack.mulPose(Axis.YP.rotationDegrees(type instanceof PlaneType ? 0F : 180F));
            default ->
            {
                // no-op
            }
        }

        float cameraDistance = Mth.clamp(type.getCameraDistance(), 0.25F, 64F);
        float finalScale = scale / cameraDistance;
        poseStack.scale(finalScale, finalScale, finalScale);
    }
}
