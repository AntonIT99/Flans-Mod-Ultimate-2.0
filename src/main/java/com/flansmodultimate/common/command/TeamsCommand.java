package com.flansmodultimate.common.command;

import com.flansmodultimate.FlansModItems;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Brigadier implementation of the legacy /teams administration and player commands. */
public final class TeamsCommand
{
    private static final String TEAMS = "teams";
    private static final String CLASS = "class";
    private static final String SCORE = "score";
    private static final String PLAYER = "player";
    private static final String MSG_ROUND_STARTED = "Round started";
    private static final String MSG_NO_VALID_ROUND = "No valid round is configured";
    private static final String VALUE = "value";
    private static final String AMOUNT = "amount";
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private static final String SECONDS = "seconds";
    private static final String SECONDS_SUFFIX = " seconds";
    private static final String INDEX = "index";
    private static final String MSG_INVALID_ROUND_INDEX = "Invalid round index";
    private static final String DIVISOR = "divisor";
    private static final String NO_LONGER = "no longer";
    private static final String RANK = "rank";
    private static final String GAMETYPE = "gametype";
    private static final String MINUTES = "minutes";
    private static final String IMAGE = "image";
    private static final String FROM = "from";
    private static final String MSG_UNKNOWN_MAP = "Unknown map";
    /** Prefix of the arguments that each name one side of a round being added. */
    private static final String SIDE = "side";

