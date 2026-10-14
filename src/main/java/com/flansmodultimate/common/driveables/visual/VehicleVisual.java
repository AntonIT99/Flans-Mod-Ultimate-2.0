package com.flansmodultimate.common.driveables.visual;

/** Wheel and track motion of a ground vehicle. */
public interface VehicleVisual extends DriveableVisual
{
    /** @return rolling angle of the wheels, in degrees */
    float getWheelAngle();

    float getPrevWheelAngle();

    /** @return steering angle of the wheels, in degrees */
    float getWheelYaw();

    float getPrevWheelYaw();

    /** @return phase of the left track or walker leg cycle, from 0 to 1 */
    float getLeftTrackProgress();

    float getRightTrackProgress();

    /** @return how far the left track phase moved this tick; its sign tells forward from reverse */
    float getLeftTrackStep();

    float getRightTrackStep();
}
