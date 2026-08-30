package com.flansmodultimate.mixin;

import com.flansmodultimate.client.input.MouseInputHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends free look to the seat a rider occupies instead of to its world rotation.
 *
 * <p>A rider's aim is stored in the driveable's own frame and composed with the
 * driveable orientation to build the camera. Letting vanilla add the mouse
 * delta to the rider's world yaw and pitch feeds a world space rotation into
 * that frame, so once the driveable banks the view no longer follows the screen
 * axes the player is aiming along. Redirecting the turn here also keeps the
 * input at frame rate and outside the vanilla pitch clamp, which would
 * otherwise swallow look input whenever the composed view neared vertical.</p>
 */
@Mixin(MouseHandler.class)
public abstract class MountedRiderTurnMixin
{
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void flansmodultimate$turnMountedRider(LocalPlayer player, double yawDelta, double pitchDelta)
    {
        if (!MouseInputHandler.turnMountedRider(player, yawDelta, pitchDelta))
            player.turn(yawDelta, pitchDelta);
    }
}
