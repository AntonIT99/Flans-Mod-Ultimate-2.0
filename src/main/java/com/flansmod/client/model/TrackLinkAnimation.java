package com.flansmod.client.model;

import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.types.VehicleType;

import net.minecraft.util.Mth;

/**
 * Per-vehicle rotation state for the individual links of a fancy tank track,
 * reproducing 1.7.10's {@code EntityVehicle.animateFancyTracks}.
 *
 * <p>Link positions themselves are stateless: every link advances at the same
 * speed, so their spacing never changes and the shared scroll offset on
 * {@link Vehicle} places them. Only the link <em>angle</em> carries history. Each
 * tick a link eases halfway towards the direction of travel of the path segment
 * it currently sits on, which smooths the corner-to-corner transitions but spins
 * the wrong way through the seam where {@code atan2} flips between +&pi; and
 * -&pi;. That is what {@code FixTrackLink} / {@code TrackLinkFix} exists for: it
 * names the segment index on which links snap straight to the true angle instead
 * of easing, so the seam is crossed in a single tick. The snap segment shifts by
 * one when the track runs backwards, matching the legacy sign handling.</p>
 */
public final class TrackLinkAnimation
{
    /** Fraction of the remaining angle a link closes each tick away from the fix segment. */
    private static final float EASE = 0.5F;
    /** Legacy per-tick link travel, in model pixels per unit of throttle. */
    private static final float THROTTLE_SPEED = 1.5F;
    /** Legacy per-tick link travel contributed by steering, in model pixels per degree. */
    private static final float STEERING_SPEED = 1F / 12F;
    /** Legacy links started a hundredth of a pixel along the loop rather than exactly on a vertex. */
    private static final float START_OFFSET = 0.01F;
    private static final int MAX_LINKS = 512;

    private VehicleType configuredType;
    private TrackPath leftPath = TrackPath.EMPTY;
    private TrackPath rightPath = TrackPath.EMPTY;
    private float[] leftAngles = new float[0];
    private float[] rightAngles = new float[0];
    private boolean seeded;

    /** True once there is per-link angle state worth rendering with. */
    public boolean isActive()
    {
        return seeded && leftAngles.length > 0;
    }

    public int linkCount()
    {
        return leftAngles.length;
    }

    public float[] angles(boolean left)
    {
        return left ? leftAngles : rightAngles;
    }

    /**
     * Advances one animation tick. {@code elapsed} covers ticks skipped while the
     * vehicle was not rendered, so a track that comes back into view catches up
     * instead of easing from a stale angle.
     */
    public void advance(Vehicle vehicle, VehicleType type, int elapsed)
    {
        if (!configure(type))
            return;

        // Legacy link travel is measured in model pixels per tick, while the
        // shared scroll offset is normalised, so convert through the loop length.
        float throttle = vehicle.getThrottle();
        float steering = vehicle.getWheelYaw();
        float leftSpeed = throttle * THROTTLE_SPEED - steering * STEERING_SPEED;
        float rightSpeed = throttle * THROTTLE_SPEED + steering * STEERING_SPEED;
        int ticks = Mth.clamp(elapsed, 1, 5);

        for (int tick = 0; tick < ticks; tick++)
        {
            step(leftPath, leftAngles, vehicle.getLeftTrackProgress(), leftSpeed, type);
            step(rightPath, rightAngles, vehicle.getRightTrackProgress(), rightSpeed, type);
        }
        seeded = true;
    }

    private void step(TrackPath path, float[] angles, float scroll, float speed, VehicleType type)
    {
        if (path.isEmpty() || angles.length == 0)
            return;

        // The legacy sign handling: running forwards the links snap one segment
        // later than the authored index, running backwards they snap on it.
        float snapSegment = type.getTrackLinkFix() + (speed < 0F ? 0F : 1F);
        float spacing = type.getTrackLinkLength();
        float scrolled = scroll * path.length();

        for (int link = 0; link < angles.length; link++)
        {
            float distance = path.wrap(scrolled + START_OFFSET + spacing * link);
            int segment = path.segmentAt(distance);
            int previous = path.previousOf(segment);
            float progress = path.progressAlongSegment(distance, segment);
            float x = Mth.lerp(progress, path.pointX(previous), path.pointX(segment));
            float y = Mth.lerp(progress, path.pointY(previous), path.pointY(segment));
            // 1.7.10 aimed each link at the point it is travelling towards, which
            // is the reverse of the static pose used for inventory renders.
            float target = (float) Math.atan2(path.pointY(segment) - y, path.pointX(segment) - x);
            float ease = segment != snapSegment ? EASE : 1F;
            angles[link] = seeded ? Mth.lerp(ease, angles[link], target) : target;
        }
    }

    /** Rebuilds the paths and link arrays when the vehicle type changes. Returns false if the type has no fancy track. */
    private boolean configure(VehicleType type)
    {
        if (configuredType == type)
            return leftAngles.length > 0;

        configuredType = type;
        seeded = false;
        leftPath = TrackPath.create(type.getLeftTrackPoints());
        rightPath = TrackPath.create(type.getRightTrackPoints());
        float spacing = type.getTrackLinkLength();
        int count = spacing <= 0F || rightPath.isEmpty() || leftPath.isEmpty()
            ? 0 : Mth.clamp(Math.round(rightPath.length() / spacing), 1, MAX_LINKS);
        leftAngles = new float[count];
        rightAngles = new float[count];
        return count > 0;
    }
}
