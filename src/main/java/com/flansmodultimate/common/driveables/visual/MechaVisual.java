package com.flansmodultimate.common.driveables.visual;

/** Leg motion of a mecha. */
public interface MechaVisual extends DriveableVisual
{
    /** @return phase of the walk cycle, from 0 to 1 */
    float getLegSwing();

    /** @return yaw the legs face, in degrees, in the same frame as the hull yaw */
    float getLegYaw();

    float getPrevLegYaw();
}
