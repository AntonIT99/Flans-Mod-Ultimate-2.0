package com.wolffsmod.npcs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.wolffsmod.npcs.teams.NpcCreationFactions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Sets or clears the executing builder's faction override for newly created NPCs. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcFactionCommands
{
    private static final String FACTION = "faction";
    private static final String KEY = "wolffsmodnpcs.creation_faction.";
    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Component.translatable(KEY + "unknown"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansnpcfaction").requires(source -> source.hasPermission(2)).then(Commands.literal("set").then(Commands.argument(FACTION, StringArgumentType.string())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionController.instance.factions.values().stream().map(faction -> StringArgumentType.escapeIfRequired(faction.name)),
                builder))
            .executes(context -> set(context.getSource(), StringArgumentType.getString(context, FACTION))))).then(Commands.literal("reset").executes(context -> reset(context.getSource()))));
    }

    // Brigadier reports one changed player setting.
    @SuppressWarnings("SameReturnValue")
    private static int set(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        Faction faction = FactionController.instance.getFactionFromName(name);
        if (faction == null)
            throw UNKNOWN.create();
        NpcCreationFactions.get(source.getServer()).set(player.getUUID(), faction.id);
        source.sendSuccess(() -> Component.translatable(KEY + "set", faction.name), false);
        return 1;
    }

    // Brigadier reports one changed player setting, including an already cleared override.
    @SuppressWarnings("SameReturnValue")
    private static int reset(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        NpcCreationFactions.get(source.getServer()).reset(player.getUUID());
        source.sendSuccess(() -> Component.translatable(KEY + "reset"), false);
        return 1;
    }
}
