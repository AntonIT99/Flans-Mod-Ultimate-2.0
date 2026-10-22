package com.wolffsmod.npcs.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wolffsmod.npcs.diag.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Measures what NPCs cost the client: for a number of seconds it records every frame time and how much of it goes to
 * drawing Custom NPCs, then writes a summary and the per-frame figures to the logs folder. Started with
 * {@code /flansnpcclientbenchmark [seconds] [warmup] [label]} or from the Server page of the Wolff's Mod tab. The command
 * also accepts {@code status}, {@code stop} and {@code mark <label>}. Chat pauses measurement so those controls remain
 * usable without including the command screen in the measured frames. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientBenchmark
{
    private static final String SECONDS_ARGUMENT = "seconds";
    private static final String WARM_UP_ARGUMENT = "warmup";
    private static final String LABEL_ARGUMENT = "label";
    /** Seconds left out at the start: closing a screen and loading chunks are not what is measured. */
    private static final int DEFAULT_WARM_UP_SECONDS = 3;
    private static final double NANOS_PER_MILLI = 1.0E6D;
    private static final double NANOS_PER_SECOND = 1.0E9D;

    @Nullable
    private static Run run;

    /** One benchmark in progress. */
    private static final class Run
    {
        private final BenchmarkOptions options;
        private final BenchmarkStats frame = new BenchmarkStats();
        private final BenchmarkStats npc = new BenchmarkStats();
        private final BenchmarkStats drawn = new BenchmarkStats();
        private final BenchmarkReport report;
        private long started = System.nanoTime();
        private long lastFrame;
        private long measuringSince;
        private boolean chatOpen;

        private Run(BenchmarkOptions options)
        {
            this.options = options;
            report = new BenchmarkReport("client", "frame,frame_ms,npc_ms,npcs_drawn", options);
        }

        /** Excludes chat and the transition back to the world from both the clock and frame samples. */
        private boolean skipChatFrame(long now, boolean chat)
        {
            if (!chat && !chatOpen)
                return false;
            if (lastFrame != 0L)
            {
                long pause = now - lastFrame;
                started += pause;
                if (measuringSince != 0L)
                    measuringSince += pause;
            }
            chatOpen = chat;
            lastFrame = now;
            return true;
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansnpcclientbenchmark").executes(context -> start(ServerBenchmark.DEFAULT_SECONDS))
            .then(Commands.argument(SECONDS_ARGUMENT, IntegerArgumentType.integer(ServerBenchmark.MIN_SECONDS, ServerBenchmark.MAX_SECONDS))
                .executes(context -> start(IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT)))
                .then(Commands.argument(WARM_UP_ARGUMENT, IntegerArgumentType.integer(0, BenchmarkOptions.MAX_WARM_UP_SECONDS))
                    .executes(context -> start(new BenchmarkOptions(IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT), IntegerArgumentType.getInteger(context, WARM_UP_ARGUMENT), "")))
                    .then(Commands.argument(LABEL_ARGUMENT, StringArgumentType.greedyString())
                        .executes(context -> start(new BenchmarkOptions(IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT), IntegerArgumentType.getInteger(context, WARM_UP_ARGUMENT),
                            StringArgumentType.getString(context, LABEL_ARGUMENT)))))))
            .then(Commands.literal("status").executes(context -> status())).then(Commands.literal("stop").executes(context -> stop())).then(
                Commands.literal("mark").then(Commands.argument(LABEL_ARGUMENT, StringArgumentType.greedyString()).executes(context -> mark(StringArgumentType.getString(context, LABEL_ARGUMENT))))));
    }

    /** @return 1 when the run started, 0 when one was already in progress */
    public static int start(int seconds)
    {
        return start(new BenchmarkOptions(seconds, DEFAULT_WARM_UP_SECONDS, ""));
    }

    private static int start(BenchmarkOptions options)
    {
        if (run != null)
        {
            say(Component.translatable("commands.wolffsmodnpcs.benchmark.busy"));
            return 0;
        }
        run = new Run(options);
        NpcProfiler.setClient(true);
        say(Component.translatable("commands.wolffsmodnpcs.benchmark.client_started", options.seconds(), options.warmUpSeconds()));
        return 1;
    }

    private static int status()
    {
        Run current = run;
        if (current == null)
            return idle();
        long now = System.nanoTime();
        int warmUp = (int) Math.ceil(Math.max(0D, current.options.warmUpSeconds() - (now - current.started) / NANOS_PER_SECOND));
        int measured = current.measuringSince == 0L ? 0 : (int) ((now - current.measuringSince) / NANOS_PER_SECOND);
        Component label = current.options.label().isEmpty() ? Component.translatable("commands.wolffsmodnpcs.benchmark.unnamed") : Component.literal(current.options.label());
        say(Component.translatable("commands.wolffsmodnpcs.benchmark.status", label, measured, current.options.seconds(), warmUp));
        return 1;
    }

    private static int stop()
    {
        if (run == null)
            return idle();
        finish("stopped by command");
        return 1;
    }

    private static int mark(String label)
    {
        Run current = run;
        if (current == null)
            return idle();
        current.report.mark("frame " + current.frame.count() + (current.measuringSince == 0L ? " (warm-up)" : ""), label);
        say(Component.translatable("commands.wolffsmodnpcs.benchmark.marked", BenchmarkOptions.cleanLabel(label)));
        return 1;
    }

    @SuppressWarnings("SameReturnValue") // The shared idle command response always reports that no action was taken.
    private static int idle()
    {
        say(Component.translatable("commands.wolffsmodnpcs.benchmark.idle"));
        return 0;
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
        if (current.skipChatFrame(now, minecraft.screen instanceof ChatScreen))
            return;
        boolean warm = (now - current.started) / NANOS_PER_SECOND >= current.options.warmUpSeconds();
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
        if ((now - current.measuringSince) / NANOS_PER_SECOND >= current.options.seconds())
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
        report.describeSession();
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
