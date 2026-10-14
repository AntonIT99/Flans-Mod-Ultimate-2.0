package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.sim.TargetMotion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

/** Intercepts using Flan's discrete move-then-drag-and-gravity physics, without world access. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectileAim
{
    private static final double MAX_FLIGHT_TICKS = 200D;

    /** Number of times the lead is refined against the flight time it produces. */
    private static final int LEAD_PASSES = 4;

    /**
     * A firing solution.
     *
     * @param direction
     *            unit launch direction
     * @param ticks
     *            flight time to the intercept; 0 when the target cannot be reached and the direction is the plain line to it
     */
    public record Solution(Vec3 direction, double ticks)
    {}

    public static Vec3 direction(Vec3 displacement, Vec3 targetVelocity, double speed, double gravity, double drag, boolean highArc)
    {
        return solve(displacement, targetVelocity, speed, gravity, drag, highArc).direction();
    }

    /**
     * Leads a target by its tracked motion, acceleration included. The lead depends on the flight time and the flight
     * time on the lead, so the two are refined against each other a few times.
     */
    public static Solution solve(Vec3 displacement, TargetMotion motion, double speed, double gravity, double drag, boolean highArc)
    {
        Solution solution = solve(displacement, motion.velocity(), speed, gravity, drag, highArc);
        boolean settled = solution.ticks() <= 0D;
        for (int pass = 0; pass < LEAD_PASSES && !settled; pass++)
        {
            Solution refined = solve(displacement.add(motion.lead(solution.ticks())), Vec3.ZERO, speed, gravity, drag, highArc);
            // A lead that puts the target out of reach is no better than the last one that did not.
            settled = refined.ticks() <= 0D || Math.abs(refined.ticks() - solution.ticks()) < 0.05D;
            if (refined.ticks() > 0D)
                solution = refined;
        }
        return solution;
    }

    public static Solution solve(Vec3 displacement, Vec3 targetVelocity, double speed, double gravity, double drag, boolean highArc)
    {
        if (!Double.isFinite(speed) || speed <= 0D || !Double.isFinite(gravity) || !Double.isFinite(drag))
            return new Solution(displacement.normalize(), 0D);
        double retention = Math.clamp(drag, 0D, 1D);
        double previousTime = 0.01D;
        double previousError = requiredVelocity(displacement, targetVelocity, previousTime, gravity, retention).length() - speed;
        Solution solution = null;
        for (double time = 0.5D; time <= MAX_FLIGHT_TICKS; time += 0.5D)
        {
            double error = requiredVelocity(displacement, targetVelocity, time, gravity, retention).length() - speed;
            if ((previousError > 0D && error <= 0D) || (previousError <= 0D && error > 0D))
            {
                double intercept = interceptTime(displacement, targetVelocity, new Ballistics(speed, gravity, retention), previousTime, time, previousError > 0D);
                solution = new Solution(requiredVelocity(displacement, targetVelocity, intercept, gravity, retention).normalize(), intercept);
                if (!highArc)
                    return solution;
            }
            previousTime = time;
            previousError = error;
        }
        return solution == null ? new Solution(displacement.normalize(), 0D) : solution;
    }

    private static double interceptTime(Vec3 displacement, Vec3 targetVelocity, Ballistics ballistics, double low, double high, boolean descending)
    {
        for (int i = 0; i < 24; i++)
        {
            double middle = (low + high) * 0.5D;
            double error = requiredVelocity(displacement, targetVelocity, middle, ballistics.gravity(), ballistics.drag()).length() - ballistics.speed();
            if ((error > 0D) == descending)
                low = middle;
            else
                high = middle;
        }
        return (low + high) * 0.5D;
    }

    private static Vec3 requiredVelocity(Vec3 displacement, Vec3 targetVelocity, double time, double gravity, double drag)
    {
        // Interpolate the exact integer-tick sums, keeping the no-drag limit numerically stable.
        int ticks = (int) time;
        double fraction = time - ticks;
        double sum = drag > 0.999999D ? ticks : (1D - Math.pow(drag, ticks)) / (1D - drag);
        double drop = drag > 0.999999D ? gravity * ticks * (ticks - 1D) * 0.5D : gravity * (ticks - sum) / (1D - drag);
        double lastRetention = Math.pow(drag, ticks);
        double nextDropVelocity = drag > 0.999999D ? gravity * ticks : gravity * (1D - lastRetention) / (1D - drag);
        sum += fraction * lastRetention;
        drop += fraction * nextDropVelocity;
        return displacement.add(targetVelocity.scale(time)).add(0D, drop, 0D).scale(1D / Math.max(1.0E-9D, sum));
    }

    private record Ballistics(double speed, double gravity, double drag)
    {}
}
