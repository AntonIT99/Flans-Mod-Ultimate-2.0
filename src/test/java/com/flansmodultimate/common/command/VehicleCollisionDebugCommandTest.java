package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VehicleCollisionDebugCommandTest
{
    @Test
    void registersSenderPlayerAndAllEntityForms()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        VehicleCollisionDebugCommand.register(dispatcher);

        CommandNode<CommandSourceStack> command = dispatcher.getRoot().getChild("flandebug")
            .getChild("vehiclecollision");
        assertToggleAndExplicit(command);

        CommandNode<CommandSourceStack> targets = command.getChild("player").getChild("targets");
        assertNotNull(targets);
        assertToggleAndExplicit(targets);

        assertToggleAndExplicit(command.getChild("all"));
    }

    private static void assertToggleAndExplicit(CommandNode<CommandSourceStack> node)
    {
        assertNotNull(node);
        assertNotNull(node.getCommand());
        assertNotNull(node.getChild("collision"));
        assertNotNull(node.getChild("collision").getCommand());
    }
}
