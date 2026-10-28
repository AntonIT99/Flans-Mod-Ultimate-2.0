package com.wolffsmod.npcs.teams;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /flansnpcfriendlyfire}: shows and sets whether NPCs hurt friends, for the world and for single factions. Without a
 * setting, a faction follows the world and the world follows the common configuration; Teams rounds follow their game
 * type's friendlyfire variable instead.
 *
 * <ul>
 * <li>{@code /flansnpcfriendlyfire} shows the world's settings.</li>
 * <li>{@code /flansnpcfriendlyfire <npcs|players> <true|false|default>} sets one for the world.</li>
 * <li>{@code /flansnpcfriendlyfire faction <faction>} shows a faction's settings.</li>
 * <li>{@code /flansnpcfriendlyfire faction <faction> <npcs|players> <true|false|default>} sets one for the faction.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FriendlyFireCommands
{
    private static final String FACTION = "faction";
    private static final String KEY = "wolffsmodnpcs.friendly_fire.";
    private static final SimpleCommandExceptionType NO_FACTION = new SimpleCommandExceptionType(Component.translatable(KEY + "unknown"));
    private static final SimpleCommandExceptionType ROUND_FACTION = new SimpleCommandExceptionType(Component.translatable(KEY + "round_faction"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("flansnpcfriendlyfire").requires(source -> source.hasPermission(2)).executes(FriendlyFireCommands::showWorld);
        RequiredArgumentBuilder<CommandSourceStack, String> faction = Commands.argument(FACTION, StringArgumentType.string())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                FactionController.instance.factions.values().stream().filter(found -> !BotFactions.isRoundFaction(found.name)).map(found -> StringArgumentType.escapeIfRequired(found.name)), builder))
            .executes(FriendlyFireCommands::showFaction);
        for (FriendlyFire.Kind kind : FriendlyFire.Kind.values())
        {
            LiteralArgumentBuilder<CommandSourceStack> worldKind = Commands.literal(kind.getWord());
            LiteralArgumentBuilder<CommandSourceStack> factionKind = Commands.literal(kind.getWord());
            for (FriendlyFire.Setting setting : FriendlyFire.Setting.values())
            {
                worldKind.then(Commands.literal(setting.getWord()).executes(context -> setWorld(context, kind, setting)));
                factionKind.then(Commands.literal(setting.getWord()).executes(context -> setFaction(context, kind, setting)));
            }
            root.then(worldKind);
            faction.then(factionKind);
        }
        root.then(Commands.literal(FACTION).then(faction));
        dispatcher.register(root);
    }

    // --- World ---

    private static int showWorld(CommandContext<CommandSourceStack> context)
    {
        MinecraftServer server = context.getSource().getServer();
        for (FriendlyFire.Kind kind : FriendlyFire.Kind.values())
            context.getSource().sendSuccess(() -> text("world_status", kindName(kind), state(FriendlyFire.worldEnabled(kind, server)), worldSource(kind, server)), false);
        return FriendlyFire.Kind.values().length;
    }

    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int setWorld(CommandContext<CommandSourceStack> context, FriendlyFire.Kind kind, FriendlyFire.Setting setting)
    {
        MinecraftServer server = context.getSource().getServer();
        FriendlyFireSettings.get(server).set(kind, setting);
        boolean enabled = FriendlyFire.worldEnabled(kind, server);
        Component message = setting == FriendlyFire.Setting.DEFAULT ? text("world_reset", kindName(kind), state(enabled)) : text("world_set", kindName(kind), state(enabled));
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    // --- Factions ---

    private static int showFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Faction faction = faction(context);
        MinecraftServer server = context.getSource().getServer();
        for (FriendlyFire.Kind kind : FriendlyFire.Kind.values())
        {
            boolean own = FactionFriendlyFire.of(faction).wolffsmodnpcsFriendlyFire(kind) != FriendlyFire.Setting.DEFAULT;
            Component source = own ? text("source.faction") : worldSource(kind, server);
            context.getSource().sendSuccess(() -> text("faction_status", faction.name, kindName(kind), state(FriendlyFire.enabled(kind, faction, server)), source), false);
        }
        return FriendlyFire.Kind.values().length;
    }

    // A command reports how many settings it changed, which is always one here.
    @SuppressWarnings("SameReturnValue")
    private static int setFaction(CommandContext<CommandSourceStack> context, FriendlyFire.Kind kind, FriendlyFire.Setting setting) throws CommandSyntaxException
    {
        Faction faction = faction(context);
        FactionFriendlyFire.of(faction).wolffsmodnpcsSetFriendlyFire(kind, setting);
        FactionController.instance.saveFaction(faction);
        boolean enabled = FriendlyFire.enabled(kind, faction, context.getSource().getServer());
        Component message = setting == FriendlyFire.Setting.DEFAULT
            ? text("faction_reset", faction.name, kindName(kind), state(enabled))
            : text("faction_set", faction.name, kindName(kind), state(enabled));
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    private static Faction faction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Faction faction = FactionController.instance.getFactionFromName(StringArgumentType.getString(context, FACTION));
        if (faction == null)
            throw NO_FACTION.create();
        if (BotFactions.isRoundFaction(faction.name))
            throw ROUND_FACTION.create();
        return faction;
    }

    // --- Text ---

    private static Component worldSource(FriendlyFire.Kind kind, MinecraftServer server)
    {
        return text(FriendlyFireSettings.get(server).get(kind) == FriendlyFire.Setting.DEFAULT ? "source.config" : "source.world");
    }

    private static Component kindName(FriendlyFire.Kind kind)
    {
        return text("kind." + kind.getWord());
    }

    private static Component state(boolean enabled)
    {
        return text(enabled ? "on" : "off");
    }

    private static Component text(String key, Object... args)
    {
        return Component.translatable(KEY + key, args);
    }
}
