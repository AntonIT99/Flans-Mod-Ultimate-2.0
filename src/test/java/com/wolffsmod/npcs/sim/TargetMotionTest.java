package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.ProjectileAim;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetMotionTest
{
    @Test
    void aNewTargetIsLedFromItsFirstTickOfMovement()
    {
        TargetMotion motion = new TargetMotion();
        motion.observe(new Vec3(0D, 64D, 0D), 100L);
        assertEquals(Vec3.ZERO, motion.velocity());
        motion.observe(new Vec3(0.5D, 64D, 0D), 101L);
        assertEquals(0.5D, motion.velocity().x, 1.0E-9D);
        assertEquals(Vec3.ZERO, motion.acceleration());
    }

    @Test
    void aSingleJerkIsSmoothedRatherThanFollowed()
    {
        TargetMotion motion = new TargetMotion();
        Vec3 position = Vec3.ZERO;
        for (long tick = 0; tick < 20; tick++)
        {
            motion.observe(position, tick);
            position = position.add(0.2D, 0D, 0D);
        }
        // One tick of a sudden sidestep.
        motion.observe(position.add(0D, 0D, 1D), 20L);
        assertEquals(0.2D, motion.velocity().x, 0.01D);
        assertTrue(motion.velocity().z < 0.5D, "the sidestep is taken in only by the blend share");
        assertTrue(motion.acceleration().length() <= TargetMotion.MAX_ACCELERATION + 1.0E-9D);
    }

    @Test
    void steadyAccelerationIsEstimatedCautiously()
    {
        TargetMotion motion = new TargetMotion();
        double position = 0D;
        double speed = 0D;
        for (long tick = 0; tick < 60; tick++)
        {
            motion.observe(new Vec3(position, 0D, 0D), tick);
            speed += 0.01D;
            position += speed;
        }
        assertTrue(motion.acceleration().x > 0.004D, "the acceleration is seen");
        assertTrue(motion.acceleration().x <= 0.0101D, "but never overestimated");
        // The lead adds more than plain velocity would, but only for the horizon the acceleration is trusted for.
        double plain = motion.velocity().x * 40D;
        double led = motion.lead(40D).x;
        assertTrue(led > plain);
        assertTrue(led < plain + 0.5D * 0.0101D * 40D * 40D);
    }

    @Test
    void teleportsGapsAndTimeRunningBackwardsStartOver()
    {
        TargetMotion motion = new TargetMotion();
        motion.observe(Vec3.ZERO, 0L);
        motion.observe(new Vec3(0.3D, 0D, 0D), 1L);
        motion.observe(new Vec3(500D, 0D, 0D), 2L);
        assertEquals(Vec3.ZERO, motion.velocity());
        motion.observe(new Vec3(500.3D, 0D, 0D), 3L);
        assertEquals(0.3D, motion.velocity().x, 1.0E-9D);
        motion.observe(new Vec3(501D, 0D, 0D), 3L + TargetMotion.MAX_GAP + 1L);
        assertEquals(Vec3.ZERO, motion.velocity());
        motion.observe(new Vec3(501.3D, 0D, 0D), 2L);
        assertEquals(Vec3.ZERO, motion.velocity());
    }

    @Test
    void theLeadIsCappedInTimeSidewaysAndVertically()
    {
        TargetMotion motion = new TargetMotion();
        motion.observe(Vec3.ZERO, 0L);
        motion.observe(new Vec3(3D, 2D, 4D), 1L);
        Vec3 lead = motion.lead(10_000D);
        assertEquals(TargetMotion.MAX_HORIZONTAL_LEAD, Math.sqrt(lead.x * lead.x + lead.z * lead.z), 1.0E-6D);
        assertEquals(TargetMotion.MAX_VERTICAL_LEAD, lead.y, 1.0E-9D);
        // The direction of travel is kept.
        assertEquals(3D / 4D, lead.x / lead.z, 1.0E-9D);
        assertEquals(Vec3.ZERO, motion.lead(0D));
        assertEquals(Vec3.ZERO, motion.lead(Double.NaN));
    }

    @Test
    void theSolverMeetsATargetThatKeepsAccelerating()
    {
        TargetMotion motion = new TargetMotion();
        double position = 0D;
        double speed = 0.1D;
        for (long tick = 0; tick < 60; tick++)
        {
            motion.observe(new Vec3(position, 0D, 60D), tick);
            speed += 0.004D;
            position += speed;
        }
        Vec3 displacement = new Vec3(position - speed, 0D, 60D);
        var tracked = ProjectileAim.solve(displacement, motion, 3D, 0D, 1D, false);
        var constant = ProjectileAim.solve(displacement, motion.velocity(), 3D, 0D, 1D, false);
        assertTrue(tracked.ticks() > 0D);
        // Where each solution puts the round when the target, still accelerating, gets there.
        double flight = tracked.ticks();
        double actual = displacement.x + speed * flight + 0.5D * 0.004D * flight * flight;
        double trackedMiss = Math.abs(tracked.direction().x * 3D * flight - actual);
        double constantMiss = Math.abs(constant.direction().x * 3D * constant.ticks() - (displacement.x + speed * constant.ticks() + 0.5D * 0.004D * constant.ticks() * constant.ticks()));
        assertTrue(trackedMiss < constantMiss, "tracking the motion misses by less: " + trackedMiss + " against " + constantMiss);
    }
}
