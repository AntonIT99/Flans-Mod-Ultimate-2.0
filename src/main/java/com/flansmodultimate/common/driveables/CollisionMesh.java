package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;

import java.util.List;

/** Convex collision mesh retained as content data for advanced driveable ray tracing. */
public record CollisionMesh(Vector3f position, Vector3f size, List<Vector3f> vertices, EnumDriveablePart part)
{
    public CollisionMesh
    {
        position = new Vector3f(position.x, position.y, position.z);
        size = new Vector3f(size.x, size.y, size.z);
        vertices = vertices.stream().map(v -> new Vector3f(v.x, v.y, v.z)).toList();
        part = part == null ? EnumDriveablePart.CORE : part;
    }
}
