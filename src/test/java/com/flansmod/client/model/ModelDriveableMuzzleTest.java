package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.MuzzleMeasurements;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;

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
        assertEquals(-42F, vehicle.z, 1.0E-4F, "geometry and attachments share the lateral mirror");
        assertEquals(-42F, plane.z, 1.0E-4F);
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

    /** Two tubes 24 pixels apart ending at the same depth, as a twin mount builds them. */
    private static ModelVehicle twinMount()
    {
        ModelVehicle model = new ModelVehicle();
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 0F, -1F, -13F, 80, 2, 2),
            barrelSection(model, 0F, 0F, 0F, 0F, -1F, 11F, 80, 2, 2)
        };
        return model;
    }

    @Test
    void aTwinMountMeasuredWholeLandsBetweenItsBarrels()
    {
        Vec3 muzzle = twinMount().getPrimaryBarrelMuzzle();

        assertNotNull(muzzle);
        assertEquals(0D, muzzle.z, 1.0E-4D, "why the load-time correction asks for the barrel near the authored point");
    }

    @Test
    void aLateralHintPicksTheBarrelTheTypeFiresFrom()
    {
        ModelVehicle model = twinMount();

        Vec3 onBarrel = model.getPrimaryBarrelMuzzleNear(new Vec3(0D, 0D, -12.5D));
        Vec3 beyond = model.getPrimaryBarrelMuzzleNear(new Vec3(0D, 0D, 40D));

        assertNotNull(onBarrel);
        assertEquals(80D, onBarrel.x, 1.0E-4D);
        assertEquals(-12D, onBarrel.z, 1.0E-4D, "the tube containing the hint");
        assertNotNull(beyond);
        assertEquals(12D, beyond.z, 1.0E-4D, "a hint beyond the mount takes the outermost tube on its side");
    }

    @Test
    void aHintOnTheBoreKeepsAMuzzleBrakeWhole()
    {
        ModelVehicle model = new ModelVehicle();
        // The asymmetric brake below: two plates with the bore between them.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, -4F, 2, 4, 2),
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, 2F, 2, 4, 2)
        };

        Vec3 muzzle = model.getPrimaryBarrelMuzzleNear(new Vec3(0D, 0D, 0D));

        assertNotNull(muzzle);
        assertEquals(0D, muzzle.z, 1.0E-4D, "a hint between two groups is a bore, not a choice of barrel");
    }

    @Test
    void aFrontSightBehindTheMuzzleDoesNotLiftTheBore()
    {
        ModelVehicle model = new ModelVehicle();
        // The Warfare 44 Tiger's MG34, rest pose: the muzzle ring on the bore at
        // y 18 and, 2.3 pixels short of it, a front sight standing a pixel above.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 36F, 18F, 10F, 11.4F, -0.25F, -0.25F, 1, 1, 1),
            barrelSection(model, 36F, 18F, 10F, 9.1F, 0.15F, -0.04F, 1, 1, 1)
        };

        Vec3 muzzle = model.getPrimaryBarrelMuzzle();

        assertNotNull(muzzle);
        assertEquals(48.4D, muzzle.x, 1.0E-4D);
        assertEquals(18.25D, muzzle.y, 1.0E-4D, "centred on the muzzle ring alone");
        assertEquals(10.25D, muzzle.z, 1.0E-4D);
    }

    @Test
    void aShootPointPitchesRoundTheBarrelSectionItIsBuiltOn()
    {
        ModelVehicle model = new ModelVehicle();
        // The Warfare 44 M4A3E8's gun and its roof .30, both in barrelModel,
        // each drawn pitching round its own rotation point.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 14F, 28F, 0F, -3F, -1F, -1F, 60, 2, 2),
            barrelSection(model, 3F, 39.5F, -2F, 1F, 0F, -0.5F, 8, 1, 1)
        };

        assertEquals(new Vec3(14D / 16D, 28D / 16D, 0D), model.getBarrelPitchPivotNear(new Vec3(70D, 28D, 0D)));
        assertEquals(new Vec3(3D / 16D, 39.5D / 16D, -2D / 16D),
            model.getBarrelPitchPivotNear(new Vec3(7D, 40D, -1.7D)), "the roof gun, not the main gun's trunnion");
        assertNull(model.getBarrelPitchPivotNear(new Vec3(-20D, 20D, 15D)), "nothing near: keep the main pivot");
    }

    @Test
    void aQuadMountMeasuresOneMuzzlePerBarrel()
    {
        ModelVehicle model = new ModelVehicle();
        // Two rows of two tubes, three tube widths apart, as the Flakvierling
        // lays out its flash hiders.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 22F, 5.6F, -4.9F, 0F, 0F, 0F, 2, 1, 1),
            barrelSection(model, 22F, 1.6F, -4.9F, 0F, 0F, 0F, 2, 1, 1),
            barrelSection(model, 22F, 1.6F, 4.1F, 0F, 0F, 0F, 2, 1, 1),
            barrelSection(model, 22F, 5.6F, 4.1F, 0F, 0F, 0F, 2, 1, 1)
        };

        List<Vec3> muzzles = model.getPrimaryBarrelMuzzles();

        assertEquals(4, muzzles.size());
        assertTrue(muzzles.stream().anyMatch(muzzle -> muzzle.distanceTo(new Vec3(24D, 6.1D, -4.4D)) < 1.0E-4D));
        assertTrue(muzzles.stream().anyMatch(muzzle -> muzzle.distanceTo(new Vec3(24D, 2.1D, 4.6D)) < 1.0E-4D));
    }

    @Test
    void aShorterLowerPairStillCountsAsBarrels()
    {
        ModelVehicle model = new ModelVehicle();
        // The M45 quad mount: the upper pair ends at x 17, the lower pair four
        // pixels behind, and a receiver the barrels run out of.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, -10F, 5F, -12F, 16, 17, 25),
            barrelSection(model, 0F, 0F, 0F, 7F, 17F, -14.5F, 10, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 7F, 17F, 14.5F, 10, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 3F, 12F, -16.5F, 10, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 3F, 12F, 16.5F, 10, 1, 1)
        };

        List<Vec3> muzzles = model.getPrimaryBarrelMuzzles();

        assertEquals(4, muzzles.size());
        assertTrue(muzzles.stream().anyMatch(muzzle -> muzzle.distanceTo(new Vec3(17D, 17.5D, -14D)) < 1.0E-4D));
        assertTrue(muzzles.stream().anyMatch(muzzle -> muzzle.distanceTo(new Vec3(13D, 12.5D, 17D)) < 1.0E-4D));
    }

    @Test
    void aMuzzleBrakeIsStillOneBarrel()
    {
        ModelVehicle model = new ModelVehicle();
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, -4F, 2, 4, 2),
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, 2F, 2, 4, 2)
        };

        List<Vec3> muzzles = model.getPrimaryBarrelMuzzles();
        assertEquals(1, muzzles.size());
        assertEquals(0D, muzzles.get(0).distanceTo(new Vec3(80D, 0D, 0D)), 1.0E-4D);
    }

    @Test
    void aMuzzleBrakeRoundItsBoreIsStillOneBarrel()
    {
        ModelVehicle model = new ModelVehicle();
        // The side plates stand either side of the tube and end no bore of their own.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 40F, -0.5F, -0.5F, 38, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, -4F, 2, 4, 2),
            barrelSection(model, 0F, 0F, 0F, 78F, -2F, 2F, 2, 4, 2)
        };

        assertEquals(1, model.getPrimaryBarrelMuzzles().size());
    }

    @Test
    void stackedBarrelsEndingTheirOwnBoresAreSeparateBarrels()
    {
        ModelVehicle model = new ModelVehicle();
        // The Manus Flakpanzer IV's Flakvierling: four tubes, each ending in a
        // flash hider standing only its own width off the one above it.
        List<ModelRendererTurbo> parts = new java.util.ArrayList<>();
        for (float y : new float[] { 38F, 42F })
        {
            for (float z : new float[] { -6.5F, 5.5F })
            {
                parts.add(barrelSection(model, 0F, 0F, 0F, 12F, y, z, 20, 1, 1));
                parts.add(barrelSection(model, 0F, 0F, 0F, 32F, y - 0.5F, z - 0.5F, 5, 2, 2));
            }
        }
        model.barrelModel = parts.toArray(ModelRendererTurbo[]::new);

        List<Vec3> muzzles = model.getPrimaryBarrelMuzzles();

        assertEquals(4, muzzles.size());
        for (Vec3 expected : List.of(new Vec3(37D, 38.5D, 6D), new Vec3(37D, 42.5D, 6D),
            new Vec3(37D, 38.5D, -6D), new Vec3(37D, 42.5D, -6D)))
            assertTrue(muzzles.stream().anyMatch(muzzle -> muzzle.distanceTo(expected) < 1.0E-4D), "barrel at " + expected);
    }

    @Test
    void aGasCylinderUnderATwinGunIsNotABarrel()
    {
        ModelVehicle model = new ModelVehicle();
        // A twin Vickers K: each barrel has a shorter gas cylinder a pixel below it.
        model.barrelModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 16F, 21F, 3F, 10, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 16F, 21F, 10F, 10, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 12F, 19.9F, 3F, 10, 1, 1),
            barrelSection(model, 0F, 0F, 0F, 12F, 19.9F, 10F, 10, 1, 1)
        };

        List<Vec3> muzzles = model.getPrimaryBarrelMuzzles();

        assertEquals(2, muzzles.size());
        assertTrue(muzzles.stream().allMatch(muzzle -> muzzle.x == 26D));
    }

    @Test
    void aTwinSeatGunMeasuresOneMuzzlePerBarrel()
    {
        ModelVehicle model = new ModelVehicle();
        model.registerGunModel("Twin", new ModelRendererTurbo[][] {
            {},
            { barrelSection(model, 0F, 0F, 0F, -2F, -1F, -2F, 4, 2, 4) },
            {
                barrelSection(model, 0F, 0F, 0F, 0F, 0F, -3F, 17, 1, 1),
                barrelSection(model, 0F, 0F, 0F, 0F, 0F, 2F, 17, 1, 1)
            }
        });

        List<Vec3> muzzles = model.getRegisteredGunMuzzles("Twin", 1F);

        assertEquals(2, muzzles.size());
        assertTrue(muzzles.contains(new Vec3(17D, 0.5D, -2.5D)));
        assertTrue(muzzles.contains(new Vec3(17D, 0.5D, 2.5D)));
        assertEquals(new Vec3(17D, 0.5D, 0D), model.getRegisteredGunMuzzle("Twin", 1F), "the whole gun still centres between them");
    }

    /** A turret machine gun on the +Z side of the model, its muzzle at x 30. */
    private static ModelVehicle turretMachineGun()
    {
        ModelVehicle model = new ModelVehicle();
        model.turretModel = new ModelRendererTurbo[] {
            barrelSection(model, 0F, 0F, 0F, 0F, 20F, -8F, 20, 10, 16),
            barrelSection(model, 0F, 0F, 0F, 20F, 26F, 5F, 10, 1, 1)
        };
        return model;
    }

    @Test
    void aMachineGunWrittenOnTheWrongSideIsMirrored()
    {
        MuzzleMeasurements.DriveableInputs inputs = new MuzzleMeasurements.DriveableInputs(false, 1D, 1F, List.of());

        assertTrue(MuzzleMeasurements.isMirroredMuzzle(turretMachineGun(), inputs, new Vector3f(30F, 26.5F, -5.5F)));
        assertFalse(MuzzleMeasurements.isMirroredMuzzle(turretMachineGun(), inputs, new Vector3f(30F, 26.5F, 5.5F)),
            "a point on its gun stays where it is");
        assertFalse(MuzzleMeasurements.isMirroredMuzzle(turretMachineGun(), inputs, new Vector3f(40F, 26.5F, -5.5F)),
            "nothing drawn on either side: no evidence to move it");
    }

    @Test
    void aRotatedPartIsFoundWhereItIsDrawn()
    {
        ModelVehicle model = new ModelVehicle();
        ModelRendererTurbo plank = barrelSection(model, 0F, 0F, 0F, 0F, 0F, 0F, 10, 1, 1);
        plank.rotateAngleY = (float) (Math.PI / 2D);
        model.bodyModel = new ModelRendererTurbo[] { plank };

        assertEquals(0D, model.distanceToGeometry(new Vec3(0.5D, 0.5D, -5D)), 1.0E-4D, "turned onto -Z");
        assertTrue(model.distanceToGeometry(new Vec3(5D, 0.5D, 0.5D)) > 3D, "no longer along +X");
    }

    @Test
    void aModelWithNoBarrelGeometryMeasuresNothing()
    {
        assertNull(new ModelVehicle().getPrimaryBarrelMuzzle());
    }
}
