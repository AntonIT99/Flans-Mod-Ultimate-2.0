package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RigidBatchTest
{
    @Test
    void packedMetadataPreservesEveryPartAcrossFullAndPartialFallbackBatches()
    {
        Backend backend = new Backend();
        backend.available = false;
        RigidBatch batch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH);
        Recording expected = new Recording();
        RigidGeometry geometry = geometry();
        PoseStack pose = new PoseStack();
        batch.begin(backend);
        for (int i = 0; i < GpuModelCache.PARTS_PER_BATCH + 3; i++)
        {
            pose.translate(0.25, -0.5, 1);
            int light = 0xCAFE0123 + i, overlay = 0xBEEF4321 - i;
            float tint = i / 32F;
            geometry.draw(pose.last(), expected, light, overlay, tint, 1 - tint, 0.5F, 0.75F);
            batch.submit(geometry, pose.last(), light, overlay, tint, 1 - tint, 0.5F, 0.75F);
        }
        batch.end();
        assertEquals(expected.vertices.size(), backend.output.vertices.size());
        for (int i = 0; i < expected.vertices.size(); i++)
            assertArrayEquals(expected.vertices.get(i), backend.output.vertices.get(i), 1E-6F);
    }

    @Test
    void interruptedUploadCanStartAgainAfterReload()
    {
        BufferBuilder upload = new BufferBuilder(256);
        upload.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        GpuModelCache.discardUpload(upload);
        assertFalse(upload.building());
        upload.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        geometry().draw(new PoseStack().last(), upload, 17, 23, 1, 1, 1, 1);
        var rendered = upload.end();
        assertEquals(4, rendered.drawState().vertexCount());
        rendered.release();
        GpuModelCache.discardUpload(upload);
    }

    @Test
    void cacheProbeDistinguishesIdentityOrderLengthAndSurvivesReuse()
    {
        RigidGeometry a = geometry(), b = geometry();
        GeometryKey probe = new GeometryKey(3);
        probe.add(a); probe.add(b);
        MeshCache<FakeMesh> cache = new MeshCache<>(100, 4);
        FakeMesh mesh = new FakeMesh(10);
        cache.put(probe, mesh);
        assertSame(mesh, cache.get(probe));
        probe.clear(); probe.add(b); probe.add(a);
        assertNull(cache.get(probe));
        probe.clear(); probe.add(a);
        assertNull(cache.get(probe));
        probe.add(geometry());
        assertNull(cache.get(probe));
        probe.clear(); probe.add(a); probe.add(b);
        assertSame(mesh, cache.get(probe));
    }

    @Test
    void lruEvictionReplacementAndClearDisposeResourcesOnce()
    {
        MeshCache<FakeMesh> cache = new MeshCache<>(25, 2);
        GeometryKey a = key(), b = key(), c = key();
        FakeMesh first = new FakeMesh(10), second = new FakeMesh(10), third = new FakeMesh(10);
        cache.put(a, first); cache.put(b, second);
        assertSame(first, cache.get(a)); // b is now the eviction candidate.
        cache.put(c, third);
        assertEquals(1, second.closes);
        assertEquals(0, first.closes);
        FakeMesh replacement = new FakeMesh(20);
        cache.put(c, replacement); // replace c and evict a for the byte budget.
        assertEquals(1, third.closes);
        assertEquals(1, first.closes);
        cache.clear(); cache.clear();
        assertEquals(1, replacement.closes);
        assertNull(cache.get(c));
    }

    @Test
    void consecutiveAndPartialBatchesOnlyFlushAtActualTransitions()
    {
        Backend backend = new Backend();
        RigidBatch batch = new RigidBatch(2);
        batch.begin(backend);
        for (int i = 0; i < 5; i++) submit(batch, geometry(), new PoseStack());
        batch.vertex(1, 2, 3, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0);
        submit(batch, geometry(), new PoseStack());
        batch.end();
        assertEquals(List.of("barrier", "gpu:2", "gpu:2", "gpu:1", "cpu", "barrier", "gpu:1"), backend.events);
        assertEquals(0, batch.key.count);
        for (RigidGeometry geometry : batch.key.geometries) assertNull(geometry);
    }

    @Test
    void copiedPalettesPreservePoseNormalsLightAndTintOnFailure()
    {
        Backend backend = new Backend();
        backend.throwOnDraw = true;
        RigidBatch batch = new RigidBatch(3);
        batch.begin(backend);
        RigidGeometry geometry = geometry();
        PoseStack pose = new PoseStack();
        pose.translate(3, -2, 7);
        pose.mulPose(Axis.YP.rotation(0.37F));
        pose.scale(-2, 0.5F, 3);
        Recording expected = new Recording();
        int light = 0xCAFE0123, overlay = 0xBEEF4321;
        geometry.draw(pose.last(), expected, light, overlay, 0.1F, 0.2F, 0.3F, 0.4F);
        batch.submit(geometry, pose.last(), light, overlay, 0.1F, 0.2F, 0.3F, 0.4F);
        assertArrayEquals(pose.last().pose().get(new float[16]), java.util.Arrays.copyOf(batch.poses, 16));
        assertArrayEquals(pose.last().normal().get(new float[9]), java.util.Arrays.copyOf(batch.normals, 9));
        pose.setIdentity(); // Retaining the caller's mutable pose would corrupt the queued draw.
        batch.end();
        assertEquals(List.of("failed", "cpu", "cpu", "cpu", "cpu"), backend.events);
        assertEquals(expected.vertices.size(), backend.output.vertices.size());
        for (int i = 0; i < expected.vertices.size(); i++)
            assertArrayEquals(expected.vertices.get(i), backend.output.vertices.get(i), 1E-6F);
    }

    @Test
    void throttleFallbackAfterGpuIsFlushedOnCloseAndContextCanBeReused()
    {
        Backend backend = new Backend();
        RigidBatch batch = new RigidBatch(1);
        batch.begin(backend);
        submit(batch, geometry(), new PoseStack());
        backend.available = false;
        submit(batch, geometry(), new PoseStack());
        batch.end();
        assertEquals("flushCpu", backend.events.get(backend.events.size() - 1));
        backend.available = true;
        backend.events.clear();
        batch.begin(backend); submit(batch, geometry(), new PoseStack()); batch.end();
        assertEquals(List.of("barrier", "gpu:1"), backend.events);
    }

    @Test
    void suspendedParentAndNestedBatchHaveIndependentStorageAndOrder()
    {
        Backend backend = new Backend();
        RigidBatch parent = new RigidBatch(2), child = new RigidBatch(2);
        parent.begin(backend);
        submit(parent, geometry(), new PoseStack());
        parent.suspend();
        child.begin(backend);
        submit(child, geometry(), new PoseStack());
        child.end();
        submit(parent, geometry(), new PoseStack());
        parent.end();
        assertEquals(List.of("barrier", "gpu:1", "barrier", "gpu:1", "barrier", "gpu:1"), backend.events);
        assertNotSame(parent.poses, child.poses);
    }

    static RigidGeometry geometry()
    {
        return new RigidGeometry(new TexturedPolygon[]{new TexturedPolygon(new PositionTextureVertex[]{
            new PositionTextureVertex(0, 0, 0, 0, 0), new PositionTextureVertex(16, 0, 0, 1, 0),
            new PositionTextureVertex(0, 16, 0, 0, 1)})});
    }

    private static GeometryKey key() { GeometryKey key = new GeometryKey(1); key.add(geometry()); return key; }
    static void submit(RigidBatch batch, RigidGeometry geometry, PoseStack pose) { batch.submit(geometry, pose.last(), 17, 23, 1, 1, 1, 1); }

    static class FakeMesh implements MeshCache.Resource
    {
        final long bytes;
        int closes;
        FakeMesh(long bytes) { this.bytes = bytes; }
        @Override public long bytes() { return bytes; }
        @Override public void close() { closes++; }
    }

    private static class Backend implements RigidBatch.Backend
    {
        final List<String> events = new ArrayList<>();
        final Recording output = new Recording()
        {
            @Override public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz)
            {
                events.add("cpu");
                super.vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
            }
        };
        boolean available = true, throwOnDraw;
        @Override public boolean draw(RigidBatch batch, boolean flushPending)
        {
            if (throwOnDraw) throw new IllegalStateException("injected GPU failure");
            if (!available) return false;
            if (flushPending) events.add("barrier");
            events.add("gpu:" + batch.key.count);
            return true;
        }
        @Override public VertexConsumer fallback() { return output; }
        @Override public void flushFallback() { events.add("flushCpu"); }
        @Override public void failed(RuntimeException exception) { events.add("failed"); }
    }

    static class Recording implements VertexConsumer
    {
        final List<float[]> vertices = new ArrayList<>();
        @Override public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz)
        { vertices.add(new float[]{x,y,z,r,g,b,a,u,v,overlay,light,nx,ny,nz}); }
        @Override public VertexConsumer vertex(double x, double y, double z) { throw new AssertionError(); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { throw new AssertionError(); }
        @Override public VertexConsumer uv(float u, float v) { throw new AssertionError(); }
        @Override public VertexConsumer overlayCoords(int u, int v) { throw new AssertionError(); }
        @Override public VertexConsumer uv2(int u, int v) { throw new AssertionError(); }
        @Override public VertexConsumer normal(float x, float y, float z) { throw new AssertionError(); }
        @Override public void endVertex() { throw new AssertionError(); }
        @Override public void defaultColor(int r, int g, int b, int a) { throw new AssertionError(); }
        @Override public void unsetDefaultColor() { throw new AssertionError(); }
    }
}
