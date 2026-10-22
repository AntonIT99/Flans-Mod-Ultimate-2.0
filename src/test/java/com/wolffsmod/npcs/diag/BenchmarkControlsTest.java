package com.wolffsmod.npcs.diag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BenchmarkControlsTest
{
    @Test
    void optionsBoundDurationWarmupAndUntrustedLabels()
    {
        BenchmarkOptions options = new BenchmarkOptions(0, -1, "  road\nturn\t../test  ");
        assertEquals(ServerBenchmark.MIN_SECONDS, options.seconds());
        assertEquals(0, options.warmUpSeconds());
        assertEquals("road turn ../test", options.label());
        assertEquals("road-turn-test", options.fileLabel());
        BenchmarkOptions longLabel = new BenchmarkOptions(1000, 100, "🚗".repeat(150));
        assertEquals(ServerBenchmark.MAX_SECONDS, longLabel.seconds());
        assertEquals(BenchmarkOptions.MAX_WARM_UP_SECONDS, longLabel.warmUpSeconds());
        assertEquals(120, longLabel.label().codePointCount(0, longLabel.label().length()));
        assertEquals("", longLabel.fileLabel());
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // Mutating the returned snapshot must fail: this checks the immutable result contract.
    void annotationsFollowTheOutcomeAndPreserveTheirSamplePosition()
    {
        BenchmarkReport report = new BenchmarkReport("client", "frame,frame_ms,npc_ms,npcs_drawn", new BenchmarkOptions(45, 0, "busy road"));
        report.mark("frame 18", "first\nturn");
        report.line("NPC client benchmark: 100 frames measured");
        report.describeSession();
        assertEquals(List.of("NPC client benchmark: 100 frames measured", "Session: busy road", "Requested duration: 45 s; warm-up: 0 s", "Markers:", "  frame 18: first turn"), report.summary());
        List<String> summary = report.summary();
        assertThrows(UnsupportedOperationException.class, () -> summary.add("changed"));
    }

    @Test
    void serverCommandsRetainQuickStartAndAcceptWarmupLabelsStatusAndStop() throws CommandSyntaxException
    {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(true);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        NpcBenchmarkCommands.register(dispatcher);
        try (var benchmark = mockStatic(ServerBenchmark.class))
        {
            benchmark.when(() -> ServerBenchmark.start(eq(source), any(BenchmarkOptions.class))).thenReturn(true);
            assertEquals(1, dispatcher.execute("flansnpcbenchmark", source));
            assertEquals(1, dispatcher.execute("flansnpcbenchmark 45", source));
            assertEquals(1, dispatcher.execute("flansnpcbenchmark 45 0 busy road", source));
            benchmark.verify(() -> ServerBenchmark.start(source, new BenchmarkOptions(30, 5, "")));
            benchmark.verify(() -> ServerBenchmark.start(source, new BenchmarkOptions(45, 5, "")));
            benchmark.verify(() -> ServerBenchmark.start(source, new BenchmarkOptions(45, 0, "busy road")));
            benchmark.when(() -> ServerBenchmark.status(source)).thenReturn(true);
            assertEquals(1, dispatcher.execute("flansnpcbenchmark status", source));
            benchmark.when(() -> ServerBenchmark.stop(null, "stopped by command")).thenReturn(true);
            assertEquals(1, dispatcher.execute("flansnpcbenchmark stop", source));
            assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansnpcbenchmark 45 61", source));
            assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansnpcbenchmark 45 -1", source));
            when(source.hasPermission(2)).thenReturn(false);
            assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansnpcbenchmark status", source));
        }
    }

    @Test
    void zeroWarmupMeasuresTheFirstTickWithoutEarlierProfilingSamples()
    {
        CommandSourceStack source = mock(CommandSourceStack.class);
        MinecraftServer server = mock(MinecraftServer.class);
        NpcServerSettings settings = mock(NpcServerSettings.class);
        try (var reports = mockConstruction(BenchmarkReport.class); var serverSettings = mockStatic(NpcServerSettings.class))
        {
            serverSettings.when(() -> NpcServerSettings.get(server)).thenReturn(settings);
            NpcProfiler.setServer(true);
            NpcProfiler.npcTick(20_000_000L);
            NpcProfiler.targetSearch(0, 5, 3, true, 1_000_000L);
            try
            {
                assertTrue(ServerBenchmark.start(source, new BenchmarkOptions(5, 0, "zero warm-up")));
                assertEquals(0, NpcProfiler.band(0).searches());
                ServerBenchmark.serverTickStart();
                NpcProfiler.npcTick(1_000_000L);
                ServerBenchmark.serverTickEnd(server);
                BenchmarkReport report = reports.constructed().get(0);
                verify(report).row(matches("1,[^,]+,1\\.0000,1,0\\.0000,0,0,0\\.0000"));
                assertTrue(ServerBenchmark.status(source));
            }
            finally
            {
                ServerBenchmark.stop(server, "test finished");
                NpcProfiler.setServer(false);
            }
        }
    }
}
