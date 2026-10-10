package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.IFlanDamageModel;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Resolves a round striking an entity that stands in for a driveable ({@link IFlanDamageModel}) against that driveable's
 * core armour, as a hit on the real hull would be. The entity has no part boxes, so the struck face is chosen from the
 * round's direction relative to the entity's body yaw.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelArmourDamage
{
    /**
     * @param damage
     *            damage to deal, in the same units a driveable part takes
     * @param blocked
     *            whether the armour stopped the round outright
     * @param remainingPower
     *            penetrating power the round keeps for whatever lies behind
     */
    public record Result(float damage, boolean blocked, float remainingPower)
    {}

    /** @return the resolved hit, or empty when the entity does not stand in for an armoured driveable */
    public static Optional<Result> resolve(Entity target, FiredShot shot, BulletType bulletType, Vec3 motion, float penetratingPower)
    {
        if (!(target instanceof IFlanDamageModel stand) || !stand.flansModelArmour() || !(stand.getFlansDamageModel().orElse(null) instanceof DriveableType type)
            || motion.lengthSqr() < 1.0E-12D)
            return Optional.empty();
        Vec3 local = localDirection(target, motion.normalize());
        // The face struck is the one turned towards where the round came from.
        EnumArmorFacing facing = EnumArmorFacing.fromOutwardNormal(local.scale(-1D));
        boolean heat = bulletType.isHeat();
        ResolvedArmorHit armour = type.getResolvedArmor().resolveHit(EnumDriveablePart.CORE, facing, local, ModCommonConfig.maxArmorImpactAngleDeg(), heat);
        float previousPower = Math.max(0F, penetratingPower);
        boolean normalizedHealth = type.getResolvedHealth().enabled();
        float fixedDamage = bulletType.getDamage().getDamageAgainstEntity(target);
        float selectedFixedDamage = normalizedHealth ? fixedDamage : fixedDamage * Mth.clamp(previousPower, 0.1F, 1F);
        float p100 = shot.getPenetrationAt100m();
        VehicleProjectileDamageResolver.Result resolved = VehicleProjectileDamageResolver.resolve(normalizedHealth, shot.getProjectileMass(), selectedFixedDamage, shot.getMuzzleVelocity(),
            armour, p100 > 0F && Float.isFinite(p100) ? p100 : null, heat ? Driveable.shapedChargeKg(shot, bulletType) : 0F, ModCommonConfig.heatDamageReference());
        boolean blocked = resolved.penetration().armourGateRequired() && !resolved.penetration().penetrated();
        CollisionBox core = type.getHealth().get(EnumDriveablePart.CORE);
        float resistance = core == null ? 1F : Math.max(1F, core.getPenetrationResistance());
        // A shaped charge spends its jet on the first surface, and a stopped round goes no further.
        float remaining = heat || blocked ? 0F : Math.max(0F, previousPower - resistance);
        return Optional.of(new Result(blocked ? 0F : resolved.damage(), blocked, remaining));
    }

    /**
     * The round's direction in the driveable's armour frame, where -Z faces forward, +X to the right and +Y up. Only the
     * body yaw turns the stand-in, so pitch and roll stay level.
     */
    static Vec3 localDirection(Entity target, Vec3 worldDirection)
    {
        double yaw = Math.toRadians(bodyYaw(target));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0D, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0D, -Math.sin(yaw));
        return new Vec3(worldDirection.dot(right), worldDirection.y, -worldDirection.dot(forward));
    }

    private static float bodyYaw(@Nullable Entity target)
    {
        if (target instanceof LivingEntity living)
            return living.yBodyRot;
        return target == null ? 0F : target.getYRot();
    }
}
