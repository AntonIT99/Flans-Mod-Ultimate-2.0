package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.enchantments.EnchantmentModule;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ShootableType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The weapon side of a shot: everything the firing weapon contributes, with the ammunition
 * deliberately left out.
 *
 * <p>Velocity is split in two so that ammunition can take precedence without losing what the
 * weapon does to it. {@link #bulletSpeed} is the velocity the weapon supplies <em>on its own</em>
 * and is only a fallback for ammunition that declares no {@code MuzzleVelocity};
 * {@link #bulletSpeedMultiplier} is applied on top of whichever velocity wins, so a barrel
 * attachment still speeds up a round that states its own muzzle velocity.
 * {@link FiredShot#getMuzzleVelocity()} is the single place those two are combined.
 */
public class FireableGun
{
    @Getter
    private final InfoType type;
    @Getter
    private float damage;
    @Getter
    private float spread;
    /** Velocity in blocks per tick the weapon supplies before ammunition is considered. */
    @Getter
    private final float bulletSpeed;
    /** Factor the weapon's loadout applies on top of the resolved muzzle velocity. */
    @Getter
    private final float bulletSpeedMultiplier;
    @Getter
    private final EnumSpreadPattern spreadPattern;

    /** A gun item held by a living entity, with its attachments and stance resolved. */
    public FireableGun(GunType gunType, @Nullable ItemStack gunStack, @Nullable LivingEntity shooter, @Nullable ItemStack otherHandStack, EnumMovement enumMovement, boolean airborne)
    {
        this(gunType, gunType.getDamage(gunStack), gunType.getSpread(gunStack, enumMovement, airborne),
            gunType.getBaseBulletSpeed(gunStack), gunType.getBulletSpeedMultiplier(gunStack), gunType.getSpreadPattern(gunStack));
        EnchantmentModule.modifyGun(this, shooter, otherHandStack);

        if (isAimingDownSights(shooter))
            multiplySpread(gunType.getAdsSpreadMultiplier(gunStack));
    }

    /**
     * Whether the shot being composed is aimed down the sights.
     *
     * <p>The client owns nothing here beyond telling the server that it raised the sights; what
     * that is worth is the weapon's own number, resolved per shot against the stance the shooter
     * is actually in at the moment it is fired.
     */
    private static boolean isAimingDownSights(@Nullable LivingEntity shooter)
    {
        return shooter instanceof Player player && PlayerData.getInstance(player).isScoped();
    }

    /** A mounted gun, which has no item stack and therefore no attachments. */
    public FireableGun(@NotNull GunType gunType)
    {
        this(gunType, null, null, null, EnumMovement.NONE, false);
    }

    public FireableGun(InfoType type, float damage, float spread, float bulletSpeed, EnumSpreadPattern spreadPattern)
    {
        this(type, damage, spread, bulletSpeed, 1F, spreadPattern);
    }

    public FireableGun(InfoType type, float damage, float spread, float bulletSpeed, float bulletSpeedMultiplier, EnumSpreadPattern spreadPattern)
    {
        this.type = type;
        this.damage = damage;
        this.spread = spread;
        this.bulletSpeed = bulletSpeed;
        this.bulletSpeedMultiplier = bulletSpeedMultiplier > 0F ? bulletSpeedMultiplier : 1F;
        this.spreadPattern = spreadPattern;
    }

    /**
     * Folds in what the loaded ammunition does to this weapon's own numbers, so a
     * gun, an AA gun and a vehicle mount all shoot the round the same way.
     *
     * <p>Call this once, while composing a shot and before the {@link FiredShot} is
     * built: the resolved values are what a projectile carries in its save data, so
     * applying it twice would compound on reload.</p>
     */
    public void applyAmmunition(@Nullable ShootableType ammunition)
    {
        if (ammunition == null)
            return;
        damage *= ammunition.getDamageMultiplier();
        spread *= ammunition.getSpreadMultiplier();
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
