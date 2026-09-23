package com.flansmodultimate.common.command;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.brigadier.context.CommandContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Pieces {@link TryClassCommand} and {@link TryTeamCommand} share. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TryCommandSupport
{
    /** In the order they are worn, so listings and messages read top to bottom. */
    static final List<EquipmentSlot> ARMOUR_SLOTS =
        List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /**
     * Empties the carried inventory and the off hand but leaves the armour slots alone, so that
     * armour set up with one of these commands survives trying the next class.
     */
    static void clearCarriedItems(ServerPlayer player)
    {
        Inventory inventory = player.getInventory();
        inventory.items.replaceAll(ignored -> ItemStack.EMPTY);
        inventory.offhand.replaceAll(ignored -> ItemStack.EMPTY);
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    static void clearArmour(ServerPlayer player)
    {
        for (EquipmentSlot slot : ARMOUR_SLOTS)
            player.setItemSlot(slot, ItemStack.EMPTY);
        player.containerMenu.broadcastChanges();
    }

    /** Adds a stack to the inventory, dropping it at the player's feet when there is no room. */
    static void give(ServerPlayer player, ItemStack stack)
    {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy))
            player.drop(copy, false);
    }

    /**
     * The unique name to type, followed by the pack's own spelling when another pack claimed that
     * shortname first and this definition had to be given an alias.
     */
    static MutableComponent idPrefix(InfoType type)
    {
        MutableComponent line = Component.literal(type.getShortName()).withStyle(ChatFormatting.GREEN);
        if (!type.getShortName().equalsIgnoreCase(type.getOriginalShortName()))
            line.append(Component.literal(" (= " + type.getOriginalShortName() + ")")
                .withStyle(ChatFormatting.DARK_GRAY));
        return line;
    }

    static MutableComponent packSuffix(InfoType type)
    {
        String pack = packName(type);
        return pack == null ? Component.empty()
            : Component.literal(" [" + pack + "]").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /** Content pack names may contain spaces, so they are matched loosely rather than by equality. */
    @Nullable
    static String matchingContentPack(String filter, Collection<? extends InfoType> candidates)
    {
        String wanted = normalize(filter);
        return contentPacks(candidates).stream().filter(pack -> normalize(pack).equals(wanted))
            .findFirst().orElse(null);
    }

    static Set<String> contentPacks(Collection<? extends InfoType> candidates)
    {
        Set<String> packs = new LinkedHashSet<>();
        candidates.stream().map(TryCommandSupport::packName).filter(Objects::nonNull).forEach(packs::add);
        return packs;
    }

    @Nullable
    static String packName(InfoType type)
    {
        IContentProvider pack = type.getContentPack();
        return pack == null ? null : pack.getName();
    }

    static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    static void send(CommandContext<CommandSourceStack> context, ChatFormatting colour, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message).withStyle(colour), false);
    }
}
