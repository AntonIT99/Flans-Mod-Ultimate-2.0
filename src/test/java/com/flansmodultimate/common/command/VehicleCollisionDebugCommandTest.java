package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import org.junit.jupiter.api.Test;

import net.minecraft.commands.CommandSourceStack;

import static org.junit.jupiter.api.Assertions.*;

class VehicleCollisionDebugCommandTest
{
    @Test
    void registersSenderPlayerAndAllEntityForms()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        VehicleCollisionDebugCommand.register(dispatcher);

        CommandNode<CommandSourceStack> command = dispatcher.getRoot().getChild("flansdebug").getChild("vehiclecollision");
        assertToggleAndExplicit(command);

        CommandNode<CommandSourceStack> targets = command.getChild("player").getChild("targets");
        assertNotNull(targets);
        assertToggleAndExplicit(targets);

        assertToggleAndExplicit(command.getChild("all"));
    }

    @Test
    void debugAliasIncludesAllServerDebugCommands()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ShootPointDebugCommand.register(dispatcher);
        HitboxDebugCommand.register(dispatcher);
        VehicleCollisionDebugCommand.register(dispatcher);

        CommandNode<CommandSourceStack> debug = dispatcher.getRoot().getChild("flansdebug");
        assertSame(debug, dispatcher.getRoot().getChild("flansmodultimate").getChild("debug").getRedirect());
        assertNotNull(debug.getChild("shootpoint"));
        assertNotNull(debug.getChild("hitbox").getCommand());
        assertNull(debug.getChild("htibox"));
        assertNull(dispatcher.getRoot().getChild("flandebug"));
        assertToggleAndExplicit(debug.getChild("vehiclecollision"));
    }

    private static void assertToggleAndExplicit(CommandNode<CommandSourceStack> node)
    {
        assertNotNull(node);
        assertNotNull(node.getCommand());
        assertNotNull(node.getChild("collision"));
        assertNotNull(node.getChild("collision").getCommand());
    }
}
