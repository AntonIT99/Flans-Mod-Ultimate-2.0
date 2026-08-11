package com.flansmodultimate.mixin;

import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.common.entity.Seat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/**
 * Changes only Camera.setup's requested third-person distance. Camera still
 * passes this value through its vanilla getMaxZoom collision probes.
 */
@Mixin(Camera.class)
public abstract class DriveableCameraMixin
{
    @Shadow private Vec3 position;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    /**
     * Replace the nested player/seat tick anchor with the exact render-time
     * driveable seat transform before vanilla applies third-person zoom.
     */
    @Inject(method = "setup", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void flansmodultimate$smoothDriveableSeatCamera(BlockGetter level, Entity cameraEntity,
                                                            boolean detached, boolean reverse,
                                                            float partialTick, CallbackInfo callback)
    {
        if (!(cameraEntity instanceof Player player) || !(player.getVehicle() instanceof Seat seat)
            || seat.getDriveable() == null)
            return;

        Vec3 vanillaFeet = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()),
            Mth.lerp(partialTick, player.yo, player.getY()),
            Mth.lerp(partialTick, player.zo, player.getZ()));
        Vec3 riderFeet = seat.getDriveable().getInterpolatedSeatWorldPosition(seat.getSeatIndex(), partialTick)
            .add(0D, seat.getPassengerRidingOffset(player), 0D);
        setPosition(riderFeet.add(position.subtract(vanillaFeet)));
    }

    @ModifyConstant(method = "setup", constant = @Constant(doubleValue = 4.0D))
    private double flansmodultimate$driveableCameraDistance(double vanillaDistance)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return vanillaDistance;

        var controllable = KeyInputHandler.resolveControllable(player);
        if (controllable == null)
            return vanillaDistance;

        float requestedDistance = controllable.getCameraDistance();
        return Float.isFinite(requestedDistance)
            ? Mth.clamp(requestedDistance, 1F, 64F)
            : vanillaDistance;
    }
}
