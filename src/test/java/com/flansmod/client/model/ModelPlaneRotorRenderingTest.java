package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelPlaneRotorRenderingTest
{
    @Test
    void mainAndTailRotorsRotateAboutTheirAuthoredHubAtEveryScaleAndPass()
    {
        ModelPlane model = new ModelPlane();
        model.oldRotateOrder = true;
        CapturedRotor rotor = new CapturedRotor(model);
        Vector3f origin = new Vector3f(7F, 3F, -0.5F);
        for (Axis axis : new Axis[] {Axis.YP, Axis.ZP})
        {
            for (float scale : new float[] {0.5F, 1F, 2F})
            {
                for (EnumRenderPass pass : EnumRenderPass.values())
                {
                    PoseStack stack = new PoseStack();
                    stack.translate(3D, 4D, 5D);
                    Matrix4f before = new Matrix4f(stack.last().pose());
                    model.renderAround(new ModelRendererTurbo[] {rotor, null}, origin, axis, 90F,
                        stack, null, 0, 0, 1F, 1F, 1F, 1F, scale, pass);
                    org.joml.Vector3f hub = new org.joml.Vector3f(origin.x * scale, origin.y * scale, origin.z * scale);
                    assertTrue(before.transformPosition(new org.joml.Vector3f(hub))
                        .equals(rotor.pose.transformPosition(new org.joml.Vector3f(hub)), 1E-5F));
                    org.joml.Vector3f tip = new org.joml.Vector3f(hub).add(scale, 0F, 0F);
                    org.joml.Vector3f actualTip = rotor.pose.transformPosition(tip);
                    org.joml.Vector3f expectedTip = before.transformPosition(new org.joml.Vector3f(hub)
                        .add(0F, axis == Axis.ZP ? scale : 0F, axis == Axis.YP ? -scale : 0F));
                    assertTrue(actualTip.equals(expectedTip, 1E-5F));
                    assertFalse(rotor.oldOrder, "Rotor meshes use the legacy ordinary rotation order");
                    assertEquals(pass, rotor.pass);
                    assertEquals(before, stack.last().pose(), "A rotor must not move subsequent model groups");
                    assertTrue(stack.clear());
                }
            }
        }
    }

    private static final class CapturedRotor extends ModelRendererTurbo
    {
        private Matrix4f pose;
        private boolean oldOrder;
        private EnumRenderPass pass;

        private CapturedRotor(ModelPlane model)
        {
            super(model, 0, 0);
        }

        @Override
        public void render(@NotNull PoseStack stack, @NotNull VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass, boolean oldRotateOrder)
        {
            pose = new Matrix4f(stack.last().pose());
            oldOrder = oldRotateOrder;
            pass = renderPass;
        }
    }
}
