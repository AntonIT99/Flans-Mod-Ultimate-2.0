package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Reload bonuses from worn pouches. The fastest matching pouch wins instead of stacking. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReloadPouchSupport
{
    public static float multiplier(LivingEntity holder, ItemStack weapon)
    {
        if (!(weapon.getItem() instanceof GunItem item))
            return 1F;
        float selected = Float.NaN;
        for (ItemStack worn : holder.getArmorSlots())
        {
            if (worn.getItem() instanceof CustomArmorItem armor && !armor.isDepleted(worn) && matches(armor.getConfigType(), item.getConfigType()))
            {
                float candidate = armor.getConfigType().getReloadMultiplier();
                selected = Float.isNaN(selected) ? candidate : Math.min(selected, candidate);
            }
        }
        return Float.isNaN(selected) ? 1F : selected;
    }

    static boolean matches(ArmorType armor, GunType gun)
    {
        return armor.isHasPouch() || armor.isHasBowPouch() && gun.isBow() || armor.isHasOldGunPouch() && gun.isOldGun()
            || armor.isHasGunPouch() && gun.isRepeatingGun() || armor.isHasHeavyPouch() && gun.isHeavy();
    }
}
