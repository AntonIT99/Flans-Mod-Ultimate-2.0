package com.flansmodultimate.client.render;

import com.flansmodultimate.client.render.gpu.RenderDiagnostics;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.flansmodultimate.platform.render.VertexWriterPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.util.Mth;

/**
 * Collects a model part's vertices in native memory and hands them to the bulk vertex writer of Sodium or Embeddium,
 * a few hundred at a time, instead of making one {@link VertexConsumer} call per vertex. Under Iris with Sodium, the
 * bulk write also converts the batch to the shader pack's extended format in one pass. Without such a renderer, or for
 * a consumer it cannot write to, vertices go to the consumer one at a time as before. Render thread only.
 */
public final class EntityVertexBatch
{
    /** Minecraft's entity vertex layout: position, colour, texture, overlay, light, normal and one padding byte. */
    static final int STRIDE = 36;
    /** Vertices per bulk write; a multiple of four, so a quad is never split between two writes. */
    private static final int CAPACITY = 512;
    @Nullable
    private static VertexConsumer target;
    private static int count;

    private EntityVertexBatch() {}

    /**
     * Starts batching the vertices given to {@link #vertex} for {@code consumer}. Returns false, leaving every
     * vertex to go straight to its consumer, when no renderer can bulk write to it or a batch is already open.
     * Pair a true result with {@link #end()}, and give the consumer no other vertices in between.
     */
    public static boolean begin(VertexConsumer consumer)
    {
        if (target != null || !VertexWriterPlatform.canWrite(consumer) || !RenderSystem.isOnRenderThread())
            return false;
        target = consumer;
        count = 0;
        return true;
    }

    /** One entity-format vertex, already transformed: batched for the open batch's consumer, otherwise written directly. */
    public static void vertex(VertexConsumer consumer, float x, float y, float z, float red, float green, float blue, float alpha,
                              float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ)
    {
        if (consumer != target)
        {
            VertexPlatform.vertex(consumer, x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
            return;
        }
        if (count == CAPACITY)
            flush();
        put(Scratch.BUFFER + (long)count * STRIDE, x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
        count++;
    }

    /** Writes the remaining vertices and closes the batch. */
    public static void end()
    {
        try
        {
            flush();
        }
        finally
        {
            target = null;
            count = 0;
        }
    }

    private static void flush()
    {
        // A part that failed while drawing a polygon leaves an incomplete quad, which must not be written.
        int vertices = count & ~3;
        count = 0;
        if (vertices == 0)
            return;
        Scratch.STACK.push();
        try
        {
            VertexWriterPlatform.push(target, Scratch.STACK, Scratch.BUFFER, vertices);
        }
        finally
        {
            Scratch.STACK.pop();
        }
        RenderDiagnostics.countBulkWrite(vertices);
    }

    /** Encodes one vertex exactly as Minecraft's buffer builder does, with a zero padding byte. */
    static void put(long pointer, float x, float y, float z, float red, float green, float blue, float alpha,
                    float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ)
    {
        MemoryUtil.memPutFloat(pointer, x);
        MemoryUtil.memPutFloat(pointer + 4, y);
        MemoryUtil.memPutFloat(pointer + 8, z);
        MemoryUtil.memPutInt(pointer + 12, colour(red) | colour(green) << 8 | colour(blue) << 16 | colour(alpha) << 24);
        MemoryUtil.memPutFloat(pointer + 16, u);
        MemoryUtil.memPutFloat(pointer + 20, v);
        // Both short pairs keep their low half first, like the builder's two separate shorts.
        MemoryUtil.memPutInt(pointer + 24, packedOverlay);
        MemoryUtil.memPutInt(pointer + 28, packedLight);
        MemoryUtil.memPutInt(pointer + 32, normal(normalX) | normal(normalY) << 8 | normal(normalZ) << 16);
    }

    private static int colour(float value)
    {
        return (int)(value * 255.0F) & 0xFF;
    }

    private static int normal(float value)
    {
        return (int)(Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF;
    }

    /** Native memory, allocated once the first batch opens, so it is never reserved without a bulk writer. */
    private static final class Scratch
    {
        private static final long BUFFER = MemoryUtil.nmemAlignedAlloc(64, (long)CAPACITY * STRIDE);
        /** Room for writers that copy a batch to convert it, such as outline and enchantment glint consumers. */
        private static final MemoryStack STACK = MemoryStack.create(8 * CAPACITY * STRIDE);
    }
}
