package com.flansmod.client.tmt;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.ModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class RenderStateLifecycleTest
{
    @Test
    void trackedListsPreserveSymmetricListEqualityAndHashCodes()
    {
        TextureGroup first = new TextureGroup(), second = new TextureGroup();
        TexturedPolygon polygon = new TexturedPolygon(new PositionTextureVertex[0]);
        first.poly.add(polygon);
        first.poly.add(null);
        second.poly.addAll(first.poly);
        assertListEquality(first.poly, second.poly);

        ModelRenderer firstParent = new ModelRenderer(new ModelBase() {});
        ModelRenderer secondParent = new ModelRenderer(new ModelBase() {});
        firstParent.childModels.add(new ModelRenderer(new ModelBase() {}));
        firstParent.childModels.add(null);
        secondParent.childModels.addAll(firstParent.childModels);
        assertListEquality(firstParent.childModels, secondParent.childModels);
    }

    private static <T> void assertListEquality(List<T> first, List<T> second)
    {
        List<T> ordinary = new ArrayList<>(first);
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first, ordinary);
        assertEquals(ordinary, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.hashCode(), ordinary.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, new Object());
        second.remove(0);
        assertNotEquals(first, second);
    }

    @Test
    void cleanupResetsCullingAndRenderingCanResumeWithIdenticalVertices() throws Exception
    {
        RecordingVertexConsumer before = drawPolygon();
        try
        {
            ModelRendererTurbo.beginFixedScaleCulling(100, 1);
            assertEquals(100F, cullingMinimum());
            ModelRendererTurbo.clearRenderScratch();
            assertEquals(0F, cullingMinimum());
            RecordingVertexConsumer after = drawPolygon();
            assertEquals(before.vertices.size(), after.vertices.size());
            for (int i = 0; i < before.vertices.size(); i++)
                assertArrayEquals(before.vertices.get(i), after.vertices.get(i));
        }
        finally
        {
            ModelRendererTurbo.clearRenderScratch();
        }
    }

    @Test
    void cleanupOnAnotherThreadDoesNotResetTheCallingThreadsCulling() throws Exception
    {
        try
        {
            ModelRendererTurbo.beginFixedScaleCulling(100, 1);
            CompletableFuture.runAsync(() -> {
                ModelRendererTurbo.beginFixedScaleCulling(1, 100);
                ModelRendererTurbo.clearRenderScratch();
            }).join();
            assertEquals(100F, cullingMinimum());
        }
        finally
        {
            ModelRendererTurbo.clearRenderScratch();
        }
        assertEquals(0F, cullingMinimum());
        ModelRendererTurbo.clearRenderScratch();
    }

    private static float cullingMinimum() throws Exception
    {
        var method = ModelRendererTurbo.class.getDeclaredMethod("cullingState");
        method.setAccessible(true);
        Object state = method.invoke(null);
        var field = state.getClass().getDeclaredField("minimumPixelDiameter");
        field.setAccessible(true);
        return field.getFloat(state);
    }

    private static RecordingVertexConsumer drawPolygon()
    {
        RecordingVertexConsumer consumer = new RecordingVertexConsumer();
        TexturedPolygon polygon = new TexturedPolygon(new PositionTextureVertex[] {
            new PositionTextureVertex(0, 0, 0, 0, 0),
            new PositionTextureVertex(16, 0, 0, 1, 0),
            new PositionTextureVertex(0, 16, 0, 0, 1)
        });
        polygon.draw(new PoseStack().last(), consumer, 17, 23, 1, 1, 1, 1);
        return consumer;
    }
}
