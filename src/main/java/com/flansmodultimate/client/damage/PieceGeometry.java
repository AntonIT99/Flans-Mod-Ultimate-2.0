package com.flansmodultimate.client.damage;

import com.flansmodultimate.platform.render.PositionCollector;
import org.joml.Vector3f;

import java.util.LinkedHashSet;
import java.util.Set;

/** Collects the actual drawn vertices once at detachment, including model and animation transforms. */
final class PieceGeometry extends PositionCollector
{
    private final Set<Vector3f> points = new LinkedHashSet<>();
    private final Vector3f faceCentre = new Vector3f();
    private int faceVertices;

    Vector3f[] relativeTo(Vector3f pivot)
    {
        return points.stream().map(point -> new Vector3f(point).sub(pivot)).toArray(Vector3f[]::new);
    }

    @Override
    public void vertexPosition(float x, float y, float z)
    {
        Vector3f vertex = new Vector3f(x, y, z);
        if (vertex.isFinite())
            points.add(vertex);
        // Entity models emit quads (including triangles padded to four vertices). Sample their interiors too:
        // a long track can overhang both edges of a narrow block while its face still rests on that block.
        faceCentre.add(x, y, z);
        if (++faceVertices == 4)
        {
            if (faceCentre.isFinite())
                points.add(new Vector3f(faceCentre).mul(0.25F));
            faceCentre.zero();
            faceVertices = 0;
        }
    }
}
