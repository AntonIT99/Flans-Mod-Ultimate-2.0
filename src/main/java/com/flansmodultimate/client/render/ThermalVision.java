package com.flansmodultimate.client.render;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.common.driveables.ThermalPalette;
import com.flansmodultimate.common.driveables.VehicleOptics;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Which thermal image the local player sees, if any: a thermal vehicle sight with its driveable's palette and
 * generation, or a thermal gun or attachment scope, which the Labjac Edition always showed white-hot at
 * generation 3.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThermalVision
{
    /** The generation the Labjac Edition used for hand-held thermal scopes. */
    static final int GUN_SCOPE_GENERATION = 3;

    /** Whether the local player is looking through any thermal sight. */
    public static boolean active()
    {
        return VehicleOpticsClient.thermal() || ModClient.isThermalScoped();
    }

    /**
     * Whether this client's lightmap is lit as if by night vision: through a night or thermal vehicle sight, as
     * before, and through a thermal gun scope, so a thermal image stays readable at night.
     */
    public static boolean opticalNightVision()
    {
        return VehicleOpticsClient.nightVision() || ModClient.isThermalScoped();
    }

    public static ThermalPalette palette()
    {
        VehicleOptics optics = vehicleOptics();
        return optics == null ? ThermalPalette.WHITE : optics.getThermalPalette();
    }

    /** 0 for the clean original image, otherwise the Labjac Edition generation from 1 to 3. */
    public static int generation()
    {
        VehicleOptics optics = vehicleOptics();
        if (optics != null)
            return optics.getThermalGeneration();
        return ModClient.isThermalScoped() ? GUN_SCOPE_GENERATION : 0;
    }

    /** The driver optics definition of the thermal vehicle sight in use, which carries the vehicle-wide settings. */
    private static VehicleOptics vehicleOptics()
    {
        if (!VehicleOpticsClient.thermal())
            return null;
        Seat seat = VehicleOpticsClient.activeSeat();
        if (seat == null || seat.getDriveable() == null || seat.getDriveable().getConfigType() == null)
            return null;
        return seat.getDriveable().getConfigType().getOptics();
    }
}
