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
import com.wolffsmod.npcs.tactics.NpcTacticState;
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

    /**
     * A supply of one kind of round for a single inventory slot of the NPC. Ammunition that stacks is stocked a full stack
     * at a time, as it always was; ordnance whose item stacks only one high, such as a tank shell, is stocked
     * {@link NpcTuning.Value#AMMO_STOCK} rounds at a time, so that an NPC which uses its ammunition up can carry a useful
     * number of shells in its nine slots. An NPC's slots hold what its save says rather than what a container would accept,
     * and the mod leaves the drop chance of ammunition it stocks at zero.
     *
     * @param round
     *            the ammunition to stock, unchanged
     * @return a new stack of it; empty for an empty stack
     */
    public static ItemStack stock(EntityNPCInterface npc, ItemStack round)
    {
        if (round.isEmpty())
            return ItemStack.EMPTY;
        int rounds = round.getMaxStackSize() > 1 ? round.getMaxStackSize() : NpcTuning.of(npc).get(NpcTuning.Value.AMMO_STOCK);
        return round.copyWithCount(Math.max(round.getCount(), rounds));
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
     * @return what infinite ammunition copies to reload the gun: the round of those it may load that suits the NPC's target
     *         best, the projectile slot and the inventory first and the rounds the gun is issued with by default after them;
     *         never a stack to use up
     */
    public static Optional<ItemStack> infiniteTemplate(EntityNPCInterface npc, ItemStack weapon)
    {
        if (tacticallySelected(npc, weapon))
        {
            ItemStack selected = NpcWeaponChoice.ammunition(npc, weapon);
            return selected.isEmpty() ? Optional.empty() : Optional.of(selected);
        }
        List<ItemStack> pool = carried(npc, weapon, slotChoice(npc));
        append(pool, NpcDefaultAmmo.forWeapon(npc, weapon));
        // A held gun has no definition the NPC module can key a remembered choice on, so its rounds are scored as needed.
        ItemStack chosen = choose(npc, null, false, pool);
        return chosen.isEmpty() ? Optional.empty() : Optional.of(chosen);
    }

    /**
     * What the NPC fires without an equipped ranged weapon, as {@link NpcThrowing#mainProjectile} chooses it. An NPC wearing an
     * AA gun or driveable model fires what its projectile slot holds, which was put there on purpose, unless it is held to
     * ammunition its model fires. Adaptive vehicle combat always uses ammunition compatible with its selected bank, so crew MG
     * ammunition in the slot cannot replace the cannon's shell. Otherwise the round its model fires that suits its target
     * best, out of what it carries and the rounds its AA gun or selected bank is issued with by default.
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
        boolean secondary = NpcEquipment.secondaryBank(npc, weapon);
        // Anything but Flan ammunition in the projectile slot is the NPC's own Custom NPCs projectile.
        boolean arbitrarySlot = slotChoice(npc) && !(weapon instanceof IDriveableType && NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT));
        if (!projectile.isEmpty() && (arbitrarySlot || !FlansProjectiles.isProjectile(projectile)))
            return projectile;
        List<ItemStack> pool = carried(npc, weapon, secondary, projectile);
        if (options.enabled(Feature.PROJECTILES))
            append(pool, NpcDefaultAmmo.forBank(npc, weapon, secondary));
        return choose(npc, weapon, secondary, pool);
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
        boolean secondary = NpcEquipment.secondaryBank(npc, weapon);
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
     * Ammunition template for a model's driver gun: the round of those the gun may fire that suits the NPC's target best,
     * out of the projectile slot, the real inventory slots in order, and the rounds the gun is issued with by default. Model
     * weapons do not consume these stacks or exchange any inventory entries. Server thread.
     */
    public static ItemStack driverGunTemplate(EntityNPCInterface npc, IContentType gun)
    {
        List<ItemStack> pool = carried(npc, gun);
        append(pool, NpcDefaultAmmo.forGun(npc, gun));
        return choose(npc, gun, false, pool);
    }

    /**
     * Ammunition for a weapon group of the NPC's model that has no gun of its own, such as a tank's shell bank or a plane's
     * bombs: what it carries for that group and the rounds the group is issued with by default, the one that suits its target
     * best. Templates are never consumed.
     */
    public static ItemStack bankTemplate(EntityNPCInterface npc, IContentType model, boolean secondary)
    {
        List<ItemStack> pool = carried(npc, model, secondary);
        append(pool, NpcDefaultAmmo.forBank(npc, model, secondary));
        return choose(npc, model, secondary, pool);
    }

    /** Vehicle ammunition, an NPC gunner's own, then the seat gun's default rounds; templates are never consumed. */
    public static ItemStack crewGunTemplate(EntityNPCInterface vehicle, LivingEntity gunner, IContentType gun)
    {
        List<ItemStack> pool = carried(vehicle, gun);
        if (gunner instanceof EntityNPCInterface npc && npc != vehicle)
            append(pool, carried(npc, gun));
        append(pool, NpcDefaultAmmo.forGun(vehicle, gun));
        return choose(vehicle, gun, false, pool);
    }

    /** Compatible supplies for a placed gun, with the same finite/infinite fallback policy as held weapons. */
    public static ItemStack emplacementTemplate(EntityNPCInterface npc, IContentType gun)
    {
        List<ItemStack> pool = gun instanceof IAAGunType ? carried(npc, gun, false) : carried(npc, gun);
        if (pool.isEmpty())
            append(pool, gun instanceof IAAGunType ? NpcDefaultAmmo.forBank(npc, gun, false) : NpcDefaultAmmo.forGun(npc, gun));
        return choose(npc, gun, false, pool);
    }

    /** Transfers one magazine to a placed gun, or copies it when ammunition is infinite. Server thread. */
    public static ItemStack takeEmplacementAmmo(EntityNPCInterface npc, IContentType gun)
    {
        ItemStack source = emplacementTemplate(npc, gun);
        if (source.isEmpty())
            return ItemStack.EMPTY;
        ItemStack loaded = source.copyWithCount(1);
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
        {
            source.shrink(1);
            clearUsedUp(npc, source);
            npc.updateClient = true;
        }
        return loaded;
    }

    /** @return every round the NPC carries that a mounted gun fires, the projectile slot first, then the inventory in order */
    private static List<ItemStack> carried(EntityNPCInterface npc, IContentType gun)
    {
        return gun.getItem() == null ? new ArrayList<>() : carried(npc, new ItemStack(gun.getItem()), false);
    }

    /**
     * @param anyBullet
     *            whether Flan ammunition in the projectile slot counts whatever the gun takes, as a held gun allows
     * @return every round the NPC carries that the gun may load, the projectile slot first, then the inventory in order
     */
    private static List<ItemStack> carried(EntityNPCInterface npc, ItemStack weapon, boolean anyBullet)
    {
        List<ItemStack> pool = new ArrayList<>();
        ItemStack projectile = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, projectile, anyBullet))
            pool.add(projectile);
        for (ItemStack candidate : inventory(npc))
            if (FlansEquipment.acceptsAmmunition(weapon, candidate))
                pool.add(candidate);
        return pool;
    }

    /** @return every round the NPC carries that the model's selected bank fires, the projectile slot first */
    private static List<ItemStack> carried(EntityNPCInterface npc, IContentType model, boolean secondary)
    {
        return carried(npc, model, secondary, stack(npc.inventory.getProjectile()));
    }

    /**
     * @param projectile
     *            what the NPC has in its projectile slot, as its model resolved it
     * @return every round the NPC carries that the model's selected bank fires, the projectile slot first
     */
    private static List<ItemStack> carried(EntityNPCInterface npc, IContentType model, boolean secondary, ItemStack projectile)
    {
        List<ItemStack> pool = new ArrayList<>();
        if (FlansEquipment.acceptsAmmunition(model, secondary, projectile))
            pool.add(projectile);
        for (ItemStack candidate : inventory(npc))
            if (FlansEquipment.acceptsAmmunition(model, secondary, candidate))
                pool.add(candidate);
        return pool;
    }

    /** Adds the rounds that are not in the pool already, so that carried ammunition keeps its place ahead of a default round. */
    private static void append(List<ItemStack> pool, List<ItemStack> rounds)
    {
        for (ItemStack round : rounds)
        {
            if (!round.isEmpty() && pool.stream().noneMatch(present -> sameRound(present, round)))
                pool.add(round);
        }
    }

    /**
     * The round of the pool the NPC fires: the one that suits its target best, or the first of the pool, which is the
     * ammunition it carries before any default round, when it has no target or does not choose its ammunition. The choice is
     * remembered per weapon on the server, so that the rounds are scored at most every
     * {@link NpcAmmoSelection#CHECK_INTERVAL} ticks and a weapon does not alternate between two rounds that score alike.
     *
     * @param weapon
     *            the gun or model a remembered choice belongs to, or null for a held gun, whose rounds are scored as needed
     * @param pool
     *            the rounds the weapon may fire, carried ammunition first
     */
    private static ItemStack choose(EntityNPCInterface npc, @Nullable IContentType weapon, boolean secondary, List<ItemStack> pool)
    {
        if (pool.isEmpty())
            return ItemStack.EMPTY;
        LivingEntity target = npc.getTarget();
        if (pool.size() == 1 || target == null || !NpcEquipment.enabled(npc, Feature.TACTICAL_WEAPON_SWITCH))
            return pool.get(0);
        if (weapon == null || npc.level().isClientSide || !(npc instanceof NpcTacticState.Holder))
            return best(npc, target, pool);
        NpcAmmoSelection selection = NpcTacticState.of(npc).getSafety().ammoSelection(new NpcAmmoSelection.Key(weapon, secondary));
        long now = npc.level().getGameTime();
        ItemStack kept = pool.stream().filter(round -> sameRound(round, selection.getAmmunition())).findFirst().orElse(ItemStack.EMPTY);
        boolean sameTarget = selection.getTarget() == target.getId();
        if (!kept.isEmpty() && sameTarget && now < selection.getCheckedAt() + NpcAmmoSelection.CHECK_INTERVAL)
            return kept;
        selection.setCheckedAt(now);
        selection.setTarget(target.getId());
        ItemStack best = best(npc, target, pool);
        // A round just chosen stands against the same target while a loader would still be busy with it; another target is
        // engaged with the round that suits it at once.
        if (!kept.isEmpty() && sameTarget && !sameRound(best, kept) && now < selection.getChangedAt() + NpcAmmoSelection.CHANGE_INTERVAL)
            return kept;
        if (!sameRound(best, selection.getAmmunition()))
            selection.setChangedAt(now);
        selection.setAmmunition(best.copyWithCount(1));
        return best;
    }

    /** @return whether both stacks hold the same kind of round, whatever either holds of it */
    private static boolean sameRound(ItemStack first, ItemStack second)
    {
        return !first.isEmpty() && !second.isEmpty() && first.getItem() == second.getItem();
    }

    /** @return the round of the pool that scores highest against the target, the first of several that score alike */
    private static ItemStack best(EntityNPCInterface npc, LivingEntity target, List<ItemStack> pool)
    {
        int best = best(pool, round -> NpcWeaponChoice.ammunitionScore(npc, target, round), Double.NEGATIVE_INFINITY);
        return pool.get(Math.max(0, best));
    }

    /**
     * Makes sure the projectile slot holds ammunition the gun takes, fetching it from the inventory. Whatever the slot
     * held instead takes the place the ammunition came from.
     */
    private static boolean supply(EntityNPCInterface npc, ItemStack weapon)
    {
        ItemStack current = stack(npc.inventory.getProjectile());
        if (FlansEquipment.acceptsAmmunition(weapon, current, !tacticallySelected(npc, weapon) && slotChoice(npc)))
            return true;
        int slot = find(npc, stack -> FlansEquipment.acceptsAmmunition(weapon, stack));
        if (slot < 0)
            return false;
        swap(npc, PROJECTILE_INDEX, slot);
        return true;
    }

    private static boolean tacticallySelected(EntityNPCInterface npc, ItemStack weapon)
    {
        return npc instanceof NpcTacticState.Holder holder && holder.wolffsmodnpcsTacticState().getSafety().getSelectedWeapon() == weapon;
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
    static void swap(EntityNPCInterface npc, int weaponSlot, int inventorySlot)
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
