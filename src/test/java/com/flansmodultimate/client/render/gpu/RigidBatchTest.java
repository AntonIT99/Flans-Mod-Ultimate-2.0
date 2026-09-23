package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.PositionTextureVertex;
import com.flansmod.client.tmt.TexturedPolygon;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
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
    void sharedPaletteMappingIsPartOfMeshIdentity()
    {
        RigidGeometry a = geometry(), b = geometry();
        GeometryKey shared = new GeometryKey(2), separate = new GeometryKey(2);
        shared.add(a, 0); shared.add(b, 0);
        separate.add(a, 0); separate.add(b, 1);
        assertNotEquals(shared, separate);
        GeometryKey stored = shared.snapshot();
        shared.clear(); shared.add(a, 0); shared.add(b, 1);
        assertNotEquals(stored, shared);
        shared.clear(); shared.add(a, 0); shared.add(b, 0);
        assertEquals(stored, shared);
    }

    @Test
    void sharedTransformsUseOneDrawAndSplitAtGeometryAndUploadLimits()
    {
        Backend backend = new Backend();
        RigidBatch batch = new RigidBatch(2, 8, 24);
        batch.begin(backend);
        for (int i = 0; i < 8; i++) submit(batch, geometry(), new PoseStack());
        batch.end();
        assertEquals(List.of("barrier", "gpu:6", "gpu:2"), backend.events);
        backend.events.clear();
        batch = new RigidBatch(2, 8, 1000);
        batch.begin(backend);
        for (int i = 0; i < 9; i++) submit(batch, geometry(), new PoseStack());
        batch.end();
        assertEquals(List.of("barrier", "gpu:8", "gpu:1"), backend.events);
    }

    @Test
    void sharedPaletteFallbackPreservesMutableParentPoseAndDifferentLighting()
    {
        Backend backend = new Backend();
        backend.available = false;
        RigidBatch batch = new RigidBatch(3, 12, 1000);
        Recording expected = new Recording();
        PoseStack pose = new PoseStack();
        RigidGeometry geometry = geometry();
        batch.begin(backend);
        for (int i = 0; i < 10; i++)
        {
            if (i == 4) pose.translate(3, 4, 5);
            if (i == 7) pose.last().normal().scale(2); // Normal changes alone must split palettes too.
            int light = i < 6 ? 0xCAFE0123 : 0x1234BEEF;
            geometry.draw(pose.last(), expected, light, 23, 1, 0.5F, 0.25F, 1);
            batch.submit(geometry, pose.last(), light, 23, 1, 0.5F, 0.25F, 1);
        }
        pose.setIdentity();
        batch.end();
        assertEquals(expected.vertices.size(), backend.output.vertices.size());
        for (int i = 0; i < expected.vertices.size(); i++)
            assertArrayEquals(expected.vertices.get(i), backend.output.vertices.get(i), 1E-6F);
    }

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
        try (ByteBufferBuilder storage = new ByteBufferBuilder(256))
        {
            new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            storage.clear(); // Reload discards an unfinished builder's shared storage.
            BufferBuilder upload = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            geometry().draw(new PoseStack().last(), upload, 17, 23, 1, 1, 1, 1);
            try (var rendered = upload.buildOrThrow())
            {
                assertEquals(4, rendered.drawState().vertexCount());
            }
        }
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
        batch.addVertex(1, 2, 3).setColor(255, 255, 255, 255).setUv(0, 0)
            .setUv1(0, 0).setUv2(0, 0).setNormal(0, 1, 0);
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
            @Override public VertexConsumer setNormal(float x, float y, float z)
            {
                events.add("cpu");
                return super.setNormal(x, y, z);
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
        private final float[] pending = new float[14];
        @Override public VertexConsumer addVertex(float x, float y, float z)
        { pending[0] = x; pending[1] = y; pending[2] = z; return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a)
        { pending[3] = r / 255F; pending[4] = g / 255F; pending[5] = b / 255F; pending[6] = a / 255F; return this; }
        @Override public VertexConsumer setUv(float u, float v)
        { pending[7] = u; pending[8] = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v)
        { pending[9] = u | v << 16; return this; }
        @Override public VertexConsumer setUv2(int u, int v)
        { pending[10] = u | v << 16; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z)
        { pending[11] = x; pending[12] = y; pending[13] = z; vertices.add(pending.clone()); return this; }
    }
}
