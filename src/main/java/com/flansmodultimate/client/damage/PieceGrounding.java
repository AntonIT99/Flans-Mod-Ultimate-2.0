package com.flansmodultimate.client.damage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.world.phys.Vec3;

/** Ground support and gravity-driven settling for cosmetic detached parts. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PieceGrounding
{
    private static final double CONTACT_TOLERANCE = 0.001D;
    private static final float SETTLE_ANGLE = 0.06F;
    private static final float MIN_SETTLE_ANGLE = SETTLE_ANGLE / 64F;
    private static final Vector3f[] PIVOT_ONLY = {new Vector3f()};

    @FunctionalInterface
    interface Surface
    {
        /** Highest collision surface below ceiling, at the point's horizontal coordinates. */
        double height(Vec3 point, double ceiling);
    }

    record Settlement(Quaternionf rotation, double height, boolean moved)
    {}

    /**
     * Required pivot height over the support points. The old pose bounds the vertical sweep, so fast-falling
     * pieces cannot skip the floor and a low corner cannot pull a piece up onto a block above it.
     */
    static double supportHeight(Vector3f[] points, Quaternionf rotation, Vec3 pivot, Quaternionf previousRotation, Vec3 previousPivot, Surface surface)
    {
        double height = Double.NEGATIVE_INFINITY;
        Vector3f rotated = new Vector3f();
        Vector3f previous = new Vector3f();
        for (Vector3f point : samples(points))
        {
            rotation.transform(point, rotated);
            previousRotation.transform(point, previous);
            Vec3 at = pivot.add(rotated.x, rotated.y, rotated.z);
            // Allow vertical rotation to settle against rising terrain, but horizontal spin cannot raise the search ceiling.
            double ceiling = Math.max(at.y, previousPivot.y + previous.y) + Math.abs(rotated.y - previous.y) + CONTACT_TOLERANCE;
            double ground = surface.height(at, ceiling);
            if (Double.isFinite(ground))
                height = Math.max(height, ground - rotated.y);
        }
        return height;
    }

    /**
     * Lowers the centre of mass while keeping the whole piece above its supports. Searching small rotations
     * lets a long track tip off a corner onto a face, including on uneven terrain, without snapping it flat.
     */
    static Settlement settle(Vector3f[] points, Quaternionf rotation, Vec3 pivot, Surface surface)
    {
        Vector3f centre = new Vector3f();
        for (Vector3f point : samples(points))
            centre.add(point);
        centre.div(samples(points).length);
        double energy = pivot.y + rotation.transform(new Vector3f(centre)).y;
        for (float angle = SETTLE_ANGLE; angle >= MIN_SETTLE_ANGLE; angle *= 0.5F)
        {
            Settlement best = new Settlement(rotation, pivot.y, false);
            for (int direction = 0; direction < 8; direction++)
            {
                double heading = direction * Math.PI / 4D;
                Quaternionf candidate = new Quaternionf().rotateAxis(angle, (float) Math.cos(heading), 0F, (float) Math.sin(heading)).mul(rotation);
                double height = supportHeight(points, candidate, pivot, rotation, pivot, surface);
                double candidateEnergy = height + candidate.transform(new Vector3f(centre)).y;
                if (Double.isFinite(height) && candidateEnergy < energy - 1.0E-6D)
                {
                    energy = candidateEnergy;
                    best = new Settlement(candidate, height, true);
                }
            }
            if (best.moved())
                return best;
        }
        return new Settlement(rotation, pivot.y, false);
    }

    private static Vector3f[] samples(Vector3f[] points)
    {
        return points.length == 0 ? PIVOT_ONLY : points;
    }
}
