package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure movement calculations shared by server mecha physics and client animation. */
public final class MechaPhysics
{
    /** Legacy mechas converted their configured blocks-per-second speed to blocks per tick with this factor. */
    public static final double LEGACY_SPEED_PER_TICK = 4.3D / 20D;
    /** Mounted mecha cameras are defined ninety degrees clockwise from the legacy torso axes. */
    public static final float DRIVER_YAW_OFFSET = 90F;
    /** Share of the combined mining speed added to block progress each tick in 1.7.10. */
    private static final float LEGACY_MINING_RATE = 0.1F;
    /** 1.7.10's speed for blocks of about zero hardness. */
    private static final float LEGACY_INSTANT_MINE_SPEED = 9001F;

    private MechaPhysics() {}

    public static float driverMovementYaw(float torsoYaw)
    {
        return Mth.wrapDegrees(torsoYaw + DRIVER_YAW_OFFSET);
    }

    /** Converts an idempotent world-space torso target back to seat-relative aim. */
    public static float relativeAimYaw(float torsoYaw, float targetTorsoYaw)
    {
        if (!Float.isFinite(torsoYaw) || !Float.isFinite(targetTorsoYaw))
            return 0F;
        return Mth.wrapDegrees(targetTorsoYaw - torsoYaw);
    }

    /**
     * Converts normalized forward / strafe input into a horizontal world-space direction.
     * Mecha movement follows the torso aim just as the 1.7.10 torso axes followed the driver view.
     */
    public static Vec3 movementIntent(float yawDegrees, float forwardInput, float strafeInput)
    {
        if (!Float.isFinite(yawDegrees) || !Float.isFinite(forwardInput) || !Float.isFinite(strafeInput))
            return Vec3.ZERO;

        double yaw = yawDegrees * Mth.DEG_TO_RAD;
        double sine = Math.sin(yaw);
        double cosine = Math.cos(yaw);
        // Minecraft yaw zero faces +Z. Its right vector is (-cos(yaw), 0, -sin(yaw)).
        Vec3 intent = new Vec3(-sine * forwardInput - cosine * strafeInput, 0D,
            cosine * forwardInput - sine * strafeInput);
        return intent.lengthSqr() > 1D ? intent.normalize() : intent;
    }

    /**
     * Resolves the chassis speed to blocks per tick. A positive {@code RealMaxSpeedKmh}
     * is authoritative; legacy definitions fall back to the historical
     * {@code 4.3 * MoveSpeed} blocks-per-second conversion. Engine and addon
     * multipliers remain gameplay modifiers on both paths.
     */
    public static double movementSpeed(float legacyConfiguredSpeed, @Nullable Float realMaxSpeedKmh,
                                       float engineSpeed, float addonMultiplier)
    {
        if (!Float.isFinite(legacyConfiguredSpeed) || !Float.isFinite(engineSpeed) || !Float.isFinite(addonMultiplier))
            return 0D;
        double chassisSpeed = VehiclePhysicsUnits.isUsablePositive(realMaxSpeedKmh)
            ? VehiclePhysicsUnits.kmhToBlocksPerTick(realMaxSpeedKmh)
            : Math.max(0D, legacyConfiguredSpeed) * LEGACY_SPEED_PER_TICK;
        return chassisSpeed * Math.max(0D, engineSpeed) * Math.max(0D, addonMultiplier);
    }

    /** Returns the legacy +X-forward mecha-model yaw that faces the supplied world direction. */
    public static float movementYaw(Vec3 intent, float fallbackYaw)
    {
        if (intent == null || !Double.isFinite(intent.x) || !Double.isFinite(intent.z)
            || intent.x * intent.x + intent.z * intent.z < 1.0E-8D)
            return Mth.wrapDegrees(Float.isFinite(fallbackYaw) ? fallbackYaw : 0F);
        float minecraftYaw = (float) Math.toDegrees(Math.atan2(-intent.x, intent.z));
        return Mth.wrapDegrees(minecraftYaw - DRIVER_YAW_OFFSET);
    }

    /** Legacy RotateSpeed is an angular limit in degrees per tick, not a scaled steering input. */
    public static float approachYaw(float currentYaw, float targetYaw, float rotateSpeed)
    {
        if (!Float.isFinite(currentYaw) || !Float.isFinite(targetYaw))
            return 0F;
        float step = Float.isFinite(rotateSpeed) ? Math.max(0F, rotateSpeed) : 0F;
        float difference = Mth.wrapDegrees(targetYaw - currentYaw);
        return Mth.wrapDegrees(currentYaw + Mth.clamp(difference, -step, step));
    }

    /** Mechas have no reverse gear; throttle represents walking effort in every direction. */
    public static float throttle(float forwardInput, float strafeInput)
    {
        if (!Float.isFinite(forwardInput) || !Float.isFinite(strafeInput))
            return 0F;
        return Mth.clamp(Mth.sqrt(forwardInput * forwardInput + strafeInput * strafeInput), 0F, 1F);
    }

    /**
     * 1.7.10 mecha digging progress for one tick, where 1 breaks the block. The rate
     * starts at 1, is multiplied by the Speed of every effective tool and divided by
     * the block hardness, and a tenth of it is added per tick. Blocks of about zero
     * hardness break at once and unbreakable blocks never do.
     */
    public static float miningProgressPerTick(float hardness, Iterable<Float> effectiveToolSpeeds)
    {
        if (!Float.isFinite(hardness) || hardness < -0.01F)
            return 0F;
        if (Math.abs(hardness) < 0.01F)
            return LEGACY_MINING_RATE * LEGACY_INSTANT_MINE_SPEED;
        float speed = 1F;
        for (Float toolSpeed : effectiveToolSpeeds)
        {
            if (toolSpeed != null && Float.isFinite(toolSpeed))
                speed *= toolSpeed;
        }
        return Math.max(0F, LEGACY_MINING_RATE * speed / hardness);
    }
}
