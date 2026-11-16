package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.InteractionHand.MAIN_HAND;
import static net.minecraft.world.InteractionHand.OFF_HAND;
import static org.junit.jupiter.api.Assertions.*;

class MinigunSoundsTest
{
    private static final MinigunSounds.Sounds ALL = new MinigunSounds.Sounds("warmup", 3, "spin", "cooldown");

    private final MinigunSounds sounds = new MinigunSounds();
    private final Recorder out = new Recorder();

    private static final class Recorder implements MinigunSounds.Output
    {
        private final List<String> events = new ArrayList<>();

        @Override
        public void playOnce(String sound)
        {
            events.add("once:" + sound);
        }

        @Override
        public void loop(InteractionHand hand, String sound)
        {
            events.add("loop:" + hand + ":" + sound);
        }

        @Override
        public void stopLoop(InteractionHand hand)
        {
            events.add("stop:" + hand);
        }

        List<String> drain()
        {
            List<String> drained = new ArrayList<>(events);
            events.clear();
            return drained;
        }
    }

    private void tick(InteractionHand hand, MinigunSounds.Sounds configured, boolean pressed)
    {
        sounds.update(hand, configured, pressed, false, out);
        sounds.endTick(out);
    }

    @Test
    void spinLoopStartsOnceAfterWarmupAndIsOnlyRenewed()
    {
        tick(MAIN_HAND, ALL, true);
        assertEquals(List.of("once:warmup"), out.drain());
        tick(MAIN_HAND, ALL, true);
        tick(MAIN_HAND, ALL, true);
        assertTrue(out.drain().isEmpty());
        tick(MAIN_HAND, ALL, true);
        assertEquals(List.of("loop:MAIN_HAND:spin"), out.drain());

        for (int i = 1; i < MinigunSounds.LOOP_REFRESH_TICKS; i++)
            tick(MAIN_HAND, ALL, true);
        assertTrue(out.drain().isEmpty());
        tick(MAIN_HAND, ALL, true);
        assertEquals(List.of("loop:MAIN_HAND:spin"), out.drain());

        tick(MAIN_HAND, ALL, false);
        assertEquals(List.of("stop:MAIN_HAND", "once:cooldown"), out.drain());
        tick(MAIN_HAND, ALL, false);
        assertTrue(out.drain().isEmpty());
    }

    @Test
    void warmupAndCooldownWorkWithoutSpinSound()
    {
        MinigunSounds.Sounds noSpin = new MinigunSounds.Sounds("warmup", 2, null, "cooldown");
        tick(MAIN_HAND, noSpin, true);
        for (int i = 0; i < 20; i++)
            tick(MAIN_HAND, noSpin, true);
        assertEquals(List.of("once:warmup"), out.drain());
        tick(MAIN_HAND, noSpin, false);
        assertEquals(List.of("once:cooldown"), out.drain());
    }

    @Test
    void cooldownOnlyGunPlaysCooldownOnRelease()
    {
        MinigunSounds.Sounds cooldownOnly = new MinigunSounds.Sounds(null, 20, null, "cooldown");
        tick(MAIN_HAND, cooldownOnly, true);
        tick(MAIN_HAND, cooldownOnly, true);
        assertTrue(out.drain().isEmpty());
        tick(MAIN_HAND, cooldownOnly, false);
        assertEquals(List.of("once:cooldown"), out.drain());
    }

    @Test
    void releaseDuringWarmupCoolsDownWhenWarmupEnds()
    {
        tick(MAIN_HAND, ALL, true);
        tick(MAIN_HAND, ALL, false);
        tick(MAIN_HAND, ALL, false);
        assertEquals(List.of("once:warmup"), out.drain());
        tick(MAIN_HAND, ALL, false);
        assertEquals(List.of("once:cooldown"), out.drain());
    }

    @Test
    void reloadInterruptsWithCooldown()
    {
        tick(MAIN_HAND, ALL, true);
        for (int i = 0; i < 3; i++)
            tick(MAIN_HAND, ALL, true);
        out.drain();
        sounds.update(MAIN_HAND, ALL, true, true, out);
        sounds.endTick(out);
        assertEquals(List.of("stop:MAIN_HAND", "once:cooldown"), out.drain());
        sounds.update(MAIN_HAND, ALL, true, true, out);
        sounds.endTick(out);
        assertTrue(out.drain().isEmpty());
    }

    @Test
    void reloadDuringWarmupAlsoCoolsDown()
    {
        tick(MAIN_HAND, ALL, true);
        out.drain();
        sounds.update(MAIN_HAND, ALL, true, true, out);
        sounds.endTick(out);
        assertEquals(List.of("once:cooldown"), out.drain());
    }

    @Test
    void puttingTheGunAwayStopsTheLoopWithoutCooldown()
    {
        MinigunSounds.Sounds spinOnly = new MinigunSounds.Sounds(null, 0, "spin", "cooldown");
        tick(OFF_HAND, spinOnly, true);
        out.drain();
        sounds.endTick(out);
        assertEquals(List.of("stop:OFF_HAND"), out.drain());
    }
}
