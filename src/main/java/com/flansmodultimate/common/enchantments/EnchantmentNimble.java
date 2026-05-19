package com.flansmodultimate.common.enchantments;

public class EnchantmentNimble extends OffHandEnchantment
{
    public EnchantmentNimble()
    {
        super(Rarity.UNCOMMON, true);
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }
}
