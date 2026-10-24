package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.Bootstrap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlansModCommandAliasesTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks Forge's NetworkEvent transformer; vanilla registries are initialized first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
    }

    @Test
    void aliasesKeepBareActionsArgumentsAndSuggestions() throws Exception
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("rearm").requires(source -> source.hasPermission(2)).executes(context -> 7).then(Commands.argument("ammo", StringArgumentType.word())
            .suggests((context, builder) -> builder.suggest("shell").buildFuture()).executes(context -> StringArgumentType.getString(context, "ammo").length())));
        FlansModHelpCommand.register(dispatcher);
        FlansModCommandAliases.registerAlias(dispatcher, "rearm", "rearm");
        CommandSourceStack operator = source(true);

        assertEquals(7, dispatcher.execute("rearm", operator));
        assertEquals(7, dispatcher.execute("flansmodultimate rearm", operator));
        assertEquals(5, dispatcher.execute("flansmodultimate rearm shell", operator));
        var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("flansmodultimate rearm sh", operator)).get().getList();
        assertEquals(List.of("shell"), suggestions.stream().map(Suggestion::getText).toList());
        assertNotNull(dispatcher.getRoot().getChild("flansmodultimate").getCommand());
        assertNotNull(dispatcher.getRoot().getChild("flansmodultimate").getChild("help"));
    }

    @Test
    void aliasDoesNotBypassPermissions()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("restricted").requires(source -> source.hasPermission(2)).executes(context -> 1).then(Commands.literal("child").executes(context -> 2)));
        FlansModCommandAliases.registerAlias(dispatcher, "restricted", "restricted");
        CommandSourceStack player = source(false);

        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansmodultimate restricted", player));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansmodultimate restricted child", player));
        // Minecraft filters the command tree sent to each player with this predicate.
        assertFalse(dispatcher.getRoot().getChild("flansmodultimate").getChild("restricted").canUse(player));
    }

    @Test
    void everyServerAliasUsesTheOriginalCommandAndPermission()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CraftingPartsCommand.register(dispatcher);
        DefaultAmmoCommand.register(dispatcher);
        DigitalAmmoCommand.register(dispatcher);
        RearmCommand.register(dispatcher);
        TryClassCommand.register(dispatcher);
        TryTeamCommand.register(dispatcher);
        TeamsCommand.register(dispatcher);
        VehiclePhysicsCommand.register(dispatcher);
        FMParticleCommand.register(dispatcher);
        WorldPhysicsCommand.register(dispatcher);
        FlansModCommandAliases.register(dispatcher);

        for (String name : List.of("craftingparts", "defaultammo", "digitalammo", "rearm", "tryclass", "tryteam", "teams", "ranks", "vehiclephysics", "particle", "physics"))
        {
            String original = switch (name)
            {
                case "particle" -> "fmparticle";
                case "physics" -> "flansphysics";
                default -> name;
            };
            var target = dispatcher.getRoot().getChild(original);
            var alias = dispatcher.getRoot().getChild("flansmodultimate").getChild(name);
            assertSame(target, alias.getRedirect(), name);
            assertSame(target.getCommand(), alias.getCommand(), name);
            assertSame(target.getRequirement(), alias.getRequirement(), name);
        }
    }

    private static CommandSourceStack source(boolean operator)
    {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(2)).thenReturn(operator);
        return source;
    }
}
