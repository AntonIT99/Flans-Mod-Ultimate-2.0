package com.flansmodultimate.client.render;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.platform.render.VertexWriterPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/** Drives the real reflective binding through the test stand-ins of Embeddium's writer API. */
class EntityVertexBatchTest
{
    private static final int LIGHT = 0x00A000F0;
    private static final int OVERLAY = 0x000A0003;

    @BeforeAll
    static void renderThread()
    {
        if (!RenderSystem.isOnRenderThread())
            RenderSystem.initRenderThread();
    }

    @Test
    void bulkWritesMatchTheBuilderByteForByteAcrossBatchBoundaries()
    {
        // 30 boxes of 24 vertices need two bulk writes of at most 512 vertices.
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        for (int box = 0; box < 30; box++)
            part.addBox(box - 15, box % 4, -box * 0.5F, 3, 2 + box % 3, 4);
        part.rotationPointX = 2F;
        part.rotateAngleY = 0.4F;
        PoseStack pose = new PoseStack();
        pose.translate(1.5F, -2F, 3F);
        pose.mulPose(Axis.XP.rotationDegrees(30F));

        byte[] expected = renderToBuilder(part, pose);
        BulkRecorder recorder = new BulkRecorder(true);
        part.render(pose, recorder, LIGHT, OVERLAY, 0.9F, 0.5F, 0.25F, 0.75F, 1F);
        byte[] actual = recorder.bytes.toByteArray();

        assertEquals(720 * 36, expected.length);
        assertEquals(expected.length, actual.length);
        assertEquals(2, recorder.pushes);
        assertEquals(0, recorder.singleVertices);
        assertSame(ModelVertex.FORMAT, recorder.format);
        for (int vertex = 0; vertex < expected.length / 36; vertex++)
        {
            // The builder leaves the padding byte unwritten.
            for (int index = vertex * 36; index < vertex * 36 + 35; index++)
                assertEquals(expected[index], actual[index], "vertex " + vertex + " byte " + index % 36);
            assertEquals(0, actual[vertex * 36 + 35]);
        }
    }

    @Test
    void consumersThatRefuseBulkWritesStillGetEveryVertex()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        part.addBox(0, 0, 0, 16, 16, 16);
        BulkRecorder refusing = new BulkRecorder(false);
        assertFalse(VertexWriterPlatform.canWrite(refusing));
        part.render(new PoseStack(), refusing, LIGHT, OVERLAY, 1F, 1F, 1F, 1F, 1F);
        assertEquals(0, refusing.pushes);
        assertEquals(24, refusing.singleVertices);
    }

    @Test
    void onlyWriterConsumersAreBatched()
    {
        assertTrue(VertexWriterPlatform.canWrite(new BulkRecorder(true)));
        assertEquals("Embeddium", VertexWriterPlatform.rendererName());
        BufferBuilder builder = new BufferBuilder(256);
        assertFalse(VertexWriterPlatform.canWrite(builder));
        assertFalse(EntityVertexBatch.begin(builder));
    }

    private static byte[] renderToBuilder(ModelRendererTurbo part, PoseStack pose)
    {
        BufferBuilder builder = new BufferBuilder(256);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        part.render(pose, builder, LIGHT, OVERLAY, 0.9F, 0.5F, 0.25F, 0.75F, 1F);
        BufferBuilder.RenderedBuffer rendered = builder.end();
        try
        {
            ByteBuffer vertices = rendered.vertexBuffer();
            byte[] bytes = new byte[vertices.remaining()];
            vertices.get(bytes);
            return bytes;
        }
        finally
        {
            rendered.release();
        }
    }

    /** A consumer that Embeddium would have made a writer, recording what each path receives. */
    private static final class BulkRecorder implements VertexConsumer, VertexBufferWriter
    {
        private final boolean acceptsBulk;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private int pushes;
        private int singleVertices;
        private VertexFormatDescription format;

        private BulkRecorder(boolean acceptsBulk)
        {
            this.acceptsBulk = acceptsBulk;
        }

        @Override
        public boolean canUseIntrinsics()
        {
            return acceptsBulk;
        }

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format)
        {
            assertEquals(0, count % 4, "a quad was split between writes");
            byte[] copy = new byte[count * 36];
            MemoryUtil.memByteBuffer(ptr, copy.length).get(copy);
            bytes.writeBytes(copy);
            pushes++;
            this.format = format;
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                           float u, float v, int overlay, int light, float nx, float ny, float nz)
        {
            singleVertices++;
        }

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
