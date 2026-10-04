package com.wolffsmod.npcs.client;

import net.minecraft.world.phys.AABB;

/** Raises the name anchor before Custom NPCs applies its normal title and text spacing. */
public final class FlanNameplatePlacement
{
    private FlanNameplatePlacement() {}

    public static float height(float original, float npcHeight, AABB modelBounds, float modelScale, double renderOffsetY)
    {
        double modelTop = modelBounds.maxY * modelScale + renderOffsetY;
        if (!Double.isFinite(modelTop))
            return original;
        return (float)Math.max(original, Math.max(npcHeight, modelTop) + 0.15F * modelScale);
    }
}
