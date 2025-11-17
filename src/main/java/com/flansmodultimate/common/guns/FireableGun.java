package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/**
 * Class used for storing the properties of a gun
 */
public class FireableGun
{
    /** Spread of the bullets shot with this gun */
    @Getter
    private float spread;
    @Getter
    private final EnumSpreadPattern spreadPattern;
    /** Speed a bullet fired from this gun will travel at. (0 means instant/raytraced) */
    @Getter
    private final float bulletSpeed;
    /** the InfoType of this gun */
    @Getter
    private final InfoType type;
    /** the damage this gun will cause */
    @Getter
    private float damage;
    /** the damage this gun will cause against vehicles */
    @Getter
    private final float damageAgainstVehicles;

    public FireableGun(GunType gunType, @NotNull ItemStack gunStack)
    {
        this(gunType, gunStack, null);
    }

    public FireableGun(GunType gunType, @NotNull ItemStack gunStack, @Nullable ItemStack otherHandStack)
    {
        this(gunType, gunType.getDamage(gunStack), gunType.getSpread(gunStack), gunType.getBulletSpeed(gunStack), gunType.getSpreadPattern(gunStack));
        //TODO: shields & gloves & EnchantmentModule
        /*if (otherHandStack != null && !otherHandStack.isEmpty() && otherHandStack.getItem() instanceof ShieldItem || otherHandStack.getItem() instanceof ItemGlove)
        {
            EnchantmentModule.modifyGun(fireableGun, player, otherHand);
        }*/
    }

    public FireableGun(InfoType type, float damage, float spread, float bulletSpeed, EnumSpreadPattern spreadPattern)
    {
        this(type, damage, damage, spread, bulletSpeed, spreadPattern);
    }

    public FireableGun(InfoType type, float damage, float damageAgainstVehicles, float spread, float bulletSpeed, EnumSpreadPattern spreadPattern)
    {
        this.type = type;
        this.damage = damage;
        this.spread = spread;
        this.bulletSpeed = bulletSpeed;
        this.damageAgainstVehicles = damageAgainstVehicles;
        this.spreadPattern = spreadPattern;
    }

    public void multiplySpread(float multiplier)
    {
        spread *= multiplier;
    }

    public void multiplyDamage(float multiplier)
    {
        damage *= multiplier;
    }
}
