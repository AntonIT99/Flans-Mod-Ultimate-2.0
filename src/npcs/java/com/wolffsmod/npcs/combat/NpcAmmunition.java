package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.WeaponSwitch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Supplies an NPC's finite Flan equipment from its real inventory. Guns reload from the projectile slot, empty main-hand
 * and projectile slots are replenished, and an exhausted gun may be exchanged for another weapon. Without consumption
 * enabled, Flan ammunition and thrown weapons are infinite and inventory is left alone. Server thread.
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

    /**
     * @return whether a reload of the gun would load nothing: ammunition that is used up has none left, and infinite
     *         ammunition has no template to copy
     */
    public static boolean nothingToLoad(EntityNPCInterface npc, ItemStack weapon)
    {
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return !hasAmmunition(npc, weapon);
        return infiniteTemplate(npc, weapon).isEmpty();
    }

    /**
     * @return what infinite ammunition copies to reload the gun: the projectile slot when the gun takes what it holds, or
     *         else the ammunition the gun is issued with by default; never a stack to use up
     */
    public static Optional<ItemStack> infiniteTemplate(EntityNPCInterface npc, ItemStack weapon)
    {
        ItemStack projectile = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, projectile))
            return Optional.of(projectile);
        return FlansEquipment.createDefaultAmmunition(weapon).filter(ammunition -> FlansEquipment.acceptsAmmunition(weapon, ammunition));
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
     * Fills empty main-hand and projectile slots from real inventory entries, in inventory order. A replacement gun must
     * have a loaded round or available compatible ammunition. Occupied slots are preserved; only the reload path exchanges
     * incompatible ammunition. Empty-slot replenishment is independent of the exhausted-gun switching preference.
     *
     * @return whether either slot received an inventory entry; server thread only
     */
    public static boolean refillEmptySlots(EntityNPCInterface npc)
    {
        if (npc.level().isClientSide || !npc.isAlive() || npc.isKilled() || !NpcEquipment.enabled(npc, Feature.ITEM_WEAPONS) || !NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return false;
        boolean changed = false;
        if (npc.getMainHandItem().isEmpty())
        {
            int slot = find(npc, item -> usableWeapon(npc, item));
            if (slot >= 0)
            {
                swap(npc, MAIN_HAND_INDEX, slot);
                changed = true;
            }
        }
        if (stack(npc.inventory.getProjectile()).isEmpty())
        {
            ItemStack held = npc.getMainHandItem();
            int slot = find(npc, item -> FlansEquipment.acceptsAmmunition(held, item));
            if (slot < 0)
                slot = find(npc, item -> throwable(npc, item));
            if (slot >= 0)
            {
                swap(npc, PROJECTILE_INDEX, slot);
                changed = true;
            }
        }
        if (changed)
            npc.updateClient = true;
        // The item goal resolves its live weapon every tick; rebuilding it here would lose the throw cooldown.
        return changed;
    }

    /**
     * Puts the main-hand gun, which has no ammunition left, away for another weapon from the inventory, as the NPC's
     * setting allows: a gun with loaded or available ammunition, or a throwable, then the melee weapon that hits hardest.
     * The gun takes the inventory slot the weapon came from.
     *
     * @return whether the NPC now holds another weapon
     */
    public static boolean switchWeapon(EntityNPCInterface npc)
    {
        WeaponSwitch mode = NpcTuning.of(npc).weaponSwitch();
        if (mode == WeaponSwitch.NONE || !NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return false;
        int slot = mode == WeaponSwitch.GUNS_AND_MELEE ? find(npc, stack -> usableWeapon(npc, stack)) : -1;
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

    private static boolean usableWeapon(EntityNPCInterface npc, ItemStack stack)
    {
        return !stack.isEmpty() && (usableGun(npc, stack) || throwable(npc, stack));
    }

    private static boolean throwable(EntityNPCInterface npc, ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        if (FlansProjectiles.isGrenade(stack))
            return NpcEquipment.enabled(npc, Feature.GRENADES);
        return FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).filter(weapon -> weapon.throwable() && !weapon.shield()).isPresent() && !FlansEquipment.isDeployable(stack);
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
        IItemStack previous = npc.inventory.weapons.get(weaponSlot);
        ItemStack held = stack(previous);
        npc.inventory.weapons.put(weaponSlot, npc.inventory.drops.get(inventorySlot));
        if (held.isEmpty())
        {
            npc.inventory.drops.remove(inventorySlot);
            npc.inventory.dropchance.remove(inventorySlot);
        }
        else
            npc.inventory.drops.put(inventorySlot, previous);
    }

    private static ItemStack stack(@Nullable IItemStack item)
    {
        ItemStack stack = item == null ? null : item.getMCItemStack();
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
