package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/** Pins how damage, burning and wrecks are shown, and which parts show what. */
class DamageVisualCurvesTest
{
    @Test
    @DisplayName("A lightly damaged part shows nothing, a lost one shows fully")
    void damageStartsLateAndRisesToFull()
    {
        assertEquals(0F, DamageVisualCurves.damage(1F));
        assertEquals(0F, DamageVisualCurves.damage(DamageVisualCurves.DAMAGE_THRESHOLD));
        assertEquals(1F, DamageVisualCurves.damage(0F), 1.0E-6F);
        float previous = 0F;
        for (float health = DamageVisualCurves.DAMAGE_THRESHOLD; health >= 0F; health -= 0.05F)
        {
            float damage = DamageVisualCurves.damage(health);
            assertTrue(damage >= previous, "damage grows as health falls");
            previous = damage;
        }
        assertEquals(0F, DamageVisualCurves.damage(Float.NaN));
    }

    @Test
    @DisplayName("A wreck burns hard, dies down to smoke, and ends smouldering")
    void wreckBurnsOutInStages()
    {
        int lifetime = 4000;
        int fire = DamageVisualCurves.fireTicks(lifetime);
        assertEquals(DamageVisualCurves.MAX_FIRE_TICKS, fire, "the heavy fire is capped on a long-lived wreck");
        assertEquals(1F, DamageVisualCurves.wreckFire(0, lifetime));
        assertEquals(1F, DamageVisualCurves.wreckFire(fire / 2 - 1, lifetime));
        assertTrue(DamageVisualCurves.wreckFire(fire * 3 / 4, lifetime) < 1F);
        assertEquals(0F, DamageVisualCurves.wreckFire(fire, lifetime));
        assertEquals(1F, DamageVisualCurves.wreckSmoke(fire - 1, lifetime), "the column stands while it burns");
        assertTrue(DamageVisualCurves.wreckSmoke(lifetime - 1, lifetime) < DamageVisualCurves.wreckSmoke(fire + 10, lifetime), "the smoke thins out");
        assertTrue(DamageVisualCurves.wreckSmoke(lifetime - 1, lifetime) > 0F, "it is still smouldering at the end");
        assertEquals(0F, DamageVisualCurves.wreckFire(-1, lifetime));
        assertEquals(0F, DamageVisualCurves.wreckSmoke(-1, lifetime));
    }

    @Test
    @DisplayName("A short-lived wreck still gets a fire, scaled to its lifetime")
    void shortWreckFireScales()
    {
        assertEquals(35, DamageVisualCurves.fireTicks(100));
        assertTrue(DamageVisualCurves.fireTicks(1) >= 1);
    }

    @ParameterizedTest
    @CsvSource({"1.0, false, -1, 1.0", "0.0, false, 300, 0.3"})
    @DisplayName("Clean parts keep their colour; a burnt-out wreck is charred")
    void tintEnds(float health, boolean burning, int wreckAge, float expected)
    {
        assertEquals(expected, DamageVisualCurves.tint(health, burning, wreckAge), 1.0E-4F);
    }

    @Test
    @DisplayName("Damage and fire darken a part, never below the floor for a working vehicle")
    void tintDarkens()
    {
        float clean = DamageVisualCurves.tint(1F, false, -1);
        float damaged = DamageVisualCurves.tint(0.1F, false, -1);
        float burning = DamageVisualCurves.tint(0.1F, true, -1);
        assertTrue(damaged < clean);
        assertTrue(burning < damaged);
        assertTrue(burning >= DamageVisualCurves.DAMAGED_TINT);
        assertTrue(DamageVisualCurves.tint(1F, false, 0) > DamageVisualCurves.tint(1F, false, DamageVisualCurves.CHAR_TICKS), "a fresh wreck chars over time");
    }

    @Test
    @DisplayName("A wrecked mecha falls, lands and stays down")
    void collapse()
    {
        assertEquals(0F, DamageVisualCurves.collapse(0F));
        assertTrue(DamageVisualCurves.collapse(DamageVisualCurves.COLLAPSE_TICKS / 2F) < DamageVisualCurves.collapse(DamageVisualCurves.COLLAPSE_TICKS));
        assertEquals(1F, DamageVisualCurves.collapse(DamageVisualCurves.COLLAPSE_TICKS), 1.0E-6F);
        assertEquals(1F, DamageVisualCurves.collapse(1000F), 1.0E-6F);
    }

    @Test
    @DisplayName("A wreck's fire stages run in order, so a client is told the moment one changes")
    void fireStagesRunInOrder()
    {
        int lifetime = 2400;
        int fire = DamageVisualCurves.fireTicks(lifetime);
        assertEquals(0, DamageVisualCurves.fireStage(0, lifetime), "the fire is at its height");
        assertEquals(0, DamageVisualCurves.fireStage(-1, lifetime), "and a working driveable reads as that too");
        assertEquals(1, DamageVisualCurves.fireStage(fire - 1, lifetime), "then dies down");
        assertEquals(4, DamageVisualCurves.fireStage(lifetime - 1, lifetime), "and ends smouldering");
        int previous = -1;
        for (int age = 0; age < lifetime; age++)
        {
            int stage = DamageVisualCurves.fireStage(age, lifetime);
            assertTrue(stage >= previous, "a wreck's fire never picks back up");
            previous = stage;
        }
    }

