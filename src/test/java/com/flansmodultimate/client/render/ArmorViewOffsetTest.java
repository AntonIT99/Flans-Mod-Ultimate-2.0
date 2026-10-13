package com.flansmodultimate.client.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Armor must end up nearer to the viewer than the body it is flush with, in the world and in GUI entity previews. */
class ArmorViewOffsetTest
{
    /** One step of a 24-bit depth buffer, in normalized device depth */
    private static final float DEPTH_STEP = 2F / (1 << 24);

    @Test
    void guiPreviewArmorMovesTowardsTheViewerWithoutShiftingOnScreen()
    {
        // GameRenderer's GUI projection and model view, with and without a layered screen.
        for (float farPlane : new float[]{11000F, 21000F})
        {
            Matrix4f projection = new Matrix4f().setOrtho(0F, 960F, 540F, 0F, 1000F, farPlane);
            Matrix4f bodyView = new Matrix4f().translation(0F, 0F, 1000F - farPlane);
            Matrix4f armorView = new Matrix4f(bodyView);
            CustomRenderType.applyArmorViewOffset(projection, armorView);

            // InventoryScreen draws the entity at depth 50; the far corner of the screen shifts the most under a scale.
            Vector4f body = clip(projection, bodyView, 900F, 500F, 50F);
            Vector4f armor = clip(projection, armorView, 900F, 500F, 50F);

            assertEquals(body.x, armor.x, 0F);
            assertEquals(body.y, armor.y, 0F);
            assertTrue(body.z - armor.z > 16F * DEPTH_STEP, "armor is not clearly in front of the body at far plane " + farPlane);
            assertTrue(body.z - armor.z < 256F * DEPTH_STEP, "armor is offset far enough to show through nearer geometry at far plane " + farPlane);
        }
    }

    @Test
    void worldArmorKeepsTheVanillaViewOffsetScale()
    {
        Matrix4f projection = new Matrix4f().setPerspective((float) Math.toRadians(70D), 16F / 9F, 0.05F, 512F);
        Matrix4f armorView = new Matrix4f().rotationY(0.6F);
        Matrix4f expected = new Matrix4f(armorView).scale(0.99975586F);
        CustomRenderType.applyArmorViewOffset(projection, armorView);

        assertEquals(expected, armorView);
        Vector4f body = clip(projection, new Matrix4f().rotationY(0.6F), 0.5F, 1F, -4F);
        Vector4f armor = clip(projection, armorView, 0.5F, 1F, -4F);
        assertTrue(armor.z / armor.w < body.z / body.w);
    }

    private static Vector4f clip(Matrix4f projection, Matrix4f modelView, float x, float y, float z)
    {
        return new Matrix4f(projection).mul(modelView).transform(new Vector4f(x, y, z, 1F));
    }
}
