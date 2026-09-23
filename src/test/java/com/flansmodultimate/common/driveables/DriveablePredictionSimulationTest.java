package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.DriveablePrediction.Outcome;
import com.flansmodultimate.common.driveables.DriveablePrediction.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs a predicting client against an authoritative server over a simulated
 * network, both stepping the same deterministic toy vehicle. The server applies
 * whichever input arrived last once per tick, as the real one does, so every
 * disagreement comes from latency, jitter and input timing.
 */
class DriveablePredictionSimulationTest
{
    /** Client input steps are numbered this far ahead of the server's tick count. */
    private static final int SEQUENCE_BASE = 5000;
    private static final int TICKS = 260;

    @Test
    void withoutLatencyThePredictionAlwaysAgrees()
    {
        Map<Outcome, Integer> outcomes = run(0, 0, 1L).outcomes;
        assertEquals(0, outcomes.get(Outcome.CORRECTED));
        assertEquals(0, outcomes.get(Outcome.RESYNC));
    }

    @Test
    void jitteredLatencyNeverResynchronisesAndSettlesOnTheServerState()
    {
        for (long seed = 1L; seed <= 20L; seed++)
        {
            Result result = run(2, 2, seed);
            assertEquals(0, result.outcomes.get(Outcome.RESYNC), "seed " + seed);
            // Once both have come to rest the client stands where the server does.
            assertEquals(result.server.x, result.client.x, 0.01D, "seed " + seed);
            assertEquals(result.server.yaw, result.client.yaw, 0.01D, "seed " + seed);
            // Only input timing is corrected, never the jitter itself: under 1% of the distance driven.
            assertTrue(result.corrected < 0.01D * result.server.x,
                "seed " + seed + " corrected " + result.corrected + " of " + result.server.x);
        }
    }

    @Test
    void highJitterStaysStable()
    {
        Result result = run(4, 5, 7L);
        assertEquals(0, result.outcomes.get(Outcome.RESYNC));
        assertEquals(result.server.x, result.client.x, 0.01D);
        assertTrue(result.corrected < 0.02D * result.server.x, "corrected " + result.corrected);
    }

    /** One tick of driving: throttle held, then steering, then coasting to a stop. */
    private static boolean forward(int tick)
    {
        return tick >= 10 && tick < 90;
    }

    private static boolean steering(int tick)
    {
        return tick >= 30 && tick < 55;
    }

    private static Result run(int latency, int jitter, long seed)
    {
        Random random = new Random(seed);
        Toy client = new Toy();
        Toy server = new Toy();
        DriveablePrediction prediction = new DriveablePrediction();
        List<Input> upstream = new ArrayList<>();
        List<Report> downstream = new ArrayList<>();
        Map<Outcome, Integer> outcomes = new EnumMap<>(Outcome.class);
        for (Outcome outcome : Outcome.values())
            outcomes.put(outcome, 0);
        double corrected = 0D;
        int applied = SEQUENCE_BASE;
        boolean appliedForward = false;
        boolean appliedSteering = false;

        for (int tick = 0; tick < TICKS; tick++)
        {
            // Server: take the latest input that has arrived, step, report.
            for (Input input : upstream)
            {
                if (input.arrival <= tick && input.sequence > applied)
                {
                    applied = input.sequence;
                    appliedForward = input.forward;
                    appliedSteering = input.steering;
                }
            }
            server.step(appliedForward, appliedSteering);
            downstream.add(new Report(tick + delay(latency, jitter, random), tick, applied, server.state()));

            // Client: reconcile reports that have arrived, then predict this tick's input.
            for (Report report : List.copyOf(downstream))
            {
                if (report.arrival > tick)
                    continue;
                downstream.remove(report);
                DriveablePrediction.Reconciliation result = prediction.reconcile(report.serverStep,
                    report.acknowledged, report.state);
                outcomes.merge(result.outcome(), 1, Integer::sum);
                if (result.outcome() == Outcome.CORRECTED)
                {
                    client.vx += result.vx();
                    client.throttle += result.throttle();
                }
                else if (result.outcome() == Outcome.RESYNC)
                    client.set(result.resyncTo());
            }
            DriveablePrediction.Blend blend = prediction.drainBlend();
            client.x += blend.x();
            client.yaw += blend.yaw();
            corrected += Math.abs(blend.x());

            int sequence = SEQUENCE_BASE + 1 + tick;
            boolean forward = forward(tick);
            boolean steering = steering(tick);
            upstream.add(new Input(tick + delay(latency, jitter, random), sequence, forward, steering));
            client.step(forward, steering);
            prediction.record(sequence, client.state());
        }
        return new Result(client, server, outcomes, corrected);
    }

    private static int delay(int latency, int jitter, Random random)
    {
        return latency + (jitter == 0 ? 0 : random.nextInt(jitter + 1));
    }

    /** A deterministic stand-in for a driveable's movement step. */
    private static final class Toy
    {
        double x;
        double vx;
        float yaw;
        float throttle;

        void step(boolean forward, boolean steering)
        {
            throttle += Math.max(-0.05F, Math.min(0.05F, (forward ? 1F : 0F) - throttle));
            vx = vx * 0.95D + throttle * 0.05D;
            if (Math.abs(vx) < 1.0E-4D && !forward)
                vx = 0D;
            x += vx;
            if (steering)
                yaw += 3F;
        }

        State state()
        {
            return new State(x, 64D, 0D, vx, 0D, 0D, yaw, 0F, 0F, throttle);
        }

        void set(State state)
        {
            x = state.x();
            vx = state.vx();
            yaw = state.yaw();
            throttle = state.throttle();
        }
    }

    private record Input(int arrival, int sequence, boolean forward, boolean steering) {}

    private record Report(int arrival, int serverStep, int acknowledged, State state) {}

    private record Result(Toy client, Toy server, Map<Outcome, Integer> outcomes, double corrected) {}
}
