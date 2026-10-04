package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.client.render.EntityVertexBatch;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Reusable ordered submission buffer, independent of OpenGL so transitions can be tested. */
final class RigidBatch implements RigidGeometryConsumer
{
    interface Backend
    {
        /** Return false for upload throttling/failure; flush pending vanilla data only on a GPU draw. */
        boolean draw(RigidBatch batch, boolean flushPending);
        VertexConsumer fallback();
        void flushFallback();
        void failed(RuntimeException exception);
        /** GPU draws are over until the next draw: restore any render state kept open between them. */
        default void endDraws() {}
    }

    final GeometryKey key;
    final float[] poses;
    final float[] normals;
    final float[] data;
    /** The same entries in the shader's std140 layout, 36 floats each, so a draw uploads them in one copy. */
    final float[] palette;
    static final int PALETTE_FLOATS = 36;
    final VisibleRanges ranges;
    private final PoseStack.Pose fallbackPose = new PoseStack().last();
    private final int capacity;
    private final int maxVertices;
    private int paletteCount;
    /** Distinct palette entries in the pending batch. */
    int paletteCount() { return paletteCount; }
    private long vertexCount;
    /** Pose source shared by every part-composed pose. */
    private static final Object COMPOSED = new Object();
    private Object lastSource;
    private int lastLight, lastOverlay;
    private float lastRed, lastGreen, lastBlue, lastAlpha;
    private Backend backend;
    private VertexConsumer fallback;
    private boolean needsBarrier;
    private boolean drewGpu;

    RigidBatch(int capacity)
    {
        this(capacity, capacity, Integer.MAX_VALUE);
    }

    RigidBatch(int capacity, int maxGeometries, int maxVertices)
    {
        this.capacity = capacity;
        this.maxVertices = maxVertices;
        key = new GeometryKey(maxGeometries);
        ranges = new VisibleRanges(maxGeometries, true);
        poses = new float[capacity * 16];
        normals = new float[capacity * 9];
        // Two parts share a mat4; round up so odd capacities still upload whole matrices.
        data = new float[((capacity + 1) / 2) * 16];
        palette = new float[capacity * PALETTE_FLOATS];
    }

    void begin(Backend backend)
    {
        this.backend = backend;
        needsBarrier = true;
        drewGpu = false;
    }

