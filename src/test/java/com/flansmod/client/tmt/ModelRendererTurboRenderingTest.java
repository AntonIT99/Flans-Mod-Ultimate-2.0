package com.flansmod.client.tmt;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ModelRendererTurboRenderingTest
{
    @Test
    void transformsMatchOriginalQuaternionStackMathIncludingChildrenAndExceptionalScales()
    {
        for (boolean children : new boolean[]{false, true})
            for (boolean oldOrder : new boolean[]{false, true})
                for (float scale : new float[]{1, 0.0625F, 2, -2, 0, Float.NaN, Float.POSITIVE_INFINITY})
                {
                    ModelRendererTurbo parent = box(), child = box();
                    parent.offsetX = 0.2F; parent.offsetY = -0.4F;
                    parent.rotationPointX = 3; parent.rotationPointZ = -7;
                    parent.rotateAngleX = 0.31F; parent.rotateAngleY = -0.79F; parent.rotateAngleZ = 1.11F;
                    child.rotationPointY = 12; child.rotateAngleY = -0.62F;
                    if (children) parent.addChild(child);
                    PoseStack stack = new PoseStack();
                    stack.scale(-2, 0.75F, 3);
                    RecordingVertexConsumer actual = new RecordingVertexConsumer();
                    parent.render(stack, actual, 17, 23, 1, 1, 1, 1, scale, EnumRenderPass.DEFAULT, oldOrder);
                    RecordingVertexConsumer expected = new RecordingVertexConsumer();
                    originalTransform(stack, parent, scale, oldOrder);
                    box().render(stack, expected, 17, 23, 1, 1, 1, 1, 1);
                    if (children)
                    {
                        originalTransform(stack, child, scale, false);
                        box().render(stack, expected, 17, 23, 1, 1, 1, 1, 1);
                    }
                    assertEquals(expected.vertices.size(), actual.vertices.size());
                    for (int i = 0; i < expected.vertices.size(); i++)
                        for (int component = 0; component < expected.vertices.get(i).length; component++)
                        {
                            float value = expected.vertices.get(i)[component];
                            float result = actual.vertices.get(i)[component];
                            if (!Float.isFinite(value)) assertEquals(value, result);
                            else assertEquals(value, result, Math.max(2E-5F, Math.abs(value) * 2E-6F),
                                "children=" + children + ", oldOrder=" + oldOrder + ", scale=" + scale);
                        }
                }
    }

    private static void originalTransform(PoseStack stack, ModelRendererTurbo part, float scale, boolean oldOrder)
    {
        stack.translate(part.offsetX, part.offsetY, part.offsetZ);
        stack.translate(part.rotationPointX * 0.0625F * scale, part.rotationPointY * 0.0625F * scale, part.rotationPointZ * 0.0625F * scale);
        if (!oldOrder && part.rotateAngleY != 0) stack.mulPose(Axis.YP.rotation(part.rotateAngleY));
        if (part.rotateAngleZ != 0) stack.mulPose(Axis.ZP.rotation(oldOrder ? -part.rotateAngleZ : part.rotateAngleZ));
        if (oldOrder && part.rotateAngleY != 0) stack.mulPose(Axis.YP.rotation(-part.rotateAngleY));
        if (part.rotateAngleX != 0) stack.mulPose(Axis.XP.rotation(part.rotateAngleX));
        if (scale != 1) stack.scale(scale, scale, scale);
    }

    @Test
    void squaredCullingNeverRejectsMoreThanPreviousBoundIncludingShear()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        part.addBox(0, 0, 0, 16, 16, 16);
        java.util.Random random = new java.util.Random(1234);
        ModelRendererTurbo.beginScreenSpaceCulling(8, 150);
        int culled = 0;
        try
        {
            for (int i = 0; i < 2000; i++)
            {
                PoseStack pose = new PoseStack();
                pose.translate(random.nextFloat() * 100, random.nextFloat() * 100, random.nextFloat() * 100);
                pose.scale(0.1F + random.nextFloat() * 3, 0.1F + random.nextFloat() * 3, 0.1F + random.nextFloat() * 3);
                pose.mulPose(Axis.YP.rotation(random.nextFloat() * 6));
                pose.mulPose(Axis.XP.rotation(random.nextFloat() * 6));
                RecordingVertexConsumer output = new RecordingVertexConsumer();
                part.render(pose, output, 17, 23, 1, 1, 1, 1, 1);
                if (!output.vertices.isEmpty()) continue;
                culled++;
                Matrix4f m = pose.last().pose();
                float x = (m.m00() + m.m10() + m.m20()) * 0.5F + m.m30();
                float y = (m.m01() + m.m11() + m.m21()) * 0.5F + m.m31();
                float z = (m.m02() + m.m12() + m.m22()) * 0.5F + m.m32();
                double sx = Math.sqrt(m.m00()*m.m00() + m.m01()*m.m01() + m.m02()*m.m02());
                double sy = Math.sqrt(m.m10()*m.m10() + m.m11()*m.m11() + m.m12()*m.m12());
                double sz = Math.sqrt(m.m20()*m.m20() + m.m21()*m.m21() + m.m22()*m.m22());
                double radius = Math.sqrt(0.75) * Math.max(sx, Math.max(sy, sz));
                double diameter = 2 * radius * 150 / Math.max(0.01, Math.sqrt(x*x + y*y + z*z) - radius);
                assertTrue(diameter < 8);
            }
            assertTrue(culled > 0);
            RecordingVertexConsumer near = new RecordingVertexConsumer();
            part.render(new PoseStack(), near, 17, 23, 1, 1, 1, 1, 1);
            assertEquals(24, near.vertices.size());
        }
        finally { ModelRendererTurbo.endScreenSpaceCulling(); }
    }

    @Test
    void customArmourRegistersPartsThroughModelContract()
    {
        ModelCustomArmour model = new ModelCustomArmour();
        new ModelRendererTurbo(model, 0, 0);

        AtomicInteger modelBoxCount = new AtomicInteger();
        model.forEachModelBox(modelRenderer -> modelBoxCount.incrementAndGet());

        assertEquals(1, modelBoxCount.get());
    }

    @Test
    void interfaceTypedChildIsRetainedAndRendered()
    {
        ModelBase model = new ModelBase() {};
        IModelRenderer parent = new ModelRendererTurbo(model, 0, 0);
        IModelRenderer child = new ModelRendererTurbo(model, 0, 0);
        child.addBox(0, 0, 0, 1, 1, 1);

        parent.addChild(child);
        RecordingVertexConsumer vertices = new RecordingVertexConsumer();
        parent.render(new PoseStack(), vertices, 17, 23, 1, 1, 1, 1, 1);

        assertFalse(vertices.vertices.isEmpty());
    }

    @Test
    void leafCacheMatchesStackPathAcrossAnimationScaleAndRotationOrderChanges()
    {
        ModelRendererTurbo leaf = box();
        ModelRendererTurbo reference = box();
        ModelRendererTurbo invisibleChild = box();
        invisibleChild.isHidden = true;
        reference.addChild(invisibleChild); // Retains the original parent/child stack path.

        CountingPoseStack parent = new CountingPoseStack();
        for (boolean oldOrder : new boolean[]{false, true, false})
        {
            for (float scale : new float[]{1F, 0.0625F, 2F, -0.5F, 0F, 1F})
            {
                for (int state = 0; state < 12; state++)
                {
                    // Change each cache input separately, then render the unchanged pose twice.
                    mutate(leaf, state);
                    copyTransform(leaf, reference);
                    for (int instance = 0; instance < 2; instance++)
                    {
                        parent.setIdentity();
                        parent.translate(7 + state, -3, 9 + instance);
                        parent.mulPose(Axis.YP.rotation(0.7F + instance));
                        parent.scale(1.2F, 0.8F, 0.4F);
                        Matrix4f originalPosition = new Matrix4f(parent.last().pose());
                        Matrix3f originalNormal = new Matrix3f(parent.last().normal());

                        RecordingVertexConsumer actual = new RecordingVertexConsumer();
                        int pushes = parent.pushes;
                        leaf.render(parent, actual, 17, 23, 0.2F, 0.3F, 0.4F, 0.5F,
                            scale, EnumRenderPass.DEFAULT, oldOrder);
                        if (scale > 0F)
                            assertEquals(pushes, parent.pushes, "Cached leaf draws must not push the caller's stack");
                        assertEquals(originalPosition, parent.last().pose());
                        assertEquals(originalNormal, parent.last().normal());

                        RecordingVertexConsumer expected = new RecordingVertexConsumer();
                        reference.render(parent, expected, 17, 23, 0.2F, 0.3F, 0.4F, 0.5F,
                            scale, EnumRenderPass.DEFAULT, oldOrder);
                        assertEquals(expected.vertices.size(), actual.vertices.size());
                        for (int i = 0; i < actual.vertices.size(); i++)
                            assertArrayEquals(expected.vertices.get(i), actual.vertices.get(i), 2E-5F);
                        assertTrue(parent.clear());
                    }
                }
            }
        }
    }

    @Test
    void eightSidedWheelExtrusionUses56VerticesAndCacheSurvivesUvRescaling()
    {
        ModelRendererTurbo wheel = new ModelRendererTurbo(new ModelBase() {}, 0, 0, 64, 32);
        Coord2D[] points = {new Coord2D(4, 0, 4, 0), new Coord2D(11, 0, 11, 0),
            new Coord2D(15, 4, 15, 4), new Coord2D(15, 11, 15, 11), new Coord2D(11, 15, 11, 15),
            new Coord2D(4, 15, 4, 15), new Coord2D(0, 11, 0, 11), new Coord2D(0, 4, 0, 4)};
        wheel.addShape3D(0, 0, 0, new Shape2D(points), 2, 15, 15, 52, 2, 0);
        wheel.rotationPointX = 32;
        RecordingVertexConsumer before = new RecordingVertexConsumer();
        wheel.render(new PoseStack(), before, 17, 23, 1, 1, 1, 1, 1);
        assertEquals(56, before.vertices.size()); // Formerly 80, with the same 28 triangles.
        assertTrue(wheel.applyActualTextureSize(128, 32));
        RecordingVertexConsumer after = new RecordingVertexConsumer();
        wheel.render(new PoseStack(), after, 17, 23, 1, 1, 1, 1, 1);
        assertEquals(before.vertices.size(), after.vertices.size());
        for (int i = 0; i < before.vertices.size(); i++)
        {
            float[] expected = before.vertices.get(i).clone();
            expected[7] *= 0.5F;
            assertArrayEquals(expected, after.vertices.get(i));
        }
    }

    private static ModelRendererTurbo box()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        part.addBox(1, 2, 3, 4, 5, 6);
        return part;
    }

    private static void mutate(ModelRendererTurbo part, int state)
    {
        switch (state)
        {
            case 0 -> part.offsetX += 0.2F;
            case 1 -> part.offsetY -= 0.3F;
            case 2 -> part.offsetZ += 0.4F;
            case 3 -> part.rotationPointX += 2F;
            case 4 -> part.rotationPointY -= 3F;
            case 5 -> part.rotationPointZ += 4F;
            case 6 -> part.rotateAngleX += 0.1F;
            case 7 -> part.rotateAngleY -= 0.2F;
            case 8 -> part.rotateAngleZ += 0.3F;
            case 9 -> { part.rotateAngleX = 0; part.rotateAngleY = 0; part.rotateAngleZ = 0; }
            case 10 -> { part.offsetX = 0; part.offsetY = 0; part.offsetZ = 0; }
            case 11 -> { part.rotationPointX = 0; part.rotationPointY = 0; part.rotationPointZ = 0; }
            default -> throw new AssertionError();
        }
    }

    private static void copyTransform(ModelRendererTurbo from, ModelRendererTurbo to)
    {
        to.offsetX = from.offsetX; to.offsetY = from.offsetY; to.offsetZ = from.offsetZ;
        to.rotationPointX = from.rotationPointX; to.rotationPointY = from.rotationPointY;
        to.rotationPointZ = from.rotationPointZ;
        to.rotateAngleX = from.rotateAngleX; to.rotateAngleY = from.rotateAngleY; to.rotateAngleZ = from.rotateAngleZ;
    }

    private static final class CountingPoseStack extends PoseStack
    {
        private int pushes;

        @Override
        public void pushPose()
        {
            pushes++;
            super.pushPose();
        }
    }
}
