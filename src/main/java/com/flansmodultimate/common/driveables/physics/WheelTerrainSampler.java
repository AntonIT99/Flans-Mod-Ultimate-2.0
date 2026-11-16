package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.api.driveables.WheelSupport;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Probes the terrain under a set of wheels and accumulates what a suspension needs from it: the tilt that puts the
 * wheels at both ends on their support, how far the carried wheels sit from it, and how much of the footprint is carried
 * at all. Real driveables and entities wearing a driveable model both sample through this, so that the two cannot
 * drift apart.
 *
 * <p>
 * The tilt looks ahead along the movement, so that a vehicle meets a slope already pitched for it. What carries the
 * vehicle must not: a wheel heading over a ledge or into a wall is still carried by the ground it stands on. A wheel the
 * terrain ahead does not carry is therefore probed again where it stands.
 * </p>
 */
public final class WheelTerrainSampler
{
    /** Movement below which the look-ahead is rounding and the predicted probe is the standing one. */
    private static final double PREDICTION_EPSILON = 1.0E-8D;

    private final Terrain terrain;
    private final Reach reach;
    private final Vec3 prediction;
    private final boolean predicting;
    @Nullable
    private final Footprint footprint;
    private final Axis longitudinal = new Axis();
    private final Axis lateral = new Axis();
    private double errorSum;
    private double highestError = Double.NEGATIVE_INFINITY;
    private int grounded;

    /** The world, as far as a wheel probe looks at it. */
    @FunctionalInterface
    public interface Terrain
    {
        /** @return the collision a ray straight down meets first; null where it meets none, or the terrain is not loaded */
        @Nullable
        BlockHitResult clip(Vec3 from, Vec3 to);
    }

    /**
     * How far a wheel's suspension reaches, in blocks.
     *
     * @param rayAbove
     *            height above the wheel at which its probe starts
     * @param rayBelow
     *            depth below the wheel at which its probe ends
     * @param clearance
     *            height above its support at which the wheel's anchor rests
     * @param maxCompression
     *            highest the support may lie above the resting wheel; anything higher is a wall the wheel is inside
     * @param poseDroop
     *            deepest the support may lie below the resting wheel and still tilt the vehicle
     * @param groundedDroop
     *            deepest it may lie there and still carry the wheel
     */
    public record Reach(double rayAbove, double rayBelow, double clearance, double maxCompression, double poseDroop, double groundedDroop)
    {}

    /** What a probe found under a wheel. */
    private record Contact(BlockHitResult hit, double error)
    {}

    /**
     * @param prediction
     *            horizontal movement the tilt looks ahead by
     * @param trackFootprint
     *            whether {@link #support()} is asked for, which probes every wheel where it stands
     */
    public WheelTerrainSampler(Terrain terrain, Reach reach, Vec3 prediction, boolean trackFootprint)
    {
        this.terrain = terrain;
        this.reach = reach;
        this.prediction = prediction;
        predicting = prediction.lengthSqr() > PREDICTION_EPSILON;
        footprint = trackFootprint ? new Footprint() : null;
    }

    /**
     * Probes one wheel.
     *
     * @param standing
     *            where the wheel's anchor is in the world
     * @param forward
     *            its position along the vehicle, positive towards the nose
     * @param right
     *            its position across the vehicle, positive to the right
     * @param mount
     *            its authored anchor height
     * @return the terrain carrying the wheel; null when none does
     */
    @Nullable
    public BlockHitResult add(Vec3 standing, double forward, double right, double mount)
    {
        Contact ahead = probe(predicting ? standing.add(prediction) : standing);
        if (ahead != null)
        {
            double surface = ahead.hit().getLocation().y;
            longitudinal.add(forward, mount, surface);
            lateral.add(right, mount, surface);
        }
        Contact carrying = carries(ahead) ? ahead : null;
        boolean carriedHere = carrying != null;
        // Where the vehicle stands decides what carries it; where it is heading only decides how it is tilted.
        if (predicting && (footprint != null || carrying == null))
        {
            Contact here = probe(standing);
            carriedHere = carries(here);
            if (carrying == null && carriedHere)
                carrying = here;
        }
        if (footprint != null)
            footprint.add(standing, carriedHere);
        if (carrying == null)
            return null;
        errorSum += carrying.error();
        highestError = Math.max(highestError, carrying.error());
        grounded++;
        return carrying.hit();
    }

