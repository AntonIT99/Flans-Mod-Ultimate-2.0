package com.flansmodultimate.mixin;

import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.render.MountedCameraView;
import com.flansmodultimate.client.render.VehicleOpticsClient;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.DeployedGunMuzzleGeometry;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.GunType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/**
 * Anchors mounted cameras to their weapon or driveable, and changes only
 * Camera.setup's requested third-person distance. Camera still passes that
 * distance through its vanilla getMaxZoom collision probes.
 */
@Mixin(Camera.class)
public abstract class DriveableCameraMixin
{
    @Shadow private Vec3 position;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    /**
     * Replace the nested player/seat tick anchor and the rider's own rotation
     * with the exact render-time driveable seat transform, before vanilla
     * applies third-person zoom.
     *
     * <p>Both halves matter, and each one hides in the view the other breaks.
     * Vanilla builds the third-person boom from the rider's rotation, which
     * carries whatever mouse movement was folded into it since the last tick,
     * so a detached camera swims around a steady plane. Vanilla also raises the
     * eye along world up rather than along the driveable's up, which slides the
     * first-person view out of a banking cockpit while the boom conceals it.</p>
     */
    @Inject(method = "setup", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void flansmodultimate$positionMountedCamera(BlockGetter level, Entity cameraEntity,
                                                        boolean detached, boolean reverse,
                                                        float partialTick, CallbackInfo callback)
    {
        if (!detached && cameraEntity instanceof Player player && player.getVehicle() instanceof DeployedGun gun)
        {
            GunType type = gun.getConfigType();
            if (type != null)
            {
                // The rider receives mouse look between entity ticks. Interpolate
                // that view so the camera and rendered barrel cross center together.
                float baseYaw = Direction.from2DDataValue(gun.getGunDirection()).toYRot();
                float riderYaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
                float side = Math.abs(type.getSideViewLimit());
                float yaw = baseYaw + Mth.clamp(Mth.wrapDegrees(riderYaw - baseYaw), -side, side);
                float top = Math.min(type.getTopViewLimit(), type.getBottomViewLimit());
                float bottom = Math.max(type.getTopViewLimit(), type.getBottomViewLimit());
                float pitch = Mth.clamp(Mth.lerp(partialTick, player.xRotO, player.getXRot()), top, bottom);
                Vec3 muzzle = type.getMeasuredDeployableMuzzle();
                if (muzzle != null)
                {
                    double cameraY = gun.getY() + DeployedGunMuzzleGeometry.recommendedCameraHeight(
                        muzzle, type.getModelScale());
                    setPosition(new Vec3(position.x, cameraY, position.z));
                }
                // PivotHeight is only the legacy projectile fallback. Model
                // geometry controls the view height when available; otherwise
                // vanilla's interpolated standing eye position remains intact.
                setRotation(yaw, pitch);
            }
            return;
        }

        if (!(cameraEntity instanceof Player player) || !(player.getVehicle() instanceof Seat seat)
            || seat.getDriveable() == null)
            return;

        // Vanilla stacks the eye height on world up. 1.7.10 instead placed the
        // rider's eye at the seat anchor plus its whole offset rotated by the
        // driveable, which is what keeps a pilot's head in the cockpit through
        // a roll. Rotating only the seating offset and then rising along world
        // up slides the eye more than a block sideways out of a banked plane,
        // and the third person boom hides it, so it shows up in first person.
        double vanillaFeetY = Mth.lerp(partialTick, player.yo, player.getY());
        double eyeOffset = seat.getPassengerRidingOffset(player) + (position.y - vanillaFeetY);
        setPosition(seat.getDriveable().getInterpolatedRiderWorldPosition(
            seat.getSeatIndex(), eyeOffset, partialTick));
        if (!detached && VehicleOpticsClient.activeSeat() == seat && seat.getOptics() != null && seat.getOptics().isHasCamera())
            setPosition(seat.getDriveable().getInterpolatedOpticsPosition(seat, partialTick));

        // Build the boom from the same composed seat view this frame renders
        // with. The rider's own rotation only catches up once per tick, so a
        // boom hung off it lags both the driveable and the rider's own look.
        LegacyDriveableCoordinates.ViewAngles view = MountedCameraView.resolve(player, partialTick);
        if (view != null)
            setRotation(view.yaw(), view.pitch());
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
