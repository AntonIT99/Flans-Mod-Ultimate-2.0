package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Successful-shot smoke and ejection, independent of whether a weapon draws a flash model. Client thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeaponShotEffects
{
    public static void held(LivingEntity shooter, InteractionHand hand, Vec3 muzzle, Vec3 breech)
    {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().level != shooter.level())
            return;
        if (!(shooter.getItemInHand(hand).getItem() instanceof GunItem item) || item.getConfigType().isThrowable())
            return;
        GunType gun = item.getConfigType();
        Vec3 forward = shooter.getLookAngle();
        double side = hand == InteractionHand.OFF_HAND ? -1D : 1D;
        if (shooter.getMainArm() == HumanoidArm.LEFT)
            side = -side;
        Vec3 right = right(forward).scale(side);
        // Authored casing models already animate one casing at this gun's breech.
        emit(gun, muzzle, forward, 1F, false, StringUtils.isBlank(gun.getCasingModelClassName()), breech, right);
    }

    public static void mounted(@Nullable GunType gun, Vec3 origin, Vec3 direction, float scale, boolean heavy)
    {
        if (!Float.isFinite(scale) || scale <= 0F || direction.lengthSqr() < 1.0E-8D)
            return;
        Vec3 forward = direction.normalize();
        Vec3 breech = origin.subtract(forward.scale(0.4D * Math.min(scale, 4F)));
        emit(gun, origin, forward, Math.min(scale, 4F), heavy, true, breech, right(forward));
    }

    // The positions and ejection basis are separate because a gun's breech is behind its muzzle.
    @SuppressWarnings("java:S107")
    private static void emit(@Nullable GunType gun, Vec3 origin, Vec3 forward, float scale, boolean heavy, boolean casing, Vec3 breech, Vec3 right)
    {
        Vec3 drift = forward.scale(0.03D);
        ParticleHelper.spawnFromString(FlanParticles.FM_GUN_SMOKE, origin.x, origin.y, origin.z, drift.x, drift.y, drift.z, heavy ? scale * 2F : scale);
        ModClientConfig config = ModClientConfig.get();
        boolean firearm = gun == null
            || gun.getAmmoTypes().stream().anyMatch(ammo -> ammo instanceof BulletType bullet && (bullet.getWeaponType() == EnumWeaponType.GUN || bullet.getWeaponType() == EnumWeaponType.SHELL));
        if (casing && firearm && (config == null || config.showCasingEjections))
        {
            Vec3 eject = right.scale(0.12D).add(0D, 0.09D, 0D);
            ParticleHelper.spawnFromString(FlanParticles.FM_CASING, breech.x, breech.y, breech.z, eject.x, eject.y, eject.z, scale);
        }
        if (heavy)
        {
            ParticleHelper.spawnFromString(FlanParticles.FM_NU_FLASH, origin.x, origin.y, origin.z, 0D, 0D, 0D, scale);
            ParticleHelper.spawnFromString(FlanParticles.FM_NU_MUZZLE, origin.x, origin.y, origin.z, drift.x, drift.y, drift.z, scale);
            ParticleHelper.spawnFromString(FlanParticles.FM_NU_SPARK, origin.x, origin.y, origin.z, forward.x * 0.2D, forward.y * 0.2D, forward.z * 0.2D, scale);
        }
    }

    private static Vec3 right(Vec3 forward)
    {
        Vec3 side = forward.cross(new Vec3(0D, 1D, 0D));
        return side.lengthSqr() < 1.0E-8D ? new Vec3(1D, 0D, 0D) : side.normalize();
    }
}
