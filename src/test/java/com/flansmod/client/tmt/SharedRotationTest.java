package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Animated parts build their local transform directly; it must equal the chained PoseStack operations it replaces. */
class SharedRotationTest
{
    @Test
    void directLocalTransformsMatchTheChainedPoseStackOperations()
    {
        float[][] angles = {{0, 0, 0}, {0.7F, 0, 0}, {0, -1.2F, 0}, {0, 0, 2.5F}, {0.3F, -0.8F, 1.9F}, {-2F, 3F, -0.4F}};
        for (boolean oldRotateOrder : new boolean[]{false, true})
            for (float scale : new float[]{1F, 0.75F})
                for (float[] angle : angles)
                {
                    // Two parts sharing the angles, as an animated array does; the second reuses the rotation.
                    for (int copy = 0; copy < 2; copy++)
                    {
                        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0, 64, 64);
                        part.addBox(-1, -2, -3, 2, 4, 6);
                        part.setRotationPoint(3 + copy, -2, 5);
                        part.offsetX = 0.25F;
                        part.offsetZ = -0.5F;
                        part.rotateAngleX = angle[0];
                        part.rotateAngleY = angle[1];
                        part.rotateAngleZ = angle[2];
                        PoseStack parent = new PoseStack();
                        parent.translate(1, 2, 3);
                        parent.mulPose(Axis.YP.rotation(0.4F));
                        Recording actual = new Recording();
                        part.render(parent, actual, 0, 0, 1, 1, 1, 1, scale, com.flansmodultimate.client.render.EnumRenderPass.DEFAULT, oldRotateOrder);

                        PoseStack expected = new PoseStack();
                        expected.translate(1, 2, 3);
                        expected.mulPose(Axis.YP.rotation(0.4F));
                        expected.translate(part.offsetX, part.offsetY, part.offsetZ);
                        expected.translate(part.rotationPointX * 0.0625F * scale, part.rotationPointY * 0.0625F * scale, part.rotationPointZ * 0.0625F * scale);
                        if (!oldRotateOrder && angle[1] != 0) expected.mulPose(Axis.YP.rotation(angle[1]));
                        if (angle[2] != 0) expected.mulPose(Axis.ZP.rotation(oldRotateOrder ? -angle[2] : angle[2]));
                        if (oldRotateOrder && angle[1] != 0) expected.mulPose(Axis.YP.rotation(-angle[1]));
                        if (angle[0] != 0) expected.mulPose(Axis.XP.rotation(angle[0]));
                        if (scale != 1) expected.scale(scale, scale, scale);
                        Recording reference = new Recording();
                        for (TexturedPolygon face : part.getRenderPolygons())
                            face.draw(expected.last(), reference, 0, 0, 1, 1, 1, 1);
                        assertEquals(reference.vertices.size(), actual.vertices.size());
                        for (int i = 0; i < reference.vertices.size(); i++)
                            assertArrayEquals(reference.vertices.get(i), actual.vertices.get(i), 2E-5F,
                                "angles " + java.util.Arrays.toString(angle) + ", old order " + oldRotateOrder + ", scale " + scale);
                    }
                }
    }

    private static final class Recording implements VertexConsumer
    {
        final List<float[]> vertices = new ArrayList<>();
        @Override public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int o, int l, float nx, float ny, float nz)
        { vertices.add(new float[]{x, y, z, u, v, nx, ny, nz}); }
        @Override public VertexConsumer vertex(double x, double y, double z) { throw new AssertionError(); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { throw new AssertionError(); }
        @Override public VertexConsumer uv(float u, float v) { throw new AssertionError(); }
        @Override public VertexConsumer overlayCoords(int u, int v) { throw new AssertionError(); }
        @Override public VertexConsumer uv2(int u, int v) { throw new AssertionError(); }
        @Override public VertexConsumer normal(float x, float y, float z) { throw new AssertionError(); }
        @Override public void endVertex() { throw new AssertionError(); }
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
    }
}
