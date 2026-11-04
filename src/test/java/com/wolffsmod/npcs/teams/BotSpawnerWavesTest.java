package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BotSpawnerWavesTest
{
    @Test
    void queuedWaveSurvivesTickBoundariesButDoesNotAdmitLaterArrivals()
    {
        BotSpawnerWaves waves = new BotSpawnerWaves();
        UUID spawner = UUID.randomUUID();
        waves.started(spawner, List.of(2, 3, 4, 5, 6));
        waves.attempted(2);
        waves.attempted(3);
        waves.attempted(4);
        waves.retain(List.of(5, 6, 7));
        assertTrue(waves.contains(spawner, 5));
        assertTrue(waves.contains(spawner, 6));
        assertFalse(waves.contains(spawner, 7));
        assertFalse(waves.contains(UUID.randomUUID(), 5));
        waves.started(spawner, List.of(6, 7));
        assertFalse(waves.contains(spawner, 7));
        waves.attempted(5);
        waves.attempted(6);
        assertFalse(waves.contains(spawner, 6));
    }

    @Test
    void failedOrCancelledMembersCannotReuseWaveAdmission()
    {
        BotSpawnerWaves waves = new BotSpawnerWaves();
        UUID spawner = UUID.randomUUID();
        waves.started(spawner, List.of(1, 2));
        waves.attempted(1);
        waves.retain(List.of());
        assertFalse(waves.contains(spawner, 1));
        assertFalse(waves.contains(spawner, 2));
        waves.started(spawner, List.of(3));
        assertTrue(waves.contains(spawner, 3));
    }
}
