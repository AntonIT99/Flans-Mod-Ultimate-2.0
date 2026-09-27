package com.flansmodultimate.client.input;

import com.flansmodultimate.client.render.MountedCameraView;
import com.flansmodultimate.client.render.VehicleOpticsClient;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.IControllable;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    /** Degrees Entity.turn applies per unit of sensitivity scaled mouse movement. */
    private static final float TURN_DEGREES_PER_UNIT = 0.15F;

    private static int flightDriveableId = -1;
    private static int aaGunTurnId = -1;
    private static int aaGunViewId = -1;
    private static long lastAAGunTurnNanos;
    private static int viewSeatId = -1;
    private static float synchronizedFlightViewYaw;
    private static float synchronizedFlightViewPitch;
    private static boolean flightViewSynchronized;
    @Getter
    private static float flightPitchControl;
    @Getter
    private static float flightRollControl;

    /** Recentres the virtual flight stick and validates its current mount once per client tick. */
    public static void beginTick(Player player)
    {
        bindAAGunView(player);
        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        if (!isMouseFlightActive(player, driveable))
        {
            resetFlightControls();
            bindMountedSeatView(player, driveable);
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

        // Seats take their look input straight from Entity.turn, which runs at
        // frame rate instead of once per tick and carries vanilla sensitivity.
        if (player.getVehicle() instanceof Seat)
            return;

        IControllable controllable = KeyInputHandler.resolveControllable(player);
        if (controllable != null)
            controllable.onMouseMoved(dx, dy);
    }

    /**
     * Turns a rider's free look while it is seated in a driveable.
     *
     * <p>Vanilla would add the delta to the rider's world yaw and pitch, but
     * the rider looks out of a cockpit that pitches and rolls with the
     * driveable, and the camera composes the seat aim with that orientation.
     * Feeding world angles into a rolled frame is what makes mouse movement
     * point the view somewhere other than where the screen says it should go,
     * so the delta is applied to the seat's own aim instead. The rider's
     * rotation follows from that aim once per tick.</p>
     *
     * @return whether the seat consumed the input and vanilla must not turn the rider
     */
    public static boolean turnMountedRider(Player player, double yawDelta, double pitchDelta)
    {
        if (player.getVehicle() instanceof AAGun aaGun && aaGun.getFirstPassenger() == player)
        {
            float speed = aaGun.getConfigType() == null ? 0F : aaGun.getConfigType().getTraverseSpeed();
            if (speed <= 0F)
                return false;

            long now = System.nanoTime();
            // The input already includes vanilla sensitivity. Cap its angular
            // change by elapsed time, so frame rate cannot change traverse speed.
            double seconds = aaGunTurnId == aaGun.getId()
                ? Mth.clamp((now - lastAAGunTurnNanos) * 1.0E-9D, 0D, 0.05D)
                : 1D / 60D;
            aaGunTurnId = aaGun.getId();
            lastAAGunTurnNanos = now;
            double maxInput = speed * seconds / TURN_DEGREES_PER_UNIT;
            player.turn(Mth.clamp(yawDelta, -maxInput, maxInput),
                Mth.clamp(pitchDelta, -maxInput, maxInput));
            return true;
        }
        aaGunTurnId = -1;

        if (!(player.getVehicle() instanceof Seat seat) || seat.getRiddenByEntity() != player
            || seat.getDriveable() == null)
            return false;

        // Mouse flight steers the aircraft rather than the view. That mode
        // reads the vanilla view delta back in beginTick, so leave it alone.
        if (MountedCameraView.isViewLockedToDriveable(seat.getDriveable(), seat))
            return false;

        float zoom = VehicleOpticsClient.zoom();
        seat.applyClientAimDelta((float) yawDelta * TURN_DEGREES_PER_UNIT / zoom,
            (float) pitchDelta * TURN_DEGREES_PER_UNIT / zoom);
        return true;
    }

    public static void resetFlightControls()
    {
        flightDriveableId = -1;
        flightPitchControl = 0F;
        flightRollControl = 0F;
        flightViewSynchronized = false;
    }

    /** Start a new ride looking down the barrel rather than across the seat. */
    private static void bindAAGunView(Player player)
    {
        if (!(player.getVehicle() instanceof AAGun aaGun) || aaGun.getFirstPassenger() != player)
        {
            aaGunViewId = -1;
            return;
        }
        if (aaGunViewId == aaGun.getId())
            return;

        aaGunViewId = aaGun.getId();
        float yaw = player.getYRot() + Mth.wrapDegrees(aaGun.getGunYaw() - player.getYRot());
        player.setYRot(yaw);
        player.setXRot(aaGun.getGunPitch());
        player.yRotO = yaw;
        player.xRotO = aaGun.getGunPitch();
        player.yHeadRot = player.yHeadRotO = yaw;
        player.yBodyRot = player.yBodyRotO = yaw;
    }

    /** Adopts the aim the server holds for a seat the local player just took. */
    private static void bindMountedSeatView(Player player, Driveable driveable)
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
        }
    }

    private static boolean isMouseFlightActive(Player player, Driveable driveable)
    {
        return player.getVehicle() instanceof Seat seat && seat.getDriveable() == driveable
            && seat.getRiddenByEntity() == player
            && MountedCameraView.isViewLockedToDriveable(driveable, seat);
    }

    private static float recenter(float control)
    {
        float value = control * FLIGHT_CONTROL_RETURN;
        return Math.abs(value) < CONTROL_DEADZONE ? 0F : value;
    }
}
