package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Muzzles measured off a model have to come back in the units and convention a
 * type file is written in, or the value the debug command reports cannot be
 * pasted into one. The numbers here are the Tiger's barrel as its model builds
 * it, so the expectations are checkable against real content.
 */
class ModelDriveableMuzzleTest
{
    private static ModelRendererTurbo barrelSection(ModelVehicle model, float pivotX, float pivotY, float pivotZ,
                                                    float offsetX, float offsetY, float offsetZ,
                                                    int width, int height, int depth)
    {
        ModelRendererTurbo part = new ModelRendererTurbo(model, 0, 0, 512, 512);
        part.addBox(offsetX, offsetY, offsetZ, width, height, depth);
        part.setRotationPoint(pivotX, pivotY, pivotZ);
        return part;
    }

    /** The Tiger's furthest-forward barrel box, as its constructor writes it. */
    private static ModelVehicle tigerBarrel()
    {
        ModelVehicle model = new ModelVehicle();
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 22.5F, -28F, 0F, 0.5F, -2F, -10F, 1, 8, 20),
            barrelSection(model, 22.5F, -28F, 0F, 66.5F, -1.5F, -2F, 1, 4, 4)
        };
        return model;
    }

    @Test
    void theMuzzleIsTheForwardFaceOfTheFurthestBarrelSection()
    {
        Vec3 muzzle = tigerBarrel().getPrimaryBarrelMuzzle();

        assertNotNull(muzzle);
        assertEquals(90D, muzzle.x, 1.0E-4D, "front face of the muzzle brake");
        assertEquals(-27.5D, muzzle.y, 1.0E-4D, "centred on the bore, not a corner vertex");
        assertEquals(0D, muzzle.z, 1.0E-4D);
    }

    @Test
    void theMuzzleFollowsTheConstructorTimeFlip()
    {
        ModelVehicle model = tigerBarrel();
        model.flipAll();
        Vec3 muzzle = model.getPrimaryBarrelMuzzle();

        assertNotNull(muzzle);
        assertEquals(90D, muzzle.x, 1.0E-4D, "the flip leaves the forward axis alone");
        assertEquals(27.5D, muzzle.y, 1.0E-4D, "Y is negated, which is what makes it agree with a type file");
        assertEquals(0D, muzzle.z, 1.0E-4D);
    }

    @Test
    void aMeasuredMuzzleReadsBackAsTypeFileCoordinates()
    {
        ModelVehicle model = tigerBarrel();
        model.flipAll();

        Vector3f authored = LegacyDriveableCoordinates.modelPixelsToTypeFile(model.getPrimaryBarrelMuzzle(), false);

        // The Tiger authors BarrelPosition 0 34 0: its height is about right and
        // its forward coordinate sits at the turret pivot instead of the muzzle,
        // which is the 90 pixel correction this tooling exists to find.
        assertEquals(90F, authored.x, 1.0E-4F);
        assertEquals(27.5F, authored.y, 1.0E-4F);
        assertEquals(0F, authored.z, 1.0E-4F);
    }

    @Test
    void aircraftUndoTheModelFacingHalfTurnAsWell()
    {
        Vec3 measured = new Vec3(11D, 22D, -42D);

        Vector3f vehicle = LegacyDriveableCoordinates.modelPixelsToTypeFile(measured, false);
        Vector3f plane = LegacyDriveableCoordinates.modelPixelsToTypeFile(measured, true);

        assertEquals(11F, vehicle.x, 1.0E-4F);
        assertEquals(-11F, plane.x, 1.0E-4F, "a plane type file measures forward the other way");
        assertEquals(22F, vehicle.y, 1.0E-4F);
        assertEquals(22F, plane.y, 1.0E-4F);
        assertEquals(42F, vehicle.z, 1.0E-4F, "the lateral mirror applies either way");
        assertEquals(42F, plane.z, 1.0E-4F);
    }

    @Test
    void anAsymmetricMuzzleBrakeStaysCentredOnTheBore()
    {
        ModelVehicle model = new ModelVehicle();
        // Two boxes ending at the same depth, offset to opposite sides. Taking the
        // single furthest box would put the muzzle on whichever one won the scan.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, -4F, 2, 4, 2),
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, 2F, 2, 4, 2)
        };

        Vec3 muzzle = model.getPrimaryBarrelMuzzle();

        assertNotNull(muzzle);
        assertEquals(80D, muzzle.x, 1.0E-4D);
        assertEquals(0D, muzzle.y, 1.0E-4D);
        assertEquals(0D, muzzle.z, 1.0E-4D, "the union of both boxes is the bore centre");
    }

    @Test
    void aModelWithNoBarrelGeometryMeasuresNothing()
    {
        assertNull(new ModelVehicle().getPrimaryBarrelMuzzle());
    }
}
