package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Legacy gunsight definition. Runtime selection belongs to the occupied Seat, never this shared type. */
@Getter
public final class VehicleOptics
{
    boolean hasScope;
    boolean driverDefinition;
    boolean nightSight;
    boolean autoScope;
    boolean opticsMode;
    boolean seatOverlay;
    boolean hasCamera;
    boolean thermalSight;
    boolean showCrosshair;
    float zoom = 1F;
    Vector3f camera = new Vector3f();
    String fallbackOverlay = "";
    String[] overlays = new String[0];
    float[] zooms = new float[0];
    final Set<Integer> thermalSights = new HashSet<>();
    final OpticsHud hud = new OpticsHud();

    public boolean available()
    {
        return hasScope || seatOverlay || opticsMode || autoScope;
    }

    /** An explicit AutoScope keeps the seat looking through its sight; the gunner cannot leave it. */
    public boolean forced()
    {
        return autoScope;
    }

    /**
     * Whether a gunner taking the seat starts out looking through its sight. A seat overlay does, as the
     * Krishna gunner seats did, but unlike AutoScope the gunner may lower it with the zoom key just as a
     * driver can.
     */
    public boolean startsActive()
    {
        return autoScope || seatOverlay;
    }

    public int sightCount()
    {
        if (driverDefinition)
            return Math.max(1, overlays.length);
        int count = Math.max(overlays.length, zooms.length);
        for (int index : thermalSights)
            count = Math.max(count, index + 1);
        return Math.max(1, count);
    }

    public String overlay(int sight)
    {
        return overlays.length == 0 ? fallbackOverlay : overlays[Math.max(0, Math.min(sight, overlays.length - 1))];
    }

    public float zoom(int sight)
    {
        return zooms.length == 0 ? zoom : zooms[Math.max(0, Math.min(sight, zooms.length - 1))];
    }

    public boolean thermal(int sight)
    {
        return thermalSights.contains(sight);
    }

    void readOverlays(String[] values, int start)
    {
        overlays = Arrays.copyOfRange(values, start, values.length);
    }

    void readZooms(String[] values, int start)
    {
        float[] parsed = new float[values.length - start];
        for (int i = 0; i < parsed.length; i++)
            parsed[i] = positive(values[start + i], 1F);
        zooms = parsed;
        if (zooms.length > 0)
            zoom = zooms[0];
    }

    void readThermal(String[] values, int start)
    {
        Set<Integer> parsed = new HashSet<>();
        for (int i = start; i < values.length; i++)
            parsed.add(Math.max(1, Integer.parseInt(values[i])) - 1);
        thermalSights.clear();
        thermalSights.addAll(parsed);
    }

    static float positive(String value, float minimum)
    {
        float parsed = Float.parseFloat(value);
        if (!Float.isFinite(parsed))
            throw new IllegalArgumentException("Non-finite optics value");
        return Math.max(minimum, parsed);
    }
}
