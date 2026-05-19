package com.flansmodultimate.common.enchantments;

public class EnchantmentSteady extends OffHandEnchantment
{
    public EnchantmentSteady()
    {
        super(Rarity.COMMON, false);
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }
}
