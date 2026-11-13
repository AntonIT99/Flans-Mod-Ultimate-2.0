package com.flansmodultimate.client.render;

import com.flansmodultimate.client.render.gpu.RenderDiagnostics;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.flansmodultimate.platform.render.VertexWriterPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
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
    /** Floats of one flattened quad: its normal, then four vertices of x, y, z, u and v. */
    public static final int QUAD_FLOATS = 23;
    /** Vertices per bulk write; a multiple of four, so a quad is never split between two writes. */
    private static final int CAPACITY = 512;
    @Nullable
    private static VertexConsumer target;
    private static int count;

    private EntityVertexBatch()
    {}

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
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    public static void vertex(VertexConsumer consumer, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay, int packedLight, float normalX,
        float normalY, float normalZ)
    {
        if (consumer != target)
        {
            VertexPlatform.vertex(consumer, x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
            return;
        }
        if (count == CAPACITY)
            flush();
        put(Scratch.BUFFER + (long) count * STRIDE, x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
        count++;
    }

    /**
     * Flattened quads, {@link #QUAD_FLOATS} floats each: the face normal, then four vertices of position and texture
     * coordinates, all in local space. Transforms them by the pose and writes them exactly as {@link #vertex} would one
     * vertex at a time, but packs the colour once per call and the normal once per quad, and writes an open batch's
     * vertices straight into its memory.
     */
    // One argument per vertex attribute, as VertexConsumer#vertex, plus the pose.
    @SuppressWarnings("java:S107")
    public static void quads(VertexConsumer consumer, float[] quads, Matrix4f pose, Matrix3f normalMatrix, float red, float green, float blue, float alpha, int packedOverlay, int packedLight)
    {
        final float m00 = pose.m00();
        final float m01 = pose.m01();
        final float m02 = pose.m02();
        final float m10 = pose.m10();
        final float m11 = pose.m11();
        final float m12 = pose.m12();
        final float m20 = pose.m20();
        final float m21 = pose.m21();
        final float m22 = pose.m22();
        final float m30 = pose.m30();
        final float m31 = pose.m31();
        final float m32 = pose.m32();
        final float n00 = normalMatrix.m00();
        final float n01 = normalMatrix.m01();
        final float n02 = normalMatrix.m02();
        final float n10 = normalMatrix.m10();
        final float n11 = normalMatrix.m11();
        final float n12 = normalMatrix.m12();
        final float n20 = normalMatrix.m20();
        final float n21 = normalMatrix.m21();
        final float n22 = normalMatrix.m22();
        final boolean batched = consumer == target;
        final int colour = colour(red) | colour(green) << 8 | colour(blue) << 16 | colour(alpha) << 24;

        for (int quad = 0; quad + QUAD_FLOATS <= quads.length; quad += QUAD_FLOATS)
        {
            final float localX = quads[quad];
            final float localY = quads[quad + 1];
            final float localZ = quads[quad + 2];
            // The same sums, in the same order, as the normal matrix's own transform.
            final float normalX = n00 * localX + (n10 * localY + n20 * localZ);
            final float normalY = n01 * localX + (n11 * localY + n21 * localZ);
            final float normalZ = n02 * localX + (n12 * localY + n22 * localZ);

            if (batched)
            {
                if (count > CAPACITY - 4)
                    flush();
                final int packedNormal = normal(normalX) | normal(normalY) << 8 | normal(normalZ) << 16;
                long pointer = Scratch.BUFFER + (long) count * STRIDE;
                for (int vertex = quad + 3; vertex < quad + QUAD_FLOATS; vertex += 5, pointer += STRIDE)
                {
                    final float x = quads[vertex];
                    final float y = quads[vertex + 1];
                    final float z = quads[vertex + 2];
                    MemoryUtil.memPutFloat(pointer, m00 * x + m10 * y + m20 * z + m30);
                    MemoryUtil.memPutFloat(pointer + 4, m01 * x + m11 * y + m21 * z + m31);
                    MemoryUtil.memPutFloat(pointer + 8, m02 * x + m12 * y + m22 * z + m32);
                    MemoryUtil.memPutInt(pointer + 12, colour);
                    MemoryUtil.memPutFloat(pointer + 16, quads[vertex + 3]);
                    MemoryUtil.memPutFloat(pointer + 20, quads[vertex + 4]);
                    MemoryUtil.memPutInt(pointer + 24, packedOverlay);
                    MemoryUtil.memPutInt(pointer + 28, packedLight);
                    MemoryUtil.memPutInt(pointer + 32, packedNormal);
                }
                count += 4;
                continue;
            }

            for (int vertex = quad + 3; vertex < quad + QUAD_FLOATS; vertex += 5)
            {
                final float x = quads[vertex];
                final float y = quads[vertex + 1];
                final float z = quads[vertex + 2];
                VertexPlatform.vertex(consumer, m00 * x + m10 * y + m20 * z + m30, m01 * x + m11 * y + m21 * z + m31, m02 * x + m12 * y + m22 * z + m32, red, green, blue, alpha, quads[vertex + 3],
                    quads[vertex + 4], packedOverlay, packedLight, normalX, normalY, normalZ);
            }
        }
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
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    static void put(long pointer, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY,
        float normalZ)
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
        return (int) (value * 255.0F) & 0xFF;
    }

    private static int normal(float value)
    {
        return (int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF;
    }

    /** Native memory, allocated once the first batch opens, so it is never reserved without a bulk writer. */
    private static final class Scratch
    {
        private static final long BUFFER = MemoryUtil.nmemAlignedAlloc(64, (long) CAPACITY * STRIDE);
        /** Room for writers that copy a batch to convert it, such as outline and enchantment glint consumers. */
        private static final MemoryStack STACK = MemoryStack.create(8 * CAPACITY * STRIDE);
    }
}
