package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.hits.VehiclePartHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Shared armour, projectile damage and immediate part-to-part carryover for driveables and vehicle-model entities. */
public final class VehicleProjectileImpact
{
    private VehicleProjectileImpact()
    {}

    /** Only geometry, damage delivery and feedback differ between real driveables and entities wearing their boxes. */
    public interface Target<H extends VehiclePartHit>
    {
        DriveableType type();

        DriveablePart part(EnumDriveablePart part);

        boolean active(EnumDriveablePart part);

        boolean finished();

        List<H> traceBehind(H hit);

        /** Applies the requested damage through the target's own damage hooks and returns the unspent incoming budget. */
        float apply(H hit, Impact impact, float damage, Vec3 detonation, ShootingHelper.HitData data);

        default ResolvedVehicleArmor armor()
        {
            return type().getResolvedArmor();
        }

        default boolean shotOff(EnumDriveablePart part)
        {
            DriveablePart state = part(part);
            return state != null && state.getMaxHealth() > 0F && state.isDestroyed();
        }

        default float resistance(EnumDriveablePart part)
        {
            return part(part).getPenetrationResistance();
        }
    }

    public record Impact(ResolvedArmorHit armor, VehicleProjectileDamageResolver.Result damage, float remainingPower, float penetrationRatio)
    {
        public boolean blocked()
        {
            return damage.penetration().armourGateRequired() && !damage.penetration().penetrated();
        }
    }

    private record PartResult(ShootingHelper.HitData data, float overflow)
    {}

    public static <H extends VehiclePartHit> ShootingHelper.HitData hit(Target<H> target, @Nullable FiredShot shot, BulletType bulletType, H hit, ShootingHelper.HitData data)
    {
        if (!target.active(hit.getPart()) || !data.resolvedDriveableParts().add(target.part(hit.getPart())))
            return data;
        PartResult result = hitPart(target, shot, bulletType, hit, data, Float.POSITIVE_INFINITY, hit.getHitPosition());
        result = carryOver(target, shot, bulletType, hit, result);
        // Jets resolve inside now, while their carriers detonate at the original contact. HESH remains a surface effect.
        if (bulletType.isHeat() || bulletType.isHesh() || target.part(hit.getPart()).isDestroyed() && result.overflow() <= 0F)
            return stop(result.data());
        return result.data();
    }

    private static <H extends VehiclePartHit> PartResult carryOver(Target<H> target, @Nullable FiredShot shot, BulletType bulletType, H first, PartResult result)
    {
        if (first.getEntity().level().isClientSide || bulletType.isHesh() || result.overflow() <= 0F || target.finished())
            return result;
        for (H next : target.traceBehind(first))
        {
            if (!target.active(next.getPart()) || !result.data().resolvedDriveableParts().add(target.part(next.getPart())))
                continue;
            result = hitPart(target, shot, bulletType, next, result.data(), result.overflow(), first.getHitPosition());
            if (result.overflow() <= 0F || target.finished())
                return new PartResult(stop(result.data()), 0F);
        }
        return result;
    }

    private static <H extends VehiclePartHit> PartResult hitPart(Target<H> target, @Nullable FiredShot shot, BulletType bulletType, H hit, ShootingHelper.HitData data, float limit,
        Vec3 detonation)
    {
        ResolvedArmorHit armor = resolveArmor(target, bulletType, hit, data, limit);
        // Only the actual entry face matters. A HEAT jet has no second armour penetration after its first surface.
        if (bulletType.isHeat() && Float.isFinite(limit) && armor.isArmoured())
            return new PartResult(stop(data), 0F);
        Impact impact = resolveDamage(target, shot, bulletType, hit, data.penetratingPower(), armor);
        float amount = impact.damage().damage();
        if (impact.blocked())
            amount = 0F;
        else if (Float.isFinite(limit))
            amount = limit;
        float overflow = target.apply(hit, impact, amount, detonation, data);
        ShootingHelper.HitData result = new ShootingHelper.HitData(impact.blocked() ? 0F : impact.remainingPower(), impact.blocked() ? 0F : impact.penetrationRatio(), false,
            data.resolvedDriveableParts());
        return new PartResult(result, overflow);
    }

    private static <H extends VehiclePartHit> ResolvedArmorHit resolveArmor(Target<H> target, BulletType bulletType, H hit, ShootingHelper.HitData data, float limit)
    {
        EnumDriveablePart cover = ResolvedVehicleArmor.coveringPart(hit.getFacing());
        EnumDriveablePart penetrated = null;
        if (Float.isFinite(limit) && cover != null && data.resolvedDriveableParts().contains(target.part(cover)))
            penetrated = cover;
        return target.armor().resolveHit(hit.getPart(), hit.getFacing(), hit.getLocalProjectileDirection(), ModCommonConfig.maxArmorImpactAngleDeg(), bulletType.isHeat(), target::shotOff,
            penetrated);
    }

    private static <H extends VehiclePartHit> Impact resolveDamage(Target<H> target, @Nullable FiredShot shot, BulletType bulletType, H hit, float power, ResolvedArmorHit armor)
    {
        float previousPower = Math.max(0F, power);
        float remainingPower = Math.max(0F, previousPower - target.resistance(hit.getPart()));
        float ratio = previousPower <= 0F ? 0F : remainingPower / previousPower;
        int shotIndex = shot == null ? 0 : shot.getShot();
        float fixedDamage = bulletType.getDamage().getDamageAgainstEntity(hit.getEntity());
        boolean normalized = target.type().getResolvedHealth().enabled();
        if (!normalized)
            fixedDamage *= Mth.clamp(previousPower, 0.1F, 1F);
        float p100 = shot == null ? bulletType.getPenetrationAt100m(shotIndex) : shot.getPenetrationAt100m();
        float velocity = shot == null ? ShootingHelper.getMuzzleVelocity(bulletType, shotIndex, null) : shot.getMuzzleVelocity();
        float mass = shot == null ? bulletType.getMass(shotIndex) : shot.getProjectileMass();
        VehicleProjectileDamageResolver.Result damage = VehicleProjectileDamageResolver.resolve(normalized, mass, fixedDamage, velocity, armor,
            p100 > 0F && Float.isFinite(p100) ? p100 : null, bulletType.isHeat() ? Driveable.shapedChargeKg(shot, bulletType) : 0F, ModCommonConfig.heatDamageReference());
        return new Impact(armor, damage, bulletType.isHeat() || bulletType.isHesh() ? 0F : remainingPower, ratio);
    }

    private static ShootingHelper.HitData stop(ShootingHelper.HitData data)
    {
        return new ShootingHelper.HitData(0F, data.lastHitPenAmount(), false, data.resolvedDriveableParts());
    }
}
