package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileAimTest
{
    @Test
    void aFastRoundLeadsALateralMovingTarget()
    {
        Vec3 target = new Vec3(0D, 0D, 80D);
        Vec3 movement = new Vec3(0.2D, 0D, 0D);
        Vec3 aim = ProjectileAim.direction(target, movement, 8D, 0D, 1D, false);
        assertTrue(aim.x > 0D);
        assertIntercept(target, movement, aim.scale(8D), 0D, 1D, 0.01D);
    }

    @Test
    void grenadeArcsReachTheTargetUnderDiscreteGravityAndDrag()
    {
        Vec3 target = new Vec3(0D, -1D, 12D);
        Vec3 low = ProjectileAim.direction(target, Vec3.ZERO, 0.9D, 0.024525D, 0.99D, false);
        Vec3 high = ProjectileAim.direction(target, Vec3.ZERO, 0.9D, 0.024525D, 0.99D, true);
        assertTrue(high.y > low.y);
        assertIntercept(target, Vec3.ZERO, low.scale(0.9D), 0.024525D, 0.99D, 0.01D);
        assertIntercept(target, Vec3.ZERO, high.scale(0.9D), 0.024525D, 0.99D, 0.01D);
    }

    @Test
    void compensationAlsoWorksAgainstMovingTargetsWithNondefaultDimensionPhysics()
    {
        Vec3 target = new Vec3(3D, 2D, 35D);
        Vec3 movement = new Vec3(-0.08D, 0D, 0.04D);
        Vec3 aim = ProjectileAim.direction(target, movement, 3D, 0.04905D, Math.pow(0.99D, 2D), false);
        assertIntercept(target, movement, aim.scale(3D), 0.04905D, Math.pow(0.99D, 2D), 0.01D);
    }

    @Test
    void unreachableOrInvalidShotsKeepAFiniteDirectAim()
    {
        Vec3 target = new Vec3(0D, 0D, 1000D);
        assertEquals(target.normalize(), ProjectileAim.direction(target, Vec3.ZERO, 0.5D, 0.1D, 0.99D, false));
        assertEquals(target.normalize(), ProjectileAim.direction(target, Vec3.ZERO, Double.NaN, 0D, 1D, false));
        assertEquals(Vec3.ZERO, ProjectileAim.direction(Vec3.ZERO, Vec3.ZERO, 1D, 0D, 1D, false));
    }

    private static void assertIntercept(Vec3 target, Vec3 targetVelocity, Vec3 velocity, double gravity, double drag, double tolerance)
    {
        Vec3 position = Vec3.ZERO;
        double closest = Double.MAX_VALUE;
        for (int tick = 0; tick < 200; tick++)
        {
            Vec3 relative = position.subtract(target.add(targetVelocity.scale(tick)));
            Vec3 segment = velocity.subtract(targetVelocity);
            double fraction = Math.max(0D, Math.min(1D, -relative.dot(segment) / segment.lengthSqr()));
            closest = Math.min(closest, relative.add(segment.scale(fraction)).length());
            position = position.add(velocity);
            velocity = velocity.scale(drag).add(0D, -gravity, 0D);
        }
        assertTrue(closest < tolerance, "Closest approach was " + closest + " blocks");
    }
}
