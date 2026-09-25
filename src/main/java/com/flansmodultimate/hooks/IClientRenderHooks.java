package com.flansmodultimate.hooks;

import com.flansmodultimate.client.render.KillMessageData;
import com.flansmodultimate.common.driveables.DerivedMuzzle;
import com.flansmodultimate.common.types.DriveableType;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
public interface IClientRenderHooks
{
    IClientItemExtensions customItemExtensions();

    void spawnParticle(String s, double x, double y, double z, float scale);

    void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale);

    /**
     * As {@link #spawnParticle(String, double, double, double, double, double, double, float)}, but
     * multiplies the particle's authored lifetime by {@code lifetimeScale}. Below one the particle
     * clears early, which is what makes a small-calibre detonation read as brief; above one it
     * lingers. It changes only how long the particle lives, never how fast it animates.
     */
    void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale, float lifetimeScale);

    /**
     * Emits a burst of particles scattered around the given point and keeps replacing them as they
     * expire for {@code durationTicks}, so the effect lasts without slowing any particle's animation.
     */
    void spawnSustainedParticles(String particleType, double x, double y, double z, double spread, double drift, float scale, int burstSize, int durationTicks, float lifetimeScale);

    /** A vehicle smoke-launcher shell, flown and burst entirely on the client. */
    void launchSmokeShell(double x, double y, double z, double vx, double vy, double vz, int fuseTicks);

    void spawnParticle(String s, BlockState state, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale);

    void spawnMuzzleFlashParticle(UUID playerUUID, InteractionHand hand, String particleType, float scale, boolean showToShooter);

    /**
     * Muzzle positions measured from the loaded model of {@code type}, in type-file
     * units and convention, for comparison against its authored shoot points.
     *
     * <p>Model geometry only exists on the client, so a dedicated server returns an
     * empty list. In singleplayer the integrated server reaches the client's own
     * loaded models through this hook.</p>
     */
    List<DerivedMuzzle> deriveMuzzles(DriveableType type);

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

    void updatePlayerClassSkins(Map<UUID, String> playerClasses);

    void addKillMessage(KillMessageData message);
}
