package com.flansmodultimate.common.command;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.driveables.weapons.*;
import com.flansmodultimate.common.entity.*;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.debug.PacketDebugShootPoint;
import com.flansmodultimate.network.client.debug.PacketDebugShootPoint.Operation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.math.BigDecimal;
import java.util.*;

/**
 * Developer tooling for placing driveable muzzles.
 *
 * <p>
 * Two jobs. It reports what a type's weapon geometry is authored as next to
 * what the loaded model measures, which is the difference that puts a tank's
 * shell several blocks behind its muzzle when a pack author placed
 * {@code BarrelPosition} at the turret pivot. And it moves those points live, so
 * a value can be walked onto the barrel with the in-world marker as the guide
 * and then written into the type file by hand.
 * </p>
 *
 * <p>
 * Every coordinate is in the units and convention of a type file: model
 * pixels, Y up, lateral axis as authored. A value read out of {@code list} can
 * be pasted into the definition unchanged.
 * </p>
 *
 * <p>
 * Overrides live on the loaded type and so apply to every driveable of it,
 * for the rest of the session or until {@code reset}. Nothing is written to
 * disk.
 * </p>
 *
 * <p>
 * Named {@code flandebug} rather than {@code debug} because vanilla owns that
 * root already.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootPointDebugCommand
{
    private static final String NO_TARGET = "Ride a driveable or AA gun, hold one as an item, or look at an AA gun or sentry, to inspect its shoot points";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flandebug").requires(source -> source.hasPermission(2))
            .then(Commands.literal("shootpoint").then(Commands.literal("list").executes(ShootPointDebugCommand::list)).then(Commands.literal("reset").executes(ShootPointDebugCommand::reset))
                .then(Commands.literal("apply").executes(ShootPointDebugCommand::applyMeasured))
                .then(Commands.literal("primary").then(Commands.argument("index", IntegerArgumentType.integer(0)).then(vector(context -> setShootPoint(context, false, false)))))
                .then(Commands.literal("secondary").then(Commands.argument("index", IntegerArgumentType.integer(0)).then(vector(context -> setShootPoint(context, true, false)))))
                .then(Commands.literal("seat").then(Commands.argument("seat", IntegerArgumentType.integer(1)).then(vector(context -> setGunOrigin(context, false)))))
                .then(Commands.literal("nudge")
                    .then(Commands.literal("primary").then(Commands.argument("index", IntegerArgumentType.integer(0)).then(vector(context -> setShootPoint(context, false, true)))))
                    .then(Commands.literal("secondary").then(Commands.argument("index", IntegerArgumentType.integer(0)).then(vector(context -> setShootPoint(context, true, true)))))
                    .then(Commands.literal("seat").then(Commands.argument("seat", IntegerArgumentType.integer(1)).then(vector(context -> setGunOrigin(context, true))))))
                .then(Commands.literal("add").then(Commands.literal("primary").then(addVector(false))).then(Commands.literal("secondary").then(addVector(true)))))
            .then(Commands.literal("gunorigin").then(Commands.literal("nudge").then(Commands.argument("seat", IntegerArgumentType.integer(1)).then(vector(context -> setGunOrigin(context, true)))))
                .then(Commands.argument("seat", IntegerArgumentType.integer(1)).then(vector(context -> setGunOrigin(context, false)))))
            .then(Commands.literal("barrelposition").then(vector(ShootPointDebugCommand::setBarrelPosition))));
    }

    /** Trailing {@code x y z} shared by every placement subcommand. */
    private static ArgumentBuilder<CommandSourceStack, ?> vector(FloatTriple action)
    {
        return Commands.argument("x", FloatArgumentType.floatArg())
            .then(Commands.argument("y", FloatArgumentType.floatArg()).then(Commands.argument("z", FloatArgumentType.floatArg()).executes(action::run)));
    }

    /** As {@link #vector}, plus the optional part the new point mounts on. */
    private static ArgumentBuilder<CommandSourceStack, ?> addVector(boolean secondary)
    {
        return Commands.argument("x", FloatArgumentType.floatArg())
            .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg()).executes(context -> addShootPoint(context, secondary, EnumDriveablePart.CORE))
                    .then(Commands.argument("part", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(EnumDriveablePart.values()).map(EnumDriveablePart::getShortName), builder))
                        .executes(context -> addShootPoint(context, secondary, EnumDriveablePart.getPart(StringArgumentType.getString(context, "part")))))));
    }

    @FunctionalInterface
    private interface FloatTriple
    {
        int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    // ---------------------------------------------------------------- reporting

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        AAGunType aaGun = aaGunTarget(context);
        if (aaGun != null)
            return listAAGun(context, aaGun);
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        List<DerivedMuzzle> derived = ClientHooks.RENDER.deriveMuzzles(type);
        send(context, ChatFormatting.GOLD, "=== " + type.getShortName() + " shoot points ===");
        send(context, ChatFormatting.WHITE,
            "type-file units; edit " + type.getFileName() + " in " + type.getContentPack().getName() + (type.hasDebugOverrides() ? "  (overridden)" : StringUtils.EMPTY));
        if (derived.isEmpty())
            send(context, ChatFormatting.GRAY, "no measured geometry: dedicated server, or this model has no barrel or gun parts");

        listBank(context, type, false, derived);
        listBank(context, type, true, derived);
        listGunOrigins(context, type, derived);
        return 1;
    }

    private static void listBank(CommandContext<CommandSourceStack> context, DriveableType type, boolean secondary, List<DerivedMuzzle> derived)
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
                "  [" + index + "] " + describe(point) + "  " + format(current) + (point.isDebugOverride() ? "  (overridden)" : StringUtils.EMPTY));
            if (point.getBarrelCount() > 1)
                send(context, ChatFormatting.DARK_AQUA, "        fires from " + point.getBarrelCount() + " measured barrels in turn: " + formatBarrels(point.getBarrels()));
            if (barrel != null)
                send(context, ChatFormatting.DARK_AQUA, "        measured barrel " + format(barrel) + "   delta " + format(delta(barrel, current)));
            send(context, ChatFormatting.DARK_GRAY, "        " + typeFileLine(bank, point, current));
        }
    }

    private static void listGunOrigins(CommandContext<CommandSourceStack> context, DriveableType type, List<DerivedMuzzle> derived)
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
                "  seat " + seat + " " + info.getGunName() + "  " + format(current) + (overridden ? "  (overridden)" : StringUtils.EMPTY));
            if (info.getGunBarrelCount() > 1)
                send(context, ChatFormatting.DARK_AQUA, "        fires from " + info.getGunBarrelCount() + " measured barrels in turn: " + formatBarrels(info.getGunBarrels()));
            DerivedMuzzle measured = findMuzzle(derived, seat);
            if (measured != null)
            {
                // Both are rest-pose values: firing turns GunOrigin round the pivot as the gun aims.
                send(context, ChatFormatting.DARK_AQUA, "        measured gun " + format(measured.position()) + "   delta " + format(delta(measured.position(), current)));
                if (measured.pivot() != null)
                    send(context, ChatFormatting.DARK_AQUA, "        gun pivot " + format(measured.pivot()) + "   GunOrigin turns round it with the gun");
            }
            send(context, ChatFormatting.DARK_GRAY, "        GunOrigin " + seat + " " + format(current));
        }
        if (!any)
            send(context, ChatFormatting.GRAY, "  (none)");
    }

    // ---------------------------------------------------------------- placement

    private static int setShootPoint(CommandContext<CommandSourceStack> context, boolean secondary, boolean relative) throws CommandSyntaxException
    {
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        int index = IntegerArgumentType.getInteger(context, "index");
        List<ShootPoint> points = type.shootPoints(secondary);
        String bank = secondary ? "secondary" : "primary";
        if (index >= points.size())
        {
            context.getSource().sendFailure(Component.literal("No " + bank + " shoot point at index " + index + "; this type has " + points.size()));
            return 0;
        }

        Vector3f target = argumentVector(context);
        if (relative)
            target = add(modelPixels(points.get(index)), target);
        if (!type.setDebugShootPoint(secondary, index, target))
            return 0;

        broadcast(context, new PacketDebugShootPoint(type.getShortName(), secondary ? Operation.SET_SECONDARY : Operation.SET_PRIMARY, index, target, StringUtils.EMPTY));
        confirm(context, bank + " [" + index + "]", target, typeFileLine(bank, type.shootPoints(secondary).get(index), target));
        return 1;
    }

    private static int addShootPoint(CommandContext<CommandSourceStack> context, boolean secondary, @Nullable EnumDriveablePart part) throws CommandSyntaxException
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
        broadcast(context, new PacketDebugShootPoint(type.getShortName(), secondary ? Operation.ADD_SECONDARY : Operation.ADD_PRIMARY, index, target, part.getShortName()));
        String bank = secondary ? "secondary" : "primary";
        confirm(context, bank + " [" + index + "] added", target, "ShootPoint" + StringUtils.capitalize(bank) + " " + format(target) + " " + part.getShortName());
        return 1;
    }

    private static int setGunOrigin(CommandContext<CommandSourceStack> context, boolean relative) throws CommandSyntaxException
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

        broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.GUN_ORIGIN, seat, target, StringUtils.EMPTY));
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
            broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.ADD_PRIMARY, index, target, EnumDriveablePart.TURRET.getShortName()));
        }
        else
        {
            if (!type.setDebugShootPoint(false, 0, target))
                return 0;
            broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.SET_PRIMARY, 0, target, StringUtils.EMPTY));
        }
        confirm(context, "BarrelPosition", target, "BarrelPosition " + format(target));
        return 1;
    }

    /**
     * Moves every point that has a trustworthy measurement onto it, so the
     * suggestions {@code list} makes can be tried in one step and undone with
     * {@code reset}.
     *
     * <p>
     * Every passenger gun with a measured muzzle takes it as its GunOrigin.
     * Only one barrel is measured, so it is applied only to a primary bank of
     * exactly one point that is not an {@code AddGun}: anything else would drag
     * a coaxial or a twin barrel onto the main gun's muzzle.
     * </p>
     */
    private static int applyMeasured(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        AAGunType aaGun = aaGunTarget(context);
        if (aaGun != null)
            return applyAAGun(context, aaGun);
        DriveableType type = requireType(context);
        if (type == null)
            return 0;

        List<DerivedMuzzle> derived = ClientHooks.RENDER.deriveMuzzles(type);
        if (derived.isEmpty())
        {
            context.getSource().sendFailure(Component.literal("No measured geometry: dedicated server, or this model has no barrel or gun parts"));
            return 0;
        }

        send(context, ChatFormatting.GOLD, "=== " + type.getShortName() + " measured points applied ===");
        int applied = 0;
        Vector3f barrel = muzzleFor(derived, -1);
        List<ShootPoint> primary = type.shootPoints(false);
        if (barrel != null)
        {
            if (primary.size() == 1 && !(primary.get(0).getRootPos() instanceof PilotGun))
            {
                if (type.setDebugShootPoint(false, 0, barrel))
                {
                    broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.SET_PRIMARY, 0, barrel, StringUtils.EMPTY));
                    confirm(context, "primary [0]", barrel, typeFileLine("primary", type.shootPoints(false).get(0), barrel));
                    applied++;
                }
            }
            else
                send(context, ChatFormatting.GRAY,
                    "  primary skipped: the measured barrel fits one plain point, and " + "this bank has " + primary.size() + (primary.isEmpty() ? StringUtils.EMPTY : " or an AddGun"));
        }

        for (DerivedMuzzle muzzle : derived)
        {
            if (muzzle.isBarrel() || !type.setDebugGunOrigin(muzzle.seatIndex(), muzzle.position()))
                continue;
            broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.GUN_ORIGIN, muzzle.seatIndex(), muzzle.position(), StringUtils.EMPTY));
            confirm(context, "GunOrigin seat " + muzzle.seatIndex(), muzzle.position(), "GunOrigin " + muzzle.seatIndex() + " " + format(muzzle.position()));
            applied++;
        }

        if (applied == 0)
        {
            send(context, ChatFormatting.GRAY, "Nothing had a measurement that could be applied");
            return 0;
        }
        send(context, ChatFormatting.WHITE, applied + " point(s) moved; /flandebug shootpoint reset undoes it");
        return applied;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        AAGunType aaGun = aaGunTarget(context);
        if (aaGun != null)
        {
            if (!aaGun.hasDebugOverrides())
            {
                send(context, ChatFormatting.GRAY, aaGun.getShortName() + " has no overrides to reset");
                return 0;
            }
            aaGun.resetDebugOverrides();
            broadcast(context, new PacketDebugShootPoint(aaGun.getShortName(), Operation.AA_RESET, 0, new Vector3f(), StringUtils.EMPTY));
            send(context, ChatFormatting.GREEN, aaGun.getShortName() + " restored to its authored barrels");
            return 1;
        }
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

    // ---------------------------------------------------------------- AA guns

    /**
     * AA guns and sentries author their muzzles as {@code Barrel} lines. A pack
     * shipped as a mod fires from them; a flan folder pack fires from the muzzles
     * measured off its model while the content loaded, unless that correction is
     * switched off. Each line says which one fires, and the magenta markers show
     * the lines beside the real muzzles.
     */
    private static int listAAGun(CommandContext<CommandSourceStack> context, AAGunType type)
    {
        List<Vec3> measured = ClientHooks.RENDER.deriveAAGunBarrelOffsets(type);
        send(context, ChatFormatting.GOLD, "=== " + type.getShortName() + " barrels ===");
        send(context, ChatFormatting.WHITE,
            "type-file units; edit " + type.getFileName() + " in " + type.getContentPack().getName() + (type.hasDebugOverrides() ? "  (overridden)" : StringUtils.EMPTY));
        if (measured.isEmpty())
            send(context, ChatFormatting.GRAY, "no measured geometry: dedicated server, or the model's barrels are empty");

        for (int barrel = 0; barrel < type.getNumBarrels(); barrel++)
        {
            Vector3f current = new Vector3f(type.getBarrelX()[barrel], type.getBarrelY()[barrel], type.getBarrelZ()[barrel]);
            boolean overridden = type.isBarrelOverridden(barrel);
            String fires = type.firesFromBarrelLine(barrel) ? "  (fires)" : type.hasMeasuredBarrels() ? "  (the measured muzzle fires)" : StringUtils.EMPTY;
            send(context, overridden ? ChatFormatting.YELLOW : ChatFormatting.GRAY, "  [" + barrel + "] " + format(current) + (overridden ? "  (overridden)" : StringUtils.EMPTY) + fires);
            if (barrel < measured.size())
            {
                Vector3f suggested = AAGunBarrelGeometry.legacyBarrelFor(measured.get(barrel), type.isSentry());
                send(context, ChatFormatting.DARK_AQUA, "        measured muzzle " + format(suggested) + "   delta " + format(delta(suggested, current)));
                send(context, ChatFormatting.DARK_GRAY, "        " + barrelLine(barrel, suggested));
            }
        }
        return 1;
    }

    private static int applyAAGun(CommandContext<CommandSourceStack> context, AAGunType type)
    {
        List<Vec3> measured = ClientHooks.RENDER.deriveAAGunBarrelOffsets(type);
        if (measured.isEmpty())
        {
            context.getSource().sendFailure(Component.literal("No measured geometry: dedicated server, or the model's barrels are empty"));
            return 0;
        }

        send(context, ChatFormatting.GOLD, "=== " + type.getShortName() + " measured barrels applied ===");
        int applied = 0;
        for (int barrel = 0; barrel < type.getNumBarrels() && barrel < measured.size(); barrel++)
        {
            Vector3f suggested = AAGunBarrelGeometry.legacyBarrelFor(measured.get(barrel), type.isSentry());
            if (!type.setDebugBarrel(barrel, suggested))
                continue;
            broadcast(context, new PacketDebugShootPoint(type.getShortName(), Operation.AA_BARREL, barrel, suggested, StringUtils.EMPTY));
            confirm(context, "Barrel " + barrel, suggested, barrelLine(barrel, suggested));
            applied++;
        }
        send(context, ChatFormatting.WHITE, applied + " barrel(s) moved, to the hundredth of a pixel; " + "/flandebug shootpoint reset undoes it");
        return applied;
    }

    /** Barrel lines are kept to the hundredth of a model pixel, without trailing zeros. */
    private static String barrelLine(int barrel, Vector3f position)
    {
        return "Barrel " + barrel + " " + barrelPixels(position.x) + " " + barrelPixels(position.y) + " " + barrelPixels(position.z);
    }

    private static String barrelPixels(float value)
    {
        return new BigDecimal(Float.toString(AAGunType.roundBarrelPixels(value))).stripTrailingZeros().toPlainString();
    }

    /** How far the look ray reaches for an AA gun or sentry nobody is riding. */
    private static final double AA_GUN_LOOK_REACH = 8D;

    /**
     * The AA gun the command should act on, but only when no driveable is the
     * target: ridden, held as an item, or looked at (a sentry has no seat).
     */
    @Nullable
    private static AAGunType aaGunTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (findType(player) != null)
            return null;
        if (player.getVehicle() instanceof AAGun ridden && ridden.getConfigType() != null)
            return ridden.getConfigType();
        for (InteractionHand hand : InteractionHand.values())
        {
            if (player.getItemInHand(hand).getItem() instanceof AAGunItem item)
                return item.getConfigType();
        }

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1F).scale(AA_GUN_LOOK_REACH));
        AAGun nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (AAGun gun : player.level().getEntitiesOfClass(AAGun.class, player.getBoundingBox().inflate(AA_GUN_LOOK_REACH)))
        {
            var hit = gun.getBoundingBox().inflate(0.3D).clip(eye, end);
            if (gun.getConfigType() != null && hit.isPresent() && hit.get().distanceToSqr(eye) < nearestDistance)
            {
                nearest = gun;
                nearestDistance = hit.get().distanceToSqr(eye);
            }
        }
        return nearest == null ? null : nearest.getConfigType();
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
            Driveable driveable = vehicle instanceof Driveable direct ? direct : vehicle instanceof Seat seat ? seat.getDriveable() : vehicle.getVehicle() instanceof Driveable parent ? parent : null;
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
     * <p>
     * The integrated server shares its type objects with its own client, which
     * has therefore already applied the change; sending it there would apply an
     * {@code add} twice.
     * </p>
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
        return new Vector3f(FloatArgumentType.getFloat(context, "x"), FloatArgumentType.getFloat(context, "y"), FloatArgumentType.getFloat(context, "z"));
    }

    /** The muzzle a point resolves to, in type-file units: its mount plus its offset. */
    private static Vector3f modelPixels(ShootPoint point)
    {
        return new Vector3f((point.getRootPos().getPosition().x + point.getOffPos().x) * 16F, (point.getRootPos().getPosition().y + point.getOffPos().y) * 16F,
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
        DerivedMuzzle muzzle = findMuzzle(derived, seatIndex);
        return muzzle == null ? null : muzzle.position();
    }

    @Nullable
    private static DerivedMuzzle findMuzzle(List<DerivedMuzzle> derived, int seatIndex)
    {
        for (DerivedMuzzle muzzle : derived)
        {
            if (muzzle.seatIndex() == seatIndex)
                return muzzle;
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
        String gun = point.getRootPos() instanceof PilotGun pilotGun ? " " + pilotGun.getGunTypeShortName() : StringUtils.EMPTY;
        return "ShootPoint" + StringUtils.capitalize(bank) + " " + format(position) + " " + point.getRootPos().getPart().getShortName() + gun;
    }

    private static void confirm(CommandContext<CommandSourceStack> context, String what, Vector3f position, String typeFileLine)
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

    /** Barrel offsets, stored in blocks, as type-file pixels relative to the point. */
    private static String formatBarrels(List<Vector3f> barrels)
    {
        return String.join(", ", barrels.stream().map(barrel -> "(" + format(scale(barrel, 16F)) + ")").toList());
    }
}
