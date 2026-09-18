package com.flansmodultimate.common.command;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.TeamsManager;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.flansmodultimate.common.command.TryCommandSupport.*;

/**
 * Equips a Teams player class outside of a round, so that a class can be tried without setting
 * up a map, a game type and two teams first.
 *
 * <p>It does what the respawn loadout does for a spawning player, minus the parts only a round
 * can supply: the carried items are emptied, the class armour is worn, the starting items are
 * handed over and the class is recorded on the player so its SkinOverride is broadcast. No team
 * is joined, so ranked loadout pools are deliberately left out of it.</p>
 *
 * <p>The armour slots are treated the way a real spawn treats them: a class only writes the
 * pieces it actually defines, and whatever is worn in the other slots stays. That is what makes
 * {@code /tryteam} useful next to this command, since team armour set up there survives trying
 * one class after another. {@code clear} empties the carried items and drops the skin override,
 * again leaving the armour alone.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TryClassCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        // Registered twice rather than redirected, so the prefixed spelling accepts the same
        // arguments; a redirect only forwards the arguments that follow it.
        dispatcher.register(commandRoot("tryclass"));
        dispatcher.register(commandRoot("flantryclass"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandRoot(String name)
    {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .executes(TryClassCommand::usage)
            .then(Commands.literal("clear").executes(TryClassCommand::clearClass))
            .then(Commands.literal("list").executes(context -> list(context, null))
                .then(Commands.argument("filter", StringArgumentType.greedyString())
                    .suggests(TryClassCommand::suggestFilters)
                    .executes(context -> list(context, StringArgumentType.getString(context, "filter")))))
            // Reached only when the first word is neither "clear" nor "list", so a class named
            // after one of those has to be found through the team or content pack listing.
            .then(Commands.argument("class", StringArgumentType.word())
                .suggests(TryClassCommand::suggestClasses)
                .executes(TryClassCommand::equipClass));
    }

    private static int usage(CommandContext<CommandSourceStack> context)
    {
        send(context, ChatFormatting.GRAY,
            "/tryclass <class>, /tryclass clear, /tryclass list [<team>|<content pack>]");
        return 1;
    }

    private static int equipClass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(context, "class");
        PlayerClass playerClass = PlayerClass.getPlayerClass(id);
        if (playerClass == null)
        {
            context.getSource().sendFailure(Component.literal("Unknown player class: " + id));
            return 0;
        }

        clearCarriedItems(player);

        // A slot the class leaves undefined keeps what is worn, exactly as a real spawn lets team
        // armour fill the gaps the class does not cover.
        int worn = 0;
        for (EquipmentSlot slot : ARMOUR_SLOTS)
        {
            ItemStack armour = playerClass.getArmour(slot);
            if (armour.isEmpty())
                continue;
            player.setItemSlot(slot, armour.copy());
            worn++;
        }

        int given = 0;
        for (ItemStack stack : playerClass.createStartingItems())
        {
            give(player, stack);
            given++;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        applyClass(player, playerClass);

        int items = given;
        int pieces = worn;
        context.getSource().sendSuccess(() -> Component.literal("Equipped class " + playerClass.getName()
            + " (" + items + (items == 1 ? " item" : " items")
            + ", " + pieces + (pieces == 1 ? " armour piece" : " armour pieces") + ")")
            .withStyle(ChatFormatting.GREEN), true);
        if (playerClass.getSkinOverride().isBlank())
            send(context, ChatFormatting.DARK_GRAY, "This class defines no SkinOverride");
        return 1;
    }

    private static int clearClass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        clearCarriedItems(player);
        applyClass(player, null);
        send(context, ChatFormatting.GREEN,
            "Class cleared and carried items emptied; your armour was left on. Use /tryteam clear to take it off");
        return 1;
    }

    /**
     * Records the class on the player and pushes the assignment out at once. Both the current and
     * the pending class are set, so a respawn does not silently undo the choice.
     */
    private static void applyClass(ServerPlayer player, @Nullable PlayerClass playerClass)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.setPlayerClass(playerClass);
        data.setNewPlayerClass(playerClass);
        TeamsManager.getInstance().syncPlayerClassSkins(true);
    }

    private static int list(CommandContext<CommandSourceStack> context, @Nullable String filter)
    {
        Collection<PlayerClass> classes;
        String header;
        if (filter == null)
        {
            classes = PlayerClass.values();
            header = "All player classes";
        }
        else
        {
            Team team = Team.getTeam(filter.trim());
            if (team != null)
            {
                classes = team.getClasses();
                header = "Classes of team " + team.getName();
            }
            else
            {
                String pack = matchingContentPack(filter, PlayerClass.values());
                if (pack == null)
                {
                    context.getSource().sendFailure(Component.literal(
                        "No team or content pack named " + filter.trim()));
                    return 0;
                }
                classes = PlayerClass.values().stream().filter(type -> pack.equals(packName(type))).toList();
                header = "Classes of content pack " + pack;
            }
        }

        if (classes.isEmpty())
        {
            send(context, ChatFormatting.GRAY, header + ": none");
            return 0;
        }

        send(context, ChatFormatting.GOLD, "=== " + header + " (" + classes.size() + ") ===");
        for (PlayerClass playerClass : classes)
            context.getSource().sendSuccess(() -> classLine(playerClass), false);
        return classes.size();
    }

    private static Component classLine(PlayerClass playerClass)
    {
        MutableComponent line = idPrefix(playerClass)
            .append(Component.literal(" - ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(playerClass.getName()).withStyle(ChatFormatting.WHITE));
        if (playerClass.getUnlockLevel() > 0)
            line.append(Component.literal(" (rank " + playerClass.getUnlockLevel() + ")")
                .withStyle(ChatFormatting.YELLOW));
        return line.append(packSuffix(playerClass));
    }

    private static CompletableFuture<Suggestions> suggestClasses(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(
            PlayerClass.values().stream().map(PlayerClass::getShortName), builder);
    }

    private static CompletableFuture<Suggestions> suggestFilters(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        Set<String> filters = new LinkedHashSet<>();
        Team.values().forEach(team -> filters.add(team.getShortName()));
        filters.addAll(contentPacks(PlayerClass.values()));
        return SharedSuggestionProvider.suggest(filters, builder);
    }
}
