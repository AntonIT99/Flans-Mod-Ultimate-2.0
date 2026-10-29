package com.flansmodultimate.mixin;

import com.flansmodultimate.api.client.NameTagRanges;
import com.flansmodultimate.api.driveables.IFlanSeatOccupant;
import com.flansmodultimate.api.entity.ILivingVisualEffects;
import com.flansmodultimate.common.driveables.physics.PlaneRiderRotation;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Controls living-entity name range, optional hurt/death effects and seated body alignment, including aircraft attitude. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityNameTagRangeMixin
{
    // Mounted bodies

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getVehicle()Lnet/minecraft/world/entity/Entity;", ordinal = 2))
    private Entity flansmodultimateBodyRotationMount(LivingEntity rider)
    {
        // Only the living-mount clamp's instanceof check: the earlier sitting checks still see the real vehicle.
        return rider instanceof IFlanSeatOccupant occupant && occupant.flansmodultimateHasSeatBodyRotation() ? null : rider.getVehicle();
    }

    @SuppressWarnings("NameDoesntMatchTargetClass") // Callback names describe the rider and body heading independently of mapping parameter names.
    @Inject(method = "setupRotations", at = @At("RETURN"))
    private void flansmodultimateRotateRiderWithPlane(LivingEntity rider, PoseStack pose, float ageInTicks, float bodyYaw, float partialTick, float scale, CallbackInfo callback)
    {
        if (rider.getVehicle() instanceof Seat seat && seat.getDriveable() instanceof Plane plane)
            pose.mulPose(PlaneRiderRotation.at(plane, partialTick, bodyYaw));
    }

    // Presentation and name range

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void flansmodultimateHurtOverlay(LivingEntity entity, float whiteOverlay, CallbackInfoReturnable<Integer> callback)
    {
        if (entity instanceof ILivingVisualEffects effects && !effects.flansmodultimateHurtFlash())
            callback.setReturnValue(OverlayTexture.pack(OverlayTexture.u(whiteOverlay), OverlayTexture.v(false)));
    }

    @Inject(method = "getFlipDegrees", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateDeathRotation(LivingEntity entity, CallbackInfoReturnable<Float> callback)
    {
        if (entity instanceof ILivingVisualEffects effects && !effects.flansmodultimateDeathRotation())
            callback.setReturnValue(0F);
    }

    @ModifyConstant(method = "shouldShowName*", constant = @Constant(floatValue = 64F))
    private float flansmodultimateNormalNameTagRange(float vanillaRange, LivingEntity entity)
    {
        return NameTagRanges.resolve(entity, ModCommonConfig.nameTagRenderRange(false));
    }

    @ModifyConstant(method = "shouldShowName*", constant = @Constant(floatValue = 32F))
    private float flansmodultimateSneakingNameTagRange(float vanillaRange, LivingEntity entity)
    {
        return NameTagRanges.resolve(entity, ModCommonConfig.nameTagRenderRange(true));
    }
}
