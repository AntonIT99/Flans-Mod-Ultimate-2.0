package com.flansmodultimate.common.enchantments;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentSharpshooter extends OffHandEnchantment
{
    public EnchantmentSharpshooter()
    {
        super(Rarity.RARE, false);
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }

    @Override
    public int getMinCost(int level)
    {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level)
    {
        return getMinCost(level) + 10;
    }

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other)
    {
        return !(other instanceof EnchantmentSharpshooter) && super.checkCompatibility(other);
    }
}
