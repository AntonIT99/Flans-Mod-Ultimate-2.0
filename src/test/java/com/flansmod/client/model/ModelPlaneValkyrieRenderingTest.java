package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.driveables.ValkyrieAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPlaneValkyrieRenderingTest
{
    private static final int CORE = 0;
    private static final int MID_FRONT = 1;

    @Test
    void restingJointsSitAtTheirLegacyPositions()
    {
        CapturedJoint[] joints = render(new ValkyrieAnimation());
        ValkyrieAnimation reference = new ValkyrieAnimation();
        for (ValkyrieAnimation.Part part : reference.getParts())
        {
            Vector3f joint = part.getPosition();
            Vector3f actual = joints[part.getId()].pose.transformPosition(new Vector3f());
            // RenderPlane.renderAnimPart translated by (x, -y, -z) / 16.
            assertTrue(actual.equals(new Vector3f(joint.x, -joint.y, -joint.z).div(16F), 1E-5F),
                "joint " + part.getId() + " at " + actual);
            assertFalse(joints[part.getId()].oldOrder, "Joints use the legacy ordinary rotation order");
        }
    }

    @Test
    void childrenFollowTheirParentsRotation()
    {
        ValkyrieAnimation animation = new ValkyrieAnimation();
        ValkyrieAnimation.Part core = animation.getCore();
        core.getRotation().set(0F, 90F, 0F);
        core.getPreviousRotation().set(0F, 90F, 0F);
        CapturedJoint[] joints = render(animation);
        // The mid front joint is 5 units behind the core along X. A quarter
        // turn about Y carries that onto +Z.
        Vector3f corePosition = joints[CORE].pose.transformPosition(new Vector3f());
        Vector3f child = joints[MID_FRONT].pose.transformPosition(new Vector3f());
        assertTrue(child.sub(corePosition).equals(new Vector3f(0F, 0F, 5F / 16F), 1E-5F), "offset " + child);
    }

    private static CapturedJoint[] render(ValkyrieAnimation animation)
    {
        ModelPlane model = new ModelPlane();
        model.oldRotateOrder = true;
        CapturedJoint[] joints = new CapturedJoint[ValkyrieAnimation.PART_COUNT];
        model.valkyrie = new ModelRendererTurbo[ValkyrieAnimation.PART_COUNT][];
        for (int i = 0; i < joints.length; i++)
        {
            joints[i] = new CapturedJoint(model);
            model.valkyrie[i] = new ModelRendererTurbo[] {joints[i]};
        }
        PoseStack stack = new PoseStack();
        model.renderValkyriePart(null, ModelDriveable.RenderState.ITEM, animation.getCore(), null, stack, null,
            0, 0, 1F, 1F, 1F, 1F, 1F, EnumRenderPass.values()[0]);
        assertTrue(stack.clear(), "The skeleton must leave the pose stack balanced");
        return joints;
    }

    private static final class CapturedJoint extends ModelRendererTurbo
    {
        private Matrix4f pose;
        private boolean oldOrder;

        private CapturedJoint(ModelPlane model)
        {
            super(model, 0, 0);
        }

        @Override
        public void render(PoseStack stack, VertexConsumer vertices, int light, int overlay,
                           float red, float green, float blue, float alpha, float scale,
                           EnumRenderPass renderPass, boolean oldRotateOrder)
        {
            pose = new Matrix4f(stack.last().pose());
            oldOrder = oldRotateOrder;
        }
    }
}
