package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.driveables.ReloadCycle;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static com.wolffsmod.npcs.properties.NpcNativeRhythm.INHERITED;
import static org.junit.jupiter.api.Assertions.*;

class NpcNativeRhythmTest
{
    private static final ReloadCycle BELT = new ReloadCycle(40, 2.5D, 60D, OptionalInt.empty());

    @Test
    void withoutAReloadCycleTheModelFiresSteadilyAtItsFiringInterval()
    {
        assertEquals(new NpcNativeRhythm(2.5D, 1, 2.5D, false), NpcNativeRhythm.of(2.5D, null, INHERITED, INHERITED, INHERITED));
    }

    @Test
    void aReloadCycleFiresItsMagazineThenReloads()
    {
        assertEquals(new NpcNativeRhythm(2.5D, 40, 60D, true), NpcNativeRhythm.of(2.5D, BELT, INHERITED, INHERITED, INHERITED));
    }

    @Test
    void eachPartTheNpcKeepsReplacesOnlyThatPart()
    {
        assertEquals(new NpcNativeRhythm(6D, 40, 60D, true), NpcNativeRhythm.of(2.5D, BELT, 6, INHERITED, INHERITED));
        assertEquals(new NpcNativeRhythm(2.5D, 5, 60D, true), NpcNativeRhythm.of(2.5D, BELT, INHERITED, 5, INHERITED));
        var quickReload = NpcNativeRhythm.of(2.5D, BELT, INHERITED, INHERITED, 20);
        assertEquals(new NpcNativeRhythm(2.5D, 40, 20D, true), quickReload);
        // A steady model can still be given bursts and pauses by hand.
        var manual = NpcNativeRhythm.of(2.5D, null, INHERITED, 3, 30);
        assertEquals(new NpcNativeRhythm(2.5D, 3, 30D, false), manual);
        assertFalse(manual.reloads());
        assertTrue(quickReload.reloads());
    }

    @Test
    void unusableValuesFallBackToOneTickAndOneTrigger()
    {
        // The editor allows a burst delay of zero, which still takes a tick here.
        assertEquals(new NpcNativeRhythm(1D, 1, 1D, false), NpcNativeRhythm.of(Double.NaN, null, 0, 0, 0));
        assertEquals(1D, NpcNativeRhythm.of(0D, null, INHERITED, INHERITED, INHERITED).pause());
    }
}
