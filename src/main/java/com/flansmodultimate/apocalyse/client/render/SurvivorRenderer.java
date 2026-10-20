package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class SurvivorRenderer extends HumanoidMobRenderer<SurvivorEntity, HumanoidModel<SurvivorEntity>>
{
    public SurvivorRenderer(EntityRendererProvider.Context context)
    {
        super(context, new SurvivorModel<>(), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()));
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull SurvivorEntity entity)
    {
        return ApocalypseContent.SURVIVOR_TEXTURE;
    }
}
