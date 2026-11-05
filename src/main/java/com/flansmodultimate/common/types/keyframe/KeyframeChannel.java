package com.flansmodultimate.common.types.keyframe;

import java.util.Arrays;

/**
 * One animated property (position or rotation) of a keyframe bone: vectors at ascending times in seconds.
 * Sampling follows Tyrants and Plebeians: before the first and after the last keyframe the value eases
 * towards zero at the start and the end of the animation.
 */
public final class KeyframeChannel
{
    public static final KeyframeChannel EMPTY = new KeyframeChannel(new float[0], new float[0]);

    private final float[] times;
    private final float[] values;

    /** @param times ascending keyframe times; @param values three components per keyframe */
    KeyframeChannel(float[] times, float[] values)
    {
        if (values.length != times.length * 3)
            throw new IllegalArgumentException("Expected three values per keyframe");
        this.times = times.clone();
        this.values = values.clone();
    }

    public boolean isEmpty()
    {
        return times.length == 0;
    }

    public int size()
    {
        return times.length;
    }

    /**
     * Writes the interpolated vector at {@code time} into {@code out}.
     *
     * @param length animation length in seconds; the implicit zero keyframe after the last authored one sits there
     */
    public void sample(float time, float length, float[] out)
    {
        Arrays.fill(out, 0F);
        if (times.length == 0)
            return;

        float prevTime = 0F;
        int prev = -1;
        float nextTime = Math.max(length, times[times.length - 1]);
        int next = -1;
        for (int i = 0; i < times.length; i++)
        {
            if (times[i] <= time)
            {
                prevTime = times[i];
                prev = i;
            }
            if (times[i] >= time && next < 0)
            {
                nextTime = times[i];
                next = i;
            }
        }

        float factor = nextTime != prevTime ? (time - prevTime) / (nextTime - prevTime) : 0F;
        for (int axis = 0; axis < 3; axis++)
        {
            float from = prev < 0 ? 0F : values[prev * 3 + axis];
            float to = next < 0 ? 0F : values[next * 3 + axis];
            out[axis] = from + (to - from) * factor;
        }
    }
}
