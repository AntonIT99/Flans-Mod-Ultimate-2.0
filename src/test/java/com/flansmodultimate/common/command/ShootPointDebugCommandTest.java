package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShootPointDebugCommandTest
{
    @Test
    void registersPassengerSeatPlacementUnderShootPoint()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ShootPointDebugCommand.register(dispatcher);

        assertExecutable(dispatcher, "flandebug", "shootpoint", "seat", "seat", "x", "y", "z");
        assertExecutable(dispatcher, "flandebug", "shootpoint", "nudge", "seat", "seat", "x", "y", "z");
    }

    @Test
    void registersApplyingEveryMeasuredPoint()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ShootPointDebugCommand.register(dispatcher);

        assertExecutable(dispatcher, "flandebug", "shootpoint", "apply");
    }

    private static void assertExecutable(CommandDispatcher<CommandSourceStack> dispatcher, String... path)
    {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        for (String name : path)
        {
            node = node.getChild(name);
            assertNotNull(node, "Missing command node " + String.join(" ", path));
        }
        assertNotNull(node.getCommand(), "Command path is not executable: " + String.join(" ", path));
    }
}
