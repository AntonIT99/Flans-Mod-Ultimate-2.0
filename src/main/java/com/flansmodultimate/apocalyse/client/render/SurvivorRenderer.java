package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SurvivorRenderer extends HumanoidMobRenderer<SurvivorEntity, HumanoidModel<SurvivorEntity>>
{
    public SurvivorRenderer(EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull SurvivorEntity entity)
    {
        return ApocalypseContent.survivorTexture;
    }
}
