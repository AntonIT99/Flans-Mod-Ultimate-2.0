package com.flansmodultimate.apocalyse.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ItemEntityRenderer<T extends Entity> extends EntityRenderer<T, ItemEntityRenderer.State>
{
    private final Supplier<ItemStack> stackSupplier;
    private final ItemModelResolver itemModelResolver;
    private final float scale;

    public ItemEntityRenderer(EntityRendererProvider.Context context, Supplier<ItemStack> stackSupplier, float scale)
    {
        super(context);
        this.stackSupplier = stackSupplier;
        this.itemModelResolver = context.getItemModelResolver();
        this.scale = scale;
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick)
    {
        super.extractRenderState(entity, state, partialTick);
        state.yOffset = entity.getBbHeight() * 0.5F;
        state.scale = scale;
        itemModelResolver.updateForNonLiving(state.item, stackSupplier.get(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        poseStack.pushPose();
        poseStack.translate(0.0F, state.yOffset, 0.0F);
        poseStack.scale(state.scale, state.scale, state.scale);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState
    {
        private final ItemStackRenderState item = new ItemStackRenderState();
        private float yOffset;
        private float scale;
    }
}
