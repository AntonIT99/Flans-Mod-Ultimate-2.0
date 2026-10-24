package com.flansmodultimate.common.command;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionBypass;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.driveable.PacketDriveableCollisionBypass;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Operator control for temporarily passing through driveable collision hulls:
 * for the sender, for chosen players, or for every entity.
 *
 * <p>
 * The optional argument states whether vehicle collision is on, matching the
 * command's name: {@code false} lets the targets pass through hulls, {@code true}
 * makes hulls solid for them again. Internally that is the inverse bypass flag.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VehicleCollisionDebugCommand
{
    private static final String TARGETS = "targets";
    private static final String MESSAGE_PREFIX = "Vehicle collision ";
    private static final String COLLISION = "collision";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansdebug").requires(source -> source.hasPermission(2))
            .then(Commands.literal("vehiclecollision").executes(context -> setPlayers(context, List.of(context.getSource().getPlayerOrException()), null))
                .then(Commands.argument(COLLISION, BoolArgumentType.bool()).executes(context -> setPlayers(context, List.of(context.getSource().getPlayerOrException()), bypass(context))))
                .then(Commands.literal("player")
                    .then(Commands.argument(TARGETS, EntityArgument.players()).executes(context -> setPlayers(context, EntityArgument.getPlayers(context, TARGETS), null))
                        .then(Commands.argument(COLLISION, BoolArgumentType.bool()).executes(context -> setPlayers(context, EntityArgument.getPlayers(context, TARGETS), bypass(context))))))
                .then(Commands.literal("all").executes(context -> setAll(context, !DriveableCollisionBypass.isAllEntities()))
                    .then(Commands.argument(COLLISION, BoolArgumentType.bool()).executes(context -> setAll(context, bypass(context)))))));
        dispatcher.register(Commands.literal("flansmodultimate").then(Commands.literal("debug").requires(source -> source.hasPermission(2)).redirect(dispatcher.getRoot().getChild("flansdebug"))));
    }

    /** The bypass state the typed collision state asks for: no collision means bypassing hulls. */
    private static boolean bypass(CommandContext<CommandSourceStack> context)
    {
        return !BoolArgumentType.getBool(context, COLLISION);
    }

    /** Sets each target's own bypass, or toggles each one individually when {@code bypass} is null. */
    private static int setPlayers(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, @Nullable Boolean bypass)
    {
        CommandSourceStack source = context.getSource();
        boolean lastState = false;
        for (ServerPlayer player : targets)
        {
            boolean state = bypass != null ? bypass : !DriveableCollisionBypass.isEnabledForPlayer(player);
            DriveableCollisionBypass.setEnabled(player, state);
            PacketHandler.sendTo(new PacketDriveableCollisionBypass(false, state), player);
            if (player != source.getEntity())
                player.sendSystemMessage(stateMessage(MESSAGE_PREFIX + describe(state) + " for you", state));
            lastState = state;
        }

        // A toggle over several players can leave them in different states.
        boolean single = targets.size() == 1;
        String groupAction = bypass == null ? "toggled" : describe(bypass);
        String action = single ? describe(lastState) : groupAction;
        String who = single ? targets.iterator().next().getName().getString() : targets.size() + " players";
        String note = DriveableCollisionBypass.isAllEntities() ? " (it is still off for all entities)" : "";
        boolean highlight = single ? lastState : bypass == null || bypass;
        source.sendSuccess(() -> stateMessage(MESSAGE_PREFIX + action + " for " + who + note, highlight), true);
        return targets.size();
    }

    private static int setAll(CommandContext<CommandSourceStack> context, boolean bypass)
    {
        DriveableCollisionBypass.setAllEntities(bypass);
        PacketHandler.sendToAll(new PacketDriveableCollisionBypass(true, bypass));
        context.getSource().sendSuccess(() -> stateMessage(MESSAGE_PREFIX + describe(bypass) + " for all entities", bypass), true);
        return 1;
    }

    /** Sends the all-entities bypass to a player who joins while it is on. */
    public static void syncOnLogin(ServerPlayer player)
    {
        if (DriveableCollisionBypass.isAllEntities())
            PacketHandler.sendTo(new PacketDriveableCollisionBypass(true, true), player);
    }

    /** Describes the collision state a bypass leaves, so messages read like the command's own argument. */
    private static String describe(boolean bypass)
    {
        return bypass ? "disabled" : "enabled";
    }

    private static Component stateMessage(String text, boolean bypass)
    {
        return Component.literal(text).withStyle(bypass ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
    }
}
