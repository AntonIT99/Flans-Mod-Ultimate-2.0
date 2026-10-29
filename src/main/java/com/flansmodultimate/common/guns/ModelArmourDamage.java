package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.parts.IFlanDamageModel;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.*;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.raytracing.hits.PartBodyHit;
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
 * armour, as a hit on the real driveable would be. An entity hit as one box meets the core armour, on the face chosen
 * from the round's direction relative to its body yaw; one hit by the part meets the armour of the part and face the
 * round struck.
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
     * @param shapedCharge
     *            whether the damage is that of a shaped-charge jet, whose blast then spares what the jet went through
     */
    public record Result(float damage, boolean blocked, float remainingPower, boolean shapedCharge)
    {}

    /** @return the resolved hit, or empty when the entity does not stand in for an armoured driveable */
    public static Optional<Result> resolve(Entity target, FiredShot shot, BulletType bulletType, Vec3 motion, float penetratingPower)
    {
        if (!(target instanceof IFlanDamageModel stand) || !stand.flansmodultimateModelArmour() || !(stand.flansmodultimateDamageModel().orElse(null) instanceof DriveableType type)
            || motion.lengthSqr() < 1.0E-12D)
            return Optional.empty();
        Vec3 local = localDirection(target, motion.normalize());
        // The face struck is the one turned towards where the round came from.
        EnumArmorFacing facing = EnumArmorFacing.fromOutwardNormal(local.scale(-1D));
        CollisionBox core = type.getHealth().get(EnumDriveablePart.CORE);
        return Optional
            .of(resolve(type, type.getResolvedArmor(), target, EnumDriveablePart.CORE, facing, local, core == null ? 1F : core.getPenetrationResistance(), shot, bulletType, penetratingPower));
    }

    /**
     * Resolves a round that struck one part of an entity wearing the part boxes of its driveable, against the armour of
     * that part on the face the round entered by.
     *
     * @return the resolved hit, or empty when the entity's armour is switched off or it takes no driveable's damage
     */
    public static Optional<Result> resolve(PartBodyHit hit, FiredShot shot, BulletType bulletType, float penetratingPower)
    {
        Entity target = hit.getEntity();
        if (!(target instanceof IFlanDamageModel stand) || !stand.flansmodultimateModelArmour() || !(stand.flansmodultimateDamageModel().orElse(null) instanceof DriveableType type))
            return Optional.empty();
        return Optional.of(resolve(type, hit.getBody().armor(), target, hit.getPart(), hit.getFacing(), hit.getLocalProjectileDirection(), hit.getBody().penetrationResistance(hit.getPart()), shot,
            bulletType, penetratingPower));
    }

    // Every argument is one fact of the hit; a holder would only be built to be taken apart again.
    @SuppressWarnings("java:S107")
    private static Result resolve(DriveableType type, ResolvedVehicleArmor plates, Entity target, EnumDriveablePart part, EnumArmorFacing facing, Vec3 local, float partResistance, FiredShot shot,
        BulletType bulletType, float penetratingPower)
    {
        boolean heat = bulletType.isHeat();
        ResolvedArmorHit armour = plates.resolveHit(part, facing, local, ModCommonConfig.maxArmorImpactAngleDeg(), heat);
        float previousPower = Math.max(0F, penetratingPower);
        boolean normalizedHealth = type.getResolvedHealth().enabled();
        float fixedDamage = bulletType.getDamage().getDamageAgainstEntity(target);
        float selectedFixedDamage = normalizedHealth ? fixedDamage : fixedDamage * Mth.clamp(previousPower, 0.1F, 1F);
        float p100 = shot.getPenetrationAt100m();
        VehicleProjectileDamageResolver.Result resolved = VehicleProjectileDamageResolver.resolve(normalizedHealth, shot.getProjectileMass(), selectedFixedDamage, shot.getMuzzleVelocity(), armour,
            p100 > 0F && Float.isFinite(p100) ? p100 : null, heat ? Driveable.shapedChargeKg(shot, bulletType) : 0F, ModCommonConfig.heatDamageReference());
        boolean blocked = resolved.penetration().armourGateRequired() && !resolved.penetration().penetrated();
        float resistance = Math.max(1F, partResistance);
        // Explosive warheads act at the first surface, and a stopped round goes no further.
        float remaining = heat || bulletType.isHesh() || blocked ? 0F : Math.max(0F, previousPower - resistance);
        return new Result(blocked ? 0F : resolved.damage(), blocked, remaining, resolved.shapedCharge());
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
