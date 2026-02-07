package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootableFactory
{
    /**
     * General way of spawning Shootable entities
     * @param shootingEntity: the entity shooting (if any)
     * @param attacker: the living entity causing the shot (if any, can be the same as shootingEntity)
     */
    @NotNull
    public static Shootable createShootable(Level level, @NotNull FireableGun fireableGun, @NotNull ShootableType type, Vec3 origin, Vec3 direction, @Nullable Entity shootingEntity, @Nullable LivingEntity attacker)
    {
        if (type instanceof BulletType bulletType)
        {
            return new Bullet(level, new FiredShot(fireableGun, bulletType, shootingEntity, attacker), origin, direction);
        }
        else if (type instanceof GrenadeType grenadeType)
        {
            return new Grenade(level, grenadeType, origin, direction, attacker);
        }
        throw new IllegalArgumentException("Unknown Shootable Type");
    }

    /**
     * For spawning Shootable entities associated with a living entity shooting a gun item
     */
    @NotNull
    public static Shootable createShootable(Level level, @NotNull GunType gunType, @NotNull ShootableType type, @NotNull LivingEntity shooter, @NotNull ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack)
    {
        if (type instanceof BulletType bulletType)
        {
            return new Bullet(level, new FiredShot(gunType, bulletType, gunStack, shootableStack, otherHandStack, shooter), shooter.getEyePosition(0F), shooter.getLookAngle());
        }
        else if (type instanceof GrenadeType grenadeType)
        {
            return new Grenade(level, grenadeType, shooter);
        }
        throw new IllegalArgumentException("Unknown Shootable Type");
    }

    /**
     * For spawning Shootable entities associated with a living entity shooting a deployable gun
     */
    @NotNull
    public static Shootable createShootable(Level level, @NotNull ShootableType type, @NotNull DeployedGun deployedGun, @Nullable LivingEntity shooter, @NotNull ItemStack shootableStack)
    {
        if (type instanceof BulletType bulletType)
        {
            return new Bullet(level, new FiredShot(deployedGun.getConfigType(), bulletType, shootableStack, deployedGun, shooter), deployedGun.getShootingOrigin(), deployedGun.getShootingDirection());
        }
        else if (type instanceof GrenadeType grenadeType)
        {
            return new Grenade(level, grenadeType, deployedGun.getShootingOrigin(), deployedGun.getShootingPitch(), deployedGun.getShootingYaw(), shooter);
        }
        throw new IllegalArgumentException("Unknown Shootable Type");
    }

    /**
     * For Spawning submunitions
     * @param firedShot: the shot that spawns the submunitions
     */
    public static Optional<Shootable> createSubmunition(Level level, @NotNull FiredShot firedShot, Vec3 origin, Vec3 direction)
    {
        BulletType bulletType = firedShot.getBulletType();
        ShootableType submunitionType = ShootableType.getAmmoType(bulletType.getSubmunition(), bulletType.getContentPack()).orElse(null);

        if (submunitionType instanceof BulletType subBulletType)
        {
            FireableGun fireableGun = firedShot.getFireableGun();
            FireableGun newFireableGun = new FireableGun(fireableGun.getType(), fireableGun.getDamage(), subBulletType.getSubmunitionSpread(), fireableGun.getBulletSpeed(), fireableGun.getSpreadPattern());
            FiredShot newFiredShot = new FiredShot(newFireableGun, subBulletType, firedShot.getCausingEntity().orElse(null), firedShot.getAttacker().orElse(null));
            return Optional.of(new Bullet(level, newFiredShot, origin, direction));
        }
        else if (submunitionType instanceof GrenadeType grenadeType)
        {
            return Optional.of(new Grenade(level, grenadeType, origin, direction, firedShot.getAttacker().orElse(null)));
        }
        return Optional.empty();
    }

}
