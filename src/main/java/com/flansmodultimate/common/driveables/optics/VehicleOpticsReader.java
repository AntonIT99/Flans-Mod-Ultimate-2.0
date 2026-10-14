package com.flansmodultimate.common.driveables.optics;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.types.TypeFile;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Reads the Krishna optics syntax in source order, including aliases and repeated last-wins values. */
public final class VehicleOpticsReader
{
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String SEAT_HUD_PREFIX = "seatoptics";
    private static final String KEY_PILOT_OPTICS = "pilotoptics";
    private static final String PILOT_HUD_PREFIX = KEY_PILOT_OPTICS + "hud";
    /** Every key {@link #read} has a case for. A line with another key is not split at all, so a new case needs its key here. */
    private static final Set<String> KEYS = Set.of("hasscope", "nightscope", "nightsight", "gunsight", "overlay", "gunsightzoom", "thermalguis", "thermalsight", "thermalvisioncolor",
        "thermalvisioncolour", "thermalvisiongeneration", "thermalvisiongen", "showcrosshair", "hasgunsightpos", "gunsightpos", "heligui", "passengergunsight", "opticsmodeseat", "seatopticsmode",
        KEY_PILOT_OPTICS, "driveroptics", "gunneroptic", "seatopticscamera", "opticscameraseat", "pilotopticscamera", "driveropticscamera", "gunneropticcamera", "passengerzoom", "seathasscope",
        "passengerhasscope", "seatautoscope", "passengerautoscope", "seatnightsight", "passengernightsight", "heliguiseat", "seatoverlay", "seatgunsight", "passengergunsights", "seatgunsightzoom",
        "seatgunsightzooms", "passengergunsightzoom", "seatthermalguis", "passengerthermalguis");

    private VehicleOpticsReader()
    {}

