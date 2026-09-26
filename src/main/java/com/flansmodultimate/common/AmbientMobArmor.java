package com.flansmodultimate.common;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.flansmodultimate.platform.registry.RegistryEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmbientMobArmor
{
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final String NBT_PENDING = "flansmodultimate:ambient_armor_pending";
    private static final String NBT_ARMOR_SLOTS = "flansmodultimate:ambient_armor_slots";

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
            setArmor(mob, EntityPlatform.equipmentSlotFor(mob, armor), armor);
            return;
        }

        Map<EquipmentSlot, ItemStack> outfit = pool.teamOutfits().get(random.nextInt(pool.teamOutfits().size()));
        outfit.forEach((slot, armor) -> setArmor(mob, slot, armor));
    }

    private static void setArmor(Mob mob, EquipmentSlot slot, ItemStack armor)
    {
        mob.setItemSlot(slot, armor.copy());
        // Vanilla never drops this armor; dropArmor rolls ambientMobArmorDropRate instead
        mob.setDropChance(slot, 0.0F);
        CompoundTag data = mob.getPersistentData();
        data.putByte(NBT_ARMOR_SLOTS, (byte) (data.getByte(NBT_ARMOR_SLOTS) | 1 << slot.getIndex()));
    }

    /** Marks a mob to receive ambient armor once it joins the level; survives chunk-generation serialization. */
    public static void markPending(Mob mob)
    {
        mob.getPersistentData().putBoolean(NBT_PENDING, true);
    }

    /** Equips a mob previously marked by {@link #markPending}. */
    public static void equipIfPending(Mob mob)
    {
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(NBT_PENDING))
            return;
        data.remove(NBT_PENDING);
        equip(mob);
    }

    /** Adds the ambient armor still worn by a dying mob to its drops, each piece rolling ambientMobArmorDropRate. */
    public static void dropArmor(Mob mob, Collection<ItemEntity> drops)
    {
        int slots = mob.getPersistentData().getByte(NBT_ARMOR_SLOTS);
        int dropRate = ModCommonConfig.get().ambientMobArmorDropRate();
        if (slots == 0 || dropRate <= 0)
            return;

        for (EquipmentSlot slot : ARMOR_SLOTS)
        {
            ItemStack armor = mob.getItemBySlot(slot);
            if ((slots & 1 << slot.getIndex()) == 0 || armor.isEmpty() || mob.getRandom().nextInt(100) >= dropRate)
                continue;
            ItemEntity drop = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), armor.copy());
            drop.setDefaultPickUpDelay();
            drops.add(drop);
        }
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
