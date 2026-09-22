package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** A textured, six-face envelope for a small rigid multipart track link. */
public final class TrackLinkLod
{
    private static final ThreadLocal<Integer> ACTIVE_GROUP = ThreadLocal.withInitial(() -> 0);
    private final ModelRendererTurbo[] source;
    private final List<TexturedPolygon> polygons = new ArrayList<>();
    private final long[] revisions;
    private final boolean[] rigid;
    private final float[] transforms;
    private final ModelRendererTurbo[] simplified;
    private final ModelRendererTurbo[] doubled;
    private final ModelRendererTurbo[] quadrupled;
    private final float diameter;
    private final float originRadius;
    private final boolean oldRotateOrder;
    private final float spacing;

    private TrackLinkLod(ModelRendererTurbo[] parts, boolean oldRotateOrder, float spacing)
    {
        this.oldRotateOrder = oldRotateOrder;
        this.spacing = spacing;
        source = parts.clone();
        transforms = new float[parts.length * 9];
        boolean supported = parts.length > 1 && parts.length <= 32;
        Collector collector = new Collector();
        for (int i = 0; i < parts.length; i++)
        {
            ModelRendererTurbo part = parts[i];
            if (!supported(part))
            {
                supported = false;
                continue;
            }
            storeTransform(part, transforms, i * 9);
            for (var group : part.getTextureGroups())
                for (TexturedPolygon polygon : group.poly)
                {
                    polygons.add(polygon);
                    supported &= polygon != null && polygon.isRigidLodGeometry();
                }
        }
        revisions = new long[polygons.size()];
        rigid = new boolean[polygons.size()];
        for (int i = 0; i < revisions.length; i++)
        {
            revisions[i] = polygons.get(i) == null ? -1 : polygons.get(i).geometryRevision();
            rigid[i] = polygons.get(i) != null && polygons.get(i).isRigidLodGeometry();
        }
        if (supported)
            for (ModelRendererTurbo part : parts)
                part.render(new PoseStack(), collector, 0, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT, oldRotateOrder);
        // Measure the original geometry for conservative LOD selection, before clipping.
        diameter = collector.diameter();
        originRadius = collector.originRadius();
        ModelRendererTurbo mesh = supported ? collector.build(spacing, 1) : null;
        simplified = mesh == null ? null : new ModelRendererTurbo[]{mesh};
        ModelRendererTurbo doubleMesh = mesh == null || !Float.isFinite(spacing) ? null : collector.build(spacing, 2);
        doubled = doubleMesh == null ? null : new ModelRendererTurbo[]{doubleMesh};
        ModelRendererTurbo quadrupleMesh = doubleMesh == null ? null : collector.build(spacing, 4);
        quadrupled = quadrupleMesh == null ? null : new ModelRendererTurbo[]{quadrupleMesh};
    }

    public static TrackLinkLod create(ModelRendererTurbo[] parts, boolean oldRotateOrder)
    {
        return create(parts, oldRotateOrder, Float.POSITIVE_INFINITY);
    }

    public static TrackLinkLod create(ModelRendererTurbo[] parts, boolean oldRotateOrder, float spacing)
    {
        return new TrackLinkLod(parts == null ? new ModelRendererTurbo[0] : parts, oldRotateOrder, spacing);
    }

    public boolean matches(ModelRendererTurbo[] parts, boolean oldRotateOrder, float spacing)
    {
        return this.spacing == spacing && matches(parts, oldRotateOrder);
    }

    public boolean matches(ModelRendererTurbo[] parts, boolean oldRotateOrder)
    {
        if (parts == null || parts.length != source.length || this.oldRotateOrder != oldRotateOrder)
            return false;
        int polygonIndex = 0;
        for (int i = 0; i < parts.length; i++)
        {
            ModelRendererTurbo part = parts[i];
            if (part != source[i])
                return false;
            if (!supported(part))
                return simplified == null;
            if (!sameTransform(part, transforms, i * 9))
                return false;
            for (var group : part.getTextureGroups())
                for (TexturedPolygon polygon : group.poly)
                {
                    if (polygonIndex >= polygons.size() || polygon != polygons.get(polygonIndex)
                        || polygon == null || polygon.geometryRevision() != revisions[polygonIndex]
                        || polygon.isRigidLodGeometry() != rigid[polygonIndex])
                        return false;
                    polygonIndex++;
                }
        }
        return polygonIndex == polygons.size();
    }

    private static boolean supported(ModelRendererTurbo part)
    {
        return part != null && part.getClass() == ModelRendererTurbo.class && part.isVisible()
            && part.childModels.isEmpty() && !part.glow && !part.glowAdditive && !part.glowNoDepthWrite;
    }

