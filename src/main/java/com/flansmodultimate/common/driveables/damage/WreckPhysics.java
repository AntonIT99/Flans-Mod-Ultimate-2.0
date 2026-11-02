package com.flansmodultimate.common.driveables.damage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/**
 * How a wreck behaves once nothing drives it any more, as plain numbers both sides can agree on and tests can pin.
 *
 * <p>
 * A wreck is no longer held up by its suspension and no longer steered, but it is still a few tons of metal: it
 * tumbles with whatever it was thrown by until the ground takes that out of it, lies along the slope it comes to rest
 * on, slides on a slope steep enough to overcome what is left of its running gear, and fills and goes down when it is
 * holed in water.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WreckPhysics
{
    /** The fastest a wreck tumbles, in degrees a tick, however hard it was hit. */
    public static final float MAX_SPIN = 16F;
    /** Below this a tumble has stopped and the wreck can be left alone. */
    public static final float SPIN_EPSILON = 0.08F;
    /** Degrees a tick a wreck turns towards the attitude of the ground it lies on. */
    public static final float SETTLE_DEGREES = 3F;
    /** The steepest slope a wreck is laid along, in degrees, so a hull never stands on its nose. */
    public static final float MAX_SETTLE_ANGLE = 35F;
    /** Share of a tumble that survives one tick in the air: next to nothing slows it. */
    private static final float AIR_SPIN_RETENTION = 0.985F;
    /** Share of a tumble that survives one tick of ground or water contact. */
    private static final float CONTACT_SPIN_RETENTION = 0.4F;
    /** Degrees a tick of tumble a wreck takes on for every block a tick it was travelling at. */
    private static final float TUMBLE_PER_SPEED = 9F;
    /** The gentlest slope, as a gradient, that gets a wreck moving at all. */
    private static final double SLIDE_THRESHOLD = 0.36D;
    /** Share of the downhill pull that reaches a sliding wreck, the rest being lost in ploughing and friction. */
    private static final double SLIDE_EFFICIENCY = 0.45D;
    /** Share of its speed a wreck keeps over a tick on its own running gear, which still rolls. */
    private static final double ROLLING_DRAG_RETENTION = 0.86D;
    /** Share of its speed a hull dragging its belly over the ground keeps over a tick. */
    private static final double GROUND_DRAG_RETENTION = 0.5D;
    /** Share of its speed a wreck keeps over a tick in water. */
    private static final double WATER_DRAG_RETENTION = 0.9D;
    /** The slowest a holed hull goes down, in blocks a tick: a landing craft still settles. */
    private static final double MIN_SINK_SPEED = 0.018D;
    /** The fastest a holed hull goes down, in blocks a tick. */
    private static final double MAX_SINK_SPEED = 0.09D;
    /** Degrees a tick a filling hull lists further over. */
    private static final float LIST_DEGREES = 0.08F;
    /** The furthest a filling hull lists before it is on its beam ends. */
    private static final float MAX_LIST = 28F;
    /** Degrees a tick a filling hull goes down by the bow or the stern. */
    private static final float TRIM_DEGREES = 0.02F;
    /** The furthest a filling hull trims by the bow or the stern. */
    private static final float MAX_TRIM = 12F;
    /** Fall speed in blocks a tick at which a landing is as bad as it gets. */
    private static final double WORST_FALL_SPEED = 1.6D;
    /** Below this a wreck is settling onto its springs rather than striking the ground. */
    public static final double MIN_IMPACT_FALL_SPEED = 0.25D;
    /** The furthest from level a tumbling wreck is pitched before it goes over the other way, in degrees. */
    public static final float MAX_SPIN_PITCH = 72F;
    /** What being caught under a wreck that is barely moving costs. */
    private static final float MIN_CRUSH_DAMAGE = 4F;
    /** What being caught under a wreck going straight in costs. */
    private static final float MAX_CRUSH_DAMAGE = 30F;
    /** Share of the crush a machine not heavy enough to squash what it drives over still does. */
    private static final float LIGHT_CRUSH_SHARE = 0.5F;

    /** @return the tumble, in degrees a tick, a wreck thrown at this speed takes on, for a signed random in [-1, 1] */
    public static float tumbleFrom(double speed, double spread)
    {
        if (!Double.isFinite(speed) || !Double.isFinite(spread))
            return 0F;
        return clampSpin((float) (speed * TUMBLE_PER_SPEED * Mth.clamp(spread, -1D, 1D)));
    }

    public static float clampSpin(float spin)
    {
        return Float.isFinite(spin) ? Mth.clamp(spin, -MAX_SPIN, MAX_SPIN) : 0F;
    }

    /**
     * @param contact
     *            whether the wreck is touching the ground or the water this tick, which takes the tumble out of it
     * @return what is left of the tumble next tick
     */
    public static float dampSpin(float spin, boolean contact)
    {
        if (!Float.isFinite(spin))
            return 0F;
        float left = spin * (contact ? CONTACT_SPIN_RETENTION : AIR_SPIN_RETENTION);
        return Math.abs(left) < SPIN_EPSILON ? 0F : left;
    }

    /**
     * How much of its speed a wreck keeps over one tick on the ground. A hull still on its wheels or tracks rolls on
     * for a while; one that has lost them, or never had any, ploughs to a stop.
     */
    public static double groundDragRetention(boolean onRunningGear)
    {
        return onRunningGear ? ROLLING_DRAG_RETENTION : GROUND_DRAG_RETENTION;
    }

    public static double waterDragRetention()
    {
        return WATER_DRAG_RETENTION;
    }

    /**
     * The pull a wreck feels down a slope, as blocks a tick squared along the downhill direction. A shallow slope
     * holds it: a burnt-out hull on a hillside stays where it died rather than creeping downhill for ever.
     *
     * @param gradient
     *            rise over run of the slope, positive downhill
     * @param gravity
     *            the gravity applied to the wreck this tick
     */
    public static double slideAcceleration(double gradient, double gravity)
    {
        if (!Double.isFinite(gradient) || !Double.isFinite(gravity) || gravity <= 0D)
            return 0D;
        double slope = Math.abs(gradient);
        if (slope <= SLIDE_THRESHOLD)
            return 0D;
        // Only what the slope has over the threshold does anything, so the pull comes on smoothly as it steepens.
        double excess = Math.min(slope, 4D) - SLIDE_THRESHOLD;
        return Math.copySign(gravity * SLIDE_EFFICIENCY * excess / (1D + slope * slope), gradient);
    }

    /**
     * @param buoyancy
     *            the type's buoyancy, where a hull built to float goes down more slowly as it fills
     * @return how fast the holed hull goes down, in blocks a tick
     */
    public static double sinkSpeed(float buoyancy, boolean floats)
    {
        float lift = Float.isFinite(buoyancy) ? Mth.clamp(buoyancy, 0F, 0.25F) : 0F;
        // A ship's compartments hold air; a tank that drives into a river has nothing to hold it up at all.
        double resistance = floats ? 0.35D + lift * 2.6D : lift * 1.2D;
        return Mth.clamp(MAX_SINK_SPEED * (1D - resistance), MIN_SINK_SPEED, MAX_SINK_SPEED);
    }

    /** @return how far over the filling hull has listed after this tick */
    public static float listAngle(float roll, boolean toStarboard)
    {
        if (!Float.isFinite(roll))
            return 0F;
        if (Math.abs(roll) >= MAX_LIST)
            return roll;
        return Mth.clamp(roll + (toStarboard ? LIST_DEGREES : -LIST_DEGREES), -MAX_LIST, MAX_LIST);
    }

    /** @return how far down by the bow or the stern the filling hull is after this tick */
    public static float trimAngle(float pitch, boolean byTheBow)
    {
        if (!Float.isFinite(pitch))
            return 0F;
        return Mth.clamp(pitch + (byTheBow ? TRIM_DEGREES : -TRIM_DEGREES), -MAX_TRIM, MAX_TRIM);
    }

    /** @return how bad a landing at this fall speed was, from 0 for settling onto the ground to 1 for going straight in */
    public static float impactSeverity(double fallSpeed)
    {
        if (!Double.isFinite(fallSpeed) || fallSpeed <= MIN_IMPACT_FALL_SPEED)
            return 0F;
        return (float) Mth.clamp((fallSpeed - MIN_IMPACT_FALL_SPEED) / (WORST_FALL_SPEED - MIN_IMPACT_FALL_SPEED), 0D, 1D);
    }

    /**
     * @param severity
     *            how bad the landing was, from {@link #impactSeverity(double)}
     * @param heavy
     *            whether the machine is heavy enough to squash what it drives over
     * @return what being caught under the wreck as it comes down costs
     */
    public static float crushDamage(float severity, boolean heavy)
    {
        if (!Float.isFinite(severity) || severity <= 0F)
            return 0F;
        float damage = Mth.lerp(Mth.clamp(severity, 0F, 1F), MIN_CRUSH_DAMAGE, MAX_CRUSH_DAMAGE);
        return heavy ? damage : damage * LIGHT_CRUSH_SHARE;
    }

    /**
     * Turns the heights sampled under two ends of a wreck into the gradient between them.
     *
     * @param span
     *            the distance between the two samples, in blocks
     */
    public static double gradient(double nearHeight, double farHeight, double span)
    {
        if (!Double.isFinite(nearHeight) || !Double.isFinite(farHeight) || !Double.isFinite(span) || span < 0.25D)
            return 0D;
        return (farHeight - nearHeight) / span;
    }
}
