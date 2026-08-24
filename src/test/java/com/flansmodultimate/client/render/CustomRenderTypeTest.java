package com.flansmodultimate.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomRenderTypeTest
{
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("flansmodultimate", "test/glow");

    @Test
    void additivePassPreservesLegacyBlendAndShading()
    {
        RenderType renderType = CustomRenderType.entityEmissiveAdditive(TEXTURE);

        assertEquals(Optional.of(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE,
            SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)),
            renderType.pipeline().getColorTargetState().blendFunction());
        assertTrue(renderType.sortOnUpload());
        assertTrue(renderType.pipeline().getShaderDefines().flags().contains("EMISSIVE"));
        assertTrue(renderType.pipeline().getShaderDefines().flags().contains("NO_CARDINAL_LIGHTING"));
        assertTrue(renderType.pipeline().getShaderDefines().flags().contains("NO_OVERLAY"));
        assertEquals("0.1", renderType.pipeline().getShaderDefines().values().get("ALPHA_CUTOUT"));
        assertFalse(renderType.pipeline().getShaderDefines().flags().contains("PER_FACE_LIGHTING"));
    }

    @Test
    void emissiveDepthVariantsRetainTheirWriteMasks()
    {
        RenderType alpha = CustomRenderType.entityEmissiveAlpha(TEXTURE);
        assertEquals(Optional.of(BlendFunction.TRANSLUCENT),
            alpha.pipeline().getColorTargetState().blendFunction());
        assertTrue(alpha.sortOnUpload());
        assertTrue(alpha.pipeline().getDepthStencilState().writeDepth());
        assertFalse(CustomRenderType.entityEmissiveAlphaNoDepthWrite(TEXTURE).pipeline().getDepthStencilState().writeDepth());
    }
}
