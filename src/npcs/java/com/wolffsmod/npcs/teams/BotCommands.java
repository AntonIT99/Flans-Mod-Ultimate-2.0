package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsLiveRound;
import com.flansmodultimate.api.ITeamsRound;
import com.flansmodultimate.api.TeamsCapturePoint;
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
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
    private static final String VALUE = "value";
    private static final String TEMPLATE = "template";
    private static final String MAP = "map";
    private static final String NAME = "name";
    private static final String NAMES = "names";
    static final String NONE = "none";
    private static final String CURRENT = "current";
    private static final String RESPAWN = "respawn";
    private static final String MEMBERS = "members";
    private static final String POSTURE = "posture";
    private static final String POINT = "point";
    private static final String SOURCE = "source";
    private static final String REMOVE = "remove";
    private static final String PREFIX = "wolffsmodnpcs.command.";
    private static final String SET = "set";
    /** Blocks within which a marker is removed. */
    private static final double MARKER_REACH = 8D;
    private static final SimpleCommandExceptionType NO_ROUND = new SimpleCommandExceptionType(text("no_round"));
    private static final SimpleCommandExceptionType NO_TEAM = new SimpleCommandExceptionType(text("no_team"));
    private static final SimpleCommandExceptionType NOT_A_PLAYER = new SimpleCommandExceptionType(text("not_a_player"));
    private static final SimpleCommandExceptionType NO_PRESET = new SimpleCommandExceptionType(text("preset.unknown"));
    private static final SimpleCommandExceptionType BAD_PRESET_NAME = new SimpleCommandExceptionType(text("preset.bad_name"));

    static final SuggestionProvider<CommandSourceStack> ROUNDS = (context, builder) ->
    {
        List<String> scopes = new ArrayList<>(List.of(CURRENT));
        for (int i = 0; i < FlansTeams.getRounds().size(); i++)
            scopes.add(Integer.toString(i));
        return SharedSuggestionProvider.suggest(scopes, builder);
    };
    private static final SuggestionProvider<CommandSourceStack> TEAMS = (context, builder) -> SharedSuggestionProvider
        .suggest(roundOrEmpty(context).map(round -> round.getTeams().stream().map(ITeam::getShortName).toList()).orElse(List.of()), builder);
    private static final SuggestionProvider<CommandSourceStack> POINTS = (context, builder) -> SharedSuggestionProvider
        .suggest(FlansTeams.getRunningRound().map(live -> live.getCapturePoints().stream().map(point -> String.valueOf(point.symbol())).toList()).orElse(List.of()), builder);
    private static final SuggestionProvider<CommandSourceStack> PRESETS = (context, builder) -> SharedSuggestionProvider.suggest(BotPresets.get().names(), builder);
    private static final SuggestionProvider<CommandSourceStack> MAPS = (context, builder) -> SharedSuggestionProvider
        .suggest(FlansTeams.getRounds().stream().map(ITeamsRound::getMapId).distinct().toList(), builder);
    /** A team of the round the command names, whose force is kept with the round. */
    private static final TeamForceCommands.Target ROUND_TEAM = new TeamForceCommands.Target()
    {
        @Override
        public Optional<ITeam> team(CommandContext<CommandSourceStack> context)
        {
            return roundOrEmpty(context).flatMap(round -> round.getTeams().stream().filter(team -> team.getShortName().equals(StringArgumentType.getString(context, TEAM))).findFirst());
        }

        @Override
        public int edit(CommandContext<CommandSourceStack> context, Consumer<BotSettings.TeamForce> change, Component message) throws CommandSyntaxException
        {
            return editTeam(context, change, message);
        }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> ai = Commands.literal("ai");
        ai.then(Commands.literal("board").executes(context -> order(context, true)));
        ai.then(Commands.literal("dismount").executes(context -> order(context, false)));
        ai.then(orders());
        ai.then(Commands.literal("squad").then(Commands.literal("lead").executes(context -> tell(context, BotLeaders.lead(context.getSource().getPlayerOrException(), BotOrders.NEARBY_SQUADS))))
            .then(Commands.literal("leave").executes(context -> tell(context, BotLeaders.leave(context.getSource().getPlayerOrException())))));
        ai.then(Commands.literal("rally").executes(context -> tell(context, BotLeaders.rally(context.getSource().getPlayerOrException()))));
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn");
        for (BotLeaders.SpawnChoice choice : BotLeaders.SpawnChoice.values())
            spawn.then(Commands.literal(lower(choice)).executes(context -> tell(context, BotLeaders.chooseSpawn(context.getSource().getPlayerOrException(), choice))));
        ai.then(spawn);
        ai.then(Commands.literal("commander").then(Commands.literal("take").executes(context -> command(context, true))).then(Commands.literal("leave").executes(context -> command(context, false))));
        ai.then(Commands.literal("editor").requires(source -> source.hasPermission(2)).executes(context -> editor(context, -1))
            .then(Commands.argument(ROUND, IntegerArgumentType.integer(0)).executes(context -> editor(context, IntegerArgumentType.getInteger(context, ROUND)))));
        ai.then(Commands.literal("status").requires(source -> source.hasPermission(2)).executes(BotCommands::status));
        ai.then(Commands.literal("clear").requires(source -> source.hasPermission(2)).executes(context ->
        {
            int removed = TeamsBots.clear(context.getSource().getServer());
            context.getSource().sendSuccess(() -> text("cleared", removed), true);
            return removed;
        }));
        ai.then(Commands.literal(ROUND).requires(source -> source.hasPermission(2)).then(roundSettings()));
        ai.then(templates());
        ai.then(presets());
        ai.then(Commands.literal("marker").requires(source -> source.hasPermission(2)).then(markers()));
        dispatcher.register(Commands.literal("teams").then(ai));
    }

    // --- Round settings ---

    private static RequiredArgumentBuilder<CommandSourceStack, String> roundSettings()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> round = Commands.argument(ROUND, StringArgumentType.word()).suggests(ROUNDS);
        round.then(Commands.literal("show").executes(BotCommands::show));
        round.then(Commands.literal("enable").executes(context -> edit(context, settings -> settings.setEnabled(true), text("enabled"))));
        round.then(Commands.literal("disable").executes(context -> edit(context, settings -> settings.setEnabled(false), text("disabled"))));
        round.then(enumSetting("mode", BotSettings.Mode.class, BotSettings::setMode));
        round.then(enumSetting("nameplates", BotSettings.Nameplates.class, BotSettings::setNameplates));
        round.then(enumSetting("scoreboard", BotSettings.Scoreboard.class, BotSettings::setScoreboard));
        round.then(enumSetting("difficulty", BotSettings.Difficulty.class, BotSettings::setDifficulty));
        round.then(switchSetting("playertickets", BotSettings::setPlayerTickets));
        round.then(switchSetting("radio", BotSettings::setRadio));
        round.then(switchSetting("suppression", BotSettings::setSuppression));
        round.then(switchSetting("rallypoints", BotSettings::setRallyPoints));
        round.then(switchSetting("vehiclecrews", BotSettings::setVehicleCrews));
        round.then(intSetting("retreat", 0, BotSettings::setRetreatHealth));
        round.then(Commands.literal("copyfrom").then(Commands.argument(SOURCE, StringArgumentType.word()).suggests(ROUNDS).executes(BotCommands::copyFrom)));
        round.then(intSetting("total", 0, BotSettings::setTotal));
        round.then(intSetting(RESPAWN, 0, BotSettings::setRespawnSeconds));
        round.then(intSetting("tickets", 0, BotSettings::setTickets));
        round.then(intSetting("squadsize", 1, BotSettings::setSquadSize));
        round.then(intSetting("abandon", 0, BotSettings::setAbandonChance));
        round.then(intSetting("rubberband", 0, BotSettings::setRubberBand));
        round.then(Commands.literal("capture")
            .then(Commands.literal("infantry")
                .then(Commands.argument(VALUE, FloatArgumentType.floatArg(0F))
                    .executes(context -> edit(context, settings -> settings.setInfantryCaptureWeight(FloatArgumentType.getFloat(context, VALUE)),
                        text(SET, "capture infantry", FloatArgumentType.getFloat(context, VALUE))))))
            .then(Commands.literal("vehicles").then(Commands.argument(VALUE, FloatArgumentType.floatArg(0F)).executes(context -> edit(context,
                settings -> settings.setVehicleCaptureWeight(FloatArgumentType.getFloat(context, VALUE)), text(SET, "capture vehicles", FloatArgumentType.getFloat(context, VALUE)))))));
        round.then(Commands.literal(TEAM).then(TeamForceCommands.build(Commands.argument(TEAM, StringArgumentType.word()).suggests(TEAMS), ROUND_TEAM)));
        return round;
    }

    private static <E extends Enum<E>> LiteralArgumentBuilder<CommandSourceStack> enumSetting(String name, Class<E> type, BiConsumer<BotSettings, E> setter)
    {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name);
        for (E constant : type.getEnumConstants())
            node.then(Commands.literal(constant.name().toLowerCase(Locale.ROOT)).executes(context -> edit(context, settings -> setter.accept(settings, constant), text(SET, name, lower(constant)))));
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> switchSetting(String name, BiConsumer<BotSettings, Boolean> setter)
    {
        return Commands.literal(name).then(Commands.literal("on").executes(context -> edit(context, settings -> setter.accept(settings, true), text(SET, name, onOff(true)))))
            .then(Commands.literal("off").executes(context -> edit(context, settings -> setter.accept(settings, false), text(SET, name, onOff(false)))));
    }

    /** Copies another round's bot settings over this one's, team by team, as presets carry them over. */
    // A command reports how many rounds it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int copyFrom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ITeamsRound target = round(context);
        ITeamsRound source = roundNamed(StringArgumentType.getString(context, SOURCE)).orElseThrow(NO_ROUND::create);
        BotSettings copied = BotPresets.carryOver(source.getExtensionData(BotSettings.KEY), teams(source), teams(target));
        FlansTeams.setRoundExtensionData(target, BotSettings.KEY, copied.save());
        context.getSource().sendSuccess(() -> text("copied", source.getGameTypeId() + " @ " + source.getMapId()), true);
        return 1;
    }

    private static List<String> teams(ITeamsRound round)
    {
        return round.getTeams().stream().map(ITeam::getShortName).toList();
    }

    // --- Presets ---

    private static LiteralArgumentBuilder<CommandSourceStack> presets()
    {
        LiteralArgumentBuilder<CommandSourceStack> presets = Commands.literal("preset").requires(source -> source.hasPermission(2));
        presets.then(Commands.literal("list").executes(context ->
        {
            Collection<String> names = BotPresets.get().names();
            context.getSource().sendSuccess(() -> names.isEmpty() ? text("preset.none") : text("preset.list", String.join(", ", names)), false);
            return names.size();
        }));
        presets.then(Commands.literal("save").then(Commands.argument(NAME, StringArgumentType.word()).then(Commands.argument(ROUND, StringArgumentType.word()).suggests(ROUNDS).executes(context ->
        {
            String name = StringArgumentType.getString(context, NAME).toLowerCase(Locale.ROOT);
            if (BotPresets.invalidName(name))
                throw BAD_PRESET_NAME.create();
            ITeamsRound round = round(context);
            BotPresets.get().put(new BotPresets.Preset(name, teams(round), round.getExtensionData(BotSettings.KEY)));
            context.getSource().sendSuccess(() -> text("preset.saved", name), true);
            return 1;
        }))));
        presets.then(Commands.literal("apply")
            .then(Commands.argument(NAME, StringArgumentType.word()).suggests(PRESETS).then(Commands.literal("all").executes(context -> applyPreset(context, FlansTeams.getRounds())))
                .then(Commands.argument(ROUND, StringArgumentType.word()).suggests(ROUNDS).executes(context -> applyPreset(context, List.of(round(context)))))));
        presets.then(Commands.literal(REMOVE).then(Commands.argument(NAME, StringArgumentType.word()).suggests(PRESETS).executes(context ->
        {
            if (!BotPresets.get().remove(StringArgumentType.getString(context, NAME)))
                throw NO_PRESET.create();
            context.getSource().sendSuccess(() -> text("preset.removed"), true);
            return 1;
        })));
        return presets;
    }

    private static int applyPreset(CommandContext<CommandSourceStack> context, List<ITeamsRound> rounds) throws CommandSyntaxException
    {
        BotPresets.Preset preset = BotPresets.get().find(StringArgumentType.getString(context, NAME)).orElseThrow(NO_PRESET::create);
        for (ITeamsRound round : rounds)
            FlansTeams.setRoundExtensionData(round, BotSettings.KEY, BotPresets.carryOver(preset.settings(), preset.teams(), teams(round)).save());
        context.getSource().sendSuccess(() -> text("preset.applied", preset.name(), rounds.size()), true);
        return rounds.size();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intSetting(String name, int minimum, ObjIntConsumer<BotSettings> setter)
    {
        return Commands.literal(name).then(Commands.argument(VALUE, IntegerArgumentType.integer(minimum))
            .executes(context -> edit(context, settings -> setter.accept(settings, IntegerArgumentType.getInteger(context, VALUE)), text(SET, name, IntegerArgumentType.getInteger(context, VALUE)))));
    }

    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int edit(CommandContext<CommandSourceStack> context, Consumer<BotSettings> change, Component message) throws CommandSyntaxException
    {
        ITeamsRound round = round(context);
        BotSettings settings = BotSettings.load(round.getExtensionData(BotSettings.KEY));
        boolean fresh = round.getExtensionData(BotSettings.KEY).isEmpty();
        // The first setting given to a round switches its bots on, unless that setting is the switch itself.
        if (fresh)
            settings.setEnabled(true);
        change.accept(settings);
        FlansTeams.setRoundExtensionData(round, BotSettings.KEY, settings.save());
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    private static int editTeam(CommandContext<CommandSourceStack> context, Consumer<BotSettings.TeamForce> change, Component message) throws CommandSyntaxException
    {
        ITeamsRound round = round(context);
        String team = StringArgumentType.getString(context, TEAM);
        if (round.getTeams().stream().noneMatch(candidate -> candidate.getShortName().equals(team)))
            throw NO_TEAM.create();
        return edit(context, settings -> change.accept(settings.team(team)), message);
    }

    /** A word argument, with {@code none} meaning empty. */
    static String word(CommandContext<CommandSourceStack> context, String name)
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
        return roundNamed(StringArgumentType.getString(context, ROUND));
    }

    /** A round by {@code current} or by its index in the rotation. */
    static Optional<ITeamsRound> roundNamed(String scope)
    {
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
        List<Component> lines = new ArrayList<>();
        Component tickets = settings.getTickets() == 0 ? text("show.unlimited") : Component.literal(Integer.toString(settings.getTickets()));
        lines.add(text(settings.isEnabled() ? "show.round_enabled" : "show.round_disabled", round.getGameTypeId(), round.getMapId(), lower(settings.getMode()), settings.getTotal(),
            settings.getRespawnSeconds(), tickets, settings.getSquadSize()));
        lines.add(text("show.look", lower(settings.getNameplates()), lower(settings.getScoreboard()), settings.getInfantryCaptureWeight(), settings.getVehicleCaptureWeight(),
            lower(settings.getDifficulty()), onOff(settings.isPlayerTickets())));
        lines.add(text("show.rules", onOff(settings.isRadio()), onOff(settings.isSuppression()), settings.getRetreatHealth(), onOff(settings.isRallyPoints())));
        lines.add(text("show.crews", onOff(settings.isVehicleCrews())));
        for (ITeam team : round.getTeams())
            lines.addAll(describe(team, settings.team(team.getShortName())));
        BotEditor.problems(round, settings).forEach(problem -> lines.add(problem.copy().withStyle(ChatFormatting.YELLOW)));
        lines.forEach(line -> context.getSource().sendSuccess(() -> line, false));
        return lines.size();
    }

    /** One line for a team's infantry, and one for each of its squad and vehicle entries. */
    private static List<Component> describe(ITeam team, BotSettings.TeamForce force)
    {
        List<Component> lines = new ArrayList<>();
        String names = force.getNameList().isEmpty() ? NONE : force.getNameList();
        String cloneKey = force.isKeepCloneEquipment() ? "show.clone_equipped" : "show.clone";
        Component clone = force.usesClone() ? text(cloneKey, force.getCloneTab(), force.getCloneName()) : Component.empty();
        Component classes = force.getClassWeights().isEmpty() ? text("show.all_classes") : Component.literal(force.getClassWeights().toString());
        lines.add(text("show.team", team.getShortName(), force.getCount(), budget(force.getBudget()), names, clone, classes));
        if (!force.getClassRoles().isEmpty())
            lines.add(text("show.roles", BotSettingsText.formatRoles(force.getClassRoles())));
        lines.add(text("show.crew", BotSettingsText.formatCrew(force)));
        force.getSquads().forEach(entry -> lines.add(text("show.squad", entry.template(), entry.count(), budget(entry.budget()))));
        for (BotSettings.VehicleEntry entry : force.getVehicles())
        {
            Component respawn = entry.respawnSeconds() > 0 ? text("show.respawn", entry.respawnSeconds()) : Component.empty();
            Component carry = entry.carry().isEmpty() ? Component.empty() : text("show.carry", entry.carry());
            lines.add(text("show.vehicle", entry.type(), entry.count(), budget(entry.budget()), respawn, entry.ticketCost(), carry));
        }
        return lines;
    }

    private static Component budget(int budget)
    {
        return budget > 0 ? text("show.budget", budget) : Component.empty();
    }

    private static String onOff(boolean value)
    {
        return value ? "on" : "off";
    }

    private static int status(CommandContext<CommandSourceStack> context)
    {
        Optional<BotBattle> current = TeamsBots.current();
        if (current.isEmpty())
        {
            context.getSource().sendSuccess(() -> text("status.none"), false);
            return 0;
        }
        BotBattle battle = current.get();
        for (ITeam team : battle.getGround().teams())
        {
            long alive = battle.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && slot.getEntity() != null && slot.getDiedAt() == 0).count();
            long slots = battle.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && !slot.isRetired()).count();
            int side = battle.getGround().sideOf(team);
            Component tickets = battle.getSettings().getTickets() > 0 ? text("status.tickets", BotPopulation.ticketsLeft(battle, side)) : Component.empty();
            List<String> tasks = battle.getSquads().values().stream().filter(squad -> squad.getTeam().equals(team.getShortName()) && squad.getTask() != null)
                .map(squad -> "#" + squad.getId() + " " + lower(squad.getRole()) + " " + lower(squad.getTask().kind())).toList();
            context.getSource().sendSuccess(() -> text("status.team", team.getShortName(), alive, slots, tickets, tasks.toString()), false);
        }
        return battle.getSlots().size();
    }

    /** {@code order follow|hold|release} and {@code order attack|defend <point>}, for the allied squads around the player. */
    private static LiteralArgumentBuilder<CommandSourceStack> orders()
    {
        LiteralArgumentBuilder<CommandSourceStack> order = Commands.literal("order");
        for (PlayerOrder.Kind kind : List.of(PlayerOrder.Kind.FOLLOW, PlayerOrder.Kind.HOLD, PlayerOrder.Kind.RELEASE))
            order.then(Commands.literal(lower(kind)).executes(context -> giveOrder(context, kind, BotOrders.HERE)));
        for (PlayerOrder.Kind kind : List.of(PlayerOrder.Kind.ATTACK, PlayerOrder.Kind.DEFEND))
            order.then(Commands.literal(lower(kind)).executes(context -> giveOrder(context, kind, BotOrders.HERE))
                .then(Commands.argument(POINT, StringArgumentType.word()).suggests(POINTS).executes(context -> giveOrder(context, kind, pointIndex(context)))));
        return order;
    }

    private static int giveOrder(CommandContext<CommandSourceStack> context, PlayerOrder.Kind kind, int objective) throws CommandSyntaxException
    {
        int ordered = BotOrders.give(context.getSource().getPlayerOrException(), BotOrders.NEARBY_SQUADS, kind, objective, null);
        if (ordered == 0)
            context.getSource().sendFailure(text("order.nobody", (int) BotOrders.NEARBY));
        else
            context.getSource().sendSuccess(() -> text("order.given", ordered, lower(kind)), false);
        return ordered;
    }

    /** The capture point a letter names, or the player's own position for a letter no point has. */
    private static int pointIndex(CommandContext<CommandSourceStack> context)
    {
        String letter = StringArgumentType.getString(context, POINT);
        List<TeamsCapturePoint> points = FlansTeams.getRunningRound().map(ITeamsLiveRound::getCapturePoints).orElse(List.of());
        for (int i = 0; i < points.size(); i++)
            if (String.valueOf(points.get(i).symbol()).equalsIgnoreCase(letter))
                return i;
        return BotOrders.HERE;
    }

    private static int command(CommandContext<CommandSourceStack> context, boolean take) throws CommandSyntaxException
    {
        boolean holds = BotOrders.command(context.getSource().getPlayerOrException(), take);
        String message;
        if (take)
            message = holds ? "commander.took" : "commander.refused";
        else
            message = "commander.left";
        context.getSource().sendSuccess(() -> text(message), false);
        return holds ? 1 : 0;
    }

    // A command reports how many editors it opened, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int editor(CommandContext<CommandSourceStack> context, int round) throws CommandSyntaxException
    {
        BotEditor.open(context.getSource().getPlayerOrException(), round);
        return 1;
    }

    private static int order(CommandContext<CommandSourceStack> context, boolean board) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int count = board ? BotGunners.board(player) : BotGunners.dismount(player);
        if (count < 0)
            context.getSource().sendFailure(text(board ? "board.no_driveable" : "dismount.no_driveable"));
        else
            context.getSource().sendSuccess(() -> text(board ? "board.done" : "dismount.done", count), false);
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
            context.getSource().sendSuccess(() -> text("template.saved", id), true);
            return 1;
        }))));
        root.then(Commands.literal(MEMBERS).then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TeamForceCommands.TEMPLATES)
            .then(Commands.argument(MEMBERS, StringArgumentType.greedyString()).executes(context -> editTemplate(context, template ->
            {
                List<SquadTemplate.Member> members = BotSettingsText.parseMembers(StringArgumentType.getString(context, MEMBERS));
                return template.withMembers(members);
            })))));
        root.then(Commands.literal(POSTURE)
            .then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TeamForceCommands.TEMPLATES)
                .then(Commands.argument(POSTURE, StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(SquadTemplate.Posture.values()).map(BotCommands::lower).toList(), builder))
                    .executes(context -> editTemplate(context,
                        template -> template.withPosture(BotSettings.parse(SquadTemplate.Posture.class, StringArgumentType.getString(context, POSTURE), SquadTemplate.Posture.BALANCED)))))));
        root.then(Commands.literal(NAMES).then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TeamForceCommands.TEMPLATES).then(Commands.argument(NAME, StringArgumentType.word())
            .suggests(TeamForceCommands.NAME_LISTS).executes(context -> editTemplate(context, template -> template.withNameList(word(context, NAME)))))));
        root.then(Commands.literal(REMOVE).then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TeamForceCommands.TEMPLATES).executes(context ->
        {
            boolean removed = SquadTemplates.get().remove(StringArgumentType.getString(context, TEMPLATE));
            if (!removed)
                throw TeamForceCommands.NO_TEMPLATE.create();
            context.getSource().sendSuccess(() -> text("template.removed"), true);
            return 1;
        })));
        return root;
    }

    // A command reports how many templates it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int editTemplate(CommandContext<CommandSourceStack> context, UnaryOperator<SquadTemplate> change) throws CommandSyntaxException
    {
        SquadTemplate template = SquadTemplates.get().find(StringArgumentType.getString(context, TEMPLATE)).orElseThrow(TeamForceCommands.NO_TEMPLATE::create);
        SquadTemplates.get().put(change.apply(template));
        context.getSource().sendSuccess(() -> text("template.saved", template.id()), true);
        return 1;
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
                context.getSource().sendSuccess(() -> text("marker.placed", lower(type)), true);
                return 1;
            }));
        map.then(add);
        map.then(Commands.literal(REMOVE).executes(context ->
        {
            Optional<BotMarkers.Marker> removed = BotMarkers.get(context.getSource().getServer()).removeNearest(StringArgumentType.getString(context, MAP), context.getSource().getPosition(),
                MARKER_REACH);
            context.getSource().sendSuccess(() -> removed.map(marker -> text("marker.removed", lower(marker.type()))).orElseGet(() -> text("marker.none_near", (int) MARKER_REACH)), true);
            return removed.isPresent() ? 1 : 0;
        }));
        map.then(Commands.literal("list").executes(context ->
        {
            List<BotMarkers.Marker> markers = BotMarkers.get(context.getSource().getServer()).of(StringArgumentType.getString(context, MAP));
            markers.forEach(
                marker -> context.getSource().sendSuccess(() -> text("marker.at", lower(marker.type()), (int) marker.position().x, (int) marker.position().y, (int) marker.position().z), false));
            return markers.size();
        }));
        map.then(Commands.literal("clear").executes(context ->
        {
            int removed = BotMarkers.get(context.getSource().getServer()).clear(StringArgumentType.getString(context, MAP));
            context.getSource().sendSuccess(() -> text("marker.cleared", removed), true);
            return removed;
        }));
        map.then(Commands.literal("show").executes(context ->
        {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String mapId = StringArgumentType.getString(context, MAP);
            BotMarkerDisplay.show(player, mapId);
            int count = BotMarkers.get(context.getSource().getServer()).of(mapId).size();
            context.getSource().sendSuccess(() -> text("marker.shown", count, BotMarkerDisplay.SHOW_TICKS / 20), false);
            return count;
        }));
        return map;
    }

    // A command reports one action done, which is always one here; failures are told in the message.
    @SuppressWarnings("SameReturnValue")
    private static int tell(CommandContext<CommandSourceStack> context, Component message)
    {
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    static Component text(String key, Object... args)
    {
        return Component.translatable(PREFIX + key, args);
    }

    static String lower(Enum<?> constant)
    {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
