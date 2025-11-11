package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.types.BulletType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Class for creating an object containing all necessary information about a fired shot
 */
public class FiredShot {

    /**
     * The weapon used to fire the shot
     */
    @Getter
    private final FireableGun fireableGun;
    /**
     * The BulletType of the fired bullet
     */
    @Getter
    private final BulletType bulletType;
    /**
     * Living Entity, if one can be associated with the shot. Can be the same as shooter
     */
    @Nullable
    private final LivingEntity attacker;
    /**
     * Entity which fired the shot.
     */
    @Nullable
    private final Entity shooter;

    /**
     * @param fireableGun weapon used to fire the shot
     * @param bulletType  BulletType of the fired bullet
     */
    public FiredShot(FireableGun fireableGun, BulletType bulletType)
    {
        this(fireableGun, bulletType, null, null);
    }

    /**
     * This constructor should be used when a living entity shot
     *
     * @param fireableGun   Weapon used to fire the shot
     * @param bulletType    BulletType of the fired bullet
     * @param attacker   Entity which fired the shot
     */
    public FiredShot(FireableGun fireableGun, BulletType bulletType, @Nullable LivingEntity attacker)
    {
        this(fireableGun, bulletType, attacker, attacker);
    }

    /**
     * This constructor should be used if a living entity causes a shot, but it is actually not the entity shooting it
     * e.g a player flying a plane
     *
     * @param fireableGun   weapon used to fire the shot
     * @param bulletType    BulletType of the fired bullet
     * @param shooter       the Entity firing the shot
     * @param attacker      the living entity indirectly causing the shot
     */
    public FiredShot(FireableGun fireableGun, BulletType bulletType, @Nullable Entity shooter, @Nullable LivingEntity attacker)
    {
        this.fireableGun = fireableGun;
        this.bulletType = bulletType;
        this.attacker = attacker;
        this.shooter = shooter;
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
        return FlansDamageSources.createDamageSource(level, (bullet != null) ? bullet : shooter, attacker, headshot ? FlansDamageSources.FLANS_HEADSHOT : FlansDamageSources.FLANS_SHOOTABLE);
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
}
