package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.WeaponBallistics;
import com.wolffsmod.npcs.sim.TargetMotion;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class WeaponAimTest
{
    private static final WeaponBallistics SHELL = new WeaponBallistics(3D, 0.05D, 1D, false);

    private static TargetMotion movingEast()
    {
        TargetMotion motion = new TargetMotion();
        for (int tick = 0; tick < 20; tick++)
            motion.observe(new Vec3(tick * 0.3D, 64D, 60D), tick);
        return motion;
    }

    @Test
    void withoutLeadOrDropAWeaponAimsStraightAtItsTarget()
    {
        Vec3 displacement = new Vec3(0D, 0D, 60D);
        assertEquals(0D, WeaponAim.launch(displacement, movingEast(), SHELL, false, false, false).subtract(new Vec3(0D, 0D, 1D)).length(), 1.0E-6D);
    }

    @Test
    void aShellIsLaunchedAboveItsTargetToAllowForItsDrop()
    {
        Vec3 direction = WeaponAim.launch(new Vec3(0D, 0D, 60D), new TargetMotion(), SHELL, false, false, true);
        assertTrue(direction.y > 0.01D, "launched " + direction);
        assertEquals(0D, direction.x, 1.0E-6D);
    }

    @Test
    void aMovingTargetIsLedTheWayItMoves()
    {
        Vec3 direction = WeaponAim.launch(new Vec3(0D, 0D, 60D), movingEast(), SHELL, true, false, false);
        assertTrue(direction.x > 0.05D, "launched " + direction);
        assertEquals(direction.x, WeaponAim.launch(new Vec3(0D, 0D, 60D), movingEast(), SHELL, true, true, false).x, 0.02D);
    }

    @Test
    void aDroppedBombKeepsTheVelocityItLeftWithAndFallsByItsGravity()
    {
        // From 40 blocks up it falls 0.05 more each tick: 41 ticks bring it down, 41 blocks on at a block a tick.
        Vec3 impact = WeaponAim.impact(new Vec3(0D, 40D, 0D), new Vec3(1D, 0D, 0D), 0.05D, 1D, 0D);
        assertEquals(41D, impact.x, 1.0E-6D);
        assertTrue(impact.y <= 0D);
        // Drag shortens how far it carries.
        assertTrue(WeaponAim.impact(new Vec3(0D, 40D, 0D), new Vec3(1D, 0D, 0D), 0.05D, 0.98D, 0D).x < 41D);
    }
}
