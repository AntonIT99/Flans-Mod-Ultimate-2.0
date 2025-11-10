package com.flansmodultimate.common.item;

import com.flansmodultimate.client.render.CustomBewlr;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface ICustomRendererItem<T extends InfoType> extends IFlanItem<T>
{
    /**
     * Override this method and reuse the default implementation to enable custom item rendering
     */
    @OnlyIn(Dist.CLIENT)
    default void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                Minecraft mc = Minecraft.getInstance();
                return new CustomBewlr(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        });
    }

    boolean useCustomRendererInHand();

    boolean useCustomRendererOnGround();

    boolean useCustomRendererInFrame();

    boolean useCustomRendererInGui();

    @OnlyIn(Dist.CLIENT)
    void renderItem(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);
}
