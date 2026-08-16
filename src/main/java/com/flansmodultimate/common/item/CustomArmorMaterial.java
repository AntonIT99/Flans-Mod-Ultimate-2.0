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

final class CustomArmorMaterial
{
    private CustomArmorMaterial()
    {
    }

    static Holder<ArmorMaterial> create(ArmorType type)
    {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type armorType : ArmorItem.Type.values())
            defense.put(armorType, type.getDefaultMinecraftArmorPoints());

        Holder<SoundEvent> equipSound = FlansMod.getSoundEvent(type.getEquipSound())
            .<Holder<SoundEvent>>map(holder -> holder)
            .orElse(SoundEvents.ARMOR_EQUIP_GENERIC);

        ArmorMaterial material = new ArmorMaterial(
            defense,
            type.getEnchantability(),
            equipSound,
            () -> Ingredient.of(Items.IRON_INGOT),
            List.of(),
            type.getToughness(),
            0.0F
        );
        return Holder.direct(material);
    }
}
