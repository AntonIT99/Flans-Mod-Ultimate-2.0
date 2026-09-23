package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.InventoryHolderEntity;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The stand-in wears the survivor's look, as the 1.7.10 fake player did. */
public class InventoryHolderRenderer extends HumanoidMobRenderer<InventoryHolderEntity, HumanoidModel<InventoryHolderEntity>>
{
    public InventoryHolderRenderer(EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull InventoryHolderEntity entity)
    {
        return ApocalypseContent.SURVIVOR_TEXTURE;
    }
}
