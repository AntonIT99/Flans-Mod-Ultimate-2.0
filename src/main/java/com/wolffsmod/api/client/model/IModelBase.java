package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface IModelBase
{
    int TEXTURE_WIDTH = 64;
    int TEXTURE_HEIGHT = 32;

    ResourceLocation getTexture();

    void setTexture(ResourceLocation texture);

    void addModelBox(IModelRenderer modelRenderer);

    void forEachModelBox(Consumer<IModelRenderer> action);

    default float getScale()
    {
        return 1F;
    }

    default void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        forEachModelBox(modelRenderer -> modelRenderer.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, getScale()));
    }

    default int getTextureWidth()
    {
        return TEXTURE_WIDTH;
    }

    default int getTextureHeight()
    {
        return TEXTURE_HEIGHT;
    }

    TextureOffset getTextureOffset(String partName);

    void setTextureOffset(String partName, int x, int y);

    default void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {}

    default void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {}

    default void setLivingAnimations(LivingEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {}

    IModelRenderer getRandomModelBox(Random rand);

    static void copyModelAngles(IModelRenderer source, IModelRenderer dest)
    {
        dest.setRotateAngleX(source.getRotateAngleX());
        dest.setRotateAngleY(source.getRotateAngleY());
        dest.setRotateAngleZ(source.getRotateAngleZ());
        dest.setRotationPointX(source.getRotationPointX());
        dest.setRotationPointY(source.getRotationPointY());
        dest.setRotationPointZ(source.getRotationPointZ());
    }
}
