package com.flansmodultimate.client.render.gpu;

import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Nanoseconds per part through ModelDriveable.renderPart into a real RigidBatch, for static baked
 * parts sharing a parent pose with size culling on. Excludes OpenGL. Set FLANS_RENDER_BENCHMARK=true.
 */
@EnabledIfEnvironmentVariable(named = "FLANS_RENDER_BENCHMARK", matches = "true")
class PartSubmissionBenchmarkTest
{
    @Test
    void bakedPartArray()
    {
        ModelBase base = new ModelBase() {};
        ModelRendererTurbo[] hull = new ModelRendererTurbo[120];
        for (int i = 0; i < hull.length; i++)
        {
            hull[i] = new ModelRendererTurbo(base, 0, 0, 256, 256);
            hull[i].addBox(0, 0, 0, 2 + i % 7, 1 + i % 3, 3);
            hull[i].setRotationPoint(i % 11 - 5, i % 5, i % 13 - 6);
            if (i % 4 == 0) hull[i].rotateAngleY = 0.3F;
        }
        ModelVehicle model = new ModelVehicle();
        RigidBatch batch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH, GpuModelCache.PARTS_PER_BATCH * 8, 100000);
        Backend backend = new Backend();
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 12; round++)
        {
            int frames = 4000;
            long start = System.nanoTime();
            for (int frame = 0; frame < frames; frame++)
            {
                PoseStack stack = new PoseStack();
                stack.translate(0, -2, -20 - frame % 40);
                stack.mulPose(Axis.YP.rotationDegrees(frame % 360));
                ModelRendererTurbo.beginScreenSpaceCulling(0.75F, 540F);
                try
                {
                    batch.begin(backend);
                    model.renderPart(hull, stack, batch, 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT);
                    batch.end();
                }
                finally { ModelRendererTurbo.endScreenSpaceCulling(); }
            }
            long elapsed = System.nanoTime() - start;
            if (round >= 4) best = Math.min(best, elapsed);
        }
        System.out.printf("baked part array: %.1f ns per part%n", best / (4000.0 * hull.length));
    }

    /** Like the stress test: 54 vehicles of 15 model types, 1,100 parts each, so parts no longer fit in cache. */
    @Test
    void manyVehiclesOfSeveralModels()
    {
        ModelBase base = new ModelBase() {};
        int types = 15, partsPerModel = 1100, vehicles = 54;
        ModelRendererTurbo[][] models = new ModelRendererTurbo[types][];
        for (int t = 0; t < types; t++)
        {
            models[t] = new ModelRendererTurbo[partsPerModel];
            for (int i = 0; i < partsPerModel; i++)
            {
                ModelRendererTurbo part = new ModelRendererTurbo(base, 0, 0, 256, 256);
                part.addBox(0, 0, 0, 2 + i % 7, 1 + i % 3, 3);
                part.setRotationPoint(i % 11 - 5, i % 5, i % 13 - 6);
                if (i % 4 == 0) part.rotateAngleY = 0.3F;
                models[t][i] = part;
            }
        }
        ModelVehicle model = new ModelVehicle();
        RigidBatch batch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH, GpuModelCache.PARTS_PER_BATCH * 8, 100000);
        Backend backend = new Backend();
        long best = Long.MAX_VALUE;
        int frames = 60;
        for (int round = 0; round < 10; round++)
        {
            long start = System.nanoTime();
            for (int frame = 0; frame < frames; frame++)
                for (int v = 0; v < vehicles; v++)
                {
                    PoseStack stack = new PoseStack();
                    stack.translate(v % 9 * 6 - 24, -2, -15 - v / 9 * 8);
                    stack.mulPose(Axis.YP.rotationDegrees(v * 23 + frame));
                    ModelRendererTurbo.beginScreenSpaceCulling(0.75F, 540F);
                    try
                    {
                        batch.begin(backend);
                        model.renderPart(models[v % types], stack, batch, 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT);
                        batch.end();
                    }
                    finally { ModelRendererTurbo.endScreenSpaceCulling(); }
                }
            long elapsed = System.nanoTime() - start;
            if (round >= 3) best = Math.min(best, elapsed);
        }
        System.out.printf("many vehicles: %.1f ns per part, %.2f ms per frame%n",
            best / ((double)frames * vehicles * partsPerModel), best / 1E6 / frames);
    }

    /** Track links: a few parts per link, each link under its own pushed pose, 160 links per vehicle. */
    @Test
    void trackLinks()
    {
        ModelBase base = new ModelBase() {};
        int types = 15, vehicles = 54, links = 160;
        ModelRendererTurbo[][] linkModels = new ModelRendererTurbo[types][];
        for (int t = 0; t < types; t++)
        {
            linkModels[t] = new ModelRendererTurbo[4];
            for (int i = 0; i < 4; i++)
            {
                ModelRendererTurbo part = new ModelRendererTurbo(base, 0, 0, 64, 64);
                part.addBox(0, 0, 0, 2, 1, 6);
                part.setRotationPoint(i * 2 - 3, 0, 0);
                linkModels[t][i] = part;
            }
        }
        ModelVehicle model = new ModelVehicle();
        RigidBatch batch = new RigidBatch(GpuModelCache.PARTS_PER_BATCH, GpuModelCache.PARTS_PER_BATCH * 8, 100000);
        Backend backend = new Backend();
        long best = Long.MAX_VALUE;
        int frames = 60;
        for (int round = 0; round < 10; round++)
        {
            long start = System.nanoTime();
            for (int frame = 0; frame < frames; frame++)
                for (int v = 0; v < vehicles; v++)
                {
                    PoseStack stack = new PoseStack();
                    stack.translate(v % 9 * 6 - 24, -2, -15 - v / 9 * 8);
                    ModelRendererTurbo.beginScreenSpaceCulling(0.75F, 540F);
                    try
                    {
                        batch.begin(backend);
                        for (int link = 0; link < links; link++)
                        {
                            stack.pushPose();
                            stack.translate(link * 0.1, Math.sin(link * 0.2), 0);
                            stack.mulPose(Axis.ZP.rotationDegrees(link * 2.25F));
                            model.renderPart(linkModels[v % types], stack, batch, 15728880, 0, 1, 1, 1, 1, 1, EnumRenderPass.DEFAULT);
                            stack.popPose();
                        }
                        batch.end();
                    }
                    finally { ModelRendererTurbo.endScreenSpaceCulling(); }
                }
            long elapsed = System.nanoTime() - start;
            if (round >= 3) best = Math.min(best, elapsed);
        }
        System.out.printf("track links: %.1f ns per part, %.2f ms per frame%n",
            best / ((double)frames * vehicles * links * 4), best / 1E6 / frames);
    }

    private static final class Backend implements RigidBatch.Backend
    {
        @Override public boolean draw(RigidBatch batch, boolean flushPending) { return true; }
        @Override public VertexConsumer fallback() { throw new AssertionError(); }
        @Override public void flushFallback() {}
        @Override public void failed(RuntimeException exception) { throw new AssertionError(exception); }
    }
}
