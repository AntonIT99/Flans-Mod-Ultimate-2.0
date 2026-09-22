package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.TexturedPolygon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Revision-checked local geometry; poses, lighting and tint deliberately are not cached. */
public final class RigidGeometry
{
    private final TexturedPolygon[] faces;
    private final long[] revisions;
    private final boolean supported;
    private final int vertexCount;
    private final RigidGeometry source;
    private final PoseStack.Pose localPose;
    private final PoseStack.Pose transformedPose;

    public RigidGeometry(TexturedPolygon[] polygons)
    {
        source = null;
        localPose = transformedPose = null;
        faces = polygons.clone();
        revisions = new long[faces.length];

        boolean rigid = faces.length > 0;
        int count = 0;

        for (int i = 0; i < faces.length; i++)
        {
            TexturedPolygon face = faces[i];

            if (face == null)
            {
                rigid = false;
                continue;
            }

            revisions[i] = face.geometryRevision();
            rigid &= face.getClass() == TexturedPolygon.class && face.isRigidGpuGeometry();

            int n = face.nVertices;
            if (n >= 3)
                count += ((n - 1) / 2) * 4;
        }

        supported = rigid && count > 0;
        vertexCount = count;
    }

    private RigidGeometry(RigidGeometry source, PoseStack.Pose transform)
    {
        this.source = source;
        faces = source.faces;
        revisions = source.revisions;
        supported = source.supported;
        vertexCount = source.vertexCount;
        localPose = new PoseStack().last();
        localPose.pose().set(transform.pose());
        localPose.normal().set(transform.normal());
        transformedPose = new PoseStack().last();
    }

    /** Immutable local-transform snapshot, evaluated only during VBO upload or CPU fallback. */
    public RigidGeometry transformed(PoseStack.Pose transform)
    {
        return new RigidGeometry(this, transform);
    }

    public boolean matches(TexturedPolygon[] polygons)
    {
        final TexturedPolygon[] localFaces = faces;
        final int length = localFaces.length;

        if (polygons.length != length)
            return false;

        for (int i = 0; i < length; i++)
        {
            final TexturedPolygon face = localFaces[i];

            if (face != polygons[i])
                return false;

            if (face != null && revisions[i] != face.geometryRevision())
                return false;
        }

        return true;
    }

    public boolean supported()
    {
        return supported;
    }

    public int vertexCount()
    {
        return vertexCount;
    }

    public void draw(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha)
    {
        if (source != null)
        {
            transformedPose.pose().set(pose.pose()).mul(localPose.pose());
            transformedPose.normal().set(pose.normal()).mul(localPose.normal());
            source.draw(transformedPose, consumer, light, overlay, red, green, blue, alpha);
            return;
        }
        for (TexturedPolygon face : faces)
            face.draw(pose, consumer, light, overlay, red, green, blue, alpha);
    }
}
