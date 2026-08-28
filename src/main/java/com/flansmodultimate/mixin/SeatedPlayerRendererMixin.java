package com.flansmodultimate.mixin;

import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;

/** Makes every visible rider layer inherit the aircraft's pitch and roll. */
@Mixin(PlayerRenderer.class)
public abstract class SeatedPlayerRendererMixin
{
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
        at = @At("RETURN"))
    private void flansmodultimate$rotateRiderWithPlane(AbstractClientPlayer player, PoseStack poseStack,
                                                       float ageInTicks, float bodyYaw, float partialTick,
                                                       CallbackInfo callback)
    {
        if (!(player.getVehicle() instanceof Seat seat) || !(seat.getDriveable() instanceof Plane plane))
            return;

        float planeYaw = Mth.rotLerp(partialTick, plane.getPrevYaw(), plane.getYaw());
        float pitch = Mth.rotLerp(partialTick, plane.getPrevPitch(), plane.getPitch());
        float roll = Mth.rotLerp(partialTick, plane.getPrevRoll(), plane.getRoll());
        float forwardYaw = plane.getEntityFacingYaw(planeYaw);
        float bodyOffset = Mth.wrapDegrees(bodyYaw - forwardYaw);

        // PlayerRenderer has already applied its world-yaw rotation. Move into
        // plane model space, apply the same pitch/roll order as DriveableRenderer,
        // then return to the rider's local body heading.
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyOffset - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        poseStack.mulPose(Axis.YP.rotationDegrees(90F - bodyOffset));
    }
}
