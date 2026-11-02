package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansGrenadeDevices;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Finite inventory transactions and demand checks for deployed supply bags. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class NpcDeviceSupplies
{
    static ItemStack weapon(EntityNPCInterface npc)
    {
        ItemStack main = npc.getMainHandItem();
        return FlansEquipment.createDefaultAmmunition(main).isPresent() ? main : npc.getOffhandItem();
    }

    static boolean ammunitionNeeded(EntityNPCInterface npc, ItemStack weapon)
    {
        return NpcAmmunition.exhausted(npc, weapon) && FlansEquipment.createDefaultAmmunition(weapon).filter(ammo -> capacity(npc, ammo) > 0).isPresent();
    }

    static boolean needs(EntityNPCInterface npc, Entity bag)
    {
        ItemStack gun = weapon(npc);
        return FlansGrenadeDevices.needsSupply(bag, npc, gun, ammunitionNeeded(npc, gun));
    }

    static boolean needs(EntityNPCInterface npc, ItemStack bag)
    {
        ItemStack gun = weapon(npc);
        return FlansGrenadeDevices.needsSupply(bag, npc, gun, ammunitionNeeded(npc, gun));
    }

    static boolean use(EntityNPCInterface npc, Entity bag)
    {
        ItemStack gun = weapon(npc);
        boolean ammunition = ammunitionNeeded(npc, gun);
        return FlansGrenadeDevices.useSupply(bag, npc, gun, stack -> ammunition && store(npc, stack));
    }

    static int capacity(EntityNPCInterface npc, ItemStack ammo)
    {
        int capacity = 0;
        for (ItemStack stack : NpcAmmunition.inventory(npc))
            if (stack.isEmpty())
                capacity += ammo.getMaxStackSize();
            else if (NpcPlatform.isSameItemSameData(stack, ammo))
                capacity += Math.max(0, stack.getMaxStackSize() - stack.getCount());
        return capacity;
    }

    /** Stores the whole offered stack or leaves the inventory untouched. The API retains ownership of the offered stack. */
    static boolean store(EntityNPCInterface npc, ItemStack ammo)
    {
        if (ammo.isEmpty() || capacity(npc, ammo) < ammo.getCount())
            return false;
        int remaining = ammo.getCount();
        var inventory = NpcAmmunition.inventory(npc);
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++)
            remaining -= storeSlot(npc, ammo, inventory.get(slot), slot, remaining);
        npc.updateClient = true;
        return true;
    }

    private static int storeSlot(EntityNPCInterface npc, ItemStack ammo, ItemStack stack, int slot, int remaining)
    {
        if (!stack.isEmpty() && !NpcPlatform.isSameItemSameData(stack, ammo))
            return 0;
        int count = Math.min(remaining, ammo.getMaxStackSize() - stack.getCount());
        if (count <= 0)
            return 0;
        if (stack.isEmpty())
        {
            npc.inventory.drops.put(slot, NpcAPI.Instance().getIItemStack(ammo.copyWithCount(count)));
            npc.inventory.dropchance.put(slot, 0F);
        }
        else
            stack.grow(count);
        return count;
    }

}
