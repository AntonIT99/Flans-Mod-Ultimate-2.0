package com.flansmodultimate.common.driveables.parts;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.parts.FlansRepairs;
import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PartBodyRepairsTest
{
    @Test
    void repairedBuoyancyRestoresNavalSpeedAndLostCompartmentsCanStillSinkTheShip()
    {
        PartBody ship = new PartBody(null, Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(1000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F), EnumDriveablePart.BUOYANCY,
            CollisionBox.inWorldUnits(6000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F), EnumDriveablePart.BOW, CollisionBox.inWorldUnits(1000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F)), Vec3.ZERO, Vec3.ZERO);
        ship.damage("buoyancy", 3000F, false);
        Vec3 healthy = new Vec3(1D, 0D, 1D);
        assertEquals(0.5D, FlansRepairs.navalVelocity(ship, Vec3.ZERO, healthy, 2D).x, 1E-5D);
        FlansRepairs.repairPart(ship, "buoyancy", 3000F, true);
        assertEquals(1D, FlansRepairs.navalVelocity(ship, Vec3.ZERO, healthy, 2D).x, 1E-5D);
        ship.damage("bow", 1000F, false);
        assertFalse(FlansRepairs.sunk(ship, 2D));
        assertTrue(FlansRepairs.sunk(ship, 16D));
        assertTrue(FlansRepairs.navalVelocity(ship, Vec3.ZERO, healthy, 2D).y < 0D);
    }

    @Test
    void toolsRestoreOnlyFivePercentOfOnePartAndClearItsFire()
    {
        PartBody body = body();
        body.damage("core", 500F, true);
        assertTrue(FlansRepairs.repairBody(body, Vec3.ZERO, 100));
        assertEquals(550F, body.getHealth());
        assertFalse(body.getPart("core").orElseThrow().onFire());
    }

    @Test
    void passiveRepairsCannotRebuildPartsButToolRepairsCan()
    {
        PartBody body = body();
        body.damage("core", 1000F, false);
        assertEquals(0F, FlansRepairs.repairPart(body, "core", 100F, false));
        assertEquals(100F, FlansRepairs.repairPart(body, "core", 100F, true));
        assertFalse(body.getPart("core").orElseThrow().destroyed());
    }

    @Test
    void repairReachMeasuresThePosedPartSurfaceRatherThanEntityOrigin()
    {
        PartBody body = body();
        body.setPose(new PartBodyPose(new Vec3(100D, 0D, 0D), 0F, 10F, MountAim.REST));
        body.damage("core", 100F, false);
        assertEquals(0D, body.repairDistanceSqr(new Vec3(109D, 1D, 0D)), 1E-5D);
        assertTrue(body.repairDistanceSqr(Vec3.ZERO) > 36D);
    }

    private static PartBody body()
    {
        return new PartBody(null, Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(1000F, -1F, 0F, -1F, 2F, 2F, 2F, 1F, 0F)), Vec3.ZERO, Vec3.ZERO);
    }
}
