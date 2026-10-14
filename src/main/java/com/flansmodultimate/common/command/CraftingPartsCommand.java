package com.flansmodultimate.common.command;

import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.util.ModUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.*;

/** Gives the direct ingredients for one recipe operation of a held content-pack item. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CraftingPartsCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("craftingparts").requires(source -> source.hasPermission(2)).executes(context -> give(context.getSource())));
    }

    private static int give(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof IFlanItem<?>))
            held = player.getOffhandItem();
        if (!(held.getItem() instanceof IFlanItem<?> item))
        {
            source.sendFailure(Component.literal("Hold a Flan's Mod item in your main or off hand"));
            return 0;
        }
        List<ItemStack> ingredients = findIngredients(player, item.getConfigType(), held);
        if (ingredients.isEmpty())
        {
            source.sendFailure(Component.literal("No usable crafting recipe found for " + held.getHoverName().getString()));
            return 0;
        }
        int total = 0;
        for (ItemStack ingredient : ingredients)
        {
            total += ingredient.getCount();
            int remaining = ingredient.getCount();
            while (remaining > 0)
            {
                ItemStack stack = ingredient.copyWithCount(Math.min(remaining, ingredient.getMaxStackSize()));
                remaining -= stack.getCount();
                if (!player.addItem(stack) && !stack.isEmpty())
                    player.drop(stack, false);
            }
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        String name = held.getHoverName().getString();
        int given = total;
        source.sendSuccess(() -> Component.literal("Gave " + given + " crafting ingredients for " + name), true);
        return total;
    }

    private static List<ItemStack> findIngredients(ServerPlayer player, InfoType type, ItemStack held)
    {
        if (type instanceof DriveableType driveable)
        {
            List<ItemStack> ingredients = driveableIngredients(driveable);
            if (!ingredients.isEmpty() || driveable.numEngines() > 0)
                return ingredients;
        }
        if (type instanceof PartType part && !part.getPartBoxRecipe().isEmpty())
            return part.getPartBoxRecipe();
        // Prefer the item's own pack; short names make alternative box recipes stable across loads.
        List<InfoType> boxes = InfoType.getInfoTypes().values().stream().distinct().filter(box -> box instanceof GunBoxType || box instanceof ArmorBoxType)
            .sorted(Comparator.comparing((InfoType box) -> box.getContentPack() != type.getContentPack()).thenComparing(InfoType::getShortName)).toList();
        for (InfoType box : boxes)
        {
            List<ItemStack> ingredients = boxIngredients(box, type);
            if (!ingredients.isEmpty())
                return ingredients;
        }
        return player.serverLevel().getRecipeManager().getRecipes().stream().filter(holder -> holder.value() instanceof CraftingRecipe || holder.value() instanceof AbstractCookingRecipe)
            .filter(holder -> holder.value().getResultItem(player.level().registryAccess()).is(held.getItem()))
            .sorted(Comparator.comparing((RecipeHolder<?> holder) -> !(holder.value() instanceof CraftingRecipe)).thenComparing(holder -> holder.id().toString()))
            .map(holder -> resolveIngredients(holder.value().getIngredients())).filter(ingredients -> !ingredients.isEmpty()).findFirst().orElse(List.of());
    }

    private static List<ItemStack> boxIngredients(InfoType box, InfoType type)
    {
        if (box instanceof GunBoxType gunBox)
        {
            GunBoxType.GunBoxEntry entry = gunBox.findEntry(type);
            if (entry != null)
                return entry.getRequiredParts();
        }
        if (box instanceof ArmorBoxType armorBox)
            return armorIngredients(armorBox, type);
        return List.of();
    }

    private static List<ItemStack> armorIngredients(ArmorBoxType box, InfoType type)
    {
        for (ArmorBoxType.ArmourBoxEntry entry : box.getPages())
            for (int slot = 0; slot < 4; slot++)
                if (entry.getArmorType(slot) == type)
                    return entry.getRequiredStacks().get(slot);
        return List.of();
    }

    private static List<ItemStack> driveableIngredients(DriveableType type)
    {
        List<ItemStack> ingredients = new ArrayList<>(type.getDriveableRecipe());
        if (type.numEngines() <= 0)
            return ingredients;
        PartType engine = PartType.getDefaultEngine(type.getType(), type.getContentPack(), type.getEngine());
        if (engine == null || !engine.worksWith(type.getType()))
            return List.of();
        ItemStack engineStack = ModUtils.getItemStack(engine, type.numEngines()).orElse(ItemStack.EMPTY);
        if (engineStack.isEmpty())
            return List.of();
        ingredients.add(engineStack);
        return ingredients;
    }

    static List<ItemStack> resolveIngredients(List<Ingredient> ingredients)
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (Ingredient ingredient : ingredients)
        {
            if (ingredient.isEmpty())
                continue;
            ItemStack[] alternatives = ingredient.getItems();
            if (alternatives.length == 0)
                return List.of();
            stacks.add(alternatives[0].copyWithCount(1));
        }
        return stacks;
    }
}
