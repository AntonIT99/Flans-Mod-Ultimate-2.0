package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.types.BulletType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
     * Player, if one can be associated with the shot
     */
    @Nullable
    private final ServerPlayer player;
    /**
     * Entity which fired the shot. Can be the same as the Player optional
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
     * @param fireableGun weapon used to fire the shot
     * @param bulletType  BulletType of the fired bullet
     * @param player The player who shot
     */
    public FiredShot(FireableGun fireableGun, BulletType bulletType, ServerPlayer player)
    {
        this(fireableGun, bulletType, player, player);
    }

    /**
     * This constructor should be used when an entity shot, but no player is involved
     * e.g a zombie holding a gun or a sentry
     *
     * @param fireableGun weapon used to fire the shot
     * @param bulletType  BulletType of the fired bullet
     * @param shooter Entity which fired the shot
     */
    public FiredShot(FireableGun fireableGun, BulletType bulletType, @Nullable Entity shooter)
    {
        this(fireableGun, bulletType, shooter, null);
    }

    /**
     * This constructor should be used if a player causes a shot, but the player is actually not the entity shooting it
     * e.g a player flying a plane
     *
     * @param fireableGun  weapon used to fire the shot
     * @param bulletType  BulletType of the fired bullet
     * @param shooter the Entity firing the shot
     * @param player  the Player causing the shot
     */
    public FiredShot(FireableGun fireableGun, BulletType bulletType, @Nullable Entity shooter, @Nullable ServerPlayer player)
    {
        this.fireableGun = fireableGun;
        this.bulletType = bulletType;
        this.player = player;
        this.shooter = shooter;
    }

    /**
     * @return the matching DamageSource for the shot
     */
    public DamageSource getDamageSource(Level level)
    {
        return getDamageSource(false, level);
    }

    /**
     * @return the matching DamageSource for the shot with the additional 'headshot' information
     */
    public DamageSource getDamageSource(boolean headshot, Level level)
    {
        if (player != null)
        {
            // hitscan: direct == attacker == player
            return FlansDamageSources.createDamageSource(level, player, player, headshot ? FlansDamageSources.FLANS_GUN_HEADSHOT : FlansDamageSources.FLANS_GUN);
        }
        else if (shooter != null)
        {
            // no distinct direct cause known – at least attribute to the shooter
            return FlansDamageSources.createDamageSource(level, null, shooter, headshot ? FlansDamageSources.FLANS_GUN_HEADSHOT : FlansDamageSources.FLANS_GUN);
        }
        return level.damageSources().generic();
    }

    /**
     * @return Optional containing a player if one is involved in the cause of the shot
     */
    public Optional<ServerPlayer> getPlayerOptional()
    {
        return Optional.ofNullable(player);
    }

    /**
     * @return Optional containing the Entity which shot. Might be the same as the player optional
     */
    public Optional<Entity> getShooterOptional()
    {
        return Optional.ofNullable(shooter);
    }
}
