package com.flansmod.client.tmt;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TexturedPolygonRenderingTest
{
    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 7, 8, 9, 12})
    void pairedQuadsPreserveEveryFanTriangleAndWinding(int count)
    {
        PositionTextureVertex[] vertices = new PositionTextureVertex[count];
        for (int i = 0; i < count; i++)
            vertices[i] = new PositionTextureVertex(i * 16F, i * i, 0F, i, 0F);
        TexturedPolygon polygon = new TexturedPolygon(vertices);
        for (boolean flipped : new boolean[]{false, true})
        {
            if (flipped)
                polygon.flipFace();
            RecordingVertexConsumer output = draw(polygon);
            List<List<Integer>> actual = new ArrayList<>();
            for (int quad = 0; quad < output.vertices.size(); quad += 4)
            {
                int a = (int)output.vertices.get(quad)[7];
                int b = (int)output.vertices.get(quad + 1)[7];
                int c = (int)output.vertices.get(quad + 2)[7];
                int d = (int)output.vertices.get(quad + 3)[7];
                actual.add(List.of(a, b, c));
                if (c != d)
                    actual.add(List.of(a, c, d));
            }
            List<List<Integer>> expected = new ArrayList<>();
            for (int i = 1; i < count - 1; i++)
                expected.add(List.of((int)polygon.vertexPositions[0].texturePositionX,
                    (int)polygon.vertexPositions[i].texturePositionX,
                    (int)polygon.vertexPositions[i + 1].texturePositionX));
            assertEquals(expected, actual);
            assertEquals(((count - 1) / 2) * 4, output.vertices.size());
        }
    }

    @Test
    void rigidTransformVerticesFollowNeutralPositionUvAndLateBoneChanges()
    {
        PositionTransformVertex a = new PositionTransformVertex(0, 0, 0, 0, 0);
        PositionTransformVertex b = new PositionTransformVertex(16, 0, 0, 1, 0);
        PositionTransformVertex c = new PositionTransformVertex(0, 16, 0, 0, 1);
        TexturedPolygon polygon = new TexturedPolygon(new PositionTextureVertex[]{a, b, c});
        float[] initial = draw(polygon).vertices.get(2);
        assertArrayEquals(initial, draw(polygon).vertices.get(2));
        assertEquals(1F, initial[13]);

        c.neutralVector = new Vec3(0, 16, 16);
        c.texturePositionX = 0.25F;
        float[] changed = draw(polygon).vertices.get(2);
        assertEquals(1F, changed[2]);
        assertEquals(0.25F, changed[7]);
        assertEquals(-Math.sqrt(0.5), changed[12], 1E-6);
        assertEquals(Math.sqrt(0.5), changed[13], 1E-6);

        // Direct public-list mutation is supported, including removal after a warm cache.
        c.transformGroups.add(new TransformGroup()
        {
            @Override public double getWeight() { return 1D; }
            @Override public Vec3 doTransformation(PositionTransformVertex vertex)
            {
                return vertex.neutralVector.add(0, 0, 16);
            }
        });
        assertEquals(2F, draw(polygon).vertices.get(2)[2]);
        c.transformGroups.clear();
        assertArrayEquals(changed, draw(polygon).vertices.get(2));
        polygon.flipFace();
        assertEquals(-Math.sqrt(0.5), draw(polygon).vertices.get(0)[13], 1E-6);
    }

    @Test
    void incompleteNormalListsKeepLegacyFanSubmissionOrder()
    {
        PositionTextureVertex[] vertices = new PositionTextureVertex[8];
        for (int i = 0; i < vertices.length; i++)
            vertices[i] = new PositionTextureVertex(i, i * i, 0, i, 0);
        TexturedPolygon polygon = new TexturedPolygon(vertices);
        polygon.setNormals(List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)));
        RecordingVertexConsumer output = draw(polygon);
        assertEquals(24, output.vertices.size());
        for (int triangle = 0; triangle < 6; triangle++)
        {
            assertEquals(0F, output.vertices.get(triangle * 4)[7]);
            assertEquals(triangle + 1F, output.vertices.get(triangle * 4 + 1)[7]);
            assertEquals(triangle + 2F, output.vertices.get(triangle * 4 + 2)[7]);
            assertArrayEquals(output.vertices.get(triangle * 4 + 2), output.vertices.get(triangle * 4 + 3));
        }
    }

    @Test
    void completeVertexNormalsAndGlowSurviveCapPairing()
    {
        PositionTextureVertex[] vertices = new PositionTextureVertex[8];
        List<Vec3> normals = new ArrayList<>();
        for (int i = 0; i < vertices.length; i++)
        {
            vertices[i] = new PositionTextureVertex(i, i * i, 0, i, 0);
            normals.add(new Vec3(i, i + 1, i + 2));
        }
        TexturedPolygon polygon = new TexturedPolygon(vertices);
        polygon.setNormals(normals);
        for (float[] vertex : draw(polygon).vertices)
        {
            assertEquals(vertex[7], vertex[11]);
            assertEquals(vertex[7] + 1, vertex[12]);
            assertEquals(vertex[7] + 2, vertex[13]);
        }
        RecordingVertexConsumer glow = new RecordingVertexConsumer();
        polygon.draw(new PoseStack().last(), glow, 17, 23, 0.2F, 0.3F, 0.4F, 0.5F, true);
        for (float[] vertex : glow.vertices)
        {
            assertEquals(LightTexture.FULL_BRIGHT, vertex[10]);
            assertEquals(23, vertex[9]);
            assertEquals(0.5F, vertex[6], 1F / 255F); // 1.21 packs vertex tint into byte channels.
            assertArrayEquals(new float[]{0, 1, 0}, new float[]{vertex[11], vertex[12], vertex[13]});
        }
    }

    private static RecordingVertexConsumer draw(TexturedPolygon polygon)
    {
        RecordingVertexConsumer output = new RecordingVertexConsumer();
        polygon.draw(new PoseStack().last(), output, 17, 23, 1, 1, 1, 1);
        return output;
    }
}
