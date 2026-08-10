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
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.hooks.IClientRenderHooks;
import com.flansmodultimate.util.FileUtils;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
public final class ClientRenderHooksImpl implements IClientRenderHooks
{
    @Override
    public IClientItemExtensions customItemExtensions()
    {
        return new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                Minecraft mc = Minecraft.getInstance();
                return new CustomBewlr(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        };
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
    public void spawnParticle(String s, BlockState state, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        ParticleHelper.spawnFromString(s, state, sourcePos, x, y, z, vx, vy, vz, scale);
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
        boolean localFirstPerson = localShooter && mc.options.getCameraType() == CameraType.FIRST_PERSON;
        if (localShooter)
        {
            ItemStack localStack = shooter.getItemInHand(hand);
            if (!(localStack.getItem() instanceof GunItem localGunItem))
                return;
            if (!showToShooter || (localFirstPerson && !localGunItem.getConfigType().isShowMuzzleFlashParticlesFirstPerson()))
                return;
        }

        ItemStack gunStack = shooter.getItemInHand(hand);
        if (!(gunStack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();
        Vector3f shoulderOffset = new Vector3f();
        Vector3f.add(shoulderOffset, gunType.getMuzzleFlashParticlesShoulderOffset(), shoulderOffset);

        Vector3f handOffset = getMuzzleFlashHandOffset(gunType, gunStack);
        if (localFirstPerson)
            Vector3f.add(handOffset, new Vector3f(-0.7F, -0.35F, 0.1F), handOffset);
        Vector3f.add(handOffset, gunType.getMuzzleFlashParticlesHandOffset(), handOffset);

        Vec3 pos = getMuzzleFlashPosition(shooter, hand, shoulderOffset, handOffset);
        Vec3 velocity = getMuzzleFlashVelocity(shooter, mc);
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

    /**
     * Modern port of 1.7.10 PlayerItemPositionUtils.GetPlayerHandPosition.
     * Gun-model X is the barrel direction; it must be transformed by the arm
     * axes rather than interpreted as a camera-space sideways offset.
     */
    private static Vec3 getMuzzleFlashPosition(Player player, InteractionHand hand, Vector3f shoulderOffset, Vector3f handOffset)
    {
        boolean offHand = hand == InteractionHand.OFF_HAND;
        float side = offHand ? -1.0F : 1.0F;

        RotatedAxes bodyAxes = new RotatedAxes(player.yBodyRot + 90.0F, 0.0F, 0.0F);
        // The legacy shoulder joint was fixed at 22/16 blocks. Deriving it from
        // eye height keeps the same standing-player placement while following
        // crouching and entities whose dimensions have been changed.
        double shoulderHeight = player.getEyeHeight() - (1.62D - 22.0D / 16.0D);
        Vec3 pos = player.position()
            .add(toVec3(bodyAxes.getYAxis()).scale(shoulderHeight))
            .subtract(toVec3(bodyAxes.getZAxis()).scale(side * 6.0D / 16.0D));

        Vector3f adjustedShoulderOffset = new Vector3f(shoulderOffset.x, shoulderOffset.y, shoulderOffset.z * side);
        pos = pos.add(toVec3(bodyAxes.findLocalVectorGlobally(toJoml(adjustedShoulderOffset))));

        RotatedAxes armAxes = new RotatedAxes(
            player.getYHeadRot() + 90.0F - 8.0F * side,
            player.getXRot(),
            0.0F);
        pos = pos.add(toVec3(armAxes.getXAxis()).scale(10.0D / 16.0D));

        Vector3f adjustedHandOffset = new Vector3f(handOffset.x, handOffset.y, handOffset.z * side);
        return pos.add(toVec3(armAxes.findLocalVectorGlobally(toJoml(adjustedHandOffset))));
    }

    private static Vec3 getMuzzleFlashVelocity(Player player, Minecraft minecraft)
    {
        RotatedAxes axes = new RotatedAxes(player.getYHeadRot() + 90.0F, player.getXRot(), 0.0F);
        org.joml.Vector3f velocity = axes.getXAxis();
        velocity.add(
            minecraft.level.random.nextFloat() * 2.0F - 1.0F,
            minecraft.level.random.nextFloat() * 2.0F - 1.0F,
            minecraft.level.random.nextFloat() * 2.0F - 1.0F);
        velocity.mul(0.05F);
        return toVec3(velocity);
    }

    private static org.joml.Vector3f toJoml(Vector3f vector)
    {
        return new org.joml.Vector3f(vector.x, vector.y, vector.z);
    }

    private static Vec3 toVec3(org.joml.Vector3f vector)
    {
        return new Vec3(vector.x, vector.y, vector.z);
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
