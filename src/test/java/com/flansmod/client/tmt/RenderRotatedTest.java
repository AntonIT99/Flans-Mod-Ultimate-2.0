package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.gpu.RigidGeometry;
import com.flansmodultimate.client.render.gpu.RigidGeometryConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The direct animated path must draw what setting each part's angles around its render draws. */
class RenderRotatedTest
{
    private static ModelRendererTurbo[] wheel()
    {
        ModelBase base = new ModelBase()
        {};
        ModelRendererTurbo[] parts = new ModelRendererTurbo[7];
        for (int i = 0; i < parts.length; i++)
        {
            ModelRendererTurbo part = new ModelRendererTurbo(base, 0, 0, 64, 64);
            part.addBox(-2, -2, -1 + i, 4, 4, 1);
            if (i % 3 != 2)
                part.setRotationPoint(5, -3, 2 + i % 2); // Two shared pivots and a pivotless part
            if (i == 1)
            {
                part.offsetX = 0.25F;
                part.offsetZ = -0.5F;
            }
            parts[i] = part;
        }
        parts[4].glow = true; // Glow parts draw in their own pass
        ModelRendererTurbo child = new ModelRendererTurbo(base, 0, 0, 64, 64);
        child.addBox(0, 0, 0, 1, 1, 1);
        parts[5].addChild(child); // Parents keep the stack path
        return parts;
    }

    @Test
    void directAnimatedPartsDrawWhatSettingTheirAnglesDraws()
    {
        float[][] angles = {{0, 0, 0}, {0, 0.4F, 1.7F}, {0, -0.3F, -2.9F}, {0.6F, 0, 0}, {0, 0.4F, 1.7F}};
        for (boolean oldRotateOrder : new boolean[]{false, true})
            for (float scale : new float[]{1F, 0.75F})
            {
                ModelRendererTurbo[] direct = wheel(), legacy = wheel();
                for (int frame = 0; frame < 40; frame++)
                {
                    float[] angle = angles[frame % angles.length]; // Different vehicles, different spin
                    PoseStack parent = new PoseStack();
                    parent.translate(0.5, -1, -3 - frame * 4);
                    parent.mulPose(Axis.YP.rotationDegrees(frame * 17));
                    Sink actual = new Sink(), expected = new Sink();
                    cull(() -> ModelRendererTurbo.renderRotated(direct, angle[0], angle[1], angle[2], parent, actual, 15728880, 0, 1, 1, 1, 1, scale, EnumRenderPass.DEFAULT, oldRotateOrder));
                    cull(() ->
                    {
                        for (ModelRendererTurbo part : legacy)
                        {
                            float x = part.rotateAngleX, y = part.rotateAngleY, z = part.rotateAngleZ;
                            part.rotateAngleX = angle[0];
                            part.rotateAngleY = angle[1];
                            part.rotateAngleZ = angle[2];
                            part.render(parent, expected, 15728880, 0, 1, 1, 1, 1, scale, EnumRenderPass.DEFAULT, oldRotateOrder);
                            part.rotateAngleX = x;
                            part.rotateAngleY = y;
                            part.rotateAngleZ = z;
                        }
                    });
                    String where = "frame " + frame + ", old order " + oldRotateOrder + ", scale " + scale;
                    assertEquals(expected.vertices.size(), actual.vertices.size(), where);
                    for (int i = 0; i < expected.vertices.size(); i++)
                        assertArrayEquals(expected.vertices.get(i), actual.vertices.get(i), 5E-5F, where + ", vertex " + i);
                    for (ModelRendererTurbo part : direct)
                        assertEquals(0F, part.rotateAngleY + part.rotateAngleZ, "the parts' own angles are untouched");
                }
            }
    }

    private static void cull(Runnable render)
    {
        ModelRendererTurbo.beginScreenSpaceCulling(2F, 300F);
        try
        {
            render.run();
        }
        finally
        {
            ModelRendererTurbo.endScreenSpaceCulling();
        }
    }

    /** Expands every visible submission to vertices, so differently batched paths compare by what they draw. */
    private static final class Sink implements RigidGeometryConsumer
    {
        final List<float[]> vertices = new ArrayList<>();
        private final VertexConsumer recorder = new VertexConsumer()
        {
            // Minecraft 1.21 writes each vertex as a chain; the normal ends it.
            private final float[] pending = new float[8];
            @Override
            public VertexConsumer addVertex(float x, float y, float z)
            {
                pending[0] = x;
                pending[1] = y;
                pending[2] = z;
                return this;
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a)
            {
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v)
            {
                pending[3] = u;
                pending[4] = v;
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v)
            {
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v)
            {
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z)
            {
                pending[5] = x;
                pending[6] = y;
                pending[7] = z;
                vertices.add(pending.clone());
                return this;
            }
        };

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha)
        {
            geometry.draw(pose, recorder, light, overlay, red, green, blue, alpha);
        }

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
        {
            if (visible)
                geometry.draw(pose, recorder, light, overlay, red, green, blue, alpha);
        }

        @Override
        public void submitComposed(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
        {
            if (visible)
                geometry.draw(pose, recorder, light, overlay, red, green, blue, alpha);
        }
        // Minecraft 1.21 writes each vertex as a chain; the normal ends it.
        private final float[] pending = new float[8];
        @Override
        public VertexConsumer addVertex(float x, float y, float z)
        {
            pending[0] = x;
            pending[1] = y;
            pending[2] = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a)
        {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v)
        {
            pending[3] = u;
            pending[4] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v)
        {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v)
        {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z)
        {
            pending[5] = x;
            pending[6] = y;
            pending[7] = z;
            vertices.add(pending.clone());
            return this;
        }
    }
}
