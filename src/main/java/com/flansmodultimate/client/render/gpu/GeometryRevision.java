package com.flansmodultimate.client.render.gpu;

/** Cheap invalidation hint, not an ownership graph. Geometry edits happen during load or on the render thread. */
public final class GeometryRevision
{
    private static long epoch;

    public static long current()
    {
        return epoch;
    }

    public static void changed()
    {
        epoch++;
    }

    private GeometryRevision() {}
}
