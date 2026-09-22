package com.flansmodultimate.client.render.gpu;

/** Cheap invalidation hint, not an ownership graph. Geometry edits happen during load or on the render thread. */
final class GeometryRevision
{
    private static long epoch;

    static long current()
    {
        return epoch;
    }

    static void changed()
    {
        epoch++;
    }

    private GeometryRevision() {}
}
