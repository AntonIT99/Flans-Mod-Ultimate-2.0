package com.flansmodultimate.common.command;

import com.flansmodultimate.IContentProvider;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Equips a Teams player class outside of a round, so that a class can be tried without setting
 * up a map, a game type and two teams first.
 *
 * <p>It does what the respawn loadout does for a spawning player, minus the parts only a round
 * can supply: the inventory is emptied, the class armour is worn, the starting items are handed
 * over and the class is recorded on the player so its SkinOverride is broadcast. No team is
 * joined, so team armour and ranked loadout pools are deliberately left out of it.</p>
 *
 * <p>{@code clear} only forgets the class, which drops the skin override again; whatever the
 * player is carrying stays theirs.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TryClassCommand
{
    private static final List<EquipmentSlot> ARMOUR_SLOTS =
        List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

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

        player.getInventory().clearContent();
        for (EquipmentSlot slot : ARMOUR_SLOTS)
            player.setItemSlot(slot, playerClass.getArmour(slot).copy());

        int given = 0;
        for (ItemStack stack : playerClass.createStartingItems())
        {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy))
                player.drop(copy, false);
            given++;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        applyClass(player, playerClass);

        int items = given;
        context.getSource().sendSuccess(() -> Component.literal("Equipped class " + playerClass.getName()
            + " (" + items + (items == 1 ? " item" : " items") + ")").withStyle(ChatFormatting.GREEN), true);
        if (playerClass.getSkinOverride().isBlank())
            send(context, ChatFormatting.DARK_GRAY, "This class defines no SkinOverride");
        return 1;
    }

    private static int clearClass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        applyClass(context.getSource().getPlayerOrException(), null);
        send(context, ChatFormatting.GREEN, "Player class skin cleared; your inventory was left untouched");
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
                String pack = matchingContentPack(filter);
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
        MutableComponent line = Component.literal(playerClass.getOriginalShortName())
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(" - ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(playerClass.getName()).withStyle(ChatFormatting.WHITE));
        if (playerClass.getUnlockLevel() > 0)
            line.append(Component.literal(" (rank " + playerClass.getUnlockLevel() + ")")
                .withStyle(ChatFormatting.YELLOW));
        String pack = packName(playerClass);
        if (pack != null)
            line.append(Component.literal(" [" + pack + "]").withStyle(ChatFormatting.LIGHT_PURPLE));
        return line;
    }

    /** Content pack names may contain spaces, so they are matched loosely rather than by equality. */
    @Nullable
    private static String matchingContentPack(String filter)
    {
        String wanted = normalize(filter);
        return contentPacks().stream().filter(pack -> normalize(pack).equals(wanted)).findFirst().orElse(null);
    }

    private static Set<String> contentPacks()
    {
        Set<String> packs = new LinkedHashSet<>();
        PlayerClass.values().stream().map(TryClassCommand::packName).filter(Objects::nonNull).forEach(packs::add);
        return packs;
    }

    @Nullable
    private static String packName(PlayerClass playerClass)
    {
        IContentProvider pack = playerClass.getContentPack();
        return pack == null ? null : pack.getName();
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    private static CompletableFuture<Suggestions> suggestClasses(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(
            PlayerClass.values().stream().map(PlayerClass::getOriginalShortName), builder);
    }

    private static CompletableFuture<Suggestions> suggestFilters(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        Set<String> filters = new LinkedHashSet<>();
        Team.values().forEach(team -> filters.add(team.getOriginalShortName()));
        filters.addAll(contentPacks());
        return SharedSuggestionProvider.suggest(filters, builder);
    }

    private static void send(CommandContext<CommandSourceStack> context, ChatFormatting colour, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message).withStyle(colour), false);
    }
}
