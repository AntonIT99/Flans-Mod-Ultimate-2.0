package com.flansmodultimate.common.entity;

import net.minecraft.world.phys.Vec3;

/**
 * An entity heavy enough that outside pushes are weighed against its mass
 * rather than applied as they are, and that exchanges momentum with other such
 * entities on contact.
 */
public interface IMassiveEntity
{
    /** Mass in kilograms that pushes and collisions are weighed against. */
    double getImpulseMassKg();

    /** Applies a velocity change already weighed against this entity's mass, so it is not scaled a second time. */
    void applyResolvedImpulse(Vec3 impulse);
}
