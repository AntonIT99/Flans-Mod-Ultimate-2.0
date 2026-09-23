package com.flansmodultimate.common.command;

import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.flansmodultimate.common.command.TryCommandSupport.*;

/**
 * Wears the armour a team issues, outside of a round.
 *
 * <p>The companion of {@code /tryclass}: a team contributes the armour a player spawns in, so
 * this puts on exactly the pieces the team defines and nothing else. No team is joined, no round
 * state is touched and the carried inventory is left alone, so the armour can be put on before or
 * after a class without either command undoing the other.</p>
 *
 * <p>A class that defines its own piece for a slot overrides the team piece when it is equipped,
 * which is the order a real spawn applies them in.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TryTeamCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(commandRoot("tryteam"));
        dispatcher.register(commandRoot("flantryteam"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandRoot(String name)
    {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .executes(TryTeamCommand::usage)
            .then(Commands.literal("clear").executes(TryTeamCommand::clearTeamArmour))
            .then(Commands.literal("list").executes(context -> list(context, null))
                .then(Commands.argument("filter", StringArgumentType.greedyString())
                    .suggests(TryTeamCommand::suggestFilters)
                    .executes(context -> list(context, StringArgumentType.getString(context, "filter")))))
            // Reached only when the first word is neither "clear" nor "list", so a team named
            // after one of those has to be found through the content pack listing.
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests(TryTeamCommand::suggestTeams)
                .executes(TryTeamCommand::equipTeam));
    }

    private static int usage(CommandContext<CommandSourceStack> context)
    {
        send(context, ChatFormatting.GRAY,
            "/tryteam <team>, /tryteam clear, /tryteam list [<content pack>]");
        return 1;
    }

    private static int equipTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(context, "team");
        Team team = Team.getTeam(id);
        if (team == null)
        {
            context.getSource().sendFailure(Component.literal("Unknown team: " + id));
            return 0;
        }

        // Every slot is written, including the ones this team leaves empty, so switching from one
        // team to another never leaves a piece of the previous uniform behind.
        int worn = 0;
        for (EquipmentSlot slot : ARMOUR_SLOTS)
        {
            ItemStack armour = team.getArmour(slot);
            player.setItemSlot(slot, armour.copy());
            if (!armour.isEmpty())
                worn++;
        }
        player.containerMenu.broadcastChanges();

        if (worn == 0)
        {
            send(context, ChatFormatting.GRAY, team.getName() + " issues no armour; your armour slots are now empty");
            return 0;
        }

        int pieces = worn;
        context.getSource().sendSuccess(() -> Component.literal("Wearing the armour of ")
            .withStyle(ChatFormatting.GREEN)
            .append(team.getDisplayComponent())
            .append(Component.literal(" (" + pieces + (pieces == 1 ? " piece" : " pieces") + ")")
                .withStyle(ChatFormatting.GREEN)), true);

        List<PlayerClass> classes = team.getClasses();
        if (!classes.isEmpty())
            send(context, ChatFormatting.DARK_GRAY, "Use /tryclass list " + team.getShortName()
                + " for the " + classes.size() + " class(es) of this team");
        return worn;
    }

    private static int clearTeamArmour(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        clearArmour(context.getSource().getPlayerOrException());
        send(context, ChatFormatting.GREEN, "Armour slots emptied; your carried items were left untouched");
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context, @Nullable String filter)
    {
        Collection<Team> teams;
        String header;
        if (filter == null)
        {
            teams = Team.values();
            header = "All teams";
        }
        else
        {
            String pack = matchingContentPack(filter, Team.values());
            if (pack == null)
            {
                context.getSource().sendFailure(Component.literal(
                    "No content pack named " + filter.trim()));
                return 0;
            }
            teams = Team.values().stream().filter(type -> pack.equals(packName(type))).toList();
            header = "Teams of content pack " + pack;
        }

        if (teams.isEmpty())
        {
            send(context, ChatFormatting.GRAY, header + ": none");
            return 0;
        }

        send(context, ChatFormatting.GOLD, "=== " + header + " (" + teams.size() + ") ===");
        for (Team team : teams)
            context.getSource().sendSuccess(() -> teamLine(team), false);
        return teams.size();
    }

    private static Component teamLine(Team team)
    {
        int classes = team.getClasses().size();
        int pieces = (int)ARMOUR_SLOTS.stream().filter(slot -> !team.getArmour(slot).isEmpty()).count();
        MutableComponent line = idPrefix(team)
            .append(Component.literal(" - ").withStyle(ChatFormatting.GOLD))
            .append(team.getDisplayComponent())
            .append(Component.literal(" (" + classes + " class(es), " + pieces + " armour piece(s))")
                .withStyle(ChatFormatting.YELLOW));
        return line.append(packSuffix(team));
    }

    private static CompletableFuture<Suggestions> suggestTeams(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(Team.values().stream().map(Team::getShortName), builder);
    }

    private static CompletableFuture<Suggestions> suggestFilters(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(contentPacks(Team.values()), builder);
    }
}
