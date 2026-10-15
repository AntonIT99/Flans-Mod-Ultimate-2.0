package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.gpu.RigidGeometry;
import com.flansmodultimate.client.render.gpu.RigidGeometryConsumer;
import com.flansmodultimate.platform.render.TestVertexSink;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnboundShapeVerticesTest
{
    private static ModelRendererTurbo wheel()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase()
        {}, 0, 0, 64, 64);
        part.addShape3D(0, 0, 0, new Shape2D(new Coord2D[]{new Coord2D(0, 0), new Coord2D(8, 0), new Coord2D(0, 4)}), 2, 8, 4, 8, 2, ModelRendererTurbo.MR_FRONT);
        part.setRotationPoint(3, 1, 2);
        return part;
    }

    @Test
    void shapeVerticesWithoutABoneUseTheGpuCacheWithTheirCpuPositions()
    {
        ModelRendererTurbo part = wheel();
        Sink gpu = new Sink();
        PoseStack pose = new PoseStack();
        pose.translate(1, 2, 3);
        part.render(pose, gpu, 0, 0, 1, 1, 1, 1, 1);
        assertEquals(1, gpu.geometries.size(), "an addShape3D part without bones is rigid");
        assertTrue(gpu.cpu.isEmpty());

        // The cached geometry, drawn under the submitted pose, matches the CPU path's vertices.
        Sink cached = new Sink();
        gpu.geometries.get(0).draw(gpu.poses.get(0), cached, 0, 0, 1, 1, 1, 1);
        Sink cpu = new Sink();
        wheel().render(pose, new Plain(cpu), 0, 0, 1, 1, 1, 1, 1);
        assertEquals(cpu.cpu.size(), cached.cpu.size());
        for (int i = 0; i < cpu.cpu.size(); i++)
            assertArrayEquals(cpu.cpu.get(i), cached.cpu.get(i), 1E-5F);
    }

    @Test
    void handingOutTheGeometryOrBindingABoneKeepsTheCpuPath()
    {
        ModelRendererTurbo exposed = wheel();
        Sink first = new Sink();
        exposed.render(new PoseStack(), first, 0, 0, 1, 1, 1, 1, 1);
        assertEquals(1, first.geometries.size());
        // Outside code holding the polygons could bind bones to their vertices.
        for (TexturedPolygon face : exposed.getTextureGroup().poly)
            for (PositionTextureVertex vertex : face.vertexPositions)
                if (vertex instanceof PositionTransformVertex transform)
                    transform.addGroup(new TransformGroupBone(new Bone(0, 0, 0, 0), 1D));
        Sink after = new Sink();
        exposed.render(new PoseStack(), after, 0, 0, 1, 1, 1, 1, 1);
        assertTrue(after.geometries.isEmpty());
        assertFalse(after.cpu.isEmpty());
    }

    /** Records GPU submissions and CPU vertices. */
    private static final class Sink extends TestVertexSink implements RigidGeometryConsumer
    {
        final List<RigidGeometry> geometries = new ArrayList<>();
        final List<PoseStack.Pose> poses = new ArrayList<>();
        final List<float[]> cpu = new ArrayList<>();

        @Override
        public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha)
        {
            geometries.add(geometry);
            PoseStack.Pose copy = new PoseStack().last();
            copy.pose().set(pose.pose());
            copy.normal().set(pose.normal());
            poses.add(copy);
        }

        @Override
        protected void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
        {
            cpu.add(new float[]{x, y, z, u, v, normalX, normalY, normalZ});
        }
    }

    /** A plain consumer, so the part takes the CPU path. */
    private static final class Plain extends TestVertexSink
    {
        private final Sink sink;

        private Plain(Sink sink)
        {
            this.sink = sink;
        }

        @Override
        protected void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
        {
            sink.cpu.add(new float[]{x, y, z, u, v, normalX, normalY, normalZ});
        }
    }
}
