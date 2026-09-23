package com.flansmodultimate.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.IModelRenderer;
import com.wolffsmod.api.client.model.TextureOffset;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "java:S1104"})
public abstract class ModelBase extends Model implements IModelBase
{
    public int textureWidth = TEXTURE_WIDTH;
    public int textureHeight = TEXTURE_HEIGHT;

    private final List<ModelRenderer> boxList = new ArrayList<>();
    private final Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    @Getter @Setter
    private ResourceLocation texture;
    @Getter @Setter
    private float scale = 1F;

    protected ModelBase()
    {
        super(RenderType::entityTranslucent);
    }

    @Override
    public void addModelBox(IModelRenderer modelRenderer)
    {
        if (!(modelRenderer instanceof ModelRenderer renderer))
            throw new IllegalArgumentException("Unsupported model renderer implementation: " + modelRenderer);
        boxList.add(renderer);
    }

    @Override
    public void forEachModelBox(Consumer<IModelRenderer> action)
    {
        boxList.forEach(action);
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
        return modelTextureMap.get(partName);
    }

    @Override
    public void setTextureOffset(String partName, int x, int y)
    {
        modelTextureMap.put(partName, new TextureOffset(x, y));
    }

    // Keep these concrete bridge methods: transformed legacy bytecode invokes them on ModelBase.
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
        return boxList.get(rand.nextInt(boxList.size()));
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, EnumRenderPass.DEFAULT);
    }

    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
    {
        for (ModelRenderer modelRenderer : boxList)
        {
            if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo)
            {
                modelRendererTurbo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
            }
            else if (renderPass == EnumRenderPass.DEFAULT)
            {
                modelRenderer.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
            }
        }
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int packedColor)
    {
        float alpha = ((packedColor >>> 24) & 0xFF) / 255F;
        float red = ((packedColor >>> 16) & 0xFF) / 255F;
        float green = ((packedColor >>> 8) & 0xFF) / 255F;
        float blue = (packedColor & 0xFF) / 255F;
        renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
