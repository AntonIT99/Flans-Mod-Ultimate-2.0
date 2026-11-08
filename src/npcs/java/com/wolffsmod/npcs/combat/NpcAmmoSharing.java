package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Transfers actual spare ammunition between allied soldiers. Callers decide allegiance, reach and cooldown. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcAmmoSharing
{
    private static final int SPARE_MAGAZINES = 2;

    /** Finds a carried round that the donor cannot use but an ally's carried weapon needs; never manufactures ammunition. */
    public static ItemStack spare(EntityNPCInterface donor, LivingEntity ally)
    {
        if (donor == ally || !NpcEquipment.enabled(donor, Feature.CONSUME_AMMO)
            || ally instanceof EntityNPCInterface npc && !NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return ItemStack.EMPTY;
        List<ItemStack> carried = NpcAmmunition.supplies(donor);
        List<ItemStack> receiving = supplies(ally);
        for (ItemStack ammo : carried)
        {
            if (ammo.isEmpty() || !FlansProjectiles.hasRoundsLeft(ammo) || carried.stream().anyMatch(weapon -> FlansEquipment.acceptsAmmunition(weapon, ammo)))
                continue;
            if (receiving.stream().anyMatch(weapon -> needs(weapon, ammo, receiving)) && room(ally, ammo))
                return ammo;
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> supplies(LivingEntity ally)
    {
        if (ally instanceof EntityNPCInterface npc)
            return NpcAmmunition.supplies(npc);
        if (ally instanceof ServerPlayer player)
        {
            List<ItemStack> items = new ArrayList<>(player.getInventory().items);
            items.add(player.getOffhandItem());
            return items;
        }
        return List.of();
    }

    private static boolean needs(ItemStack weapon, ItemStack ammo, List<ItemStack> carried)
    {
        return FlansEquipment.acceptsAmmunition(weapon, ammo)
            && carried.stream().filter(stack -> FlansEquipment.acceptsAmmunition(weapon, stack)).mapToLong(ItemStack::getCount).sum() < SPARE_MAGAZINES;
    }

    private static boolean room(LivingEntity ally, ItemStack ammo)
    {
        if (ally instanceof EntityNPCInterface npc)
            return NpcDeviceSupplies.capacity(npc, ammo) > 0;
        return ally instanceof ServerPlayer player && player.getInventory().items.stream()
            .anyMatch(stack -> stack.isEmpty() || NpcPlatform.isSameItemSameData(stack, ammo) && stack.getCount() < stack.getMaxStackSize());
    }

    /** Gives one actual ammunition item, preserving remaining rounds and tags; failed storage leaves the donor untouched. */
    public static boolean transfer(EntityNPCInterface donor, LivingEntity ally)
    {
        if (donor.level().isClientSide || ally.level() != donor.level() || !donor.isAlive() || donor.isKilled() || !ally.isAlive()
            || ally instanceof EntityNPCInterface npc && npc.isKilled())
            return false;
        ItemStack ammo = spare(donor, ally);
        if (ammo.isEmpty())
            return false;
        ItemStack offered = ammo.copyWithCount(1);
        boolean stored = ally instanceof EntityNPCInterface npc ? NpcDeviceSupplies.store(npc, offered)
            : ally instanceof ServerPlayer player && player.getInventory().add(offered);
        if (!stored)
            return false;
        ammo.shrink(1);
        NpcAmmunition.clearUsedUp(donor, ammo);
        donor.updateClient = true;
        if (ally instanceof ServerPlayer player)
            player.containerMenu.broadcastChanges();
        return true;
    }
}
