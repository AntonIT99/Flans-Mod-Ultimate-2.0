package com.wolffsarmormod.common.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.client.render.CustomBewlr;
import com.wolffsarmormod.common.types.InfoType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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

    void renderItem(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Object... data);
}
