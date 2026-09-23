package com.flansmodultimate.common.driveables.physics;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Pure rules for a turning main rotor meeting terrain.
 *
 * <p>Blade tips travel at roughly two hundred metres a second whatever the
 * airframe is doing, so a rotor strike does not depend on how fast the
 * helicopter moves: a hovering or parked aircraft whose disc touches a hillside
 * or a tree is struck just as hard. Foliage and other soft blocks are chopped at
 * a small cost to the blades and to rotor speed, while soil, wood or stone met at
 * governed speed shatters the rotor outright. The energy in the blades goes with
 * the square of their speed, so a rotor still spooling up or winding down
 * survives what a governed one would not.
 */
public final class RotorStrikePhysics
{
    /** Below this spool fraction the blades are coasting too slowly to strike anything. */
    public static final float MIN_STRIKING_ROTOR_SPEED = 0.15F;
    /** Foliage, snow and similar soft blocks are chopped rather than breaking the blades. */
    public static final float MAX_CHOPPABLE_HARDNESS = 0.2F;
    /** Lets a disc resting flush against a block face still register contact. */
    public static final double CONTACT_TOLERANCE = 0.02D;
    /** Thinnest disc considered, so a zero-height blade box still has a swept volume. */
    public static final double MIN_HALF_THICKNESS = 0.0625D;
    /** Soil already breaks a rotor turning at full governed speed. */
    private static final float BLADE_BREAKING_HARDNESS = 0.5F;
    /** Stands in for bedrock, barriers and other unbreakable blocks. */
    private static final float UNBREAKABLE_HARDNESS = 50F;
    /** Share of the rotor's health one chopped soft block costs at full speed. */
    private static final float CHOP_HEALTH_FRACTION = 0.01F;
    /** Rotor speed lost per share of rotor health lost in the same strike. */
    private static final float SPEED_LOSS_PER_HEALTH_FRACTION = 3F;
    /** Attitude rate, in degrees per tick, a strike that destroys the rotor throws the airframe into. */
    private static final float MAX_ATTITUDE_KICK = 12F;
    /** Yaw rate, in degrees per tick, the rotor's angular momentum dumps into the fuselage. */
    private static final float MAX_YAW_KICK = 8F;

    private RotorStrikePhysics() {}

    /**
     * @param hardness   the struck block's destroy speed; unbreakable blocks report a negative value
     * @param rotorSpeed the rotor's spool fraction, one at governed speed
     * @return the share of the rotor's maximum health one struck block costs
     */
    public static float bladeDamageFraction(float hardness, float rotorSpeed)
    {
        float speed = Float.isFinite(rotorSpeed) ? Mth.clamp(rotorSpeed, 0F, 1F) : 0F;
        if (!Float.isFinite(hardness) || speed < MIN_STRIKING_ROTOR_SPEED)
            return 0F;
        float energy = speed * speed;
        if (hardness >= 0F && hardness <= MAX_CHOPPABLE_HARDNESS)
            return CHOP_HEALTH_FRACTION * energy;
        float effective = hardness < 0F ? UNBREAKABLE_HARDNESS : hardness;
        return Math.min(1F, energy * effective / BLADE_BREAKING_HARDNESS);
    }

    /** Whether the blades cut through the block rather than being stopped by it. */
    public static boolean chops(float hardness)
    {
        return Float.isFinite(hardness) && hardness >= 0F && hardness <= MAX_CHOPPABLE_HARDNESS;
    }

    /** Rotor speed left after a strike cost the blades the given share of their health. */
    public static float rotorSpeedAfterStrike(float rotorSpeed, float healthFraction)
    {
        float speed = Float.isFinite(rotorSpeed) ? Mth.clamp(rotorSpeed, 0F, 1F) : 0F;
        float loss = Float.isFinite(healthFraction)
            ? Mth.clamp(healthFraction * SPEED_LOSS_PER_HEALTH_FRACTION, 0F, 1F) : 1F;
        return speed * (1F - loss);
    }

    /** Pitch and roll rate the airframe is thrown into, tilting it toward the struck side of the disc. */
    public static float attitudeKick(float healthFraction)
    {
        return MAX_ATTITUDE_KICK * severity(healthFraction);
    }

    /** Yaw rate the stopped blades hand to the fuselage. */
    public static float yawKick(float healthFraction)
    {
        return MAX_YAW_KICK * severity(healthFraction);
    }

    private static float severity(float healthFraction)
    {
        return Float.isFinite(healthFraction) ? Mth.clamp(healthFraction, 0F, 1F) : 0F;
    }

    /** World-aligned bounds of a rotor disc, for gathering candidate blocks. */
    public static AABB discBounds(Vec3 hub, Vec3 axis, double radius, double halfThickness)
    {
        Vec3 normal = axis.normalize();
        double ex = radius * Math.sqrt(Math.max(0D, 1D - normal.x * normal.x)) + halfThickness * Math.abs(normal.x);
        double ey = radius * Math.sqrt(Math.max(0D, 1D - normal.y * normal.y)) + halfThickness * Math.abs(normal.y);
        double ez = radius * Math.sqrt(Math.max(0D, 1D - normal.z * normal.z)) + halfThickness * Math.abs(normal.z);
        return new AABB(hub.x - ex, hub.y - ey, hub.z - ez, hub.x + ex, hub.y + ey, hub.z + ez)
            .inflate(CONTACT_TOLERANCE);
    }

    /**
     * Whether a block's collision box reaches into the rotor disc: a flat
     * cylinder about {@code axis} through {@code hub}.
     *
     * <p>The box is projected onto the disc normal and onto the radial direction
     * toward its centre, which is exact for the flat faces of the disc and at
     * most a fraction of a block generous across the corners of a block.
     */
    public static boolean intersectsDisc(Vec3 hub, Vec3 axis, double radius, double halfThickness, AABB box)
    {
        Vec3 normal = axis.normalize();
        Vec3 offset = box.getCenter().subtract(hub);
        double halfX = box.getXsize() * 0.5D;
        double halfY = box.getYsize() * 0.5D;
        double halfZ = box.getZsize() * 0.5D;
        double along = offset.dot(normal);
        double normalReach = halfX * Math.abs(normal.x) + halfY * Math.abs(normal.y) + halfZ * Math.abs(normal.z);
        if (Math.abs(along) > halfThickness + normalReach + CONTACT_TOLERANCE)
            return false;
        Vec3 radial = offset.subtract(normal.scale(along));
        double distance = radial.length();
        if (distance < 1.0E-9D)
            return true;
        Vec3 direction = radial.scale(1D / distance);
        double radialReach = halfX * Math.abs(direction.x) + halfY * Math.abs(direction.y)
            + halfZ * Math.abs(direction.z);
        return distance - radialReach <= radius + CONTACT_TOLERANCE;
    }
}
