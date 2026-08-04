package com.flansmodultimate.client.input;

import com.flansmodultimate.api.IControllable;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MouseInputHandler
{
    private static final float FLIGHT_MOUSE_SENSITIVITY = 0.003F;
    private static final float FLIGHT_CONTROL_RETURN = 0.86F;
    private static final float CONTROL_DEADZONE = 0.002F;

    private static int flightDriveableId = -1;
    private static float flightPitchControl;
    private static float flightRollControl;

    /** Recentres the virtual flight stick and validates its current mount once per client tick. */
    @OnlyIn(Dist.CLIENT)
    public static void beginTick(Player player)
    {
        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        if (!isMouseFlightActive(player, driveable))
        {
            resetFlightControls();
            return;
        }

        if (flightDriveableId != driveable.getId())
        {
            resetFlightControls();
            flightDriveableId = driveable.getId();
            return;
        }

        flightPitchControl = recenter(flightPitchControl);
        flightRollControl = recenter(flightRollControl);
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
        {
            flightDriveableId = driveable.getId();
            flightPitchControl = Mth.clamp(flightPitchControl + (float) dy * FLIGHT_MOUSE_SENSITIVITY, -1F, 1F);
            flightRollControl = Mth.clamp(flightRollControl + (float) dx * FLIGHT_MOUSE_SENSITIVITY, -1F, 1F);
            return;
        }

        IControllable controllable = KeyInputHandler.resolveControllable(player);
        if (controllable != null)
            controllable.onMouseMoved(dx, dy);
    }

    public static float getFlightPitchControl()
    {
        return flightPitchControl;
    }

    public static float getFlightRollControl()
    {
        return flightRollControl;
    }

    public static void resetFlightControls()
    {
        flightDriveableId = -1;
        flightPitchControl = 0F;
        flightRollControl = 0F;
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
