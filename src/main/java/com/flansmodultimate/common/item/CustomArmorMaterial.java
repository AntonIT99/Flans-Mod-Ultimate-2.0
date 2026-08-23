package com.flansmodultimate.common.item;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.ArmorType;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;

final class CustomArmorMaterial
{
    private CustomArmorMaterial()
    {
    }

    static ArmorMaterial create(ArmorType type)
    {
        EnumMap<net.minecraft.world.item.equipment.ArmorType, Integer> defense =
            new EnumMap<>(net.minecraft.world.item.equipment.ArmorType.class);
        for (net.minecraft.world.item.equipment.ArmorType armorType : net.minecraft.world.item.equipment.ArmorType.values())
            defense.put(armorType, type.getDefaultMinecraftArmorPoints());

        Holder<SoundEvent> equipSound = FlansMod.getSoundEvent(type.getEquipSound())
            .<Holder<SoundEvent>>map(holder -> holder)
            .orElse(SoundEvents.ARMOR_EQUIP_GENERIC);

        return new ArmorMaterial(
            1,
            defense,
            // The 26.1 Enchantable component requires a positive value. Legacy
            // armor definitions commonly use 0 to mean "not enchantable";
            // CustomArmorItem still enforces that behavior at the item API.
            Math.max(1, type.getEnchantability()),
            equipSound,
            type.getToughness(),
            0.0F,
            ItemTags.REPAIRS_IRON_ARMOR,
            // CustomArmorLayer supplies the legacy model and texture. Leather remains a
            // valid fallback asset for contexts where that custom layer is unavailable.
            EquipmentAssets.LEATHER
        );
    }
}
