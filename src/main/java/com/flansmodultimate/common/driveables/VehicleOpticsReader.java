package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.types.TypeFile;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Reads the Krishna optics syntax in source order, including aliases and repeated last-wins values. */
public final class VehicleOpticsReader
{
    private VehicleOpticsReader() {}

    public static VehicleOptics read(TypeFile file, List<SeatInfo> seats, Consumer<String> warning)
    {
        VehicleOptics driver = new VehicleOptics();
        driver.driverDefinition = true;
        String passengerOverlay = "";
        for (String line : file.getLines())
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue;
            String[] s = trimmed.split("\\s+");
            String key = s[0].toLowerCase(Locale.ROOT);
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
                    case "showcrosshair" -> driver.showCrosshair = Boolean.parseBoolean(s[1]);
                    case "hasgunsightpos" -> driver.hasCamera = Boolean.parseBoolean(s[1]);
                    case "gunsightpos" -> driver.camera = vector(s, 1);
                    case "heligui", "passengergunsight" -> passengerOverlay = s[1];
                    case "opticsmodeseat", "seatopticsmode" -> setMode(seat(seats, s[1]), s[2]);
                    case "pilotoptics", "driveroptics", "gunneroptic" -> setMode(seat(seats, "0"), s[1]);
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
                        if (!optic.opticsMode) optic.seatOverlay = true;
                    }
                    case "seatgunsightzoom", "seatgunsightzooms", "passengergunsightzoom" -> seat(seats, s[1]).readZooms(s, 2);
                    case "seatthermalguis", "passengerthermalguis" -> {
                        VehicleOptics optic = seat(seats, s[1]);
                        optic.readThermal(s, 2);
                        optic.hasScope = true;
                    }
                    default -> {
                        if (key.startsWith("seatoptics"))
                            seat(seats, s[1]).hud.read(key.substring("seatoptics".length()), s, 2);
                        else if (key.startsWith("pilotopticshud"))
                        {
                            OpticsHud hud = seat(seats, "0").hud;
                            boolean override = hud.overridePilotDefaults;
                            hud.read(key.substring("pilotoptics".length()), s, 1);
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
            if (seat != null) seat.getOptics().fallbackOverlay = passengerOverlay;
        return driver;
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
        if (optic.opticsMode) optic.seatOverlay = false;
    }

    private static void setCamera(VehicleOptics optic, String[] s, int offset)
    {
        optic.camera = vector(s, offset);
        optic.hasCamera = true;
    }

    private static Vector3f vector(String[] s, int offset)
    {
        float x = Float.parseFloat(s[offset]), y = Float.parseFloat(s[offset + 1]), z = Float.parseFloat(s[offset + 2]);
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z))
            throw new IllegalArgumentException("Non-finite optics camera position");
        return new Vector3f(x / 16F, y / 16F, z / 16F);
    }
}
