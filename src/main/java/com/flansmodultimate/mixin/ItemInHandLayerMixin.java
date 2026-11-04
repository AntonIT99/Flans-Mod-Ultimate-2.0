package com.flansmodultimate.mixin;

import com.flansmodultimate.api.client.equipment.FlansEquipmentRender;
import com.flansmodultimate.api.driveables.IEmplacement;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Applies per-hand local placement to held 3D Flan items, including Custom NPCs' standard held-item layer, and hides the hand items of
 * non-player operators manning a deployed or AA gun.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin
{
    @SuppressWarnings("java:S107") // Mixin callbacks must match renderArmWithItem's signature.
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateHideOperatorItems(LivingEntity holder, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int light,
        CallbackInfo callback)
    {
        // NPCs manning an emplacement fire the mounted weapon, so their own items would float through it.
        if (!(holder instanceof Player) && holder.getVehicle() instanceof IEmplacement)
            callback.cancel();
    }

    @SuppressWarnings("java:S107") // The redirect matches ItemInHandRenderer.renderItem's signature.
    @Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void flansmodultimateHeldItemTransform(ItemInHandRenderer renderer, LivingEntity holder, ItemStack stack, ItemDisplayContext context, boolean left, PoseStack pose,
        MultiBufferSource buffers, int light)
    {
        pose.pushPose();
        try
        {
            InteractionHand hand = left == (holder.getMainArm() == HumanoidArm.LEFT) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            FlansEquipmentRender.applyHeldItemTransform(pose, holder, stack, hand, context);
            renderer.renderItem(holder, stack, context, left, pose, buffers, light);
        }
        finally
        {
            pose.popPose();
        }
    }
}
