package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.DriveControl;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;

import java.util.EnumSet;
import java.util.Set;

/**
 * Holds an aircraft on the pitch, bank, heading and throttle its pilot wants, with the keys a player has. Each axis
 * steers by its error and against the rate it already turns at, so it settles without swinging past. A key is either held
 * or not, so an axis holds its keys for the share of ticks its command asks for, which the aircraft's control surfaces
 * smooth into a partial deflection. Angles are in degrees, pitch positive nose up, bank positive to the right and heading
 * Minecraft's yaw; no world access. One per pilot.
 */
final class FlightControl
{
    /** Stick per degree of pitch error, and per degree a tick of pitch rate to steer against. */
    static final float PITCH_GAIN = 0.08F;
    static final float PITCH_DAMPING = 0.9F;
    static final float BANK_GAIN = 0.05F;
    static final float BANK_DAMPING = 0.7F;
    static final float YAW_GAIN = 0.05F;
    static final float YAW_DAMPING = 0.6F;
    /** Throttle the lever may stand off what is wanted before it is moved. */
    static final float THROTTLE_SLACK = 0.02F;

    private final Pulse pitch = new Pulse(DriveControl.ASCEND, DriveControl.DESCEND);
    private final Pulse bank = new Pulse(DriveControl.ROLL_RIGHT, DriveControl.ROLL_LEFT);
    private final Pulse yaw = new Pulse(DriveControl.RIGHT, DriveControl.LEFT);
    /** The attitude last tick, to measure how fast it turns; NaN before the first. */
    private float lastPitch = Float.NaN;
    private float lastBank = Float.NaN;
    private float lastHeading = Float.NaN;

    /** Where the aircraft points and how far its throttle lever stands. */
    record State(float pitch, float bank, float heading, float throttle)
    {}

    /**
     * What the pilot wants of the aircraft this tick.
     *
     * @param heading
     *            the heading to yaw to; null leaves the rudder to hold the yaw steady
     * @param throttle
     *            where the throttle lever should stand, from -1 to 1
     * @param brake
     *            whether to hold the wheel brakes, rolling out on the ground
     */
    record Command(float pitch, float bank, @Nullable Float heading, float throttle, boolean brake)
    {
        Command(float pitch, float bank, @Nullable Float heading, float throttle)
        {
            this(pitch, bank, heading, throttle, false);
        }
    }

    /** @return the controls that bring the aircraft toward the command */
    Set<DriveControl> hold(State state, Command command)
    {
        float pitchRate = rate(state.pitch(), lastPitch);
        float bankRate = rate(state.bank(), lastBank);
        float yawRate = rate(state.heading(), lastHeading);
        lastPitch = state.pitch();
        lastBank = state.bank();
        lastHeading = state.heading();
        Set<DriveControl> controls = EnumSet.noneOf(DriveControl.class);
        pitch.press(controls, PITCH_GAIN * Mth.wrapDegrees(command.pitch() - state.pitch()) - PITCH_DAMPING * pitchRate);
        bank.press(controls, BANK_GAIN * Mth.wrapDegrees(command.bank() - state.bank()) - BANK_DAMPING * bankRate);
        float yawError = command.heading() == null ? 0F : Mth.wrapDegrees(command.heading() - state.heading());
        yaw.press(controls, YAW_GAIN * yawError - YAW_DAMPING * yawRate);
        if (state.throttle() < command.throttle() - THROTTLE_SLACK)
            controls.add(DriveControl.FORWARD);
        else if (state.throttle() > command.throttle() + THROTTLE_SLACK)
            controls.add(DriveControl.BACKWARD);
        if (command.brake())
            controls.add(DriveControl.BRAKE);
        return controls;
    }

    /** Forgets the last attitude and what each axis owed, for an aircraft the pilot just took the controls of. */
    void reset()
    {
        lastPitch = Float.NaN;
        lastBank = Float.NaN;
        lastHeading = Float.NaN;
        pitch.carry = 0F;
        bank.carry = 0F;
        yaw.carry = 0F;
    }

    private static float rate(float now, float last)
    {
        return Float.isNaN(last) ? 0F : Mth.wrapDegrees(now - last);
    }

    /** One axis: a command from -1 to 1 held as the share of ticks one of its two keys is down. */
    private static final class Pulse
    {
        private final DriveControl positive;
        private final DriveControl negative;
        /** The share of a key press owed: it adds up the commands and presses a key whenever half a press is owed. */
        private float carry;

        private Pulse(DriveControl positive, DriveControl negative)
        {
            this.positive = positive;
            this.negative = negative;
        }

        private void press(Set<DriveControl> controls, float command)
        {
            carry += Float.isFinite(command) ? Mth.clamp(command, -1F, 1F) : 0F;
            if (carry >= 0.5F)
            {
                carry -= 1F;
                controls.add(positive);
            }
            else if (carry <= -0.5F)
            {
                carry += 1F;
                controls.add(negative);
            }
        }
    }
}
