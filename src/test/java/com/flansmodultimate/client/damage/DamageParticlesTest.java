package com.flansmodultimate.client.damage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DamageParticlesTest
{
    @ParameterizedTest
    @ValueSource(floats = {0.21F, 0.6F, 1F})
    void smallPartBurstsKeepTheirSize(float size)
    {
        assertEquals(size, DamageParticles.burstScale(size));
    }

    @Test
    void largeHullDeathMagazineAndBreakUpBurstsStayBounded()
    {
        // sizeOf caps a large hull at 3.5; break-up adds another 1.3 multiplier.
        for (float size : new float[]{3.5F, 3.5F * 1.3F, 100F})
        {
            float scale = DamageParticles.burstScale(size);
            assertTrue(scale <= 1.5F);
            // Include the largest starting puff and its growth over its lifetime.
            float smokeDiameter = 2F * 1.3F * scale * (1F + DamageParticles.BLACK_SMOKE.growth());
            assertTrue(smokeDiameter < 15F, "A single burst puff must not become a cloud tens of blocks wide");
        }
        assertTrue(DamageParticles.burstScale(1.4F) > DamageParticles.burstScale(1F));
        assertTrue(DamageParticles.burstScale(1.4F) < 1.4F);
    }

    @ParameterizedTest
    @ValueSource(floats = {0F, -1F, Float.NaN, Float.POSITIVE_INFINITY})
    void invalidSizesDoNotEmitBursts(float size)
    {
        assertEquals(0F, DamageParticles.burstScale(size));
    }
}
