package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/** Successful-shot smoke and ejection, independent of whether a weapon draws a flash model. Client thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeaponShotEffects
{
    private static final Map<InteractionHand, PendingSmoke> FIRST_PERSON_SMOKE = new EnumMap<>(InteractionHand.class);
    private static final Matrix4f WORLD_FROM_VIEW = new Matrix4f();
    private static final Matrix4f WORLD_PROJECTION_INVERSE = new Matrix4f();

    public static void captureWorldView(PoseStack poseStack, Matrix4f projection)
    {
        if (ShaderPlatform.isRenderingShadowPass())
            return;
        Minecraft minecraft = Minecraft.getInstance();
        FIRST_PERSON_SMOKE.values().removeIf(pending -> pending.shooter != minecraft.player || pending.tick + 2 < pending.shooter.level().getGameTime());
        WORLD_FROM_VIEW.set(poseStack.last().pose()).invert();
        WORLD_PROJECTION_INVERSE.set(projection).invert();
    }

    private record PendingSmoke(LivingEntity shooter, GunType gun, long tick, int count)
    {}

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
        Minecraft minecraft = Minecraft.getInstance();
        boolean firstPerson = shooter == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
        if (firstPerson)
        {
            PendingSmoke previous = FIRST_PERSON_SMOKE.get(hand);
            long tick = shooter.level().getGameTime();
            int count = previous != null && previous.shooter == shooter && previous.gun == gun && previous.tick == tick ? previous.count + 1 : 1;
            FIRST_PERSON_SMOKE.put(hand, new PendingSmoke(shooter, gun, tick, count));
        }
        // Authored casing models already animate one casing at this gun's breech.
        emit(gun, muzzle, forward, 1F, false, StringUtils.isBlank(gun.getCasingModelClassName()), breech, right, !firstPerson);
    }

    /** Consume confirmed shots once, at the same transformed point used to draw the first-person flash. */
    public static void firstPersonSmoke(GunType gun, boolean leftHand, PoseStack poseStack, float x, float y, float z)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson() || ShaderPlatform.isRenderingShadowPass())
            return;
        InteractionHand hand = leftHand == (minecraft.player.getMainArm() == HumanoidArm.LEFT) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        PendingSmoke pending = FIRST_PERSON_SMOKE.remove(hand);
        if (pending == null || pending.shooter != minecraft.player || pending.gun != gun || pending.tick + 2 < minecraft.level.getGameTime())
            return;
        Vector3f point = poseStack.last().pose().transformPosition(new Vector3f(x, y, z));
        Vector3f direction = poseStack.last().pose().transformDirection(new Vector3f(1F, 0F, 0F)).normalize();
        // Hands and world particles can use different FOVs. Preserve the flash's screen position when crossing passes.
        Matrix4f viewFromHand = new Matrix4f(WORLD_PROJECTION_INVERSE).mul(RenderSystem.getProjectionMatrix());
        viewFromHand.transformProject(point);
        WORLD_FROM_VIEW.transformPosition(point);
        WORLD_FROM_VIEW.transformDirection(direction).normalize();
        var camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition().add(point.x, point.y, point.z);
        Vec3 drift = new Vec3(direction.x, direction.y, direction.z).scale(0.03D);
        ModClientConfig config = ModClientConfig.get();
        if (config == null || config.showWeaponSmoke)
            for (int i = 0; i < pending.count; i++)
                ParticleHelper.spawnFromString(FlanParticles.FM_GUN_SMOKE, origin.x, origin.y, origin.z, drift.x, drift.y, drift.z, 1F);
    }

    public static void mounted(@Nullable GunType gun, Vec3 origin, Vec3 direction, float scale, boolean heavy)
    {
        if (!Float.isFinite(scale) || scale <= 0F || direction.lengthSqr() < 1.0E-8D)
            return;
        Vec3 forward = direction.normalize();
        Vec3 breech = origin.subtract(forward.scale(0.4D * Math.min(scale, 4F)));
        emit(gun, origin, forward, Math.min(scale, 4F), heavy, true, breech, right(forward), true);
    }

    // The positions and ejection basis are separate because a gun's breech is behind its muzzle.
    @SuppressWarnings("java:S107")
    private static void emit(@Nullable GunType gun, Vec3 origin, Vec3 forward, float scale, boolean heavy, boolean casing, Vec3 breech, Vec3 right, boolean smoke)
    {
        Vec3 drift = forward.scale(0.03D);
        ModClientConfig config = ModClientConfig.get();
        if (smoke && (config == null || config.showWeaponSmoke))
            ParticleHelper.spawnFromString(FlanParticles.FM_GUN_SMOKE, origin.x, origin.y, origin.z, drift.x, drift.y, drift.z, heavy ? scale * 2F : scale);
        boolean firearm = gun == null
            || gun.isEjectCasings() && gun.getAmmoTypes().stream().anyMatch(ammo -> ammo instanceof BulletType bullet && (bullet.getWeaponType() == EnumWeaponType.GUN || bullet.getWeaponType() == EnumWeaponType.SHELL));
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
