package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.DriveControl;
import com.flansmodultimate.common.driveables.physics.LegacyPlanePhysics;
import com.wolffsmod.npcs.drivers.FlightControl.Command;
import com.wolffsmod.npcs.drivers.FlightControl.State;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The flight controller against the response of an aircraft's controls as the plane physics model it: a key moves its
 * control surface, which settles over some ten ticks, the surface sets the rate the aircraft turns at, and the turn
 * gathers momentum a degree a tick at most.
 */
class FlightControlTest
{
    /** Ticks given to settle, and the error and swing past the wanted angle allowed. */
    private static final int TICKS = 240;
    private static final float SETTLED = 1.5F;
    private static final float OVERSHOOT = 4F;

    /** One axis of the aircraft: a surface, the momentum of its turn and the angle it has turned to. */
    private static final class Axis
    {
        private final float authority;
        private final float retention;
        private float surface;
        private float momentum;
        private float angle;

        private Axis(float authority, float retention)
        {
            this.authority = authority;
            this.retention = retention;
        }

        private void step(Set<DriveControl> controls, DriveControl positive, DriveControl negative)
        {
            float input = (controls.contains(positive) ? 1F : 0F) - (controls.contains(negative) ? 1F : 0F);
            surface = LegacyPlanePhysics.flap(surface, input);
            momentum = LegacyPlanePhysics.approachMomentum(momentum, surface * authority * 0.125F);
            angle += momentum;
            momentum *= retention;
        }
    }

    /** @return the largest swing past the wanted angle, after checking the axis settled on it */
    private static float settle(float wanted, Axis axis, Function<Float, State> state, Function<Float, Command> command, DriveControl positive, DriveControl negative)
    {
        FlightControl control = new FlightControl();
        float overshoot = 0F;
        for (int tick = 0; tick < TICKS; tick++)
        {
            axis.step(control.hold(state.apply(axis.angle), command.apply(wanted)), positive, negative);
            overshoot = Math.max(overshoot, Math.signum(wanted) * (axis.angle - wanted));
        }
        assertEquals(wanted, axis.angle, SETTLED, "settled");
        return overshoot;
    }

    @Test
    void thePitchSettlesOnWhatIsWantedWithoutSwingingPastWhateverTheElevatorAuthority()
    {
        for (float authority : new float[]{0.5F, 1F, 2F, 3F})
        {
            float overshoot = settle(10F, new Axis(authority, 0.99F), pitch -> new State(pitch, 0F, 0F, 1F), pitch -> new Command(pitch, 0F, 0F, 1F), DriveControl.ASCEND, DriveControl.DESCEND);
            assertTrue(overshoot < OVERSHOOT, "authority " + authority + " overshoots by " + overshoot);
        }
    }

    @Test
    void theBankSettlesOnWhatIsWantedWithoutSwingingPastWhateverTheAileronAuthority()
    {
        for (float authority : new float[]{0.5F, 1F, 2F, 3F})
        {
            float overshoot = settle(-30F, new Axis(authority, 0.99F), bank -> new State(0F, bank, 0F, 1F), bank -> new Command(0F, bank, 0F, 1F), DriveControl.ROLL_RIGHT, DriveControl.ROLL_LEFT);
            assertTrue(overshoot < OVERSHOOT, "authority " + authority + " overshoots by " + overshoot);
        }
    }

    @Test
    void aRotorcraftYawsToTheHeadingWantedWithoutSwingingPastIt()
    {
        for (float authority : new float[]{0.5F, 1F, 2F, 3F})
        {
            // A rotorcraft's turn loses its momentum faster than a plane's.
            float overshoot = settle(60F, new Axis(authority, 0.95F), heading -> new State(0F, 0F, heading, 0.5F), heading -> new Command(0F, 0F, heading, 0.5F), DriveControl.RIGHT,
                DriveControl.LEFT);
            assertTrue(overshoot < OVERSHOOT, "authority " + authority + " overshoots by " + overshoot);
        }
    }

    @Test
    void aSmallCommandHoldsItsKeyForThatShareOfTicks()
    {
        FlightControl control = new FlightControl();
        // An error that asks for a third of the stick, the aircraft not turning.
        float error = 1F / 3F / FlightControl.PITCH_GAIN;
        int pressed = 0;
        for (int tick = 0; tick < 300; tick++)
        {
            if (control.hold(new State(0F, 0F, 0F, 0F), new Command(error, 0F, 0F, 0F)).contains(DriveControl.ASCEND))
                pressed++;
        }
        assertEquals(100, pressed, 2);
    }

    @Test
    void theThrottleLeverMovesTowardWhatIsWantedAndRestsNearIt()
    {
        FlightControl control = new FlightControl();
        assertTrue(control.hold(new State(0F, 0F, 0F, 0.2F), new Command(0F, 0F, 0F, 1F)).contains(DriveControl.FORWARD));
        assertTrue(control.hold(new State(0F, 0F, 0F, 0.8F), new Command(0F, 0F, 0F, 0.5F)).contains(DriveControl.BACKWARD));
        Set<DriveControl> resting = control.hold(new State(0F, 0F, 0F, 0.51F), new Command(0F, 0F, 0F, 0.5F));
        assertFalse(resting.contains(DriveControl.FORWARD) || resting.contains(DriveControl.BACKWARD));
    }
}
