package com.flansmodultimate.common.guns;

import net.minecraft.world.phys.Vec3;

/** Bounded proportional navigation in blocks and ticks, retaining projectile speed. */
public final class ProportionalNavigation
{
    private ProportionalNavigation()
    {}

    public static Vec3 steer(Vec3 velocity, Vec3 relativePosition, Vec3 targetVelocity, double gain, double maxTurn)
    {
        double rangeSquared = relativePosition.lengthSqr();
        double speed = velocity.length();
        if (!Double.isFinite(rangeSquared) || !Double.isFinite(speed) || rangeSquared < 1E-12D || speed < 1E-6D
            || !Double.isFinite(gain) || !Double.isFinite(maxTurn) || gain <= 0D || maxTurn <= 0D)
            return velocity;
        Vec3 relativeVelocity = targetVelocity.subtract(velocity);
        double closingSpeed = -relativePosition.dot(relativeVelocity) / Math.sqrt(rangeSquared);
        if (!Double.isFinite(closingSpeed) || closingSpeed <= 0D)
            return velocity;
        Vec3 angularVelocity = relativePosition.cross(relativeVelocity).scale(1D / rangeSquared);
        Vec3 direction = velocity.scale(1D / speed);
        Vec3 acceleration = angularVelocity.cross(direction).scale(Math.min(10D, gain) * closingSpeed);
        double magnitude = acceleration.length();
        if (!Double.isFinite(magnitude) || magnitude < 1E-12D)
            return velocity;
        double turn = Math.min(Math.atan2(magnitude, speed), Math.min(Math.PI / 2D, maxTurn));
        return direction.scale(Math.cos(turn) * speed).add(acceleration.scale(Math.sin(turn) * speed / magnitude));
    }
}
