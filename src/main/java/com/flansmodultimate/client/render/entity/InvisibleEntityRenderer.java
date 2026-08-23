package com.flansmodultimate.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/** Renderer for logical seat/wheel proxies whose visuals belong to the root model. */
public final class InvisibleEntityRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState>
{
    public InvisibleEntityRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState()
    {
        return new EntityRenderState();
    }
}
