package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.registry.RegistryEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmbientMobArmor
{
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static volatile EquipmentPool equipmentPool;

    public static void equip(Mob mob)
    {
        EquipmentPool pool = getEquipmentPool();
        if (pool.armorPieces().isEmpty() && pool.teamOutfits().isEmpty())
            return;

        RandomSource random = mob.getRandom();
        boolean equipPiece = pool.teamOutfits().isEmpty()
            || (!pool.armorPieces().isEmpty() && random.nextBoolean());
        if (equipPiece)
        {
            ItemStack armor = pool.armorPieces().get(random.nextInt(pool.armorPieces().size()));
            mob.setItemSlot(mob.getEquipmentSlotForItem(armor), armor.copy());
            return;
        }

        Map<EquipmentSlot, ItemStack> outfit = pool.teamOutfits().get(random.nextInt(pool.teamOutfits().size()));
        outfit.forEach((slot, armor) -> mob.setItemSlot(slot, armor.copy()));
    }

    private static EquipmentPool getEquipmentPool()
    {
        EquipmentPool result = equipmentPool;
        if (result == null)
        {
            synchronized (AmbientMobArmor.class)
            {
                result = equipmentPool;
                if (result == null)
                    equipmentPool = result = buildEquipmentPool();
            }
        }
        return result;
    }

    private static EquipmentPool buildEquipmentPool()
    {
        List<ItemStack> armorPieces = FlansMod.getItems(EnumType.ARMOR).stream()
            .map(RegistryEntry::get)
            .filter(CustomArmorItem.class::isInstance)
            .map(ItemStack::new)
            .toList();

        List<Map<EquipmentSlot, ItemStack>> teamOutfits = new ArrayList<>();
        for (Team team : Team.values())
        {
            Map<EquipmentSlot, ItemStack> outfit = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : ARMOR_SLOTS)
            {
                ItemStack armor = team.getArmour(slot);
                if (!armor.isEmpty())
                    outfit.put(slot, armor.copy());
            }
            if (!outfit.isEmpty())
                teamOutfits.add(Map.copyOf(outfit));
        }
        return new EquipmentPool(List.copyOf(armorPieces), List.copyOf(teamOutfits));
    }

    private record EquipmentPool(List<ItemStack> armorPieces, List<Map<EquipmentSlot, ItemStack>> teamOutfits) {}
}
