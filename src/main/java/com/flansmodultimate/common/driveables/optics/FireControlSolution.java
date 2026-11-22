package com.flansmodultimate.common.driveables.optics;

import com.flansmodultimate.api.equipment.WeaponBallistics;
import com.flansmodultimate.api.projectiles.BallisticDrag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Ballistic fire-control solution of the Labjac fork's {@code FCSMode AdjustingBarrel}: the extra elevation a round needs
 * to land on a lased point instead of falling short of it under gravity and drag. No world access.
 *
 * <p>
 * The round is flown tick by tick as {@link WeaponBallistics} describes, and the launch elevation that brings it through
 * the lased point is found by bisection. Elevations are in degrees above the horizon.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FireControlSolution
{
    /** Longest flight followed, in ticks: a minute. */
    private static final int MAX_TICKS = 1200;
    private static final int BISECTION_STEPS = 30;
    private static final float MAX_ELEVATION = 89F;

    /**
     * @param range
     *            lased distance in blocks along the line of sight
     * @param lineElevation
     *            elevation of the line of sight, in degrees
     * @param ballistics
     *            how the loaded round flies
     * @param maxCorrection
     *            largest correction, in degrees, either way
     * @return the elevation in degrees to add to the line of sight, clamped to {@code maxCorrection}, or 0 when there is no
     *         solution to compute
     */
    public static float elevationCorrection(double range, float lineElevation, WeaponBallistics ballistics, float maxCorrection)
    {
        if (ballistics == null || ballistics.dropped() || !(range > 0D) || !(ballistics.speed() > 0D) || !(maxCorrection > 0F) || !Float.isFinite(lineElevation))
            return 0F;
        double lineRadians = Math.toRadians(lineElevation);
        double horizontal = range * Math.cos(lineRadians);
        double vertical = range * Math.sin(lineRadians);
        if (horizontal < 1.0E-3D)
            return 0F;

        float low = lineElevation - maxCorrection;
        float high = Math.min(MAX_ELEVATION, lineElevation + maxCorrection);
        // Fired at the highest correction allowed, the round still passes below the point: give it all there is.
        if (!(heightAt(horizontal, high, ballistics) >= vertical))
            return high - lineElevation;
        if (heightAt(horizontal, low, ballistics) >= vertical)
            return low - lineElevation;
        for (int step = 0; step < BISECTION_STEPS; step++)
        {
            float middle = (low + high) * 0.5F;
            if (heightAt(horizontal, middle, ballistics) >= vertical)
                high = middle;
            else
                low = middle;
        }
        return high - lineElevation;
    }

    /**
     * @return the height, relative to the muzzle, at which a round launched at an elevation crosses a horizontal distance,
     *         or NaN when it never gets that far
     */
    static double heightAt(double horizontal, float elevation, WeaponBallistics ballistics)
    {
        double radians = Math.toRadians(elevation);
        double vx = ballistics.speed() * Math.cos(radians);
        double vy = ballistics.speed() * Math.sin(radians);
        double x = 0D;
        double y = 0D;
        for (int tick = 0; tick < MAX_TICKS; tick++)
        {
            double nextX = x + vx;
            double nextY = y + vy;
            if (nextX >= horizontal)
            {
                double share = vx > 0D ? (horizontal - x) / vx : 0D;
                return y + vy * share;
            }
            x = nextX;
            y = nextY;
            double retention = ballistics.drag() * BallisticDrag.retention(ballistics.quadraticDrag(), Math.sqrt(vx * vx + vy * vy));
            vx *= retention;
            vy = vy * retention - ballistics.gravity();
            if (vx < 1.0E-6D)
                return Double.NaN;
        }
        return Double.NaN;
    }
}