    @Test
    @DisplayName("A wreck settles onto its springs, then sinks away over its last seconds")
    void wreckSettlesThenSinksAway()
    {
        int lifetime = 2400;
        assertEquals(0F, DamageVisualCurves.wreckSag(0F));
        assertTrue(DamageVisualCurves.wreckSag(1000F) > DamageVisualCurves.wreckSag(10F), "it settles as it cools");
        assertEquals(DamageVisualCurves.wreckSag(1000F), DamageVisualCurves.wreckSag(2000F), "and then stays where it is");

        assertEquals(0F, DamageVisualCurves.wreckSink(0F, lifetime), "it does not start sinking the moment it dies");
        assertEquals(0F, DamageVisualCurves.wreckSink(lifetime - DamageVisualCurves.SINK_TICKS, lifetime));
        float almostGone = DamageVisualCurves.wreckSink(lifetime - 1, lifetime);
        assertTrue(almostGone > 0.9F, "by the time it is removed it is in the ground");
        assertTrue(DamageVisualCurves.wreckSink(lifetime - 10, lifetime) < almostGone, "and it goes down over those seconds");
        assertEquals(0F, DamageVisualCurves.wreckSink(100_000F, 0), "a wreck that stays until removed never sinks away");
    }

    @Test
    @DisplayName("A wrecked gun droops and its turret slumps off its bearing, then stops")
    void wreckedTurretSlumps()
    {
        assertEquals(0F, DamageVisualCurves.turretDroop(0F));
        assertEquals(0F, DamageVisualCurves.turretSkew(0F));
        assertTrue(DamageVisualCurves.turretDroop(25F) > 0F);
        assertTrue(DamageVisualCurves.turretDroop(50F) > DamageVisualCurves.turretDroop(25F), "the barrel goes on dropping");
        assertEquals(DamageVisualCurves.turretDroop(50F), DamageVisualCurves.turretDroop(5000F), 1.0E-6F, "until it is resting");
        assertEquals(DamageVisualCurves.turretSkew(50F), DamageVisualCurves.turretSkew(5000F), 1.0E-6F);
        assertTrue(DamageVisualCurves.turretSkew(5000F) > 0F, "and the turret has moved off its bearing");
    }

    @Test
    @DisplayName("A collapsing mecha goes over its feet and stops there")
    void mechaCollapseIsBounded()
    {
        assertEquals(0F, DamageVisualCurves.mechaCollapseDegrees(0F));
        assertTrue(DamageVisualCurves.mechaCollapseDegrees(1F) > 70F, "it ends up on its side");
        assertEquals(DamageVisualCurves.mechaCollapseDegrees(1.05F), DamageVisualCurves.mechaCollapseDegrees(50F), "and no further");
    }

    @ParameterizedTest
    @CsvSource({"ENGINE_ROOM_3, ENGINE", "BOILER_ROOM_1, BOILER", "TURRET, TURRET", "TURRET_5, TURRET", "TURRET_ARMOR, ARMOR", "LEFT_TRACK, RUNNING_GEAR", "FRONT_LEFT_WHEEL, RUNNING_GEAR",
        "LEFT_WING, AIRFOIL", "TAIL, AIRFOIL", "BLADES, ROTOR", "LEFT_ARM, LIMB", "HIPS, LIMB", "ERA, ARMOR", "RADAR_2, ELECTRONICS", "BOW, HULL", "BRIDGE, SUPERSTRUCTURE", "GASBAG, GASBAG",
        "INFANTRY, NONE", "CORE, STRUCTURE"})
    @DisplayName("Parts show the damage their names imply")
    void defaultKinds(EnumDriveablePart part, PartDamageKind expected)
    {
        assertEquals(expected, PartDamageKind.defaultFor(part));
    }

    @ParameterizedTest
    @EnumSource(EnumDriveablePart.class)
    @DisplayName("Every part has a kind")
    void everyPartHasAKind(EnumDriveablePart part)
    {
        assertNotNull(PartDamageKind.defaultFor(part));
    }

    @Test
    @DisplayName("Type files name kinds in any case; unknown names are rejected")
    void parseKinds()
    {
        assertEquals(PartDamageKind.BOILER, PartDamageKind.parse(" boiler ").orElseThrow());
        assertTrue(PartDamageKind.parse("smoky").isEmpty());
        assertTrue(PartDamageKind.parse(null).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false, false, true, false, false, false, MECHA", "false, false, false, true, false, true, SHIP", "true, false, false, false, false, false, PLANE",
        "true, true, false, false, false, false, HELICOPTER", "false, false, false, false, true, false, TANK", "false, false, false, false, false, true, TANK",
        "false, false, false, false, false, false, GROUND"})
    @DisplayName("Machines die as their kind")
    void classify(boolean plane, boolean helicopter, boolean mecha, boolean ship, boolean tracked, boolean turret, DriveableDamageClass expected)
    {
        assertEquals(expected, DriveableDamageClass.classify(plane, helicopter, mecha, ship, tracked, turret));
    }
}
