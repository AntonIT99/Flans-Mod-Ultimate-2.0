package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.equipment.WeaponBallistics;
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
        assertEquals(0D, WeaponAim.launch(displacement, movingEast(), SHELL, false, false, false, 1F).subtract(new Vec3(0D, 0D, 1D)).length(), 1.0E-6D);
    }

    @Test
    void aShellIsLaunchedAboveItsTargetToAllowForItsDrop()
    {
        Vec3 direction = WeaponAim.launch(new Vec3(0D, 0D, 60D), new TargetMotion(), SHELL, false, false, true, 1F);
        assertTrue(direction.y > 0.01D, "launched " + direction);
        assertEquals(0D, direction.x, 1.0E-6D);
    }

    @Test
    void aMovingTargetIsLedTheWayItMoves()
    {
        Vec3 direction = WeaponAim.launch(new Vec3(0D, 0D, 60D), movingEast(), SHELL, true, false, false, 1F);
        assertTrue(direction.x > 0.05D, "launched " + direction);
        assertEquals(direction.x, WeaponAim.launch(new Vec3(0D, 0D, 60D), movingEast(), SHELL, true, true, false, 1F).x, 0.02D);
    }

    @Test
    void aDroppedBombKeepsTheVelocityItLeftWithAndFallsByItsGravity()
    {
        // After 39 force-and-move steps it is at y=1, falling at 1.95 blocks/tick. The next trace hits the ground.
        WeaponAim.Fall fall = WeaponAim.fall(new Vec3(0D, 40D, 0D), new Vec3(1D, 0D, 0D), 0.05D, 1D, 0D);
        assertTrue(fall.landed());
        assertEquals(39D + 1D / 1.95D, fall.ticks(), 1.0E-6D);
        assertEquals(fall.ticks(), fall.impact().x, 1.0E-6D);
        assertEquals(0D, fall.impact().y, 1.0E-6D);
        // Drag shortens how far it carries.
        assertTrue(WeaponAim.impact(new Vec3(0D, 40D, 0D), new Vec3(1D, 0D, 0D), 0.05D, 0.98D, 0D).x < fall.impact().x);
    }

    @Test
    void collisionUsesIncomingVelocityButMovementUsesDragAndGravity()
    {
        // Two actual Bullet moves: (1, 8.5, -2), then (1.5, 6.75, -3).
        // Its next incoming trace is (0.5, -1.75, -1), meeting y=6 at 3/7 of that trace.
        WeaponAim.Fall fall = WeaponAim.fall(new Vec3(0D, 10D, 0D), new Vec3(2D, -1D, -4D), 1D, 0.5D, 6D);
        assertTrue(fall.landed());
        assertEquals(2D + 3D / 7D, fall.ticks(), 1.0E-9D);
        assertEquals(1.5D + 0.5D * 3D / 7D, fall.impact().x, 1.0E-9D);
        assertEquals(6D, fall.impact().y, 1.0E-9D);
        assertEquals(-3D - 3D / 7D, fall.impact().z, 1.0E-9D);
    }

    @Test
    void aBombAlreadyDescendingCanHitDuringItsFirstTrace()
    {
        WeaponAim.Fall fall = WeaponAim.fall(new Vec3(10D, 2D, 20D), new Vec3(8D, -4D, -6D), 1D, 0.5D, 0D);
        assertTrue(fall.landed());
        assertEquals(0.5D, fall.ticks());
        assertEquals(new Vec3(14D, 0D, 17D), fall.impact());
    }

    @Test
    void aClimbingCarrierDelaysImpactUntilTheBombFallsBackDown()
    {
        // With gravity=1 and no drag, y runs 4, 6, 7, 7, 6, 4, 1; the next trace hits one third of the way through.
        WeaponAim.Fall fall = WeaponAim.fall(new Vec3(0D, 4D, 0D), new Vec3(2D, 3D, 0D), 1D, 1D, 0D);
        assertTrue(fall.landed());
        assertEquals(6D + 1D / 3D, fall.ticks(), 1.0E-9D);
        assertEquals(2D * fall.ticks(), fall.impact().x, 1.0E-9D);
    }

    @Test
    void aPredictionThatNeverReachesTheGroundIsNotAnImpact()
    {
        WeaponAim.Fall fall = WeaponAim.fall(new Vec3(0D, 40D, 0D), new Vec3(1D, 0D, 0D), 0D, 1D, 0D);
        assertFalse(fall.landed());
        assertEquals(400D, fall.ticks());
    }

    @Test
    void bombLeadingRetainsOnlyAQuarterOfTheDifficultyPenalty()
    {
        assertEquals(1F, WeaponAim.bombLeadShare(1F));
        assertEquals(0.925F, WeaponAim.bombLeadShare(0.7F), 1.0E-6F);
        assertEquals(0.875F, WeaponAim.bombLeadShare(0.5F));
        assertEquals(0.825F, WeaponAim.bombLeadShare(0.3F), 1.0E-6F);
        assertEquals(0.75F, WeaponAim.bombLeadShare(0F));
    }
}
