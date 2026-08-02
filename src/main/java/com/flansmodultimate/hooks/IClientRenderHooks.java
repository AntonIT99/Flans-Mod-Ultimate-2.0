package com.flansmodultimate.hooks;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

public interface IClientRenderHooks
{
    void initCustomBewlr(Consumer<IClientItemExtensions> consumer);

    void spawnParticle(String s, double x, double y, double z, float scale);

    void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale);

    void spawnParticle(String s, BlockState state, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale);

    void spawnMuzzleFlashParticle(UUID playerUUID, InteractionHand hand, String particleType, float scale, boolean showToShooter);

    boolean isDebugMode();

    void setDebugMode(boolean value);

    void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime, float red, float green, float blue);

    void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime);

    void spawnDebugDot(Vec3 position, int lifeTime, float red, float green, float blue);

    void spawnDebugDot(Vec3 position, int lifeTime);

    boolean hasFancyGraphics();

    void spawnTrail(String trailTexture, Vec3 origin, Vec3 hitPos, float width, float length, float bulletSpeed);

    void updateHitMarker(int time, float penAmount, boolean headshot, boolean explosionHit);

    void updateFlash(boolean value, int time);
}
