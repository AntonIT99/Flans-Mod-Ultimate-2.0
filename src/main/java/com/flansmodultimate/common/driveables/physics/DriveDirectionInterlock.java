package com.flansmodultimate.common.driveables.physics;

/**
 * Direction interlock for the real-world ground drivetrain.
 *
 * <p>A gearbox cannot drive a vehicle backwards while it is still rolling
 * forwards, nor the other way round. Demand against the direction of travel
 * therefore leaves the drive in neutral and the vehicle only sheds its momentum.
 * The opposite gear is selected once the vehicle has all but stopped and stayed
 * so for {@link VehiclePhysicsConstants#DIRECTION_CHANGE_TICKS}, and its clutch
 * then takes up tractive force progressively over
 * {@link VehiclePhysicsConstants#CLUTCH_ENGAGE_TICKS}.
 *
 * <p>Without it, reverse demand became full launch force on the tick the vehicle
 * crossed zero. Reverse speeds are low enough that the whole run sits inside the
 * launch-limited regime, so even a heavy tank snapped into reverse in a few ticks.
 *
 * <p>Pulling away in the gear already selected is unaffected. One instance
 * belongs to one vehicle and {@link #advance} must be called exactly once per
 * tick, because the shift pause and the clutch are counted in calls.
 */
public final class DriveDirectionInterlock
{
    /** +1 forward, -1 reverse. A new vehicle sits in forward, fully engaged. */
    private int gear = 1;
    private int neutralTicks;
    private int engagedTicks = VehiclePhysicsConstants.CLUTCH_ENGAGE_TICKS;
    private boolean shifting;

    /**
     * Advances the drivetrain by one tick.
     *
     * @param demand         signed drive demand; only its sign is used
     * @param forwardSpeedMs signed longitudinal speed in m/s, positive forwards
     * @return the share of available tractive force transmitted this tick, from 0 to 1
     */
    public double advance(double demand, double forwardSpeedMs)
    {
        int requested = direction(demand);
        shifting = requested != 0 && requested != gear;
        if (!shifting)
        {
            neutralTicks = 0;
            if (requested != 0 && engagedTicks < VehiclePhysicsConstants.CLUTCH_ENGAGE_TICKS)
                engagedTicks++;
            return transmission();
        }
        double speed = Double.isFinite(forwardSpeedMs) ? forwardSpeedMs : 0D;
        // The shift pause only counts once the vehicle is no longer rolling
        // meaningfully against the requested direction.
        if (speed * requested < -VehiclePhysicsConstants.DIRECTION_CHANGE_SPEED_MS)
            neutralTicks = 0;
        else if (++neutralTicks >= VehiclePhysicsConstants.DIRECTION_CHANGE_TICKS)
        {
            gear = requested;
            neutralTicks = 0;
            engagedTicks = 0;
            shifting = false;
        }
        return 0D;
    }

    /** Whether this tick's demand opposed the selected gear, leaving the drivetrain in neutral. */
    public boolean isShifting()
    {
        return shifting;
    }

    /** The selected gear: +1 forward, -1 reverse. */
    public int getGear()
    {
        return gear;
    }

    private double transmission()
    {
        double engagement = Math.min(1D, engagedTicks / (double) VehiclePhysicsConstants.CLUTCH_ENGAGE_TICKS);
        // Clutch torque builds with plate pressure, so take-up starts gently.
        return engagement * engagement;
    }

    private static int direction(double demand)
    {
        if (!Double.isFinite(demand) || demand == 0D)
            return 0;
        return demand > 0D ? 1 : -1;
    }
}
