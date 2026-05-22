package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.ArmorType;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record CustomArmorMaterial(String name, int durability, int defense, int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairMaterial)
{
    CustomArmorMaterial(ArmorType type)
    {
        this(type.getShortName(), type.getDurability(), (int) Math.round(type.getDefence() * ArmorType.ARMOR_POINT_FACTOR), type.getEnchantability(), FlansMod.getSoundEvent(type.getEquipSound()).map(h -> (Holder<SoundEvent>)h).orElse(SoundEvents.ARMOR_EQUIP_GENERIC), type.getToughness(), 0.0F, () -> Ingredient.of(Items.IRON_INGOT));
    }

    public Holder<ArmorMaterial> asArmorMaterial()
    {
        Map<ArmorItem.Type, Integer> defenseMap = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type slot : ArmorItem.Type.values())
            defenseMap.put(slot, defense);

        ArmorMaterial material = new ArmorMaterial(
            defenseMap,
            enchantability,
            equipSound,
            repairMaterial,
            List.of(),
            toughness,
            knockbackResistance
        );

        return Holder.direct(material);
    }
}