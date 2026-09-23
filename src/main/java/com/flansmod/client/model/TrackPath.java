package com.flansmod.client.model;

import com.flansmod.common.vector.Vector3f;

import net.minecraft.util.Mth;

import java.util.List;

/**
 * The closed loop a tank track's links travel along, built from the authored
 * {@code LeftLinkPoint} / {@code RightLinkPoint} vertices.
 *
 * <p>Distances are measured in model pixels on the XY plane only, exactly as
 * 1.7.10's {@code AnimTankTrack} measured them, so authored
 * {@code TrackLinkLength} values keep their meaning. Segment {@code i} runs
 * from point {@code i - 1} to point {@code i}, wrapping at zero.</p>
 */
public final class TrackPath
{
    public static final TrackPath EMPTY = new TrackPath(new float[0], new float[0], new float[0], new float[0], 0F);

    private final float[] x;
    private final float[] y;
    private final float[] z;
    private final float[] cumulative;
    private final float length;

    private TrackPath(float[] x, float[] y, float[] z, float[] cumulative, float length)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.cumulative = cumulative;
        this.length = length;
    }

    public static TrackPath create(List<Vector3f> points)
    {
        if (points == null || points.size() < 2)
            return EMPTY;
        int count = points.size();
        float[] x = new float[count];
        float[] y = new float[count];
        float[] z = new float[count];
        float[] cumulative = new float[count];
        float length = 0F;
        for (int i = 0; i < count; i++)
        {
            Vector3f point = points.get(i);
            x[i] = point == null ? 0F : point.x;
            y[i] = point == null ? 0F : point.y;
            z[i] = point == null ? 0F : point.z;
        }
        for (int i = 0; i < count; i++)
        {
            int previous = i == 0 ? count - 1 : i - 1;
            float dx = x[i] - x[previous];
            float dy = y[i] - y[previous];
            length += Mth.sqrt(dx * dx + dy * dy);
            cumulative[i] = length;
        }
        return length > 0F ? new TrackPath(x, y, z, cumulative, length) : EMPTY;
    }

    public float length()
    {
        return length;
    }

    public int size()
    {
        return x.length;
    }

    public boolean isEmpty()
    {
        return length <= 0F;
    }

    public float pointX(int index)
    {
        return x[index];
    }

    public float pointY(int index)
    {
        return y[index];
    }

    public float pointZ(int index)
    {
        return z[index];
    }

    /** Wraps a distance into {@code [0, length)}. */
    public float wrap(float distance)
    {
        return distance - Mth.floor(distance / length) * length;
    }

    /** Index of the segment's end point for a distance already wrapped into the loop. */
    public int segmentAt(float distance)
    {
        int low = 0;
        int high = cumulative.length - 1;
        while (low < high)
        {
            int middle = (low + high) >>> 1;
            if (cumulative[middle] < distance)
                low = middle + 1;
            else
                high = middle;
        }
        return low;
    }

    /** Index of the segment's start point, wrapping at zero. */
    public int previousOf(int segment)
    {
        return segment == 0 ? x.length - 1 : segment - 1;
    }

    /** How far along its own segment a wrapped distance falls, in {@code [0, 1]}. */
    public float progressAlongSegment(float distance, int segment)
    {
        float segmentStart = segment == 0 ? 0F : cumulative[segment - 1];
        float segmentLength = cumulative[segment] - segmentStart;
        return segmentLength <= 0F ? 0F : (distance - segmentStart) / segmentLength;
    }
}
