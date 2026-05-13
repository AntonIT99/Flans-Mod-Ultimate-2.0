package com.flansmodultimate.common.recipe;

import com.flansmodultimate.IContentProvider;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

public class RecipeIngredient
{
    @Getter
    protected final String itemName;
    @Getter
    protected final int amount;
    @Getter
    protected final int meta;
    @Nullable
    protected final IContentProvider contentPack;
    protected ItemStack cachedStack = ItemStack.EMPTY;
    protected boolean resolved;

    public RecipeIngredient(String itemName, int amount, int meta, @Nullable IContentProvider contentPack)
    {
        this.itemName = itemName;
        this.amount = amount;
        this.meta = meta;
        this.contentPack = contentPack;
    }

    public static RecipeIngredient parse(String itemToken, int amount, @Nullable IContentProvider contentPack)
    {
        String itemName;
        int meta;

        int dotIndex = itemToken.lastIndexOf('.');
        if (isLegacyMetaSeparator(itemToken, dotIndex))
        {
            itemName = itemToken.substring(0, dotIndex);
            meta = Integer.parseInt(itemToken.substring(dotIndex + 1));
        }
        else
        {
            itemName = itemToken;
            meta = 0;
        }

        return new RecipeIngredient(itemName, amount, meta, contentPack);
    }

    private static boolean isLegacyMetaSeparator(String itemToken, int dotIndex)
    {
        if (dotIndex <= 0 || dotIndex >= itemToken.length() - 1)
            return false;

        for (int i = dotIndex + 1; i < itemToken.length(); i++)
        {
            if (!Character.isDigit(itemToken.charAt(i)))
                return false;
        }
        return true;
    }

    public ItemStack resolve()
    {
        if (!resolved)
        {
            cachedStack = RecipeResolver.resolve(itemName, amount, meta, contentPack);
            resolved = true;
        }

        return cachedStack.copy();
    }
}
