package com.wolffsmod.npcs.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@code /teams skirmish transfer|release|enlist}: moves the soldiers of a skirmish to a side of another, lets them go as
 * NPCs of their faction, and enlists NPCs made by hand in a skirmish their faction fights in. Each takes every soldier
 * meant, or only those within a radius of the command's position. Operators only.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SkirmishTroopCommands
{
    private static final String FROM = "from";
    private static final String FROM_SIDE = "from_side";
    private static final String TO = "to";
    private static final String TO_SIDE = "to_side";
    private static final String RADIUS = "radius";
    private static final String ALL = "all";
    private static final double DEFAULT_ENLIST_RADIUS = 16D;
    private static final SimpleCommandExceptionType NO_SKIRMISH = new SimpleCommandExceptionType(text("unknown"));
    private static final SimpleCommandExceptionType NOT_LIVE = new SimpleCommandExceptionType(text("not_live"));
    private static final SimpleCommandExceptionType NO_SIDE = new SimpleCommandExceptionType(text("no_side"));
    private static final SimpleCommandExceptionType SAME = new SimpleCommandExceptionType(text("same_skirmish"));

    private static final SuggestionProvider<CommandSourceStack> SKIRMISHES = (context, builder) -> SharedSuggestionProvider
        .suggest(SkirmishStore.get(context.getSource().getServer()).all().stream().filter(Skirmish::isLive).map(skirmish -> StringArgumentType.escapeIfRequired(skirmish.getName())).toList(), builder);

    static void addTo(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        RequiredArgumentBuilder<CommandSourceStack, String> toSide = sideArgument(TO_SIDE, TO, false).executes(context -> transfer(context, -1D));
        toSide.then(radius(context -> transfer(context, DoubleArgumentType.getDouble(context, RADIUS))));
        root.then(Commands.literal("transfer").then(skirmishArgument(FROM).then(sideArgument(FROM_SIDE, FROM, true).then(skirmishArgument(TO).then(toSide)))));
        root.then(Commands.literal("release").then(skirmishArgument(FROM)
            .then(sideArgument(FROM_SIDE, FROM, true).executes(context -> release(context, -1D)).then(radius(context -> release(context, DoubleArgumentType.getDouble(context, RADIUS)))))));
        root.then(Commands.literal("enlist").then(skirmishArgument(TO).then(
            sideArgument(TO_SIDE, TO, false).executes(context -> enlist(context, DEFAULT_ENLIST_RADIUS)).then(radius(context -> enlist(context, DoubleArgumentType.getDouble(context, RADIUS)))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> skirmishArgument(String name)
    {
        return Commands.argument(name, StringArgumentType.string()).suggests(SKIRMISHES);
    }

    /** A side of the skirmish named by another argument, by its faction, or {@code all} where every side may be meant. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> sideArgument(String name, String skirmishArgument, boolean all)
    {
        return Commands.argument(name, StringArgumentType.string()).suggests((context, builder) ->
        {
            List<String> sides = new ArrayList<>();
            if (all)
                sides.add(ALL);
            SkirmishStore.get(context.getSource().getServer()).named(StringArgumentType.getString(context, skirmishArgument))
                .ifPresent(skirmish -> skirmish.getSides().forEach(side -> sides.add(StringArgumentType.escapeIfRequired(Skirmishes.factionName(side.faction())))));
            return SharedSuggestionProvider.suggest(sides, builder);
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Double> radius(Command<CommandSourceStack> command)
    {
        return Commands.argument(RADIUS, DoubleArgumentType.doubleArg(1D, 512D)).executes(command);
    }

    /** Moves soldiers of a side, or of every side, to a side of another skirmish. */
    private static int transfer(CommandContext<CommandSourceStack> context, double radius) throws CommandSyntaxException
    {
        Skirmish from = live(context, FROM);
        Skirmish to = live(context, TO);
        if (from == to)
            throw SAME.create();
        int side = side(context, TO_SIDE, to);
        ServerLevel level = level(context, from);
        int moved = BotTransfers.transfer(level, from, selection(context, from, radius), to, side);
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("transferred", moved, from.getName(), Skirmishes.factionName(to.getSides().get(side).faction()), to.getName()), true);
        return moved;
    }

    /** Lets soldiers go as NPCs of their faction, no bots any more. */
    private static int release(CommandContext<CommandSourceStack> context, double radius) throws CommandSyntaxException
    {
        Skirmish from = live(context, FROM);
        int released = BotTransfers.release(level(context, from), from, selection(context, from, radius));
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("released", released, from.getName()), true);
        return released;
    }

    /** Enlists the NPCs made by hand around the command's position that belong to a side's faction. */
    private static int enlist(CommandContext<CommandSourceStack> context, double radius) throws CommandSyntaxException
    {
        Skirmish to = live(context, TO);
        int side = side(context, TO_SIDE, to);
        int enlisted = BotTransfers.enlist(level(context, to), to, side, context.getSource().getPosition(), radius);
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("enlisted", enlisted, Skirmishes.factionName(to.getSides().get(side).faction()), to.getName()), true);
        return enlisted;
    }

    /**
     * @param radius
     *            the radius around the command's position soldiers are taken within, or a negative one for anywhere
     */
    private static BotTransfers.Selection selection(CommandContext<CommandSourceStack> context, Skirmish from, double radius) throws CommandSyntaxException
    {
        String side = StringArgumentType.getString(context, FROM_SIDE);
        Integer faction = ALL.equalsIgnoreCase(side) ? null : from.getSides().get(side(context, FROM_SIDE, from)).faction();
        return new BotTransfers.Selection(faction, radius < 0D ? null : context.getSource().getPosition(), radius);
    }

    private static Skirmish live(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException
    {
        Skirmish skirmish = SkirmishStore.get(context.getSource().getServer()).named(StringArgumentType.getString(context, argument)).orElseThrow(NO_SKIRMISH::create);
        if (!skirmish.isLive() || skirmish.getBattle() == null)
            throw NOT_LIVE.create();
        return skirmish;
    }

    private static int side(CommandContext<CommandSourceStack> context, String argument, Skirmish skirmish) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, argument));
        int side = faction == null ? -1 : skirmish.sideOf(faction.id);
        if (side < 0)
            throw NO_SIDE.create();
        return side;
    }

    private static ServerLevel level(CommandContext<CommandSourceStack> context, Skirmish skirmish) throws CommandSyntaxException
    {
        ServerLevel level = context.getSource().getServer().getLevel(skirmish.getDimension());
        if (level == null)
            throw NOT_LIVE.create();
        return Objects.requireNonNull(level);
    }

    private static Component text(String key, Object... args)
    {
        return BotCommands.text("skirmish." + key, args);
    }
}
