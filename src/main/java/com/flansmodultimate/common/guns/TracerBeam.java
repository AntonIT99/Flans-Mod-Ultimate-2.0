package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.TypeFile;
import net.minecraft.util.Mth;

import static com.flansmodultimate.util.TypeReaderUtils.readFloatValues;
import static com.flansmodultimate.util.TypeReaderUtils.readValue;

/**
 * A glowing 3D tracer beam drawn behind a round, from the Labjac Edition's {@code TracerBeam} keys.
 *
 * <p>
 * Every tick a flying round leaves a beam {@link #length} blocks long behind it, which fades over a few
 * ticks. Colours and alpha are fractions from 0 to 1, the length is in blocks and the width is the beam's
 * core half-width in blocks.
 * </p>
 */
public record TracerBeam(boolean enabled, float red, float green, float blue, float length, float width, float alpha)
{
    /** The Labjac Edition defaults: a thin green beam four blocks long. */
    public static final TracerBeam DEFAULT = new TracerBeam(false, 0.1F, 1F, 0.15F, 4F, 0.04F, 0.9F);

    /** Below this speed, in blocks per tick, a beam keeps its configured length. */
    static final double SPEED_SCALE_THRESHOLD = 15D;

    /**
     * Reads one beam whose keys all start with {@code prefix}, such as {@code TracerBeam} or
     * {@code AlternateTracerBeam}, starting from the given defaults.
     */
    public static TracerBeam read(String prefix, TracerBeam defaults, TypeFile file)
    {
        boolean enabled = readValue(prefix, defaults.enabled, file);
        float red = defaults.red;
        float green = defaults.green;
        float blue = defaults.blue;
        float[] colour = readFloatValues(prefix + "Color", file, 3).orElse(null);
        if (colour != null && colour.length >= 3)
        {
            red = unit(colour[0], red);
            green = unit(colour[1], green);
            blue = unit(colour[2], blue);
        }
        float length = nonNegative(readValue(prefix + "Length", defaults.length, file), defaults.length);
        float width = nonNegative(readValue(prefix + "Width", defaults.width, file), defaults.width);
        float alpha = unit(readValue(prefix + "Alpha", defaults.alpha, file), defaults.alpha);
        return new TracerBeam(enabled, red, green, blue, length, width, alpha);
    }

    /** Whether this beam would draw anything at all. */
    public boolean visible()
    {
        return enabled && alpha > 0F && width > 0F && length > 0F;
    }

    /**
     * How much longer the beam is drawn for a round moving at the given speed, so fast shells still leave
     * a continuous streak. Up to 15 blocks per tick the beam keeps its length; above that it grows with the
     * square root of the speed, at most threefold.
     */
    public static float speedScale(double speed)
    {
        if (!(speed > SPEED_SCALE_THRESHOLD))
            return 1F;
        return (float) Math.min(3D, Math.sqrt(speed / SPEED_SCALE_THRESHOLD));
    }

    private static float unit(float value, float fallback)
    {
        return Float.isFinite(value) ? Mth.clamp(value, 0F, 1F) : fallback;
    }

    private static float nonNegative(float value, float fallback)
    {
        return Float.isFinite(value) ? Math.max(0F, value) : fallback;
    }
}
