package com.flansmodultimate.common.command;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketDebugHitboxes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/** Operator-only, session-only SetupPart geometry editor. */
public final class HitboxDebugCommand
{
    private static final Set<DriveableType> EDITED = new HashSet<>();
    private static final String[] AXES = {"x", "y", "z", "width", "height", "depth"};
    private HitboxDebugCommand()
    {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        var command = Commands.literal("htibox").executes(c -> edit(c, "list"));
        for (String operation : new String[]{"list", "reset"})
            command.then(Commands.literal(operation).executes(c -> edit(c, operation)));
        command.then(Commands.literal("remove").then(part().executes(c -> edit(c, "remove"))));
        command.then(Commands.literal("set").then(part().then(geometry("set", 0))));
        command.then(Commands.literal("add").then(part().then(Commands.argument("hp", FloatArgumentType.floatArg(0.001F, 1000000F)).then(geometry("add", 0)))));
        command.then(Commands.literal("nudge").then(part().then(geometry("nudge", 0))));
        dispatcher.register(Commands.literal("flandebug").requires(s -> s.hasPermission(2)).then(command));
        dispatcher.register(Commands.literal("flandebug").requires(s -> s.hasPermission(2))
            .then(Commands.literal("hitbox").redirect(dispatcher.getRoot().getChild("flandebug").getChild("htibox"))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> part()
    {
        return Commands.argument("part", StringArgumentType.word())
            .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(EnumDriveablePart.values()).map(EnumDriveablePart::getShortName), b));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> geometry(String operation, int index)
    {
        var argument = Commands.argument(AXES[index], index < 3 ? FloatArgumentType.floatArg(-1024F, 1024F) : FloatArgumentType.floatArg(0.001F, 1024F));
        if (index == (operation.equals("nudge") ? 2 : 5))
            return argument.executes(c -> edit(c, operation));
        return argument.then(geometry(operation, index + 1));
    }

    private static DriveableType target(ServerPlayer player)
    {
        var vehicle = player.getVehicle();
        Driveable driveable = vehicle instanceof Driveable d
            ? d
            : vehicle instanceof Seat seat ? seat.getDriveable() : vehicle != null && vehicle.getVehicle() instanceof Driveable d ? d : null;
        if (driveable != null)
            return driveable.getConfigType();
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).getItem() instanceof DriveableItem<?, ?> item)
                return item.getConfigType();
        return null;
    }

    private static int edit(CommandContext<CommandSourceStack> context, String operation) throws CommandSyntaxException
    {
        DriveableType type = target(context.getSource().getPlayerOrException());
        if (type == null)
            return fail(context, "Ride a driveable or hold its item to edit hitboxes");
        if (operation.equals("list"))
        {
            say(context, type.getShortName() + " hitboxes (SetupPart model pixels); edit " + type.getFileName());
            type.getHealth().forEach((part, box) -> say(context, line(type, part, box)));
            return type.getHealth().size();
        }
        if (operation.equals("reset"))
        {
            type.resetDebugHitboxes();
            EDITED.remove(type);
        }
        else
        {
            EnumDriveablePart part = EnumDriveablePart.getPart(StringArgumentType.getString(context, "part"));
            if (part == null)
                return fail(context, "Unknown driveable part");
            CollisionBox old = type.getHealth().get(part);
            boolean add = operation.equals("add");
            if (add && old != null)
                return fail(context, "Part already has a hitbox; use set");
            if (!add && old == null)
                return fail(context, "Part has no hitbox; use add");
            var boxes = new EnumMap<EnumDriveablePart, CollisionBox>(EnumDriveablePart.class);
            boxes.putAll(type.getHealth());
            if (operation.equals("remove"))
                boxes.remove(part);
            else
            {
                float[] geometry = operation.equals("nudge") ? type.debugHitboxPixels(old) : new float[6];
                for (int i = 0; i < (operation.equals("nudge") ? 3 : 6); i++)
                {
                    float value = FloatArgumentType.getFloat(context, AXES[i]);
                    geometry[i] = operation.equals("nudge") ? geometry[i] + value : value;
                }
                for (float value : geometry)
                    if (!Float.isFinite(value) || Math.abs(value) > 1024F)
                        return fail(context, "Geometry must be finite and within 1024 model pixels");
                float hp = add ? FloatArgumentType.getFloat(context, "hp") : old.getHealth();
                if (!Float.isFinite(hp))
                    return fail(context, "HP must be finite");
                CollisionBox box = type.debugHitboxFromPixels(hp, geometry, add ? 5F : old.getPenetrationResistance(), add ? 0F : old.getCrewDamageMultiplier());
                boxes.put(part, box);
                say(context, line(type, part, box));
            }
            type.setDebugHitboxes(boxes);
            EDITED.add(type);
        }
        var server = context.getSource().getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (!server.isSingleplayerOwner(player.getGameProfile()))
                PacketHandler.sendTo(new PacketDebugHitboxes(type, operation.equals("reset")), player);
        say(context, "Hitboxes " + operation + ": " + type.getShortName() + "; session only, affects every driveable of this type. /flandebug htibox reset restores geometry.");
        return 1;
    }

    public static void clearSession()
    {
        EDITED.forEach(DriveableType::resetDebugHitboxes);
        EDITED.clear();
    }

    public static void syncOnLogin(ServerPlayer player)
    {
        if (!player.getServer().isSingleplayerOwner(player.getGameProfile()))
            for (DriveableType type : EDITED)
                PacketHandler.sendTo(new PacketDebugHitboxes(type, false), player);
    }

    static String line(DriveableType type, EnumDriveablePart part, CollisionBox box)
    {
        StringBuilder result = new StringBuilder("SetupPart ").append(part.getShortName()).append(' ').append(box.getHealth());
        for (float value : type.debugHitboxPixels(box))
            result.append(' ').append(value);
        return result.append(' ').append(box.getPenetrationResistance()).append(' ').append(box.getCrewDamageMultiplier()).toString();
    }

    private static int fail(CommandContext<CommandSourceStack> context, String message)
    {
        context.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    private static void say(CommandContext<CommandSourceStack> context, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
