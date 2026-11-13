package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

/**
 * CPU time of drawing static vehicle parts without cached meshes, as while a shader pack is active: the plain
 * consumer path against the direct sink, into Minecraft's buffer builder and into a bulk vertex writer. 54 vehicles
 * of 15 model types, 1,100 boxes each, with size culling on. Excludes OpenGL and the renderer's own vertex
 * conversion. Set FLANS_RENDER_BENCHMARK=true.
 */
// A benchmark: it reports timings instead of asserting.
@SuppressWarnings("java:S2699")
@EnabledIfEnvironmentVariable(named = "FLANS_RENDER_BENCHMARK", matches = "true")
class DirectRigidSinkBenchmarkTest
{
    private static final int TYPES = 15;
    private static final int PARTS = 1100;
    private static final int VEHICLES = 54;
    private static final int FRAMES = 20;

    @BeforeAll
    static void renderThread()
    {
        if (!RenderSystem.isOnRenderThread())
            RenderSystem.initRenderThread();
    }

    @Test
    void plainConsumerAgainstDirectSink()
    {
        ModelVehicle model = new ModelVehicle();
        ByteBufferBuilder storage = new ByteBufferBuilder(64 << 20);
        BufferBuilder[] builder = new BufferBuilder[1];
        Runnable begin = () -> builder[0] = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        Runnable finish = () ->
        {
            MeshData mesh = builder[0].build();
            if (mesh != null)
                mesh.close();
        };
        BulkTarget bulk = new BulkTarget(64 << 20);
        DirectRigidSink sink = new DirectRigidSink();

        report("buffer builder, plain consumer", models(), (parts, stack) -> model.renderPart(parts, stack, builder[0], 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT), begin, finish);
        report("buffer builder, direct sink", models(), (parts, stack) ->
        {
            sink.begin(type -> builder[0], null);
            model.renderPart(parts, stack, sink, 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT);
            sink.end();
        }, begin, finish);
        report("bulk writer, plain consumer", models(), (parts, stack) -> model.renderPart(parts, stack, bulk, 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT), bulk::reset, bulk::reset);
        report("bulk writer, direct sink", models(), (parts, stack) ->
        {
            sink.begin(type -> bulk, null);
            model.renderPart(parts, stack, sink, 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT);
            sink.end();
        }, bulk::reset, bulk::reset);
        bulk.free();
        storage.close();
    }

    private interface VehicleRender
    {
        void render(ModelRendererTurbo[] parts, PoseStack stack);
    }

    private static ModelRendererTurbo[][] models()
    {
        ModelBase base = new ModelBase()
        {};
        ModelRendererTurbo[][] models = new ModelRendererTurbo[TYPES][];
        for (int t = 0; t < TYPES; t++)
        {
            models[t] = new ModelRendererTurbo[PARTS];
            for (int i = 0; i < PARTS; i++)
            {
                ModelRendererTurbo part = new ModelRendererTurbo(base, 0, 0, 256, 256);
                part.addBox(0, 0, 0, 2 + i % 7, 1 + i % 3, 3);
                part.setRotationPoint(i % 11 - 5, i % 5, i % 13 - 6);
                if (i % 4 == 0)
                    part.rotateAngleY = 0.3F;
                models[t][i] = part;
            }
        }
        return models;
    }

    private static void report(String name, ModelRendererTurbo[][] models, VehicleRender render, Runnable beginFrame, Runnable endFrame)
    {
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 10; round++)
        {
            long start = System.nanoTime();
            for (int frame = 0; frame < FRAMES; frame++)
            {
                beginFrame.run();
                for (int v = 0; v < VEHICLES; v++)
                {
                    PoseStack stack = new PoseStack();
                    stack.translate(v % 9 * 6 - 24, -2, -15 - v / 9 * 8);
                    stack.mulPose(Axis.YP.rotationDegrees(v * 23 + frame));
                    ModelRendererTurbo.beginScreenSpaceCulling(0.75F, 540F);
                    try
                    {
                        render.render(models[v % TYPES], stack);
                    }
                    finally
                    {
                        ModelRendererTurbo.endScreenSpaceCulling();
                    }
                }
                endFrame.run();
            }
            long elapsed = System.nanoTime() - start;
            if (round >= 4)
                best = Math.min(best, elapsed);
        }
        System.out.printf("%s: %.1f ns per part, %.2f ms per frame%n", name, best / ((double) FRAMES * VEHICLES * PARTS), best / 1E6 / FRAMES);
    }

    /** Copies bulk writes into native memory, as a renderer's buffer builder does for vertices in its own format. */
    private static final class BulkTarget implements VertexConsumer, VertexBufferWriter
    {
        private final long memory;
        private final long capacity;
        private long used;

        private BulkTarget(long capacity)
        {
            this.capacity = capacity;
            memory = MemoryUtil.nmemAlloc(capacity);
        }

        private void reset()
        {
            used = 0;
        }

        private void free()
        {
            MemoryUtil.nmemFree(memory);
        }

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormat format)
        {
            long bytes = count * 36L;
            if (used + bytes > capacity)
                throw new IllegalStateException("benchmark buffer too small");
            MemoryUtil.memCopy(ptr, memory + used, bytes);
            used += bytes;
        }

        /** Minecraft 1.21 has no endVertex: every element-wise vertex starts here. */
        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z)
        {
            throw new AssertionError("expected bulk writes only");
        }

        @Override
        public @NotNull VertexConsumer setColor(int r, int g, int b, int a)
        {
            throw new AssertionError();
        }

        @Override
        public @NotNull VertexConsumer setUv(float u, float v)
        {
            throw new AssertionError();
        }

        @Override
        public @NotNull VertexConsumer setUv1(int u, int v)
        {
            throw new AssertionError();
        }

        @Override
        public @NotNull VertexConsumer setUv2(int u, int v)
        {
            throw new AssertionError();
        }

        @Override
        public @NotNull VertexConsumer setNormal(float x, float y, float z)
        {
            throw new AssertionError();
        }
    }
}
