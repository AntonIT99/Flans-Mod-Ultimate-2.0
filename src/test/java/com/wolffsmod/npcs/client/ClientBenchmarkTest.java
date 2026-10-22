package com.wolffsmod.npcs.client;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wolffsmod.npcs.diag.BenchmarkOptions;
import com.wolffsmod.npcs.diag.BenchmarkReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientBenchmarkTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    @SuppressWarnings("IgnoreResultOfCall") // Mockito records the static Minecraft.getInstance method reference for stubbing.
    void chatControlsDoNotAbortTheRunOrMeasureTheChatAndTransitionFrames() throws CommandSyntaxException
    {
        Minecraft minecraft = mock(Minecraft.class);
        minecraft.level = mock(ClientLevel.class);
        ChatScreen chat = mock(ChatScreen.class);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandSourceStack source = mock(CommandSourceStack.class);
        ClientBenchmark.registerCommands(dispatcher);
        try (var client = mockStatic(Minecraft.class);
            var reports = mockConstruction(BenchmarkReport.class, (report, context) -> assertEquals(new BenchmarkOptions(60, 0, "road test"), context.arguments().get(2))))
        {
            client.when(Minecraft::getInstance).thenReturn(minecraft);
            assertEquals(1, dispatcher.execute("flansnpcclientbenchmark 60 0 road test", source));
            BenchmarkReport report = reports.constructed().get(0);
            ClientBenchmark.frame();
            ClientBenchmark.frame();
            verify(report, times(1)).row(anyString());
            minecraft.screen = chat;
            ClientBenchmark.frame();
            assertEquals(1, dispatcher.execute("flansnpcclientbenchmark status", source));
            assertEquals(1, dispatcher.execute("flansnpcclientbenchmark mark first turn", source));
            ClientBenchmark.frame();
            verify(report).mark("frame 1", "first turn");
            verify(report, times(1)).row(anyString());
            minecraft.screen = null;
            ClientBenchmark.frame();
            verify(report, times(1)).row(anyString());
            ClientBenchmark.frame();
            verify(report, times(2)).row(anyString());
            assertEquals(1, dispatcher.execute("flansnpcclientbenchmark stop", source));
            verify(report).describeSession();
            verify(report).write();
            assertEquals(0, dispatcher.execute("flansnpcclientbenchmark status", source));
        }
    }
}
