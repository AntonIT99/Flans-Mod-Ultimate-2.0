package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings({"unused", "java:S1104"})
public abstract class ModelBase extends Model<Unit> implements IModelBase
{
    public int textureWidth = TEXTURE_WIDTH;
    public int textureHeight = TEXTURE_HEIGHT;

    @Getter
    private final List<ModelRenderer> boxList = new ArrayList<>();
    @Getter
    private final Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    @Getter @Setter
    private Identifier texture;
    @Getter @Setter
    private float scale = 1F;

    protected ModelBase()
    {
        super(new ModelPart(List.of(), Map.of()), RenderTypes::entityTranslucent);
    }

    @Override
    public int getTextureWidth()
    {
        return textureWidth;
    }

    @Override
    public int getTextureHeight()
    {
        return textureHeight;
    }

    @Override
    public TextureOffset getTextureOffset(String partName)
    {
        return IModelBase.super.getTextureOffset(partName);
    }

    @Override
    public void setTextureOffset(String partName, int x, int y)
    {
        IModelBase.super.setTextureOffset(partName, x, y);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        IModelBase.super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
    {
        IModelBase.super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
    }

    @Override
    public void setLivingAnimations(LivingEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime)
    {
        IModelBase.super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
    }

    @Override
    public ModelRenderer getRandomModelBox(Random rand)
    {
        return IModelBase.super.getRandomModelBox(rand);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        for (ModelRenderer modelRenderer : boxList)
        {
            modelRenderer.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
        }
    }

}
