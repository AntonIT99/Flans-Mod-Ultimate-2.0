package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** The per-array cache must submit exactly what rendering each part would, through every kind of change. */
class PartArrayCacheTest
{
    private static ModelRendererTurbo[] model()
    {
        ModelBase base = new ModelBase()
        {};
        ModelRendererTurbo[] parts = new ModelRendererTurbo[40];
        for (int i = 0; i < parts.length; i++)
        {
            ModelRendererTurbo part = new ModelRendererTurbo(base, 0, 0, 128, 128);
            part.addBox(0, 0, 0, 1 + i % 5, 1 + i % 3, 2);
            if (i % 9 != 4)
                part.setRotationPoint(i % 7 - 3, i % 4, i % 6 - 3); // A few untransformed parts
            if (i % 5 == 0)
                part.rotateAngleY = 0.4F;
            if (i == 11)
                part.glow = true;
            parts[i] = part;
        }
        parts[17] = null;
        return parts;
    }

    @Test
    void cachedArraysSubmitWhatPerPartRenderingDoes()
    {
        ModelRendererTurbo[] cached = model(), reference = model();
        Recorder cachedBackend = new Recorder(), referenceBackend = new Recorder();
        RigidBatch cachedBatch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH, 64, 100000);
        RigidBatch referenceBatch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH, 64, 100000);
        for (int frame = 0; frame < 60; frame++)
        {
            // Changes legacy code can make between frames, applied identically to both models.
            int f = frame;
            Consumer<ModelRendererTurbo[]> change = parts ->
            {
                if (f == 12)
                    parts[3].rotateAngleZ = 0.25F; // Animate a cached part for good
                if (f == 16)
                    parts[6].isHidden = true; // Hide one
                if (f == 20)
                    parts[6].isHidden = false;
                if (f == 24)
                {
                    parts[8].rotateAngleX = 0.5F;
                } // Re-posed elsewhere, then restored:
                if (f == 25)
                {
                    parts[8].rotateAngleX = 0F;
                } // its bake was dropped meanwhile
                if (f == 30)
                    parts[9].addBox(0, 2, 0, 1, 1, 1); // Geometry grows
                if (f == 36)
                {
                    parts[2] = new ModelRendererTurbo(new ModelBase()
                    {}, 0, 0, 128, 128);
                    parts[2].addBox(0, 0, 0, 3, 3, 3);
                }
                if (f == 44)
                    parts[14].glow = true;
                if (f == 48)
                    parts[13].setRotationPoint(1, 2, 3); // An untransformed part gains a pivot
                if (f == 52)
                {
                    ModelRendererTurbo child = new ModelRendererTurbo(new ModelBase()
                    {}, 0, 0, 128, 128);
                    child.addBox(0, 0, 0, 1, 1, 1);
                    parts[15].addChild(child);
                }
                if (f == 54)
                {
                    ModelRendererTurbo child = new ModelRendererTurbo(new ModelBase()
                    {}, 0, 0, 128, 128);
                    child.addBox(1, 1, 1, 1, 1, 1);
                    parts[16].childModels.add(child);
                } // Straight into the public list
            };
            change.accept(cached);
            change.accept(reference);
            if (f == 24) // Another path renders part 8 with its temporary angle, dropping its bake.
            {
                renderSingle(cached[8], cachedBatch, cachedBackend);
                renderSingle(reference[8], referenceBatch, referenceBackend);
            }
            PoseStack parent = new PoseStack();
            parent.translate(0.5, -1, -4 - frame * 2.5); // Crosses size-culling thresholds
            parent.mulPose(Axis.YP.rotationDegrees(frame * 13));
            cachedBackend.draws.clear();
            referenceBackend.draws.clear();
            render(cachedBatch, cachedBackend, () -> ModelRendererTurbo.renderArray(cached, parent, cachedBatch, 15728880, 0, 1, 0.9F, 0.8F, 1, 1, EnumRenderPass.DEFAULT, false));
            render(referenceBatch, referenceBackend, () ->
            {
                for (ModelRendererTurbo part : reference)
                    if (part != null)
                        part.render(parent, referenceBatch, 15728880, 0, 1, 0.9F, 0.8F, 1, 1, EnumRenderPass.DEFAULT, false);
            });
            if (frame == 10)
                assertTrue(fastParts(cached) >= 36, "the cache serves the settled parts, untransformed ones too: " + fastParts(cached));
            assertEquals(referenceBackend.draws.size(), cachedBackend.draws.size(), "draws in frame " + frame);
            for (int d = 0; d < referenceBackend.draws.size(); d++)
                referenceBackend.draws.get(d).assertSame(cachedBackend.draws.get(d), "frame " + frame + " draw " + d);
        }
    }

    /** Parts the array's cache draws without the per-part path. */
    private static int fastParts(ModelRendererTurbo[] parts)
    {
        try
        {
            var arrays = ModelRendererTurbo.class.getDeclaredField("PART_ARRAYS");
            arrays.setAccessible(true);
            Object cache = ((java.util.Map<?, ?>) arrays.get(null)).get(parts);
            var fast = cache.getClass().getDeclaredField("fast");
            fast.setAccessible(true);
            int count = 0;
            for (boolean value : (boolean[]) fast.get(cache))
                if (value)
                    count++;
            return count;
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError(exception);
        }
    }

    private static void renderSingle(ModelRendererTurbo part, RigidBatch batch, Recorder backend)
    {
        render(batch, backend, () -> part.render(new PoseStack(), batch, 0, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT, false));
    }

    private static void render(RigidBatch batch, Recorder backend, Runnable submit)
    {
        ModelRendererTurbo.beginScreenSpaceCulling(2F, 400F);
        try
        {
            batch.begin(backend);
            submit.run();
            batch.end();
        }
        finally
        {
            ModelRendererTurbo.endScreenSpaceCulling();
        }
    }

    private record Draw(int[] palette, int[] vertexCounts, int[] starts, int[] counts, int visibleVertices, float[] poses, float[] normals, float[] data)
    {
        void assertSame(Draw other, String where)
        {
            assertArrayEquals(palette, other.palette, where + " palette");
            assertArrayEquals(vertexCounts, other.vertexCounts, where + " geometry");
            assertArrayEquals(starts, other.starts, where + " range starts");
            assertArrayEquals(counts, other.counts, where + " range counts");
            assertEquals(visibleVertices, other.visibleVertices, where + " visible vertices");
            assertArrayEquals(poses, other.poses, where + " poses");
            assertArrayEquals(normals, other.normals, where + " normals");
            assertArrayEquals(data, other.data, where + " data");
        }
    }

    private static final class Recorder implements RigidBatch.Backend
    {
        final List<Draw> draws = new ArrayList<>();

        @Override
        public boolean draw(RigidBatch batch, boolean flushPending)
        {
            int n = batch.key.count;
            int[] vertices = new int[n];
            for (int i = 0; i < n; i++)
                vertices[i] = batch.key.geometries[i].vertexCount();
            draws.add(new Draw(Arrays.copyOf(batch.key.paletteIndices, n), vertices, Arrays.copyOf(batch.ranges.starts, batch.ranges.count), Arrays.copyOf(batch.ranges.counts, batch.ranges.count),
                batch.ranges.visibleVertices, batch.poses.clone(), batch.normals.clone(), batch.data.clone()));
            return true;
        }

        @Override
        public VertexConsumer fallback()
        {
            throw new AssertionError("glow and unsupported parts do not draw in this pass");
        }

        @Override
        public void flushFallback()
        {}

        @Override
        public void failed(RuntimeException exception)
        {
            throw new AssertionError(exception);
        }
    }
}
