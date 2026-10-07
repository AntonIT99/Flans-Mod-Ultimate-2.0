package com.wolffsmod.npcs.client;

import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fits every corner of a centered, rotated box with room around the preview. */
public final class FlanPreviewFraming
{
    private FlanPreviewFraming() {}

    public static float pixelsPerBlock(AABB bounds, Quaternionf rotation, float width, float height)
    {
        Vector3f corner = new Vector3f();
        double halfX = bounds.getXsize() / 2;
        double halfY = bounds.getYsize() / 2;
        double halfZ = bounds.getZsize() / 2;
        float extentX = 0;
        float extentY = 0;
        for (int i = 0; i < 8; i++)
        {
            corner.set((float)((i & 1) == 0 ? -halfX : halfX),
                (float)((i & 2) == 0 ? -halfY : halfY), (float)((i & 4) == 0 ? -halfZ : halfZ));
            rotation.transform(corner);
            extentX = Math.max(extentX, Math.abs(corner.x));
            extentY = Math.max(extentY, Math.abs(corner.y));
        }
        float x = Math.max(0.1F, 2 * extentX);
        float y = Math.max(0.1F, 2 * extentY);
        float scale = (x != 0 && y != 0) ? 0.8F * Math.min(width / x, height / y) : 1F;
        return Float.isFinite(scale) && scale > 0 ? Math.min(30F, scale) : 1F;
    }
}
