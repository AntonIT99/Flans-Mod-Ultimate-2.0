package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsLiveRound;
import com.flansmodultimate.api.ITeamsRound;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.names.SoldierNames;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.UnaryOperator;

/**
 * {@code /teams ai}: sets up the bots of each round of the rotation, the squad templates and the map markers, and lets
 * players order their allied bots in and out of the driveable they drive. Settings apply at once to a round being played.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotCommands
{
    private static final String ROUND = "round";
    private static final String TEAM = "team";
    private static final String COUNT = "count";
    private static final String BUDGET = "budget";
    private static final String VALUE = "value";
    private static final String TEMPLATE = "template";
    private static final String TYPE = "type";
    private static final String MAP = "map";
    private static final String NAME = "name";
    private static final String NONE = "none";
    private static final String CURRENT = "current";
    private static final String CLASS = "class";
    private static final String RESPAWN = "respawn";
    private static final String TICKET_COST = "ticketcost";
    private static final String MEMBERS = "members";
    private static final String POSTURE = "posture";
    private static final SimpleCommandExceptionType NO_ROUND = new SimpleCommandExceptionType(Component.literal("No such round: use 'current' or an index from /teams listRotation"));
    private static final SimpleCommandExceptionType NO_TEAM = new SimpleCommandExceptionType(Component.literal("That team does not play this round"));
    private static final SimpleCommandExceptionType NO_TEMPLATE = new SimpleCommandExceptionType(Component.literal("No squad template has that id"));
    private static final SimpleCommandExceptionType NO_VEHICLE = new SimpleCommandExceptionType(Component.literal("That definition has no NPC model"));
    private static final SimpleCommandExceptionType NOT_A_PLAYER = new SimpleCommandExceptionType(Component.literal("Only a player can place markers"));

    private static final SuggestionProvider<CommandSourceStack> ROUNDS = (context, builder) ->
    {
        List<String> scopes = new ArrayList<>(List.of(CURRENT));
        for (int i = 0; i < FlansTeams.getRounds().size(); i++)
            scopes.add(Integer.toString(i));
        return SharedSuggestionProvider.suggest(scopes, builder);
    };
    private static final SuggestionProvider<CommandSourceStack> TEAMS = (context, builder) -> SharedSuggestionProvider
        .suggest(roundOrEmpty(context).map(round -> round.getTeams().stream().map(ITeam::getShortName).toList()).orElse(List.of()), builder);
    private static final SuggestionProvider<CommandSourceStack> CLASSES = (context, builder) -> SharedSuggestionProvider
        .suggest(roundOrEmpty(context).flatMap(round -> round.getTeams().stream().filter(team -> team.getShortName().equals(StringArgumentType.getString(context, TEAM))).findFirst())
            .map(team -> team.getClasses().stream().map(IContentType::getShortName).toList()).orElse(List.of()), builder);
    private static final SuggestionProvider<CommandSourceStack> NAME_LISTS = (context, builder) ->
    {
        List<String> lists = new ArrayList<>(SoldierNames.ids());
        lists.add(NONE);
        return SharedSuggestionProvider.suggest(lists, builder);
    };
    private static final SuggestionProvider<CommandSourceStack> VEHICLES = (context, builder) -> SharedSuggestionProvider
        .suggest(FlanModelEntities.getEntityTypes().stream().map(FlanModelEntityType::getShortName).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> TEMPLATES = (context, builder) -> SharedSuggestionProvider.suggest(SquadTemplates.get().all().stream().map(SquadTemplate::id).toList(),
        builder);
    private static final SuggestionProvider<CommandSourceStack> MAPS = (context, builder) -> SharedSuggestionProvider
        .suggest(FlansTeams.getRounds().stream().map(ITeamsRound::getMapId).distinct().toList(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> ai = Commands.literal("ai");
        ai.then(Commands.literal("board").executes(context -> order(context, true)));
        ai.then(Commands.literal("dismount").executes(context -> order(context, false)));
        ai.then(Commands.literal("status").requires(source -> source.hasPermission(2)).executes(BotCommands::status));
        ai.then(Commands.literal("clear").requires(source -> source.hasPermission(2)).executes(context ->
        {
            int removed = TeamsBots.clear(context.getSource().getServer());
            context.getSource().sendSuccess(() -> Component.literal("Removed " + removed + " bots; they come back while bots are enabled"), true);
            return removed;
        }));
        ai.then(Commands.literal(ROUND).requires(source -> source.hasPermission(2)).then(roundSettings()));
        ai.then(templates());
        ai.then(Commands.literal("marker").requires(source -> source.hasPermission(2)).then(markers()));
        dispatcher.register(Commands.literal("teams").then(ai));
    }

    // --- Round settings ---

    private static RequiredArgumentBuilder<CommandSourceStack, String> roundSettings()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> round = Commands.argument(ROUND, StringArgumentType.word()).suggests(ROUNDS);
        round.then(Commands.literal("show").executes(BotCommands::show));
        round.then(Commands.literal("enable").executes(context -> edit(context, settings -> settings.setEnabled(true), "Bots enabled")));
        round.then(Commands.literal("disable").executes(context -> edit(context, settings -> settings.setEnabled(false), "Bots disabled")));
        round.then(enumSetting("mode", BotSettings.Mode.class, BotSettings::setMode));
        round.then(enumSetting("nameplates", BotSettings.Nameplates.class, BotSettings::setNameplates));
        round.then(enumSetting("scoreboard", BotSettings.Scoreboard.class, BotSettings::setScoreboard));
        round.then(intSetting("total", 0, BotSettings::setTotal));
        round.then(intSetting(RESPAWN, 0, BotSettings::setRespawnSeconds));
        round.then(intSetting("tickets", 0, BotSettings::setTickets));
        round.then(intSetting("squadsize", 1, BotSettings::setSquadSize));
        round.then(Commands.literal("capture")
            .then(Commands.literal("infantry")
                .then(Commands.argument(VALUE, FloatArgumentType.floatArg(0F))
                    .executes(context -> edit(context, settings -> settings.setInfantryCaptureWeight(FloatArgumentType.getFloat(context, VALUE)), "Infantry capture weight set"))))
            .then(Commands.literal("vehicles").then(Commands.argument(VALUE, FloatArgumentType.floatArg(0F))
                .executes(context -> edit(context, settings -> settings.setVehicleCaptureWeight(FloatArgumentType.getFloat(context, VALUE)), "Vehicle capture weight set")))));
        round.then(Commands.literal(TEAM).then(teamSettings()));
        return round;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> teamSettings()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> team = Commands.argument(TEAM, StringArgumentType.word()).suggests(TEAMS);
        team.then(Commands.literal(COUNT).then(Commands.argument(VALUE, IntegerArgumentType.integer(0))
            .executes(context -> editTeam(context, force -> force.setCount(IntegerArgumentType.getInteger(context, VALUE)), "Infantry count set"))));
        team.then(Commands.literal(BUDGET).then(Commands.argument(VALUE, IntegerArgumentType.integer(0))
            .executes(context -> editTeam(context, force -> force.setBudget(IntegerArgumentType.getInteger(context, VALUE)), "Infantry budget set"))));
        team.then(Commands.literal("names")
            .then(Commands.argument(NAME, StringArgumentType.word()).suggests(NAME_LISTS).executes(context -> editTeam(context, force -> force.setNameList(word(context, NAME)), "Name list set"))));
        team.then(Commands.literal("clone").then(Commands.literal(NONE).executes(context -> editTeam(context, force -> force.setCloneTab(0), "Bots are made plain")))
            .then(Commands.argument("tab", IntegerArgumentType.integer(1, 15)).then(Commands.argument(NAME, StringArgumentType.greedyString()).executes(context -> editTeam(context, force ->
            {
                force.setCloneTab(IntegerArgumentType.getInteger(context, "tab"));
                force.setCloneName(StringArgumentType.getString(context, NAME));
            }, "Bots are made from that clone")))));
        team.then(Commands.literal(CLASS).then(Commands.argument(CLASS, StringArgumentType.word()).suggests(CLASSES).then(Commands.argument(VALUE, IntegerArgumentType.integer(0)).executes(
            context -> editTeam(context, force -> force.getClassWeights().put(StringArgumentType.getString(context, CLASS), IntegerArgumentType.getInteger(context, VALUE)), "Class weight set")))));
        team.then(Commands.literal("squad")
            .then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES).then(Commands.argument(COUNT, IntegerArgumentType.integer(0)).executes(context -> squad(context, 0))
                .then(Commands.argument(BUDGET, IntegerArgumentType.integer(0)).executes(context -> squad(context, IntegerArgumentType.getInteger(context, BUDGET)))))));
        team.then(Commands.literal("removesquad").then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES)
            .executes(context -> editTeam(context, force -> force.getSquads().removeIf(entry -> entry.template().equals(word(context, TEMPLATE))), "Squad entry removed"))));
        team.then(Commands.literal("vehicle")
            .then(Commands.argument(TYPE, StringArgumentType.word()).suggests(VEHICLES)
                .then(Commands.argument(COUNT, IntegerArgumentType.integer(0)).executes(context -> vehicle(context, 0, 0, 1))
                    .then(Commands.argument(BUDGET, IntegerArgumentType.integer(0)).executes(context -> vehicle(context, IntegerArgumentType.getInteger(context, BUDGET), 0, 1))
                        .then(Commands.argument(RESPAWN, IntegerArgumentType.integer(0))
                            .executes(context -> vehicle(context, IntegerArgumentType.getInteger(context, BUDGET), IntegerArgumentType.getInteger(context, RESPAWN), 1))
                            .then(Commands.argument(TICKET_COST, IntegerArgumentType.integer(0)).executes(context -> vehicle(context, IntegerArgumentType.getInteger(context, BUDGET),
                                IntegerArgumentType.getInteger(context, RESPAWN), IntegerArgumentType.getInteger(context, TICKET_COST)))))))));
        team.then(Commands.literal("removevehicle").then(Commands.argument(TYPE, StringArgumentType.word()).suggests(VEHICLES)
            .executes(context -> editTeam(context, force -> force.getVehicles().removeIf(entry -> entry.type().equals(StringArgumentType.getString(context, TYPE))), "Vehicle entry removed"))));
        return team;
    }

    private static <E extends Enum<E>> LiteralArgumentBuilder<CommandSourceStack> enumSetting(String name, Class<E> type, BiConsumer<BotSettings, E> setter)
    {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name);
        for (E constant : type.getEnumConstants())
            node.then(Commands.literal(constant.name().toLowerCase(Locale.ROOT))
                .executes(context -> edit(context, settings -> setter.accept(settings, constant), name + " set to " + constant.name().toLowerCase(Locale.ROOT))));
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intSetting(String name, int minimum, ObjIntConsumer<BotSettings> setter)
    {
        return Commands.literal(name).then(Commands.argument(VALUE, IntegerArgumentType.integer(minimum))
            .executes(context -> edit(context, settings -> setter.accept(settings, IntegerArgumentType.getInteger(context, VALUE)), name + " set")));
    }

    private static int squad(CommandContext<CommandSourceStack> context, int budget) throws CommandSyntaxException
    {
        String template = word(context, TEMPLATE);
        if (SquadTemplates.get().find(template).isEmpty())
            throw NO_TEMPLATE.create();
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return editTeam(context, force ->
        {
            List<BotSettings.SquadEntry> squads = force.getSquads();
            BotSettings.SquadEntry entry = new BotSettings.SquadEntry(template, count, budget);
            int index = squads.stream().map(BotSettings.SquadEntry::template).toList().indexOf(template);
            if (index < 0)
                squads.add(entry);
            else
                squads.set(index, entry);
        }, "Squad entry set");
    }

    private static int vehicle(CommandContext<CommandSourceStack> context, int budget, int respawn, int ticketCost) throws CommandSyntaxException
    {
        String type = StringArgumentType.getString(context, TYPE);
        if (FlanModelEntities.getEntityTypes().stream().noneMatch(model -> model.getShortName().equals(type)))
            throw NO_VEHICLE.create();
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return editTeam(context, force ->
        {
            List<BotSettings.VehicleEntry> vehicles = force.getVehicles();
            BotSettings.VehicleEntry entry = new BotSettings.VehicleEntry(type, count, budget, respawn, ticketCost, null);
            int index = vehicles.stream().map(BotSettings.VehicleEntry::type).toList().indexOf(type);
            if (index < 0)
                vehicles.add(entry);
            else
                vehicles.set(index, entry);
        }, "Vehicle entry set");
    }

    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int edit(CommandContext<CommandSourceStack> context, Consumer<BotSettings> change, String message) throws CommandSyntaxException
    {
        ITeamsRound round = round(context);
        BotSettings settings = BotSettings.load(round.getExtensionData(BotSettings.KEY));
        boolean fresh = round.getExtensionData(BotSettings.KEY).isEmpty();
        // The first setting given to a round switches its bots on, unless that setting is the switch itself.
        if (fresh)
            settings.setEnabled(true);
        change.accept(settings);
        FlansTeams.setRoundExtensionData(round, BotSettings.KEY, settings.save());
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int editTeam(CommandContext<CommandSourceStack> context, Consumer<BotSettings.TeamForce> change, String message) throws CommandSyntaxException
    {
        ITeamsRound round = round(context);
        String team = StringArgumentType.getString(context, TEAM);
        if (round.getTeams().stream().noneMatch(candidate -> candidate.getShortName().equals(team)))
            throw NO_TEAM.create();
        return edit(context, settings -> change.accept(settings.team(team)), message);
    }

    /** A word argument, with {@code none} meaning empty. */
    private static String word(CommandContext<CommandSourceStack> context, String name)
    {
        String value = StringArgumentType.getString(context, name);
        return NONE.equalsIgnoreCase(value) ? "" : value;
    }

    private static ITeamsRound round(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        return roundOrEmpty(context).orElseThrow(NO_ROUND::create);
    }

    private static Optional<ITeamsRound> roundOrEmpty(CommandContext<CommandSourceStack> context)
    {
        String scope = StringArgumentType.getString(context, ROUND);
        if (CURRENT.equalsIgnoreCase(scope))
            return FlansTeams.getRunningRound().map(ITeamsLiveRound::getRound);
        try
        {
            int index = Integer.parseInt(scope);
            List<ITeamsRound> rounds = FlansTeams.getRounds();
            return index >= 0 && index < rounds.size() ? Optional.of(rounds.get(index)) : Optional.empty();
        }
        catch (NumberFormatException e)
        {
            return Optional.empty();
        }
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ITeamsRound round = round(context);
        BotSettings settings = BotSettings.load(round.getExtensionData(BotSettings.KEY));
        List<String> lines = new ArrayList<>();
        lines.add("Bots " + (settings.isEnabled() ? "enabled" : "disabled") + " on " + round.getGameTypeId() + " @ " + round.getMapId() + ": mode " + lower(settings.getMode()) + ", total "
            + settings.getTotal() + ", respawn " + settings.getRespawnSeconds() + "s, tickets " + (settings.getTickets() == 0 ? "unlimited" : settings.getTickets()) + ", squads of "
            + settings.getSquadSize());
        lines.add("Name tags " + lower(settings.getNameplates()) + ", scoreboard " + lower(settings.getScoreboard()) + ", capture weight " + settings.getInfantryCaptureWeight() + " / vehicles "
            + settings.getVehicleCaptureWeight());
        for (ITeam team : round.getTeams())
            lines.addAll(describe(team, settings.team(team.getShortName())));
        lines.forEach(line -> context.getSource().sendSuccess(() -> Component.literal(line), false));
        return lines.size();
    }

    /** One line for a team's infantry, and one for each of its squad and vehicle entries. */
    private static List<String> describe(ITeam team, BotSettings.TeamForce force)
    {
        List<String> lines = new ArrayList<>();
        String names = force.getNameList().isEmpty() ? NONE : force.getNameList();
        String clone = force.usesClone() ? ", clone " + force.getCloneTab() + "/" + force.getCloneName() : "";
        String classes = force.getClassWeights().isEmpty() ? "all" : force.getClassWeights().toString();
        lines.add(team.getShortName() + ": infantry " + force.getCount() + budget(force.getBudget()) + ", names " + names + clone + ", classes " + classes);
        force.getSquads().forEach(entry -> lines.add("  squad " + entry.template() + " x" + entry.count() + budget(entry.budget())));
        for (BotSettings.VehicleEntry entry : force.getVehicles())
        {
            String respawn = entry.respawnSeconds() > 0 ? ", respawn " + entry.respawnSeconds() + "s" : "";
            lines.add("  vehicle " + entry.type() + " x" + entry.count() + budget(entry.budget()) + respawn + ", " + entry.ticketCost() + " tickets");
        }
        return lines;
    }

    private static String budget(int budget)
    {
        return budget > 0 ? " (budget " + budget + ")" : "";
    }

    private static int status(CommandContext<CommandSourceStack> context)
    {
        Optional<BotRound> current = TeamsBots.current();
        if (current.isEmpty())
        {
            context.getSource().sendSuccess(() -> Component.literal("No round is being played"), false);
            return 0;
        }
        BotRound round = current.get();
        for (ITeam team : round.getLive().getRound().getTeams())
        {
            long alive = round.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && slot.getEntity() != null && slot.getDiedAt() == 0).count();
            long slots = round.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && !slot.isRetired()).count();
            int side = round.getLive().getRound().getSideOf(team);
            String tickets = round.getSettings().getTickets() > 0 ? ", side tickets " + BotPopulation.ticketsLeft(round, side) : "";
            List<String> tasks = round.getSquads().values().stream().filter(squad -> squad.getTeam().equals(team.getShortName()) && squad.getTask() != null)
                .map(squad -> "#" + squad.getId() + " " + lower(squad.getRole()) + " " + lower(squad.getTask().kind())).toList();
            context.getSource().sendSuccess(() -> Component.literal(team.getShortName() + ": " + alive + "/" + slots + " bots alive" + tickets + "; squads " + tasks), false);
        }
        return round.getSlots().size();
    }

    private static int order(CommandContext<CommandSourceStack> context, boolean board) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int count = board ? BotGunners.board(player) : BotGunners.dismount(player);
        if (count < 0)
            context.getSource().sendFailure(Component.literal(board ? "Drive a driveable to have bots board it" : "Ride a driveable to have bots leave it"));
        else
            context.getSource().sendSuccess(() -> Component.literal(count + (board ? " bots boarded" : " bots got off")), false);
        return Math.max(0, count);
    }

    // --- Squad templates ---

    private static LiteralArgumentBuilder<CommandSourceStack> templates()
    {
        LiteralArgumentBuilder<CommandSourceStack> templates = Commands.literal(TEMPLATE).requires(source -> source.hasPermission(2));
        templates.then(Commands.literal("list").executes(context ->
        {
            Collection<SquadTemplate> all = SquadTemplates.get().all();
            all.forEach(template -> context.getSource().sendSuccess(() -> Component.literal(
                template.id() + " (" + template.name() + "), " + lower(template.posture()) + ": " + template.members().stream().map(member -> member.playerClass() + " x" + member.count()).toList()),
                false));
            return all.size();
        }));
        return templateEditing(templates);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> templateEditing(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(Commands.literal("create").then(Commands.argument(TEMPLATE, StringArgumentType.word()).then(Commands.argument(NAME, StringArgumentType.greedyString()).executes(context ->
        {
            String id = StringArgumentType.getString(context, TEMPLATE).toLowerCase(Locale.ROOT);
            SquadTemplate existing = SquadTemplates.get().find(id).orElse(null);
            SquadTemplates.get().put(new SquadTemplate(id, StringArgumentType.getString(context, NAME), existing == null ? SquadTemplate.Posture.BALANCED : existing.posture(),
                existing == null ? "" : existing.nameList(), existing == null ? List.of() : existing.members()));
            context.getSource().sendSuccess(() -> Component.literal("Squad template " + id + " saved"), true);
            return 1;
        }))));
        root.then(Commands.literal(MEMBERS).then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES)
            .then(Commands.argument(MEMBERS, StringArgumentType.greedyString()).executes(context -> editTemplate(context, template ->
            {
                List<SquadTemplate.Member> members = parseMembers(StringArgumentType.getString(context, MEMBERS));
                return template.withMembers(members);
            })))));
        root.then(Commands.literal(POSTURE)
            .then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES)
                .then(Commands.argument(POSTURE, StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(SquadTemplate.Posture.values()).map(BotCommands::lower).toList(), builder))
                    .executes(context -> editTemplate(context,
                        template -> template.withPosture(BotSettings.parse(SquadTemplate.Posture.class, StringArgumentType.getString(context, POSTURE), SquadTemplate.Posture.BALANCED)))))));
        root.then(Commands.literal("names").then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES)
            .then(Commands.argument(NAME, StringArgumentType.word()).suggests(NAME_LISTS).executes(context -> editTemplate(context, template -> template.withNameList(word(context, NAME)))))));
        root.then(Commands.literal("remove").then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES).executes(context ->
        {
            boolean removed = SquadTemplates.get().remove(StringArgumentType.getString(context, TEMPLATE));
            if (!removed)
                throw NO_TEMPLATE.create();
            context.getSource().sendSuccess(() -> Component.literal("Squad template removed"), true);
            return 1;
        })));
        return root;
    }

    // A command reports how many templates it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int editTemplate(CommandContext<CommandSourceStack> context, UnaryOperator<SquadTemplate> change) throws CommandSyntaxException
    {
        SquadTemplate template = SquadTemplates.get().find(StringArgumentType.getString(context, TEMPLATE)).orElseThrow(NO_TEMPLATE::create);
        SquadTemplates.get().put(change.apply(template));
        context.getSource().sendSuccess(() -> Component.literal("Squad template " + template.id() + " saved"), true);
        return 1;
    }

    /** Reads members written as {@code class:count} pairs separated by commas or spaces; a pair without a count counts one. */
    static List<SquadTemplate.Member> parseMembers(String text)
    {
        List<SquadTemplate.Member> members = new ArrayList<>();
        for (String part : text.split("[,\\s]+"))
        {
            if (part.isBlank())
                continue;
            String[] pair = part.split(":", 2);
            members.add(new SquadTemplate.Member(pair[0], pair.length == 2 ? count(pair[1]) : 1));
        }
        return members;
    }

    /** A member count, one when it is not a number. */
    private static int count(String text)
    {
        try
        {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e)
        {
            return 1;
        }
    }

    // --- Markers ---

    private static RequiredArgumentBuilder<CommandSourceStack, String> markers()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> map = Commands.argument(MAP, StringArgumentType.word()).suggests(MAPS);
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("add");
        for (BotMarkers.Type type : BotMarkers.Type.values())
            add.then(Commands.literal(lower(type)).executes(context ->
            {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player))
                    throw NOT_A_PLAYER.create();
                BotMarkers.get(context.getSource().getServer()).add(StringArgumentType.getString(context, MAP), new BotMarkers.Marker(type, player.position()));
                context.getSource().sendSuccess(() -> Component.literal(lower(type) + " marker placed"), true);
                return 1;
            }));
        map.then(add);
        map.then(Commands.literal("remove").executes(context ->
        {
            Optional<BotMarkers.Marker> removed = BotMarkers.get(context.getSource().getServer()).removeNearest(StringArgumentType.getString(context, MAP), context.getSource().getPosition(), 8D);
            context.getSource().sendSuccess(() -> Component.literal(removed.map(marker -> lower(marker.type()) + " marker removed").orElse("No marker within 8 blocks")), true);
            return removed.isPresent() ? 1 : 0;
        }));
        map.then(Commands.literal("list").executes(context ->
        {
            List<BotMarkers.Marker> markers = BotMarkers.get(context.getSource().getServer()).of(StringArgumentType.getString(context, MAP));
            markers.forEach(marker -> context.getSource()
                .sendSuccess(() -> Component.literal(lower(marker.type()) + " at " + (int) marker.position().x + " " + (int) marker.position().y + " " + (int) marker.position().z), false));
            return markers.size();
        }));
        map.then(Commands.literal("clear").executes(context ->
        {
            int removed = BotMarkers.get(context.getSource().getServer()).clear(StringArgumentType.getString(context, MAP));
            context.getSource().sendSuccess(() -> Component.literal("Removed " + removed + " markers"), true);
            return removed;
        }));
        return map;
    }

    private static String lower(Enum<?> constant)
    {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
