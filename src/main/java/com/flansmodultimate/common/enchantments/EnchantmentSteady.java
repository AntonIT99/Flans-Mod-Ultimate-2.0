package com.flansmodultimate.common.enchantments;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.EquipmentSlot;

public class EnchantmentSteady extends OffHandEnchantment
{
    public EnchantmentSteady()
    {
        super(Rarity.UNCOMMON, false);
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }

    @Override
    public int getMinCost(int level)
    {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level)
    {
        return getMinCost(level) + 8;
    }
}
