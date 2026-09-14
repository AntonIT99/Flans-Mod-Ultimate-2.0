package com.flansmodultimate.common.command;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.DerivedMuzzle;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.PilotGun;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.driveables.ShootPoint;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketDebugShootPoint;
import com.flansmodultimate.network.client.PacketDebugShootPoint.Operation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Developer tooling for placing driveable muzzles.
 *
 * <p>Two jobs. It reports what a type's weapon geometry is authored as next to
 * what the loaded model measures, which is the difference that puts a tank's
 * shell several blocks behind its muzzle when a pack author placed
 * {@code BarrelPosition} at the turret pivot. And it moves those points live, so
 * a value can be walked onto the barrel with the in-world marker as the guide
 * and then written into the type file by hand.</p>
 *
 * <p>Every coordinate is in the units and convention of a type file: model
 * pixels, Y up, lateral axis as authored. A value read out of {@code list} can
 * be pasted into the definition unchanged.</p>
 *
 * <p>Overrides live on the loaded type and so apply to every driveable of it,
 * for the rest of the session or until {@code reset}. Nothing is written to
 * disk.</p>
 *
 * <p>Named {@code flandebug} rather than {@code debug} because vanilla owns that
 * root already.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootPointDebugCommand
{
    private static final String NO_TARGET =
        "Ride a driveable or hold a driveable item to inspect or move its shoot points";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flandebug")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("shootpoint")
                .then(Commands.literal("list").executes(ShootPointDebugCommand::list))
                .then(Commands.literal("reset").executes(ShootPointDebugCommand::reset))
                .then(Commands.literal("primary")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .then(vector(context -> setShootPoint(context, false, false)))))
                .then(Commands.literal("secondary")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .then(vector(context -> setShootPoint(context, true, false)))))
                .then(Commands.literal("nudge")
                    .then(Commands.literal("primary")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                            .then(vector(context -> setShootPoint(context, false, true)))))
                    .then(Commands.literal("secondary")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                            .then(vector(context -> setShootPoint(context, true, true))))))
                .then(Commands.literal("add")
                    .then(Commands.literal("primary").then(addVector(false)))
                    .then(Commands.literal("secondary").then(addVector(true)))))
            .then(Commands.literal("gunorigin")
                .then(Commands.literal("nudge")
                    .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                        .then(vector(context -> setGunOrigin(context, true)))))
                .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                    .then(vector(context -> setGunOrigin(context, false)))))
            .then(Commands.literal("barrelposition")
                .then(vector(ShootPointDebugCommand::setBarrelPosition))));
    }

    /** Trailing {@code x y z} shared by every placement subcommand. */
    private static ArgumentBuilder<CommandSourceStack, ?> vector(FloatTriple action)
    {
        return Commands.argument("x", FloatArgumentType.floatArg())
            .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                    .executes(action::run)));
    }

    /** As {@link #vector}, plus the optional part the new point mounts on. */
    private static ArgumentBuilder<CommandSourceStack, ?> addVector(boolean secondary)
    {
        return Commands.argument("x", FloatArgumentType.floatArg())
            .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                    .executes(context -> addShootPoint(context, secondary, EnumDriveablePart.CORE))
                    .then(Commands.argument("part", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            Arrays.stream(EnumDriveablePart.values()).map(EnumDriveablePart::getShortName), builder))
                        .executes(context -> addShootPoint(context, secondary,
                            EnumDriveablePart.getPart(StringArgumentType.getString(context, "part")))))));
    }

    @FunctionalInterface
    private interface FloatTriple
    {
        int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    // ---------------------------------------------------------------- reporting

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        List<DerivedMuzzle> derived = ClientHooks.RENDER.deriveMuzzles(type);
        send(context, ChatFormatting.GOLD, "=== " + type.getShortName() + " shoot points ===");
        send(context, ChatFormatting.WHITE, "type-file units; edit " + type.getFileName()
            + " in " + type.getContentPack().getName()
            + (type.hasDebugOverrides() ? "  (overridden)" : StringUtils.EMPTY));
        if (derived.isEmpty())
            send(context, ChatFormatting.GRAY,
                "no measured geometry: dedicated server, or this model has no barrel or gun parts");

        listBank(context, type, false, derived);
        listBank(context, type, true, derived);
        listGunOrigins(context, type, derived);
        return 1;
    }

    private static void listBank(CommandContext<CommandSourceStack> context, DriveableType type,
                                 boolean secondary, List<DerivedMuzzle> derived)
    {
        List<ShootPoint> points = type.shootPoints(secondary);
        String bank = secondary ? "secondary" : "primary";
        send(context, ChatFormatting.AQUA, "-- " + bank + " (" + type.weaponType(secondary) + ") --");
        if (points.isEmpty())
        {
            send(context, ChatFormatting.GRAY, "  (none authored; try /flandebug shootpoint add " + bank + ")");
            return;
        }

        // Only one barrel can be measured, so every point in the bank is compared
        // against the main armament. That is the comparison worth making for the
        // gun the barrel belongs to and noise for a coaxial mounted beside it,
        // which is why the line names what it measured rather than just the number.
        Vector3f barrel = muzzleFor(derived, -1);
        for (int index = 0; index < points.size(); index++)
        {
            ShootPoint point = points.get(index);
            Vector3f current = modelPixels(point);
            send(context, point.isDebugOverride() ? ChatFormatting.YELLOW : ChatFormatting.GRAY,
                "  [" + index + "] " + describe(point) + "  " + format(current)
                    + (point.isDebugOverride() ? "  (overridden)" : StringUtils.EMPTY));
            if (barrel != null)
                send(context, ChatFormatting.DARK_AQUA, "        measured barrel " + format(barrel)
                    + "   delta " + format(delta(barrel, current)));
            send(context, ChatFormatting.DARK_GRAY, "        " + typeFileLine(bank, point, current));
        }
    }

    private static void listGunOrigins(CommandContext<CommandSourceStack> context, DriveableType type,
                                       List<DerivedMuzzle> derived)
    {
        send(context, ChatFormatting.AQUA, "-- passenger guns --");
        boolean any = false;
        for (int seat = 1; seat <= type.getNumPassengers(); seat++)
        {
            SeatInfo info = type.getSeat(seat);
            if (info == null || info.getGunType() == null)
                continue;
            any = true;
            Vector3f current = scale(info.getGunOrigin(), 16F);
            boolean overridden = type.isGunOriginOverridden(seat);
            send(context, overridden ? ChatFormatting.YELLOW : ChatFormatting.GRAY,
                "  seat " + seat + " " + info.getGunName() + "  " + format(current)
                    + (overridden ? "  (overridden)" : StringUtils.EMPTY));
            Vector3f measured = muzzleFor(derived, seat);
            if (measured != null)
                send(context, ChatFormatting.DARK_AQUA, "        measured gun " + format(measured)
                    + "   delta " + format(delta(measured, current)));
            send(context, ChatFormatting.DARK_GRAY, "        GunOrigin " + seat + " " + format(current));
        }
        if (!any)
            send(context, ChatFormatting.GRAY, "  (none)");
    }

    // ---------------------------------------------------------------- placement

    private static int setShootPoint(CommandContext<CommandSourceStack> context, boolean secondary, boolean relative)
        throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        int index = IntegerArgumentType.getInteger(context, "index");
        List<ShootPoint> points = type.shootPoints(secondary);
        String bank = secondary ? "secondary" : "primary";
        if (index >= points.size())
        {
            context.getSource().sendFailure(Component.literal("No " + bank + " shoot point at index " + index
                + "; this type has " + points.size()));
            return 0;
        }

        Vector3f target = argumentVector(context);
        if (relative)
            target = add(modelPixels(points.get(index)), target);
        if (!type.setDebugShootPoint(secondary, index, target))
            return 0;

        broadcast(context, new PacketDebugShootPoint(type.getShortName(),
            secondary ? Operation.SET_SECONDARY : Operation.SET_PRIMARY, index, target, StringUtils.EMPTY));
        confirm(context, bank + " [" + index + "]", target,
            typeFileLine(bank, type.shootPoints(secondary).get(index), target));
        return 1;
    }

    private static int addShootPoint(CommandContext<CommandSourceStack> context, boolean secondary,
                                     @Nullable EnumDriveablePart part) throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;
        if (part == null)
        {
            context.getSource().sendFailure(Component.literal("Unknown driveable part"));
            return 0;
        }

        Vector3f target = argumentVector(context);
        int index = type.addDebugShootPoint(secondary, target, part);
        broadcast(context, new PacketDebugShootPoint(type.getShortName(),
            secondary ? Operation.ADD_SECONDARY : Operation.ADD_PRIMARY, index, target, part.getShortName()));
        String bank = secondary ? "secondary" : "primary";
        confirm(context, bank + " [" + index + "] added", target,
            "ShootPoint" + StringUtils.capitalize(bank) + " " + format(target) + " " + part.getShortName());
        return 1;
    }

    private static int setGunOrigin(CommandContext<CommandSourceStack> context, boolean relative)
        throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        int seat = IntegerArgumentType.getInteger(context, "seat");
        SeatInfo info = type.getSeat(seat);
        if (info == null || info.getGunType() == null)
        {
            context.getSource().sendFailure(Component.literal("Seat " + seat + " does not exist or mounts no gun"));
            return 0;
        }

        Vector3f target = argumentVector(context);
        if (relative)
            target = add(scale(info.getGunOrigin(), 16F), target);
        if (!type.setDebugGunOrigin(seat, target))
            return 0;

        broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.GUN_ORIGIN, seat, target,
            StringUtils.EMPTY));
        confirm(context, "GunOrigin seat " + seat, target, "GunOrigin " + seat + " " + format(target));
        return 1;
    }

    /**
     * Legacy shorthand: {@code BarrelPosition} is the main gun's primary point on
     * the turret, so this moves primary zero, and creates it when the definition
     * declares no primary point at all.
     */
    private static int setBarrelPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        Vector3f target = argumentVector(context);
        if (type.shootPoints(false).isEmpty())
        {
            int index = type.addDebugShootPoint(false, target, EnumDriveablePart.TURRET);
            broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.ADD_PRIMARY, index, target,
                EnumDriveablePart.TURRET.getShortName()));
        }
        else
        {
            if (!type.setDebugShootPoint(false, 0, target))
                return 0;
            broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.SET_PRIMARY, 0, target,
                StringUtils.EMPTY));
        }
        confirm(context, "BarrelPosition", target, "BarrelPosition " + format(target));
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;
        if (!type.hasDebugOverrides())
        {
            send(context, ChatFormatting.GRAY, type.getShortName() + " has no overrides to reset");
            return 0;
        }

        type.resetDebugOverrides();
        broadcast(context, PacketDebugShootPoint.reset(type.getShortName()));
        send(context, ChatFormatting.GREEN, type.getShortName() + " restored to its authored shoot points");
        return 1;
    }

    // ---------------------------------------------------------------- plumbing

    @Nullable
    private static DriveableType requireType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        DriveableType type = findType(context.getSource().getPlayerOrException());
        if (type == null)
            context.getSource().sendFailure(Component.literal(NO_TARGET));
        return type;
    }

    @Nullable
    private static DriveableType findType(ServerPlayer player)
    {
        Entity vehicle = player.getVehicle();
        if (vehicle != null)
        {
            Driveable driveable = vehicle instanceof Driveable direct ? direct
                : vehicle.getVehicle() instanceof Driveable parent ? parent : null;
            if (driveable != null && driveable.getConfigType() != null)
                return driveable.getConfigType();
        }
        DriveableType held = typeOf(player.getItemInHand(InteractionHand.MAIN_HAND));
        return held != null ? held : typeOf(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    @Nullable
    private static DriveableType typeOf(ItemStack stack)
    {
        return stack.getItem() instanceof DriveableItem<?, ?> item ? item.getConfigType() : null;
    }

    /**
     * Mirrors an override onto the clients that hold their own copy of the type.
     *
     * <p>The integrated server shares its type objects with its own client, which
     * has therefore already applied the change; sending it there would apply an
     * {@code add} twice.</p>
     */
    private static void broadcast(CommandContext<CommandSourceStack> context, PacketDebugShootPoint packet)
    {
        MinecraftServer server = context.getSource().getServer();
        for (ServerPlayer target : server.getPlayerList().getPlayers())
        {
            if (!server.isSingleplayerOwner(target.getGameProfile()))
                PacketHandler.sendTo(packet, target);
        }
    }

    private static Vector3f argumentVector(CommandContext<CommandSourceStack> context)
    {
        return new Vector3f(FloatArgumentType.getFloat(context, "x"),
            FloatArgumentType.getFloat(context, "y"),
            FloatArgumentType.getFloat(context, "z"));
    }

    /** The muzzle a point resolves to, in type-file units: its mount plus its offset. */
    private static Vector3f modelPixels(ShootPoint point)
    {
        return new Vector3f((point.getRootPos().getPosition().x + point.getOffPos().x) * 16F,
            (point.getRootPos().getPosition().y + point.getOffPos().y) * 16F,
            (point.getRootPos().getPosition().z + point.getOffPos().z) * 16F);
    }

    private static Vector3f scale(Vector3f vector, float factor)
    {
        return new Vector3f(vector.x * factor, vector.y * factor, vector.z * factor);
    }

    private static Vector3f add(Vector3f first, Vector3f second)
    {
        return new Vector3f(first.x + second.x, first.y + second.y, first.z + second.z);
    }

    private static Vector3f delta(Vector3f measured, Vector3f authored)
    {
        return new Vector3f(measured.x - authored.x, measured.y - authored.y, measured.z - authored.z);
    }

    @Nullable
    private static Vector3f muzzleFor(List<DerivedMuzzle> derived, int seatIndex)
    {
        for (DerivedMuzzle muzzle : derived)
        {
            if (muzzle.seatIndex() == seatIndex)
                return muzzle.position();
        }
        return null;
    }

    private static String describe(ShootPoint point)
    {
        String part = point.getRootPos().getPart().getShortName();
        return point.getRootPos() instanceof PilotGun gun ? part + " " + gun.getGunTypeShortName() : part;
    }

    private static String typeFileLine(String bank, ShootPoint point, Vector3f position)
    {
        String gun = point.getRootPos() instanceof PilotGun pilotGun ? " " + pilotGun.getGunTypeShortName()
            : StringUtils.EMPTY;
        return "ShootPoint" + StringUtils.capitalize(bank) + " " + format(position) + " "
            + point.getRootPos().getPart().getShortName() + gun;
    }

    private static void confirm(CommandContext<CommandSourceStack> context, String what, Vector3f position,
                                String typeFileLine)
    {
        send(context, ChatFormatting.GREEN, what + " moved to " + format(position));
        send(context, ChatFormatting.DARK_GRAY, "  " + typeFileLine);
    }

    private static void send(CommandContext<CommandSourceStack> context, ChatFormatting color, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    private static String format(Vector3f vector)
    {
        return format(vector.x) + " " + format(vector.y) + " " + format(vector.z);
    }

    private static String format(float value)
    {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
