package com.flansmod.client.tmt;

import com.flansmod.client.model.ModelDefaultMuzzleFlash;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModelMuzzleFlashRenderingTest
{
    @Test
    void defaultFlashSubmitsItsGlowingGeometryThroughTheSharedFlashEntryPoint()
    {
        ModelDefaultMuzzleFlash model = new ModelDefaultMuzzleFlash();
        RecordingVertexConsumer expected = new RecordingVertexConsumer();
        model.renderToBuffer(new PoseStack(), expected, 17, 23, 1, 1, 1, 1, EnumRenderPass.GLOW_ALPHA);
        assertFalse(expected.vertices.isEmpty());

        RecordingVertexConsumer actual = new RecordingVertexConsumer();
        model.renderToBuffer(new PoseStack(), actual, 17, 23, 1, 1, 1, 1);
        assertEquals(expected.vertices.size(), actual.vertices.size());
    }

    @Test
    void customFlashIncludesNormalAndGlowingPartsExactlyOnce()
    {
        ModelMuzzleFlash model = new ModelMuzzleFlash()
        {
            @Override
            public ResourceLocation getTexture()
            {
                return null;
            }
        };
        for (int index = 0; index < 4; index++)
        {
            ModelRendererTurbo part = new ModelRendererTurbo(model, 0, 0);
            part.addBox(index * 2, 0, 0, 1, 1, 1);
            part.glow = index == 1;
            part.glowAdditive = index == 2;
            part.glowNoDepthWrite = index == 3;
        }

        RecordingVertexConsumer vertices = new RecordingVertexConsumer();
        model.renderToBuffer(new PoseStack(), vertices, 17, 23, 1, 1, 1, 1);
        assertEquals(4 * 24, vertices.vertices.size());
    }
}
