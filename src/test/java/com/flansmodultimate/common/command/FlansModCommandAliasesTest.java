package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.Unpooled;
import net.neoforged.neoforge.server.command.CommandHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.Bootstrap;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlansModCommandAliasesTest
{
    @Test
    void operatorCanRepairARiddenNpcAndMaterialRebuildingIsAvailableToPlayers() throws Exception
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        RepairDriveableCommand.register(dispatcher);
        CommandSourceStack operator = source(true);
        var player = mock(net.minecraft.server.level.ServerPlayer.class);
        var npc = mock(net.minecraft.world.entity.LivingEntity.class, withSettings().extraInterfaces(com.flansmodultimate.api.parts.IFlanRepairTarget.class));
        var repairable = (com.flansmodultimate.api.parts.IFlanRepairTarget) npc;
        when(operator.getPlayerOrException()).thenReturn(player);
        when(player.getVehicle()).thenReturn(npc);
        when(repairable.flansmodultimateRestore()).thenReturn(true);
        assertEquals(1, dispatcher.execute("flansmodultimate repair", operator));
        verify(repairable).flansmodultimateRestore();
        when(repairable.flansmodultimateRestore()).thenReturn(false);
        assertEquals(0, dispatcher.execute("flansmodultimate repair", operator));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansmodultimate repair", source(false)));
        assertTrue(dispatcher.getRoot().getChild("flansmodultimate").getChild("rebuild").canUse(source(false)));
    }

    @Test
    void repairRequiresOperatorAndRepairsTheRiddenDriveable() throws Exception
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        RepairDriveableCommand.register(dispatcher);
        CommandSourceStack operator = source(true);
        var player = mock(net.minecraft.server.level.ServerPlayer.class);
        var seat = mock(com.flansmodultimate.common.entity.Seat.class);
        var driveable = mock(com.flansmodultimate.common.entity.Driveable.class);
        when(operator.getPlayerOrException()).thenReturn(player);
        when(player.getVehicle()).thenReturn(seat);
        when(seat.getDriveable()).thenReturn(driveable);
        when(driveable.repairAllParts()).thenReturn(true);

        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("flansmodultimate repair", source(false)));
        verify(driveable, never()).repairAllParts();
        assertEquals(1, dispatcher.execute("flansmodultimate repair", operator));
        verify(driveable).repairAllParts();
        when(player.getVehicle()).thenReturn(null);
        assertEquals(0, dispatcher.execute("flansmodultimate repair", operator));
    }

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
    void leafAliasesCanBeReconstructedFromTheCommandPacket()
    {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        FlansModHelpCommand.register(dispatcher);
        dispatcher.register(Commands.literal("craftingparts").executes(context -> 1));
        FlansModCommandAliases.registerAlias(dispatcher, "craftingparts", "craftingparts");
        RootCommandNode<SharedSuggestionProvider> root = new RootCommandNode<>();
        CommandHelper.mergeCommandNode(dispatcher.getRoot(), root, new HashMap<>(), source(true), context -> 0, provider -> SuggestionProviders.ASK_SERVER);
        var packet = new ClientboundCommandsPacket(root);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            // Minecraft 1.21 packets are written and read through their stream codec.
            ClientboundCommandsPacket.STREAM_CODEC.encode(buffer, packet);
            var received = ClientboundCommandsPacket.STREAM_CODEC.decode(buffer);
            assertDoesNotThrow(() -> received.getRoot(null));
        }
        finally
        {
            buffer.release();
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
            assertSame(target.getChildren().isEmpty() ? null : target, alias.getRedirect(), name);
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
