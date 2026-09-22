package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Development-only CPU/cache-hit harness, not an in-game FPS or GPU benchmark. */
@EnabledIfEnvironmentVariable(named = "FLANS_RENDER_BENCHMARK", matches = "true")
class RenderingAllocationTest
{
    @Test
    void warmModelSubmissionAndMeshLookupAllocateNoObjectsPerPart()
    {
        var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        ModelRendererTurbo[] parts = new ModelRendererTurbo[40];
        for (int i = 0; i < parts.length; i++)
        {
            parts[i] = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
            for (int box = 0; box < 64; box++) parts[i].addBox(box, 0, 0, 1, 1, 1);
            parts[i].rotateAngleY = i * 0.01F;
        }
        PoseStack pose = new PoseStack();
        Backend backend = new Backend();
        RigidBatch batch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH);
        run(parts, pose, batch, backend, 20_000);
        long thread = Thread.currentThread().getId();
        long allocated = bean.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        run(parts, pose, batch, backend, 20_000);
        long nanos = System.nanoTime() - start;
        allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        System.out.printf("Cached animated TMT submission: %.2f ns/part, %.5f bytes/part (%d bytes / 800000 parts), %d cache hits, %d misses, %d requested barriers%n",
            nanos / 800_000D, allocated / 800_000D, allocated, backend.hits, backend.misses, backend.barriers);
        assertEquals(2, backend.misses);
        // Allow small one-off VM bookkeeping, but reject even one small object per part.
        assertTrue(allocated < 16_384, "Hot path allocated " + allocated + " bytes");
        assertTrue(backend.checksum > 0);
    }

    private static void run(ModelRendererTurbo[] parts, PoseStack pose, RigidBatch batch, Backend backend, int frames)
    {
        for (int frame = 0; frame < frames; frame++)
        {
            batch.begin(backend);
            for (ModelRendererTurbo part : parts)
            {
                part.rotateAngleX = frame * 0.0001F;
                part.render(pose, batch, 17, 23, 1, 1, 1, 1, 1);
            }
            batch.end();
        }
    }

    private static class Backend implements RigidBatch.Backend
    {
        final MeshCache<RigidBatchTest.FakeMesh> cache = new MeshCache<>(1024, 16);
        long hits, misses, barriers;
        double checksum;
        @Override public boolean draw(RigidBatch batch, boolean flushPending)
        {
            var mesh = cache.get(batch.key);
            if (mesh == null)
            {
                misses++;
                cache.put(batch.key, new RigidBatchTest.FakeMesh(1));
            }
            else hits++;
            if (flushPending) barriers++;
            checksum += batch.poses[0] + batch.normals[0] + batch.data[0];
            return true;
        }
        @Override public VertexConsumer fallback() { throw new AssertionError("Unexpected fallback"); }
        @Override public void flushFallback() { throw new AssertionError("Unexpected fallback flush"); }
        @Override public void failed(RuntimeException exception) { throw new AssertionError(exception); }
    }
}
