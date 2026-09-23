package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.DriveableAmmoLoader.BankReport;
import com.flansmodultimate.common.driveables.DriveableAmmoLoader.LoadReport;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/** Loads ammunition directly into AA guns and deployed guns. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MountedGunAmmoLoader
{
    public static LoadReport load(AAGun gun, @Nullable ShootableType requested)
    {
        var type = gun.getConfigType();
        if (type == null)
            return new LoadReport(List.of(), List.of());
        return load(type.getName(), gun.getAmmoSlotCount(), gun::getAmmo, gun::setAmmo,
            type.getAmmoTypes(), type.getDefaultAmmo().orElse(null), requested);
    }

    public static LoadReport load(DeployedGun gun, @Nullable ShootableType requested)
    {
        var type = gun.getConfigType();
        if (type == null)
            return new LoadReport(List.of(), List.of());
        return load(type.getName(), 1, ignored -> gun.getAmmo(), (ignored, stack) -> gun.setAmmo(stack),
            type.getAmmoTypes(), type.getDefaultAmmo().orElse(null), requested);
    }

    public static Set<ShootableType> loadableAmmo(AAGun gun)
    {
        return gun.getConfigType() == null ? Set.of()
            : new LinkedHashSet<>(gun.getConfigType().getAmmoTypes());
    }

    public static Set<ShootableType> loadableAmmo(DeployedGun gun)
    {
        return gun.getConfigType() == null ? Set.of()
            : new LinkedHashSet<>(gun.getConfigType().getAmmoTypes());
    }

    static LoadReport load(String label, int slotCount, IntFunction<ItemStack> getter,
        BiConsumer<Integer, ItemStack> setter, List<ShootableType> allowed, @Nullable ShootableType defaultAmmo,
        @Nullable ShootableType requested)
    {
        if (slotCount <= 0)
            return new LoadReport(List.of(), List.of());

        ShootableType ammoType = requested == null ? defaultAmmo : allowed.contains(requested) ? requested : null;
        if (ammoType == null)
        {
            String problem = requested == null ? "no default ammunition"
                : requested.getName() + " does not fit this gun";
            return new LoadReport(List.of(new BankReport(label, slotCount, null, 0, 0, 0, 0, problem)), List.of());
        }

        ItemStack template = createAmmoStack(ammoType);
        if (template.isEmpty())
        {
            String problem = "ammunition item is not registered: " + ammoType.getShortName();
            return new LoadReport(List.of(new BankReport(label, slotCount, null, 0, 0, 0, 0, problem)), List.of());
        }

        int loaded = 0;
        int toppedUp = 0;
        int replaced = 0;
        int kept = 0;
        List<ItemStack> displaced = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++)
        {
            ItemStack current = getter.apply(slot);
            boolean sameType = current.getItem() instanceof ShootableItem item && item.getConfigType() == ammoType;
            if (sameType)
            {
                if (ShootableItem.getRoundsRemaining(current) >= Math.max(1, ShootableItem.getMaxRounds(current)))
                {
                    ++kept;
                    continue;
                }
                ItemStack toppedUpStack = current.copy();
                topUp(toppedUpStack, ammoType);
                setter.accept(slot, toppedUpStack);
                ++toppedUp;
                continue;
            }
            if (!current.isEmpty() && requested == null)
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
            setter.accept(slot, template.copy());
        }
        BankReport bank = new BankReport(label, slotCount, ammoType.getName(), loaded, toppedUp, replaced, kept, null);
        return new LoadReport(List.of(bank), List.copyOf(displaced));
    }

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
}
