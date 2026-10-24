package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.DriveControl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

import java.util.EnumSet;
import java.util.Set;

/**
 * The controls an NPC holds to take a Flan's driveable somewhere, as a player would at the keys. Angles are in degrees,
 * positive to the right as Minecraft's yaw grows; distances are in blocks and speeds in blocks per tick. No world access.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DriveSteering
{
    /** Degrees off its goal within which a driveable holds its course without steering. */
    static final float ON_COURSE = 4F;
    /** Degrees off beyond which a tracked vehicle turns on the spot before it drives on. */
    static final float PIVOT_ANGLE = 40F;
    /** Degrees off beyond which a wheeled vehicle stops accelerating through the turn. */
    static final float CORNER_ANGLE = 50F;
    /** Speed above which a wheeled vehicle brakes for a turn sharper than {@link #CORNER_ANGLE}. */
    static final double CORNER_SPEED = 0.35D;
    /** Degrees off beyond which a goal counts as behind. */
    static final float REVERSE_ANGLE = 125F;
    /** Blocks within which a goal behind is reached in reverse rather than by turning round. */
    static final double REVERSE_DISTANCE = 12D;
    /** Ticks of travel at its speed before the goal at which a vehicle stops accelerating; it brakes at half of them. */
    static final double BRAKE_LEAD = 20D;
    /** Speed under which a driveable counts as standing still. */
    static final double STOPPED = 0.02D;
    /** Share of a mecha's walk, as a cosine, beyond which it steps along an axis: up to 67.5 degrees off it. */
    static final double STEP_SHARE = 0.38D;

    /**
     * Where a vehicle stands with respect to its goal.
     *
     * @param off
     *            degrees the point it steers for lies off its heading
     * @param distance
     *            blocks to its goal
     * @param arrival
     *            blocks from its goal at which it has arrived
     * @param speed
     *            its speed along its heading, negative in reverse
     * @param tracked
     *            whether it turns on the spot
     * @param reversing
     *            whether it was reversing to its goal
     */
    record Situation(float off, double distance, double arrival, double speed, boolean tracked, boolean reversing)
    {}

    /** The controls a vehicle holds, and whether it is reversing to its goal. */
    record Decision(Set<DriveControl> controls, boolean reversing)
    {}

    /**
     * A driveable ahead of a vehicle.
     *
     * @param gap
     *            blocks between the two
     * @param closing
     *            how fast the vehicle closes in on it, negative while it draws away
     */
    record Traffic(double gap, double closing)
    {
        /** No driveable ahead. */
        static final Traffic NONE = new Traffic(Double.MAX_VALUE, 0D);
    }

    /** Drives a vehicle toward its goal: forward, steering for it, or in reverse to a goal close behind, and brakes there. */
    static Decision vehicle(Situation situation)
    {
        Set<DriveControl> controls = EnumSet.noneOf(DriveControl.class);
        double remaining = situation.distance() - situation.arrival();
        double speed = Math.abs(situation.speed());
        if (remaining <= 0D)
            return new Decision(brake(situation.speed()), false);
        // Once in reverse, it stays in reverse until the goal is clearly ahead again.
        boolean behind = Math.abs(situation.off()) > (situation.reversing() ? 90F : REVERSE_ANGLE);
        boolean reversing = behind && situation.distance() <= REVERSE_DISTANCE;
        // The end the vehicle leads with is the one that has to point at the goal.
        float course = reversing ? Mth.wrapDegrees(situation.off() + 180F) : situation.off();
        steer(controls, course, situation.speed(), situation.tracked());
        if (remaining < speed * BRAKE_LEAD * 0.5D)
            controls.add(DriveControl.BRAKE);
        else if (remaining < speed * BRAKE_LEAD)
            return new Decision(controls, reversing);
        else if (reversing)
            controls.add(DriveControl.BACKWARD);
        else if (throttles(Math.abs(course), situation.speed(), situation.tracked()))
            controls.add(DriveControl.FORWARD);
        else if (!situation.tracked() && situation.speed() > CORNER_SPEED)
            controls.add(DriveControl.BRAKE);
        return new Decision(controls, reversing);
    }

    /**
     * Backs a stuck vehicle away from what holds it, or drives it on when it got stuck in reverse, turning its front
     * toward the goal on the way: a three-point turn.
     *
     * @param backward
     *            whether to back away
     */
    static Set<DriveControl> recovery(float off, boolean backward, double speed, boolean tracked)
    {
        Set<DriveControl> controls = EnumSet.of(backward ? DriveControl.BACKWARD : DriveControl.FORWARD);
        steer(controls, off, speed, tracked);
        return controls;
    }

    /** Walks a mecha toward its goal along its heading, to its side or both; it does not have to turn to go anywhere. */
    static Set<DriveControl> mecha(float off, double distance, double arrival)
    {
        Set<DriveControl> controls = EnumSet.noneOf(DriveControl.class);
        if (distance <= arrival)
            return controls;
        double radians = Math.toRadians(off);
        double ahead = Math.cos(radians);
        double side = Math.sin(radians);
        if (ahead > STEP_SHARE)
            controls.add(DriveControl.FORWARD);
        else if (ahead < -STEP_SHARE)
            controls.add(DriveControl.BACKWARD);
        if (side > STEP_SHARE)
            controls.add(DriveControl.RIGHT);
        else if (side < -STEP_SHARE)
            controls.add(DriveControl.LEFT);
        return controls;
    }

    /**
     * Keeps a vehicle its distance to a driveable ahead: it stops accelerating while it closes in within that distance,
     * brakes for it within half of it, and waits there until it draws away.
     *
     * @param distance
     *            blocks to keep
     */
    static Set<DriveControl> keepDistance(Set<DriveControl> controls, Traffic ahead, int distance, double speed)
    {
        boolean close = ahead.gap() <= distance * 0.5D;
        if (!controls.contains(DriveControl.FORWARD) || ahead.gap() > distance || !close && ahead.closing() <= 0D)
            return controls;
        Set<DriveControl> kept = EnumSet.copyOf(controls);
        kept.remove(DriveControl.FORWARD);
        if (close)
            kept.addAll(brake(speed));
        return kept;
    }

    /** @return the brake, while the driveable still moves */
    static Set<DriveControl> brake(double speed)
    {
        return Math.abs(speed) > STOPPED ? EnumSet.of(DriveControl.BRAKE) : EnumSet.noneOf(DriveControl.class);
    }

    /**
     * Holds the key that turns the heading the wanted way. Steering turns a wheeled vehicle rolling backward the other
     * way round, as with a car; a tracked one turns the same way whichever way it moves.
     *
     * @param course
     *            degrees the heading has to turn, positive to the right
     */
    private static void steer(Set<DriveControl> controls, float course, double speed, boolean tracked)
    {
        if (Math.abs(course) < ON_COURSE)
            return;
        boolean mirrored = !tracked && speed < -STOPPED;
        controls.add(course > 0F != mirrored ? DriveControl.RIGHT : DriveControl.LEFT);
    }

    /** Whether a vehicle accelerates: a tracked one once it points near its goal, a wheeled one unless it is too fast for its turn. */
    private static boolean throttles(float course, double speed, boolean tracked)
    {
        if (tracked)
            return course < PIVOT_ANGLE;
        // A wheeled vehicle only turns while it rolls.
        return course < CORNER_ANGLE || speed < CORNER_SPEED;
    }
}
