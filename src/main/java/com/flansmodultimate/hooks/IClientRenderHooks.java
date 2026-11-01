package com.flansmodultimate.hooks;

import com.flansmodultimate.client.render.hud.KillMessageData;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.weapons.DerivedMuzzle;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.teams.PlayerClassSkin;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.platform.client.FlanItemExtensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public interface IClientRenderHooks
{
    FlanItemExtensions customItemExtensions();

    void spawnParticle(String s, double x, double y, double z, float scale);

    // Mirrors Level#addParticle: position and velocity, plus the effect options.
    @SuppressWarnings("java:S107")
    void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale);

    /**
     * As {@link #spawnParticle(String, double, double, double, double, double, double, float)}, but
     * multiplies the particle's authored lifetime by {@code lifetimeScale}. Below one the particle
     * clears early, which is what makes a small-calibre detonation read as brief; above one it
     * lingers. It changes only how long the particle lives, never how fast it animates.
     */
    // Mirrors Level#addParticle: position and velocity, plus the effect options.
    @SuppressWarnings("java:S107")
    void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale, float lifetimeScale);

    /**
     * Draws a glowing tracer beam between two points that fades over a few ticks, from the Labjac Edition's
     * {@code TracerBeam}. Colours and alpha are fractions from 0 to 1; the width is the core half-width in blocks.
     */
    void spawnTracerBeam(Vec3 start, Vec3 end, float red, float green, float blue, float alpha, float width);

    /**
     * Emits a burst of particles scattered around the given point and keeps replacing them as they
     * expire for {@code durationTicks}, so the effect lasts without slowing any particle's animation.
     */
    void spawnSustainedParticles(String particleType, double x, double y, double z, double spread, double drift, float scale, int burstSize, int durationTicks, float lifetimeScale);

    /**
     * As {@link #spawnSustainedParticles}, but the waves emitted during the first {@code hotTicks}
     * use {@code hotParticleType}, so an effect can burn as fire before it cools to smoke.
     */
    void spawnSustainedParticles(String hotParticleType, String particleType, int hotTicks, double x, double y, double z, double spread, double drift, float scale, int burstSize, int durationTicks,
        float lifetimeScale);

    /**
     * The staged layers of a detonation that play out over several ticks: the warm afterglow, the
     * dust skirt rolling out along the ground, the fireball stem climbing and darkening, and the
     * mushroom cap of the heaviest charges. Each layer is sized by {@code ExplosionVisuals}.
     */
    void spawnExplosionSpectacle(Vec3 position, float craterRadius, float blastRadius, boolean groundBurst, boolean fiery);

    /** A vehicle smoke-launcher shell, flown and burst entirely on the client. */
    void launchSmokeShell(double x, double y, double z, double vx, double vy, double vz, int fuseTicks);

    /** Updates a projectile's smoke cloud with its own bounded particle allowance. */
    void spawnSmokeCloud(int sourceId, Vec3 position, int count, String particleType, float radius);

    /** Gives off the smoke, fire, steam and sparks of the driveable's damage and, once it is a wreck, of its burning out. Client tick. */
    void tickDriveableDamageEffects(Driveable driveable);

    /** Shows parts being shot off the driveable together: a burst where each was, and each thrown clear with the parts it carries. */
    void onDriveablePartsDestroyed(Driveable driveable, List<EnumDriveablePart> parts);

    /** Plays the death of the driveable, as it turns into a wreck or just before it is removed. */
    void onDriveableWrecked(Driveable driveable);

    /** Shows a wreck blown apart by explosives, just before it is removed. */
    void onDriveableBrokenUp(Driveable driveable);

    /** Leaves a scorch mark on the ground below a point, as a fire or a crash does. */
    void spawnScorchMark(Vec3 position, float radius);

    // Mirrors Level#addParticle: position and velocity, plus the effect options.
    @SuppressWarnings("java:S107")
    void spawnParticle(String s, BlockState state, BlockPos sourcePos, double x, double y, double z, double vx, double vy, double vz, float scale);

    void spawnMuzzleFlashParticle(UUID playerUUID, InteractionHand hand, String particleType, float scale, boolean showToShooter);

    /**
     * Muzzle positions measured from the loaded model of {@code type}, in type-file
     * units and convention, for comparison against its authored shoot points.
     *
     * <p>
     * Model geometry only exists on the client, so a dedicated server returns an
     * empty list. In singleplayer the integrated server reaches the client's own
     * loaded models through this hook.
     * </p>
     */
    List<DerivedMuzzle> deriveMuzzles(DriveableType type);

    /**
     * The muzzle of every tube of the primary armament of {@code type}'s loaded
     * model, in type-file units and convention: one for a single gun, four for a
     * quad mount. Empty when no model is loaded, as on a dedicated server.
     */
    List<DerivedMuzzle> derivePrimaryBarrels(DriveableType type);

    /**
     * Where each barrel muzzle of an AA gun's loaded model sits with the gun at
     * rest, in blocks from the gun's position, in barrel order. Empty when no
     * model is loaded, as on a dedicated server.
     */
    List<Vec3> deriveAAGunBarrelOffsets(AAGunType type);

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

    /** Kicks the camera for a nearby driveable firing its main or coaxial gun. */
    void addVehicleScreenShake(float intensity, float durationSeconds);

    void addCombatFeedback(boolean explosion, float intensity, float direction);

    void playBulletFlyby(String sound, Vec3 position);

    void updatePlayerClassSkins(Map<UUID, PlayerClassSkin> playerClasses);

    void addKillMessage(KillMessageData message);
}
