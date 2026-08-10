package com.flansmodultimate.apocalyse.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ItemEntityRenderer<T extends Entity> extends EntityRenderer<T>
{
    private final Supplier<ItemStack> stackSupplier;
    private final ItemRenderer itemRenderer;
    private final float scale;

    public ItemEntityRenderer(EntityRendererProvider.Context context, Supplier<ItemStack> stackSupplier, float scale)
    {
        super(context);
        this.stackSupplier = stackSupplier;
        this.itemRenderer = context.getItemRenderer();
        this.scale = scale;
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stackSupplier.get(), ItemDisplayContext.GROUND, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
