package com.flansmodultimate.common.command;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.GameType;
import com.flansmodultimate.common.teams.PlayerStats;
import com.flansmodultimate.common.teams.RewardBoxInstance;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.teams.TeamsMap;
import com.flansmodultimate.common.teams.TeamsRound;
import com.flansmodultimate.common.types.LoadoutPool;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.RewardBox;
import com.flansmodultimate.common.types.Team;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Brigadier implementation of the legacy /teams administration and player commands. */
public final class TeamsCommand
{
    private TeamsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        var root = Commands.literal("teams").executes(TeamsCommand::help)
            .then(Commands.literal("help").executes(TeamsCommand::help))
            .then(Commands.literal("join")
                .then(Commands.argument("team", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(Team.values().stream().map(Team::getOriginalShortName), builder))
                    .executes(TeamsCommand::joinTeam)))
            .then(Commands.literal("class")
                .then(Commands.argument("class", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(PlayerClass.values().stream().map(PlayerClass::getOriginalShortName), builder))
                    .executes(TeamsCommand::selectClass)))
            .then(Commands.literal("score").executes(TeamsCommand::score))
            .then(Commands.literal("vote")
                .then(Commands.argument("option", IntegerArgumentType.integer(1, 5)).executes(TeamsCommand::vote)))
            .then(Commands.literal("stats").executes(context -> showStats(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                    .executes(context -> showStats(context.getSource(), EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("leaderboard").executes(TeamsCommand::leaderboard))
            .then(Commands.literal("loadouts").executes(TeamsCommand::openLoadouts))
            .then(Commands.literal("list")
                .then(Commands.literal("gametypes").executes(TeamsCommand::listGameTypes))
                .then(Commands.literal("teams").executes(TeamsCommand::listTeams))
                .then(Commands.literal("classes").executes(TeamsCommand::listClasses))
                .then(Commands.literal("maps").executes(TeamsCommand::listMaps))
                .then(Commands.literal("rounds").executes(TeamsCommand::listRounds))
                .then(Commands.literal("loadouts").executes(TeamsCommand::listLoadoutPools))
                .then(Commands.literal("rewardboxes").executes(TeamsCommand::listRewardBoxes)))
            // Frequently used legacy spellings remain as thin aliases.
            .then(Commands.literal("listGametypes").executes(TeamsCommand::listGameTypes))
            .then(Commands.literal("listMaps").executes(TeamsCommand::listMaps))
            .then(Commands.literal("listRounds").executes(TeamsCommand::listRounds))
            .then(Commands.literal("listAllTeams").executes(TeamsCommand::listTeams))
            .then(Commands.literal("on").requires(source -> source.hasPermission(2)).executes(context -> { manager(context).setEnabled(true); return success(context, "Teams enabled"); }))
            .then(Commands.literal("off").requires(source -> source.hasPermission(2)).executes(context -> { manager(context).setEnabled(false); return success(context, "Teams disabled"); }))
            .then(Commands.literal("start").requires(source -> source.hasPermission(2)).executes(context -> manager(context).startNextRound() ? success(context, "Round started") : failure(context, "No valid round is configured")))
            .then(Commands.literal("nextRound").requires(source -> source.hasPermission(2)).executes(context -> manager(context).startNextRound() ? success(context, "Advanced to the next round") : failure(context, "No valid round is configured")))
            .then(Commands.literal("getOpKit").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("setloadoutpool").requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.word()).suggests((context, builder) ->
                    SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), LoadoutPool.values().stream().map(LoadoutPool::getOriginalShortName)), builder))
                    .executes(TeamsCommand::setLoadoutPool)))
            .then(Commands.literal("xpmultiplier").requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0F, 100F)).executes(TeamsCommand::setExperienceMultiplier)))
            .then(Commands.literal("xp").requires(source -> source.hasPermission(2)).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(TeamsCommand::giveExperience))))
            .then(Commands.literal("resetrank").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).executes(TeamsCommand::resetRank)))
            .then(Commands.literal("giverewardbox").requires(source -> source.hasPermission(2)).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("box", StringArgumentType.word()).suggests((context, builder) ->
                    SharedSuggestionProvider.suggest(RewardBox.values().stream().map(RewardBox::getOriginalShortName), builder)).executes(TeamsCommand::giveRewardBox))))
            .then(adminCommands());

        com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        dispatcher.register(Commands.literal("flansteams").redirect(node));
        dispatcher.register(Commands.literal("team").redirect(node));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> adminCommands()
    {
        return Commands.literal("admin").requires(source -> source.hasPermission(2))
            .then(Commands.literal("enabled").then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                manager(context).setEnabled(BoolArgumentType.getBool(context, "value"));
                return success(context, "Teams " + (manager(context).isEnabled() ? "enabled" : "disabled"));
            })))
            .then(Commands.literal("voting").then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                boolean enabled = BoolArgumentType.getBool(context, "value");
                manager(context).setVoting(enabled);
                return success(context, "Round voting " + (enabled ? "enabled" : "disabled"));
            })))
            .then(Commands.literal("start").executes(context -> manager(context).startNextRound() ? success(context, "Round started") : failure(context, "No valid round is configured"))
                .then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(context -> manager(context).startRound(IntegerArgumentType.getInteger(context, "index"))
                    ? success(context, "Round started") : failure(context, "Invalid round index"))))
            .then(Commands.literal("next").executes(context -> manager(context).startNextRound() ? success(context, "Advanced to the next round") : failure(context, "No valid round is configured")))
            .then(Commands.literal("stop").executes(context -> { manager(context).stopRound(); return success(context, "Round stopped"); }))
            .then(Commands.literal("arena").executes(context -> { manager(context).applyArenaPreset(); return success(context, "Arena preset applied"); }))
            .then(Commands.literal("survival").executes(context -> { manager(context).applySurvivalPreset(); return success(context, "Survival preset applied"); }))
            .then(Commands.literal("kit").executes(TeamsCommand::giveKit))
            .then(Commands.literal("loadoutpool")
                .then(Commands.argument("id", StringArgumentType.word()).suggests((context, builder) ->
                    SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), LoadoutPool.values().stream().map(LoadoutPool::getOriginalShortName)), builder))
                    .executes(TeamsCommand::setLoadoutPool)))
            .then(Commands.literal("xpmultiplier")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0F, 100F)).executes(TeamsCommand::setExperienceMultiplier)))
            .then(Commands.literal("xp")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(TeamsCommand::giveExperience))))
            .then(Commands.literal("resetrank")
                .then(Commands.argument("player", EntityArgument.player()).executes(TeamsCommand::resetRank)))
            .then(Commands.literal("giverewardbox")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("box", StringArgumentType.word()).suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(RewardBox.values().stream().map(RewardBox::getOriginalShortName), builder))
                        .executes(TeamsCommand::giveRewardBox))))
            .then(Commands.literal("map")
                .then(Commands.literal("add")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(TeamsCommand::addMap))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("id", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder))
                        .executes(TeamsCommand::removeMap))))
            .then(Commands.literal("round")
                .then(Commands.literal("add")
                    .then(Commands.argument("map", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder))
                        .then(Commands.argument("gametype", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(GameType.values().stream().map(GameType::getId), builder))
                            .then(Commands.argument("teams", StringArgumentType.word())
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                                    .then(Commands.argument("score", IntegerArgumentType.integer(1)).executes(TeamsCommand::addRound)))))))
                .then(Commands.literal("remove").then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(TeamsCommand::removeRound))))
            .then(Commands.literal("setvariable")
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("value", StringArgumentType.word()).executes(TeamsCommand::setVariable))));
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Team team = Team.getTeam(StringArgumentType.getString(context, "team"));
        if (team == null)
            return failure(context, "Unknown team");
        if (!manager(context).selectTeam(player, team, false))
            return failure(context, "That team is unavailable or joining it would unbalance the round");
        return success(context, "You will join " + team.getName() + " on respawn");
    }

    private static int help(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSuccess(() -> Component.literal("/teams loadouts, /teams join <team>, /teams class <class>, /teams vote <number>, /teams score, /teams stats, /teams list <gametypes|teams|classes|loadouts|rewardboxes|maps|rounds>"), false);
        if (context.getSource().hasPermission(2))
            context.getSource().sendSuccess(() -> Component.literal("Administration: /teams admin <loadoutpool|xpmultiplier|xp|resetrank|giverewardbox|enabled|voting|start|next|stop|arena|survival|kit|map|round|setvariable>"), false);
        return 1;
    }

    private static int openLoadouts(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        if (manager(context).getCurrentLoadoutPool().isEmpty()) return failure(context, "No ranked loadout pool is active");
        manager(context).syncLoadouts(context.getSource().getPlayerOrException(), com.flansmodultimate.network.client.PacketLoadoutState.OpenScreen.HUB, 0, "");
        return 1;
    }

    private static int listLoadoutPools(CommandContext<CommandSourceStack> context)
    {
        LoadoutPool.values().forEach(pool -> context.getSource().sendSuccess(() -> Component.literal(pool.getOriginalShortName() + " — " + pool.getName()), false));
        return LoadoutPool.values().size();
    }

    private static int listRewardBoxes(CommandContext<CommandSourceStack> context)
    {
        RewardBox.values().forEach(box -> context.getSource().sendSuccess(() -> Component.literal(box.getOriginalShortName() + " — " + box.getName()), false));
        return RewardBox.values().size();
    }

    private static int setLoadoutPool(CommandContext<CommandSourceStack> context)
    {
        String id = StringArgumentType.getString(context, "id");
        return manager(context).setCurrentLoadoutPool(id) ? success(context, "Loadout pool set to " + id) : failure(context, "Unknown loadout pool");
    }

    private static int setExperienceMultiplier(CommandContext<CommandSourceStack> context)
    {
        float value = FloatArgumentType.getFloat(context, "value"); manager(context).setExperienceMultiplier(value);
        return success(context, "Teams XP multiplier set to " + value);
    }

    private static int giveExperience(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player"); int amount = IntegerArgumentType.getInteger(context, "amount");
        manager(context).awardExperience(player, amount); return success(context, "Granted " + amount + " XP to " + player.getScoreboardName());
    }

    private static int resetRank(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player"); manager(context).getStats(player).resetRankProgress(); manager(context).markPlayerDataDirty();
        return success(context, "Reset ranked progress for " + player.getScoreboardName());
    }

    private static int giveRewardBox(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player"); String box = StringArgumentType.getString(context, "box");
        return manager(context).grantRewardBox(player, box, RewardBoxInstance.Origin.COMMAND)
            ? success(context, "Granted " + box + " to " + player.getScoreboardName()) : failure(context, "Unknown reward box");
    }

    private static int selectClass(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerClass playerClass = PlayerClass.getPlayerClass(StringArgumentType.getString(context, "class"));
        if (!manager(context).selectClass(player, playerClass))
            return failure(context, "That class is unavailable for your selected team or rank");
        return success(context, "Class selected: " + playerClass.getName());
    }

    private static int score(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerData data = PlayerData.getInstance(player);
        context.getSource().sendSuccess(() -> Component.literal("Score " + data.getScore() + " | Kills " + data.getKills() + " | Deaths " + data.getDeaths()), false);
        return data.getScore();
    }

    private static int vote(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        int option = IntegerArgumentType.getInteger(context, "option");
        if (!manager(context).castVote(context.getSource().getPlayerOrException(), option))
            return failure(context, "There is no active vote with that option");
        return success(context, "Vote recorded for option " + option);
    }

    private static int showStats(CommandSourceStack source, ServerPlayer player)
    {
        PlayerStats stats = TeamsManager.getInstance().getStats(player);
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + ": rank " + stats.getRank() + ", " + stats.getTotalExperience()
            + " XP, " + stats.getKills() + " kills, " + stats.getDeaths() + " deaths, " + stats.getCapturedFlags() + " captures"), false);
        return stats.getTotalExperience();
    }

    private static int leaderboard(CommandContext<CommandSourceStack> context)
    {
        List<PlayerStats> stats = manager(context).getAllStats().stream().sorted(Comparator.comparingInt(PlayerStats::getTotalExperience).reversed()).limit(10).toList();
        for (int i = 0; i < stats.size(); i++)
        {
            int rank = i + 1;
            PlayerStats entry = stats.get(i);
            context.getSource().sendSuccess(() -> Component.literal(rank + ". " + entry.getLastKnownName() + " — rank " + entry.getRank() + " (" + entry.getTotalExperience() + " XP)"), false);
        }
        return stats.size();
    }

    private static int listGameTypes(CommandContext<CommandSourceStack> context)
    {
        GameType.values().forEach(type -> context.getSource().sendSuccess(() -> Component.literal(type.getId() + " — " + type.getName() + " (" + type.getRequiredTeams() + " teams)"), false));
        return GameType.values().size();
    }

    private static int listTeams(CommandContext<CommandSourceStack> context)
    {
        Team.values().forEach(team -> context.getSource().sendSuccess(() -> Component.literal(team.getOriginalShortName() + " — " + team.getName()), false));
        return Team.values().size();
    }

    private static int listClasses(CommandContext<CommandSourceStack> context)
    {
        PlayerClass.values().forEach(type -> context.getSource().sendSuccess(() -> Component.literal(type.getOriginalShortName() + " — " + type.getName() + " (rank " + type.getUnlockLevel() + ")"), false));
        return PlayerClass.values().size();
    }

    private static int listMaps(CommandContext<CommandSourceStack> context)
    {
        manager(context).getMaps().forEach(map -> context.getSource().sendSuccess(() -> Component.literal(map.getShortName() + " — " + map.getName() + " [" + map.getDimension().location() + "]"), false));
        return manager(context).getMaps().size();
    }

    private static int listRounds(CommandContext<CommandSourceStack> context)
    {
        List<TeamsRound> rounds = manager(context).getRounds();
        for (int i = 0; i < rounds.size(); i++)
        {
            int index = i;
            TeamsRound round = rounds.get(i);
            context.getSource().sendSuccess(() -> Component.literal(index + ": " + round.getGameTypeId() + " @ " + round.getMapId() + " [" + String.join(", ", round.getTeamIds()) + "] " + round.getTimeLimitMinutes() + "m / " + round.getScoreLimit()), false);
        }
        return rounds.size();
    }

    private static int addMap(CommandContext<CommandSourceStack> context)
    {
        try
        {
            TeamsMap map = manager(context).addMap(StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "name"), context.getSource().getLevel());
            return success(context, "Created map " + map.getName());
        }
        catch (IllegalArgumentException e) { return failure(context, e.getMessage()); }
    }

    private static int removeMap(CommandContext<CommandSourceStack> context)
    {
        return manager(context).removeMap(StringArgumentType.getString(context, "id")) ? success(context, "Map removed") : failure(context, "Unknown map");
    }

    private static int addRound(CommandContext<CommandSourceStack> context)
    {
        try
        {
            List<String> teams = Arrays.stream(StringArgumentType.getString(context, "teams").split(",")).filter(value -> !value.isBlank()).toList();
            TeamsRound round = manager(context).addRound(StringArgumentType.getString(context, "map"), StringArgumentType.getString(context, "gametype"), teams,
                IntegerArgumentType.getInteger(context, "minutes"), IntegerArgumentType.getInteger(context, "score"));
            return success(context, "Round added: " + round.getGameTypeId() + " @ " + round.getMapId());
        }
        catch (IllegalArgumentException e) { return failure(context, e.getMessage()); }
    }

    private static int removeRound(CommandContext<CommandSourceStack> context)
    {
        return manager(context).removeRound(IntegerArgumentType.getInteger(context, "index")) ? success(context, "Round removed") : failure(context, "Invalid round index");
    }

    private static int setVariable(CommandContext<CommandSourceStack> context)
    {
        GameType type = manager(context).getCurrentGameType().orElse(null);
        if (type == null || !type.setVariable(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "value")))
            return failure(context, "Unknown variable for the current game type");
        return success(context, "Game type variable updated");
    }

    private static int giveKit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (ItemStack stack : List.of(new ItemStack(FlansMod.opStick.get()), new ItemStack(FlansMod.flagpoleItem.get(), 16),
            new ItemStack(FlansMod.playerSpawnerItem.get(), 16), new ItemStack(FlansMod.itemSpawnerItem.get(), 16), new ItemStack(FlansMod.vehicleSpawnerItem.get(), 16)))
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        return success(context, "Teams operator kit added to your inventory");
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestMaps(com.mojang.brigadier.suggestion.SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(TeamsManager.getInstance().getMaps().stream().map(TeamsMap::getShortName), builder);
    }

    private static TeamsManager manager(CommandContext<CommandSourceStack> ignored)
    {
        return TeamsManager.getInstance();
    }

    private static int success(CommandContext<CommandSourceStack> context, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int failure(CommandContext<CommandSourceStack> context, String message)
    {
        context.getSource().sendFailure(Component.literal(message));
        return 0;
    }
}
