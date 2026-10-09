package com.flansmodultimate.client.render.thermal;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Writes plain quads into the thermal heat mask, whose buffer uses the full entity vertex format. The mask
 * colours every vertex hot itself, so only positions matter. Both windings are written so no quad is culled.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeatMaskQuads
{
    public static void quad(VertexConsumer mask, Matrix4f matrix, Vector3f a, Vector3f b, Vector3f c, Vector3f d)
    {
        vertex(mask, matrix, a);
        vertex(mask, matrix, b);
        vertex(mask, matrix, c);
        vertex(mask, matrix, d);
        vertex(mask, matrix, d);
        vertex(mask, matrix, c);
        vertex(mask, matrix, b);
        vertex(mask, matrix, a);
    }

    private static void vertex(VertexConsumer mask, Matrix4f matrix, Vector3f position)
    {
        mask.addVertex(matrix, position.x, position.y, position.z).setColor(255, 255, 255, 255).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0F,
            1F, 0F);
    }
}