    private TeamsCommand()
    {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(TEAMS).executes(TeamsCommand::help).then(Commands.literal("help").executes(TeamsCommand::help))
            .then(Commands.literal("join")
                .then(Commands.argument("team", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(Team.values().stream().map(Team::getShortName).map(StringArgumentType::escapeIfRequired), builder))
                    .executes(TeamsCommand::joinTeam)))
            .then(Commands.literal(CLASS)
                .then(Commands.argument(CLASS, StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(PlayerClass.values().stream().map(PlayerClass::getShortName).map(StringArgumentType::escapeIfRequired), builder))
                    .executes(TeamsCommand::selectClass)))
            .then(Commands.literal(SCORE).executes(TeamsCommand::score))
            .then(Commands.literal("motd").executes(TeamsCommand::showMotd)
                .then(Commands.argument("text", StringArgumentType.greedyString()).requires(source -> source.hasPermission(2)).executes(TeamsCommand::setMotd)))
            .then(Commands.literal("vote").then(Commands.argument("option", IntegerArgumentType.integer(1, 5)).executes(TeamsCommand::vote)))
            .then(Commands.literal("stats").executes(context -> showStats(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument(PLAYER, EntityArgument.player()).requires(source -> source.hasPermission(2))
                    .executes(context -> showStats(context.getSource(), EntityArgument.getPlayer(context, PLAYER)))))
            .then(Commands.literal("leaderboard").executes(TeamsCommand::leaderboard)).then(Commands.literal("loadouts").executes(TeamsCommand::openLoadouts))
            .then(Commands.literal("list").then(Commands.literal("gametypes").executes(TeamsCommand::listGameTypes)).then(Commands.literal(TEAMS).executes(TeamsCommand::listTeams))
                .then(Commands.literal("classes").executes(TeamsCommand::listClasses)).then(Commands.literal("maps").executes(TeamsCommand::listMaps))
                .then(Commands.literal("rounds").executes(TeamsCommand::listRounds)).then(Commands.literal("loadouts").executes(TeamsCommand::listLoadoutPools))
                .then(Commands.literal("rewardboxes").executes(TeamsCommand::listRewardBoxes)))
            // Frequently used legacy spellings remain as thin aliases.
            .then(Commands.literal("listGametypes").executes(TeamsCommand::listGameTypes)).then(Commands.literal("listMaps").executes(TeamsCommand::listMaps))
            .then(Commands.literal("listRounds").executes(TeamsCommand::listRounds)).then(Commands.literal("listAllTeams").executes(TeamsCommand::listTeams))
            .then(Commands.literal("on").requires(source -> source.hasPermission(2)).executes(context ->
            {
                manager().setEnabled(true);
                return success(context, "Teams enabled");
            })).then(Commands.literal("off").requires(source -> source.hasPermission(2)).executes(context ->
            {
                manager().setEnabled(false);
                return success(context, "Teams disabled");
            })).then(booleanSetting("explosions", TeamsCommand::setExplosions)).then(booleanSetting("forceAdventure", TeamsCommand::setForceAdventure))
            .then(booleanSetting("forceAdventureMode", TeamsCommand::setForceAdventure)).then(booleanSetting("fuelNeeded", TeamsCommand::setFuelNeeded))
            .then(booleanSetting("vehiclesCanZoom", TeamsCommand::setVehiclesCanZoom));
        root = addRuleCommands(root)
            .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                .executes(context -> manager().startNextRound() ? success(context, MSG_ROUND_STARTED) : failure(context, MSG_NO_VALID_ROUND)))
            .then(Commands.literal("nextRound").requires(source -> source.hasPermission(2))
                .executes(context -> manager().startNextRound() ? success(context, "Advanced to the next round") : failure(context, MSG_NO_VALID_ROUND)))
            .then(Commands.literal("getOpKit").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("setloadoutpool").requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider
                        .suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), LoadoutPool.values().stream().map(LoadoutPool::getShortName)), builder))
                    .executes(TeamsCommand::setLoadoutPool)))
            .then(Commands.literal("xpmultiplier").requires(source -> source.hasPermission(2))
                .then(Commands.argument(VALUE, FloatArgumentType.floatArg(0F, 100F)).executes(TeamsCommand::setExperienceMultiplier)))
            .then(Commands.literal("xp").requires(source -> source.hasPermission(2))
                .then(Commands.argument(PLAYER, EntityArgument.player()).then(Commands.argument(AMOUNT, IntegerArgumentType.integer(1)).executes(TeamsCommand::giveExperience))))
            .then(Commands.literal("resetrank").requires(source -> source.hasPermission(2)).then(Commands.argument(PLAYER, EntityArgument.player()).executes(TeamsCommand::resetRank)))
            .then(Commands.literal("giverewardbox").requires(source -> source.hasPermission(2))
                .then(Commands.argument(PLAYER, EntityArgument.player())
                    .then(Commands.argument("box", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(RewardBox.values().stream().map(RewardBox::getShortName), builder)).executes(TeamsCommand::giveRewardBox))))
            .then(addMapAdminCommands(adminCommands()));
        addLabjacCommands(root);
        TeamsEditorCommand.addTo(root);

        com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        // The Labjac fork keeps rank commands under /ranks; the same handlers serve both spellings.
        dispatcher.register(Commands.literal("ranks").executes(TeamsCommand::ranksHelp).then(Commands.literal("help").executes(TeamsCommand::ranksHelp))
            .then(Commands.literal("prestige").executes(TeamsCommand::prestige)).then(Commands.literal("leaderboard").executes(TeamsCommand::leaderboard))
            .then(Commands.literal("stats").executes(context -> showStats(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument(PLAYER, EntityArgument.player()).executes(context -> showStats(context.getSource(), EntityArgument.getPlayer(context, PLAYER))))));
        dispatcher.register(Commands.literal("flansteams").redirect(node));
        dispatcher.register(Commands.literal("team").redirect(node));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> adminCommands()
    {
        return Commands.literal("admin").requires(source -> source.hasPermission(2)).then(Commands.literal(ENABLED).then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(context ->
        {
            manager().setEnabled(BoolArgumentType.getBool(context, VALUE));
            return success(context, "Teams " + (manager().isEnabled() ? ENABLED : DISABLED));
        }))).then(Commands.literal("voting").then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(context ->
        {
            boolean enabled = BoolArgumentType.getBool(context, VALUE);
            manager().setVoting(enabled);
            return success(context, "Round voting " + (enabled ? ENABLED : DISABLED));
        }))).then(Commands.literal("scoreDisplayTime").then(Commands.argument(SECONDS, IntegerArgumentType.integer(0, 86400)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, SECONDS);
            manager().setScoreDisplayTimeSeconds(seconds);
            return success(context, "Round results will be displayed for " + seconds + SECONDS_SUFFIX);
        }))).then(Commands.literal("rankUpdateTime").then(Commands.argument(SECONDS, IntegerArgumentType.integer(0, 86400)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, SECONDS);
            manager().setRankUpdateTimeSeconds(seconds);
            return success(context, seconds == 0 ? "The rank update screen is disabled" : "The rank update screen will appear for " + seconds + SECONDS_SUFFIX);
        }))).then(Commands.literal("votingTime").then(Commands.argument(SECONDS, IntegerArgumentType.integer(0, 86400)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, SECONDS);
            manager().setVotingTimeSeconds(seconds);
            return success(context, "Round voting will last " + seconds + SECONDS_SUFFIX);
        }))).then(Commands.literal("motd").then(Commands.argument("text", StringArgumentType.greedyString()).executes(TeamsCommand::setMotd)))
            .then(Commands.literal("autobalancetime").then(Commands.argument(SECONDS, IntegerArgumentType.integer(11, 86400)).executes(context ->
            {
                int seconds = IntegerArgumentType.getInteger(context, SECONDS);
                manager().setAutoBalanceIntervalSeconds(seconds);
                return success(context, "Autobalance will run every " + seconds + " seconds with a 10-second warning");
            }))).then(Commands.literal("roundsGenerator").then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(context ->
            {
                boolean enabled = BoolArgumentType.getBool(context, VALUE);
                manager().setRoundsGenerator(enabled);
                return success(context, "Rounds generator " + (enabled ? ENABLED : DISABLED));
            })))
            .then(Commands.literal("start").executes(context -> manager().startNextRound() ? success(context, MSG_ROUND_STARTED) : failure(context, MSG_NO_VALID_ROUND))
                .then(Commands.argument(INDEX, IntegerArgumentType.integer(0))
                    .executes(context -> manager().startRound(IntegerArgumentType.getInteger(context, INDEX)) ? success(context, MSG_ROUND_STARTED) : failure(context, MSG_INVALID_ROUND_INDEX))))
            .then(Commands.literal("next").executes(context -> manager().startNextRound() ? success(context, "Advanced to the next round") : failure(context, MSG_NO_VALID_ROUND)))
            .then(Commands.literal("setnext").then(Commands.argument(INDEX, IntegerArgumentType.integer(0)).executes(TeamsCommand::queueRound)))
            .then(Commands.literal("ping").executes(TeamsCommand::listPings))
            .then(Commands.literal("bltss").executes(TeamsCommand::showBulletSnapshot)
                .then(Commands.argument("min", IntegerArgumentType.integer(0, 100)).then(Commands.argument(DIVISOR, IntegerArgumentType.integer(0, 1000)).executes(TeamsCommand::setBulletSnapshot))))
            .then(Commands.literal("stop").executes(context ->
            {
                manager().stopRound();
                return success(context, "Round stopped");
            })).then(Commands.literal("arena").executes(context ->
            {
                manager().applyArenaPreset();
                return success(context, "Arena preset applied");
            })).then(Commands.literal("survival").executes(context ->
            {
                manager().applySurvivalPreset();
                return success(context, "Survival preset applied");
            })).then(Commands.literal("kit").executes(TeamsCommand::giveKit))
            .then(Commands.literal("loadoutpool")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider
                        .suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), LoadoutPool.values().stream().map(LoadoutPool::getShortName)), builder))
                    .executes(TeamsCommand::setLoadoutPool)))
            .then(Commands.literal("xpmultiplier").then(Commands.argument(VALUE, FloatArgumentType.floatArg(0F, 100F)).executes(TeamsCommand::setExperienceMultiplier)))
            .then(
                Commands.literal("xp").then(Commands.argument(PLAYER, EntityArgument.player()).then(Commands.argument(AMOUNT, IntegerArgumentType.integer(1)).executes(TeamsCommand::giveExperience))))
            .then(Commands.literal("resetrank").then(Commands.argument(PLAYER, EntityArgument.player()).executes(TeamsCommand::resetRank)))
            .then(Commands.literal("giverewardbox")
                .then(Commands.argument(PLAYER, EntityArgument.player())
                    .then(Commands.argument("box", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(RewardBox.values().stream().map(RewardBox::getShortName), builder)).executes(TeamsCommand::giveRewardBox))))
            .then(Commands.literal("map")
                .then(
                    Commands.literal("add").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("name", StringArgumentType.greedyString()).executes(TeamsCommand::addMap))))
                .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder)).executes(TeamsCommand::removeMap))))
            .then(Commands.literal("round")
                .then(Commands.literal("add")
                    .then(Commands.argument("map", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder))
                        .then(Commands.argument(GAMETYPE, StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(GameType.values().stream().map(GameType::getId), builder)).then(sideArgument(1)))))
                .then(Commands.literal("remove").then(Commands.argument(INDEX, IntegerArgumentType.integer(0)).executes(TeamsCommand::removeRound))))
            .then(Commands.literal("setvariable")
                .then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument(VALUE, StringArgumentType.word()).executes(TeamsCommand::setVariable))));
    }

    /**
     * Commands ported from the Labjac fork under their legacy spellings: map and rotation
     * administration, the pacifism, movie prop and sea level rules, the screen shake test,
     * prestige and the deployment map.
     */
    private static void addLabjacCommands(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(Commands.literal("prestige").executes(TeamsCommand::prestige)).then(Commands.literal("deploy").executes(TeamsCommand::openDeployment))
            .then(Commands.literal("listRotation").executes(TeamsCommand::listRounds)).then(rule("pacifism", TeamsManager::setPacifism, enabled -> "Pacifism is now " + (enabled ? ENABLED : DISABLED)))
            .then(rule("movieProp", TeamsManager::setMovieProp, enabled -> "Vehicle self-driving movie prop mode is now " + (enabled ? ENABLED : DISABLED)))
            .then(Commands.literal("seaLevel").requires(source -> source.hasPermission(2)).then(Commands.literal("default").executes(context -> setSeaLevel(context, null)))
                .then(Commands.argument("y", IntegerArgumentType.integer(-2048, 2048)).executes(context -> setSeaLevel(context, IntegerArgumentType.getInteger(context, "y")))))
            .then(Commands.literal("shake").requires(source -> source.hasPermission(2)).then(Commands.argument("strength", FloatArgumentType.floatArg(0F, 1000F)).executes(TeamsCommand::shake)))
            .then(Commands.literal("setRank").requires(source -> source.hasPermission(2))
                .then(Commands.argument(RANK, IntegerArgumentType.integer(1)).executes(context -> setRank(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument(PLAYER, EntityArgument.player()).executes(context -> setRank(context, EntityArgument.getPlayer(context, PLAYER))))));
        root.then(mapAdd("addMap")).then(mapAddWithBounds("addMapWithBounds")).then(mapRemove("removeMap")).then(mapRename("setMapName")).then(mapRename("renameMap")).then(mapImage("setMapImage"));
        for (String name : List.of("addRound", "addToRotation", "addMapToRotation", "addRotation"))
            root.then(roundAdd(name));
        for (String name : List.of("removeRound", "removeFromRotation", "removeMapFromRotation", "removeRotation"))
            root.then(roundRemove(name));
    }

    /** The map commands of the Labjac fork, under {@code /teams admin map}. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> addMapAdminCommands(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> admin)
    {
        return admin
            .then(Commands.literal("map").then(mapAddWithBounds("addWithBounds")).then(mapRename("rename")).then(mapImage(IMAGE))
                .then(Commands.literal("bounds")
                    .then(mapArgument().then(Commands.argument(FROM, BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(TeamsCommand::setMapBounds)))))
                .then(Commands.literal("clearBounds").then(mapArgument().executes(TeamsCommand::clearMapBounds))))
            .then(Commands.literal("setrank").then(Commands.argument(RANK, IntegerArgumentType.integer(1)).executes(context -> setRank(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument(PLAYER, EntityArgument.player()).executes(context -> setRank(context, EntityArgument.getPlayer(context, PLAYER))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> mapArgument()
    {
        return Commands.argument("id", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> operatorLiteral(String name)
    {
        return Commands.literal(name).requires(source -> source.hasPermission(2));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mapAdd(String name)
    {
        return operatorLiteral(name).then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("name", StringArgumentType.greedyString()).executes(TeamsCommand::addMap)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mapAddWithBounds(String name)
    {
        return operatorLiteral(name).then(Commands.argument(FROM, BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos())
            .then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("name", StringArgumentType.greedyString()).executes(TeamsCommand::addMapWithBounds)))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mapRemove(String name)
    {
        return operatorLiteral(name).then(mapArgument().executes(TeamsCommand::removeMap));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mapRename(String name)
    {
        return operatorLiteral(name).then(mapArgument().then(Commands.argument("name", StringArgumentType.greedyString()).executes(TeamsCommand::renameMap)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mapImage(String name)
    {
        return operatorLiteral(name).then(mapArgument().then(Commands.argument(IMAGE, StringArgumentType.word()).executes(TeamsCommand::setMapImage)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> roundAdd(String name)
    {
        return operatorLiteral(name)
            .then(Commands.argument("map", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder)).then(Commands.argument(GAMETYPE, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(GameType.values().stream().map(GameType::getId), builder)).then(sideArgument(1))));
    }

    /**
     * The teams of a round, then its length and score limit: {@code us+uk vs germany+italy 15 50}.
     *
     * <p>
     * Each side is one argument, with allied teams joined by {@code +}, and further sides follow a
     * {@code vs}. A quoted argument may also list several sides separated by commas, which is the
     * way rounds were written before alliances: {@code "red,blue"}.
     * </p>
     */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> sideArgument(int index)
    {
        com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> side = Commands.argument(SIDE + index, StringArgumentType.string()).suggests((context,
            builder) -> SharedSuggestionProvider.suggest(Team.values().stream().filter(team -> team != Team.SPECTATORS).map(Team::getShortName).map(StringArgumentType::escapeIfRequired), builder));
        side.then(Commands.argument(MINUTES, IntegerArgumentType.integer(1, 1440)).then(Commands.argument(SCORE, IntegerArgumentType.integer(1)).executes(TeamsCommand::addRound)));
        if (index < TeamsRound.MAX_SIDES)
            side.then(Commands.literal("vs").then(sideArgument(index + 1)));
        return side;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> roundRemove(String name)
    {
        return operatorLiteral(name).then(Commands.argument(INDEX, IntegerArgumentType.integer(0)).executes(TeamsCommand::removeRound));
    }

    private static int addMapWithBounds(CommandContext<CommandSourceStack> context)
    {
        try
        {
            TeamsMap.Bounds bounds = TeamsMap.Bounds.of(BlockPosArgument.getBlockPos(context, FROM), BlockPosArgument.getBlockPos(context, "to"));
            TeamsMap map = manager().addMap(StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "name"), context.getSource().getLevel(), bounds);
            return success(context, "Created map " + map.getName() + " with bounds");
        }
        catch (IllegalArgumentException e)
        {
            return failure(context, e.getMessage());
        }
    }

    private static int setMapBounds(CommandContext<CommandSourceStack> context)
    {
        TeamsMap.Bounds bounds = TeamsMap.Bounds.of(BlockPosArgument.getBlockPos(context, FROM), BlockPosArgument.getBlockPos(context, "to"));
        return manager().setMapBounds(StringArgumentType.getString(context, "id"), bounds) ? success(context, "Map bounds set") : failure(context, MSG_UNKNOWN_MAP);
    }

    private static int clearMapBounds(CommandContext<CommandSourceStack> context)
    {
        return manager().setMapBounds(StringArgumentType.getString(context, "id"), null) ? success(context, "Map bounds removed") : failure(context, MSG_UNKNOWN_MAP);
    }

    private static int renameMap(CommandContext<CommandSourceStack> context)
    {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        return manager().renameMap(id, name) ? success(context, "Renamed map " + id + " to " + name) : failure(context, MSG_UNKNOWN_MAP);
    }

    private static int setMapImage(CommandContext<CommandSourceStack> context)
    {
        String id = StringArgumentType.getString(context, "id");
        if (manager().getMap(id).isEmpty())
            return failure(context, MSG_UNKNOWN_MAP);
        String image = StringArgumentType.getString(context, IMAGE);
        return manager().setMapImage(id, image)
            ? success(context, "Set deploy image for " + id + " to " + image)
            : failure(context, "Map image names may only contain letters, numbers, underscores and dashes");
    }

    private static int setSeaLevel(CommandContext<CommandSourceStack> context, @org.jetbrains.annotations.Nullable Integer seaLevel)
    {
        manager().setSeaLevelOverride(seaLevel);
        return success(context, seaLevel == null ? "Sea level is taken from each dimension again" : "Sea level is now considered to be " + seaLevel);
    }

    /** Shakes the screen of the operator who asked, to try out an explosion strength. */
    private static int shake(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        float strength = FloatArgumentType.getFloat(context, "strength");
        com.flansmodultimate.network.PacketHandler.sendTo(new com.flansmodultimate.network.client.effects.PacketCombatFeedback(true, strength, 0F), context.getSource().getPlayerOrException());
        return 1;
    }

    private static int setRank(CommandContext<CommandSourceStack> context, ServerPlayer player)
    {
        int rank = IntegerArgumentType.getInteger(context, RANK);
        manager().getStats(player).setRank(rank);
        manager().markPlayerDataDirty();
        manager().syncPlayer(player, com.flansmodultimate.network.client.teams.PacketTeamsState.OpenScreen.NONE);
        return success(context, "Set rank of " + player.getScoreboardName() + " to " + rank);
    }

    private static int prestige(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerStats stats = manager().getStats(player);
        if (stats.getPrestige() >= PlayerStats.MAX_PRESTIGE)
            return failure(context, "You have reached the highest prestige");
        if (!manager().gainPrestige(player))
            return failure(context, "You must first reach rank " + manager().getPrestigeRank() + " to gain prestige");
        return success(context, "Prestiged! New prestige: " + stats.getPrestige() + ". Your rank has been reset");
    }

    private static int ranksHelp(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSuccess(() -> Component.literal("/ranks stats [player], /ranks leaderboard, /ranks prestige (resets your rank and unlocks prestige classes)"), false);
        return 1;
    }

    private static int openDeployment(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        if (manager().getCurrentGameType().map(type -> !type.getObjectiveMode().usesDeployment()).orElse(true))
            return failure(context, "The deployment map is only used in Conquest and Assault rounds");
        manager().syncPlayer(context.getSource().getPlayerOrException(), com.flansmodultimate.network.client.teams.PacketTeamsState.OpenScreen.DEPLOY);
        return 1;
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Team team = Team.getTeam(StringArgumentType.getString(context, "team"));
        if (team == null)
            return failure(context, "Unknown team");
        if (!manager().selectTeam(player, team, false))
            return failure(context, "That team is unavailable or joining it would unbalance the round");
        return success(context, "You will join " + team.getName() + " on respawn");
    }

    private static int help(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSuccess(() -> Component.literal(
            "/teams loadouts, /teams join <team>, /teams class <class>, /teams vote <number>, /teams score, /teams motd, /teams stats, /teams prestige, /teams deploy, /teams list <gametypes|teams|classes|loadouts|rewardboxes|maps|rounds>"),
            false);
        if (context.getSource().hasPermission(2))
        {
            context.getSource().sendSuccess(() -> Component.literal(
                "Rules: /teams <explosions|forceAdventure|fuelNeeded|vehiclesCanZoom|overrideHunger|bombs|shells|bullets|canBreakGuns|canBreakGlass|survivalCanBreakVehicles|survivalCanPlaceVehicles|armourDrops|vehiclesBreakBlocks|useRotation|autobalance|alliedSpawns> <true|false>, /teams weaponDrops <on|off|smart>, /teams <mgLife|planeLife|vehicleLife|mechaLife|aaLife> <seconds>"),
                false);
            context.getSource()
                .sendSuccess(() -> Component.literal("Rounds and server: /teams setRound <index>, /teams goToMap <index>, /teams ping, /teams bltss [<min> <divisor>], /teams motd <text>"), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "Maps and Labjac rules: /teams <addMap|addMapWithBounds|removeMap|renameMap|setMapImage|addRound|removeRound|listRotation>, /teams <pacifism|movieProp> <true|false>, /teams seaLevel <y|default>, /teams shake <strength>, /teams setRank <rank> [player]"),
                false);
            context.getSource().sendSuccess(() -> Component.literal(
                "Administration: /teams admin <loadoutpool|xpmultiplier|xp|resetrank|giverewardbox|enabled|voting|scoreDisplayTime|rankUpdateTime|votingTime|autobalancetime|roundsGenerator|start|next|setnext|stop|arena|survival|kit|map|round|setvariable|ping|bltss>"),
                false);
        }
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> booleanSetting(String name, java.util.function.ToIntFunction<CommandContext<CommandSourceStack>> handler)
    {
        return Commands.literal(name).requires(source -> source.hasPermission(2)).then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(handler::applyAsInt));
    }

    /**
     * The individual Teams rules of 1.7.10, each under its legacy spelling, so an operator can tune one rule
     * without applying the whole arena or survival preset.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> addRuleCommands(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(rule("overrideHunger", TeamsManager::setOverrideHunger, enabled -> "Players will " + (enabled ? NO_LONGER : "now") + " get hungry during rounds"))
            .then(rule("noHunger", TeamsManager::setOverrideHunger, enabled -> "Players will " + (enabled ? NO_LONGER : "now") + " get hungry during rounds"))
            .then(rule("bombs", TeamsManager::setBombsEnabled, enabled -> "Bombs are now " + (enabled ? ENABLED : DISABLED)))
            .then(rule("allowBombs", TeamsManager::setBombsEnabled, enabled -> "Bombs are now " + (enabled ? ENABLED : DISABLED)))
            .then(rule("shells", TeamsManager::setShellsEnabled, enabled -> "Shells are now " + (enabled ? ENABLED : DISABLED)))
            .then(rule("bullets", TeamsManager::setBulletsEnabled, enabled -> "Bullets are now " + (enabled ? ENABLED : DISABLED)))
            .then(rule("bulletsEnabled", TeamsManager::setBulletsEnabled, enabled -> "Bullets are now " + (enabled ? ENABLED : DISABLED)))
            .then(rule("canBreakGuns", TeamsManager::setCanBreakGuns, enabled -> "AAGuns and MGs can " + (enabled ? "now" : NO_LONGER) + " be broken"))
            .then(rule("canBreakGlass", TeamsManager::setCanBreakGlass, enabled -> "Glass and glowstone can " + (enabled ? "now" : NO_LONGER) + " be broken"))
            .then(rule("survivalCanBreakVehicles", TeamsManager::setSurvivalCanBreakVehicles, enabled -> "Survival players can " + (enabled ? "now" : NO_LONGER) + " break vehicles"))
            .then(rule("survivalCanPlaceVehicles", TeamsManager::setSurvivalCanPlaceVehicles, enabled -> "Survival players can " + (enabled ? "now" : NO_LONGER) + " place vehicles"))
            .then(rule("armourDrops", TeamsManager::setArmourDrops, enabled -> "Armour will " + (enabled ? "now" : NO_LONGER) + " be dropped"))
            .then(rule("armorDrops", TeamsManager::setArmourDrops, enabled -> "Armour will " + (enabled ? "now" : NO_LONGER) + " be dropped"))
            .then(rule("vehiclesBreakBlocks", TeamsManager::setDriveablesBreakBlocks, enabled -> "Vehicles will " + (enabled ? "now" : NO_LONGER) + " break blocks"))
            .then(rule("alliedSpawns", TeamsManager::setAlliedSpawnSharing, enabled -> "Allied teams will " + (enabled ? "now" : NO_LONGER) + " spawn at each other's bases"))
            .then(Commands.literal("weaponDrops").requires(source -> source.hasPermission(2))
                .then(Commands.argument("mode", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off", "smart"), builder))
                    .executes(TeamsCommand::setWeaponDrops)))
            .then(lifeSetting("mgLife", "MGs", TeamsManager::setMgLife)).then(lifeSetting("planeLife", "Planes", TeamsManager::setPlaneLife))
            .then(lifeSetting("vehicleLife", "Vehicles", TeamsManager::setVehicleLife)).then(lifeSetting("mechaLife", "Mechas", TeamsManager::setMechaLife))
            .then(lifeSetting("aaLife", "AA guns", TeamsManager::setAaLife))
            .then(Commands.literal("useRotation").requires(source -> source.hasPermission(2)).then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(context ->
            {
                // A fixed rotation is the opposite of voting for the next round
                manager().setVoting(!BoolArgumentType.getBool(context, VALUE));
                return success(context, "Voting is now " + (manager().isVoting() ? ENABLED : DISABLED));
            })))
            .then(Commands.literal("autobalance").requires(source -> source.hasPermission(2))
                .then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(context -> setCurrentVariable(context, "autobalance", String.valueOf(BoolArgumentType.getBool(context, VALUE))))))
            .then(Commands.literal("setRound").requires(source -> source.hasPermission(2)).then(Commands.argument(INDEX, IntegerArgumentType.integer(0)).executes(TeamsCommand::queueRound)))
            .then(Commands.literal("goToMap").requires(source -> source.hasPermission(2))
                .then(Commands.argument(INDEX, IntegerArgumentType.integer(0))
                    .executes(context -> manager().startRound(IntegerArgumentType.getInteger(context, INDEX)) ? success(context, MSG_ROUND_STARTED) : failure(context, MSG_INVALID_ROUND_INDEX))))
            .then(Commands.literal("getSticks").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("getOpSticks").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("ping").requires(source -> source.hasPermission(2)).executes(TeamsCommand::listPings))
            .then(Commands.literal("bltss").requires(source -> source.hasPermission(2)).executes(TeamsCommand::showBulletSnapshot)
                .then(Commands.argument("min", IntegerArgumentType.integer(0, 100)).then(Commands.argument(DIVISOR, IntegerArgumentType.integer(0, 1000)).executes(TeamsCommand::setBulletSnapshot))))
            .then(Commands.literal("showbltss").requires(source -> source.hasPermission(2)).executes(TeamsCommand::showBulletSnapshot));
        return root;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> rule(String name, java.util.function.BiConsumer<TeamsManager, Boolean> setter,
        it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction<String> message)
    {
        return booleanSetting(name, context ->
        {
            boolean enabled = BoolArgumentType.getBool(context, VALUE);
            setter.accept(manager(), enabled);
            manager().saveSettings();
            return success(context, message.get(enabled));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> lifeSetting(String name, String what, java.util.function.ObjIntConsumer<TeamsManager> setter)
    {
        return Commands.literal(name).requires(source -> source.hasPermission(2)).then(Commands.argument(SECONDS, IntegerArgumentType.integer(0)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, SECONDS);
            setter.accept(manager(), seconds);
            manager().saveSettings();
            return success(context, seconds > 0 ? what + " will despawn after " + seconds + SECONDS_SUFFIX : what + " will not despawn");
        }));
    }

    private static int setWeaponDrops(CommandContext<CommandSourceStack> context)
    {
        String mode = StringArgumentType.getString(context, "mode").toLowerCase(java.util.Locale.ROOT);
        TeamsManager.EnumWeaponDrop drops;
        String message;
        switch (mode)
        {
            case "on" -> {
                drops = TeamsManager.EnumWeaponDrop.DROPS;
                message = "Weapons will be dropped normally";
            }
            case "off" -> {
                drops = TeamsManager.EnumWeaponDrop.NONE;
                message = "Weapons will not be dropped";
            }
            case "smart" -> {
                drops = TeamsManager.EnumWeaponDrop.SMART_DROPS;
                message = "Smart drops enabled";
            }
            default -> {
                return failure(context, "Weapon drops must be on, off or smart");
            }
        }
        manager().setWeaponDrops(drops);
        manager().saveSettings();
        return success(context, message);
    }

    private static int queueRound(CommandContext<CommandSourceStack> context)
    {
        int index = IntegerArgumentType.getInteger(context, INDEX);
        if (!manager().queueNextRound(index))
            return failure(context, MSG_INVALID_ROUND_INDEX);
        TeamsRound round = manager().getRounds().get(index);
        manager().broadcast(Component.literal("Next round will be " + round.getGameTypeId() + " in " + round.getMapId()));
        return 1;
    }

    private static int listPings(CommandContext<CommandSourceStack> context)
    {
        int sum = 0;
        int count = 0;
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers())
        {
            int ping = EntityPlatform.latency(player);
            context.getSource().sendSuccess(() -> Component.literal("[Ping] " + ping + " : " + player.getScoreboardName()), false);
            if (ping > 0)
            {
                sum += ping;
                count++;
            }
        }
        if (count > 0)
        {
            double average = (double) sum / count;
            context.getSource().sendSuccess(() -> Component.literal("[PingAverage] " + String.format(java.util.Locale.ROOT, "%.1f", average)), false);
        }
        return count;
    }

    private static int setBulletSnapshot(CommandContext<CommandSourceStack> context)
    {
        int min = IntegerArgumentType.getInteger(context, "min");
        int divisor = IntegerArgumentType.getInteger(context, DIVISOR);
        com.flansmodultimate.config.ModCommonConfig.setBulletSnapshot(min, divisor);
        return showBulletSnapshot(context);
    }

    private static int showBulletSnapshot(CommandContext<CommandSourceStack> context)
    {
        int min = manager().getBulletSnapshotMin();
        int divisor = manager().getBulletSnapshotDivisor();
        context.getSource().sendSuccess(() -> Component.literal("[BulletDelay] Min=" + min + " : Divisor=" + divisor + " (bullets use player snapshot Min + Ping / Divisor)"), false);
        return 1;
    }

    private static int setExplosions(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, VALUE);
        manager().setExplosionsBreakBlocks(enabled);
        return success(context, "Terrain damage from explosions " + (enabled ? ENABLED : DISABLED));
    }

    private static int setForceAdventure(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, VALUE);
        manager().setForceAdventureMode(enabled);
        return success(context, "Adventure mode will " + (enabled ? "now" : NO_LONGER) + " be forced on respawn");
    }

    private static int setFuelNeeded(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, VALUE);
        manager().setVehiclesNeedFuel(enabled);
        return success(context, "Vehicles will " + (enabled ? "now" : NO_LONGER) + " require fuel");
    }

    private static int setVehiclesCanZoom(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, VALUE);
        manager().setVehiclesCanZoom(enabled);
        return success(context, "Driver-controlled vehicle zoom " + (enabled ? ENABLED : DISABLED));
    }

    private static int openLoadouts(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        if (manager().getCurrentLoadoutPool().isEmpty())
            return failure(context, "No ranked loadout pool is active");
        manager().syncLoadouts(context.getSource().getPlayerOrException(), com.flansmodultimate.network.client.teams.PacketLoadoutState.OpenScreen.HUB, 0, "");
        return 1;
    }

    private static int listLoadoutPools(CommandContext<CommandSourceStack> context)
    {
        LoadoutPool.values().forEach(pool -> context.getSource().sendSuccess(() -> Component.literal(pool.getShortName() + " — " + pool.getName()), false));
        return LoadoutPool.values().size();
    }

    private static int listRewardBoxes(CommandContext<CommandSourceStack> context)
    {
        RewardBox.values().forEach(box -> context.getSource().sendSuccess(() -> Component.literal(box.getShortName() + " — " + box.getName()), false));
        return RewardBox.values().size();
    }

    private static int setLoadoutPool(CommandContext<CommandSourceStack> context)
    {
        String id = StringArgumentType.getString(context, "id");
        return manager().setCurrentLoadoutPool(id) ? success(context, "Loadout pool set to " + id) : failure(context, "Unknown loadout pool");
    }

    private static int setExperienceMultiplier(CommandContext<CommandSourceStack> context)
    {
        float value = FloatArgumentType.getFloat(context, VALUE);
        manager().setExperienceMultiplier(value);
        return success(context, "Teams XP multiplier set to " + value);
    }

    private static int giveExperience(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER);
        int amount = IntegerArgumentType.getInteger(context, AMOUNT);
        manager().awardExperience(player, amount);
        return success(context, "Granted " + amount + " XP to " + player.getScoreboardName());
    }

    private static int resetRank(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER);
        manager().getStats(player).resetRankProgress();
        manager().markPlayerDataDirty();
        return success(context, "Reset ranked progress for " + player.getScoreboardName());
    }

    private static int giveRewardBox(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER);
        String box = StringArgumentType.getString(context, "box");
        return manager().grantRewardBox(player, box, RewardBoxInstance.Origin.COMMAND)
            ? success(context, "Granted " + box + " to " + player.getScoreboardName())
            : failure(context, "Unknown reward box");
    }

    private static int selectClass(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerClass playerClass = PlayerClass.getPlayerClass(StringArgumentType.getString(context, CLASS));
        Component refusal = manager().describeClassRefusal(player, playerClass);
        if (refusal != null || !manager().selectClass(player, playerClass))
        {
            context.getSource().sendFailure(refusal == null ? Component.translatable("message.flansmodultimate.teams.class_unavailable") : refusal);
            return 0;
        }
        manager().confirmSelection(player);
        return success(context, "Class selected: " + playerClass.getName());
    }

    private static int showMotd(CommandContext<CommandSourceStack> context)
    {
        return success(context, manager().getMotd());
    }

    private static int setMotd(CommandContext<CommandSourceStack> context)
    {
        String text = StringArgumentType.getString(context, "text");
        manager().setMotd(text);
        return success(context, "Message of the day: " + manager().getMotd());
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
        if (!manager().castVote(context.getSource().getPlayerOrException(), option))
            return failure(context, "There is no active vote with that option");
        return success(context, "Vote recorded for option " + option);
    }

    private static int showStats(CommandSourceStack source, ServerPlayer player)
    {
        PlayerStats stats = TeamsManager.getInstance().getStats(player);
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + ": rank " + stats.getRank() + ", prestige " + stats.getPrestige() + ", " + stats.getTotalExperience() + " XP, "
            + stats.getKills() + " kills, " + stats.getDeaths() + " deaths, " + stats.getCapturedFlags() + " captures"), false);
        return stats.getTotalExperience();
    }

    private static int leaderboard(CommandContext<CommandSourceStack> context)
    {
        List<PlayerStats> stats = manager().getAllStats().stream().sorted(Comparator.comparingInt(PlayerStats::getTotalExperience).reversed()).limit(10).toList();
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
        GameType.values().forEach(type -> context.getSource().sendSuccess(() -> Component.literal(type.getId() + " — " + type.getName() + " (" + type.getTeamLimits().describe() + ")"), false));
        return GameType.values().size();
    }

    private static int listTeams(CommandContext<CommandSourceStack> context)
    {
        Team.values().forEach(team -> context.getSource().sendSuccess(() -> Component.literal(team.getShortName() + " — " + team.getName()), false));
        return Team.values().size();
    }

    private static int listClasses(CommandContext<CommandSourceStack> context)
    {
        PlayerClass.values().forEach(type -> context.getSource().sendSuccess(() -> Component.literal(type.getShortName() + " — " + type.getName() + " (rank " + type.getUnlockLevel() + ")"), false));
        return PlayerClass.values().size();
    }

    private static int listMaps(CommandContext<CommandSourceStack> context)
    {
        manager().getMaps().forEach(map -> context.getSource().sendSuccess(() -> Component.literal(map.getShortName() + " — " + map.getName() + " [" + map.getDimension().location() + "]"), false));
        return manager().getMaps().size();
    }

    private static int listRounds(CommandContext<CommandSourceStack> context)
    {
        List<TeamsRound> rounds = manager().getRounds();
        for (int i = 0; i < rounds.size(); i++)
        {
            int index = i;
            TeamsRound round = rounds.get(i);
            context.getSource().sendSuccess(
                () -> Component
                    .literal(index + ": " + round.getGameTypeId() + " @ " + round.getMapId() + " [" + round.describeTeams() + "] " + round.getTimeLimitMinutes() + "m / " + round.getScoreLimit()),
                false);
        }
        return rounds.size();
    }

    private static int addMap(CommandContext<CommandSourceStack> context)
    {
        try
        {
            TeamsMap map = manager().addMap(StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "name"), context.getSource().getLevel());
            return success(context, "Created map " + map.getName());
        }
        catch (IllegalArgumentException e)
        {
            return failure(context, e.getMessage());
        }
    }

    private static int removeMap(CommandContext<CommandSourceStack> context)
    {
        return manager().removeMap(StringArgumentType.getString(context, "id")) ? success(context, "Map removed") : failure(context, MSG_UNKNOWN_MAP);
    }

    private static int addRound(CommandContext<CommandSourceStack> context)
    {
        try
        {
            List<String> sideArguments = new ArrayList<>();
            for (com.mojang.brigadier.context.ParsedCommandNode<CommandSourceStack> node : context.getNodes())
                if (node.getNode() instanceof com.mojang.brigadier.tree.ArgumentCommandNode<?, ?> && node.getNode().getName().startsWith(SIDE))
                    sideArguments.add(StringArgumentType.getString(context, node.getNode().getName()));
            TeamsRound.Lineup lineup = TeamsRound.parseLineup(sideArguments);
            TeamsRound round = manager().addRound(StringArgumentType.getString(context, "map"), StringArgumentType.getString(context, GAMETYPE), lineup.teamIds(), lineup.sides(),
                IntegerArgumentType.getInteger(context, MINUTES), IntegerArgumentType.getInteger(context, SCORE));
            return success(context, "Round added: " + round.getGameTypeId() + " @ " + round.getMapId() + " [" + round.describeTeams() + "]");
        }
        catch (IllegalArgumentException e)
        {
            return failure(context, e.getMessage());
        }
    }

    private static int removeRound(CommandContext<CommandSourceStack> context)
    {
        return manager().removeRound(IntegerArgumentType.getInteger(context, INDEX)) ? success(context, "Round removed") : failure(context, MSG_INVALID_ROUND_INDEX);
    }

    private static int setVariable(CommandContext<CommandSourceStack> context)
    {
        return setCurrentVariable(context, StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, VALUE));
    }

    /**
     * Sets a variable of the game type being played. {@code scorelimit} applies to every game type, as in
     * 1.7.10, and changes the limit of the round in progress.
     */
    private static int setCurrentVariable(CommandContext<CommandSourceStack> context, String name, String value)
    {
        GameType type = manager().getCurrentGameType().orElse(null);
        if (type == null)
            return failure(context, "There is no game type to set variables for");
        try
        {
            if ("scorelimit".equalsIgnoreCase(name))
            {
                if (!manager().setCurrentScoreLimit(Integer.parseInt(value)))
                    return failure(context, "The score limit must be at least 1");
            }
            else if (!type.setVariable(name, value))
                return failure(context, "Unknown variable for the current game type");
        }
        catch (NumberFormatException e)
        {
            return failure(context, "Invalid value: " + value);
        }
        manager().saveSettings();
        return success(context, "Set variable " + name + " in game type " + type.getId() + " to " + value);
    }

    private static int giveKit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (ItemStack stack : List.of(new ItemStack(FlansModItems.opStick.get()), new ItemStack(FlansModItems.flagpoleItem.get(), 16), new ItemStack(FlansModItems.capturePointItem.get(), 5),
            new ItemStack(FlansModItems.playerSpawnerItem.get(), 16), new ItemStack(FlansModItems.itemSpawnerItem.get(), 16), new ItemStack(FlansModItems.vehicleSpawnerItem.get(), 16)))
            if (!player.getInventory().add(stack))
                player.drop(stack, false);
        return success(context, "Teams operator kit added to your inventory");
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestMaps(com.mojang.brigadier.suggestion.SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(TeamsManager.getInstance().getMaps().stream().map(TeamsMap::getShortName), builder);
    }

    private static TeamsManager manager()
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
