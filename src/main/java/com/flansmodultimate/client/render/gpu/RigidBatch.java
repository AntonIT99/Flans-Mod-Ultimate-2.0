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
    private Backend backend;
    private VertexConsumer fallback;
    private boolean needsBarrier;
    private boolean drewGpu;

    RigidBatch(int capacity)
    {
        key = new GeometryKey(capacity);
        poses = new float[capacity * 16];
        normals = new float[capacity * 9];
        data = new float[capacity * 16];
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
        int offset = key.count * 16;
        pose.pose().get(poses, offset);
        pose.normal().get(normals, key.count * 9);
        data[offset] = red;
        data[offset + 1] = green;
        data[offset + 2] = blue;
        data[offset + 3] = alpha;
        data[offset + 4] = light & 0xFFFF;
        data[offset + 5] = light >>> 16;
        data[offset + 6] = overlay & 0xFFFF;
        data[offset + 7] = overlay >>> 16;
        key.add(geometry);
        if (key.count == key.geometries.length) flush();
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
                    int offset = i * 16;
                    fallbackPose.pose().set(poses, offset);
                    int normalOffset = i * 9;
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
        finally { key.clear(); }
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
