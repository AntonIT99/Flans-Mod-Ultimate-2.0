package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
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
    /** The weapon used to fire the shot */
    @Getter
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
    public FiredShot(GunType gunType, BulletType bulletType, @NotNull ItemStack gunStack, @NotNull ItemStack shootableStack, @Nullable ItemStack otherHandStack, @Nullable LivingEntity attacker)
    {
        this(new FireableGun(gunType, gunStack, shootableStack, otherHandStack), bulletType, attacker, attacker);
    }

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
