package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelDefaultFlash;
import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.PilotGun;
import com.flansmodultimate.common.driveables.SeatInfo;
import com.flansmodultimate.common.driveables.ShootPoint;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.hooks.ClientHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Client-only, short-lived shot visuals. Positions and aim are read again at render time. */
public final class DriveableMuzzleFlashes
{
    private static final ModelFlash DEFAULT_MODEL = new ModelDefaultFlash();
    private static final Map<Driveable, List<Flash>> ACTIVE = new WeakHashMap<>();
    private static final int FLASH_TICKS = 2;

    private record Flash(boolean secondary, int point, int seat, int barrel, int expiresAt) {}

    private DriveableMuzzleFlashes() {}

    public static void bankFired(Driveable driveable, boolean secondary, int[] points, int[] barrels)
    {
        DriveableType type = driveable.getConfigType();
        if (type == null || !type.isDefaultMuzzleFlash() || !hasMuzzleFlash(type.weaponType(secondary)))
            return;
        List<ShootPoint> shootPoints = type.shootPoints(secondary);
        for (int i = 0; i < points.length; i++)
        {
            int pointIndex = points[i];
            if (pointIndex < 0 || pointIndex >= shootPoints.size())
                continue;
            ShootPoint point = shootPoints.get(pointIndex);
            int barrel = i < barrels.length ? barrels[i] : 0;
            if (barrel < 0 || barrel >= point.getBarrelCount())
                continue;
            add(driveable, new Flash(secondary, pointIndex, -1, barrel, driveable.tickCount + FLASH_TICKS));
            GunType gun = point.getRootPos() instanceof PilotGun pilot ? pilot.getType() : null;
            spawnGunParticle(gun, driveable.getDebugShootOrigin(point, barrel),
                driveable.getDebugShootDirection(point, secondary));
        }
    }

    public static void passengerFired(Driveable driveable, int seat, int barrel)
    {
        DriveableType type = driveable.getConfigType();
        SeatInfo info = type == null ? null : type.getSeat(seat);
        if (type == null || !type.isDefaultMuzzleFlash() || info == null || info.getGunType() == null
            || barrel < 0 || barrel >= info.getGunBarrelCount())
            return;
        Vec3 origin = driveable.getPassengerShootOrigin(seat, barrel);
        Vec3 direction = driveable.getPassengerShootDirection(seat);
        if (origin == null || direction == null)
            return;
        add(driveable, new Flash(false, -1, seat, barrel, driveable.tickCount + FLASH_TICKS));
        spawnGunParticle(info.getGunType(), origin, direction);
    }

    private static boolean hasMuzzleFlash(EnumWeaponType weapon)
    {
        return weapon == EnumWeaponType.GUN || weapon == EnumWeaponType.SHELL;
    }

    private static void add(Driveable driveable, Flash flash)
    {
        List<Flash> flashes = ACTIVE.computeIfAbsent(driveable, ignored -> new ArrayList<>());
        flashes.removeIf(old -> old.expiresAt < driveable.tickCount
            || old.seat == flash.seat && old.point == flash.point && old.secondary == flash.secondary
            && old.barrel == flash.barrel);
        flashes.add(flash);
    }

    private static void spawnGunParticle(@Nullable GunType gun, Vec3 origin, Vec3 direction)
    {
        if (gun == null || !gun.shouldShowMuzzleFlashParticles()
            || StringUtils.isBlank(gun.getMuzzleFlashParticle()))
            return;
        Vec3 velocity = direction.scale(0.02D);
        ClientHooks.RENDER.spawnParticle(gun.getMuzzleFlashParticle(), origin.x, origin.y, origin.z,
            velocity.x, velocity.y, velocity.z, gun.getMuzzleFlashParticleSize());
    }

    public static void render(Driveable driveable, PoseStack poseStack, MultiBufferSource buffer)
    {
        List<Flash> flashes = ACTIVE.get(driveable);
        DriveableType type = driveable.getConfigType();
        if (flashes == null || type == null)
            return;
        flashes.removeIf(flash -> flash.expiresAt < driveable.tickCount);
        for (Flash flash : flashes)
        {
            Vec3 origin;
            Vec3 direction;
            GunType gun;
            if (flash.seat >= 0)
            {
                SeatInfo info = type.getSeat(flash.seat);
                if (info == null || info.getGunType() == null || flash.barrel >= info.getGunBarrelCount())
                    continue;
                gun = info.getGunType();
                origin = driveable.getPassengerShootOrigin(flash.seat, flash.barrel);
                direction = driveable.getPassengerShootDirection(flash.seat);
            }
            else
            {
                List<ShootPoint> points = type.shootPoints(flash.secondary);
                if (flash.point < 0 || flash.point >= points.size())
                    continue;
                ShootPoint point = points.get(flash.point);
                if (flash.barrel >= point.getBarrelCount())
                    continue;
                gun = point.getRootPos() instanceof PilotGun pilot ? pilot.getType() : null;
                origin = driveable.getDebugShootOrigin(point, flash.barrel);
                direction = driveable.getDebugShootDirection(point, flash.secondary);
            }
            if (origin == null || direction == null || direction.lengthSqr() < 1.0E-8D)
                continue;
            poseStack.pushPose();
            poseStack.translate(origin.x - driveable.getX(), origin.y - driveable.getY(), origin.z - driveable.getZ());
            poseStack.mulPose(new Quaternionf().rotationTo(1F, 0F, 0F,
                (float)direction.x, (float)direction.y, (float)direction.z));
            if (gun != null)
                poseStack.scale(gun.getModelScale(), gun.getModelScale(), gun.getModelScale());
            if (gun == null || StringUtils.isNotBlank(gun.getFlashModelClassName())
                || StringUtils.isBlank(gun.getMuzzleFlashModelClassName()))
            {
                ModelFlash flashModel = gun != null && StringUtils.isNotBlank(gun.getFlashModelClassName())
                    ? ModelCache.getOrLoadFlashModel(gun) : DEFAULT_MODEL;
                ResourceLocation texture = gun != null && StringUtils.isNotBlank(gun.getFlashModelClassName())
                    ? gun.getFlashTexture() : FlansMod.TEXTURE_DEFAULTFLASH;
                if (flashModel != null && texture != null)
                {
                    if (gun != null && ModelCache.getOrLoadTypeModel(gun) instanceof ModelGun gunModel)
                        poseStack.scale(gunModel.getFlashScale(), gunModel.getFlashScale(), gunModel.getFlashScale());
                    flashModel.renderFlash(Math.max(0, Math.min(2, FLASH_TICKS - flash.expiresAt + driveable.tickCount)),
                        poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(texture)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, 1F);
                }
            }
            else
            {
                ModelMuzzleFlash model = ModelCache.getOrLoadMuzzleFlashModel(gun);
                if (model != null)
                    model.renderToBuffer(poseStack, buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(model.getTexture())),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            }
            poseStack.popPose();
        }
    }
}
