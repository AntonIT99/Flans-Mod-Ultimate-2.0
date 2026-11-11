package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Shape2D;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.platform.render.TestVertexSink;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.renderer.MultiBufferSource;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

import static org.junit.jupiter.api.Assertions.*;

/** The direct sink must write exactly the vertices the plain consumer path does, in the same order. */
class DirectRigidSinkTest
{
    private static final int LIGHT = 0x00A000F0;
    private static final int OVERLAY = 0x000A0003;

    @BeforeAll
    static void renderThread()
    {
        if (!RenderSystem.isOnRenderThread())
            RenderSystem.initRenderThread();
    }

    private static ModelRendererTurbo[] model()
    {
        ModelBase base = new ModelBase()
        {};
        ModelRendererTurbo[] parts = new ModelRendererTurbo[40];
        for (int i = 0; i < parts.length; i++)
        {
            ModelRendererTurbo part = new ModelRendererTurbo(base, 0, 0, 128, 128);
            part.addBox(0, 0, 0, 1 + i % 5, 1 + i % 3, 2);
            if (i % 6 == 1)
                part.addShape3D(0, 0, 0, new Shape2D(new Coord2D[]{new Coord2D(0, 0), new Coord2D(3, 0), new Coord2D(4, 2), new Coord2D(2, 4), new Coord2D(0, 3)}), 2, 8, 8, 16, 2,
                    ModelRendererTurbo.MR_FRONT);
            if (i % 9 != 4)
                part.setRotationPoint(i % 7 - 3, i % 4, i % 6 - 3); // A few untransformed parts
            if (i % 5 == 0)
                part.rotateAngleY = 0.4F;
            if (i == 11)
                part.glow = true;
            if (i == 21)
            {
                ModelRendererTurbo child = new ModelRendererTurbo(base, 0, 0, 128, 128);
                child.addBox(1, 0, 1, 2, 2, 2);
                child.setRotationPoint(1, 1, 1);
                part.addChild(child);
            }
            parts[i] = part;
        }
        parts[17] = null;
        return parts;
    }

    /** Changes legacy code can make between frames. */
    private static void change(ModelRendererTurbo[] parts, int frame)
    {
        if (frame == 12)
            parts[3].rotateAngleZ = 0.25F;
        if (frame == 16)
            parts[6].isHidden = true;
        if (frame == 20)
            parts[6].isHidden = false;
        if (frame >= 22)
            parts[8].rotateAngleX = frame * 0.05F; // Animated from here on
        if (frame == 30)
            parts[9].addBox(0, 2, 0, 1, 1, 1);
        if (frame == 44)
            parts[14].glow = true;
        if (frame == 48)
            parts[13].setRotationPoint(1, 2, 3);
    }

    private static PoseStack parent(int frame)
    {
        PoseStack parent = new PoseStack();
        parent.translate(0.5, -1, -4 - frame * 2.5);
        parent.mulPose(Axis.YP.rotationDegrees(frame * 13));
        parent.scale(1.25F, 1.25F, 1.25F);
        return parent;
    }

    @Test
    void writesWhatThePlainConsumerReceives()
    {
        ModelRendererTurbo[] direct = model(), reference = model();
        DirectRigidSink sink = new DirectRigidSink();
        for (int frame = 0; frame < 60; frame++)
        {
            change(direct, frame);
            change(reference, frame);
            PoseStack parent = parent(frame);
            Recorder expected = new Recorder(), actual = new Recorder();
            ModelRendererTurbo.renderArray(reference, parent, expected, LIGHT, OVERLAY, 1F, 0.9F, 0.8F, 0.7F, 1F, EnumRenderPass.DEFAULT, false);
            sink.begin(type -> actual, null);
            ModelRendererTurbo.renderArray(direct, parent, sink, LIGHT, OVERLAY, 1F, 0.9F, 0.8F, 0.7F, 1F, EnumRenderPass.DEFAULT, false);
            sink.end();
            assertFalse(expected.vertices.isEmpty());
            assertVerticesEqual(expected, actual, "frame " + frame);
        }
    }

