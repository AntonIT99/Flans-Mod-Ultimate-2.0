package com.flansmodultimate.common.enchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class EnchantmentJuggernaut extends Enchantment
{
    public EnchantmentJuggernaut()
    {
        super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR, new EquipmentSlot[] {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        });
    }

    @Override
    public int getMinCost(int level)
    {
        return level * 25;
    }

    @Override
    public int getMaxCost(int level)
    {
        return getMinCost(level) + 50;
    }

    @Override
    public boolean isTreasureOnly()
    {
        return true;
    }
}
