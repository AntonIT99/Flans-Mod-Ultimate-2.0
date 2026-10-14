package com.flansmodultimate.common.driveables.visual;

/** The aim of one seat, as its registered gun model follows it. Angles are in degrees, relative to the hull. */
public interface SeatVisual
{
    float getAimYaw();

    float getPrevAimYaw();

    float getAimPitch();

    float getPrevAimPitch();

    boolean isDriverSeat();

    /** @return whether any of the given driveable inputs is held from this seat */
    boolean isInputDown(int input);
}
