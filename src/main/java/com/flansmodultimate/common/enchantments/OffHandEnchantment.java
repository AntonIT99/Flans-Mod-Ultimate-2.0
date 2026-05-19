package com.flansmodultimate.common.enchantments;

import com.flansmodultimate.common.item.GloveItem;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public abstract class OffHandEnchantment extends Enchantment
{
    private final boolean glovesOnly;

    protected OffHandEnchantment(Rarity rarity, boolean glovesOnly)
    {
        super(rarity, EnchantmentCategory.BREAKABLE, new EquipmentSlot[] { EquipmentSlot.OFFHAND });
        this.glovesOnly = glovesOnly;
    }

    @Override
    public boolean canEnchant(@NotNull ItemStack stack)
    {
        return isValidOffHandStack(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack)
    {
        return isValidOffHandStack(stack) || stack.is(Items.BOOK);
    }

    protected boolean isValidOffHandStack(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        if (stack.getItem() instanceof GloveItem)
            return true;
        return !glovesOnly && stack.getItem() instanceof ShieldItem;
    }
}
