package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * Separates the velocity a heavy entity gave itself from everything else that
 * changed it since.
 *
 * <p>The entity settles the velocity its own simulation produced at the end of
 * its tick. Whatever differs by the start of the next one was added from
 * outside: an explosion of any origin, melee knockback, flowing water, or a
 * push from another mod. Weighing that difference against mass in one place
 * covers every source without hooking each of them.
 */
public final class ExternalImpulseTracker
{
    @Nullable
    private Vec3 baseline;

    /**
     * Returns the velocity the entity should carry once outside changes since
     * the last settle are weighed against its mass. The first call only primes
     * the baseline, so a velocity loaded from disk is taken as the entity's own.
     */
    public Vec3 absorb(Vec3 current, double massKg, double referenceMassKg)
    {
        if (baseline == null || !isFinite(baseline))
        {
            baseline = current;
            return current;
        }
        Vec3 external = current.subtract(baseline);
        if (external.lengthSqr() == 0D)
            return current;
        Vec3 absorbed = baseline.add(VehicleImpulsePhysics.scaleExternalImpulse(external, massKg, referenceMassKg));
        baseline = absorbed;
        return absorbed;
    }

    /** Records the velocity the entity's own simulation produced. */
    public void settle(Vec3 velocity)
    {
        baseline = velocity;
    }

    /** Accounts for a change that was already weighed against mass, such as a collision impulse, so it is not scaled again. */
    public void addResolvedImpulse(Vec3 impulse)
    {
        if (baseline != null)
            baseline = baseline.add(impulse);
    }

    private static boolean isFinite(Vec3 vector)
    {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
