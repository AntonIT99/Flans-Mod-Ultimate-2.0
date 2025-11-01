package com.wolffsarmormod.common.guns;

import com.wolffsarmormod.common.types.InfoType;
import lombok.Getter;

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

    /**
     * @param type        InfoType of the gun
     * @param damage      Damage of the gun
     * @param spread      Bullet spread of the gun
     * @param bulletSpeed Bullet speed of the gun (0 means instant/raytraced)
     */
    public FireableGun(InfoType type, float damage, float spread, float bulletSpeed, EnumSpreadPattern spreadPattern)
    {
        this(type, damage, damage, spread, bulletSpeed, spreadPattern);
    }

    /**
     * @param type                  InfoType of the gun
     * @param damage                Damage of the gun
     * @param damageAgainstVehicles	Damage of the gun against vehicles
     * @param spread                Bullet spread of the gun
     * @param bulletSpeed           Bullet speed of the gun (0 means instant/raytraced)
     */
    public FireableGun(InfoType type, float damage, float damageAgainstVehicles, float spread, float bulletSpeed, EnumSpreadPattern spreadPattern)
    {
        this.type = type;
        this.damage = damage;
        this.spread = spread;
        this.bulletSpeed = bulletSpeed;
        this.damageAgainstVehicles = damageAgainstVehicles;
        this.spreadPattern = spreadPattern;
    }

    /**
     * @return the shortName of the InfoType of this gun
     */
    public String getShortName()
    {
        return type.getShortName();
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