    public boolean select(float projectionPixels, double distance, float modelScale, float threshold, boolean previous)
    {
        return selectGroup(projectionPixels, distance, modelScale, threshold, 0F, previous ? 1 : 0) > 0;
    }

    /** Merge adjacent links only when their source geometry is a few screen pixels wide. */
    public int selectGroup(float projectionPixels, double distance, float modelScale, float threshold,
                           float groupingThreshold, int previousGroup)
    {
        if (simplified == null) return 0;
        float size = diameter * Math.abs(modelScale);
        double nearest = distance - originRadius * Math.abs(modelScale);
        if (!selectDiameter(size, projectionPixels, nearest, threshold, previousGroup > 0)) return 0;
        if (groupingThreshold > 0F && quadrupled != null && selectDiameter(size, projectionPixels, nearest,
            Math.min(threshold, groupingThreshold * 0.5F), previousGroup >= 4)) return 4;
        if (groupingThreshold > 0F && doubled != null && selectDiameter(size, projectionPixels, nearest,
            Math.min(threshold, groupingThreshold), previousGroup >= 2)) return 2;
        return 1;
    }

    static boolean selectDiameter(float diameter, float projectionPixels, double distance, float threshold, boolean previous)
    {
        if (distance < 32D || threshold <= 0F || projectionPixels <= 0F || diameter <= 0F
            || !Double.isFinite(distance) || !Float.isFinite(diameter) || !Float.isFinite(projectionPixels))
            return false;
        double pixels = diameter * projectionPixels / Math.max(0.01D, distance - diameter * 0.5D);
        return pixels <= threshold * (previous ? 1.25F : 1F);
    }

    @Nullable
    public ModelRendererTurbo[] parts()
    {
        return simplified;
    }

    @Nullable
    public ModelRendererTurbo[] parts(int group)
    {
        return group >= 4 ? quadrupled : group >= 2 ? doubled : simplified;
    }

    public static boolean active() { return ACTIVE_GROUP.get() > 0; }
    public static int activeGroup() { return ACTIVE_GROUP.get(); }
    public static void setActive(boolean active) { ACTIVE_GROUP.set(active ? 1 : 0); }
    public static void setGroup(int group) { ACTIVE_GROUP.set(group); }

    private static void storeTransform(ModelRendererTurbo p, float[] out, int i)
    {
        out[i] = p.offsetX; out[i + 1] = p.offsetY; out[i + 2] = p.offsetZ;
        out[i + 3] = p.rotationPointX; out[i + 4] = p.rotationPointY; out[i + 5] = p.rotationPointZ;
        out[i + 6] = p.rotateAngleX; out[i + 7] = p.rotateAngleY; out[i + 8] = p.rotateAngleZ;
    }

    private static boolean sameTransform(ModelRendererTurbo p, float[] v, int i)
    {
        return p.offsetX == v[i] && p.offsetY == v[i + 1] && p.offsetZ == v[i + 2]
            && p.rotationPointX == v[i + 3] && p.rotationPointY == v[i + 4] && p.rotationPointZ == v[i + 5]
            && p.rotateAngleX == v[i + 6] && p.rotateAngleY == v[i + 7] && p.rotateAngleZ == v[i + 8];
    }

    private static final class Collector implements VertexConsumer
    {
        private final float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        private final float[] max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        private final float[][] quad = new float[4][8];
        private final float[][][] textures = new float[6][][];
        private final float[] areas = new float[6];
        private int count;

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                           int overlay, int light, float nx, float ny, float nz)
        {
            float[] vertex = quad[count++ % 4];
            vertex[0] = x; vertex[1] = y; vertex[2] = z; vertex[3] = u; vertex[4] = v;
            vertex[5] = nx; vertex[6] = ny; vertex[7] = nz;
            for (int axis = 0; axis < 3; axis++)
            {
                min[axis] = Math.min(min[axis], vertex[axis]);
                max[axis] = Math.max(max[axis], vertex[axis]);
            }
            if (count % 4 == 0)
                acceptQuad();
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

        private float diameter()
        {
            float x = max[0] - min[0], y = max[1] - min[1], z = max[2] - min[2];
            return (float)Math.sqrt(x * x + y * y + z * z);
        }

        private float originRadius()
        {
            float x = Math.max(Math.abs(min[0]), Math.abs(max[0]));
            float y = Math.max(Math.abs(min[1]), Math.abs(max[1]));
            float z = Math.max(Math.abs(min[2]), Math.abs(max[2]));
            return (float)Math.sqrt(x * x + y * y + z * z);
        }

        private ModelRendererTurbo build(float spacing, int group)
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
}
