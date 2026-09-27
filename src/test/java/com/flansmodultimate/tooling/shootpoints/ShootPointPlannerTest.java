package com.flansmodultimate.tooling.shootpoints;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.driveables.DerivedMuzzle;
import com.flansmodultimate.common.entity.AAGunBarrelGeometry;
import com.flansmodultimate.tooling.shootpoints.Finding.Action;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShootPointPlannerTest
{
    private static DefinitionFile parse(String text)
    {
        return DefinitionFile.parse(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String text(DefinitionFile file)
    {
        return new String(file.toBytes(), StandardCharsets.ISO_8859_1);
    }

    private static List<Finding> plan(DefinitionFile file, DerivedMuzzle... derived)
    {
        return ShootPointPlanner.planDriveable("test", file, ShootPointPlanner.readDriveable(file), List.of(derived),
            false, true);
    }

    private static DerivedMuzzle barrel(float x, float y, float z)
    {
        return new DerivedMuzzle(-1, "barrel", new Vector3f(x, y, z));
    }

    @Test
    void aBarrelPositionWithoutOffsetMovesToTheMeasuredMuzzle()
    {
        DefinitionFile file = parse("Model W44.Tiger\nBarrelPosition 0 20 0\n");

        List<Finding> findings = plan(file, barrel(60.04F, 22.5F, -0.5F));

        assertEquals(Action.UPDATE, findings.get(0).action);
        assertEquals("Model W44.Tiger\nBarrelPosition 60 22.5 -0.5\n", text(file));
    }

    @Test
    void anAuthoredOffsetCarriesTheDifferenceSoTheRootStays()
    {
        DefinitionFile file = parse("ShootPointPrimary 10 20 0 turret 40 0 0\n");

        plan(file, barrel(62F, 21F, 0F));

        assertEquals("ShootPointPrimary 10 20 0 turret 52 1 0\n", text(file));
    }

    @Test
    void aSecondPassChangesNothingEvenOnARoundingBoundary()
    {
        DefinitionFile first = parse("ShootPointPrimary 10.3 20 0 turret 40 0 0\nPassenger 1 0 30 0 turret -360 360 -10 60 MG34 mg\n");
        DerivedMuzzle barrel = barrel(56.35F, 22.45F, -0.05F);
        DerivedMuzzle gun = new DerivedMuzzle(1, "seat 1 (mg)", new Vector3f(32.95F, -4.05F, 8.25F));
        plan(first, barrel, gun);

        DefinitionFile second = DefinitionFile.parse(first.toBytes());
        List<Finding> findings = plan(second, barrel, gun);

        assertFalse(second.isModified());
        assertTrue(findings.stream().allMatch(finding -> finding.action == Action.UNCHANGED), findings.toString());
    }

    @Test
    void aMatchingPointIsLeftAlone()
    {
        DefinitionFile file = parse("BarrelPosition 60 22.5 0\n");

        List<Finding> findings = plan(file, barrel(60.02F, 22.5F, 0F));

        assertEquals(Action.UNCHANGED, findings.get(0).action);
        assertTrue(!file.isModified());
    }

    @Test
    void aBankOfSeveralPointsIsSkippedAndFlagged()
    {
        DefinitionFile file = parse("ShootPointPrimary 1 2 3 turret\nBarrelPosition 4 5 6\n");

        List<Finding> findings = plan(file, barrel(60F, 20F, 0F));

        assertEquals(Action.SKIPPED, findings.get(0).action);
        assertTrue(findings.get(0).score >= 15);
        assertTrue(!file.isModified());
    }

    @Test
    void aMirroredLateralValueIsListedToCheck()
    {
        DefinitionFile file = parse("BarrelPosition 60 20 -12\n");

        List<Finding> findings = plan(file, barrel(60F, 20F, 12F));

        assertTrue(findings.get(0).score >= 15, findings.get(0).reasons.toString());
    }

    @Test
    void aLargeMoveAloneIsNotListedToCheck()
    {
        DefinitionFile file = parse("BarrelPosition 0 20 0\n");

        List<Finding> findings = plan(file, barrel(60F, 20F, 0F));

        assertTrue(findings.get(0).score < 15, findings.get(0).reasons.toString());
    }

    @Test
    void aMissingGunOriginIsAddedAfterItsSeat()
    {
        DefinitionFile file = parse("Passenger 1 0 30 0 turret -360 360 -10 60 MG34 mg\nOther 1\n");

        List<Finding> findings = plan(file, new DerivedMuzzle(1, "seat 1 (mg)", new Vector3f(20F, 12F, -3F)));

        assertEquals(Action.ADD, findings.get(0).action);
        assertEquals("Passenger 1 0 30 0 turret -360 360 -10 60 MG34 mg\nGunOrigin 1 20 12 -3\nOther 1\n", text(file));
    }

    @Test
    void anAuthoredGunOriginIsRewrittenInPlace()
    {
        DefinitionFile file = parse("Passenger 1 0 30 0 turret -360 360 -10 60 MG34 mg\nGunOrigin 1 0 0 0\n"
            + "GunOrigin 1 5 5 5\n");

        plan(file, new DerivedMuzzle(1, "seat 1 (mg)", new Vector3f(20F, 12F, -3F)));

        assertEquals("Passenger 1 0 30 0 turret -360 360 -10 60 MG34 mg\nGunOrigin 1 0 0 0\nGunOrigin 1 20 12 -3\n",
            text(file));
    }

    @Test
    void aGunAcrossTheHullFromItsGunnerIsFlagged()
    {
        DefinitionFile file = parse("Passenger 1 20 30 -10 core -360 360 -10 60 MG34 mg\nGunOrigin 1 20 12 12\n");

        List<Finding> findings = plan(file, new DerivedMuzzle(1, "seat 1 (mg)", new Vector3f(20F, 12F, 12F)));

        assertEquals(Action.UNCHANGED, findings.get(0).action);
        assertTrue(findings.get(0).score >= 20, findings.get(0).reasons.toString());
    }

    @Test
    void aSeatGunTheModelDoesNotRegisterIsReported()
    {
        DefinitionFile file = parse("Passenger 1 0 30 0 turret -360 360 -10 60 MG34 mg\n");

        List<Finding> findings = plan(file);

        assertEquals(1, findings.size());
        assertEquals(Action.SKIPPED, findings.get(0).action);
    }

    @Test
    void aaGunBarrelsAreRewrittenOrAddedToTheMeasuredMuzzles()
    {
        DefinitionFile file = parse("NumBarrels 2\nBarrel 0 1 2 3\n");
        Vec3 first = new Vec3(1.25D, 1.4375D, -0.5D);
        Vec3 second = new Vec3(1.25D, 1.4375D, 0.5D);

        List<Finding> findings = ShootPointPlanner.planAAGun("test", file, ShootPointPlanner.readAAGun(file),
            List.of(first, second));

        Vector3f line0 = AAGunBarrelGeometry.legacyBarrelFor(first, false);
        Vector3f line1 = AAGunBarrelGeometry.legacyBarrelFor(second, false);
        assertEquals(Action.UPDATE, findings.get(0).action);
        assertEquals(Action.ADD, findings.get(1).action);
        assertEquals("NumBarrels 2\nBarrel 0 " + join(line0) + "\nBarrel 1 " + join(line1) + "\n", text(file));
    }

    @Test
    void aSentryIsReadFromItsTargets()
    {
        assertTrue(ShootPointPlanner.readAAGun(parse("TargetPlayers true\n")).sentry());
        assertTrue(!ShootPointPlanner.readAAGun(parse("TargetVehicles true\nTargetDriveables false\n")).sentry());
    }

    @Test
    void modelNamesResolveAsTheGameResolvesThem()
    {
        assertEquals("com.flansmod.client.model.W44.ModelTiger", ShootPointSync.modelClassName("W44.Tiger"));
        assertEquals("com.flansmod.client.model.Manus_WW2.AAGun.ModelFlak88",
            ShootPointSync.modelClassName("Manus_WW2.AAGun.Flak88"));
        assertEquals("com.flansmod.client.model.ModelJeep", ShootPointSync.modelClassName("Jeep"));
        assertNull(ShootPointSync.modelClassName("none"));
    }

    private static String join(Vector3f line)
    {
        return DefinitionFile.formatNumber(line.x, 2) + " " + DefinitionFile.formatNumber(line.y, 2) + " "
            + DefinitionFile.formatNumber(line.z, 2);
    }
}
