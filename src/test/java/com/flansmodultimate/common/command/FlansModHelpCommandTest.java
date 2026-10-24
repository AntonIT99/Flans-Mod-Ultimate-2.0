package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlansModHelpCommandTest
{
    @Test
    void bareRootAndHelpOpenTheSameCompactMenu() throws Exception
    {
        List<Component> output = new ArrayList<>();
        CommandSourceStack source = source(output, true);
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        dispatcher.execute("flansmodultimate", source);
        List<String> menu = output.stream().map(Component::getString).toList();
        assertTrue(menu.size() <= 10);
        Component equipment = output.stream().filter(line -> line.getString().startsWith("equipment")).findFirst().orElseThrow();
        assertEquals(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/flansmodultimate help equipment"), equipment.getStyle().getClickEvent());

        output.clear();
        dispatcher.execute("flansmodultimate help", source);
        assertEquals(menu, output.stream().map(Component::getString).toList());
    }

    @Test
    void equipmentCommandsFillChatAndExplainUsageOnHover() throws Exception
    {
        List<Component> output = new ArrayList<>();
        dispatcher().execute("flansmodultimate help equipment", source(output, true));
        Component rearm = output.stream().filter(line -> line.getString().startsWith("rearm")).findFirst().orElseThrow();
        assertEquals(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/flansmodultimate rearm "), rearm.getStyle().getClickEvent());
        HoverEvent hover = rearm.getStyle().getHoverEvent();
        assertNotNull(hover);
        Component hint = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        assertNotNull(hint);
        assertTrue(hint.getString().contains("/flansmodultimate rearm [ammo]"));
        assertTrue(hint.getString().contains("Shortcut: /rearm"));
        assertEquals(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/flansmodultimate help"), output.get(output.size() - 1).getStyle().getClickEvent());
    }

    @Test
    void playersOnlySeeHelpForCommandsTheyCanUse() throws Exception
    {
        List<Component> output = new ArrayList<>();
        CommandSourceStack player = source(output, false);
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        dispatcher.execute("flansmodultimate help", player);
        assertFalse(output.stream().anyMatch(line -> line.getString().startsWith("admin") || line.getString().startsWith("debug")));
        assertFalse(dispatcher.getRoot().getChild("flansmodultimate").getChild("help").getChild("admin").canUse(player));

        output.clear();
        dispatcher.execute("flansmodultimate help equipment", player);
        assertTrue(output.stream().anyMatch(line -> line.getString().startsWith("attachments")));
        assertFalse(output.stream().anyMatch(line -> line.getString().contains("rearm") || line.getString().contains("defaultammo")));
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        FlansModHelpCommand.register(dispatcher);
        return dispatcher;
    }

    private static CommandSourceStack source(List<Component> output, boolean operator)
    {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(operator);
        doAnswer(invocation ->
        {
            Supplier<Component> message = invocation.getArgument(0);
            output.add(message.get());
            return null;
        }).when(source).sendSuccess(any(), eq(false));
        return source;
    }
}