    /** With size culling, the sink draws the parts the rigid paths report visible, as the mesh path does. */
    @Test
    void sizeCulledPartsWriteNothing()
    {
        ModelRendererTurbo[] direct = model(), reference = model();
        DirectRigidSink sink = new DirectRigidSink();
        boolean culledSome = false;
        for (int frame = 0; frame < 60; frame++)
        {
            change(direct, frame);
            change(reference, frame);
            PoseStack parent = parent(frame);
            Recorder expected = new Recorder(), actual = new Recorder();
            PolygonSink polygons = new PolygonSink(expected);
            ModelRendererTurbo.beginScreenSpaceCulling(2F, 400F);
            try
            {
                ModelRendererTurbo.renderArray(reference, parent, polygons, LIGHT, OVERLAY, 1F, 0.9F, 0.8F, 0.7F, 1F, EnumRenderPass.DEFAULT, false);
                sink.begin(type -> actual, null);
                ModelRendererTurbo.renderArray(direct, parent, sink, LIGHT, OVERLAY, 1F, 0.9F, 0.8F, 0.7F, 1F, EnumRenderPass.DEFAULT, false);
                sink.end();
            }
            finally
            {
                ModelRendererTurbo.endScreenSpaceCulling();
            }
            culledSome |= polygons.culled > 0;
            assertVerticesEqual(expected, actual, "frame " + frame);
        }
        assertTrue(culledSome, "the scene crosses size-culling thresholds");
    }

    @Test
    void bulkWritesSpanPartsAndMatchTheBuilderByteForByte()
    {
        ModelRendererTurbo[] direct = model(), reference = model();
        DirectRigidSink sink = new DirectRigidSink();
        for (int frame = 0; frame < 8; frame++)
        {
            PoseStack parent = parent(frame);
            BufferBuilder builder = new BufferBuilder(256);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            ModelRendererTurbo.renderArray(reference, parent, builder, LIGHT, OVERLAY, 1F, 0.9F, 0.8F, 0.7F, 1F, EnumRenderPass.DEFAULT, false);
            byte[] expected = bytes(builder);

            BulkRecorder recorder = new BulkRecorder();
            sink.begin(type -> recorder, null);
            ModelRendererTurbo.renderArray(direct, parent, sink, LIGHT, OVERLAY, 1F, 0.9F, 0.8F, 0.7F, 1F, EnumRenderPass.DEFAULT, false);
            sink.end();
            byte[] actual = recorder.bytes.toByteArray();

            assertEquals(expected.length, actual.length, "frame " + frame);
            assertEquals(0, recorder.singleVertices, "frame " + frame);
            // 38 parts of at least 24 vertices fit a few writes of at most 512 vertices instead of one per part.
            assertEquals((expected.length / 36 + 511) / 512, recorder.pushes, "frame " + frame);
            for (int vertex = 0; vertex < expected.length / 36; vertex++)
            {
                // The builder leaves the padding byte unwritten.
                for (int index = vertex * 36; index < vertex * 36 + 35; index++)
                    assertEquals(expected[index], actual[index], "frame " + frame + " vertex " + vertex + " byte " + index % 36);
                assertEquals(0, actual[vertex * 36 + 35]);
            }
        }
    }

