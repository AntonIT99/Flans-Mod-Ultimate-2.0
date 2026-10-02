package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.PlaneItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.item.VehicleItem;
import com.flansmodultimate.common.types.GunType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Applies the Teams {@code weaponDrops} and {@code armourDrops} rules to the items a dying player drops,
 * as the 1.7.10 {@code TeamsManager#playerDrops} handler did.
 *
 * <ul>
 *     <li>{@code on}: everything drops normally.</li>
 *     <li>{@code off}: guns, planes, vehicles, AA guns and bullets are removed.</li>
 *     <li>{@code smart}: each gun type drops once, as a gun bundle carrying the dead player's matching
 *     ammunition; the remaining weapon items are removed.</li>
 * </ul>
 * With {@code armourDrops} off, Flan's armour is removed as well.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsDeathDrops
{
    public static void apply(TeamsManager manager, Collection<ItemEntity> drops)
    {
        if (manager.getWeaponDrops() == TeamsManager.EnumWeaponDrop.SMART_DROPS)
            bundleGuns(drops);

        drops.removeIf(entity -> {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty())
                return true;
            if (isWeapon(stack.getItem()))
                return manager.getWeaponDrops() != TeamsManager.EnumWeaponDrop.DROPS;
            return stack.getItem() instanceof CustomArmorItem && !manager.isArmourDrops();
        });
    }

    /** Replaces the gun drops with one bundle per gun type, each absorbing the ammunition it can fire. */
    private static void bundleGuns(Collection<ItemEntity> drops)
    {
        List<GunItemEntity> bundles = new ArrayList<>();
        List<GunType> bundledTypes = new ArrayList<>();
        for (ItemEntity entity : drops)
        {
            ItemStack stack = entity.getItem();
            if (!(stack.getItem() instanceof GunItem gunItem))
                continue;
            if (!bundledTypes.contains(gunItem.getConfigType()))
            {
                bundledTypes.add(gunItem.getConfigType());
                bundles.add(new GunItemEntity(entity));
            }
            // 1.7.10 also consumed duplicate guns of a type that already had a bundle
            entity.setItem(ItemStack.EMPTY);
        }

        for (GunItemEntity bundle : bundles)
        {
            GunType gunType = ((GunItem) bundle.getItem().getItem()).getConfigType();
            for (ItemEntity entity : drops)
            {
                ItemStack stack = entity.getItem();
                if (stack.getItem() instanceof ShootableItem shootable && gunType.getAmmoTypes().contains(shootable.getConfigType()))
                {
                    bundle.addAmmoStack(stack);
                    entity.setItem(ItemStack.EMPTY);
                }
            }
            bundle.level().addFreshEntity(bundle);
        }
    }

    private static boolean isWeapon(Item item)
    {
        return item instanceof GunItem || item instanceof PlaneItem || item instanceof VehicleItem
            || item instanceof AAGunItem || item instanceof BulletItem;
    }
}
