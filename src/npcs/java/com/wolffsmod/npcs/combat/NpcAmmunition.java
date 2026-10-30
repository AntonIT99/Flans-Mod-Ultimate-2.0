package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IAAGunType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.WeaponKind;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.WeaponSwitch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
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
     * Reloads the gun in a hand from the projectile slot, using the ammunition up: any Flan bullet put there, unless the NPC
     * is held to ammunition that fits. The slot is first given ammunition the
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
        boolean loaded = FlansEquipment.loadMagazine(npc, weapon, spare, slotChoice(npc));
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
     * @return what infinite ammunition copies to reload the gun: the projectile slot when the gun may load what it holds, then the
     *         first inventory slot it takes, or else the ammunition the gun is issued with by default; never a stack to use up
     */
    public static Optional<ItemStack> infiniteTemplate(EntityNPCInterface npc, ItemStack weapon)
    {
        ItemStack projectile = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, projectile, slotChoice(npc)))
            return Optional.of(projectile);
        for (ItemStack candidate : inventory(npc))
            if (FlansEquipment.acceptsAmmunition(weapon, candidate))
                return Optional.of(candidate);
        return FlansEquipment.createDefaultAmmunition(weapon).filter(ammunition -> FlansEquipment.acceptsAmmunition(weapon, ammunition));
    }

    /**
     * What the NPC fires without an equipped ranged weapon, as {@link NpcThrowing#mainProjectile} chooses it. An NPC wearing an
     * AA gun or driveable model fires what its projectile slot holds, which was put there on purpose, unless it is held to
     * ammunition its model fires; otherwise the first round in its inventory its model fires, or the AA gun's or selected
     * bank's default when it carries none and consumption is off.
     */
    public static ItemStack modelTemplate(EntityNPCInterface npc)
    {
        ItemStack projectile = NpcThrowing.mainProjectile(npc);
        // Old saves can leave the final, zero-round magazine in the projectile slot.
        if (NpcModels.flan(npc) == null && FlansProjectiles.isProjectile(projectile) && !FlansProjectiles.hasRoundsLeft(projectile) && NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)
            && !npc.level().isClientSide)
        {
            consume(npc, projectile);
            projectile = NpcThrowing.mainProjectile(npc);
        }
        IContentType weapon = modelWeapon(npc);
        if (weapon == null)
            return projectile;
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        boolean secondary = NpcEquipment.secondaryBank(options, weapon);
        // Anything but Flan ammunition in the projectile slot is the NPC's own Custom NPCs projectile.
        if (!projectile.isEmpty() && (slotChoice(npc) || !FlansProjectiles.isProjectile(projectile) || FlansEquipment.acceptsAmmunition(weapon, secondary, projectile)))
            return projectile;
        for (ItemStack candidate : inventory(npc))
            if (FlansEquipment.acceptsAmmunition(weapon, secondary, candidate))
                return candidate;
        if (!options.enabled(Feature.PROJECTILES) || NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return ItemStack.EMPTY;
        return FlansEquipment.createDefaultAmmunition(weapon, secondary).orElse(ItemStack.EMPTY);
    }

    /**
     * @return whether Flan ammunition in the projectile slot is fired whatever the weapon takes, as it was put there on
     *         purpose: true unless the NPC is held to ammunition that fits. The inventory only ever supplies ammunition that
     *         fits.
     */
    public static boolean slotChoice(EntityNPCInterface npc)
    {
        return !NpcEquipment.enabled(npc, Feature.FITTING_SLOT_AMMO);
    }

    /**
     * The ammunition a model's inherited firing statistics are read with: the Flan round the NPC fires, or else the AA gun's
     * or selected bank's default, so that the NPC shows what its model would fire even while it carries nothing it can.
     *
     * @param fired
     *            what {@link #modelTemplate} returns for the NPC
     * @return the round, or what the NPC fires when the model has no default either
     */
    public static ItemStack modelReference(EntityNPCInterface npc, ItemStack fired)
    {
        IContentType weapon = modelWeapon(npc);
        if (weapon == null || FlansProjectiles.isProjectile(fired))
            return fired;
        boolean secondary = NpcEquipment.secondaryBank(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions(), weapon);
        return FlansEquipment.createDefaultAmmunition(weapon, secondary).orElse(fired);
    }

    /** @return the AA gun or driveable whose model the NPC wears, or null */
    @Nullable
    private static IContentType modelWeapon(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType definition = model == null ? null : model.getInfoType();
        return definition instanceof IDriveableType || definition instanceof IAAGunType ? definition : null;
    }

    private static boolean hasAmmunition(EntityNPCInterface npc, ItemStack weapon)
    {
        return FlansEquipment.acceptsAmmunition(weapon, stack(npc.inventory.getProjectile()), slotChoice(npc)) || find(npc, stack -> FlansEquipment.acceptsAmmunition(weapon, stack)) >= 0;
    }

    /**
     * Ammunition template for a model's driver gun: the projectile slot, then real inventory slots in order, then the
     * gun's default. Model weapons do not consume these stacks or exchange any inventory entries. Server thread.
     */
    public static ItemStack driverGunTemplate(EntityNPCInterface npc, IContentType gun)
    {
        return gunTemplate(npc, gun).orElseGet(() -> FlansEquipment.createDefaultAmmunition(gun).orElse(ItemStack.EMPTY));
    }

    /**
     * Compatible inventory ammunition for a weapon group of the NPC's model that has no gun of its own, such as a tank's shell
     * bank or a plane's bombs, then that group's default; templates are never consumed.
     */
    public static ItemStack bankTemplate(EntityNPCInterface npc, IContentType model, boolean secondary)
    {
        ItemStack projectile = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(model, secondary, projectile))
            return projectile;
        return inventory(npc).stream().filter(candidate -> FlansEquipment.acceptsAmmunition(model, secondary, candidate)).findFirst()
            .orElseGet(() -> FlansEquipment.createDefaultAmmunition(model, secondary).orElse(ItemStack.EMPTY));
    }

    /** Compatible vehicle ammunition, then an NPC gunner's ammunition, then the seat gun's default; templates are never consumed. */
    public static ItemStack crewGunTemplate(EntityNPCInterface vehicle, LivingEntity gunner, IContentType gun)
    {
        return gunTemplate(vehicle, gun).or(() -> gunner instanceof EntityNPCInterface npc ? gunTemplate(npc, gun) : Optional.empty())
            .orElseGet(() -> FlansEquipment.createDefaultAmmunition(gun).orElse(ItemStack.EMPTY));
    }

    private static Optional<ItemStack> gunTemplate(EntityNPCInterface npc, IContentType gun)
    {
        if (gun.getItem() == null)
            return Optional.empty();
        ItemStack weapon = new ItemStack(gun.getItem());
        ItemStack projectile = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, projectile))
            return Optional.of(projectile);
        return inventory(npc).stream().filter(candidate -> FlansEquipment.acceptsAmmunition(weapon, candidate)).findFirst();
    }

    /**
     * Makes sure the projectile slot holds ammunition the gun takes, fetching it from the inventory. Whatever the slot
     * held instead takes the place the ammunition came from.
     */
    private static boolean supply(EntityNPCInterface npc, ItemStack weapon)
    {
        ItemStack current = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, current, slotChoice(npc)))
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
        return FlansEquipment.getWeaponKind(stack).filter(kind -> kind.throwable() && !kind.shield()).isPresent() && !FlansEquipment.isDeployable(stack);
    }

    private static boolean usableGun(EntityNPCInterface npc, ItemStack stack)
    {
        Optional<WeaponKind> weapon = FlansEquipment.getWeaponKind(stack);
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

    /**
     * Uses up one of a throwable or round the NPC carries and threw, or fired without a gun: one round of Flan ammunition,
     * otherwise one item. A slot the stack is left empty in is cleared, and an emptied projectile slot takes the next stack of
     * the same item from the inventory, so that the item stays the preferred one. Server thread.
     */
    public static void consume(EntityNPCInterface npc, ItemStack used)
    {
        ItemStack template = used.copyWithCount(1);
        boolean ammunition = FlansProjectiles.isProjectile(used);
        if (ammunition)
        {
            FlansProjectiles.consumeRound(used);
            if (!FlansProjectiles.hasRoundsLeft(used))
                used.setCount(0);
        }
        else
            used.shrink(1);
        npc.updateClient = true;
        if (!used.isEmpty())
            return;
        if (stack(npc.inventory.getProjectile()) == used)
        {
            npc.inventory.weapons.remove(PROJECTILE_INDEX);
            // Flan ammunition counts its rounds in its tag, which a fresh item of it does not share.
            int slot = find(npc, stack -> ammunition ? ItemStack.isSameItem(stack, template) && FlansProjectiles.hasRoundsLeft(stack) : NpcPlatform.isSameItemSameData(stack, template));
            if (slot >= 0)
                swap(npc, PROJECTILE_INDEX, slot);
        }
        else
            removeEmptyInventoryStack(npc, used);
    }

    /** Takes a stack the NPC carried and used up, such as a spent tool, out of its weapon or inventory slot. Server thread. */
    public static void clearUsedUp(EntityNPCInterface npc, ItemStack used)
    {
        if (!used.isEmpty())
            return;
        npc.updateClient = true;
        if (npc.inventory.weapons.values().removeIf(item -> stack(item) == used))
            return;
        removeEmptyInventoryStack(npc, used);
    }

    private static void removeEmptyInventoryStack(EntityNPCInterface npc, ItemStack used)
    {
        List<ItemStack> inventory = inventory(npc);
        for (int slot = 0; slot < inventory.size(); slot++)
        {
            if (inventory.get(slot) == used)
            {
                npc.inventory.drops.remove(slot);
                npc.inventory.dropchance.remove(slot);
                return;
            }
        }
    }

    /** @return what the projectile slot holds, or an empty stack */
    static ItemStack projectile(EntityNPCInterface npc)
    {
        return stack(npc.inventory.getProjectile());
    }

    /** Actual carried stacks for supplies and upgrades; mutating a returned stack mutates the NPC inventory. */
    public static List<ItemStack> supplies(EntityNPCInterface npc)
    {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(npc.getMainHandItem());
        stacks.add(npc.getOffhandItem());
        stacks.add(projectile(npc));
        stacks.addAll(inventory(npc));
        return List.copyOf(stacks);
    }

    /** @return the inventory slots in order, empty stacks standing for empty slots */
    static List<ItemStack> inventory(EntityNPCInterface npc)
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
