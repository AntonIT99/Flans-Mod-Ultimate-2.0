package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.ITeam;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.names.SoldierNames;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * The commands that set up what a team fields: its loose infantry, the weights and roles of its classes, its squads,
 * vehicles and their crews, and the clone its bots are made from. The same commands set up a team of a round of the
 * rotation and a branch of a managed faction, each through its own {@link Target}. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TeamForceCommands
{
    private static final String COUNT = "count";
    private static final String BUDGET = "budget";
    private static final String VALUE = "value";
    private static final String TEMPLATE = "template";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String NAMES = "names";
    private static final String CLASS = "class";
    private static final String RESPAWN = "respawn";
    private static final String TICKET_COST = "ticketcost";
    private static final String ROLE = "role";
    private static final String SET = "set";
    static final SimpleCommandExceptionType NO_TEMPLATE = new SimpleCommandExceptionType(BotCommands.text("no_template"));
    private static final SimpleCommandExceptionType NO_VEHICLE = new SimpleCommandExceptionType(BotCommands.text("no_vehicle"));
    private static final SimpleCommandExceptionType NO_ROLE = new SimpleCommandExceptionType(BotCommands.text("unknown_role"));

    static final SuggestionProvider<CommandSourceStack> NAME_LISTS = (context, builder) ->
    {
        List<String> lists = new ArrayList<>(SoldierNames.ids());
        lists.add(BotCommands.NONE);
        return SharedSuggestionProvider.suggest(lists, builder);
    };
    static final SuggestionProvider<CommandSourceStack> VEHICLES = (context, builder) -> SharedSuggestionProvider
        .suggest(FlanModelEntities.getEntityTypes().stream().map(FlanModelEntityType::getShortName).toList(), builder);
    static final SuggestionProvider<CommandSourceStack> TEMPLATES = (context, builder) -> SharedSuggestionProvider.suggest(SquadTemplates.get().all().stream().map(SquadTemplate::id).toList(),
        builder);
    private static final SuggestionProvider<CommandSourceStack> ROLES = (context, builder) -> SharedSuggestionProvider
        .suggest(Arrays.stream(BotSettings.ClassRole.values()).map(BotCommands::lower).toList(), builder);

    /** Where the force a command sets up is found and kept. */
    interface Target
    {
        /** @return the team whose force the command's arguments name, or empty when there is none */
        Optional<ITeam> team(CommandContext<CommandSourceStack> context);

        /**
         * Changes the force the command's arguments name, keeps it, and tells the command's source.
         *
         * @return the command's result
         */
        int edit(CommandContext<CommandSourceStack> context, Consumer<BotSettings.TeamForce> change, Component message) throws CommandSyntaxException;
    }

    /** Adds the commands that set up a force to the node naming it. */
    static <T extends ArgumentBuilder<CommandSourceStack, T>> T build(T team, Target target)
    {
        SuggestionProvider<CommandSourceStack> classes = (context, builder) -> SharedSuggestionProvider
            .suggest(target.team(context).map(found -> found.getClasses().stream().map(IContentType::getShortName).toList()).orElse(List.of()), builder);
        team.then(Commands.literal(COUNT).then(Commands.argument(VALUE, IntegerArgumentType.integer(0))
            .executes(context -> target.edit(context, force -> force.setCount(IntegerArgumentType.getInteger(context, VALUE)), text(SET, COUNT, IntegerArgumentType.getInteger(context, VALUE))))));
        team.then(Commands.literal(BUDGET).then(Commands.argument(VALUE, IntegerArgumentType.integer(0))
            .executes(context -> target.edit(context, force -> force.setBudget(IntegerArgumentType.getInteger(context, VALUE)), text(SET, BUDGET, IntegerArgumentType.getInteger(context, VALUE))))));
        team.then(Commands.literal(NAMES).then(Commands.argument(NAME, StringArgumentType.word()).suggests(NAME_LISTS)
            .executes(context -> target.edit(context, force -> force.setNameList(BotCommands.word(context, NAME)), text(SET, NAMES, StringArgumentType.getString(context, NAME))))));
        team.then(Commands.literal("clone").then(Commands.literal(BotCommands.NONE).executes(context -> target.edit(context, force -> force.setCloneTab(0), text("clone_none"))))
            .then(Commands.argument("tab", IntegerArgumentType.integer(1, 15)).then(Commands.argument(NAME, StringArgumentType.greedyString()).executes(context -> target.edit(context, force ->
            {
                force.setCloneTab(IntegerArgumentType.getInteger(context, "tab"));
                force.setCloneName(StringArgumentType.getString(context, NAME));
            }, text("clone", StringArgumentType.getString(context, NAME)))))));
        team.then(crewSettings(target, classes));
        team.then(Commands.literal("keepequipment").then(Commands.literal("on").executes(context -> target.edit(context, force -> force.setKeepCloneEquipment(true), text("keep_equipment_on"))))
            .then(Commands.literal("off").executes(context -> target.edit(context, force -> force.setKeepCloneEquipment(false), text("keep_equipment_off")))));
        team.then(Commands.literal("role")
            .then(Commands.argument(CLASS, StringArgumentType.word()).suggests(classes).then(Commands.literal("auto").executes(
                context -> target.edit(context, force -> force.getClassRoles().remove(StringArgumentType.getString(context, CLASS)), text("role_auto", StringArgumentType.getString(context, CLASS)))))
                .then(Commands.argument(ROLE, StringArgumentType.word()).suggests(ROLES).executes(context -> role(context, target)))));
        team.then(Commands.literal(CLASS)
            .then(Commands.argument(CLASS, StringArgumentType.word()).suggests(classes)
                .then(Commands.argument(VALUE, IntegerArgumentType.integer(0))
                    .executes(context -> target.edit(context, force -> force.getClassWeights().put(StringArgumentType.getString(context, CLASS), IntegerArgumentType.getInteger(context, VALUE)),
                        text("class_weight", StringArgumentType.getString(context, CLASS), IntegerArgumentType.getInteger(context, VALUE)))))));
        team.then(Commands.literal("squad").then(
            Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES).then(Commands.argument(COUNT, IntegerArgumentType.integer(0)).executes(context -> squad(context, target, 0))
                .then(Commands.argument(BUDGET, IntegerArgumentType.integer(0)).executes(context -> squad(context, target, IntegerArgumentType.getInteger(context, BUDGET)))))));
        team.then(Commands.literal("removesquad").then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES).executes(context -> target.edit(context,
            force -> force.getSquads().removeIf(entry -> entry.template().equals(BotCommands.word(context, TEMPLATE))), text("squad_removed", BotCommands.word(context, TEMPLATE))))));
        team.then(Commands.literal("vehicle")
            .then(Commands.argument(TYPE, StringArgumentType.word()).suggests(VEHICLES)
                .then(Commands.argument(COUNT, IntegerArgumentType.integer(0)).executes(context -> vehicle(context, target, 0, 0, 1))
                    .then(Commands.argument(BUDGET, IntegerArgumentType.integer(0)).executes(context -> vehicle(context, target, IntegerArgumentType.getInteger(context, BUDGET), 0, 1))
                        .then(Commands.argument(RESPAWN, IntegerArgumentType.integer(0))
                            .executes(context -> vehicle(context, target, IntegerArgumentType.getInteger(context, BUDGET), IntegerArgumentType.getInteger(context, RESPAWN), 1))
                            .then(Commands.argument(TICKET_COST, IntegerArgumentType.integer(0)).executes(context -> vehicle(context, target, IntegerArgumentType.getInteger(context, BUDGET),
                                IntegerArgumentType.getInteger(context, RESPAWN), IntegerArgumentType.getInteger(context, TICKET_COST)))))))));
        team.then(Commands.literal("carry").then(Commands.argument(TYPE, StringArgumentType.word()).suggests(VEHICLES)
            .then(Commands.argument(TEMPLATE, StringArgumentType.word()).suggests(TEMPLATES).executes(context -> carry(context, target)))));
        team.then(Commands.literal("removevehicle").then(Commands.argument(TYPE, StringArgumentType.word()).suggests(VEHICLES).executes(context -> target.edit(context,
            force -> force.getVehicles().removeIf(entry -> entry.type().equals(StringArgumentType.getString(context, TYPE))), text("vehicle_removed", StringArgumentType.getString(context, TYPE))))));
        return team;
    }

    /** The class crewing each kind of seat of the team's vehicle bots: a class, any class by weight, or none. */
    private static LiteralArgumentBuilder<CommandSourceStack> crewSettings(Target target, SuggestionProvider<CommandSourceStack> classes)
    {
        LiteralArgumentBuilder<CommandSourceStack> crew = Commands.literal("crew");
        for (CrewPost post : CrewPost.values())
        {
            crew.then(Commands.literal(post.id())
                .then(Commands.literal(BotSettingsText.ANY_CLASS).executes(context -> target.edit(context, force -> force.setCrewClass(post, ""), text("crew_any", post.id()))))
                .then(Commands.literal(BotSettings.TeamForce.NO_CREW)
                    .executes(context -> target.edit(context, force -> force.setCrewClass(post, BotSettings.TeamForce.NO_CREW), text("crew_none", post.id()))))
                .then(Commands.argument(CLASS, StringArgumentType.word()).suggests(classes)
                    .executes(context -> target.edit(context, force -> force.setCrewClass(post, BotCommands.word(context, CLASS)), text("crew_class", post.id(), BotCommands.word(context, CLASS))))));
        }
        return crew;
    }

    private static int role(CommandContext<CommandSourceStack> context, Target target) throws CommandSyntaxException
    {
        String playerClass = StringArgumentType.getString(context, CLASS);
        BotSettings.ClassRole role = BotSettingsText.role(StringArgumentType.getString(context, ROLE)).orElseThrow(NO_ROLE::create);
        return target.edit(context, force -> force.getClassRoles().put(playerClass, role), text("role_set", playerClass, BotCommands.lower(role)));
    }

    private static int squad(CommandContext<CommandSourceStack> context, Target target, int budget) throws CommandSyntaxException
    {
        String template = BotCommands.word(context, TEMPLATE);
        if (SquadTemplates.get().find(template).isEmpty())
            throw NO_TEMPLATE.create();
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return target.edit(context, force ->
        {
            List<BotSettings.SquadEntry> squads = force.getSquads();
            BotSettings.SquadEntry entry = new BotSettings.SquadEntry(template, count, budget);
            int index = squads.stream().map(BotSettings.SquadEntry::template).toList().indexOf(template);
            if (index < 0)
                squads.add(entry);
            else
                squads.set(index, entry);
        }, text("squad_set", template, count));
    }

    private static int vehicle(CommandContext<CommandSourceStack> context, Target target, int budget, int respawn, int ticketCost) throws CommandSyntaxException
    {
        String type = StringArgumentType.getString(context, TYPE);
        if (FlanModelEntities.getEntityTypes().stream().noneMatch(model -> model.getShortName().equals(type)))
            throw NO_VEHICLE.create();
        int count = IntegerArgumentType.getInteger(context, COUNT);
        return target.edit(context, force ->
        {
            List<BotSettings.VehicleEntry> vehicles = force.getVehicles();
            int index = vehicles.stream().map(BotSettings.VehicleEntry::type).toList().indexOf(type);
            // The squad a vehicle carries is set on its own and kept when the rest of the entry changes.
            String carry = index < 0 ? "" : vehicles.get(index).carry();
            BotSettings.VehicleEntry entry = new BotSettings.VehicleEntry(type, count, budget, respawn, ticketCost, null, carry);
            if (index < 0)
                vehicles.add(entry);
            else
                vehicles.set(index, entry);
        }, text("vehicle_set", type, count));
    }

    private static int carry(CommandContext<CommandSourceStack> context, Target target) throws CommandSyntaxException
    {
        String type = StringArgumentType.getString(context, TYPE);
        String template = BotCommands.word(context, TEMPLATE);
        if (!template.isEmpty() && SquadTemplates.get().find(template).isEmpty())
            throw NO_TEMPLATE.create();
        return target.edit(context, force ->
        {
            List<BotSettings.VehicleEntry> vehicles = force.getVehicles();
            for (int i = 0; i < vehicles.size(); i++)
            {
                BotSettings.VehicleEntry entry = vehicles.get(i);
                if (entry.type().equals(type))
                    vehicles.set(i, new BotSettings.VehicleEntry(entry.type(), entry.count(), entry.budget(), entry.respawnSeconds(), entry.ticketCost(), entry.spawner(), template));
            }
        }, template.isEmpty() ? text("carry_none", type) : text("carry", type, template));
    }

    private static Component text(String key, Object... args)
    {
        return BotCommands.text(key, args);
    }
}
