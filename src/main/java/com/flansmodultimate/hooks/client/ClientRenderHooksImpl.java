package com.flansmodultimate.hooks.client;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.client.render.InstantBulletRenderer;
import com.flansmodultimate.client.render.InstantShotTrail;
import com.flansmodultimate.client.render.item.CustomBewlr;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.hooks.IClientRenderHooks;
import com.flansmodultimate.util.FileUtils;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

public final class ClientRenderHooksImpl implements IClientRenderHooks
{
    @Override
    public void initCustomBewlr(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                Minecraft mc = Minecraft.getInstance();
                return new CustomBewlr(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        });
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, float scale)
    {
        ParticleHelper.spawnFromString(s, x, y, z, 0, 0, 0, scale);
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        ParticleHelper.spawnFromString(s, x, y, z, vx, vy, vz, scale);
    }

    @Override
    public void spawnMuzzleFlashParticle(UUID playerUUID, InteractionHand hand, String particleType, float scale, boolean showToShooter)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        Player shooter = mc.level.getPlayerByUUID(playerUUID);
        if (shooter == null)
            return;

        boolean localShooter = shooter == mc.player;
        if (localShooter)
        {
            boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
            ItemStack localStack = shooter.getItemInHand(hand);
            if (!(localStack.getItem() instanceof GunItem localGunItem))
                return;
            if (!showToShooter || (firstPerson && !localGunItem.getConfigType().isShowMuzzleFlashParticlesFirstPerson()))
                return;
        }

        ItemStack gunStack = shooter.getItemInHand(hand);
        if (!(gunStack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();
        Vector3f shoulderOffset = new Vector3f(0F, localShooter ? -22F / 16F : 0F, 0F);
        Vector3f.add(shoulderOffset, gunType.getMuzzleFlashParticlesShoulderOffset(), shoulderOffset);

        Vector3f handOffset = getMuzzleFlashHandOffset(gunType, gunStack);
        if (localShooter)
            Vector3f.add(handOffset, new Vector3f(-0.7F, -0.35F, 0.1F), handOffset);
        Vector3f.add(handOffset, gunType.getMuzzleFlashParticlesHandOffset(), handOffset);

        Vector3f offset = Vector3f.add(shoulderOffset, handOffset, null);
        Vec3 pos = toWorldOffset(shooter, offset);
        Vec3 velocity = new Vec3((mc.level.random.nextFloat() * 2F - 1F) * 0.05F, (mc.level.random.nextFloat() * 2F - 1F) * 0.05F, (mc.level.random.nextFloat() * 2F - 1F) * 0.05F);
        ParticleHelper.spawnFromString(particleType, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z, scale);
    }

    private static Vector3f getMuzzleFlashHandOffset(GunType gunType, ItemStack gunStack)
    {
        Vector3f muzzlePoint = new Vector3f(0.5F, 0.22F, 0F);
        if (ModelCache.getOrLoadTypeModel(gunType) instanceof ModelGun model)
        {
            if (model.getMuzzleFlashPoint() != null && !model.getMuzzleFlashPoint().equals(ModelGun.getInvalid()))
                muzzlePoint = new Vector3f(model.getMuzzleFlashPoint().x, model.getMuzzleFlashPoint().y, model.getMuzzleFlashPoint().z);
            else if (model.getBarrelAttachPoint() != null)
                muzzlePoint = new Vector3f(model.getBarrelAttachPoint().x, model.getBarrelAttachPoint().y, model.getBarrelAttachPoint().z);

            AttachmentType barrelAttachment = gunType.getBarrel(gunStack);
            if (barrelAttachment != null && ModelCache.getOrLoadTypeModel(barrelAttachment) instanceof ModelAttachment barrelModel)
            {
                muzzlePoint = barrelModel.getMuzzleFlashPoint(muzzlePoint, model.getBarrelAttachPoint());
            }
            else if (model.getDefaultBarrelFlashPoint() != null)
            {
                muzzlePoint = Vector3f.add(muzzlePoint, model.getDefaultBarrelFlashPoint(), null);
            }
        }
        return muzzlePoint.scale(gunType.getModelScale());
    }

    private static Vec3 toWorldOffset(Player player, Vector3f offset)
    {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 worldUp = new Vec3(0D, 1D, 0D);
        Vec3 right = worldUp.cross(forward);
        if (right.lengthSqr() < 1.0E-6D)
            right = Vec3.directionFromRotation(0F, player.getYRot() + 90F);
        else
            right = right.normalize();
        Vec3 up = forward.cross(right).normalize();

        return player.getEyePosition()
            .add(right.scale(offset.x))
            .add(up.scale(offset.y))
            .add(forward.scale(offset.z));
    }

    @Override
    public boolean isDebugMode()
    {
        return ModClient.isDebug();
    }

    @Override
    public void setDebugMode(boolean value)
    {
        ModClient.setDebug(value);
    }

    public void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime, float red, float green, float blue)
    {
        DebugHelper.spawnDebugVector(start, end, lifeTime, red, green, blue);
    }

    public void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime)
    {
        DebugHelper.spawnDebugVector(start, end, lifeTime, 1F, 1F, 1F);
    }

    public void spawnDebugDot(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        DebugHelper.spawnDebugDot(position, lifeTime, red, green, blue);
    }

    public void spawnDebugDot(Vec3 position, int lifeTime)
    {
        DebugHelper.spawnDebugDot(position, lifeTime, 1F, 1F, 1F);
    }

    @Override
    public boolean hasFancyGraphics()
    {
        return ModClient.hasFancyGraphics();
    }

    @Override
    public void spawnTrail(String trailTexture, Vec3 origin, Vec3 hitPos, float width, float length, float bulletSpeed)
    {
        ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/skins/" + trailTexture + FileUtils.PNG_EXTENSION);
        InstantBulletRenderer.addTrail(new InstantShotTrail(origin, hitPos, width, length, bulletSpeed, resLoc));
    }

    @Override
    public void updateHitMarker(int time, float penAmount, boolean headshot, boolean explosionHit)
    {
        ModClient.setHitMarkerTime(time);
        ModClient.setHitMarkerPenAmount(penAmount);
        ModClient.setHitMarkerHeadshot(headshot);
        ModClient.setHitMarkerExplosion(explosionHit);
    }

    @Override
    public void updateFlash(boolean value, int time)
    {
        ModClient.setInFlash(true);
        ModClient.setFlashTime(time);
    }
}
