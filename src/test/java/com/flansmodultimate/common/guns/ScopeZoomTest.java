package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.IScope;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScopeZoomTest
{
    @Test
    void zoomStepsAndClampsAtBothBounds()
    {
        IScope scope = scope(2F, 5F, 0.75F);

        assertEquals(2.75F, ScopeZoom.next(2F, scope, true));
        assertEquals(5F, ScopeZoom.next(4.75F, scope, true));
        assertEquals(2F, ScopeZoom.next(2.25F, scope, false));
    }

    @Test
    void reversedBoundsAndInvalidAugmentRemainSafe()
    {
        IScope reversed = scope(5F, 2F, 1F);
        IScope invalidAugment = scope(1F, 4F, Float.NaN);

        assertEquals(3F, ScopeZoom.next(2F, reversed, true));
        assertEquals(4F, ScopeZoom.next(6F, invalidAugment, true));
    }

    private static IScope scope(float min, float max, float augment)
    {
        return new IScope()
        {
            @Override public float getFovFactor() { return 1F; }
            @Override public float getZoomFactor() { return min; }
            @Override public boolean hasVariableZoom() { return true; }
            @Override public float getMinZoom() { return min; }
            @Override public float getMaxZoom() { return max; }
            @Override public float getZoomAugment() { return augment; }
            @Override public boolean hasZoomOverlay() { return false; }
            @Override public ResourceLocation getZoomOverlay() { return ResourceLocation.fromNamespaceAndPath("minecraft", "empty"); }
        };
    }
}
