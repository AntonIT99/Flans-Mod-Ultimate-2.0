package com.flansmodultimate.common.distant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * How far the client draws driveables as entities while a far-terrain renderer such as Distant Horizons
 * takes over beyond the vanilla chunks. Set by the client every tick and read by {@code Driveable}, which is
 * common code, so it holds a plain value rather than reaching into client classes. Always 0 on a server.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantRenderRange
{
    private static volatile double driveableRenderDistance;

    /** Camera distance in blocks up to which driveables are drawn as entities, or 0 for vanilla behaviour. */
    public static double driveableRenderDistance()
    {
        return driveableRenderDistance;
    }

    public static void setDriveableRenderDistance(double distance)
    {
        driveableRenderDistance = Double.isFinite(distance) && distance > 0D ? distance : 0D;
    }
}