    public static VehicleOptics read(TypeFile file, List<SeatInfo> seats, Consumer<String> warning)
    {
        VehicleOptics driver = new VehicleOptics();
        driver.driverDefinition = true;
        String passengerOverlay = "";
        for (String line : file.getLines())
        {
            String trimmed = line.trim();
            // Most lines of a vehicle are blank, a comment or not about optics, and are not worth splitting to find that out.
            String key = firstToken(trimmed).toLowerCase(Locale.ROOT);
            if (!KEYS.contains(key) && !key.startsWith(SEAT_HUD_PREFIX) && !key.startsWith(PILOT_HUD_PREFIX))
                continue;
            String[] s = WHITESPACE.split(trimmed);
            try
            {
                switch (key)
                {
                    case "hasscope" -> driver.hasScope = Boolean.parseBoolean(s[1]);
                    case "nightscope", "nightsight" -> driver.nightSight = Boolean.parseBoolean(s[1]);
                    case "gunsight" -> driver.readOverlays(s, 1);
                    case "overlay" -> driver.fallbackOverlay = s[1];
                    case "gunsightzoom" -> driver.readZooms(s, 1);
                    case "thermalguis" -> driver.readThermal(s, 1);
                    case "thermalsight" -> driver.thermalSight = Boolean.parseBoolean(s[1]);
                    case "thermalvisioncolor", "thermalvisioncolour" -> driver.thermalPalette = ThermalPalette.parse(s[1]);
                    case "thermalvisiongeneration", "thermalvisiongen" -> driver.thermalGeneration = VehicleOptics.generation(s[1]);
                    case "showcrosshair" -> driver.showCrosshair = Boolean.parseBoolean(s[1]);
                    case "hasgunsightpos" -> driver.hasCamera = Boolean.parseBoolean(s[1]);
                    case "gunsightpos" -> driver.camera = vector(s, 1);
                    case "heligui", "passengergunsight" -> passengerOverlay = s[1];
                    case "opticsmodeseat", "seatopticsmode" -> setMode(seat(seats, s[1]), s[2]);
                    case KEY_PILOT_OPTICS, "driveroptics", "gunneroptic" -> setMode(seat(seats, "0"), s[1]);
                    case "seatopticscamera", "opticscameraseat" -> setCamera(seat(seats, s[1]), s, 2);
                    case "pilotopticscamera", "driveropticscamera", "gunneropticcamera" -> setCamera(seat(seats, "0"), s, 1);
                    case "passengerzoom" -> seat(seats, s[1]).zoom = VehicleOptics.positive(s[2], 1F);
                    case "seathasscope", "passengerhasscope" -> seat(seats, s[1]).hasScope = Boolean.parseBoolean(s[2]);
                    case "seatautoscope", "passengerautoscope" -> seat(seats, s[1]).autoScope = Boolean.parseBoolean(s[2]);
                    case "seatnightsight", "passengernightsight" -> seat(seats, s[1]).nightSight = Boolean.parseBoolean(s[2]);
                    case "heliguiseat", "seatoverlay" -> seat(seats, s[1]).seatOverlay = Boolean.parseBoolean(s[2]);
                    case "seatgunsight", "passengergunsights" -> {
                        VehicleOptics optic = seat(seats, s[1]);
                        optic.readOverlays(s, 2);
                        optic.hasScope = true;
                        if (!optic.opticsMode)
                            optic.seatOverlay = true;
                    }
                    case "seatgunsightzoom", "seatgunsightzooms", "passengergunsightzoom" -> seat(seats, s[1]).readZooms(s, 2);
                    case "seatthermalguis", "passengerthermalguis" -> {
                        VehicleOptics optic = seat(seats, s[1]);
                        optic.readThermal(s, 2);
                        optic.hasScope = true;
                    }
                    default -> {
                        if (key.startsWith(SEAT_HUD_PREFIX))
                            seat(seats, s[1]).hud.read(key.substring(SEAT_HUD_PREFIX.length()), s, 2);
                        else if (key.startsWith(PILOT_HUD_PREFIX))
                        {
                            OpticsHud hud = seat(seats, "0").hud;
                            boolean override = hud.overridePilotDefaults;
                            hud.read(key.substring(KEY_PILOT_OPTICS.length()), s, 1);
                            hud.overridePilotDefaults = override;
                        }
                    }
                }
            }
            catch (IllegalArgumentException | IndexOutOfBoundsException ex)
            {
                warning.accept("Invalid optics definition: " + line + " (" + ex.getMessage() + ")");
            }
        }
        for (SeatInfo seat : seats)
            if (seat != null)
                seat.getOptics().fallbackOverlay = passengerOverlay;
        return driver;
    }

    /** The part of a trimmed line before the first character {@link #WHITESPACE} splits on. */
    private static String firstToken(String trimmed)
    {
        for (int i = 0; i < trimmed.length(); i++)
        {
            char c = trimmed.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r')
                return trimmed.substring(0, i);
        }
        return trimmed;
    }

    private static VehicleOptics seat(List<SeatInfo> seats, String value)
    {
        int id = Integer.parseInt(value);
        if (id < 0 || id >= seats.size() || seats.get(id) == null)
            throw new IllegalArgumentException("Unknown seat " + id);
        return seats.get(id).getOptics();
    }

    private static void setMode(VehicleOptics optic, String value)
    {
        optic.opticsMode = Boolean.parseBoolean(value);
        optic.hasScope = true;
        if (optic.opticsMode)
            optic.seatOverlay = false;
    }

    private static void setCamera(VehicleOptics optic, String[] s, int offset)
    {
        optic.camera = vector(s, offset);
        optic.hasCamera = true;
    }

    private static Vector3f vector(String[] s, int offset)
    {
        float x = Float.parseFloat(s[offset]);
        float y = Float.parseFloat(s[offset + 1]);
        float z = Float.parseFloat(s[offset + 2]);
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z))
            throw new IllegalArgumentException("Non-finite optics camera position");
        return new Vector3f(x / 16F, y / 16F, z / 16F);
    }
}
