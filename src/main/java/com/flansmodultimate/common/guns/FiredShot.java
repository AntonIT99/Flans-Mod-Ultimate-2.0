package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.IAmmoOverrideUser;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class for creating an object containing all necessary information about a fired shot
 */
public class FiredShot
{
    /** counter of fired shots from the magazine */
    @Getter
    private int shot;
    /** The weapon used to fire the shot. Null on the client spawn-data path, where the gun is not transmitted. */
    @Getter @Nullable
    private final FireableGun fireableGun;
    /** The BulletType of the fired bullet */
    @Getter
    private final BulletType bulletType;
    /** Living Entity, if one can be associated with the shot. Can be the same as shooter */
    @Setter @Nullable
    private LivingEntity attacker;
    /** Entity which fired the shot. */
    @Setter @Nullable
    private Entity shooter;

    /** Constructor for living entities shooting with a gun item in hand */
    public FiredShot(GunType gunType, BulletType bulletType, @NotNull ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack, @NotNull LivingEntity shooter)
    {
        this(new FireableGun(gunType, gunStack, shooter, otherHandStack, ModUtils.getEnumMovement(shooter), !shooter.onGround()),
            bulletType, shooter, shooter, ShootableItem.getRoundsFired(shootableStack));
    }

    /** General Constructor */
    public FiredShot(FireableGun fireableGun, BulletType bulletType, @Nullable Entity shooter, @Nullable LivingEntity attacker, int shot)
    {
        this.fireableGun = fireableGun;
        this.bulletType = bulletType;
        this.attacker = attacker;
        this.shooter = shooter;
        this.shot = shot;
    }

    /**
     * The per-ammunition override that applies to this shot's ammunition, or
     * {@link AmmoOverride#EMPTY} when nothing declares one.
     *
     * <p>The weapon that actually fired is asked first, because it is the weapon that declared the
     * ammunition. The platform carrying it is the fallback, so a driveable can still restate what a
     * shared round does out of its own mounts without the mounted gun having to know about it.
     */
    public AmmoOverride getAmmoOverride()
    {
        AmmoOverride override = declaredOverride(fireableGun == null ? null : fireableGun.getType());

        if (override == null && shooter instanceof IFlanEntity<?> platform)
            override = declaredOverride(platform.getConfigType());

        return override == null ? AmmoOverride.EMPTY : override;
    }

    @Nullable
    private AmmoOverride declaredOverride(@Nullable InfoType weapon)
    {
        return weapon instanceof IAmmoOverrideUser user
            ? user.getAmmoOverrides().get(bulletType.getOriginalShortName())
            : null;
    }

    /** Projectile mass in grams for this shot, after any per-weapon override. */
    public float getProjectileMass()
    {
        return getAmmoOverride().resolveMass(bulletType, shot);
    }

    /**
     * Muzzle velocity in blocks per tick for this shot: the value the projectile is actually fired
     * with and the value its kinetic damage and penetration are derived from.
     *
     * <p>Precedence, highest first: a per-ammunition override declared by the weapon or its platform,
     * the round selected from an {@code AddRound} belt, the ammunition's own {@code MuzzleVelocity},
     * and finally the velocity the weapon itself supplies. Whatever wins is then scaled by the
     * weapon's attachment multiplier.
     */
    public float getMuzzleVelocity()
    {
        return getMuzzleVelocity(true);
    }

    /**
     * @param useDefaultFallback when false, zero is returned if neither the ammunition, the weapon nor an
     *                           override declares a velocity, which marks the shot as an instant raytrace
     *                           rather than a projectile entity
     */
    public float getMuzzleVelocity(boolean useDefaultFallback)
    {
        float resolved = getAmmoOverride().resolveBulletSpeed(bulletType, shot, weaponBulletSpeed(), useDefaultFallback);
        return resolved * (fireableGun == null ? 1F : fireableGun.getBulletSpeedMultiplier());
    }

    /** Bursting charge in kg TNT equivalent for this shot, after any per-weapon override. */
    public float getExplosiveMass()
    {
        return getAmmoOverride().resolveExplosiveMass(bulletType, shot);
    }

    /** Penetration in millimetres at 100 m for this shot, after any per-weapon override. */
    public float getPenetrationAt100m()
    {
        return getAmmoOverride().resolvePenetrationAt100m(bulletType, shot);
    }

    /**
     * Penetrating power for this shot. Mirrors {@link BulletType#getPenetratingPower(int, float)}
     * but derives the kinetic value from the overridden mass and velocity, so a weapon that
     * restates a shared round's ballistics also restates how far it punches through.
     */
    public float getPenetratingPower()
    {
        if (!bulletType.isPenetrates())
            return bulletType.getPenetratingPower(shot, weaponBulletSpeed());
        float mass = getProjectileMass();
        if (mass <= 0F)
            return bulletType.getPenetratingPower(shot, weaponBulletSpeed());
        return ShootingHelper.getKineticPenetratingPower(mass, getMuzzleVelocity());
    }

    /** The velocity the firing weapon contributes, or zero when the gun is unknown. */
    private float weaponBulletSpeed()
    {
        return fireableGun == null ? 0F : fireableGun.getBulletSpeed();
    }

    /** Spread pattern of the firing weapon, or a plain circle when the weapon is unknown. */
    public EnumSpreadPattern getSpreadPattern()
    {
        return fireableGun == null ? EnumSpreadPattern.CIRCLE : fireableGun.getSpreadPattern();
    }

    public float getSpread()
    {
        if (fireableGun == null)
            return bulletType.getBulletSpread();

        float spread = -1F;

        if (fireableGun.getType() instanceof GunType gunType && gunType.isAllowSpreadByBullet())
            spread = bulletType.getBulletSpread();

        if (spread <= 0F)
            spread = fireableGun.getSpread();

        return spread;
    }

    /**
     * @return the matching DamageSource for the shot
     */
    public DamageSource getDamageSource(Level level, @Nullable Bullet bullet)
    {
        return getDamageSource(false, level, bullet);
    }

    /**
     * @return the matching DamageSource for the shot with the additional 'headshot' information
     */
    public DamageSource getDamageSource(boolean headshot, Level level, @Nullable Bullet bullet)
    {
        return FlanDamageSources.createDamageSource(level, (bullet != null) ? bullet : shooter, attacker, headshot ? FlanDamageSources.HEADSHOT : FlanDamageSources.SHOOTABLE);
    }

    public Optional<ServerPlayer> getPlayerAttacker()
    {
        return Optional.ofNullable(attacker).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast);
    }

    public Optional<LivingEntity> getAttacker()
    {
        return Optional.ofNullable(attacker);
    }

    public Optional<Entity> getCausingEntity()
    {
        return Optional.ofNullable(shooter);
    }

    public List<Entity> getOwnerEntities()
    {
        List<Entity> entities = new ArrayList<>();
        if (shooter != null)
            entities.add(shooter);
        if (attacker != null)
            entities.add(attacker);
        return entities;
    }
}
