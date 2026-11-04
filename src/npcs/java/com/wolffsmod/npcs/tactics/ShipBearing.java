package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.MountMotion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.function.DoubleFunction;
import java.util.function.Predicate;

/**
 * Where a ship steams to bring its weapons onto its target. Torpedo tubes and other weapons fixed to a ship's hull only
 * bear with the bow on the target, so a ship that has such a weapon to fire runs in at its target, and turns away again
 * once it is closer than it wants to be, rather than stopping under its guns. With none left to fire it circles its
 * target instead, so that the turrets along its side bear; a ship whose main weapon turns with a turret circles from the
 * first. Angles are in degrees, positive to the right as Minecraft's yaw grows, and distances are in blocks on the ground
 * plane. No world access.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShipBearing
{
    /** Blocks ahead of the ship its steering point is laid, so that it steams on rather than stopping at it. */
    public static final double LEAD = 24D;
    /** Degrees off the bearing to its target a ship turns away on: enough to open the range while its side comes round. */
    static final float TURN_AWAY_ANGLE = 135F;
    /** Degrees a circling ship turns in toward its target per block it is out beyond the distance it circles at, and the most. */
    static final float CIRCLE_GAIN = 2F;
    static final float MAX_CIRCLE_TURN = 60F;
    /** Share of the distance it holds that a ship which has turned away opens back out to before it runs in again. */
    static final double REOPEN_SHARE = 1.5D;
    /** Fewest blocks a ship holds from its target, so that a short firing range does not leave it with nowhere to run in from. */
    static final double MIN_HOLD = 12D;
    /** Degrees a blocked course is swung, in turn, to find water the hull can steam through; signed with the way the ship turns. */
    static final float[] CLEARANCE_SWINGS = {0F, 25F, -25F, 50F, -50F, 80F, -80F, 115F, -115F, 150F, -150F, 180F};
    /** Passes taken to settle where a round fixed to the hull meets a moving target. */
    static final int INTERCEPT_PASSES = 2;

    /** What a ship does with its target. */
    public enum Course
    {
        /** Steams at the target, its bow on it, to fire the weapons fixed to its hull. */
        RUN_IN,
        /** Turns away to open the range after a run, bringing its side round as it goes. */
        TURN_AWAY,
        /** Steams round the target at the distance it holds, its side to it, so that its turrets bear. */
        CIRCLE
    }

    /**
     * @param bowWeapon
     *            whether the ship has a weapon fixed to its hull that it can fire now
     * @param distance
     *            blocks to its target across the ground
     * @param hold
     *            blocks from its target the ship fights at
     * @param last
     *            what it did last tick, so that it does not flap between running in and turning away
     * @return what the ship does this tick
     */
    public static Course course(boolean bowWeapon, double distance, double hold, Course last)
    {
        if (!bowWeapon)
            return Course.CIRCLE;
        if (last == Course.TURN_AWAY)
            return distance >= hold(hold) * REOPEN_SHARE ? Course.RUN_IN : Course.TURN_AWAY;
        return distance > hold(hold) ? Course.RUN_IN : Course.TURN_AWAY;
    }

    /**
     * @param side
     *            which way round the ship turns and circles, 1 to its right and -1 to its left
     * @return the point the ship steams for, {@link #LEAD} blocks ahead of it along its course
     */
    public static Vec3 point(Course course, Vec3 position, Vec3 target, double hold, int side)
    {
        return position.add(Vec3.directionFromRotation(0F, heading(course, position, target, hold, side)).scale(LEAD));
    }

    /**
     * The same point, but on a heading the ship can actually steam: the course it wants is swung away in widening steps
     * until the water along it carries the hull, so that a ship fighting along a coast turns off it rather than running
     * onto it. A ship boxed in on every heading is left with the course it wanted, and whatever moves it deals with the
     * ground as it does anywhere else.
     *
     * @param navigable
     *            whether the ship can steam from where it is to the given point
     */
    public static Vec3 clearPoint(Course course, Vec3 position, Vec3 target, double hold, int side, Predicate<Vec3> navigable)
    {
        float heading = heading(course, position, target, hold, side);
        // Away from the shore first on the side the ship is already turning to, so that a clear course costs no swing at all.
        for (float swing : CLEARANCE_SWINGS)
        {
            Vec3 point = position.add(Vec3.directionFromRotation(0F, Mth.wrapDegrees(heading + swing * side)).scale(LEAD));
            if (navigable.test(point))
                return point;
        }
        return point(course, position, target, hold, side);
    }

    /**
     * Where a ship lays its bow so that a weapon fixed to its hull meets a moving target: the point the target runs on to
     * while the round runs out to it. A torpedo takes seconds to cross the water, so a bow laid on a steaming ship points
     * at its wake by the time the round arrives; one laid on this point does not. It only moves the bow, so the ship still
     * runs in on, turns away from and circles the target itself.
     *
     * @param speed
     *            blocks per tick the round makes; the target itself is returned for a round whose speed is not known, and
     *            for one that arrives at once
     * @param lead
     *            how far the target is expected to move over a number of ticks, as its tracked motion predicts it
     * @return the point to lay the bow on
     */
    public static Vec3 intercept(Vec3 position, Vec3 target, double speed, DoubleFunction<Vec3> lead)
    {
        if (!Double.isFinite(speed) || speed <= 0D)
            return target;
        Vec3 point = target;
        // Two passes: how long the round runs depends on where the target will have run to by the time it arrives.
        for (int pass = 0; pass < INTERCEPT_PASSES; pass++)
            point = target.add(lead.apply(horizontal(position, point) / speed));
        return point;
    }

    /** @return the heading the ship steams on this tick */
    static float heading(Course course, Vec3 position, Vec3 target, double hold, int side)
    {
        double x = target.x - position.x;
        double z = target.z - position.z;
        float bearing = MountMotion.yaw(new Vec3(x, 0D, z));
        int turn = side < 0 ? -1 : 1;
        return Mth.wrapDegrees(switch (course)
        {
            case RUN_IN -> bearing;
            case TURN_AWAY -> bearing + turn * TURN_AWAY_ANGLE;
            case CIRCLE -> bearing + turn * (90F - inward(Math.sqrt(x * x + z * z), hold));
        });
    }

    /** @return degrees a circling ship turns in toward its target to close the circle, negative out to widen it */
    static float inward(double distance, double hold)
    {
        return Mth.clamp((float) (distance - hold(hold)) * CIRCLE_GAIN, -MAX_CIRCLE_TURN, MAX_CIRCLE_TURN);
    }

    /** @return the distance the ship fights at, never so short that it has nowhere to run in from */
    static double hold(double wanted)
    {
        return Math.max(MIN_HOLD, wanted);
    }

    /** @return blocks between the two points across the ground */
    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }
}
