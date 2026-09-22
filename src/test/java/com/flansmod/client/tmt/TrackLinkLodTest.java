package com.flansmod.client.tmt;

import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.model.TrackLinkLod;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TrackLinkLodTest
{
    @Test
    void longitudinalEnvelopeDoesNotOverlapTheNextStraightLink()
    {
        var parts = link();
        TrackLinkLod lod = TrackLinkLod.create(parts, false, 3.8F);
        assertNotNull(lod.parts());
        float[] b = bounds(record(lod.parts(), false));
        assertEquals(3.8F / 16F, b[3] - b[0], 1E-6F);
        assertTrue(b[3] <= b[0] + 3.8F / 16F + 1E-6F);
        assertTrue(lod.matches(parts, false, 3.8F));
        assertFalse(lod.matches(parts, false, 4F));
        assertNull(TrackLinkLod.create(parts, false, 0).parts());
    }

    @Test
    void longOffsetTrackPathsDelaySimplification()
    {
        var model = new ModelVehicle();
        model.fancyTrackModel = link();
        var compact = new TrackType();
        compact.getLeftTrackPoints().add(new Vector3f(0, 0, 0));
        compact.getLeftTrackPoints().add(new Vector3f(16, 0, 0));
        assertTrue(model.selectTrackLinkLod(compact, 500, 150, 1, 8, false));
        var extended = new TrackType();
        extended.getLeftTrackPoints().add(new Vector3f(0, 0, 0));
        extended.getLeftTrackPoints().add(new Vector3f(2048, 0, 0));
        assertFalse(model.selectTrackLinkLod(extended, 500, 150, 1, 8, false));
    }

    @Test
    void collapsesPartsTo24VerticesKeepingBoundsTextureCoordinatesAndLighting()
    {
        ModelRendererTurbo[] parts = link();
        TrackLinkLod lod = TrackLinkLod.create(parts, false);
        assertNotNull(lod.parts());
        var original = record(parts, false);
        var simplified = record(lod.parts(), false);
        assertEquals(96, original.vertices.size());
        assertEquals(24, simplified.vertices.size());
        assertArrayEquals(bounds(original), bounds(simplified), 1E-6F);
        for (float[] vertex : simplified.vertices)
        {
            assertTrue(original.vertices.stream().anyMatch(v -> v[7] == vertex[7] && v[8] == vertex[8]));
            assertArrayEquals(new float[]{0.2F, 0.4F, 0.6F, 0.8F}, java.util.Arrays.copyOfRange(vertex, 3, 7));
            assertEquals(23, vertex[9]);
            assertEquals(17, vertex[10]);
        }
        // Check actual triangle winding against emitted normals for every face.
        for (int i = 0; i < 24; i += 4)
        {
            float[] a = simplified.vertices.get(i), b = simplified.vertices.get(i + 1), c = simplified.vertices.get(i + 2);
            float x = (b[1]-a[1])*(c[2]-a[2]) - (b[2]-a[2])*(c[1]-a[1]);
            float y = (b[2]-a[2])*(c[0]-a[0]) - (b[0]-a[0])*(c[2]-a[2]);
            float z = (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0]);
            assertTrue(x*a[11] + y*a[12] + z*a[13] > 0);
        }
    }

    @Test
    void observesLegacyRotationOrderAndPublicPoseChanges()
    {
        ModelRendererTurbo[] parts = link();
        for (ModelRendererTurbo p : parts)
        {
            p.rotateAngleX = (float)Math.PI / 2;
            p.rotateAngleY = (float)Math.PI / 2;
            p.rotationPointX = 3;
            p.offsetZ = 0.25F;
        }
        for (boolean oldOrder : new boolean[]{false, true})
        {
            TrackLinkLod lod = TrackLinkLod.create(parts, oldOrder);
            assertNotNull(lod.parts());
            assertArrayEquals(bounds(record(parts, oldOrder)), bounds(record(lod.parts(), oldOrder)), 1E-6F);
            assertFalse(lod.matches(parts, !oldOrder));
            parts[0].offsetX += 0.1F;
            assertFalse(lod.matches(parts, oldOrder));
        }
    }

    @Test
    void invalidatesAfterGeometryTextureAndVisibilityChanges()
    {
        ModelRendererTurbo[] parts = link();
        TrackLinkLod lod = TrackLinkLod.create(parts, false);
        assertTrue(lod.matches(parts, false));
        parts[0].doMirror(true, false, false);
        assertFalse(lod.matches(parts, false));
        lod = TrackLinkLod.create(parts, false);
        assertTrue(parts[0].applyActualTextureSize(128, 32));
        assertFalse(lod.matches(parts, false));
        lod = TrackLinkLod.create(parts, false);
        parts[0].addBox(2, 3, 4, 1, 1, 1);
        assertFalse(lod.matches(parts, false));
        lod = TrackLinkLod.create(parts, false);
        parts[0].isHidden = true;
        assertFalse(lod.matches(parts, false));
        TrackLinkLod hidden = TrackLinkLod.create(parts, false);
        assertNull(hidden.parts());
        assertTrue(hidden.matches(parts, false));
        parts[0].isHidden = false;
        assertFalse(hidden.matches(parts, false));
    }

    @Test
    void fallsBackForGlowBonesChildrenAndMutableNormalLists()
    {
        ModelRendererTurbo[] parts = link();
        parts[0].glow = true;
        assertNull(TrackLinkLod.create(parts, false).parts());
        parts[0].glow = false;
        parts[0].childModels.add(parts[1]);
        assertNull(TrackLinkLod.create(parts, false).parts());
        parts[0].childModels.clear();
        var normals = new ArrayList<Vec3>();
        parts[0].getTextureGroup().poly.get(0).setNormals(normals);
        TrackLinkLod lod = TrackLinkLod.create(parts, false);
        normals.add(new Vec3(0, 1, 0));
        assertFalse(lod.matches(parts, false));
        assertNull(TrackLinkLod.create(parts, false).parts());
        normals.clear();
        var vertices = new PositionTextureVertex[]{new PositionTransformVertex(0, 0, 0, 0, 0),
            new PositionTransformVertex(1, 0, 0, 1, 0), new PositionTransformVertex(0, 1, 0, 0, 1)};
        parts[0].copyTo(vertices, new TexturedPolygon[]{new TexturedPolygon(vertices)});
        assertNull(TrackLinkLod.create(parts, false).parts());
    }

    @Test
    void selectionHasSizeHysteresisAndExplicitOff()
    {
        ModelRendererTurbo[] parts = new ModelRendererTurbo[2];
        for (int i = 0; i < 2; i++)
        {
            parts[i] = new ModelRendererTurbo(new ModelBase() {}, 0, 0);
            parts[i].addBox(0, 0, 0, 16, 16, 16);
        }
        TrackLinkLod lod = TrackLinkLod.create(parts, false);
        assertTrue(lod.select(500, 150, 1, 8, false));
        assertFalse(lod.select(500, 100, 1, 8, false));
        assertTrue(lod.select(500, 100, 1, 8, true));
        assertFalse(lod.select(500, 80, 1, 8, true));
        assertFalse(lod.select(500, 150, 2, 8, false));
        assertFalse(lod.select(500, 150, 1, 0, true));
        assertFalse(lod.select(1, 20, 1, 8, false));
        assertFalse(lod.select(Float.NaN, 150, 1, 8, true));
    }

    private static ModelRendererTurbo[] link()
    {
        ModelRendererTurbo[] parts = new ModelRendererTurbo[4];
        for (int i = 0; i < parts.length; i++)
        {
            parts[i] = new ModelRendererTurbo(new ModelBase() {}, i * 4, i * 3);
            parts[i].addBox(-2, -1, -6 + i * 2, 4, 2, 6);
        }
        return parts;
    }

    private static final class TrackType extends com.flansmodultimate.common.types.VehicleType
    {
        TrackType() { trackLinkLength = 3.8F; }
    }

    private static RecordingVertexConsumer record(ModelRendererTurbo[] parts, boolean oldOrder)
    {
        var result = new RecordingVertexConsumer();
        for (ModelRendererTurbo part : parts)
            part.render(new PoseStack(), result, 17, 23, 0.2F, 0.4F, 0.6F, 0.8F, 1, EnumRenderPass.DEFAULT, oldOrder);
        return result;
    }

    private static float[] bounds(RecordingVertexConsumer vertices)
    {
        float[] b = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (float[] v : vertices.vertices)
            for (int i = 0; i < 3; i++)
            {
                b[i] = Math.min(b[i], v[i]);
                b[i + 3] = Math.max(b[i + 3], v[i]);
            }
        return b;
    }
}
