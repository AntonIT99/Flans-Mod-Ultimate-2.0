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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Brigadier implementation of the legacy /teams administration and player commands. */
public final class TeamsCommand
{
    private TeamsCommand()
    {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("teams").executes(TeamsCommand::help)
            .then(Commands.literal("help").executes(TeamsCommand::help))
            .then(Commands.literal("join")
                .then(Commands.argument("team", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(Team.values().stream().map(Team::getShortName), builder))
                    .executes(TeamsCommand::joinTeam)))
            .then(Commands.literal("class")
                .then(Commands.argument("class", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(PlayerClass.values().stream().map(PlayerClass::getShortName), builder)).executes(TeamsCommand::selectClass)))
            .then(Commands.literal("score").executes(TeamsCommand::score))
            .then(Commands.literal("motd").executes(TeamsCommand::showMotd)
                .then(Commands.argument("text", StringArgumentType.greedyString()).requires(source -> source.hasPermission(2)).executes(TeamsCommand::setMotd)))
            .then(Commands.literal("vote").then(Commands.argument("option", IntegerArgumentType.integer(1, 5)).executes(TeamsCommand::vote)))
            .then(Commands.literal("stats").executes(context -> showStats(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                    .executes(context -> showStats(context.getSource(), EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("leaderboard").executes(TeamsCommand::leaderboard)).then(Commands.literal("loadouts").executes(TeamsCommand::openLoadouts))
            .then(Commands.literal("list").then(Commands.literal("gametypes").executes(TeamsCommand::listGameTypes)).then(Commands.literal("teams").executes(TeamsCommand::listTeams))
                .then(Commands.literal("classes").executes(TeamsCommand::listClasses)).then(Commands.literal("maps").executes(TeamsCommand::listMaps))
                .then(Commands.literal("rounds").executes(TeamsCommand::listRounds)).then(Commands.literal("loadouts").executes(TeamsCommand::listLoadoutPools))
                .then(Commands.literal("rewardboxes").executes(TeamsCommand::listRewardBoxes)))
            // Frequently used legacy spellings remain as thin aliases.
            .then(Commands.literal("listGametypes").executes(TeamsCommand::listGameTypes)).then(Commands.literal("listMaps").executes(TeamsCommand::listMaps))
            .then(Commands.literal("listRounds").executes(TeamsCommand::listRounds)).then(Commands.literal("listAllTeams").executes(TeamsCommand::listTeams))
            .then(Commands.literal("on").requires(source -> source.hasPermission(2)).executes(context ->
            {
                manager(context).setEnabled(true);
                return success(context, "Teams enabled");
            })).then(Commands.literal("off").requires(source -> source.hasPermission(2)).executes(context ->
            {
                manager(context).setEnabled(false);
                return success(context, "Teams disabled");
            })).then(booleanSetting("explosions", TeamsCommand::setExplosions)).then(booleanSetting("forceAdventure", TeamsCommand::setForceAdventure))
            .then(booleanSetting("forceAdventureMode", TeamsCommand::setForceAdventure)).then(booleanSetting("fuelNeeded", TeamsCommand::setFuelNeeded))
            .then(booleanSetting("vehiclesCanZoom", TeamsCommand::setVehiclesCanZoom));
        root = addRuleCommands(root)
            .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                .executes(context -> manager(context).startNextRound() ? success(context, "Round started") : failure(context, "No valid round is configured")))
            .then(Commands.literal("nextRound").requires(source -> source.hasPermission(2))
                .executes(context -> manager(context).startNextRound() ? success(context, "Advanced to the next round") : failure(context, "No valid round is configured")))
            .then(Commands.literal("getOpKit").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("setloadoutpool").requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider
                        .suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), LoadoutPool.values().stream().map(LoadoutPool::getShortName)), builder))
                    .executes(TeamsCommand::setLoadoutPool)))
            .then(Commands.literal("xpmultiplier").requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0F, 100F)).executes(TeamsCommand::setExperienceMultiplier)))
            .then(Commands.literal("xp").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(TeamsCommand::giveExperience))))
            .then(Commands.literal("resetrank").requires(source -> source.hasPermission(2)).then(Commands.argument("player", EntityArgument.player()).executes(TeamsCommand::resetRank)))
            .then(Commands.literal("giverewardbox").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("box", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(RewardBox.values().stream().map(RewardBox::getShortName), builder)).executes(TeamsCommand::giveRewardBox))))
            .then(adminCommands());

        com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        dispatcher.register(Commands.literal("flansteams").redirect(node));
        dispatcher.register(Commands.literal("team").redirect(node));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> adminCommands()
    {
        return Commands.literal("admin").requires(source -> source.hasPermission(2)).then(Commands.literal("enabled").then(Commands.argument("value", BoolArgumentType.bool()).executes(context ->
        {
            manager(context).setEnabled(BoolArgumentType.getBool(context, "value"));
            return success(context, "Teams " + (manager(context).isEnabled() ? "enabled" : "disabled"));
        }))).then(Commands.literal("voting").then(Commands.argument("value", BoolArgumentType.bool()).executes(context ->
        {
            boolean enabled = BoolArgumentType.getBool(context, "value");
            manager(context).setVoting(enabled);
            return success(context, "Round voting " + (enabled ? "enabled" : "disabled"));
        }))).then(Commands.literal("scoreDisplayTime").then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            manager(context).setScoreDisplayTimeSeconds(seconds);
            return success(context, "Round results will be displayed for " + seconds + " seconds");
        }))).then(Commands.literal("rankUpdateTime").then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            manager(context).setRankUpdateTimeSeconds(seconds);
            return success(context, seconds == 0 ? "The rank update screen is disabled" : "The rank update screen will appear for " + seconds + " seconds");
        }))).then(Commands.literal("votingTime").then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            manager(context).setVotingTimeSeconds(seconds);
            return success(context, "Round voting will last " + seconds + " seconds");
        }))).then(Commands.literal("motd").then(Commands.argument("text", StringArgumentType.greedyString()).executes(TeamsCommand::setMotd)))
            .then(Commands.literal("autobalancetime").then(Commands.argument("seconds", IntegerArgumentType.integer(11, 86400)).executes(context ->
            {
                int seconds = IntegerArgumentType.getInteger(context, "seconds");
                manager(context).setAutoBalanceIntervalSeconds(seconds);
                return success(context, "Autobalance will run every " + seconds + " seconds with a 10-second warning");
            }))).then(Commands.literal("roundsGenerator").then(Commands.argument("value", BoolArgumentType.bool()).executes(context ->
            {
                boolean enabled = BoolArgumentType.getBool(context, "value");
                manager(context).setRoundsGenerator(enabled);
                return success(context, "Rounds generator " + (enabled ? "enabled" : "disabled"));
            })))
            .then(Commands.literal("start").executes(context -> manager(context).startNextRound() ? success(context, "Round started") : failure(context, "No valid round is configured"))
                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                    .executes(context -> manager(context).startRound(IntegerArgumentType.getInteger(context, "index")) ? success(context, "Round started") : failure(context, "Invalid round index"))))
            .then(Commands.literal("next").executes(context -> manager(context).startNextRound() ? success(context, "Advanced to the next round") : failure(context, "No valid round is configured")))
            .then(Commands.literal("setnext").then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(TeamsCommand::queueRound)))
            .then(Commands.literal("ping").executes(TeamsCommand::listPings))
            .then(Commands.literal("bltss").executes(TeamsCommand::showBulletSnapshot)
                .then(Commands.argument("min", IntegerArgumentType.integer(0, 100)).then(Commands.argument("divisor", IntegerArgumentType.integer(0, 1000)).executes(TeamsCommand::setBulletSnapshot))))
            .then(Commands.literal("stop").executes(context ->
            {
                manager(context).stopRound();
                return success(context, "Round stopped");
            })).then(Commands.literal("arena").executes(context ->
            {
                manager(context).applyArenaPreset();
                return success(context, "Arena preset applied");
            })).then(Commands.literal("survival").executes(context ->
            {
                manager(context).applySurvivalPreset();
                return success(context, "Survival preset applied");
            })).then(Commands.literal("kit").executes(TeamsCommand::giveKit))
            .then(Commands.literal("loadoutpool")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider
                        .suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), LoadoutPool.values().stream().map(LoadoutPool::getShortName)), builder))
                    .executes(TeamsCommand::setLoadoutPool)))
            .then(Commands.literal("xpmultiplier").then(Commands.argument("value", FloatArgumentType.floatArg(0F, 100F)).executes(TeamsCommand::setExperienceMultiplier)))
            .then(Commands.literal("xp")
                .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(TeamsCommand::giveExperience))))
            .then(Commands.literal("resetrank").then(Commands.argument("player", EntityArgument.player()).executes(TeamsCommand::resetRank)))
            .then(Commands.literal("giverewardbox")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("box", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(RewardBox.values().stream().map(RewardBox::getShortName), builder)).executes(TeamsCommand::giveRewardBox))))
            .then(Commands.literal("map")
                .then(
                    Commands.literal("add").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("name", StringArgumentType.greedyString()).executes(TeamsCommand::addMap))))
                .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder)).executes(TeamsCommand::removeMap))))
            .then(
                Commands.literal("round")
                    .then(Commands.literal("add").then(Commands.argument("map", StringArgumentType.word()).suggests((context, builder) -> suggestMaps(builder)).then(Commands
                        .argument("gametype", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(GameType.values().stream().map(GameType::getId), builder))
                        .then(Commands.argument("teams", StringArgumentType.word()).then(
                            Commands.argument("minutes", IntegerArgumentType.integer(1, 1440)).then(Commands.argument("score", IntegerArgumentType.integer(1)).executes(TeamsCommand::addRound)))))))
                    .then(Commands.literal("remove").then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(TeamsCommand::removeRound))))
            .then(Commands.literal("setvariable")
                .then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument("value", StringArgumentType.word()).executes(TeamsCommand::setVariable))));
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
        context.getSource().sendSuccess(() -> Component.literal(
            "/teams loadouts, /teams join <team>, /teams class <class>, /teams vote <number>, /teams score, /teams motd, /teams stats, /teams list <gametypes|teams|classes|loadouts|rewardboxes|maps|rounds>"),
            false);
        if (context.getSource().hasPermission(2))
        {
            context.getSource().sendSuccess(() -> Component.literal(
                "Rules: /teams <explosions|forceAdventure|fuelNeeded|vehiclesCanZoom|overrideHunger|bombs|shells|bullets|canBreakGuns|canBreakGlass|survivalCanBreakVehicles|survivalCanPlaceVehicles|armourDrops|vehiclesBreakBlocks|useRotation|autobalance> <true|false>, /teams weaponDrops <on|off|smart>, /teams <mgLife|planeLife|vehicleLife|mechaLife|aaLife> <seconds>"),
                false);
            context.getSource()
                .sendSuccess(() -> Component.literal("Rounds and server: /teams setRound <index>, /teams goToMap <index>, /teams ping, /teams bltss [<min> <divisor>], /teams motd <text>"), false);
            context.getSource().sendSuccess(() -> Component.literal(
                "Administration: /teams admin <loadoutpool|xpmultiplier|xp|resetrank|giverewardbox|enabled|voting|scoreDisplayTime|rankUpdateTime|votingTime|autobalancetime|roundsGenerator|start|next|setnext|stop|arena|survival|kit|map|round|setvariable|ping|bltss>"),
                false);
        }
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> booleanSetting(String name, java.util.function.ToIntFunction<CommandContext<CommandSourceStack>> handler)
    {
        return Commands.literal(name).requires(source -> source.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(handler::applyAsInt));
    }

    /**
     * The individual Teams rules of 1.7.10, each under its legacy spelling, so an operator can tune one rule
     * without applying the whole arena or survival preset.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> addRuleCommands(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(rule("overrideHunger", TeamsManager::setOverrideHunger, enabled -> "Players will " + (enabled ? "no longer" : "now") + " get hungry during rounds"))
            .then(rule("noHunger", TeamsManager::setOverrideHunger, enabled -> "Players will " + (enabled ? "no longer" : "now") + " get hungry during rounds"))
            .then(rule("bombs", TeamsManager::setBombsEnabled, enabled -> "Bombs are now " + (enabled ? "enabled" : "disabled")))
            .then(rule("allowBombs", TeamsManager::setBombsEnabled, enabled -> "Bombs are now " + (enabled ? "enabled" : "disabled")))
            .then(rule("shells", TeamsManager::setShellsEnabled, enabled -> "Shells are now " + (enabled ? "enabled" : "disabled")))
            .then(rule("bullets", TeamsManager::setBulletsEnabled, enabled -> "Bullets are now " + (enabled ? "enabled" : "disabled")))
            .then(rule("bulletsEnabled", TeamsManager::setBulletsEnabled, enabled -> "Bullets are now " + (enabled ? "enabled" : "disabled")))
            .then(rule("canBreakGuns", TeamsManager::setCanBreakGuns, enabled -> "AAGuns and MGs can " + (enabled ? "now" : "no longer") + " be broken"))
            .then(rule("canBreakGlass", TeamsManager::setCanBreakGlass, enabled -> "Glass and glowstone can " + (enabled ? "now" : "no longer") + " be broken"))
            .then(rule("survivalCanBreakVehicles", TeamsManager::setSurvivalCanBreakVehicles, enabled -> "Survival players can " + (enabled ? "now" : "no longer") + " break vehicles"))
            .then(rule("survivalCanPlaceVehicles", TeamsManager::setSurvivalCanPlaceVehicles, enabled -> "Survival players can " + (enabled ? "now" : "no longer") + " place vehicles"))
            .then(rule("armourDrops", TeamsManager::setArmourDrops, enabled -> "Armour will " + (enabled ? "now" : "no longer") + " be dropped"))
            .then(rule("armorDrops", TeamsManager::setArmourDrops, enabled -> "Armour will " + (enabled ? "now" : "no longer") + " be dropped"))
            .then(rule("vehiclesBreakBlocks", TeamsManager::setDriveablesBreakBlocks, enabled -> "Vehicles will " + (enabled ? "now" : "no longer") + " break blocks"))
            .then(Commands.literal("weaponDrops").requires(source -> source.hasPermission(2))
                .then(Commands.argument("mode", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("on", "off", "smart"), builder))
                    .executes(TeamsCommand::setWeaponDrops)))
            .then(lifeSetting("mgLife", "MGs", TeamsManager::setMgLife)).then(lifeSetting("planeLife", "Planes", TeamsManager::setPlaneLife))
            .then(lifeSetting("vehicleLife", "Vehicles", TeamsManager::setVehicleLife)).then(lifeSetting("mechaLife", "Mechas", TeamsManager::setMechaLife))
            .then(lifeSetting("aaLife", "AA guns", TeamsManager::setAaLife))
            .then(Commands.literal("useRotation").requires(source -> source.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context ->
            {
                // A fixed rotation is the opposite of voting for the next round
                manager(context).setVoting(!BoolArgumentType.getBool(context, "value"));
                return success(context, "Voting is now " + (manager(context).isVoting() ? "enabled" : "disabled"));
            })))
            .then(Commands.literal("autobalance").requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> setCurrentVariable(context, "autobalance", String.valueOf(BoolArgumentType.getBool(context, "value"))))))
            .then(Commands.literal("setRound").requires(source -> source.hasPermission(2)).then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(TeamsCommand::queueRound)))
            .then(Commands.literal("goToMap").requires(source -> source.hasPermission(2))
                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                    .executes(context -> manager(context).startRound(IntegerArgumentType.getInteger(context, "index")) ? success(context, "Round started") : failure(context, "Invalid round index"))))
            .then(Commands.literal("getSticks").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("getOpSticks").requires(source -> source.hasPermission(2)).executes(TeamsCommand::giveKit))
            .then(Commands.literal("ping").requires(source -> source.hasPermission(2)).executes(TeamsCommand::listPings))
            .then(Commands.literal("bltss").requires(source -> source.hasPermission(2)).executes(TeamsCommand::showBulletSnapshot)
                .then(Commands.argument("min", IntegerArgumentType.integer(0, 100)).then(Commands.argument("divisor", IntegerArgumentType.integer(0, 1000)).executes(TeamsCommand::setBulletSnapshot))))
            .then(Commands.literal("showbltss").requires(source -> source.hasPermission(2)).executes(TeamsCommand::showBulletSnapshot));
        return root;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> rule(String name, java.util.function.BiConsumer<TeamsManager, Boolean> setter,
        java.util.function.Function<Boolean, String> message)
    {
        return booleanSetting(name, context ->
        {
            boolean enabled = BoolArgumentType.getBool(context, "value");
            setter.accept(manager(context), enabled);
            manager(context).saveSettings();
            return success(context, message.apply(enabled));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> lifeSetting(String name, String what, java.util.function.ObjIntConsumer<TeamsManager> setter)
    {
        return Commands.literal(name).requires(source -> source.hasPermission(2)).then(Commands.argument("seconds", IntegerArgumentType.integer(0)).executes(context ->
        {
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            setter.accept(manager(context), seconds);
            manager(context).saveSettings();
            return success(context, seconds > 0 ? what + " will despawn after " + seconds + " seconds" : what + " will not despawn");
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
        manager(context).setWeaponDrops(drops);
        manager(context).saveSettings();
        return success(context, message);
    }

    private static int queueRound(CommandContext<CommandSourceStack> context)
    {
        int index = IntegerArgumentType.getInteger(context, "index");
        if (!manager(context).queueNextRound(index))
            return failure(context, "Invalid round index");
        TeamsRound round = manager(context).getRounds().get(index);
        manager(context).broadcast(Component.literal("Next round will be " + round.getGameTypeId() + " in " + round.getMapId()));
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
        int divisor = IntegerArgumentType.getInteger(context, "divisor");
        com.flansmodultimate.config.ModCommonConfig.setBulletSnapshot(min, divisor);
        return showBulletSnapshot(context);
    }

    private static int showBulletSnapshot(CommandContext<CommandSourceStack> context)
    {
        int min = manager(context).getBulletSnapshotMin();
        int divisor = manager(context).getBulletSnapshotDivisor();
        context.getSource().sendSuccess(() -> Component.literal("[BulletDelay] Min=" + min + " : Divisor=" + divisor + " (bullets use player snapshot Min + Ping / Divisor)"), false);
        return 1;
    }

    private static int setExplosions(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, "value");
        manager(context).setExplosionsBreakBlocks(enabled);
        return success(context, "Terrain damage from explosions " + (enabled ? "enabled" : "disabled"));
    }

    private static int setForceAdventure(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, "value");
        manager(context).setForceAdventureMode(enabled);
        return success(context, "Adventure mode will " + (enabled ? "now" : "no longer") + " be forced on respawn");
    }

    private static int setFuelNeeded(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, "value");
        manager(context).setVehiclesNeedFuel(enabled);
        return success(context, "Vehicles will " + (enabled ? "now" : "no longer") + " require fuel");
    }

    private static int setVehiclesCanZoom(CommandContext<CommandSourceStack> context)
    {
        boolean enabled = BoolArgumentType.getBool(context, "value");
        manager(context).setVehiclesCanZoom(enabled);
        return success(context, "Driver-controlled vehicle zoom " + (enabled ? "enabled" : "disabled"));
    }

    private static int openLoadouts(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        if (manager(context).getCurrentLoadoutPool().isEmpty())
            return failure(context, "No ranked loadout pool is active");
        manager(context).syncLoadouts(context.getSource().getPlayerOrException(), com.flansmodultimate.network.client.teams.PacketLoadoutState.OpenScreen.HUB, 0, "");
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
        return manager(context).setCurrentLoadoutPool(id) ? success(context, "Loadout pool set to " + id) : failure(context, "Unknown loadout pool");
    }

    private static int setExperienceMultiplier(CommandContext<CommandSourceStack> context)
    {
        float value = FloatArgumentType.getFloat(context, "value");
        manager(context).setExperienceMultiplier(value);
        return success(context, "Teams XP multiplier set to " + value);
    }

    private static int giveExperience(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        manager(context).awardExperience(player, amount);
        return success(context, "Granted " + amount + " XP to " + player.getScoreboardName());
    }

    private static int resetRank(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        manager(context).getStats(player).resetRankProgress();
        manager(context).markPlayerDataDirty();
        return success(context, "Reset ranked progress for " + player.getScoreboardName());
    }

    private static int giveRewardBox(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String box = StringArgumentType.getString(context, "box");
        return manager(context).grantRewardBox(player, box, RewardBoxInstance.Origin.COMMAND)
            ? success(context, "Granted " + box + " to " + player.getScoreboardName())
            : failure(context, "Unknown reward box");
    }

    private static int selectClass(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerClass playerClass = PlayerClass.getPlayerClass(StringArgumentType.getString(context, "class"));
        if (!manager(context).selectClass(player, playerClass))
            return failure(context, "That class is unavailable for your selected team or rank");
        manager(context).confirmSelection(player);
        return success(context, "Class selected: " + playerClass.getName());
    }

    private static int showMotd(CommandContext<CommandSourceStack> context)
    {
        return success(context, manager(context).getMotd());
    }

    private static int setMotd(CommandContext<CommandSourceStack> context)
    {
        String text = StringArgumentType.getString(context, "text");
        manager(context).setMotd(text);
        return success(context, "Message of the day: " + manager(context).getMotd());
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
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + ": rank " + stats.getRank() + ", " + stats.getTotalExperience() + " XP, " + stats.getKills() + " kills, "
            + stats.getDeaths() + " deaths, " + stats.getCapturedFlags() + " captures"), false);
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
        manager(context).getMaps()
            .forEach(map -> context.getSource().sendSuccess(() -> Component.literal(map.getShortName() + " — " + map.getName() + " [" + map.getDimension().location() + "]"), false));
        return manager(context).getMaps().size();
    }

    private static int listRounds(CommandContext<CommandSourceStack> context)
    {
        List<TeamsRound> rounds = manager(context).getRounds();
        for (int i = 0; i < rounds.size(); i++)
        {
            int index = i;
            TeamsRound round = rounds.get(i);
            context.getSource().sendSuccess(() -> Component.literal(
                index + ": " + round.getGameTypeId() + " @ " + round.getMapId() + " [" + String.join(", ", round.getTeamIds()) + "] " + round.getTimeLimitMinutes() + "m / " + round.getScoreLimit()),
                false);
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
        catch (IllegalArgumentException e)
        {
            return failure(context, e.getMessage());
        }
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
        catch (IllegalArgumentException e)
        {
            return failure(context, e.getMessage());
        }
    }

    private static int removeRound(CommandContext<CommandSourceStack> context)
    {
        return manager(context).removeRound(IntegerArgumentType.getInteger(context, "index")) ? success(context, "Round removed") : failure(context, "Invalid round index");
    }

    private static int setVariable(CommandContext<CommandSourceStack> context)
    {
        return setCurrentVariable(context, StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "value"));
    }

    /**
     * Sets a variable of the game type being played. {@code scorelimit} applies to every game type, as in
     * 1.7.10, and changes the limit of the round in progress.
     */
    private static int setCurrentVariable(CommandContext<CommandSourceStack> context, String name, String value)
    {
        GameType type = manager(context).getCurrentGameType().orElse(null);
        if (type == null)
            return failure(context, "There is no game type to set variables for");
        try
        {
            if ("scorelimit".equalsIgnoreCase(name))
            {
                if (!manager(context).setCurrentScoreLimit(Integer.parseInt(value)))
                    return failure(context, "The score limit must be at least 1");
            }
            else if (!type.setVariable(name, value))
                return failure(context, "Unknown variable for the current game type");
        }
        catch (NumberFormatException e)
        {
            return failure(context, "Invalid value: " + value);
        }
        manager(context).saveSettings();
        return success(context, "Set variable " + name + " in game type " + type.getId() + " to " + value);
    }

    private static int giveKit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (ItemStack stack : List.of(new ItemStack(FlansModItems.opStick.get()), new ItemStack(FlansModItems.flagpoleItem.get(), 16), new ItemStack(FlansModItems.playerSpawnerItem.get(), 16),
            new ItemStack(FlansModItems.itemSpawnerItem.get(), 16), new ItemStack(FlansModItems.vehicleSpawnerItem.get(), 16)))
            if (!player.getInventory().add(stack))
                player.drop(stack, false);
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