    @Test
    void elementWritesNeverOvertakeBatchedVertices()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase()
        {}, 0, 0, 64, 64);
        part.addBox(0, 0, 0, 2, 2, 2);
        BulkRecorder recorder = new BulkRecorder();
        DirectRigidSink sink = new DirectRigidSink();
        sink.begin(type -> recorder, null);
        part.render(new PoseStack(), sink, LIGHT, OVERLAY, 1F, 1F, 1F, 1F, 1F);
        assertEquals(0, recorder.pushes, "the part's vertices wait in the open batch");
        sink.vertex(1D, 2D, 3D).color(255, 255, 255, 255).uv(0F, 0F).overlayCoords(0, 0).uv2(0, 0).normal(0F, 1F, 0F).endVertex();
        assertEquals(List.of("push 24", "element"), recorder.events.subList(0, 2));
        part.render(new PoseStack(), sink, LIGHT, OVERLAY, 1F, 1F, 1F, 1F, 1F);
        sink.end();
        assertEquals("push 24", recorder.events.get(recorder.events.size() - 1));
        assertEquals(2, recorder.pushes);
    }

    @Test
    void theBufferIsFetchedOnFirstUseAndAgainAfterANestedScope()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase()
        {}, 0, 0, 64, 64);
        part.addBox(0, 0, 0, 2, 2, 2);
        Recorder first = new Recorder(), second = new Recorder();
        int[] fetches = {0};
        MultiBufferSource source = type -> fetches[0]++ == 0 ? first : second;
        DirectRigidSink sink = new DirectRigidSink();
        sink.begin(source, null);
        assertEquals(0, fetches[0], "a scope that draws nothing starts no buffer");
        part.render(new PoseStack(), sink, LIGHT, OVERLAY, 1F, 1F, 1F, 1F, 1F);
        part.render(new PoseStack(), sink, LIGHT, OVERLAY, 1F, 1F, 1F, 1F, 1F);
        assertEquals(1, fetches[0]);
        sink.suspend();
        part.render(new PoseStack(), sink, LIGHT, OVERLAY, 1F, 1F, 1F, 1F, 1F);
        sink.end();
        assertEquals(2, fetches[0]);
        assertEquals(48, first.vertices.size());
        assertEquals(24, second.vertices.size());
    }

    private static void assertVerticesEqual(Recorder expected, Recorder actual, String where)
    {
        assertEquals(expected.vertices.size(), actual.vertices.size(), where + " vertex count");
        for (int i = 0; i < expected.vertices.size(); i++)
            assertArrayEquals(expected.vertices.get(i), actual.vertices.get(i), where + " vertex " + i);
    }

    private static byte[] bytes(BufferBuilder builder)
    {
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

    private static class Recorder extends TestVertexSink
    {
        final List<float[]> vertices = new ArrayList<>();

        @Override
        protected void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
        {
            vertices.add(new float[]{x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ});
        }
    }

    /** The rigid submission paths, drawn polygon by polygon as before the flat arrays. */
    private static final class PolygonSink extends Recorder implements RigidGeometryConsumer
    {
        private final Recorder target;
        private int culled;

        private PolygonSink(Recorder target)
        {
            this.target = target;
        }

        @Override
        protected void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
        {
            target.accept(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
        }

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha)
        {
            geometry.draw(pose, target, light, overlay, red, green, blue, alpha);
        }

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
        {
            if (visible)
                submit(geometry, pose, light, overlay, red, green, blue, alpha);
            else
                culled++;
        }
    }

    /** A consumer that Embeddium would have made a writer, recording the order of what each path receives. */
    private static final class BulkRecorder implements VertexConsumer, VertexBufferWriter
    {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final List<String> events = new ArrayList<>();
        private int pushes;
        private int singleVertices;

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format)
        {
            assertEquals(0, count % 4, "a quad was split between writes");
            byte[] copy = new byte[count * 36];
            MemoryUtil.memByteBuffer(ptr, copy.length).get(copy);
            bytes.writeBytes(copy);
            pushes++;
            events.add("push " + count);
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float nx, float ny, float nz)
        {
            singleVertices++;
        }

        private VertexConsumer element()
        {
            if (events.isEmpty() || !events.get(events.size() - 1).equals("element"))
                events.add("element");
            return this;
        }

        @Override
        public @NotNull VertexConsumer vertex(double x, double y, double z)
        {
            return element();
        }

        @Override
        public @NotNull VertexConsumer color(int r, int g, int b, int a)
        {
            return element();
        }

        @Override
        public @NotNull VertexConsumer uv(float u, float v)
        {
            return element();
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int u, int v)
        {
            return element();
        }

        @Override
        public @NotNull VertexConsumer uv2(int u, int v)
        {
            return element();
        }

        @Override
        public @NotNull VertexConsumer normal(float x, float y, float z)
        {
            return element();
        }

        @Override
        public void endVertex()
        {
            element();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a)
        {
            element();
        }

        @Override
        public void unsetDefaultColor()
        {
            element();
        }
    }
}
