package com.flansmodultimate.client.render;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Single source of truth for the orientation of a driveable-mounted camera.
 *
 * <p>The third person boom is built inside {@code Camera.setup}, well before
 * {@code ViewportEvent.ComputeCameraAngles} can change the rendered angles.
 * Resolving both from here keeps the boom, the view direction and the screen
 * roll describing the same orientation instead of drifting apart by whatever
 * mouse movement vanilla folded into the rider's rotation this frame.</p>
 */
@OnlyIn(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MountedCameraView
{
    /**
     * While mouse flight is engaged the aircraft, not free look, owns the
     * rider's view, exactly as the 1.7.10 driver seat did.
     */
    public static boolean isViewLockedToDriveable(@Nullable Driveable driveable, @Nullable Seat seat)
    {
        return driveable instanceof Plane && seat != null && seat.isDriverSeat()
            && ModClient.isMouseControlEnabled();
    }

    /** Camera angles of the seat this entity rides, or null if it rides none. */
    @Nullable
    public static LegacyDriveableCoordinates.ViewAngles resolve(@Nullable Entity cameraEntity, float partialTick)
    {
        if (!(cameraEntity instanceof Player player) || !(player.getVehicle() instanceof Seat seat))
            return null;

        Driveable driveable = seat.getDriveable();
        if (driveable == null)
            return null;

        float partial = Mth.clamp(partialTick, 0F, 1F);
        boolean locked = isViewLockedToDriveable(driveable, seat);
        return LegacyDriveableCoordinates.mountedViewAngles(
            Mth.rotLerp(partial, driveable.getPrevYaw(), driveable.getYaw()),
            Mth.rotLerp(partial, driveable.getPrevPitch(), driveable.getPitch()),
            Mth.rotLerp(partial, driveable.getPrevRoll(), driveable.getRoll()),
            locked ? 0F : seat.getViewAimYaw(),
            locked ? 0F : seat.getViewAimPitch(),
            driveable instanceof Plane);
    }
}
