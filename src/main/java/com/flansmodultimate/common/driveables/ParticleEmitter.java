package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/** Data-only particle emitter; runtime decides how and when to render it. */
@Getter
public final class ParticleEmitter
{
    private final String particleType;
    private final int emitRate;
    private final Vector3f origin;
    private final Vector3f extents;
    private final Vector3f direction;
    private final float velocity;
    private final float minThrottle;
    private final float maxThrottle;
    private final float minHealth;
    private final float maxHealth;
    private final EnumDriveablePart part;

    public ParticleEmitter(String particleType, int emitRate, Vector3f origin, Vector3f extents, Vector3f velocity,
                           float minThrottle, float maxThrottle, float minHealth, float maxHealth,
                           EnumDriveablePart part)
    {
        this.particleType = StringUtils.defaultString(particleType);
        this.emitRate = Math.max(1, emitRate);
        this.origin = new Vector3f(origin.x, origin.y, origin.z);
        this.extents = new Vector3f(extents.x, extents.y, extents.z);
        this.velocity = velocity.length();
        this.direction = this.velocity > 0F
            ? new Vector3f(velocity.x / this.velocity, velocity.y / this.velocity, velocity.z / this.velocity)
            : new Vector3f();
        this.minThrottle = minThrottle;
        this.maxThrottle = maxThrottle;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
        this.part = part == null ? EnumDriveablePart.CORE : part;
    }

    public Vector3f getVelocityVector()
    {
        return new Vector3f(direction.x * velocity, direction.y * velocity, direction.z * velocity);
    }
}
