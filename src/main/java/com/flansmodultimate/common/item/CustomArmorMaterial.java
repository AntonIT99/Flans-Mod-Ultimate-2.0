package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.ArmorType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public record CustomArmorMaterial(String name, int durability, int defense, int enchantability, SoundEvent equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairMaterial) implements ArmorMaterial
{
    CustomArmorMaterial(ArmorType type)
    {
        this(type.getShortName(), type.getDurability(), (int) Math.max(type.getDamageReductionAmount(), Math.round(type.getDefence() * 20.0F)), type.getEnchantability(), SoundEvents.ARMOR_EQUIP_IRON, type.getToughness(), 0.0F, () -> Ingredient.of(Items.IRON_INGOT));
    }

    @Override
    public int getDurabilityForType(@NotNull ArmorItem.Type slot)
    {
        return durability;
    }

    @Override
    public int getDefenseForType(@NotNull ArmorItem.Type slot)
    {
        return defense;
    }

    @Override
    public int getEnchantmentValue()
    {
        return enchantability;
    }

    @Override
    @NotNull
    public SoundEvent getEquipSound()
    {
        return equipSound;
    }

    @Override
    @NotNull
    public Ingredient getRepairIngredient()
    {
        return repairMaterial.get();
    }

    @Override
    @NotNull
    public String getName()
    {
        return name;
    }

    @Override
    public float getToughness()
    {
        return toughness;
    }

    @Override
    public float getKnockbackResistance()
    {
        return knockbackResistance;
    }
}
