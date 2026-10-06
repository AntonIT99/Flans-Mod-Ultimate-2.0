package com.wolffsmod.npcs.combat;

import net.minecraft.world.phys.Vec3;

/** Intercepts using Flan's discrete move-then-drag-and-gravity physics, without world access. */
public final class ProjectileAim
{
    private static final double MAX_FLIGHT_TICKS = 200D;

    private ProjectileAim()
    {}

    public static Vec3 direction(Vec3 displacement, Vec3 targetVelocity, double speed, double gravity, double drag, boolean highArc)
    {
        if (!Double.isFinite(speed) || speed <= 0D || !Double.isFinite(gravity) || !Double.isFinite(drag))
            return displacement.normalize();
        double retention = Math.max(0D, Math.min(1D, drag));
        double previousTime = 0.01D;
        double previousError = requiredVelocity(displacement, targetVelocity, previousTime, gravity, retention).length() - speed;
        Vec3 solution = null;
        for (double time = 0.5D; time <= MAX_FLIGHT_TICKS; time += 0.5D)
        {
            double error = requiredVelocity(displacement, targetVelocity, time, gravity, retention).length() - speed;
            if ((previousError > 0D && error <= 0D) || (previousError <= 0D && error > 0D))
            {
                double intercept = interceptTime(displacement, targetVelocity, new Ballistics(speed, gravity, retention), previousTime, time, previousError > 0D);
                solution = requiredVelocity(displacement, targetVelocity, intercept, gravity, retention).normalize();
                if (!highArc)
                    return solution;
            }
            previousTime = time;
            previousError = error;
        }
        return solution == null ? displacement.normalize() : solution;
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
