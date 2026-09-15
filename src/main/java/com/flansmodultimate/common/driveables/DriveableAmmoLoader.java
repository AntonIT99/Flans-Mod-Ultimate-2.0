package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fills a driveable's weapon inventory with the ammunition its own definition implies.
 *
 * <p>Every choice is read back through the accessors the firing path uses, so what is
 * loaded here is what the driveable will actually accept and fire. Three banks are
 * covered, each resolved independently:</p>
 *
 * <ul>
 *   <li>the gun ammunition slots, one per mounted pilot gun and per passenger gunner
 *       seat, filled with that gun's own default ammunition exactly as a held gun is;</li>
 *   <li>the bomb slots, when the driveable declares a bomb or mine weapon bank;</li>
 *   <li>the missile slots, which are the same slots a tank's definition calls shell
 *       slots, when it declares a missile or shell weapon bank.</li>
 * </ul>
 *
 * <p>Nothing here consumes items: this exists for the operator command, which hands out
 * ammunition the way {@code /defaultammo} does.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableAmmoLoader
{
    /** Bomb and mine rounds share one bank; missile and shell rounds share the other. */
    private static final List<EnumWeaponType> BOMB_BANK = List.of(EnumWeaponType.BOMB, EnumWeaponType.MINE);
    private static final List<EnumWeaponType> MISSILE_BANK = List.of(EnumWeaponType.MISSILE, EnumWeaponType.SHELL);

    /** One bank's outcome, already reduced to what an operator needs to read. */
    public record BankReport(String label, int slots, @Nullable String ammoName,
        int loaded, int toppedUp, int replaced, int kept, @Nullable String problem)
    {
        public int changed()
        {
            return loaded + toppedUp + replaced;
        }
    }

    /**
     * @param banks    every bank considered, in the order it should be printed
     * @param displaced ammunition taken out to make room, to be returned to the operator
     */
    public record LoadReport(List<BankReport> banks, List<ItemStack> displaced)
    {
        public int changedSlots()
        {
            return banks.stream().mapToInt(BankReport::changed).sum();
        }

        public boolean isEmpty()
        {
            return banks.isEmpty();
        }
    }

    /**
     * Loads {@code data} in place.
     *
     * @param requested ammunition the operator named explicitly, or null to use each
     *                  bank's own default. A named round also replaces ammunition
     *                  already loaded; a default one never does.
     */
    public static LoadReport load(DriveableType type, DriveableData data, @Nullable ShootableType requested)
    {
        List<BankReport> banks = new ArrayList<>();
        List<ItemStack> displaced = new ArrayList<>();

        loadGunSlots(type, data, requested, banks, displaced);
        loadWeaponBank(type, data, requested, banks, displaced, true);
        loadWeaponBank(type, data, requested, banks, displaced, false);
        loadMechaHandGunSupply(type, data, requested, banks);

        return new LoadReport(List.copyOf(banks), List.copyOf(displaced));
    }

    /**
     * Every round this driveable could be loaded with, for the command's ammunition argument.
     * Offering anything else would only produce a refusal, so this mirrors what {@link #load}
     * itself accepts: each mounted gun's own list, plus whatever the weapon banks take.
     */
    public static Set<ShootableType> loadableAmmo(DriveableType type, DriveableData data)
    {
        Set<ShootableType> ammo = new LinkedHashSet<>();
        for (int slot = 0; slot < data.getNumAmmoSlots(); slot++)
        {
            GunType gunType = type.getGunTypeForAmmoSlot(slot);
            if (gunType != null)
                ammo.addAll(gunType.getAmmoTypes());
        }
        if (type instanceof MechaType)
        {
            for (EnumMechaSlotType handSlot : List.of(EnumMechaSlotType.LEFT_TOOL, EnumMechaSlotType.RIGHT_TOOL))
            {
                if (data.getMechaAddon(handSlot).getItem() instanceof GunItem gunItem)
                    ammo.addAll(gunItem.getConfigType().getAmmoTypes());
            }
        }

        Set<EnumWeaponType> banks = new LinkedHashSet<>();
        if (data.getNumBombSlots() > 0)
            banks.addAll(bankWeaponTypes(type.weaponType(false), type.weaponType(true), true));
        if (data.getNumMissileSlots() > 0)
            banks.addAll(bankWeaponTypes(type.weaponType(false), type.weaponType(true), false));
        if (banks.isEmpty())
            return ammo;

        for (BulletType declared : type.getAmmoTypes())
        {
            if (banks.contains(declared.getWeaponType()))
                ammo.add(declared);
        }
        if (type.isAcceptAllAmmo())
        {
            for (ShootableType candidate : ShootableType.registeredAmmoTypes())
            {
                if (candidate instanceof BulletType bulletType && banks.contains(bulletType.getWeaponType())
                    && type.isValidAmmo(bulletType))
                    ammo.add(candidate);
            }
        }
        return ammo;
    }

    /** One report line per distinct mounted gun, however many slots feed it. */
    private static void loadGunSlots(DriveableType type, DriveableData data, @Nullable ShootableType requested,
        List<BankReport> banks, List<ItemStack> displaced)
    {
        Map<GunType, List<Integer>> slotsByGun = new LinkedHashMap<>();
        List<Integer> unmounted = new ArrayList<>();
        for (int slot = 0; slot < data.getNumAmmoSlots(); slot++)
        {
            GunType gunType = type.getGunTypeForAmmoSlot(slot);
            if (gunType == null)
                unmounted.add(slot);
            else
                slotsByGun.computeIfAbsent(gunType, ignored -> new ArrayList<>()).add(data.getAmmoInventoryStart() + slot);
        }

        slotsByGun.forEach((gunType, slots) -> {
            ShootableType ammoType = gunAmmo(gunType, requested);
            if (ammoType == null)
            {
                banks.add(problem(gunType.getName(), slots.size(), requested == null
                    ? "no default ammunition" : requested.getName() + " does not fit this gun"));
                return;
            }
            banks.add(fill(data, gunType.getName(), slots, ammoType, requested != null, displaced));
        });

        if (!unmounted.isEmpty())
            banks.add(problem("gun ammunition", unmounted.size(), "no gun mounted"));
    }

    /**
     * The gun decides what it accepts, exactly as it does in a player's hands, so a
     * mounted launcher takes the same rounds either way.
     */
    @Nullable
    private static ShootableType gunAmmo(GunType gunType, @Nullable ShootableType requested)
    {
        if (requested != null)
            return gunType.getAmmoTypes().contains(requested) ? requested : null;
        return gunType.getDefaultAmmo().orElse(null);
    }

    private static void loadWeaponBank(DriveableType type, DriveableData data, @Nullable ShootableType requested,
        List<BankReport> banks, List<ItemStack> displaced, boolean bombBank)
    {
        int size = bombBank ? data.getNumBombSlots() : data.getNumMissileSlots();
        if (size <= 0)
            return;

        List<EnumWeaponType> candidates = bankWeaponTypes(type.weaponType(false), type.weaponType(true), bombBank);
        String label = bankLabel(candidates.get(0));
        ShootableType ammoType = requested == null
            ? resolveBankAmmo(type, candidates)
            : acceptRequestedBankAmmo(type, candidates, requested);
        List<Integer> slots = new ArrayList<>(size);
        int start = bombBank ? data.getBombInventoryStart() : data.getMissileInventoryStart();
        for (int index = 0; index < size; index++)
            slots.add(start + index);

        if (ammoType == null)
        {
            banks.add(problem(label, size, requested == null
                ? "no " + label + " ammunition available" : requested.getName() + " is not " + label + " ammunition this driveable accepts"));
            return;
        }
        banks.add(fill(data, label, slots, ammoType, requested != null, displaced));
    }

    /**
     * Weapon types this bank may be loaded with, best first.
     *
     * <p>A definition normally states what it fires, so its own declaration wins. The
     * rest of the bank's types follow it, because legacy definitions can declare the
     * slots and leave the weapon type to be inferred from a position key that a later
     * key then overwrote.</p>
     */
    static List<EnumWeaponType> bankWeaponTypes(EnumWeaponType primary, EnumWeaponType secondary, boolean bombBank)
    {
        List<EnumWeaponType> canonical = bombBank ? BOMB_BANK : MISSILE_BANK;
        List<EnumWeaponType> ordered = new ArrayList<>(canonical.size());
        for (EnumWeaponType declared : List.of(primary, secondary))
        {
            if (canonical.contains(declared) && !ordered.contains(declared))
                ordered.add(declared);
        }
        canonical.stream().filter(weapon -> !ordered.contains(weapon)).forEach(ordered::add);
        return ordered;
    }

    /**
     * The driveable's own {@code Ammo} list decides, its first matching entry winning the
     * way the first entry of a gun's list is that gun's default round.
     *
     * <p>Official content routinely declares bomb or shell slots and no ammunition at all,
     * relying on {@code AcceptAllAmmo} to take whatever is loaded by hand. Such a definition
     * falls back to the least destructive registered round of the right kind, preferring the
     * driveable's own content pack, so an aircraft is rearmed with the small bomb of its own
     * pack rather than with the largest one some other pack happens to register.</p>
     */
    @Nullable
    private static ShootableType resolveBankAmmo(DriveableType type, List<EnumWeaponType> candidates)
    {
        for (EnumWeaponType weapon : candidates)
        {
            for (BulletType declared : type.getAmmoTypes())
            {
                if (declared.getWeaponType() == weapon)
                    return declared;
            }
        }
        if (!type.isAcceptAllAmmo())
            return null;
        for (EnumWeaponType weapon : candidates)
        {
            ShootableType fallback = weakestRegisteredAmmo(type, weapon);
            if (fallback != null)
                return fallback;
        }
        return null;
    }

    @Nullable
    private static ShootableType acceptRequestedBankAmmo(DriveableType type, List<EnumWeaponType> candidates,
        ShootableType requested)
    {
        return requested instanceof BulletType bulletType
            && candidates.contains(bulletType.getWeaponType())
            && type.isValidAmmo(bulletType) ? requested : null;
    }

    @Nullable
    private static ShootableType weakestRegisteredAmmo(DriveableType type, EnumWeaponType weapon)
    {
        ShootableType best = null;
        boolean bestInPack = false;
        for (ShootableType candidate : ShootableType.registeredAmmoTypes())
        {
            if (!(candidate instanceof BulletType bulletType) || bulletType.getWeaponType() != weapon
                || !type.isValidAmmo(bulletType))
                continue;
            boolean inPack = Objects.equals(candidate.getContentPack(), type.getContentPack());
            if (best == null || inPack && !bestInPack
                || inPack == bestInPack && candidate.getExplosionRadius() < best.getExplosionRadius())
            {
                best = candidate;
                bestInPack = inPack;
            }
        }
        return best;
    }

    /**
     * A mecha fires the guns held in its tool slots and reloads them from its own
     * inventory, so its ammunition belongs in the cargo slots rather than in a weapon
     * bank. One stack per hand gun is enough for the mecha to keep reloading itself.
     */
    private static void loadMechaHandGunSupply(DriveableType type, DriveableData data,
        @Nullable ShootableType requested, List<BankReport> banks)
    {
        if (!(type instanceof MechaType) || data.getNumCargoSlots() <= 0)
            return;

        for (EnumMechaSlotType handSlot : List.of(EnumMechaSlotType.LEFT_TOOL, EnumMechaSlotType.RIGHT_TOOL))
        {
            ItemStack held = data.getMechaAddon(handSlot);
            if (!(held.getItem() instanceof GunItem gunItem))
                continue;
            String label = handSlot.name().toLowerCase(Locale.ROOT).replace('_', ' ') + " " + gunItem.getConfigType().getName();
            ShootableType ammoType = gunAmmo(gunItem.getConfigType(), requested);
            if (ammoType == null)
            {
                banks.add(problem(label, 1, requested == null
                    ? "no default ammunition" : requested.getName() + " does not fit this gun"));
                continue;
            }
            banks.add(supplyCargo(data, label, ammoType));
        }
    }

    private static BankReport supplyCargo(DriveableData data, String label, ShootableType ammoType)
    {
        int start = data.getCargoInventoryStart();
        int freeSlot = -1;
        for (int index = 0; index < data.getNumCargoSlots(); index++)
        {
            ItemStack stack = data.getItem(start + index);
            if (stack.getItem() instanceof ShootableItem item && item.getConfigType() == ammoType)
                return new BankReport(label, 1, ammoType.getName(), 0, 0, 0, 1, null);
            if (stack.isEmpty() && freeSlot < 0)
                freeSlot = start + index;
        }
        if (freeSlot < 0)
            return problem(label, 1, "no free cargo slot for " + ammoType.getName());

        ItemStack supply = createAmmoStack(ammoType);
        if (supply.isEmpty())
            return problem(label, 1, "ammunition item is not registered: " + ammoType.getShortName());
        supply.setCount(supply.getMaxStackSize());
        data.setItem(freeSlot, supply);
        return new BankReport(label, 1, ammoType.getName(), 1, 0, 0, 0, null);
    }

    private static BankReport fill(DriveableData data, String label, List<Integer> slots, ShootableType ammoType,
        boolean replaceMismatched, List<ItemStack> displaced)
    {
        ItemStack template = createAmmoStack(ammoType);
        if (template.isEmpty())
            return problem(label, slots.size(), "ammunition item is not registered: " + ammoType.getShortName());

        int loaded = 0;
        int toppedUp = 0;
        int replaced = 0;
        int kept = 0;
        for (int slot : slots)
        {
            ItemStack current = data.getItem(slot);
            boolean sameType = current.getItem() instanceof ShootableItem item && item.getConfigType() == ammoType;
            if (sameType)
            {
                if (isFull(current))
                {
                    ++kept;
                    continue;
                }
                ItemStack toppedUpStack = current.copy();
                topUp(toppedUpStack, ammoType);
                data.setItem(slot, toppedUpStack);
                ++toppedUp;
                continue;
            }
            if (!current.isEmpty() && !replaceMismatched)
            {
                ++kept;
                continue;
            }

            if (current.isEmpty())
                ++loaded;
            else
            {
                displaced.add(current.copy());
                ++replaced;
            }
            data.setItem(slot, template.copy());
        }
        return new BankReport(label, slots.size(), ammoType.getName(), loaded, toppedUp, replaced, kept, null);
    }

    /**
     * Ammunition holding a single round carries it as the stack count, so the weapon slot
     * limit of one item already makes such a stack full. Only a magazine tracks rounds in
     * its own tag, and only that needs setting.
     */
    private static ItemStack createAmmoStack(ShootableType ammoType)
    {
        ItemStack stack = ModUtils.getItemStack(ammoType, 1).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty() && ammoType.getRoundsPerItem() > 1)
            ShootableItem.setRoundsRemaining(stack, ammoType.getRoundsPerItem());
        return stack;
    }

    private static void topUp(ItemStack stack, ShootableType ammoType)
    {
        if (ammoType.getRoundsPerItem() > 1)
            ShootableItem.setRoundsRemaining(stack, ammoType.getRoundsPerItem());
        else
            stack.setCount(1);
    }

    private static boolean isFull(ItemStack stack)
    {
        return ShootableItem.getRoundsRemaining(stack) >= Math.max(1, ShootableItem.getMaxRounds(stack));
    }

    private static BankReport problem(String label, int slots, String problem)
    {
        return new BankReport(label, slots, null, 0, 0, 0, 0, problem);
    }

    private static String bankLabel(EnumWeaponType weapon)
    {
        return switch (weapon)
        {
            case BOMB -> "bombs";
            case MINE -> "mines";
            case MISSILE -> "missiles";
            case SHELL -> "shells";
            default -> weapon.name().toLowerCase(Locale.ROOT);
        };
    }
}
