package com.flansmodultimate.config;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DimensionPhysicsFactorsTest
{
    @Test
    void parsesDimensionOverridesAndKeepsTheLastDuplicate()
    {
        var factors = ModCommonConfig.parseDimensionFactors(List.of(
            "space:moon=0.17", "space:mars=0.38", "space:moon=0.2"));
        assertEquals(0.2D, factors.get(ResourceLocation.tryParse("space:moon")));
        assertEquals(0.38D, factors.get(ResourceLocation.tryParse("space:mars")));
        assertFalse(factors.containsKey(ResourceLocation.tryParse("minecraft:overworld")));
    }

    @Test
    void rejectsMalformedDimensionsAndFactors()
    {
        assertTrue(ModCommonConfig.validDimensionFactorLine("minecraft:the_nether=0"));
        assertTrue(ModCommonConfig.validDimensionFactorLine("space:moon=10"));
        assertFalse(ModCommonConfig.validDimensionFactorLine("moon=0.17"));
        assertFalse(ModCommonConfig.validDimensionFactorLine("space:moon=NaN"));
        assertFalse(ModCommonConfig.validDimensionFactorLine("space:moon=-1"));
        assertFalse(ModCommonConfig.validDimensionFactorLine("space:moon=11"));
        assertFalse(ModCommonConfig.validDimensionFactorLine("space:moon=bad"));
    }
}
