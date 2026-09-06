package com.flansmodultimate.hooks.server;

import com.flansmodultimate.common.KillMessageData;
import com.flansmodultimate.hooks.IClientRenderHooks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ClientRenderHooksNoop implements IClientRenderHooks
{
    @Override
    public void initCustomBewlr(Consumer<IClientItemExtensions> consumer)
    {
        /* no-op */
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
    public void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale, int lifetime)
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
