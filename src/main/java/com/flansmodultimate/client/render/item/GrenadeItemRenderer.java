package com.flansmodultimate.client.render.item;

import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.client.render.RenderTypeBufferSource;
import com.flansmodultimate.common.item.GrenadeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GrenadeItemRenderer
{
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, RenderTypeBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (stack.getItem() instanceof GrenadeItem grenadeItem && grenadeItem.useCustomRenderer(context))
        {
            IModelBase model = ModelCache.getOrLoadTypeModel(grenadeItem.getConfigType());
            if (model != null)
            {
                int color = grenadeItem.getConfigType().getColour();
                float red = (color >> 16 & 255) / 255F;
                float green = (color >> 8 & 255) / 255F;
                float blue = (color & 255) / 255F;
                LegacyTransformApplier.renderModel(model, grenadeItem.getConfigType(), grenadeItem.getConfigType().getTexture(), poseStack, buffer, packedLight, packedOverlay, red, green, blue, 1F);
                return;
            }
        }

        ICustomItemRenderer.renderItemFallback(stack, context, poseStack, buffer, packedLight, packedOverlay);
    }
}
