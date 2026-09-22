package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class RotorStrikePhysicsTest
{
    private static final Vec3 HUB = new Vec3(0.5D, 10D, 0.5D);
    private static final Vec3 UP = new Vec3(0D, 1D, 0D);
    private static final double RADIUS = 5D;
    private static final double HALF_THICKNESS = 0.25D;

    private static AABB block(int x, int y, int z)
    {
        return new AABB(x, y, z, x + 1D, y + 1D, z + 1D);
    }

    @Test
    void governedRotorShattersOnSoilWoodAndStone()
    {
        assertEquals(1F, RotorStrikePhysics.bladeDamageFraction(0.5F, 1F));
        assertEquals(1F, RotorStrikePhysics.bladeDamageFraction(2F, 1F));
        assertEquals(1F, RotorStrikePhysics.bladeDamageFraction(1.5F, 1F));
        assertEquals(1F, RotorStrikePhysics.bladeDamageFraction(-1F, 1F));
    }

    @Test
    void foliageIsChoppedAtASmallCost()
    {
        float leaves = RotorStrikePhysics.bladeDamageFraction(0.2F, 1F);
        assertTrue(leaves > 0F && leaves < 0.05F);
        assertTrue(RotorStrikePhysics.chops(0.2F));
        assertTrue(RotorStrikePhysics.chops(0F));
        assertFalse(RotorStrikePhysics.chops(0.5F));
        assertFalse(RotorStrikePhysics.chops(-1F));
    }

    @Test
    void slowRotorSurvivesWhatAGovernedOneWouldNot()
    {
        float spooling = RotorStrikePhysics.bladeDamageFraction(0.5F, 0.5F);
        assertTrue(spooling > 0F && spooling < 1F);
        assertEquals(0F, RotorStrikePhysics.bladeDamageFraction(50F, 0.1F));
        assertEquals(0F, RotorStrikePhysics.bladeDamageFraction(Float.NaN, 1F));
    }

    @Test
    void strikeCostsRotorSpeedImmediately()
    {
        assertEquals(0F, RotorStrikePhysics.rotorSpeedAfterStrike(1F, 1F));
        float chopped = RotorStrikePhysics.rotorSpeedAfterStrike(1F, 0.01F);
        assertTrue(chopped < 1F && chopped > 0.9F);
        assertEquals(1F, RotorStrikePhysics.rotorSpeedAfterStrike(1F, 0F));
    }

    @Test
    void kicksGrowWithSeverity()
    {
        assertEquals(0F, RotorStrikePhysics.attitudeKick(0F));
        assertTrue(RotorStrikePhysics.attitudeKick(1F) > RotorStrikePhysics.attitudeKick(0.1F));
        assertTrue(RotorStrikePhysics.yawKick(1F) > 0F);
        assertEquals(RotorStrikePhysics.attitudeKick(1F), RotorStrikePhysics.attitudeKick(5F));
    }

    @Test
    void levelDiscMeetsBlocksWithinItsSweepOnly()
    {
        // Beside the hub, at rotor height, just inside and just beyond the tip.
        assertTrue(RotorStrikePhysics.intersectsDisc(HUB, UP, RADIUS, HALF_THICKNESS, block(4, 10, 0)));
        assertFalse(RotorStrikePhysics.intersectsDisc(HUB, UP, RADIUS, HALF_THICKNESS, block(7, 10, 0)));
        // The ground a clear block below the disc is not touched; one reaching up to it is.
        assertFalse(RotorStrikePhysics.intersectsDisc(HUB, UP, RADIUS, HALF_THICKNESS, block(2, 8, 0)));
        assertTrue(RotorStrikePhysics.intersectsDisc(HUB, UP, RADIUS, HALF_THICKNESS, block(2, 9, 0)));
        // Diagonally past the tip, although inside the disc's square bounds.
        assertFalse(RotorStrikePhysics.intersectsDisc(HUB, UP, RADIUS, HALF_THICKNESS, block(5, 10, 5)));
    }

    @Test
    void tiltedDiscDipsIntoTheGroundOnItsLowSide()
    {
        Vec3 banked = new Vec3(Math.sin(Math.toRadians(40D)), Math.cos(Math.toRadians(40D)), 0D);
        Vec3 hub = new Vec3(0.5D, 3.5D, 0.5D);
        // Banked 40 degrees toward +x, that tip reaches about 3.2 blocks down from the hub.
        assertTrue(RotorStrikePhysics.intersectsDisc(hub, banked, RADIUS, HALF_THICKNESS, block(4, 0, 0)));
        assertFalse(RotorStrikePhysics.intersectsDisc(hub, banked, RADIUS, HALF_THICKNESS, block(-4, 0, 0)));
    }

    @Test
    void boundsCoverTheWholeDisc()
    {
        AABB level = RotorStrikePhysics.discBounds(HUB, UP, RADIUS, HALF_THICKNESS);
        assertEquals(RADIUS * 2D + RotorStrikePhysics.CONTACT_TOLERANCE * 2D, level.getXsize(), 1E-9D);
        assertEquals(HALF_THICKNESS * 2D + RotorStrikePhysics.CONTACT_TOLERANCE * 2D, level.getYsize(), 1E-9D);
    }
}
