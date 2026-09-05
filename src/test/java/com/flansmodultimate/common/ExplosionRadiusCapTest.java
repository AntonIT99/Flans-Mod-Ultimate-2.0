package com.flansmodultimate.common;

import com.flansmodultimate.common.types.DamageStats;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The radius ceiling is a performance guard, not a balance decision. An authored
 * {@code ExplosiveMass} stays honest; only the radii the server has to iterate are
 * bounded, and a server with the hardware for more simply raises the config value.
 */
class ExplosionRadiusCapTest
{
    private static final float CAP = (float) ModCommonConfig.DEFAULT_MAX_EXPLOSION_RADIUS;

    @Test
    void ordinaryOrdnanceIsUntouched()
    {
        // The largest researched charge in the shipped categories is an 88 kg TNT
        // naval shell, about 45 blocks. Nothing conventional should reach the cap.
        FlanExplosion.Stats stats = stats(45F, 112F, 30F);
        assertEquals(45F, stats.explosionRadius());
        assertEquals(112F, stats.blastRadius());
        assertEquals(30F, stats.fragRadius());
    }

    @Test
    void anExtremeChargeIsClampedOnEveryRadius()
    {
        // A 50 Mt device asks for roughly 36 800 blocks through 10 * cbrt(kg).
        FlanExplosion.Stats stats = stats(36840F, 92100F, 50000F);
        assertEquals(CAP, stats.explosionRadius());
        assertEquals(CAP, stats.blastRadius());
        assertEquals(CAP, stats.fragRadius());
    }

    @Test
    void theBlastRadiusStaysAtLeastTheExplosionRadiusAfterClamping()
    {
        FlanExplosion.Stats stats = stats(36840F, 92100F, 0F);
        assertTrue(stats.blastRadius() >= stats.explosionRadius());
    }

    @Test
    void theAuthoredChargeSurvivesTheClamp()
    {
        // Capping is about what the server iterates, so the explosive mass that
        // drives damage and every downstream derivation must be left alone.
        FlanExplosion.Stats stats = new FlanExplosion.Stats(36840F, 14736F, 92100F,
            new DamageStats(), 0F, 0F, new DamageStats(), 5.0E10F);
        assertEquals(5.0E10F, stats.explosiveMassKg());
        assertEquals(14736F, stats.explosionPower(), "power is not a radius and is not capped");
    }

    @Test
    void aNonFiniteChargeIsNormalisedRatherThanPropagated()
    {
        FlanExplosion.Stats stats = new FlanExplosion.Stats(10F, 1F, 20F,
            new DamageStats(), 0F, 0F, new DamageStats(), Float.NaN);
        assertEquals(0F, stats.explosiveMassKg());
    }

    private static FlanExplosion.Stats stats(float explosion, float blast, float frag)
    {
        return new FlanExplosion.Stats(explosion, 1F, blast, new DamageStats(),
            frag, 0F, new DamageStats(), 1F);
    }
}
