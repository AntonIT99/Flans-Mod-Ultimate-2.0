package com.flansmodultimate.hooks.server;

import com.flansmodultimate.client.render.KillMessageData;
import com.flansmodultimate.common.driveables.DerivedMuzzle;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.hooks.IClientRenderHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
public final class ClientRenderHooksNoop implements IClientRenderHooks
{
    @Override
    public IClientItemExtensions customItemExtensions()
    {
        return IClientItemExtensions.DEFAULT;
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, float scale)
    {
        /* no-op */
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        /* no-op */
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale, float lifetimeScale)
    {
        /* no-op */
    }

    @Override
    public void launchSmokeShell(double x, double y, double z, double vx, double vy, double vz, int fuseTicks)
    {
        /* no-op */
    }

    @Override
    public void spawnSustainedParticles(String particleType, double x, double y, double z, double spread, double drift, float scale, int burstSize, int durationTicks, float lifetimeScale)
    {
        /* no-op */
    }

    @Override
    public void spawnSustainedParticles(String hotParticleType, String particleType, int hotTicks, double x, double y, double z, double spread, double drift, float scale, int burstSize, int durationTicks, float lifetimeScale)
    {
        /* no-op */
    }

    @Override
    public void spawnExplosionSpectacle(Vec3 position, float craterRadius, float blastRadius, boolean groundBurst, boolean fiery)
    {
        /* no-op */
    }

    @Override
    public void spawnParticle(String s, BlockState state, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        /* no-op */
    }

    @Override
    public void spawnMuzzleFlashParticle(UUID playerUUID, InteractionHand hand, String particleType, float scale, boolean showToShooter)
    {
        /* no-op */
    }

    @Override
    public List<DerivedMuzzle> deriveMuzzles(DriveableType type)
    {
        return List.of();
    }

    @Override
    public boolean isDebugMode()
    {
        return false;
    }

    @Override
    public void setDebugMode(boolean value)
    {
        /* no-op */
    }

    @Override
    public void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime, float red, float green, float blue)
    {
        /* no-op */
    }

    @Override
    public void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime)
    {
        /* no-op */
    }

    @Override
    public void spawnDebugDot(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        /* no-op */
    }

    @Override
    public void spawnDebugDot(Vec3 position, int lifeTime)
    {
        /* no-op */
    }

    @Override
    public boolean hasFancyGraphics()
    {
        return false;
    }

    @Override
    public void spawnTrail(String trailTexture, Vec3 origin, Vec3 hitPos, float width, float length, float bulletSpeed)
    {
        /* no-op */
    }

    @Override
    public void updateHitMarker(int time, float penAmount, boolean headshot, boolean explosionHit)
    {
        /* no-op */
    }

    @Override
    public void updateFlash(boolean value, int time)
    {
        /* no-op */
    }

    @Override
    public void updatePlayerClassSkins(Map<UUID, String> playerClasses)
    {
        /* no-op */
    }

    @Override
    public void addKillMessage(KillMessageData message)
    {
        /* no-op */
    }
}
