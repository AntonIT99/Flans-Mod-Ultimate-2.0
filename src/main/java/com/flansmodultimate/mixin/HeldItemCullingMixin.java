package com.flansmodultimate.mixin;

import com.flansmodultimate.common.item.IFlanItem;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Holders of items with oversized parts such as laser beams skip frustum culling but keep the distance check. */
@Mixin(EntityRenderer.class)
public abstract class HeldItemCullingMixin
{
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$disableCulling(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> result)
    {
        if (entity instanceof LivingEntity living && (isCullingDisabled(living.getMainHandItem()) || isCullingDisabled(living.getOffhandItem())))
            result.setReturnValue(entity.shouldRender(camX, camY, camZ));
    }

    private static boolean isCullingDisabled(ItemStack stack)
    {
        return stack.getItem() instanceof IFlanItem<?> flanItem && flanItem.getConfigType().isFrustumCullingDisabled(stack);
    }
}
