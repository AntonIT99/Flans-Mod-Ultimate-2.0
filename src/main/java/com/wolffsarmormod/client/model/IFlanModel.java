package com.wolffsarmormod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsmod.api.client.model.IModelBase;

import net.minecraft.world.item.ItemDisplayContext;

public interface IFlanModel<T extends InfoType> extends IModelBase
{
    void setType(T type);

    void renderItem(ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data);
}
