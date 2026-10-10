package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EngineSound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEngineCadenceTest
{
    private static final EngineSound ENGINE = new EngineSound("engine", 96, 3);

    @Test
    void automaticClipProgressFollowsChangingPlaybackPitch()
    {
        NpcEngineCadence cadence = new NpcEngineCadence();
        assertTrue(cadence.tick(ENGINE, true, 0.5F));
        assertFalse(cadence.tick(ENGINE, true, 0.5F));
        assertFalse(cadence.tick(ENGINE, true, 0.5F));
        assertTrue(cadence.tick(ENGINE, true, 2F));
        cadence.reset();
        assertTrue(cadence.tick(ENGINE, true, 1F));
    }

    @Test
    void movingEngineStartsImmediatelyThenWaitsTheWholeClipInterval()
    {
        NpcEngineCadence cadence = new NpcEngineCadence();
        assertTrue(cadence.tick(ENGINE, true));
        assertFalse(cadence.tick(ENGINE, true));
        assertFalse(cadence.tick(ENGINE, true));
        assertTrue(cadence.tick(ENGINE, true));
    }

    @Test
    void stoppingDisablingOrChangingSettingsCancelsTheOldClip()
    {
        NpcEngineCadence cadence = new NpcEngineCadence();
        cadence.tick(ENGINE, true);
        assertTrue(cadence.needsStop(ENGINE, false));
        assertFalse(cadence.tick(ENGINE, false));
        assertTrue(cadence.tick(ENGINE, true));
        assertTrue(cadence.needsStop(null, true));
        assertFalse(cadence.tick(null, true));
        cadence.tick(ENGINE, true);
        EngineSound changed = new EngineSound("engine", 64, 100);
        assertTrue(cadence.needsStop(changed, true));
        assertTrue(cadence.tick(changed, true));
        assertFalse(cadence.needsStop(changed, true));
    }
}
