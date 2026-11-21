package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.parts.IFlanDamageModel;
import com.flansmodultimate.api.projectiles.BallisticDrag;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.ShootableType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Long-range soft-target performance ({@code DynamicBulletDelay} / {@code DynamicDamage}).
 *
 * <p>
 * Once a round has flown longer than its delay, {@code DynamicDamage} takes the place of its damage against living targets.
 * Vehicles, planes and entities standing in for them keep their ordinary damage. Kinetic rounds, whose damage is derived
 * from mass and velocity, are scaled by the ratio between the long-range value and the authored damage against that target.
 */
public final class LongRangeDamage
{
    private LongRangeDamage()
    {}

    /** Whether the target is a soft target: a living entity that is not a vehicle or a stand-in for one. */
    public static boolean isSoftTarget(@Nullable Entity target)
    {
        if (!(target instanceof LivingEntity) || target instanceof Driveable)
            return false;
        return !(target instanceof IFlanDamageModel model) || model.flansmodultimateDamageModel().isEmpty();
    }

    /**
     * Ticks the shot has been in flight when it reaches the target. Entity rounds count their own age; a raytraced shot
     * takes the time a round at its muzzle velocity would need to cover its range against air drag, so both kinds switch
     * at the same range.
     */
    public static float flightTicks(@Nullable Bullet bullet, @Nullable FiredShot shot, @Nullable Entity target)
    {
        if (bullet != null)
            return bullet.tickCount;
        if (shot == null || target == null)
            return 0F;
        float velocity = shot.getMuzzleVelocity();
        if (velocity <= 0F || shot.getLaunchOrigin() == null && shot.getCausingEntity().isEmpty())
            return 0F;
        Vec3 reached = target.getBoundingBox().getCenter();
        return (float) BallisticDrag.flightTicks(velocity, shot.getQuadraticDrag(target.level()), shot.rangeTo(reached));
    }

    /** Whether the round has flown past its delay. */
    public static boolean isActive(BulletType type, float flightTicks)
    {
        return type.hasLongRangeDamage() && flightTicks > type.getDynamicBulletDelay();
    }

    /** {@link #authoredDamage(BulletType, boolean, float, float)} for a hit in play; other ammunition keeps its damage. */
    public static float authoredDamage(ShootableType type, @Nullable Shootable shootable, @Nullable FiredShot shot, @Nullable Entity target, float shortRangeDamage)
    {
        if (!(type instanceof BulletType bulletType) || !bulletType.hasLongRangeDamage())
            return shortRangeDamage;
        return authoredDamage(bulletType, isSoftTarget(target), shortRangeDamage, flightTicks(shootable instanceof Bullet bullet ? bullet : null, shot, target));
    }

    /** {@link #kineticFactor(BulletType, boolean, float, float)} for a hit in play. */
    public static float kineticFactor(FiredShot shot, @Nullable Shootable shootable, @Nullable Entity target, float shortRangeDamage)
    {
        BulletType type = shot.getBulletType();
        if (!type.hasLongRangeDamage())
            return 1F;
        return kineticFactor(type, isSoftTarget(target), shortRangeDamage, flightTicks(shootable instanceof Bullet bullet ? bullet : null, shot, target));
    }

    /**
     * The damage-against-target value an authored-damage round uses: its long-range value when active and the target is
     * soft, otherwise the given short-range value.
     */
    public static float authoredDamage(BulletType type, boolean softTarget, float shortRangeDamage, float flightTicks)
    {
        return isActive(type, flightTicks) && softTarget ? type.getDynamicDamage() : shortRangeDamage;
    }

    /**
     * The factor applied to a kinetic round's damage: long-range damage over the authored damage against the target, or 1
     * when inactive, the target is hard, or the authored damage gives no ratio.
     */
    public static float kineticFactor(BulletType type, boolean softTarget, float shortRangeDamage, float flightTicks)
    {
        if (!isActive(type, flightTicks) || !softTarget || shortRangeDamage <= 0F)
            return 1F;
        return type.getDynamicDamage() / shortRangeDamage;
    }
}
