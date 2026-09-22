package com.flansmodultimate.client.render.gpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

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
    }

    final GeometryKey key;
    final float[] poses;
    final float[] normals;
    final float[] data;
    private final PoseStack.Pose fallbackPose = new PoseStack().last();
    private final PoseStack.Pose lastPose = new PoseStack().last();
    private final int capacity;
    private final int maxVertices;
    private int paletteCount;
    private long vertexCount;
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
        poses = new float[capacity * 16];
        normals = new float[capacity * 9];
        // Two parts share a mat4; round up so odd capacities still upload whole matrices.
        data = new float[((capacity + 1) / 2) * 16];
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
        // Split before exceeding the per-tick upload cap, so larger merged batches
        // cannot become permanently uncacheable. A single oversized part still falls back.
        if (key.count != 0 && vertexCount + geometry.vertexCount() > maxVertices) flush();
        if (paletteCount != 0 && lastLight == light && lastOverlay == overlay
            && lastRed == red && lastGreen == green && lastBlue == blue && lastAlpha == alpha
            && lastPose.pose().equals(pose.pose()) && lastPose.normal().equals(pose.normal()))
        {
            key.add(geometry, paletteCount - 1);
            vertexCount += geometry.vertexCount();
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
        lastPose.pose().set(pose.pose());
        lastPose.normal().set(pose.normal());
        lastLight = light;
        lastOverlay = overlay;
        lastRed = red;
        lastGreen = green;
        lastBlue = blue;
        lastAlpha = alpha;
        key.add(geometry, paletteCount++);
        vertexCount += geometry.vertexCount();
        if (key.count == key.geometries.length || paletteCount == capacity) flush();
    }

    void flush()
    {
        if (key.count == 0) return;
        try
        {
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
                for (int i = 0; i < key.count; i++)
                {
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
        }
        finally
        {
            key.clear();
            paletteCount = 0;
            vertexCount = 0;
        }
    }

    /** A nested renderer can change any buffer/state. Complete our work before it runs. */
    void suspend()
    {
        flush();
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
            key.clear();
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
