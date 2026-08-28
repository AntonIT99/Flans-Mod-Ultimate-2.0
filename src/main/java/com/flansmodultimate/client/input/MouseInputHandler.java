package com.flansmodultimate.client.input;

import com.flansmodultimate.api.IControllable;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MouseInputHandler
{
    private static final float FLIGHT_MOUSE_SENSITIVITY = 0.02F;
    private static final float FLIGHT_VIEW_TO_FLAP = FLIGHT_MOUSE_SENSITIVITY / 0.15F;
    private static final float FLIGHT_CONTROL_RETURN = 0.9F;
    private static final float MAX_FLAP_ANGLE = 20F;
    private static final float CONTROL_DEADZONE = 0.01F;

    private static int flightDriveableId = -1;
    private static int viewSeatId = -1;
    private static float synchronizedFlightViewYaw;
    private static float synchronizedFlightViewPitch;
    private static boolean flightViewSynchronized;
    @Getter
    private static float flightPitchControl;
    @Getter
    private static float flightRollControl;

    /** Recentres the virtual flight stick and validates its current mount once per client tick. */
    @OnlyIn(Dist.CLIENT)
    public static void beginTick(Player player)
    {
        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        if (!isMouseFlightActive(player, driveable))
        {
            resetFlightControls();
            captureMountedSeatView(player, driveable);
            return;
        }

        viewSeatId = -1;

        if (flightDriveableId != driveable.getId())
        {
            resetFlightControls();
            flightDriveableId = driveable.getId();
            return;
        }

        flightPitchControl = recenter(flightPitchControl);
        flightRollControl = recenter(flightRollControl);
        if (flightViewSynchronized && Minecraft.getInstance().screen == null)
        {
            float yawDelta = Mth.wrapDegrees(player.getYRot() - synchronizedFlightViewYaw);
            float pitchDelta = player.getXRot() - synchronizedFlightViewPitch;
            flightPitchControl = Mth.clamp(flightPitchControl - pitchDelta * FLIGHT_VIEW_TO_FLAP,
                -MAX_FLAP_ANGLE, MAX_FLAP_ANGLE);
            flightRollControl = Mth.clamp(flightRollControl + yawDelta * FLIGHT_VIEW_TO_FLAP,
                -MAX_FLAP_ANGLE, MAX_FLAP_ANGLE);
        }
        flightViewSynchronized = false;
    }

    /** Records the exact view restored after vanilla mouse input so the next tick can recover its delta. */
    @OnlyIn(Dist.CLIENT)
    public static void endTick(Player player)
    {
        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        if (!isMouseFlightActive(player, driveable))
        {
            flightViewSynchronized = false;
            return;
        }
        synchronizedFlightViewYaw = player.getYRot();
        synchronizedFlightViewPitch = player.getXRot();
        flightViewSynchronized = true;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleMouseMove(double dx, double dy)
    {
        if (Minecraft.getInstance().screen != null)
            return;

        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        if (isMouseFlightActive(player, driveable))
            return;

        // Vanilla has already applied mouse sensitivity to the mounted player's
        // view. Vehicle seats consume that view delta in beginTick instead of
        // relying on MouseHandler velocity, which may already have been reset.
        if (player.getVehicle() instanceof Seat)
            return;

        IControllable controllable = KeyInputHandler.resolveControllable(player);
        if (controllable != null)
            controllable.onMouseMoved(dx, dy);
    }

    public static void resetFlightControls()
    {
        flightDriveableId = -1;
        flightPitchControl = 0F;
        flightRollControl = 0F;
        flightViewSynchronized = false;
    }

    private static void captureMountedSeatView(Player player, Driveable driveable)
    {
        if (!(player.getVehicle() instanceof Seat seat) || driveable == null
            || seat.getDriveable() != driveable || seat.getRiddenByEntity() != player)
        {
            viewSeatId = -1;
            return;
        }

        if (viewSeatId != seat.getId())
        {
            viewSeatId = seat.getId();
            seat.synchronizeClientViewWithAim();
            return;
        }

        float yawDelta = Mth.wrapDegrees(player.getYRot() - seat.getMountedViewYaw());
        float mountedPitchBeforeInput = seat.getMountedViewPitch();
        float pitchDelta = player.getXRot() - mountedPitchBeforeInput;
        if (Math.abs(yawDelta) > 1.0E-4F || Math.abs(pitchDelta) > 1.0E-4F)
        {
            seat.applyClientAimDelta(yawDelta, pitchDelta);

            // Vanilla has already added the complete mouse delta to both xRot
            // and xRotO. If the seat rejects part of that delta at its pitch
            // limit, leaving xRotO outside the limit makes rendering interpolate
            // from the invalid angle back to the clamped one every frame.
            float mountedPitchAfterInput = seat.getMountedViewPitch();
            float appliedPitchDelta = mountedPitchAfterInput - mountedPitchBeforeInput;
            if (Math.abs(pitchDelta - appliedPitchDelta) > 1.0E-4F)
            {
                player.setXRot(mountedPitchAfterInput);
                player.xRotO = mountedPitchAfterInput;
            }
        }
    }

    private static boolean isMouseFlightActive(Player player, Driveable driveable)
    {
        if (!(driveable instanceof Plane) || !ModClient.isMouseControlEnabled())
            return false;
        return player.getVehicle() instanceof Seat seat && seat.isDriverSeat() && seat.getDriveable() == driveable
            && seat.getRiddenByEntity() == player;
    }

    private static float recenter(float control)
    {
        float value = control * FLIGHT_CONTROL_RETURN;
        return Math.abs(value) < CONTROL_DEADZONE ? 0F : value;
    }
}
