package com.flansmodultimate.common.enchantments;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.enchantment.Enchantment;

public abstract class OffHandDamageEnchantment extends OffHandEnchantment
{
    protected OffHandDamageEnchantment()
    {
        super(Rarity.COMMON, false);
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other)
    {
        return !(other instanceof OffHandDamageEnchantment) && super.checkCompatibility(other);
    }
}
