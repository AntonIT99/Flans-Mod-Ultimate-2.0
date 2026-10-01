package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.PlaneRiderRotation;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/** Makes every visible rider layer inherit the aircraft's pitch and roll. */
@Mixin(PlayerRenderer.class)
public abstract class SeatedPlayerRendererMixin
{
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
        at = @At("RETURN"))
    private void flansmodultimate$rotateRiderWithPlane(AbstractClientPlayer player, PoseStack poseStack,
                                                       float ageInTicks, float bodyYaw, float partialTick, float scale,
                                                       CallbackInfo callback)
    {
        if (!(player.getVehicle() instanceof Seat seat) || !(seat.getDriveable() instanceof Plane plane))
            return;

        // PlayerRenderer has already applied its world-yaw rotation. Move into
        // plane model space, apply the same pitch/roll order as DriveableRenderer,
        // then return to the rider's local body heading.
        poseStack.mulPose(PlaneRiderRotation.at(plane, partialTick, bodyYaw));
    }
}
