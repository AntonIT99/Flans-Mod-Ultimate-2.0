package com.flansmodultimate.client.particle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The wind every client works out for itself, from the world alone. */
class ParticleWindTest
{
    private static double strength(float[] wind)
    {
        return Math.hypot(wind[0], wind[1]);
    }

    @Test
    void everyClientWorksOutTheSameWind()
    {
        assertArrayEquals(ParticleWind.at(123456L, 42, 0F, 0F), ParticleWind.at(123456L, 42, 0F, 0F));
    }

    @Test
    void theWindTurnsSlowlyAndDiffersBetweenDimensions()
    {
        float[] now = ParticleWind.at(1000L, 42, 0F, 0F);
        float[] soon = ParticleWind.at(1001L, 42, 0F, 0F);
        float[] later = ParticleWind.at(13000L, 42, 0F, 0F);
        double turnedSoon = Math.atan2(soon[1], soon[0]) - Math.atan2(now[1], now[0]);
        assertTrue(Math.abs(turnedSoon) < 1.0E-3D, "a tick barely turns it");
        assertTrue(Math.abs(Math.atan2(later[1], later[0]) - Math.atan2(now[1], now[0])) > 0.5D, "minutes turn it well round");
        float[] elsewhere = ParticleWind.at(1000L, 7, 0F, 0F);
        assertNotEquals(Math.atan2(now[1], now[0]), Math.atan2(elsewhere[1], elsewhere[0]), 1.0E-3D);
    }

    @Test
    void stormsBlowHarder()
    {
        assertTrue(strength(ParticleWind.at(1000L, 42, 1F, 1F)) > strength(ParticleWind.at(1000L, 42, 0F, 0F)));
        assertTrue(strength(ParticleWind.at(1000L, 42, 0F, 0F)) > 0D);
    }
}
