package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;

import net.minecraft.world.phys.AABB;

/**
 * Immutable definition of a damageable driveable part's local collision box.
 * Type files express dimensions in model pixels, while this class stores blocks.
 */
@Getter
public final class CollisionBox
{
    private final float x;
    private final float y;
    private final float z;
    private final float width;
    private final float height;
    private final float depth;
    private final float health;
    private final float penetrationResistance;
    private final float crewDamageMultiplier;

    public CollisionBox(float health, float x, float y, float z, float width, float height, float depth)
    {
        this(health, x, y, z, width, height, depth, 5F, 0F, true);
    }

    public CollisionBox(float health, float x, float y, float z, float width, float height, float depth,
                        float penetrationResistance)
    {
        this(health, x, y, z, width, height, depth, penetrationResistance, 0F, true);
    }

    public CollisionBox(float health, float x, float y, float z, float width, float height, float depth,
                        float penetrationResistance, float crewDamageMultiplier)
    {
        this(health, x, y, z, width, height, depth, penetrationResistance, crewDamageMultiplier, true);
    }

    private CollisionBox(float health, float x, float y, float z, float width, float height, float depth,
                         float penetrationResistance, float crewDamageMultiplier, boolean modelUnits)
    {
        float scale = modelUnits ? 1F / 16F : 1F;
        this.health = Math.max(0F, health);
        this.x = x * scale;
        this.y = y * scale;
        this.z = z * scale;
        this.width = Math.max(0F, width * scale);
        this.height = Math.max(0F, height * scale);
        this.depth = Math.max(0F, depth * scale);
        this.penetrationResistance = Math.max(0F, penetrationResistance);
        this.crewDamageMultiplier = Math.max(0F, crewDamageMultiplier);
    }

    public static CollisionBox inWorldUnits(float health, float x, float y, float z, float width, float height,
                                            float depth, float penetrationResistance, float crewDamageMultiplier)
    {
        return new CollisionBox(health, x, y, z, width, height, depth, penetrationResistance, crewDamageMultiplier, false);
    }

    /** Legacy aliases retained for entity/model code. */
    public float getW() { return width; }
    public float getH() { return height; }
    public float getD() { return depth; }

    public Vector3f getCentre()
    {
        return new Vector3f(x + width * 0.5F, y + height * 0.5F, z + depth * 0.5F);
    }

    public Vector3f getRootPosition()
    {
        return new Vector3f(x, y, z);
    }

    public float getRadius()
    {
        return 0.5F * (float) Math.sqrt(width * width + height * height + depth * depth);
    }

    public AABB asAabb()
    {
        return new AABB(x, y, z, x + width, y + height, z + depth);
    }
}
