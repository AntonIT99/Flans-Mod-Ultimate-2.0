package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsRound;
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
import org.jetbrains.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@code /teams faction}: sets up the Custom NPCs factions managed for battles outside Teams rounds, the Flan teams that
 * serve them as branches and what each branch fields, which factions are at war, and lets players join and leave them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FactionCommands
{
    private static final String FACTION = "faction";
    private static final String OTHER = "other";
    private static final String NAME = "name";
    private static final String COLOUR = "colour";
    private static final String TEAM = "team";
    private static final String ROUND = "round";
    private static final String ROLES = "roles";
    private static final String VALUE = "value";
    private static final String PLAYER = "player";
    private static final String KEY = "faction.";
    private static final String AUTO = "auto";
    private static final String THEATRES = "theatres";
    private static final String ANYWHERE = "any";
    private static final int DEFAULT_COLOUR = 0xFFFFFF;
    private static final SimpleCommandExceptionType NO_FACTION = new SimpleCommandExceptionType(text("unknown"));
    private static final SimpleCommandExceptionType NOT_MANAGED = new SimpleCommandExceptionType(text("not_managed"));
    private static final SimpleCommandExceptionType ROUND_FACTION = new SimpleCommandExceptionType(text("round_faction"));
    private static final SimpleCommandExceptionType SAME_FACTION = new SimpleCommandExceptionType(text("same"));
    private static final SimpleCommandExceptionType BAD_COLOUR = new SimpleCommandExceptionType(text("bad_colour"));
    private static final SimpleCommandExceptionType NO_TEAM = new SimpleCommandExceptionType(text("no_team"));
    private static final SimpleCommandExceptionType NO_BRANCH = new SimpleCommandExceptionType(text("no_branch"));
    private static final SimpleCommandExceptionType NO_ROUND = new SimpleCommandExceptionType(BotCommands.text("no_round"));
    private static final SimpleCommandExceptionType NO_FORCE = new SimpleCommandExceptionType(text("no_force"));
    private static final SimpleCommandExceptionType BAD_ROLE = new SimpleCommandExceptionType(text("bad_role"));
    private static final SimpleCommandExceptionType BAD_THEATRE = new SimpleCommandExceptionType(text("bad_theatre"));

    static final SuggestionProvider<CommandSourceStack> MANAGED = (context, builder) -> SharedSuggestionProvider
        .suggest(BattleFactions.get(context.getSource().getServer()).all().stream().map(managed -> nameOf(managed.getId())).filter(Objects::nonNull).map(FactionCommands::quoted).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> ANY_FACTION = (context, builder) -> SharedSuggestionProvider
        .suggest(FactionController.instance.factions.values().stream().filter(faction -> !BotFactions.isRoundFaction(faction.name)).map(faction -> quoted(faction.name)).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> TEAMS = (context, builder) -> SharedSuggestionProvider.suggest(FlansModApi.getTeams().stream().map(ITeam::getShortName).toList(),
        builder);
    private static final SuggestionProvider<CommandSourceStack> BRANCHES = (context, builder) -> SharedSuggestionProvider
        .suggest(managedOrEmpty(context).map(managed -> managed.getBranches().stream().map(BattleFactions.Branch::getTeam).toList()).orElse(List.of()), builder);
    private static final SuggestionProvider<CommandSourceStack> ROLE_NAMES = (context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(BotRole.values()).map(BotCommands::lower).toList(),
        builder);
    private static final SuggestionProvider<CommandSourceStack> BIOMES = (context, builder) ->
    {
        Registry<Biome> biomes = context.getSource().getServer().registryAccess().registryOrThrow(Registries.BIOME);
        List<String> names = new ArrayList<>(biomes.keySet().stream().map(ResourceLocation::toString).toList());
        biomes.getTagNames().forEach(tag -> names.add("#" + tag.location()));
        return SharedSuggestionProvider.suggest(names, builder);
    };
    /** A branch of the faction the command names, whose force is kept with the managed factions. */
    private static final TeamForceCommands.Target BRANCH = new TeamForceCommands.Target()
    {
        @Override
        public Optional<ITeam> team(CommandContext<CommandSourceStack> context)
        {
            return FlansModApi.getTeam(StringArgumentType.getString(context, TEAM));
        }

        @Override
        public int edit(CommandContext<CommandSourceStack> context, Consumer<BotSettings.TeamForce> change, Component message) throws CommandSyntaxException
        {
            return editBranch(context, branch -> change.accept(branch.getForce()), message);
        }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(FACTION);
        root.then(Commands.literal("list").executes(FactionCommands::list));
        root.then(Commands.literal("info").then(managedArgument().executes(FactionCommands::info)));
        root.then(Commands.literal("join").then(managedArgument().executes(FactionCommands::join)));
        root.then(Commands.literal("leave").executes(FactionCommands::leave));
        root.then(Commands.literal("create").requires(source -> source.hasPermission(2))
            .then(Commands.argument(NAME, StringArgumentType.string()).suggests(ANY_FACTION).executes(context -> create(context, null))
                .then(Commands.literal("from").then(Commands.argument(TEAM, StringArgumentType.word()).suggests(TEAMS).executes(FactionCommands::createFromTeam)))
                .then(Commands.argument(COLOUR, StringArgumentType.word()).executes(context -> create(context, colour(context))))));
        root.then(Commands.literal("remove").requires(source -> source.hasPermission(2)).then(managedArgument().executes(FactionCommands::remove)));
        root.then(Commands.literal("hostile").requires(source -> source.hasPermission(2))
            .then(managedArgument().then(Commands.argument(OTHER, StringArgumentType.string()).suggests(ANY_FACTION).executes(context -> war(context, true)))));
        root.then(Commands.literal("peace").requires(source -> source.hasPermission(2))
            .then(managedArgument().then(Commands.argument(OTHER, StringArgumentType.string()).suggests(ANY_FACTION).executes(context -> war(context, false)))));
        LiteralArgumentBuilder<CommandSourceStack> outsiders = Commands.literal("outsiders").requires(source -> source.hasPermission(2));
        RequiredArgumentBuilder<CommandSourceStack, String> outsidersOf = managedArgument();
        for (BattleFactions.Outsiders rule : BattleFactions.Outsiders.values())
            outsidersOf.then(Commands.literal(BotCommands.lower(rule)).executes(context -> outsiders(context, rule)));
        root.then(outsiders.then(outsidersOf));
        root.then(Commands.literal("member").requires(source -> source.hasPermission(2)).then(Commands.argument(PLAYER, EntityArgument.player())
            .then(Commands.literal(BotCommands.NONE).executes(context -> member(context, false))).then(managedArgument().executes(context -> member(context, true)))));
        root.then(Commands.literal("branch").requires(source -> source.hasPermission(2)).then(branches()));
        dispatcher.register(Commands.literal("teams").then(root));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> managedArgument()
    {
        return Commands.argument(FACTION, StringArgumentType.string()).suggests(MANAGED);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> branches()
    {
        RequiredArgumentBuilder<CommandSourceStack, String> faction = managedArgument();
        faction.then(Commands.literal("add").then(Commands.argument(TEAM, StringArgumentType.word()).suggests(TEAMS).executes(FactionCommands::addBranch)));
        faction.then(Commands.literal("remove").then(Commands.argument(TEAM, StringArgumentType.word()).suggests(BRANCHES).executes(FactionCommands::removeBranch)));
        RequiredArgumentBuilder<CommandSourceStack, String> team = Commands.argument(TEAM, StringArgumentType.word()).suggests(BRANCHES);
        team.then(Commands.literal(ROLES).then(Commands.literal(AUTO).executes(context -> editBranch(context, branch -> branch.getRoles().clear(), text("roles_auto", branchTeam(context)))))
            .then(Commands.argument(ROLES, StringArgumentType.greedyString()).suggests(ROLE_NAMES).executes(FactionCommands::roles)));
        team.then(Commands.literal("weight").then(Commands.argument(VALUE, IntegerArgumentType.integer(1)).executes(context -> editBranch(context,
            branch -> branch.setWeight(IntegerArgumentType.getInteger(context, VALUE)), text("weight", branchTeam(context), IntegerArgumentType.getInteger(context, VALUE))))));
        team.then(
            Commands.literal("theatre").then(Commands.literal(ANYWHERE).executes(context -> editBranch(context, branch -> branch.getTheatres().clear(), text("theatre_any", branchTeam(context)))))
                .then(Commands.argument(THEATRES, StringArgumentType.greedyString()).suggests(BIOMES).executes(FactionCommands::theatres)));
        team.then(Commands.literal("copyfrom").then(Commands.argument(ROUND, StringArgumentType.word()).suggests(BotCommands.ROUNDS).executes(FactionCommands::copyFrom)));
        faction.then(TeamForceCommands.build(team, BRANCH));
        return faction;
    }

    // --- Factions ---

    private static int list(CommandContext<CommandSourceStack> context)
    {
        Collection<BattleFactions.Managed> managed = BattleFactions.get(context.getSource().getServer()).all();
        if (managed.isEmpty())
            context.getSource().sendSuccess(() -> text("none"), false);
        for (BattleFactions.Managed faction : managed)
        {
            String branches = String.join(", ", faction.getBranches().stream().map(BattleFactions.Branch::getTeam).toList());
            context.getSource().sendSuccess(() -> text("list_entry", nameOf(faction.getId()), branches.isEmpty() ? "-" : branches), false);
        }
        return managed.size();
    }

    // A command reports how many factions it showed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        BattleFactions data = BattleFactions.get(context.getSource().getServer());
        BattleFactions.Managed managed = managed(context);
        Faction faction = FactionController.instance.getFaction(managed.getId());
        List<String> enemies = faction.attackFactions.stream().map(FactionCommands::nameOf).filter(Objects::nonNull).toList();
        context.getSource().sendSuccess(() -> text("info", faction.name, String.format(Locale.ROOT, "%06X", faction.color & 0xFFFFFF), BotCommands.lower(managed.getOutsiders()),
            data.memberCount(managed.getId()), enemies.isEmpty() ? "-" : String.join(", ", enemies)), false);
        for (BattleFactions.Branch branch : managed.getBranches())
        {
            String roles = branch.getRoles().isEmpty() ? AUTO : String.join(" ", branch.getRoles().stream().map(BotCommands::lower).toList());
            BotSettings.TeamForce force = branch.getForce();
            String theatres = branch.getTheatres().isEmpty() ? ANYWHERE : String.join(" ", branch.getTheatres());
            context.getSource().sendSuccess(() -> text("info_branch", branch.getTeam(), roles, branch.getWeight(), force.getCount(), orDash(BotSettingsText.formatSquads(force.getSquads())),
                orDash(BotSettingsText.formatVehicles(force.getVehicles())), theatres), false);
        }
        return 1;
    }

    // A command reports how many factions it set up, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int create(CommandContext<CommandSourceStack> context, @Nullable Integer colour) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(context, NAME);
        if (BotFactions.isRoundFaction(name))
            throw ROUND_FACTION.create();
        FactionController controller = FactionController.instance;
        Faction faction = controller.getFactionFromName(name);
        boolean adopted = faction != null;
        if (faction == null)
            faction = (Faction) controller.create(name, colour == null ? DEFAULT_COLOUR : colour);
        else if (colour != null)
            faction.color = colour;
        controller.saveFaction(faction);
        BattleFactions.get(context.getSource().getServer()).manage(faction.id);
        String created = faction.name;
        context.getSource().sendSuccess(() -> text(adopted ? "adopted" : "created", created), true);
        return 1;
    }

    /** Makes a faction of a Flan team: named and coloured after it, with the team as its first branch. */
    // A command reports how many factions it set up, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int createFromTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ITeam team = FlansModApi.getTeam(StringArgumentType.getString(context, TEAM)).orElseThrow(NO_TEAM::create);
        create(context, team.getTeamColour());
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, NAME));
        BattleFactions data = BattleFactions.get(context.getSource().getServer());
        BattleFactions.Managed managed = data.manage(faction.id);
        if (managed.branch(team.getShortName()).isEmpty())
            managed.getBranches().add(new BattleFactions.Branch(team.getShortName(), new BotSettings.TeamForce()));
        data.setDirty();
        context.getSource().sendSuccess(() -> text("branch_added", team.getShortName(), faction.name), true);
        return 1;
    }

    // A command reports how many factions it released, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        BattleFactions.Managed managed = managed(context);
        BattleFactions.get(context.getSource().getServer()).release(managed.getId());
        context.getSource().sendSuccess(() -> text("removed", nameOf(managed.getId())), true);
        return 1;
    }

    /** Puts two factions at war or at peace, each with the other. */
    // A command reports how many relations it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int war(CommandContext<CommandSourceStack> context, boolean hostile) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFaction(managed(context).getId());
        Faction other = faction(StringArgumentType.getString(context, OTHER));
        if (BotFactions.isRoundFaction(other.name))
            throw ROUND_FACTION.create();
        if (faction.id == other.id)
            throw SAME_FACTION.create();
        if (hostile)
        {
            faction.attackFactions.add(other.id);
            other.attackFactions.add(faction.id);
        }
        else
        {
            faction.attackFactions.remove(other.id);
            other.attackFactions.remove(faction.id);
        }
        FactionController.instance.saveFaction(faction);
        FactionController.instance.saveFaction(other);
        context.getSource().sendSuccess(() -> text(hostile ? "hostile" : "peace", faction.name, other.name), true);
        return 1;
    }

    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int outsiders(CommandContext<CommandSourceStack> context, BattleFactions.Outsiders rule) throws CommandSyntaxException
    {
        BattleFactions.Managed managed = managed(context);
        managed.setOutsiders(rule);
        BattleFactions.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("outsiders", nameOf(managed.getId()), BotCommands.lower(rule)), true);
        return 1;
    }

    // --- Members ---

    // A command reports how many players it moved, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BattleFactions.Managed managed = managed(context);
        BattleFactions.get(context.getSource().getServer()).setMember(player.getUUID(), managed.getId());
        context.getSource().sendSuccess(() -> text("joined", nameOf(managed.getId())), false);
        return 1;
    }

    // A command reports how many players it moved, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BattleFactions.get(context.getSource().getServer()).setMember(player.getUUID(), null);
        context.getSource().sendSuccess(() -> text("left"), false);
        return 1;
    }

    // A command reports how many players it moved, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int member(CommandContext<CommandSourceStack> context, boolean joins) throws CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER);
        Integer faction = joins ? managed(context).getId() : null;
        BattleFactions.get(context.getSource().getServer()).setMember(player.getUUID(), faction);
        Component message = faction == null ? text("member_none", player.getDisplayName()) : text("member", player.getDisplayName(), nameOf(faction));
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    // --- Branches ---

    // A command reports how many branches it added, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int addBranch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ITeam team = FlansModApi.getTeam(StringArgumentType.getString(context, TEAM)).orElseThrow(NO_TEAM::create);
        BattleFactions.Managed managed = managed(context);
        if (managed.branch(team.getShortName()).isEmpty())
            managed.getBranches().add(new BattleFactions.Branch(team.getShortName(), new BotSettings.TeamForce()));
        BattleFactions.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("branch_added", team.getShortName(), nameOf(managed.getId())), true);
        return 1;
    }

    // A command reports how many branches it removed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int removeBranch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        BattleFactions.Managed managed = managed(context);
        String team = StringArgumentType.getString(context, TEAM);
        if (!managed.getBranches().removeIf(branch -> branch.getTeam().equals(team)))
            throw NO_BRANCH.create();
        BattleFactions.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> text("branch_removed", team, nameOf(managed.getId())), true);
        return 1;
    }

    private static int theatres(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Set<String> theatres = new LinkedHashSet<>();
        for (String word : StringArgumentType.getString(context, THEATRES).trim().split("\\s+"))
            theatres.add(Theatres.normalize(word).orElseThrow(BAD_THEATRE::create));
        return editBranch(context, branch ->
        {
            branch.getTheatres().clear();
            branch.getTheatres().addAll(theatres);
        }, text("theatres_set", branchTeam(context), String.join(" ", theatres)));
    }

    private static int roles(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Set<BotRole> roles = EnumSet.noneOf(BotRole.class);
        for (String word : StringArgumentType.getString(context, ROLES).trim().split("\\s+"))
            roles.add(Arrays.stream(BotRole.values()).filter(role -> role.name().equalsIgnoreCase(word)).findFirst().orElseThrow(BAD_ROLE::create));
        return editBranch(context, branch ->
        {
            branch.getRoles().clear();
            branch.getRoles().addAll(roles);
        }, text("roles_set", branchTeam(context), String.join(" ", roles.stream().map(BotCommands::lower).toList())));
    }

    /** Copies what a team fields in a round of the rotation into the branch of the same team. */
    private static int copyFrom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String team = StringArgumentType.getString(context, TEAM);
        ITeamsRound round = BotCommands.roundNamed(StringArgumentType.getString(context, ROUND)).orElseThrow(NO_ROUND::create);
        BotSettings.TeamForce force = BotSettings.load(round.getExtensionData(BotSettings.KEY)).getTeams().get(team);
        if (force == null)
            throw NO_FORCE.create();
        return editBranch(context, branch -> branch.setForce(BotSettings.TeamForce.load(force.save())), text("copied", team, round.getMapId()));
    }

    // A command reports how many branches it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int editBranch(CommandContext<CommandSourceStack> context, Consumer<BattleFactions.Branch> change, Component message) throws CommandSyntaxException
    {
        BattleFactions.Branch branch = managed(context).branch(StringArgumentType.getString(context, TEAM)).orElseThrow(NO_BRANCH::create);
        change.accept(branch);
        BattleFactions.get(context.getSource().getServer()).setDirty();
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    // --- Lookups ---

    private static BattleFactions.Managed managed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Faction faction = faction(StringArgumentType.getString(context, FACTION));
        return BattleFactions.get(context.getSource().getServer()).managed(faction.id).orElseThrow(NOT_MANAGED::create);
    }

    private static Optional<BattleFactions.Managed> managedOrEmpty(CommandContext<CommandSourceStack> context)
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, FACTION));
        return faction == null ? Optional.empty() : BattleFactions.get(context.getSource().getServer()).managed(faction.id);
    }

    private static Faction faction(String name) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(name);
        if (faction == null)
            throw NO_FACTION.create();
        return faction;
    }

    @Nullable
    private static String nameOf(int id)
    {
        Faction faction = FactionController.instance.getFaction(id);
        return faction == null ? null : faction.name;
    }

    /** A faction name as a command argument: quoted when it has spaces. */
    private static String quoted(String name)
    {
        return StringArgumentType.escapeIfRequired(name);
    }

    private static Integer colour(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        try
        {
            return Integer.parseInt(StringArgumentType.getString(context, COLOUR), 16) & 0xFFFFFF;
        }
        catch (NumberFormatException e)
        {
            throw BAD_COLOUR.create();
        }
    }

    private static String orDash(String text)
    {
        return text.isEmpty() ? "-" : text;
    }

    private static String branchTeam(CommandContext<CommandSourceStack> context)
    {
        return StringArgumentType.getString(context, TEAM);
    }

    private static Component text(String key, Object... args)
    {
        return BotCommands.text(KEY + key, args);
    }
}
