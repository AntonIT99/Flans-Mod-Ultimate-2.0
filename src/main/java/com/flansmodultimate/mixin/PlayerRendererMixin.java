package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.layer.*;
import com.flansmodultimate.common.driveables.physics.*;
import com.flansmodultimate.common.entity.*;
import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.client.player.*;
import net.minecraft.client.renderer.entity.player.*;
import net.minecraft.resources.*;

/**
 * Hooks into the player renderer: a team player class can replace the skin of the players wearing it,
 * and every visible rider layer inherits the aircraft's pitch and roll.
 */
@Mixin(PlayerRenderer.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class PlayerRendererMixin
{
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateOverridePlayerSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> callback)
    {
        ResourceLocation skin = PlayerSkinOverrides.getSkin(player, (PlayerRenderer) (Object) this);
        if (skin != null)
            callback.setReturnValue(skin);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("RETURN"))
    private void flansmodultimateRotateRiderWithPlane(AbstractClientPlayer player, PoseStack poseStack, float ageInTicks, float bodyYaw, float partialTick, float scale, CallbackInfo callback)
    {
        if (!(player.getVehicle() instanceof Seat seat) || !(seat.getDriveable() instanceof Plane plane))
            return;

        // PlayerRenderer has already applied its world-yaw rotation. Move into
        // plane model space, apply the same pitch/roll order as DriveableRenderer,
        // then return to the rider's local body heading.
        poseStack.mulPose(PlaneRiderRotation.at(plane, partialTick, bodyYaw));
    }
}
