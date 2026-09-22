package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.gpu.RigidGeometry;
import com.flansmodultimate.client.render.gpu.RigidGeometryConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryInvalidationTest
{
    @Test
    void legacySubclassConstructionHooksStillRunAndStayOnCpu()
    {
        class CustomPart extends ModelRendererTurbo
        {
            int copies;
            CustomPart() { super(new ModelBase() {}, 0, 0); }
            @Override public void copyTo(PositionTextureVertex[] vertices, TexturedPolygon[] faces)
            {
                copies++;
                super.copyTo(vertices, faces);
            }
        }
        CustomPart part = new CustomPart();
        part.addBox(0, 0, 0, 1, 1, 1);
        assertEquals(1, part.copies);
        assertNull(draw(part).geometry);
        assertEquals(24, draw(part).vertices.size());
    }

    @Test
    void growingConstructionBuffersKeepCompactRenderedGeometryAndMirroring()
    {
        ModelRendererTurbo part = box();
        for (int i = 1; i < 100; i++) part.addBox(i * 16, 0, 0, 16, 8, 4);
        double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        assertTrue(part.appendVertexBounds(bounds));
        assertEquals(1600, bounds[3]);
        part.doMirror(true, false, false);
        Capture output = draw(part);
        assertEquals(2400, output.vertices.size());
        assertEquals(2400, output.geometry.vertexCount());
        part.addBox(0, 0, 0, 1, 1, 1); // Grow again after compaction.
        assertEquals(2424, draw(part).vertices.size());
    }

    @Test
    void ownedGeometryStaysCachedAcrossOtherPartsMutations()
    {
        ModelRendererTurbo part = box();
        RigidGeometry initial = draw(part).geometry;
        assertNotNull(initial);
        assertSame(initial, draw(part).geometry);
        box().doMirror(true, false, false); // Global mutation hint must not invalidate unrelated meshes.
        assertSame(initial, draw(part).geometry);
        part.doMirror(true, false, false);
        assertNotSame(initial, draw(part).geometry);
        RigidGeometry mirrored = draw(part).geometry;
        part.addBox(1, 2, 3, 2, 2, 2);
        assertNotSame(mirrored, draw(part).geometry);
    }

    @Test
    void sameSizeMembershipOrderReplacementAndRemovalRebuildFaces()
    {
        ModelRendererTurbo part = box();
        TextureGroup group = part.getTextureGroup();
        Capture before = draw(part);
        Collections.swap(group.poly, 0, 1);
        Capture reordered = draw(part);
        assertNotSame(before.geometry, reordered.geometry);
        assertArrayEquals(before.vertices.get(4), reordered.vertices.get(0));
        List<TexturedPolygon> replacement = new ArrayList<>(group.poly);
        replacement.set(0, triangle(80));
        group.poly = replacement; // A completely unobservable public field assignment.
        Capture replaced = draw(part);
        assertNotSame(reordered.geometry, replaced.geometry);
        assertEquals(5, replaced.vertices.get(0)[0]);
        replacement.set(0, triangle(160)); // Same-sized mutation of that arbitrary list.
        assertEquals(10, draw(part).vertices.get(0)[0]);
        group.poly.subList(0, 2).clear();
        assertEquals(16, draw(part).vertices.size());
        group.poly.clear();
        assertTrue(draw(part).vertices.isEmpty());
    }

    @Test
    void textureGroupsPreserveOrderAndClearDoesNotResurrectOldPolygons()
    {
        ModelRendererTurbo part = box();
        part.setTextureGroup("extra");
        part.addBox(20, 0, 0, 1, 1, 1);
        List<float[]> expected = new ArrayList<>();
        for (TextureGroup group : part.getTextureGroups())
            for (TexturedPolygon face : group.poly)
            {
                RecordingVertexConsumer vertices = new RecordingVertexConsumer();
                face.draw(new PoseStack().last(), vertices, 17, 23, 1, 1, 1, 1);
                expected.addAll(vertices.vertices);
            }
        Capture actual = draw(part);
        for (int i = 0; i < expected.size(); i++) assertArrayEquals(expected.get(i), actual.vertices.get(i));
        part.clear();
        assertTrue(draw(part).vertices.isEmpty());
        part.addBox(0, 0, 0, 1, 1, 1);
        assertEquals(24, draw(part).vertices.size());
    }

    @Test
    void externalVertexUvFaceNormalAndArrayMutationsInvalidateGpuAndCpuData()
    {
        ModelRendererTurbo part = box();
        TexturedPolygon face = part.getTextureGroup().poly.get(0);
        Capture before = draw(part);
        face.vertexPositions[0].texturePositionX = 0.375F;
        Capture uv = draw(part);
        assertNotSame(before.geometry, uv.geometry);
        assertEquals(0.375F, uv.vertices.get(0)[7]);
        face.vertexPositions[0].vector3D = new Vec3(160, 0, 0);
        Capture moved = draw(part);
        assertNotSame(uv.geometry, moved.geometry);
        assertEquals(10, moved.vertices.get(0)[0]);
        face.setNormals(1, 0, 0);
        Capture normals = draw(part);
        assertNotSame(moved.geometry, normals.geometry);
        assertEquals(1, normals.vertices.get(0)[11]);
        face.flipFace();
        assertNotSame(normals.geometry, draw(part).geometry);
        face.vertexPositions = triangle(32).vertexPositions;
        face.nVertices = 3;
        assertEquals(24, draw(part).vertices.size());
        face.vertexPositions[0] = new PositionTransformVertex(32, 0, 0, 0, 0);
        assertNull(draw(part).geometry); // Never freeze a transform vertex added after warm-up.
        face.setInvertNormal(true);
        assertNull(draw(part).geometry);
    }

    @Test
    void retainedCopyToAndNormalsListsStayMutable()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        TexturedPolygon face = triangle(0);
        part.copyTo(face.vertexPositions, new TexturedPolygon[]{face});
        assertNotNull(draw(part).geometry);
        face.vertexPositions[0].vector3D = new Vec3(48, 0, 0);
        assertEquals(3, draw(part).vertices.get(0)[0]);
        List<Vec3> normals = new ArrayList<>();
        face.setNormals(normals);
        assertNull(draw(part).geometry);
        normals.add(new Vec3(0, 1, 0));
        assertEquals(1, draw(part).vertices.get(0)[12]);
    }

    @Test
    void uvRescalingAndLegacyFlagsKeepExpectedPaths()
    {
        ModelRendererTurbo part = box();
        Capture before = draw(part);
        assertTrue(part.applyActualTextureSize(128, 32));
        Capture after = draw(part);
        assertNotSame(before.geometry, after.geometry);
        for (int i = 0; i < before.vertices.size(); i++) assertEquals(before.vertices.get(i)[7] * 0.5F, after.vertices.get(i)[7]);
        part.forcedRecompile = true;
        assertNull(draw(part).geometry);
        part.forcedRecompile = false;
        part.useLegacyCompiler = true;
        assertNull(draw(part).geometry);
        part.useLegacyCompiler = false;
        for (EnumRenderPass pass : new EnumRenderPass[]{EnumRenderPass.GLOW_ALPHA, EnumRenderPass.GLOW_ADDITIVE, EnumRenderPass.GLOW_ALPHA_NO_DEPTH_WRITE})
        {
            part.glow = pass == EnumRenderPass.GLOW_ALPHA;
            part.glowAdditive = pass == EnumRenderPass.GLOW_ADDITIVE;
            part.glowNoDepthWrite = pass == EnumRenderPass.GLOW_ALPHA_NO_DEPTH_WRITE;
            Capture output = new Capture();
            part.render(new PoseStack(), output, 17, 23, 1, 1, 1, 1, 1, pass);
            assertNull(output.geometry);
            assertFalse(output.vertices.isEmpty());
        }
    }

    private static TexturedPolygon triangle(float x)
    {
        return new TexturedPolygon(new PositionTextureVertex[]{new PositionTextureVertex(x, 0, 0, 0, 0),
            new PositionTextureVertex(x + 16, 0, 0, 1, 0), new PositionTextureVertex(x, 16, 0, 0, 1)});
    }
    private static ModelRendererTurbo box()
    {
        ModelRendererTurbo part = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
        part.addBox(0, 0, 0, 16, 8, 4);
        return part;
    }
    private static Capture draw(ModelRendererTurbo part)
    {
        Capture output = new Capture();
        part.render(new PoseStack(), output, 17, 23, 1, 1, 1, 1, 1);
        return output;
    }
    private static class Capture extends RecordingVertexConsumer implements RigidGeometryConsumer
    {
        RigidGeometry geometry;
        @Override public void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float r, float g, float b, float a)
        {
            this.geometry = geometry;
            geometry.draw(pose, this, light, overlay, r, g, b, a);
        }
    }
}
