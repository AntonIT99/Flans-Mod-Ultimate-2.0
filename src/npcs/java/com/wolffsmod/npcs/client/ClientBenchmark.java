package com.wolffsmod.npcs.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.wolffsmod.npcs.diag.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

/**
 * Measures what NPCs cost the client: for a number of seconds it records every frame time and how much of it goes to
 * drawing Custom NPCs, then writes a summary and the per-frame figures to the logs folder. Started with
 * {@code /flansnpcclientbenchmark [seconds]} or from the Server page of the Wolff's Mod tab. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientBenchmark
{
    /** Seconds left out at the start: closing a screen and loading chunks are not what is measured. */
    private static final double WARM_UP_SECONDS = 3D;
    private static final double NANOS_PER_MILLI = 1.0E6D;
    private static final double NANOS_PER_SECOND = 1.0E9D;

    @Nullable
    private static Run run;

    /** One benchmark in progress. */
    private static final class Run
    {
        private final int seconds;
        private final long started = System.nanoTime();
        private final BenchmarkStats frame = new BenchmarkStats();
        private final BenchmarkStats npc = new BenchmarkStats();
        private final BenchmarkStats drawn = new BenchmarkStats();
        private final BenchmarkReport report = new BenchmarkReport("client", "frame,frame_ms,npc_ms,npcs_drawn");
        private long lastFrame;
        private long measuringSince;

        private Run(int seconds)
        {
            this.seconds = seconds;
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansnpcclientbenchmark").executes(context -> start(ServerBenchmark.DEFAULT_SECONDS))
            .then(Commands.argument("seconds", IntegerArgumentType.integer(ServerBenchmark.MIN_SECONDS, ServerBenchmark.MAX_SECONDS))
                .executes(context -> start(IntegerArgumentType.getInteger(context, "seconds")))));
    }

    /** @return 1 when the run started, 0 when one was already in progress */
    public static int start(int seconds)
    {
        if (run != null)
        {
            say(Component.translatable("commands.wolffsmodnpcs.benchmark.busy"));
            return 0;
        }
        int clamped = Mth.clamp(seconds, ServerBenchmark.MIN_SECONDS, ServerBenchmark.MAX_SECONDS);
        run = new Run(clamped);
        NpcProfiler.setClient(true);
        say(Component.translatable("commands.wolffsmodnpcs.benchmark.client_started", clamped, (int) WARM_UP_SECONDS));
        return 1;
    }

    /** At the end of every rendered frame. */
    public static void frame()
    {
        Run current = run;
        if (current == null)
            return;
        long now = System.nanoTime();
        NpcProfiler.ClientSample sample = NpcProfiler.drainClient();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
        {
            finish("the world was left");
            return;
        }
        boolean warm = (now - current.started) / NANOS_PER_SECOND >= WARM_UP_SECONDS;
        if (warm && current.measuringSince == 0L)
            current.measuringSince = now;
        else if (warm && minecraft.isPaused())
            finish("the game was paused");
        else if (warm && minecraft.screen != null)
            finish("a screen was opened");
        else if (warm)
            measure(current, now, sample);
        current.lastFrame = now;
    }

    private static void measure(Run current, long now, NpcProfiler.ClientSample sample)
    {
        double millis = (now - current.lastFrame) / NANOS_PER_MILLI;
        current.frame.add(millis);
        current.npc.add(sample.npcMillis());
        current.drawn.add(sample.npcs());
        current.report.row(String.format(Locale.ROOT, "%d,%.4f,%.4f,%d", current.frame.count(), millis, sample.npcMillis(), sample.npcs()));
        if ((now - current.measuringSince) / NANOS_PER_SECOND >= current.seconds)
            finish(null);
    }

    private static void finish(@Nullable String abortReason)
    {
        Run finished = run;
        run = null;
        NpcProfiler.setClient(false);
        if (finished == null)
            return;
        BenchmarkReport report = finished.report;
        int frames = finished.frame.count();
        double mean = finished.frame.mean();
        report.line(abortReason == null ? "NPC client benchmark: " + frames + " frames measured" : "NPC client benchmark aborted (" + abortReason + ") after " + frames + " frames");
        report.line(String.format(Locale.ROOT, "Frame rate: %.1f frames per second on average, %.1f for the slowest 1%% of frames", perSecond(mean), perSecond(finished.frame.worstPercentMean())));
        report.line("Frame time: " + finished.frame.summary("ms"));
        report.line(String.format(Locale.ROOT, "Drawing NPCs: %s; %.1f%% of the frame; %.1f NPCs drawn per frame on average", finished.npc.summary("ms per frame"),
            mean <= 0D ? 0D : 100D * finished.npc.mean() / mean, finished.drawn.mean()));
        if (finished.drawn.mean() < 0.5D)
            report.line("Note: hardly any NPC was drawn. Look at the NPCs to be measured. Shadow passes of shader packs count as draws.");
        List<String> lines = report.summary();
        lines.forEach(line -> say(Component.literal(line)));
        report.write().ifPresent(path -> say(Component.translatable("commands.wolffsmodnpcs.benchmark.written", path.toString())));
    }

    private static double perSecond(double frameMillis)
    {
        return frameMillis <= 0D ? 0D : 1000D / frameMillis;
    }

    private static void say(Component message)
    {
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(message, false);
    }
}