    private boolean carries(@Nullable Contact contact)
    {
        return contact != null && contact.error() >= -reach.groundedDroop();
    }

    @Nullable
    private Contact probe(Vec3 wheel)
    {
        BlockHitResult hit = terrain.clip(wheel.add(0D, reach.rayAbove(), 0D), wheel.add(0D, -reach.rayBelow(), 0D));
        if (hit == null)
            return null;
        double error = hit.getLocation().y + reach.clearance() - wheel.y;
        return error < -reach.poseDroop() || error > reach.maxCompression() ? null : new Contact(hit, error);
    }

    /** @return how many wheels the terrain carries */
    public int grounded()
    {
        return grounded;
    }

    /** @return how far the carried wheels sit below their support on average, in blocks; 0 without any */
    public double meanError()
    {
        return grounded == 0 ? 0D : errorSum / grounded;
    }

    /** @return how far the wheel reaching highest sits below its support, in blocks; negative infinity when nothing carries a wheel */
    public double highestError()
    {
        return highestError;
    }

    /**
     * @param invert
     *            whether the vehicle's pitch turns the other way round, as a plane's does
     * @param limit
     *            steepest angle the vehicle follows, in degrees
     * @return the pitch that puts the wheels at both ends on their support, nose up positive; level without support at both ends
     */
    public float pitchTarget(boolean invert, float limit)
    {
        return longitudinal.angle(invert, limit);
    }

    /** @return the roll that puts the wheels on both sides on their support, as {@link #pitchTarget} does for the ends */
    public float rollTarget(boolean invert, float limit)
    {
        return lateral.angle(invert, limit);
    }

    /** @return how much of the footprint is carried where the vehicle stands, and which way the rest hangs over */
    public WheelSupport support()
    {
        if (footprint == null)
            throw new IllegalStateException("This sampler does not track the footprint");
        return footprint.support();
    }

    /** Collects where the wheels stand and which of them the terrain carries, to say how the vehicle hangs over. */
    private static final class Footprint
    {
        private double carriedX;
        private double carriedZ;
        private double hangingX;
        private double hangingZ;
        private int carried;
        private int hanging;

        private void add(Vec3 wheel, boolean supported)
        {
            if (supported)
            {
                carriedX += wheel.x;
                carriedZ += wheel.z;
                carried++;
            }
            else
            {
                hangingX += wheel.x;
                hangingZ += wheel.z;
                hanging++;
            }
        }

        private WheelSupport support()
        {
            int total = carried + hanging;
            if (carried == 0 || hanging == 0)
                return new WheelSupport(carried, total, Vec3.ZERO);
            return new WheelSupport(carried, total, new Vec3(hangingX / hanging - carriedX / carried, 0D, hangingZ / hanging - carriedZ / carried));
        }
    }

    private static final class Axis
    {
        private final Group positive = new Group();
        private final Group negative = new Group();

        private void add(double position, double mount, double surface)
        {
            if (position > 1.0E-4D)
                positive.add(position, mount, surface);
            else if (position < -1.0E-4D)
                negative.add(position, mount, surface);
        }

        /** Without support at both ends, level rather than whatever angle the vehicle had. */
        private float angle(boolean invert, float limit)
        {
            if (positive.count == 0 || negative.count == 0)
                return 0F;
            return SuspensionPhysics.supportAngle(positive.surface / positive.count - negative.surface / negative.count, positive.position / positive.count - negative.position / negative.count,
                positive.mount / positive.count - negative.mount / negative.count, invert, limit);
        }
    }

    private static final class Group
    {
        private double position;
        private double mount;
        private double surface;
        private int count;

        private void add(double axisPosition, double mountHeight, double supportHeight)
        {
            position += axisPosition;
            mount += mountHeight;
            surface += supportHeight;
            count++;
        }
    }
}
