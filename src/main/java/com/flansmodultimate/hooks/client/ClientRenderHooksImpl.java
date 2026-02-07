package com.flansmodultimate.hooks.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.client.render.InstantBulletRenderer;
import com.flansmodultimate.client.render.InstantShotTrail;
import com.flansmodultimate.client.render.item.CustomBewlr;
import com.flansmodultimate.hooks.IClientRenderHooks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

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
        //TODO trails not visible when trail origin position and player camera position are too close. the can only be seen with an slight angle
        ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/skins/" + trailTexture + ".png");
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
