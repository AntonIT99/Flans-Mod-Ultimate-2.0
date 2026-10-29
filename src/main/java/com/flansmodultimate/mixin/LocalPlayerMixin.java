package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.HullCarriedEntity;
import com.flansmodultimate.common.driveables.collision.HullCarry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;

/**
 * Hooks into the local player.
 *
 * <p>
 * Eases a turn a moving hull or turret carried the player through into the view over the frames up to the next tick.
 * On foot the view yaw is the player's yaw as it is, which a carry only changes once per tick; see {@link HullCarry}.
 * </p>
 */
@Mixin(LocalPlayer.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class LocalPlayerMixin
{
    @Inject(method = "getViewYRot", at = @At("RETURN"), cancellable = true)
    private void flansmodultimateEaseHullCarriedYaw(float partialTick, CallbackInfoReturnable<Float> callback)
    {
        LocalPlayer player = (LocalPlayer) (Object) this;
        HullCarry carry = ((HullCarriedEntity) player).flansmodultimateHullCarryIfPresent();
        // A passenger's view yaw is interpolated already.
        if (carry == null || player.isPassenger())
            return;
        float lag = carry.viewYawLag(player.tickCount, partialTick);
        if (lag != 0F)
            callback.setReturnValue(callback.getReturnValueF() - lag);
    }
}
