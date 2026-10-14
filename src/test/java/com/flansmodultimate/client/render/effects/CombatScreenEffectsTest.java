package com.flansmodultimate.client.render.effects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatScreenEffectsTest
{
    @AfterEach
    void reset()
    {
        CombatScreenEffects.reset();
    }

    @Test
    void explosionsDecayAndRemainIndependentOfSuppression()
    {
        CombatScreenEffects.addExplosion(20F, 1000L, 0.5D);
        CombatScreenEffects.addSuppression(0.5F, -1F, 1000L, 2D);
        assertEquals(10F, CombatScreenEffects.explosionLevel(1250L, 0.5D));
        assertEquals(0F, CombatScreenEffects.explosionLevel(1500L, 0.5D));
        assertEquals(0.25F, CombatScreenEffects.suppressionLevel(1500L, 2D));
        CombatScreenEffects.resetExplosion();
        assertEquals(0.25F, CombatScreenEffects.suppressionLevel(1500L, 2D));
    }

    @Test
    void burstsAccumulateWithACeilingAndFullyRecover()
    {
        CombatScreenEffects.addSuppression(0.6F, 1F, 1000L, 2D);
        CombatScreenEffects.addSuppression(0.6F, 1F, 1000L, 2D);
        assertEquals(1F, CombatScreenEffects.suppressionLevel(1000L, 2D));
        assertEquals(0.5F, CombatScreenEffects.suppressionLevel(2000L, 2D));
        assertEquals(0F, CombatScreenEffects.suppressionRoll(3000L, 2D));
        assertEquals(0F, CombatScreenEffects.suppressionLevel(3000L, 2D));
    }

    @Test
    void invalidImpulsesCannotPoisonTheCamera()
    {
        CombatScreenEffects.addExplosion(Float.POSITIVE_INFINITY, 1000L, 0.5D);
        CombatScreenEffects.addSuppression(Float.NaN, 0F, 1000L, 2D);
        CombatScreenEffects.addSuppression(1F, Float.NaN, 1000L, 2D);
        assertEquals(0F, CombatScreenEffects.explosionLevel(1000L, 0.5D));
        assertEquals(0F, CombatScreenEffects.suppressionRoll(1000L, 2D));
    }
}
