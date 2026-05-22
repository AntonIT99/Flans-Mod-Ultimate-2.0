package com.flansmodultimate.common.recipe;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.TypeFile;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.logError;

public final class RecipeParser
{
    private RecipeParser()
    {
    }

    public static List<RecipeIngredient> parseAmountThenItemReferences(String[] split, int firstAmountIndex, @Nullable IContentProvider contentPack, TypeFile file, String context)
    {
        List<RecipeIngredient> ingredients = new ArrayList<>();

        for (int amountIndex = firstAmountIndex; amountIndex + 1 < split.length; amountIndex += 2)
        {
            int itemIndex = amountIndex + 1;
            Integer amount = parseAmount(split[amountIndex], file, context);
            if (amount == null)
                continue;

            String itemToken = split[itemIndex] == null ? "" : split[itemIndex].trim();
            if (itemToken.isEmpty())
            {
                logError("Missing item token in " + context, file);
                continue;
            }

            try
            {
                ingredients.add(RecipeIngredient.parse(itemToken, amount, contentPack));
            }
            catch (Exception ex)
            {
                logError("Could not parse " + context + " item '" + itemToken + "'", file, ex);
            }
        }

        return ingredients;
    }

    public static List<ItemStack> resolveAmountThenItemPairs(String[] split, int firstAmountIndex, @Nullable IContentProvider contentPack, TypeFile file, String context)
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (RecipeIngredient ingredient : parseAmountThenItemReferences(split, firstAmountIndex, contentPack, file, context))
        {
            ItemStack stack = ingredient.resolve();
            if (stack.isEmpty())
                logError("Could not find item for " + context + ": '" + ingredient.getItemName() + "'", file);
            else
                stacks.add(stack);
        }
        return stacks;
    }

    public static List<ItemStack> resolveItemThenAmountPairs(String[] split, int firstItemIndex, @Nullable IContentProvider contentPack, TypeFile file, String context)
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (RecipeIngredient ingredient : parseItemThenAmountReferences(split, firstItemIndex, contentPack, file, context))
        {
            ItemStack stack = ingredient.resolve();
            if (stack.isEmpty())
                logError("Could not find item for " + context + ": '" + ingredient.getItemName() + "'", file);
            else
                stacks.add(stack);
        }
        return stacks;
    }

    public static List<RecipeIngredient> parseItemThenAmountReferences(String[] split, int firstItemIndex, @Nullable IContentProvider contentPack, TypeFile file, String context)
    {
        List<RecipeIngredient> ingredients = new ArrayList<>();

        for (int itemIndex = firstItemIndex; itemIndex + 1 < split.length; itemIndex += 2)
        {
            int amountIndex = itemIndex + 1;
            String itemToken = split[itemIndex] == null ? "" : split[itemIndex].trim();
            if (itemToken.isEmpty())
            {
                logError("Missing item token in " + context, file);
                continue;
            }

            Integer amount = parseAmount(split[amountIndex], file, context);
            if (amount == null)
                continue;

            try
            {
                ingredients.add(RecipeIngredient.parse(itemToken, amount, contentPack));
            }
            catch (Exception ex)
            {
                logError("Could not parse " + context + " item '" + itemToken + "'", file, ex);
            }
        }

        return ingredients;
    }

    private static Integer parseAmount(String rawAmount, TypeFile file, String context)
    {
        try
        {
            return Integer.parseInt(rawAmount.trim());
        }
        catch (Exception ex)
        {
            logError("Invalid amount '" + rawAmount + "' in " + context, file);
            return null;
        }
    }
}
