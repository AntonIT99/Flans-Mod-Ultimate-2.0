package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class SurvivorRenderer extends HumanoidMobRenderer<SurvivorEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>
{
    public SurvivorRenderer(EntityRendererProvider.Context context)
    {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState()
    {
        return new HumanoidRenderState();
    }

    @Override
    @NotNull
    public Identifier getTextureLocation(@NotNull HumanoidRenderState state)
    {
        return ApocalypseContent.survivorTexture;
    }
}
