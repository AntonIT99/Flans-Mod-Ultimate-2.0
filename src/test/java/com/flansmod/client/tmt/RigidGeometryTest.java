package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.gpu.RigidGeometry;
import com.flansmodultimate.client.render.gpu.RigidGeometryConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RigidGeometryTest
{
    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 7, 8, 12})
    void uploadSizeAndInvalidationFollowPolygonChanges(int count)
    {
        PositionTextureVertex[] vertices = new PositionTextureVertex[count];
        for (int i = 0; i < count; i++)
            vertices[i] = new PositionTextureVertex(i, i * i, 0, i, 0);
        TexturedPolygon face = new TexturedPolygon(vertices);
        TexturedPolygon[] faces = {face};
        RigidGeometry geometry = new RigidGeometry(faces);
        assertTrue(geometry.supported());
        assertTrue(geometry.matches(faces));
        RecordingVertexConsumer output = new RecordingVertexConsumer();
        geometry.draw(new PoseStack().last(), output, 17, 23, 1, 1, 1, 1);
        assertEquals(output.vertices.size(), geometry.vertexCount());
        face.flipFace();
        assertFalse(geometry.matches(faces));
        assertFalse(new RigidGeometry(faces).matches(new TexturedPolygon[0]));
    }

    @Test
    void animatedPartsReuseGeometryButKeepCurrentPoseLightingAndTint()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        part.addBox(0, 0, 0, 16, 8, 4);
        RigidGeometry previous = null;
        for (int frame = 0; frame < 5; frame++)
        {
            part.rotateAngleY = frame * 0.2F;
            part.rotationPointX = frame;
            PoseStack stack = new PoseStack();
            stack.translate(frame, -2, 3);
            stack.mulPose(Axis.XP.rotation(0.3F));
            stack.scale(1.2F, 0.7F, 2F);
            RecordingVertexConsumer expected = new RecordingVertexConsumer();
            Capture actual = new Capture();
            part.render(stack, expected, 17 + frame, 23 + frame, 0.2F, 0.3F, 0.4F, 0.5F, 1);
            part.render(stack, actual, 17 + frame, 23 + frame, 0.2F, 0.3F, 0.4F, 0.5F, 1);
            assertNotNull(actual.geometry);
            if (previous != null) assertSame(previous, actual.geometry);
            previous = actual.geometry;
            assertEquals(expected.vertices.size(), actual.vertices.size());
            for (int i = 0; i < expected.vertices.size(); i++)
                assertArrayEquals(expected.vertices.get(i), actual.vertices.get(i), 1E-5F);
        }
    }

    @Test
    void deformableAndSpecialNormalGeometryRemainOnCpu()
    {
        TexturedPolygon face = new TexturedPolygon(new PositionTextureVertex[]{
            new PositionTransformVertex(0, 0, 0, 0, 0),
            new PositionTextureVertex(16, 0, 0, 1, 0),
            new PositionTextureVertex(0, 16, 0, 0, 1)});
        assertFalse(new RigidGeometry(new TexturedPolygon[]{face}).supported());
        TexturedPolygon rigid = new TexturedPolygon(new PositionTextureVertex[]{
            new PositionTextureVertex(0, 0, 0, 0, 0),
            new PositionTextureVertex(16, 0, 0, 1, 0),
            new PositionTextureVertex(0, 16, 0, 0, 1)});
        RigidGeometry original = new RigidGeometry(new TexturedPolygon[]{rigid});
        rigid.setInvertNormal(true);
        assertFalse(original.matches(new TexturedPolygon[]{rigid}));
        assertFalse(new RigidGeometry(new TexturedPolygon[]{rigid}).supported());
    }

    private static class Capture extends RecordingVertexConsumer implements RigidGeometryConsumer
    {
        RigidGeometry geometry;

        @Override public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                                     float red, float green, float blue, float alpha)
        {
            this.geometry = geometry;
            geometry.draw(pose, this, light, overlay, red, green, blue, alpha);
        }
    }
}
