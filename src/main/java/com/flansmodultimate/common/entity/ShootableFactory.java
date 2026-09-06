package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.guns.FireableGun;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.ShootableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The single place a fired projectile entity is created.
 *
 * <p>Every weapon - a gun in hand, a deployed gun, a mounted gun, a vehicle weapon bank - ends up
 * here, so what a round does on leaving the barrel is decided once rather than per firing path.
 * Ammunition becomes a {@link Bullet} carrying the shot's resolved ballistics; a grenade round
 * becomes a {@link Grenade} thrown along the same line.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShootableFactory
{
    /**
     * Spawns whichever projectile the given ammunition describes.
     *
     * @param shooter  the entity the shot comes from, if any
     * @param attacker the living entity credited with the shot, if any; can be the same as shooter
     * @param shot     position in the magazine, which selects the round of a belt
     */
    @NotNull
    public static Shootable createShootable(@NotNull Level level, @NotNull FireableGun fireableGun,
                                            @NotNull ShootableType type, Vec3 origin, Vec3 direction,
                                            @Nullable Entity shooter, @Nullable LivingEntity attacker, int shot)
    {
        if (type instanceof BulletType bulletType)
            return createBullet(level, new FiredShot(fireableGun, bulletType, shooter, attacker, shot), origin, direction);
        if (type instanceof GrenadeType grenadeType)
            return createGrenade(level, grenadeType, origin, direction, attacker);
        throw new IllegalArgumentException("Unknown shootable type " + type.getShortName());
    }

    /** Spawns the bullet a resolved shot describes. */
    @NotNull
    public static Bullet createBullet(@NotNull Level level, @NotNull FiredShot firedShot, Vec3 origin, Vec3 direction)
    {
        return new Bullet(level, firedShot, origin, direction);
    }

    /** Spawns a grenade round thrown along the given line. */
    @NotNull
    public static Grenade createGrenade(@NotNull Level level, @NotNull GrenadeType grenadeType, Vec3 origin,
                                        Vec3 direction, @Nullable LivingEntity attacker)
    {
        return new Grenade(level, grenadeType, origin, direction, attacker);
    }

    /**
     * For Spawning submunitions
     * @param firedShot: the shot that spawns the submunitions
     */
    public static Optional<Shootable> createSubmunition(Level level, @NotNull FiredShot firedShot, Vec3 origin, Vec3 direction)
    {
        BulletType bulletType = firedShot.getBulletType();
        ShootableType submunitionType = ShootableType.findAmmoType(bulletType.getSubmunition(), bulletType.getContentPack()).orElse(null);
        FireableGun fireableGun = firedShot.getFireableGun();

        if (submunitionType == null || fireableGun == null)
            return Optional.empty();

        // The submunition keeps the parent's weapon but scatters with its own spread.
        FireableGun submunitionGun = new FireableGun(fireableGun.getType(), fireableGun.getDamage(),
            submunitionType instanceof BulletType subBulletType ? subBulletType.getSubmunitionSpread() : fireableGun.getSpread(),
            fireableGun.getBulletSpeed(), fireableGun.getBulletSpeedMultiplier(), fireableGun.getSpreadPattern());

        return Optional.of(createShootable(level, submunitionGun, submunitionType, origin, direction,
            firedShot.getCausingEntity().orElse(null), firedShot.getAttacker().orElse(null), 0));
    }
}
