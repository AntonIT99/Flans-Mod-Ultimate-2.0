package com.wolffsmod.npcs.teams;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * {@code /teams skirmish}: drafts skirmishes between managed factions, side by side and force by force, starts them, and
 * pauses, watches and stops them; {@code quick} starts one between two factions in front of the player in one command.
 * Operators only.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkirmishCommands
{
    private static final String SKIRMISH = "skirmish";
    private static final String NAME = "name";
    private static final String FACTION = "faction";
    private static final String OTHER = "other";
    private static final String POSITION = "position";
    private static final String RADIUS = "radius";
    private static final String COUNT = "count";
    private static final String VALUE = "value";
    private static final String TEMPLATE = "template";
    private static final String TYPE = "type";
    private static final String DISTANCE = "distance";
    private static final String SET = "set";
    private static final String KEY = "skirmish.";
    private static final String LISTED = "listed";
    private static final String VEHICLES = "vehicles";
    private static final String RESPAWN = "respawn";
    private static final String PROTECTION = "protection";
    private static final double DEFAULT_RADIUS = 16D;
    private static final int QUICK_INFANTRY = 16;
    private static final double QUICK_DISTANCE = 96D;
    /** Blocks the player's crosshair may reach for the middle of a quick skirmish. */
    private static final double SIGHT = 160D;
    private static final SimpleCommandExceptionType NO_SKIRMISH = new SimpleCommandExceptionType(text("unknown"));
    private static final SimpleCommandExceptionType EXISTS = new SimpleCommandExceptionType(text("exists"));
    private static final SimpleCommandExceptionType DRAFT_ONLY = new SimpleCommandExceptionType(text("draft_only"));
    private static final SimpleCommandExceptionType NO_SIDE = new SimpleCommandExceptionType(text("no_side"));
    private static final SimpleCommandExceptionType NO_FACTION = new SimpleCommandExceptionType(BotCommands.text("faction.unknown"));
    private static final SimpleCommandExceptionType NOT_MANAGED = new SimpleCommandExceptionType(BotCommands.text("faction.not_managed"));
    private static final SimpleCommandExceptionType SIDE_TAKEN = new SimpleCommandExceptionType(text("side_taken"));
    private static final SimpleCommandExceptionType NOT_LIVE = new SimpleCommandExceptionType(text("not_live"));
    private static final SimpleCommandExceptionType NO_TEMPLATE = new SimpleCommandExceptionType(BotCommands.text("no_template"));

    private static final SuggestionProvider<CommandSourceStack> SKIRMISHES = (context, builder) -> SharedSuggestionProvider
        .suggest(SkirmishStore.get(context.getSource().getServer()).all().stream().map(skirmish -> StringArgumentType.escapeIfRequired(skirmish.getName())).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> SIDES = (context, builder) -> SharedSuggestionProvider.suggest(
        skirmishOrEmpty(context).map(skirmish -> skirmish.getSides().stream().map(side -> StringArgumentType.escapeIfRequired(Skirmishes.factionName(side.faction()))).toList()).orElse(List.of()),
        builder);
    private static final SuggestionProvider<CommandSourceStack> ROLES = (context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(BotRole.values()).map(BotCommands::lower).toList(),
        builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(SKIRMISH).requires(source -> source.hasPermission(2));
        root.then(Commands.literal("list").executes(SkirmishCommands::list));
        root.then(Commands.literal("clear").executes(SkirmishCommands::clear));
        root.then(Commands.literal("quick")
            .then(Commands.argument(FACTION, StringArgumentType.string()).suggests(FactionCommands.MANAGED)
                .then(Commands.argument(OTHER, StringArgumentType.string()).suggests(FactionCommands.MANAGED).executes(context -> quick(context, QUICK_INFANTRY, QUICK_DISTANCE))
                    .then(Commands.argument(COUNT, IntegerArgumentType.integer(0, 200)).executes(context -> quick(context, IntegerArgumentType.getInteger(context, COUNT), QUICK_DISTANCE))
                        .then(Commands.argument(DISTANCE, DoubleArgumentType.doubleArg(16D, 512D))
                            .executes(context -> quick(context, IntegerArgumentType.getInteger(context, COUNT), DoubleArgumentType.getDouble(context, DISTANCE))))))));
        root.then(Commands.literal("create").then(Commands.argument(NAME, StringArgumentType.string()).executes(SkirmishCommands::create)));
        root.then(lifecycle("start", SkirmishCommands::start));
        root.then(lifecycle("stop", SkirmishCommands::stop));
        root.then(lifecycle("pause", context -> pause(context, true)));
        root.then(lifecycle("resume", context -> pause(context, false)));
        root.then(lifecycle("status", SkirmishCommands::status));
        root.then(
            Commands
                .literal(
                    "side")
                .then(skirmishArgument()
                    .then(Commands.literal("add")
                        .then(Commands.argument(FACTION, StringArgumentType.string()).suggests(FactionCommands.MANAGED).executes(context -> addSide(context, false, DEFAULT_RADIUS))
                            .then(Commands.argument(RADIUS, DoubleArgumentType.doubleArg(2D, 256D)).executes(context -> addSide(context, false, DoubleArgumentType.getDouble(context, RADIUS))))
                            .then(Commands.literal("at")
                                .then(Commands.argument(POSITION, Vec3Argument.vec3()).executes(context -> addSide(context, true, DEFAULT_RADIUS)).then(
                                    Commands.argument(RADIUS, DoubleArgumentType.doubleArg(2D, 256D)).executes(context -> addSide(context, true, DoubleArgumentType.getDouble(context, RADIUS))))))))
                    .then(Commands.literal("remove").then(sideArgument().executes(SkirmishCommands::removeSide)))));
        root.then(forces());
        root.then(settings());
        dispatcher.register(Commands.literal("teams").then(root));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> skirmishArgument()
    {
        return Commands.argument(NAME, StringArgumentType.string()).suggests(SKIRMISHES);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> sideArgument()
    {
        return Commands.argument(FACTION, StringArgumentType.string()).suggests(SIDES);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> lifecycle(String name, Command<CommandSourceStack> command)
    {
        return Commands.literal(name).then(skirmishArgument().executes(command));
    }

    /** What each side of a draft fields: its infantry, squads and vehicles, or what its branches list. */
    private static LiteralArgumentBuilder<CommandSourceStack> forces()
    {
        LiteralArgumentBuilder<CommandSourceStack> forces = Commands.literal("force");
        RequiredArgumentBuilder<CommandSourceStack, String> side = sideArgument();
        side.then(Commands.literal(LISTED).executes(context -> editRequest(context, request -> SkirmishComposer.Request.AS_LISTED, text("force_listed", sideName(context)))));
        side.then(
            Commands.literal("infantry").then(Commands.literal(LISTED).executes(context -> editRequest(context, request -> withInfantry(request, -1), text("infantry_listed", sideName(context)))))
                .then(Commands.argument(COUNT, IntegerArgumentType.integer(0, 500)).executes(context -> editRequest(context,
                    request -> withInfantry(request, IntegerArgumentType.getInteger(context, COUNT)), text("infantry", sideName(context), IntegerArgumentType.getInteger(context, COUNT))))));
        side.then(Commands.literal("squad").then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TeamForceCommands.TEMPLATES)
            .then(Commands.argument(COUNT, IntegerArgumentType.integer(0, 50)).executes(SkirmishCommands::squad))));
        side.then(Commands.literal(VEHICLES)
            .then(Commands.literal("role")
                .then(Commands.argument(VALUE, StringArgumentType.word()).suggests(ROLES).then(Commands.argument(COUNT, IntegerArgumentType.integer(0, 100)).executes(SkirmishCommands::vehicleRole))))
            .then(Commands.literal(TYPE)
                .then(Commands.argument(VALUE, StringArgumentType.word()).suggests(TeamForceCommands.VEHICLES)
                    .then(Commands.argument(COUNT, IntegerArgumentType.integer(0, 100)).executes(SkirmishCommands::vehicleType))))
            .then(Commands.literal(LISTED).executes(
                context -> editRequest(context, request -> new SkirmishComposer.Request(request.infantry(), request.squads(), Map.of(), Map.of()), text("vehicles_listed", sideName(context))))));
        return forces.then(skirmishArgument().then(side));
    }

    /** How a skirmish is fought and ends. */
    private static LiteralArgumentBuilder<CommandSourceStack> settings()
    {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal(SET);
        RequiredArgumentBuilder<CommandSourceStack, String> skirmish = skirmishArgument();
        LiteralArgumentBuilder<CommandSourceStack> intent = Commands.literal("intent");
        for (Skirmish.Intent value : Skirmish.Intent.values())
            intent.then(Commands.literal(BotCommands.lower(value)).executes(context -> edit(context, false, target -> target.setIntent(value), text(SET, "intent", BotCommands.lower(value)))));
        skirmish.then(intent);
        skirmish.then(Commands.literal("end").then(Commands.literal("wipeout").executes(context -> edit(context, false, target -> target.setEnd(Skirmish.End.WIPEOUT), text(SET, "end", "wipeout"))))
            .then(Commands.literal("never").executes(context -> edit(context, false, target -> target.setEnd(Skirmish.End.NEVER), text(SET, "end", "never"))))
            .then(Commands.literal("time").then(Commands.argument(COUNT, IntegerArgumentType.integer(1, 600)).executes(context -> edit(context, false, target ->
            {
                target.setEnd(Skirmish.End.TIME);
                target.setTimeLimitMinutes(IntegerArgumentType.getInteger(context, COUNT));
            }, text("end_time", IntegerArgumentType.getInteger(context, COUNT)))))));
        skirmish.then(Commands.literal(RESPAWN).then(Commands.literal("off").executes(context -> edit(context, true, target -> target.setRespawn(false), text(SET, RESPAWN, "off"))))
            .then(Commands.argument(COUNT, IntegerArgumentType.integer(0, 3600)).executes(context -> edit(context, true, target ->
            {
                target.setRespawn(true);
                target.setRespawnSeconds(IntegerArgumentType.getInteger(context, COUNT));
            }, text(SET, RESPAWN, IntegerArgumentType.getInteger(context, COUNT))))));
        skirmish.then(Commands.literal("tickets").then(Commands.argument(COUNT, IntegerArgumentType.integer(0)).executes(
            context -> edit(context, true, target -> target.setTickets(IntegerArgumentType.getInteger(context, COUNT)), text(SET, "tickets", IntegerArgumentType.getInteger(context, COUNT))))));
        LiteralArgumentBuilder<CommandSourceStack> difficulty = Commands.literal("difficulty");
        for (BotSettings.Difficulty value : BotSettings.Difficulty.values())
            difficulty
                .then(Commands.literal(BotCommands.lower(value)).executes(context -> edit(context, true, target -> target.setDifficulty(value), text(SET, "difficulty", BotCommands.lower(value)))));
        skirmish.then(difficulty);
        skirmish.then(Commands.literal(PROTECTION).then(Commands.literal("on").executes(context -> edit(context, false, target -> target.setProtection(true), text(SET, PROTECTION, "on"))))
            .then(Commands.literal("off").executes(context -> edit(context, false, target -> target.setProtection(false), text(SET, PROTECTION, "off")))));
        return set.then(skirmish);
    }

    // --- Skirmishes ---

    private static int list(CommandContext<CommandSourceStack> context)
    {
        Collection<Skirmish> skirmishes = SkirmishStore.get(context.getSource().getServer()).all();
        if (skirmishes.isEmpty())
            context.getSource().sendSuccess(() -> text("none"), false);
        for (Skirmish skirmish : skirmishes)
        {
            String sides = String.join(" / ", skirmish.getSides().stream().map(side -> Skirmishes.factionName(side.faction())).toList());
            context.getSource().sendSuccess(() -> text("list_entry", skirmish.getName(), BotCommands.lower(skirmish.getState()), sides.isEmpty() ? "-" : sides), false);
        }
        return skirmishes.size();
    }

    private static int clear(CommandContext<CommandSourceStack> context)
    {
        int removed = 0;
        List<Skirmish> skirmishes = List.copyOf(SkirmishStore.get(context.getSource().getServer()).all());
        for (Skirmish skirmish : skirmishes)
            removed += Skirmishes.stop(context.getSource().getServer(), skirmish);
        int bots = removed;
        context.getSource().sendSuccess(() -> text("cleared", skirmishes.size(), bots), true);
        return skirmishes.size();
    }

    // A command reports how many skirmishes it drafted, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(context, NAME);
        SkirmishStore store = SkirmishStore.get(context.getSource().getServer());
        if (store.named(name).isPresent())
            throw EXISTS.create();
        store.add(new Skirmish(UUID.randomUUID(), name, context.getSource().getLevel().dimension()));
        context.getSource().sendSuccess(() -> text("created", name), true);
        return 1;
    }

    /**
     * Drafts and starts a skirmish between two factions in front of the player: the middle is where the player looks, the
     * first faction's zone lies toward the player and the second's beyond, the two a distance apart.
     */
    private static int quick(CommandContext<CommandSourceStack> context, int infantry, double distance) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Faction first = managed(context, FACTION);
        Faction second = managed(context, OTHER);
        if (first.id == second.id)
            throw SIDE_TAKEN.create();
        HitResult hit = player.pick(SIGHT, 1F, false);
        Vec3 look = player.getLookAngle();
        Vec3 middle = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(look.scale(distance)) : hit.getLocation();
        Vec3 flat = new Vec3(look.x, 0D, look.z);
        Vec3 along = flat.lengthSqr() < 1.0E-4D ? new Vec3(1D, 0D, 0D) : flat.normalize();
        ServerLevel level = player.serverLevel();
        Vec3 near = SkirmishBattleground.ground(level, middle.x - along.x * distance / 2D, middle.z - along.z * distance / 2D);
        Vec3 far = SkirmishBattleground.ground(level, middle.x + along.x * distance / 2D, middle.z + along.z * distance / 2D);
        SkirmishStore store = SkirmishStore.get(context.getSource().getServer());
        Skirmish skirmish = new Skirmish(UUID.randomUUID(), quickName(store), level.dimension());
        SkirmishComposer.Request request = new SkirmishComposer.Request(infantry, Map.of(), Map.of(), Map.of());
        skirmish.getSides().add(new Skirmish.Side(first.id, near, DEFAULT_RADIUS, request, List.of()));
        skirmish.getSides().add(new Skirmish.Side(second.id, far, DEFAULT_RADIUS, request, List.of()));
        store.add(skirmish);
        Skirmishes.Outcome outcome = Skirmishes.start(context.getSource().getServer(), skirmish);
        if (!outcome.started())
            store.remove(skirmish);
        tell(context, outcome.messages(), outcome.started());
        return outcome.started() ? 1 : 0;
    }

    private static String quickName(SkirmishStore store)
    {
        int number = 1;
        while (store.named("quick-" + number).isPresent())
            number++;
        return "quick-" + number;
    }

    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Skirmishes.Outcome outcome = Skirmishes.start(context.getSource().getServer(), skirmish(context));
        tell(context, outcome.messages(), outcome.started());
        return outcome.started() ? 1 : 0;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Skirmish skirmish = skirmish(context);
        int removed = Skirmishes.stop(context.getSource().getServer(), skirmish);
        context.getSource().sendSuccess(() -> text("stopped", skirmish.getName(), removed), true);
        return removed;
    }

    // A command reports how many skirmishes it paused or resumed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int pause(CommandContext<CommandSourceStack> context, boolean paused) throws CommandSyntaxException
    {
        Skirmish skirmish = skirmish(context);
        if (!Skirmishes.pause(context.getSource().getServer(), skirmish, paused))
            throw NOT_LIVE.create();
        context.getSource().sendSuccess(() -> text(paused ? "paused" : "resumed", skirmish.getName()), true);
        return 1;
    }

    // A command reports how many skirmishes it showed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Skirmish skirmish = skirmish(context);
        ServerLevel level = context.getSource().getServer().getLevel(skirmish.getDimension());
        String end = skirmish.getEnd() == Skirmish.End.TIME ? BotCommands.lower(skirmish.getEnd()) + " " + skirmish.getTimeLimitMinutes() + " min" : BotCommands.lower(skirmish.getEnd());
        String respawn = skirmish.isRespawn() ? skirmish.getRespawnSeconds() + " s" : "off";
        context.getSource().sendSuccess(() -> text("status", skirmish.getName(), BotCommands.lower(skirmish.getState()), BotCommands.lower(skirmish.getIntent()), end, respawn), false);
        BotBattle battle = skirmish.getBattle();
        for (int i = 0; i < skirmish.getSides().size(); i++)
        {
            Skirmish.Side side = skirmish.getSides().get(i);
            int index = i;
            int standing = battle == null || level == null ? 0 : Skirmishes.strength(level, battle, i);
            int deaths = battle == null ? 0 : battle.getSlots().values().stream().filter(slot -> battle.sideOf(slot.getTeam()) == index).mapToInt(BotSlot::getDeaths).sum();
            Component tickets = battle != null && skirmish.isRespawn() && skirmish.getTickets() > 0 ? text("status_tickets", BotPopulation.ticketsLeft(battle, i)) : Component.empty();
            String position = String.format(Locale.ROOT, "%.0f %.0f %.0f", side.anchor().x, side.anchor().y, side.anchor().z);
            context.getSource().sendSuccess(() -> text("status_side", Skirmishes.factionName(side.faction()), position, standing, deaths, tickets), false);
        }
        return 1;
    }

    // --- Drafting ---

    // A command reports how many sides it added, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int addSide(CommandContext<CommandSourceStack> context, boolean given, double radius) throws CommandSyntaxException
    {
        Skirmish skirmish = draft(context);
        Faction faction = managed(context, FACTION);
        if (skirmish.sideOf(faction.id) >= 0)
            throw SIDE_TAKEN.create();
        Vec3 at = given ? Vec3Argument.getVec3(context, POSITION) : context.getSource().getPosition();
        skirmish.getSides().add(new Skirmish.Side(faction.id, at, radius, SkirmishComposer.Request.AS_LISTED, List.of()));
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("side_added", faction.name, skirmish.getName()), true);
        return 1;
    }

    // A command reports how many sides it removed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int removeSide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Skirmish skirmish = draft(context);
        int side = side(context, skirmish);
        String name = Skirmishes.factionName(skirmish.getSides().remove(side).faction());
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("side_removed", name, skirmish.getName()), true);
        return 1;
    }

    private static int squad(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String template = StringArgumentType.getString(context, TEMPLATE);
        if (SquadTemplates.get().find(template).isEmpty())
            throw NO_TEMPLATE.create();
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return editRequest(context, request ->
        {
            Map<String, Integer> squads = new LinkedHashMap<>(request.squads());
            if (count == 0)
                squads.remove(template);
            else
                squads.put(template, count);
            return new SkirmishComposer.Request(request.infantry(), squads, request.vehiclesByRole(), request.vehiclesByType());
        }, text("squads", sideName(context), template, count));
    }

    private static int vehicleRole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(context, VALUE);
        BotRole role = Arrays.stream(BotRole.values()).filter(candidate -> candidate.name().equalsIgnoreCase(name)).findFirst()
            .orElseThrow(() -> new SimpleCommandExceptionType(BotCommands.text("faction.bad_role")).create());
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return editRequest(context, request ->
        {
            Map<BotRole, Integer> roles = new EnumMap<>(BotRole.class);
            roles.putAll(request.vehiclesByRole());
            if (count == 0)
                roles.remove(role);
            else
                roles.put(role, count);
            return new SkirmishComposer.Request(request.infantry(), request.squads(), roles, request.vehiclesByType());
        }, text(VEHICLES, sideName(context), BotCommands.lower(role), count));
    }

    private static int vehicleType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String type = StringArgumentType.getString(context, VALUE);
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return editRequest(context, request ->
        {
            Map<String, Integer> types = new LinkedHashMap<>(request.vehiclesByType());
            if (count == 0)
                types.remove(type);
            else
                types.put(type, count);
            return new SkirmishComposer.Request(request.infantry(), request.squads(), request.vehiclesByRole(), types);
        }, text(VEHICLES, sideName(context), type, count));
    }

    private static SkirmishComposer.Request withInfantry(SkirmishComposer.Request request, int infantry)
    {
        return new SkirmishComposer.Request(infantry, request.squads(), request.vehiclesByRole(), request.vehiclesByType());
    }

    // A command reports how many sides it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int editRequest(CommandContext<CommandSourceStack> context, UnaryOperator<SkirmishComposer.Request> change, Component message) throws CommandSyntaxException
    {
        Skirmish skirmish = draft(context);
        int side = side(context, skirmish);
        skirmish.getSides().set(side, skirmish.getSides().get(side).withRequest(change.apply(skirmish.getSides().get(side).request())));
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    /**
     * Changes a skirmish's settings; those its forces are composed from only while it is a draft.
     *
     * @param draftOnly
     *            whether the setting only matters before the skirmish starts
     */
    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int edit(CommandContext<CommandSourceStack> context, boolean draftOnly, Consumer<Skirmish> change, Component message) throws CommandSyntaxException
    {
        Skirmish skirmish = draftOnly ? draft(context) : skirmish(context);
        change.accept(skirmish);
        SkirmishStore.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    // --- Lookups ---

    private static Skirmish skirmish(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        return skirmishOrEmpty(context).orElseThrow(NO_SKIRMISH::create);
    }

    private static Optional<Skirmish> skirmishOrEmpty(CommandContext<CommandSourceStack> context)
    {
        return SkirmishStore.get(context.getSource().getServer()).named(StringArgumentType.getString(context, NAME));
    }

    private static Skirmish draft(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Skirmish skirmish = skirmish(context);
        if (skirmish.getState() != Skirmish.State.DRAFT)
            throw DRAFT_ONLY.create();
        return skirmish;
    }

    private static int side(CommandContext<CommandSourceStack> context, Skirmish skirmish) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, FACTION));
        int side = faction == null ? -1 : skirmish.sideOf(faction.id);
        if (side < 0)
            throw NO_SIDE.create();
        return side;
    }

    private static String sideName(CommandContext<CommandSourceStack> context)
    {
        return StringArgumentType.getString(context, FACTION);
    }

    private static Faction managed(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, argument));
        if (faction == null)
            throw NO_FACTION.create();
        if (BattleFactions.get(context.getSource().getServer()).managed(faction.id).isEmpty())
            throw NOT_MANAGED.create();
        return faction;
    }

    private static void tell(CommandContext<CommandSourceStack> context, List<Component> messages, boolean started)
    {
        for (Component message : messages)
        {
            if (started)
                context.getSource().sendSuccess(() -> message, true);
            else
                context.getSource().sendFailure(message);
        }
    }

    private static Component text(String key, Object... args)
    {
        return BotCommands.text(KEY + key, args);
    }
}
