package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.enchantments.EnchantmentModule;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class FireableGun
{
    @Getter
    private final InfoType type;
    @Getter
    private float damage;
    @Getter
    private float spread;
    @Getter
    private final float bulletSpeed;
    @Getter
    private final EnumSpreadPattern spreadPattern;

    public FireableGun(GunType gunType, @Nullable ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable LivingEntity shooter, @Nullable ItemStack otherHandStack, EnumMovement enumMovement, boolean airborne)
    {
        this(gunType, gunType.getDamage(gunStack), gunType.getSpread(gunStack, enumMovement, airborne), gunType.getBulletSpeed(gunStack, shootableStack), gunType.getSpreadPattern(gunStack));
        EnchantmentModule.modifyGun(this, shooter, otherHandStack);
    }

    public FireableGun(GunType gunType, @Nullable ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack, EnumMovement enumMovement, boolean airborne)
    {
        this(gunType, gunStack, shootableStack, null, otherHandStack, enumMovement, airborne);
    }

    public FireableGun(GunType gunType, @NotNull ItemStack shootableStack, @Nullable LivingEntity shooter, @Nullable ItemStack otherHandStack)
    {
        this(gunType, null, shootableStack, shooter, otherHandStack, EnumMovement.NONE, false);
    }

    public FireableGun(GunType gunType, @NotNull ItemStack shootableStack)
    {
        this(gunType, null, shootableStack, null, null, EnumMovement.NONE, false);
    }

    public FireableGun(InfoType type, float damage, float spread, float bulletSpeed, EnumSpreadPattern spreadPattern)
    {
        this.type = type;
        this.damage = damage;
        this.spread = spread;
        this.bulletSpeed = bulletSpeed;
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
