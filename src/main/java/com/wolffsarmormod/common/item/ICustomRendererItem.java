package com.wolffsarmormod.common.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.item.ItemDisplayContext;

public interface ICustomRendererItem
{
    boolean useCustomRendererInHand();

    boolean useCustomRendererOnGround();

    boolean useCustomRendererInFrame();

    boolean useCustomRendererInGui();

    void renderItem(ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data);
}
