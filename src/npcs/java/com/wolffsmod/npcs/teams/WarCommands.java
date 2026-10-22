package com.wolffsmod.npcs.teams;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

/**
 * {@code /teams war}: sets up wars between managed factions, the dimensions they are fought in and the territory each
 * faction holds, starts and stops them, and shows their state and a map of their territory. Operators only.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WarCommands
{
    private static final String WAR = "war";
    private static final String NAME = "name";
    private static final String FACTION = "faction";
    private static final String DIMENSION = "dimension";
    private static final String RADIUS = "radius";
    private static final String VALUE = "value";
    private static final String SET = "set";
    private static final String KEY = "war.";
    private static final int MAP_RADIUS = 7;
    private static final SimpleCommandExceptionType NO_WAR = new SimpleCommandExceptionType(text("unknown"));
    private static final SimpleCommandExceptionType EXISTS = new SimpleCommandExceptionType(text("exists"));
    private static final SimpleCommandExceptionType NO_FACTION = new SimpleCommandExceptionType(BotCommands.text("faction.unknown"));
    private static final SimpleCommandExceptionType NOT_MANAGED = new SimpleCommandExceptionType(BotCommands.text("faction.not_managed"));
    private static final SimpleCommandExceptionType NOT_IN_WAR = new SimpleCommandExceptionType(text("not_in_war"));
    private static final SimpleCommandExceptionType NO_ENEMIES = new SimpleCommandExceptionType(text("no_enemies"));

    private static final SuggestionProvider<CommandSourceStack> WARS = (context, builder) -> SharedSuggestionProvider
        .suggest(WarStore.get(context.getSource().getServer()).all().stream().map(war -> StringArgumentType.escapeIfRequired(war.getName())).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> WAR_FACTIONS = (context, builder) -> SharedSuggestionProvider.suggest(WarStore.get(context.getSource().getServer())
        .named(StringArgumentType.getString(context, NAME)).map(war -> war.getFactions().stream().map(Skirmishes::factionName).map(StringArgumentType::escapeIfRequired).toList()).orElse(List.of()),
        builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(WAR).requires(source -> source.hasPermission(2));
        root.then(Commands.literal("list").executes(WarCommands::list));
        root.then(Commands.literal("create").then(Commands.argument(NAME, StringArgumentType.string()).executes(WarCommands::create)));
        root.then(Commands.literal("delete").then(warArgument().executes(WarCommands::delete)));
        root.then(Commands.literal("info").then(warArgument().executes(WarCommands::info)));
        root.then(Commands.literal("start").then(warArgument().executes(WarCommands::start)));
        root.then(Commands.literal("stop").then(warArgument().executes(WarCommands::stop)));
        root.then(Commands.literal(FACTION).then(
            warArgument().then(Commands.literal("add").then(Commands.argument(FACTION, StringArgumentType.string()).suggests(FactionCommands.MANAGED).executes(context -> faction(context, true))))
                .then(Commands.literal("remove").then(Commands.argument(FACTION, StringArgumentType.string()).suggests(WAR_FACTIONS).executes(context -> faction(context, false))))));
        root.then(scope());
        root.then(Commands.literal("claim").then(warArgument().then(Commands.argument(FACTION, StringArgumentType.string()).suggests(WAR_FACTIONS).executes(context -> claim(context, true, 0))
            .then(Commands.argument(RADIUS, IntegerArgumentType.integer(0, 32)).executes(context -> claim(context, true, IntegerArgumentType.getInteger(context, RADIUS)))))));
        root.then(Commands.literal("unclaim").then(warArgument().executes(context -> claim(context, false, 0))
            .then(Commands.argument(RADIUS, IntegerArgumentType.integer(0, 32)).executes(context -> claim(context, false, IntegerArgumentType.getInteger(context, RADIUS))))));
        root.then(Commands.literal("map").then(warArgument().executes(context -> map(context, MAP_RADIUS))
            .then(Commands.argument(RADIUS, IntegerArgumentType.integer(1, 24)).executes(context -> map(context, IntegerArgumentType.getInteger(context, RADIUS))))));
        root.then(settings());
        dispatcher.register(Commands.literal("teams").then(root));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> warArgument()
    {
        return Commands.argument(NAME, StringArgumentType.string()).suggests(WARS);
    }

    /** The dimensions a war is fought in: all of them, or those listed. */
    private static LiteralArgumentBuilder<CommandSourceStack> scope()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> war = warArgument();
        war.then(Commands.literal("all").executes(context -> edit(context, target -> target.getDimensions().clear(), text("scope_all"))));
        war.then(Commands.literal("here").executes(context -> edit(context, target ->
        {
            target.getDimensions().clear();
            target.getDimensions().add(context.getSource().getLevel().dimension());
        }, text("scope_only", context.getSource().getLevel().dimension().location()))));
        war.then(Commands.literal("add").then(Commands.argument(DIMENSION, DimensionArgument.dimension()).executes(context -> scopeDimension(context, true))));
        war.then(Commands.literal("remove").then(Commands.argument(DIMENSION, DimensionArgument.dimension()).executes(context -> scopeDimension(context, false))));
        return Commands.literal("scope").then(war);
    }

    /** How a war's engagements are fought. */
    private static LiteralArgumentBuilder<CommandSourceStack> settings()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> war = warArgument();
        war.then(number("infantry", 0, 200, War::setInfantry));
        war.then(number("respawn", 0, 3600, War::setRespawnSeconds));
        war.then(number("tickets", 0, 100000, War::setTickets));
        war.then(number("range", 16, 512, War::setRange));
        war.then(number("engagements", 1, 16, War::setMostEngagements));
        war.then(number("flip", 5, 3600, War::setFlipSeconds));
        LiteralArgumentBuilder<CommandSourceStack> difficulty = Commands.literal("difficulty");
        for (BotSettings.Difficulty value : BotSettings.Difficulty.values())
            difficulty.then(Commands.literal(BotCommands.lower(value)).executes(context -> edit(context, target -> target.setDifficulty(value), text(SET, "difficulty", BotCommands.lower(value)))));
        war.then(difficulty);
        return Commands.literal(SET).then(war);
    }

    private static int scopeDimension(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException
    {
        ResourceKey<Level> dimension = DimensionArgument.getDimension(context, DIMENSION).dimension();
        return edit(context, war ->
        {
            if (add)
                war.getDimensions().add(dimension);
            else
                war.getDimensions().remove(dimension);
        }, text(add ? "scope_added" : "scope_removed", dimension.location()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> number(String name, int least, int most, ObjIntConsumer<War> setter)
    {
        return Commands.literal(name).then(Commands.argument(VALUE, IntegerArgumentType.integer(least, most))
            .executes(context -> edit(context, target -> setter.accept(target, IntegerArgumentType.getInteger(context, VALUE)), text(SET, name, IntegerArgumentType.getInteger(context, VALUE)))));
    }

    // --- Wars ---

    private static int list(CommandContext<CommandSourceStack> context)
    {
        Collection<War> wars = WarStore.get(context.getSource().getServer()).all();
        if (wars.isEmpty())
            context.getSource().sendSuccess(() -> text("none"), false);
        for (War war : wars)
        {
            String factions = String.join(" / ", war.getFactions().stream().map(Skirmishes::factionName).toList());
            context.getSource().sendSuccess(() -> text("list_entry", war.getName(), text(war.isRunning() ? "running" : "stopped_state"), factions.isEmpty() ? "-" : factions), false);
        }
        return wars.size();
    }

    // A command reports how many wars it set up, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(context, NAME);
        WarStore store = WarStore.get(context.getSource().getServer());
        if (store.named(name).isPresent())
            throw EXISTS.create();
        store.add(new War(UUID.randomUUID(), name));
        context.getSource().sendSuccess(() -> text("created", name), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        War war = war(context);
        int removed = Wars.stop(context.getSource().getServer(), war);
        WarStore.get(context.getSource().getServer()).remove(war);
        context.getSource().sendSuccess(() -> text("deleted", war.getName(), removed), true);
        return removed;
    }

    // A command reports how many wars it showed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        War war = war(context);
        String holdings = String.join(", ", war.holdings().entrySet().stream().map(held -> Skirmishes.factionName(held.getKey()) + " " + held.getValue()).toList());
        String scope = war.getDimensions().isEmpty() ? "all" : String.join(", ", war.getDimensions().stream().map(dimension -> dimension.location().toString()).toList());
        context.getSource().sendSuccess(() -> text("info", war.getName(), text(war.isRunning() ? "running" : "stopped_state"), holdings.isEmpty() ? "-" : holdings, scope, war.getInfantry(),
            war.getRespawnSeconds(), war.getTickets(), war.getRange(), war.getMostEngagements(), war.getFlipSeconds()), false);
        SkirmishStore skirmishes = SkirmishStore.get(context.getSource().getServer());
        for (War.Engagement engagement : war.getEngagements())
        {
            ChunkPos chunk = new ChunkPos(engagement.getBorder().second());
            String name = skirmishes.byId(engagement.getSkirmish()).map(Skirmish::getName).orElse("?");
            context.getSource().sendSuccess(() -> text("info_engagement", name, Skirmishes.factionName(engagement.getBorder().firstFaction()),
                Skirmishes.factionName(engagement.getBorder().secondFaction()), chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), engagement.getProgress(), war.getFlipSeconds()), false);
        }
        return 1;
    }

    // A command reports how many wars it started, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        War war = war(context);
        boolean enemies = war.getFactions().stream().anyMatch(faction -> war.getFactions().stream().anyMatch(other -> Wars.atWar(war).test(faction, other)));
        if (!enemies)
            throw NO_ENEMIES.create();
        war.setRunning(true);
        WarStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("started", war.getName()), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        War war = war(context);
        int removed = Wars.stop(context.getSource().getServer(), war);
        context.getSource().sendSuccess(() -> text("stopped", war.getName(), removed), true);
        return removed;
    }

    // A command reports how many factions it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int faction(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException
    {
        Faction faction = faction(context);
        return edit(context, war ->
        {
            if (add && !war.getFactions().contains(faction.id))
                war.getFactions().add(faction.id);
            if (!add)
                war.getFactions().remove(Integer.valueOf(faction.id));
        }, text(add ? "faction_added" : "faction_removed", faction.name));
    }

    /** Gives the chunks around the command's position to a faction of the war, or takes them from whoever held them. */
    private static int claim(CommandContext<CommandSourceStack> context, boolean claim, int radius) throws CommandSyntaxException
    {
        War war = war(context);
        Integer faction = null;
        if (claim)
        {
            Faction found = faction(context);
            if (!war.getFactions().contains(found.id))
                throw NOT_IN_WAR.create();
            faction = found.id;
        }
        ChunkPos around = new ChunkPos(BlockPos.containing(context.getSource().getPosition()));
        int changed = Wars.claim(war, context.getSource().getLevel().dimension(), around, radius, faction);
        WarStore.get(context.getSource().getServer()).setDirty();
        Component message = claim ? text("claimed", changed, Skirmishes.factionName(faction)) : text("unclaimed", changed);
        context.getSource().sendSuccess(() -> message, true);
        return changed;
    }

    // A command reports how many maps it drew, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int map(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException
    {
        War war = war(context);
        ChunkPos around = new ChunkPos(BlockPos.containing(context.getSource().getPosition()));
        List<String> legend = new ArrayList<>();
        for (int i = 0; i < war.getFactions().size(); i++)
            legend.add((char) ('A' + i) + " " + Skirmishes.factionName(war.getFactions().get(i)));
        context.getSource().sendSuccess(() -> text("map_legend", String.join(", ", legend)), false);
        for (String line : Wars.map(context.getSource().getServer(), war, context.getSource().getLevel().dimension(), around, radius))
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int edit(CommandContext<CommandSourceStack> context, Consumer<War> change, Component message) throws CommandSyntaxException
    {
        War war = war(context);
        change.accept(war);
        WarStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    // --- Lookups ---

    private static War war(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        return WarStore.get(context.getSource().getServer()).named(StringArgumentType.getString(context, NAME)).orElseThrow(NO_WAR::create);
    }

    private static Faction faction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, FACTION));
        if (faction == null)
            throw NO_FACTION.create();
        if (BattleFactions.get(context.getSource().getServer()).managed(faction.id).isEmpty())
            throw NOT_MANAGED.create();
        return faction;
    }

    private static Component text(String key, Object... args)
    {
        return BotCommands.text(KEY + key, args);
    }
}
