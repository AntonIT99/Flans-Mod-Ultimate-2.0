package com.wolffsmod.npcs.client;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlanNameplatePlacementTest
{
    @Test
    void turretGeometryAndFullCollisionBoundsIncludeRestingHeight()
    {
        AABB bounds = FlanModelBounds.combine(4, 2, 0.75F,
            Optional.of(new AABB(-3, -1, -10, 3, 5, 12)),
            Optional.of(new AABB(-4, 0, -11, 4, 7, 13)));
        assertEquals(7.75, bounds.maxY);
        assertEquals(13, bounds.maxZ);
        assertEquals(7.9F, FlanNameplatePlacement.height(1.8F, 2, bounds, 1, 0), 1E-5F);
    }

    @Test
    void npcScalingAndRenderOffsetsRaiseTheNameAboveTheRenderedTop()
    {
        AABB bounds = new AABB(-2, 0, -4, 2, 5, 4);
        assertEquals(11.3F, FlanNameplatePlacement.height(3.6F, 4, bounds, 2, 1), 1E-5F);
        assertEquals(4.65F, FlanNameplatePlacement.height(1.8F, 2, bounds, 1, -0.5), 1E-5F);
    }

    @Test
    void entityHitboxAndExistingHigherNamesRemainTheFallback()
    {
        AABB bounds = FlanModelBounds.combine(4, 3, 0, Optional.empty(), Optional.empty());
        assertEquals(6.15F, FlanNameplatePlacement.height(1.8F, 6, bounds, 1, 0), 1E-5F);
        assertEquals(8F, FlanNameplatePlacement.height(8F, 6, bounds, 1, 0));
    }
}
