package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.Random;

public interface IModelBase
{
    int TEXTURE_WIDTH = 64;
    int TEXTURE_HEIGHT = 32;

    ResourceLocation getTexture();

    void setTexture(ResourceLocation texture);

    List<ModelRenderer> getBoxList();

    Map<String, TextureOffset> getModelTextureMap();

    void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha);

    default int getTextureWidth()
    {
        return TEXTURE_WIDTH;
    }

    default int getTextureHeight()
    {
        return TEXTURE_HEIGHT;
    }

    default TextureOffset getTextureOffset(String partName)
    {
        return getModelTextureMap().get(partName);
    }

    default void setTextureOffset(String partName, int x, int y)
    {
        getModelTextureMap().put(partName, new TextureOffset(x, y));
    }

    default void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {}

    default void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {}

    default void setLivingAnimations(LivingEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {}

    default ModelRenderer getRandomModelBox(Random rand)
    {
        return getBoxList().get(rand.nextInt(getBoxList().size()));
    }

    static void copyModelAngles(ModelRenderer source, ModelRenderer dest)
    {
        dest.rotateAngleX = source.rotateAngleX;
        dest.rotateAngleY = source.rotateAngleY;
        dest.rotateAngleZ = source.rotateAngleZ;
        dest.rotationPointX = source.rotationPointX;
        dest.rotationPointY = source.rotationPointY;
        dest.rotationPointZ = source.rotationPointZ;
    }
}
