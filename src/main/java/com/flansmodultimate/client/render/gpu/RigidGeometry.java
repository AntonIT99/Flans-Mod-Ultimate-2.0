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

    public RigidGeometry(TexturedPolygon[] polygons)
    {
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
            rigid &= face.isRigidLodGeometry();
            int n = face.nVertices;
            count += n < 3 ? 0 : n == 3 ? 4 : ((n - 1) / 2) * 4;
        }
        supported = rigid && count > 0;
        vertexCount = count;
    }

    public boolean matches(TexturedPolygon[] polygons)
    {
        if (polygons.length != faces.length)
            return false;
        for (int i = 0; i < faces.length; i++)
            if (faces[i] != polygons[i] || faces[i] != null && revisions[i] != faces[i].geometryRevision())
                return false;
        return true;
    }

    public boolean supported() { return supported; }
    public int vertexCount() { return vertexCount; }

    public void draw(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                     float red, float green, float blue, float alpha)
    {
        for (TexturedPolygon face : faces)
            face.draw(pose, consumer, light, overlay, red, green, blue, alpha);
    }
}
