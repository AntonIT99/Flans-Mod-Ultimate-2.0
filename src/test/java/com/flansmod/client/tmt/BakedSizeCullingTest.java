package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.gpu.RigidGeometry;
import com.flansmodultimate.client.render.gpu.RigidGeometryConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BakedSizeCullingTest
{
    private static ModelRendererTurbo box()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase()
        {}, 0, 0, 64, 64);
        part.addBox(-1, 0, -2, 2, 3, 4);
        part.setRotationPoint(5, -3, 2);
        part.rotateAngleY = 0.7F;
        part.rotateAngleX = -0.3F;
        return part;
    }

    private static ModelRendererTurbo shape()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase()
        {}, 0, 0, 64, 64);
        part.addShape3D(0, 0, 0, new Shape2D(new Coord2D[]{new Coord2D(0, 0), new Coord2D(8, 0), new Coord2D(0, 4)}), 2, 8, 4, 8, 2, ModelRendererTurbo.MR_FRONT);
        part.setRotationPoint(1, 2, 3);
        return part;
    }

    /**
     * The baked path sizes the part from its parent pose, bounding parent and local scale separately.
     * Under uniform scale that matches the composed pose's test exactly. Under nonuniform scale the
     * separate bounds are tighter than the bound of the product, yet still never below the true size,
     * so the baked path may only cull more, near the threshold.
     */
    @Test
    void bakedPartsCullWhereTheComposedPathDoes()
    {
        for (boolean uniform : new boolean[]{true, false})
        {
            ModelRendererTurbo baked = box(), composed = box();
            Sink gpu = new Sink();
            int culled = 0;
            // The 0.31-block part falls below 2 pixels at 500 projection pixels beyond about 80 blocks.
            for (int step = 0; step < 300; step++)
            {
                double distance = 2 + step;
                PoseStack parent = parent(distance, step, uniform);
                for (int warm = 0; warm < 3; warm++)
                    render(baked, parent, gpu); // Bakes after two stable renders
                Sink cpu = new Sink();
                render(composed, parent, new Plain(cpu));
                boolean bakedCulled = gpu.lastVisible == Boolean.FALSE, composedCulled = !cpu.vertices;
                if (uniform)
                    assertEquals(composedCulled, bakedCulled, "distance " + distance);
                else if (composedCulled)
                    assertTrue(bakedCulled, "distance " + distance);
                if (composedCulled)
                    culled++;
            }
            assertTrue(culled > 0 && culled < 300, "the sweep crosses the threshold: " + culled + " culled");
        }
    }

    @Test
    void shapePartsWithoutBonesAreSizeCulled()
    {
        ModelRendererTurbo part = shape();
        Sink gpu = new Sink();
        render(part, parent(2, 0, true), gpu);
        assertEquals(Boolean.TRUE, gpu.lastVisible);
        render(part, parent(500, 0, true), gpu);
        assertEquals(Boolean.FALSE, gpu.lastVisible, "a distant addShape3D part is below the size threshold");
    }

    private static PoseStack parent(double distance, int step, boolean uniform)
    {
        PoseStack parent = new PoseStack();
        parent.translate(0.3, -0.2, -distance);
        parent.mulPose(Axis.YP.rotationDegrees(step * 7));
        if (uniform)
            parent.scale(1.1F, 1.1F, 1.1F);
        else
            parent.scale(0.9F, 1.1F, 1F);
        return parent;
    }

    private static void render(ModelRendererTurbo part, PoseStack parent, VertexConsumer consumer)
    {
        ModelRendererTurbo.beginScreenSpaceCulling(2, 500);
        try
        {
            part.render(parent, consumer, 0, 0, 1, 1, 1, 1, 1);
        }
        finally
        {
            ModelRendererTurbo.endScreenSpaceCulling();
        }
    }

    private static class Sink implements RigidGeometryConsumer
    {
        Boolean lastVisible;
        boolean vertices;

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha)
        {
            lastVisible = true;
        }

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
        {
            lastVisible = visible;
        }

        @Override
        public void submitComposed(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha, boolean visible)
        {
            lastVisible = visible;
        }

        // Minecraft 1.21 writes each CPU vertex as a chain that starts at its position.
        @Override
        public VertexConsumer addVertex(float x, float y, float z)
        {
            vertices = true;
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
            return this;
        }
    }

    /** A plain consumer, so the part takes the composed CPU path. */
    private record Plain(Sink sink) implements VertexConsumer
    {
        // Minecraft 1.21 writes each CPU vertex as a chain that starts at its position.
        @Override
        public VertexConsumer addVertex(float x, float y, float z)
        {
            sink.vertices = true;
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
            return this;
        }
    }
}
