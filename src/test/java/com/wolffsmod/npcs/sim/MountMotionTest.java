package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MountMotionTest
{
    private static WeaponMount mount(float minYaw, float maxYaw, float minPitch, float maxPitch)
    {
        return new WeaponMount(0, true, true, new MountAim(minYaw, minPitch), new MountAim(maxYaw, maxPitch), 2F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.empty());
    }

    @Test
    void aFreeTurretTakesTheShortWayRoundAtItsSpeed()
    {
        WeaponMount free = mount(-360F, 360F, -20F, 10F);
        MountAim next = MountMotion.approach(new MountAim(170F, 0F), new MountAim(-170F, -30F), free, 5F, 2F);
        assertEquals(175F, next.yaw(), 1.0E-4F);
        assertEquals(-2F, next.pitch(), 1.0E-4F);
        // Elevation stops at the mount's limit however long it turns.
        MountAim aim = new MountAim(0F, 0F);
        for (int tick = 0; tick < 100; tick++)
            aim = MountMotion.approach(aim, new MountAim(0F, -90F), free, 5F, 2F);
        assertEquals(-20F, aim.pitch(), 1.0E-4F);
    }

    @Test
    void aLimitedTurretNeverSwingsThroughTheArcItCannotReach()
    {
        // A rear-facing casemate from 100 to 260 degrees: the short way from one end to the other is through the front.
        WeaponMount rear = mount(100F, 260F, -10F, 10F);
        MountAim aim = new MountAim(110F, 0F);
        for (int tick = 0; tick < 200; tick++)
        {
            aim = MountMotion.approach(aim, new MountAim(250F, 0F), rear, 3F, 1F);
            float fromCentre = Math.abs(net.minecraft.util.Mth.wrapDegrees(aim.yaw() - 180F));
            assertTrue(fromCentre <= 80.001F, "left the arc at " + aim.yaw());
        }
        assertEquals(250F, aim.yaw(), 1.0E-3F);
        // A target outside the arc is followed to the nearer end.
        MountAim clamped = rear.clamp(new MountAim(10F, 0F));
        assertEquals(100F, clamped.yaw(), 1.0E-4F);
        assertEquals(180F, rear.restYaw(), 1.0E-4F);
        assertFalse(rear.turnsFreely());
    }

    @Test
    void withoutAMountNothingIsLimited()
    {
        MountAim next = MountMotion.approach(MountAim.REST, new MountAim(90F, -80F), null, 360F, 360F);
        assertEquals(90F, next.yaw(), 1.0E-4F);
        assertEquals(-80F, next.pitch(), 1.0E-4F);
    }

    @Test
    void alignmentIsTheLargerOfTheTwoErrors()
    {
        assertEquals(3F, MountMotion.error(new MountAim(179F, 0F), new MountAim(-178F, 2F)), 1.0E-4F);
        assertEquals(7F, MountMotion.error(new MountAim(0F, -5F), new MountAim(1F, 2F)), 1.0E-4F);
    }

    @Test
    void aVehicleSlowsDownTheMoreItsHullPointsAway()
    {
        assertEquals(1F, MountMotion.throttle(0F, 0.15F));
        assertEquals(1F, MountMotion.throttle(-MountMotion.FULL_SPEED_ANGLE, 0.15F));
        assertEquals(0.15F, MountMotion.throttle(MountMotion.SLOWEST_ANGLE, 0.15F), 1.0E-6F);
        assertEquals(0.15F, MountMotion.throttle(180F, 0.15F), 1.0E-6F);
        // Halfway between the two angles, halfway between the two speeds.
        assertEquals(0.575F, MountMotion.throttle(55F, 0.15F), 1.0E-5F);
        assertEquals(0.575F, MountMotion.throttle(-55F - 360F, 0.15F), 1.0E-5F);
        // A floor of nothing turns on the spot.
        assertEquals(0F, MountMotion.throttle(120F, 0F));
    }

    @Test
    void hullTurnsAreLimitedAndWrap()
    {
        assertEquals(-176F, MountMotion.turn(178F, -170F, 6F), 1.0E-4F);
        assertEquals(40F, MountMotion.turn(38F, 40F, 6F), 1.0E-4F);
    }

    @Test
    void aimsAndDirectionsUseMinecraftAngles()
    {
        // South is yaw 0, west is yaw 90, and looking up is a negative pitch.
        assertEquals(0F, MountMotion.yaw(new Vec3(0D, 0D, 1D)), 1.0E-4F);
        assertEquals(90F, MountMotion.yaw(new Vec3(-1D, 0D, 0D)), 1.0E-4F);
        MountAim aim = MountMotion.relative(new Vec3(-1D, 1D, 0D), 60F);
        assertEquals(30F, aim.yaw(), 1.0E-4F);
        assertEquals(-45F, aim.pitch(), 1.0E-4F);
        Vec3 back = MountMotion.direction(aim, 60F);
        assertEquals(-Math.sqrt(0.5D), back.x, 1.0E-5D);
        assertEquals(Math.sqrt(0.5D), back.y, 1.0E-5D);
        assertEquals(0D, back.z, 1.0E-5D);
        assertEquals(0F, MountMotion.yaw(new Vec3(0D, 1D, 0D)));
    }

    @Test
    void aSeatOnTheTurretIsCarriedRoundItsPivot()
    {
        WeaponMount seat = new WeaponMount(1, false, true, new MountAim(-360F, 0F), new MountAim(360F, 0F), 2F, 2F, new Vec3(0D, 1D, -2D), new Vec3(0D, 0D, -1D), Optional.empty());
        // A quarter turn to the right carries a seat ahead of the pivot to its right.
        Vec3 turned = seat.positionAt(90F);
        assertEquals(1D, turned.x, 1.0E-9D);
        assertEquals(1D, turned.y, 1.0E-9D);
        assertEquals(-1D, turned.z, 1.0E-9D);
        assertEquals(seat.position(), seat.positionAt(0F));
        WeaponMount hull = new WeaponMount(2, false, false, new MountAim(-360F, 0F), new MountAim(360F, 0F), 2F, 2F, new Vec3(1D, 1D, 1D), new Vec3(1D, 1D, 1D), Optional.empty());
        assertEquals(hull.position(), hull.positionAt(90F));
    }
}
