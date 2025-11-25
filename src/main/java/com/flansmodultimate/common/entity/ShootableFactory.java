package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootableFactory
{
    /**
     * Shootable entities associated with a living entity shooting a gun
     */
    @NotNull
    public static Shootable createShootable(Level level, @NotNull GunType gunType, @NotNull ShootableType type, @NotNull LivingEntity shooter, @NotNull ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack)
    {
        if (type instanceof BulletType bulletType)
        {
            return new Bullet(level, new FiredShot(gunType, bulletType, gunStack, shootableStack, otherHandStack, shooter), shooter.getEyePosition(0.0F), shooter.getLookAngle());
        }
        else if (type instanceof GrenadeType grenadeType)
        {
            return new Grenade(level, grenadeType, shooter);
        }
        throw new IllegalArgumentException("Unknown Shootable Type");
    }

    /**
     * For Spawning submunitions
     * @param firedShot: the shot that spawns the submunitions
     * @param type: the type of the submunition to spawn
     */
    @NotNull
    public static Shootable createShootable(Level level, @NotNull FiredShot firedShot, @NotNull ShootableType type, Vec3 origin, Vec3 direction)
    {
        if (type instanceof BulletType bulletType)
        {
            return new Bullet(level, new FiredShot(firedShot.getFireableGun(), bulletType, firedShot.getCausingEntity().orElse(null), firedShot.getAttacker().orElse(null)), origin, direction);
        }
        else if (type instanceof GrenadeType grenadeType)
        {
            return new Grenade(level, grenadeType, origin, direction, firedShot.getAttacker().orElse(null));
        }
        throw new IllegalArgumentException("Unknown Shootable Type");
    }

}
