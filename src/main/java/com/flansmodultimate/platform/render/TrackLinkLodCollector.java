package com.flansmodultimate.platform.render;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;
import com.flansmodultimate.client.model.ModelBase;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Collects rendered track-link geometry through the version-specific vertex API. */
public final class TrackLinkLodCollector implements VertexConsumer
{
    private final float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
    private final float[] max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
    private final float[][] quad = new float[4][8];
    private final float[][][] textures = new float[6][][];
    private final float[] areas = new float[6];
    private int count;

    @Override
    public VertexConsumer addVertex(float x, float y, float z)
    {
        float[] vertex = quad[count++ % 4];
        vertex[0] = x; vertex[1] = y; vertex[2] = z;
        for (int axis = 0; axis < 3; axis++)
        {
            min[axis] = Math.min(min[axis], vertex[axis]);
            max[axis] = Math.max(max[axis], vertex[axis]);
        }
        return this;
    }

    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer setUv(float u, float v)
    {
        float[] vertex = quad[(count - 1) % 4];
        vertex[3] = u; vertex[4] = v;
        return this;
    }
    @Override public VertexConsumer setUv1(int u, int v) { return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; }
    @Override public VertexConsumer setNormal(float nx, float ny, float nz)
    {
        float[] vertex = quad[(count - 1) % 4];
        vertex[5] = nx; vertex[6] = ny; vertex[7] = nz;
        if (count % 4 == 0)
            acceptQuad();
        return this;
    }

    private void acceptQuad()
    {
        for (int axis = 0; axis < 3; axis++)
        {
            float normal = quad[0][5 + axis];
            if (Math.abs(normal) < 0.95F)
                continue;
            int face = axis * 2 + (normal > 0 ? 1 : 0);
            int first = (axis + 1) % 3, second = (axis + 2) % 3;
            float[] low = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
            float[] high = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (float[] vertex : quad)
            {
                low[0] = Math.min(low[0], vertex[first]); low[1] = Math.min(low[1], vertex[second]);
                high[0] = Math.max(high[0], vertex[first]); high[1] = Math.max(high[1], vertex[second]);
            }
            float area = (high[0] - low[0]) * (high[1] - low[1]);
            if (area <= areas[face])
                continue;
            float[][] uv = new float[4][2];
            for (int corner = 0; corner < 4; corner++)
            {
                float targetFirst = (corner & 1) == 0 ? low[0] : high[0];
                float targetSecond = (corner & 2) == 0 ? low[1] : high[1];
                float nearest = Float.POSITIVE_INFINITY;
                for (float[] vertex : quad)
                {
                    float error = Math.abs(vertex[first] - targetFirst) + Math.abs(vertex[second] - targetSecond);
                    if (error < nearest)
                    {
                        nearest = error; uv[corner][0] = vertex[3]; uv[corner][1] = vertex[4];
                    }
                }
            }
            textures[face] = uv;
            areas[face] = area;
        }
    }

    public float diameter()
    {
        float x = max[0] - min[0], y = max[1] - min[1], z = max[2] - min[2];
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    public float originRadius()
    {
        float x = Math.max(Math.abs(min[0]), Math.abs(max[0]));
        float y = Math.max(Math.abs(min[1]), Math.abs(max[1]));
        float z = Math.max(Math.abs(min[2]), Math.abs(max[2]));
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    public ModelRendererTurbo build(float spacing, int group)
    {
        if (count <= 24 || count > 4096 || count % 4 != 0 || !Float.isFinite(diameter()) || !(spacing > 0))
            return null;
        for (int i = 0; i < 6; i++)
            if (textures[i] == null || areas[i] <= 0F)
                return null;
        // Adjacent link pins can overlap longitudinally, but full boxes would
        // introduce coplanar overlapping faces. Keep the envelope within a
        // single step, centered on the authored geometry. Other axes stay intact.
        float halfLength = group == 1 ? Math.min((max[0] - min[0]) * 0.5F, spacing / 32F)
            : spacing * group / 32F;
        float center = (min[0] + max[0]) * 0.5F;
        min[0] = center - halfLength;
        max[0] = center + halfLength;
        ModelRendererTurbo result = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        for (int face = 0; face < 6; face++)
        {
            int axis = face / 2, first = (axis + 1) % 3, second = (axis + 2) % 3;
            // Cyclic axes: first cross second is the positive face normal.
            int[] order = face % 2 == 1 ? new int[]{0, 1, 3, 2} : new int[]{0, 2, 3, 1};
            PositionTextureVertex[] vertices = new PositionTextureVertex[4];
            for (int i = 0; i < 4; i++)
            {
                int corner = order[i];
                float[] xyz = new float[3];
                xyz[axis] = face % 2 == 1 ? max[axis] : min[axis];
                xyz[first] = (corner & 1) == 0 ? min[first] : max[first];
                xyz[second] = (corner & 2) == 0 ? min[second] : max[second];
                vertices[i] = new PositionTextureVertex(xyz[0] * 16, xyz[1] * 16, xyz[2] * 16,
                    textures[face][corner][0], textures[face][corner][1]);
            }
            result.copyTo(vertices, new TexturedPolygon[]{new TexturedPolygon(vertices)});
        }
        return result;
    }

    // TMT uses the packed vertex entry point; unexpected custom emission is unsupported.
    public VertexConsumer vertex(double x, double y, double z) { throw new IllegalStateException("Non-TMT vertex"); }
    public VertexConsumer color(int r, int g, int b, int a) { return this; }
    public VertexConsumer uv(float u, float v) { return this; }
    public VertexConsumer overlayCoords(int u, int v) { return this; }
    public VertexConsumer uv2(int u, int v) { return this; }
    public VertexConsumer normal(float x, float y, float z) { return this; }
    public void endVertex() {}
    public void defaultColor(int r, int g, int b, int a) {}
    public void unsetDefaultColor() {}
}
