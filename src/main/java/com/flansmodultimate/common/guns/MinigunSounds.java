package com.flansmodultimate.common.guns;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;

import java.util.EnumMap;
import java.util.Map;

/**
 * Server-side warm-up, spin and cool-down sounds of a gun held with the trigger down, tracked per hand.
 * <p>
 * The spin sound is not repeated by the server: clients loop it themselves, so it plays without the gap a
 * repeat sent over the network would leave. The server only starts it, renews it every
 * {@link #LOOP_REFRESH_TICKS} ticks so that players coming into earshot pick it up, and stops it. Clients
 * stop a loop that is no longer renewed after {@link #LOOP_LEASE_TICKS} ticks, so a missed stop cannot leave
 * it running.
 * <p>
 * Each of the three sounds is optional: a gun may warm up and cool down without a spin sound, or spin without
 * warming up.
 */
public final class MinigunSounds
{
    /** Ticks between renewals of a running spin loop. */
    public static final int LOOP_REFRESH_TICKS = 10;
    /** Ticks a client keeps a spin loop running after its last renewal. */
    public static final int LOOP_LEASE_TICKS = 3 * LOOP_REFRESH_TICKS;

    private final Map<InteractionHand, HandState> hands = new EnumMap<>(InteractionHand.class);

    /** Receives the sounds the state machine decides to play. */
    public interface Output
    {
        void playOnce(String sound);

        /** Starts, or renews, the spin loop of a hand. */
        void loop(InteractionHand hand, String sound);

        void stopLoop(InteractionHand hand);
    }

    /** The configured sounds of the held gun; blank sounds are skipped. */
    public record Sounds(@Nullable String warmup, int warmupLength, @Nullable String spin, @Nullable String cooldown)
    {
        public boolean any()
        {
            return StringUtils.isNotBlank(warmup) || StringUtils.isNotBlank(spin) || StringUtils.isNotBlank(cooldown);
        }
    }

    private enum Phase
    {
        IDLE,
        WARMUP,
        SPINNING
    }

    private static final class HandState
    {
        private Phase phase = Phase.IDLE;
        private int delay;
        private int refresh;
        @Nullable
        private String loopSound;
        private boolean updated;
    }

    /**
     * Advances one hand by a tick. Call it once a tick while the hand holds a gun with any of these sounds.
     *
     * @param pressed
     *            whether the trigger of this hand is held down
     * @param reloading
     *            whether this hand is reloading, which interrupts the gun and plays its cool-down
     */
    public void update(InteractionHand hand, Sounds sounds, boolean pressed, boolean reloading, Output output)
    {
        HandState state = hands.computeIfAbsent(hand, h -> new HandState());
        state.updated = true;

        if (reloading)
        {
            if (state.phase != Phase.IDLE)
            {
                silence(hand, state, output);
                playOnce(sounds.cooldown(), output);
            }
            return;
        }

        if (state.delay > 0 && --state.delay > 0)
            return;

        if (state.phase == Phase.IDLE)
        {
            if (!pressed)
                return;
            playOnce(sounds.warmup(), output);
            state.phase = Phase.WARMUP;
            state.delay = StringUtils.isNotBlank(sounds.warmup()) ? Math.max(0, sounds.warmupLength()) : 0;
            if (state.delay > 0)
                return;
        }

        if (!pressed)
        {
            silence(hand, state, output);
            playOnce(sounds.cooldown(), output);
            return;
        }

        String spin = StringUtils.isNotBlank(sounds.spin()) ? sounds.spin() : null;
        if (state.phase == Phase.WARMUP || !StringUtils.equals(spin, state.loopSound))
        {
            state.phase = Phase.SPINNING;
            startLoop(hand, state, spin, output);
        }
        else if (spin != null && --state.refresh <= 0)
        {
            output.loop(hand, spin);
            state.refresh = LOOP_REFRESH_TICKS;
        }
    }

    /**
     * Ends the tick: a hand that was not updated no longer holds such a gun, so its loop stops without a
     * cool-down.
     */
    public void endTick(Output output)
    {
        for (Map.Entry<InteractionHand, HandState> entry : hands.entrySet())
        {
            HandState state = entry.getValue();
            if (!state.updated)
                silence(entry.getKey(), state, output);
            state.updated = false;
        }
    }

    /** Forgets every hand without sending anything, for a player whose sounds already ended with them. */
    public void reset()
    {
        hands.clear();
    }

    private static void startLoop(InteractionHand hand, HandState state, @Nullable String spin, Output output)
    {
        if (state.loopSound != null && spin == null)
            output.stopLoop(hand);
        state.loopSound = spin;
        state.refresh = LOOP_REFRESH_TICKS;
        if (spin != null)
            output.loop(hand, spin);
    }

    private static void silence(InteractionHand hand, HandState state, Output output)
    {
        if (state.loopSound != null)
            output.stopLoop(hand);
        state.loopSound = null;
        state.phase = Phase.IDLE;
        state.delay = 0;
    }

    private static void playOnce(@Nullable String sound, Output output)
    {
        if (StringUtils.isNotBlank(sound))
            output.playOnce(sound);
    }
}
