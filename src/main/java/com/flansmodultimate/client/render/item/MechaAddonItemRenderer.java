package com.flansmodultimate.client.render.item;

import com.flansmod.client.model.ModelMechaTool;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MechaAddonItemRenderer
{
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (!(stack.getItem() instanceof MechaAddonItem mechaAddonItem)
            || !mechaAddonItem.useCustomRenderer(context)
            || !(ModelCache.getOrLoadTypeModel(mechaAddonItem.getConfigType()) instanceof ModelMechaTool model))
        {
            ICustomItemRenderer.renderItemFallback(stack, context, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        MechaItemType type = mechaAddonItem.getConfigType();
        ResourceLocation texture = type.getTexture();
        int color = type.getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);

        poseStack.pushPose();
        applyDisplayTransform(context, poseStack);
        LegacyTransformApplier.applyModelTransform(model, type, poseStack);
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
        {
            model.renderAll(poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)),
                packedLight, packedOverlay, red, green, blue, 1F, type.getModelScale(), 0F, renderPass);
        }
        poseStack.popPose();
    }

    private static void applyDisplayTransform(ItemDisplayContext context, PoseStack poseStack)
    {
        switch (context)
        {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND ->
            {
                poseStack.translate(-0.15F, 0.1F, -0.1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
                poseStack.scale(1.1F, 1.1F, 1.1F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
            {
                poseStack.translate(-0.1F, -0.1F, 0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
                poseStack.scale(0.9F, 0.9F, 0.9F);
            }
            case GROUND ->
            {
                poseStack.mulPose(Axis.YP.rotationDegrees(45F));
                poseStack.scale(1.15F, 1.15F, 1.15F);
            }
            case GUI ->
            {
                poseStack.mulPose(Axis.XP.rotationDegrees(20F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-45F));
                poseStack.scale(0.9F, 0.9F, 0.9F);
            }
            case FIXED -> poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
            default ->
            {
                // no-op
            }
        }
    }
}
