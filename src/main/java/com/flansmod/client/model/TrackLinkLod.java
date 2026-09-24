package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.TexturedPolygon;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.platform.render.TrackLinkLodCollector;
import com.mojang.blaze3d.vertex.PoseStack;
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
        TrackLinkLodCollector collector = new TrackLinkLodCollector();
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
}
