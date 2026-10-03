package com.flansmodultimate.client.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldModelPoseTest
{
    @Test
    void npcSizeAndDeathTiltPreserveTheAtlasViewRotation()
    {
        Quaternionf expected = new Quaternionf().rotateY(0.9F).rotateZ(0.8F).rotateX(-0.4F);
        Matrix4f relative = new Matrix4f().translation(0.3F, 1.7F, -0.2F).rotate(expected).scale(3F);
        float scale = WorldModelPose.uniformScale(relative);
        assertEquals(3F, scale, 1E-5F);
        Quaternionf actual = WorldModelPose.rotation(relative, scale, new Matrix4f(), new Quaternionf());
        for (Vector3f direction : new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)})
        {
            Vector3f wanted = expected.transform(new Vector3f(direction));
            assertTrue(wanted.distance(actual.transform(new Vector3f(direction))) < 1E-5F);
        }
    }

    @Test
    void removingTheRendererPoseRetainsLivingOffsetsAndScale()
    {
        Matrix4f root = new Matrix4f().rotateX(0.3F).rotateY(-0.7F).translate(8, -2, 12);
        Matrix4f parent = new Matrix4f().rotateY(0.8F).scale(-2, -2, 2).translate(0, -1.501F, 0);
        // The NPC model restores Y up and its own origin before requesting optimized drawing.
        parent.translate(0, 1.501F, 0).scale(-1, -1, 1).translate(0, 0.75F, 0).rotateY(1.5707963F);
        Matrix4f rendered = new Matrix4f(root).mul(parent);
        Matrix4f relative = new Matrix4f(root).invert().mul(rendered);
        assertEquals(2F, WorldModelPose.uniformScale(relative), 1E-5F);
        assertEquals(parent.m30(), relative.m30(), 1E-5F);
        assertEquals(parent.m31(), relative.m31(), 1E-5F);
        assertEquals(parent.m32(), relative.m32(), 1E-5F);
    }

    @Test
    void unsafeImpostorTransformsRetainGeometry()
    {
        for (Matrix4f matrix : new Matrix4f[]{new Matrix4f().scale(-1, 1, 1),
            new Matrix4f().scale(1, 2, 1), new Matrix4f().m10(0.2F), new Matrix4f().scale(0),
            new Matrix4f().m00(Float.NaN)})
            assertTrue(Float.isNaN(WorldModelPose.uniformScale(matrix)));
    }

    @Test
    void menuProjectionsKeepFullModelDetail()
    {
        assertTrue(WorldModelPreview.perspectiveProjection(new Matrix4f().perspective(1F, 1.6F, 0.05F, 1024F)));
        assertFalse(WorldModelPreview.perspectiveProjection(new Matrix4f().ortho(-10, 10, -10, 10, 0.05F, 1024F)));
        assertFalse(WorldModelPreview.perspectiveProjection(new Matrix4f().m11(Float.NaN)));
    }
}
