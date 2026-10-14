package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.WeaponSwitch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * The ammunition supply of an NPC told to use its ammunition up. Its guns then reload from the real stack in the
 * projectile slot, which is refilled from the NPC's inventory slots, and a gun left without ammunition may be put away
 * for another weapon from those slots. Without that setting NPC ammunition is infinite and none of this runs. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcAmmunition
{
    /** Inventory slots of a Custom NPC, which its editor lists as the items it drops. */
    public static final int INVENTORY_SLOTS = 9;
    private static final int MAIN_HAND_INDEX = 0;
    private static final int PROJECTILE_INDEX = 1;
    /** Melee damage of an empty hand; an item has to do better to count as a melee weapon. */
    private static final double UNARMED_DAMAGE = 1D;

    /**
     * Reloads the gun in a hand from the projectile slot, using the ammunition up. The slot is first given ammunition the
     * gun takes when it holds none, and is refilled from the inventory as soon as the reload empties it.
     *
     * @return whether a magazine was loaded
     */
    public static boolean load(EntityNPCInterface npc, InteractionHand hand)
    {
        ItemStack weapon = npc.getItemInHand(hand);
        if (!supply(npc, weapon))
            return false;
        ItemStack spare = stack(npc.inventory.getProjectile());
        boolean loaded = FlansEquipment.loadMagazine(npc, weapon, spare);
        if (spare.isEmpty())
        {
            npc.inventory.weapons.remove(PROJECTILE_INDEX);
            supply(npc, weapon);
        }
        npc.updateClient = true;
        return loaded;
    }

    /** @return whether the NPC uses its ammunition up and has none left, anywhere, to reload the gun with */
    public static boolean exhausted(EntityNPCInterface npc, ItemStack weapon)
    {
        return NpcEquipment.enabled(npc, Feature.CONSUME_AMMO) && !hasAmmunition(npc, weapon);
    }

    private static boolean hasAmmunition(EntityNPCInterface npc, ItemStack weapon)
    {
        return FlansEquipment.acceptsAmmunition(weapon, stack(npc.inventory.getProjectile())) || find(npc, stack -> FlansEquipment.acceptsAmmunition(weapon, stack)) >= 0;
    }

    /**
     * Makes sure the projectile slot holds ammunition the gun takes, fetching it from the inventory. Whatever the slot
     * held instead takes the place the ammunition came from.
     */
    private static boolean supply(EntityNPCInterface npc, ItemStack weapon)
    {
        ItemStack current = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, current))
            return true;
        int slot = find(npc, stack -> FlansEquipment.acceptsAmmunition(weapon, stack));
        if (slot < 0)
            return false;
        swap(npc, PROJECTILE_INDEX, slot);
        return true;
    }

    /**
     * Puts the main-hand gun, which has no ammunition left, away for another weapon from the inventory, as the NPC's
     * setting allows: another gun that is loaded or that the NPC has ammunition for, then the melee weapon that hits
     * hardest. The gun takes the inventory slot the weapon came from.
     *
     * @return whether the NPC now holds another weapon
     */
    public static boolean switchWeapon(EntityNPCInterface npc)
    {
        WeaponSwitch mode = NpcTuning.of(npc).weaponSwitch();
        if (mode == WeaponSwitch.NONE || !NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return false;
        int slot = mode == WeaponSwitch.GUNS_AND_MELEE ? find(npc, stack -> usableGun(npc, stack)) : -1;
        if (slot < 0)
            slot = best(inventory(npc), NpcAmmunition::meleeDamage, UNARMED_DAMAGE);
        if (slot < 0)
            return false;
        swap(npc, MAIN_HAND_INDEX, slot);
        // The goals that attack with the old weapon may not suit the new one.
        npc.updateAI = true;
        npc.updateClient = true;
        return true;
    }

    private static boolean usableGun(EntityNPCInterface npc, ItemStack stack)
    {
        Optional<EquippedWeaponProperties> weapon = FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY);
        return weapon.isPresent() && weapon.get().ranged() && !weapon.get().throwable() && !weapon.get().shield() && !FlansEquipment.isDeployable(stack)
            && (FlansEquipment.hasLoadedRound(stack) || hasAmmunition(npc, stack));
    }

    /** @return what the item does in melee, or the damage of an empty hand when it is not a melee weapon */
    private static double meleeDamage(ItemStack stack)
    {
        if (stack.isEmpty() || NpcEquipment.ranged(stack) || NpcEquipment.shield(stack) || FlansProjectiles.isProjectile(stack))
            return UNARMED_DAMAGE;
        double flan = FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).map(EquippedWeaponProperties::meleeDamage).orElse(0D);
        return Math.max(flan, NpcEquipment.meleeDamage(stack));
    }

    /**
     * @param floor
     *            the score an item has to beat
     * @return the index of the item that scores highest, the first of several, or -1 when none beats the floor
     */
    static <T> int best(List<T> items, ToDoubleFunction<T> score, double floor)
    {
        int best = -1;
        double highest = floor;
        for (int i = 0; i < items.size(); i++)
        {
            double value = score.applyAsDouble(items.get(i));
            if (value > highest)
            {
                highest = value;
                best = i;
            }
        }
        return best;
    }

    private static int find(EntityNPCInterface npc, Predicate<ItemStack> wanted)
    {
        List<ItemStack> inventory = inventory(npc);
        for (int slot = 0; slot < inventory.size(); slot++)
            if (wanted.test(inventory.get(slot)))
                return slot;
        return -1;
    }

    /** @return the inventory slots in order, empty stacks standing for empty slots */
    private static List<ItemStack> inventory(EntityNPCInterface npc)
    {
        ItemStack[] slots = new ItemStack[INVENTORY_SLOTS];
        for (int slot = 0; slot < INVENTORY_SLOTS; slot++)
            slots[slot] = stack(npc.inventory.drops.get(slot));
        return List.of(slots);
    }

    /** Exchanges a weapon slot with an inventory slot; an empty weapon slot leaves the inventory slot empty. */
    private static void swap(EntityNPCInterface npc, int weaponSlot, int inventorySlot)
    {
        ItemStack held = stack(npc.inventory.weapons.get(weaponSlot));
        npc.inventory.weapons.put(weaponSlot, npc.inventory.drops.get(inventorySlot));
        if (held.isEmpty())
        {
            npc.inventory.drops.remove(inventorySlot);
            npc.inventory.dropchance.remove(inventorySlot);
        }
        else
            npc.inventory.drops.put(inventorySlot, NpcAPI.Instance().getIItemStack(held));
    }

    private static ItemStack stack(@Nullable IItemStack item)
    {
        ItemStack stack = item == null ? null : ItemStackWrapper.MCItem(item);
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