    @Override
    public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                                 float red, float green, float blue, float alpha)
    {
        submit(geometry, pose, light, overlay, red, green, blue, alpha, true);
    }

    @Override
    public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                       float red, float green, float blue, float alpha, boolean visible)
    {
        submit(geometry, System.identityHashCode(geometry), geometry.vertexCount(), pose, pose, light, overlay, red, green, blue, alpha, visible);
    }

    @Override
    public void submitComposed(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                               float red, float green, float blue, float alpha, boolean visible)
    {
        submit(geometry, System.identityHashCode(geometry), geometry.vertexCount(), pose, COMPOSED, light, overlay, red, green, blue, alpha, visible);
    }

    @Override
    public void submitCached(RigidGeometry geometry, int identityHash, int vertexCount, PoseStack.Pose pose, int light,
                             int overlay, float red, float green, float blue, float alpha, boolean visible)
    {
        submit(geometry, identityHash, vertexCount, pose, pose, light, overlay, red, green, blue, alpha, visible);
    }

    /**
     * The palette entry, and with it the cached mesh layout, is shared only with the previous submission from
     * the same pose source. Equal values from different stack entries are a coincidence of the current angles
     * (a turret at exactly zero yaw equals its hull) and would otherwise change the mesh as those angles move.
     */
    private void submit(RigidGeometry geometry, int identityHash, int vertices, PoseStack.Pose pose, Object source,
                        int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
    {
        // Split before exceeding the per-tick upload cap, so larger merged batches
        // cannot become permanently uncacheable. A single oversized part still falls back.
        if (key.count != 0 && vertexCount + vertices > maxVertices) flush();
        if (RenderDiagnostics.enabled)
        {
            RenderDiagnostics.submittedParts++;
            if (!visible) RenderDiagnostics.culledParts++;
        }
        ranges.add(vertices, visible);
        if (paletteCount != 0 && lastSource == source && lastLight == light && lastOverlay == overlay
            && lastRed == red && lastGreen == green && lastBlue == blue && lastAlpha == alpha
            && matchesLastPalette(pose))
        {
            key.add(geometry, identityHash, paletteCount - 1);
            vertexCount += vertices;
            if (key.count == key.geometries.length) flush();
            return;
        }
        int offset = paletteCount * 16;
        pose.pose().get(poses, offset);
        pose.normal().get(normals, paletteCount * 9);
        offset = paletteCount * 8;
        data[offset] = red;
        data[offset + 1] = green;
        data[offset + 2] = blue;
        data[offset + 3] = alpha;
        data[offset + 4] = light & 0xFFFF;
        data[offset + 5] = light >>> 16;
        data[offset + 6] = overlay & 0xFFFF;
        data[offset + 7] = overlay >>> 16;
        // Pose columns, normal columns padded to four floats, tint, then light and overlay.
        int p = paletteCount * PALETTE_FLOATS;
        System.arraycopy(poses, paletteCount * 16, palette, p, 16);
        for (int column = 0, n = paletteCount * 9; column < 3; column++, n += 3)
        {
            int c = p + 16 + column * 4;
            palette[c] = normals[n];
            palette[c + 1] = normals[n + 1];
            palette[c + 2] = normals[n + 2];
            palette[c + 3] = 0F;
        }
        System.arraycopy(data, offset, palette, p + 28, 8);
        lastSource = source;
        lastLight = light;
        lastOverlay = overlay;
        lastRed = red;
        lastGreen = green;
        lastBlue = blue;
        lastAlpha = alpha;
        key.add(geometry, identityHash, paletteCount++);
        vertexCount += vertices;
        if (key.count == key.geometries.length || paletteCount == capacity) flush();
    }

    @Override
    public boolean submitToLastPalette(RigidGeometry geometry, int identityHash, int vertexCount,
                                       PoseStack.Pose source, boolean visible)
    {
        // Split points stay where submit() would put them, so the mesh layout is the same either way.
        if (paletteCount == 0 || lastSource != source || vertexCount + this.vertexCount > maxVertices)
            return false;
        if (RenderDiagnostics.enabled)
        {
            RenderDiagnostics.submittedParts++;
            if (!visible) RenderDiagnostics.culledParts++;
        }
        ranges.add(vertexCount, visible);
        key.add(geometry, identityHash, paletteCount - 1);
        this.vertexCount += vertexCount;
        if (key.count == key.geometries.length) flush();
        return true;
    }

    /**
     * Whether the previous palette entry holds exactly this pose. Compared in place against the uploaded
     * arrays, translation first since it differs most often, instead of keeping and copying a second pose.
     */
    private boolean matchesLastPalette(PoseStack.Pose pose)
    {
        Matrix4f m = pose.pose();
        float[] p = poses;
        int o = (paletteCount - 1) * 16;
        if (p[o + 12] != m.m30() || p[o + 13] != m.m31() || p[o + 14] != m.m32()
            || p[o] != m.m00() || p[o + 1] != m.m01() || p[o + 2] != m.m02() || p[o + 3] != m.m03()
            || p[o + 4] != m.m10() || p[o + 5] != m.m11() || p[o + 6] != m.m12() || p[o + 7] != m.m13()
            || p[o + 8] != m.m20() || p[o + 9] != m.m21() || p[o + 10] != m.m22() || p[o + 11] != m.m23()
            || p[o + 15] != m.m33())
            return false;
        Matrix3f n = pose.normal();
        float[] q = normals;
        int i = (paletteCount - 1) * 9;
        return q[i] == n.m00() && q[i + 1] == n.m01() && q[i + 2] == n.m02()
            && q[i + 3] == n.m10() && q[i + 4] == n.m11() && q[i + 5] == n.m12()
            && q[i + 6] == n.m20() && q[i + 7] == n.m21() && q[i + 8] == n.m22();
    }

    void flush()
    {
        if (key.count == 0) return;
        try
        {
            if (ranges.count == 0) return;
            boolean rendered;
            try { rendered = backend.draw(this, needsBarrier); }
            catch (RuntimeException exception)
            {
                backend.failed(exception);
                rendered = false;
            }
            if (rendered)
            {
                fallback = null;
                needsBarrier = false;
                drewGpu = true;
            }
            else
            {
                fallback = backend.fallback();
                needsBarrier = true;
                boolean batched = EntityVertexBatch.begin(fallback);
                try
                {
                    for (int i = 0; i < key.count; i++)
                    {
                        if (!ranges.visible[i]) continue;
                        if (RenderDiagnostics.enabled) RenderDiagnostics.fallbackVertices += key.geometries[i].vertexCount();
                        int palette = key.paletteIndices[i];
                        int offset = palette * 16;
                        fallbackPose.pose().set(poses, offset);
                        offset = palette * 8;
                        int normalOffset = palette * 9;
                        fallbackPose.normal().set(normals[normalOffset], normals[normalOffset + 1], normals[normalOffset + 2],
                            normals[normalOffset + 3], normals[normalOffset + 4], normals[normalOffset + 5],
                            normals[normalOffset + 6], normals[normalOffset + 7], normals[normalOffset + 8]);
                        int light = (int)data[offset + 4] | (int)data[offset + 5] << 16;
                        int overlay = (int)data[offset + 6] | (int)data[offset + 7] << 16;
                        key.geometries[i].draw(fallbackPose, fallback, light, overlay,
                            data[offset], data[offset + 1], data[offset + 2], data[offset + 3]);
                    }
                }
                finally
                {
                    if (batched)
                        EntityVertexBatch.end();
                }
            }
        }
        finally
        {
            key.clear();
            ranges.clear();
            paletteCount = 0;
            vertexCount = 0;
            lastSource = null;
        }
    }

    /** A nested renderer can change any buffer/state. Complete our work before it runs. */
    void suspend()
    {
        flush();
        backend.endDraws();
        if (fallback != null) backend.flushFallback();
        fallback = null;
        needsBarrier = true;
    }

    void end()
    {
        try
        {
            flush();
            if (drewGpu && fallback != null) backend.flushFallback();
        }
        finally
        {
            backend.endDraws();
            key.clear();
            ranges.clear();
            paletteCount = 0;
            vertexCount = 0;
            lastSource = null;
            fallback = null;
            backend = null;
        }
    }

    private VertexConsumer fallback()
    {
        flush();
        if (fallback == null) fallback = backend.fallback();
        needsBarrier = true;
        return fallback;
    }

    @Override
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz)
    {
        RenderDiagnostics.countCpuVertex();
        fallback().vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
    }

    @Override
    @NotNull
    public VertexConsumer vertex(double x, double y, double z)
    {
        fallback().vertex(x, y, z);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer color(int r, int g, int b, int a)
    {
        fallback().color(r, g, b, a);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer uv(float u, float v)
    {
        fallback().uv(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer overlayCoords(int u, int v)
    {
        fallback().overlayCoords(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer uv2(int u, int v)
    {
        fallback().uv2(u, v);
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer normal(float x, float y, float z)
    {
        fallback().normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex()
    {
        RenderDiagnostics.countCpuVertex();
        fallback().endVertex();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a)
    {
        fallback().defaultColor(r, g, b, a);
    }

    @Override public void unsetDefaultColor()
    {
        fallback().unsetDefaultColor();
    }
}
