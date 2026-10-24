package com.flansmodultimate.common.types;

import com.flansmodultimate.api.entity.EntityTypeProperties.Sound;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityTypePropertySupportTest
{
    @Test
    void engineInspectionPrefersEngineLengthAndFallsBackForLegacyDefinitions()
    {
        VehicleType vehicle = new VehicleType()
        {
            @Override
            public float getEngineSoundRange()
            {
                return 90F;
            }
        };
        vehicle.engineSound = "engine";
        vehicle.engineSoundRange = 90;
        vehicle.engineSoundLength = 60;
        vehicle.startSoundLength = 100;
        var sound = EntityTypePropertySupport.engineSound(vehicle).orElseThrow();
        assertEquals(vehicle.getEngineSoundRange(), sound.range());
        assertEquals(60, sound.repeatTicks());
        vehicle.engineSoundLength = 0;
        assertEquals(100, EntityTypePropertySupport.engineSound(vehicle).orElseThrow().repeatTicks());
        vehicle.startSoundLength = 0;
        assertEquals(20, EntityTypePropertySupport.engineSound(vehicle).orElseThrow().repeatTicks());
        vehicle.engineSound = "";
        assertTrue(EntityTypePropertySupport.engineSound(vehicle).isEmpty());
        assertTrue(EntityTypePropertySupport.engineSound(new TestMecha()).isEmpty());
        assertTrue(EntityTypePropertySupport.engineSound(new TestAA()).isEmpty());
    }

    @Test
    void idleSoundFallsBackThroughStartAndMovementAndUsesAuthoredPitchCurve()
    {
        VehicleType vehicle = new VehicleType()
        {
            @Override
            public float getEngineSoundRange()
            {
                return 90F;
            }
        };
        vehicle.idleSound = "idle";
        vehicle.idleSoundLength = 30;
        vehicle.startSound = "start";
        vehicle.startSoundLength = 40;
        vehicle.engineSound = "engine";
        vehicle.engineSoundLength = 60;
        assertEquals("idle", EntityTypePropertySupport.engineIdleSound(vehicle).orElseThrow().sound());
        assertEquals(30, EntityTypePropertySupport.engineIdleSound(vehicle).orElseThrow().repeatTicks());
        vehicle.idleSound = "";
        assertEquals("start", EntityTypePropertySupport.engineIdleSound(vehicle).orElseThrow().sound());
        assertEquals(40, EntityTypePropertySupport.engineIdleSound(vehicle).orElseThrow().repeatTicks());
        vehicle.startSound = "";
        assertEquals(60, EntityTypePropertySupport.engineIdleSound(vehicle).orElseThrow().repeatTicks());
        vehicle.engineSoundPitchBase = 0.6F;
        vehicle.engineSoundPitchAt50 = 0.9F;
        vehicle.engineSoundPitchAt100 = 1.6F;
        assertEquals(0.6F, EntityTypePropertySupport.enginePitch(vehicle, 0F));
        assertEquals(0.9F, EntityTypePropertySupport.enginePitch(vehicle, 0.5F));
        assertEquals(1.6F, EntityTypePropertySupport.enginePitch(vehicle, 1F));
        assertTrue(EntityTypePropertySupport.engineIdleSound(new TestMecha()).isEmpty());
    }

    @Test
    void aaPropertiesExposeHealthTargetingCadenceAndTheDefaultVelocityButNoAmbientSounds()
    {
        var properties = EntityTypePropertySupport.read(new TestAA(), false).orElseThrow();
        assertEquals(250, properties.health().orElseThrow());
        assertEquals(80, properties.targetRange().orElseThrow());
        assertEquals(2, properties.shootDelay().orElseThrow());
        assertEquals(10, properties.weapons().get(0).damage());
        assertEquals(BulletType.DEFAULT_BULLET_SPEED, properties.weapons().get(0).speed().orElseThrow());
        assertTrue(properties.sounds().isEmpty());
        assertTrue(properties.takesFallDamage().isEmpty());
    }

    @Test
    void onlyAVehicleThatSquashesMobsDealsTheDriveablesDamageAboveTheSquashingSpeed()
    {
        TestVehicle vehicle = new TestVehicle();
        assertTrue(EntityTypePropertySupport.squashDamage(vehicle, 0.5D).isEmpty());
        vehicle.squashMobs = true;
        assertTrue(EntityTypePropertySupport.squashDamage(vehicle, 0.1D).isEmpty());
        assertEquals(8D, EntityTypePropertySupport.squashDamage(vehicle, 0.5D).orElseThrow(), 1.0E-9D);
        assertEquals(40D, EntityTypePropertySupport.squashDamage(vehicle, 10D).orElseThrow(), 1.0E-9D);
        assertTrue(EntityTypePropertySupport.squashDamage(new TestAA(), 0.5D).isEmpty());
    }

    @Test
    void vehicleAndPlaneBanksExposeTheirOwnValuesAndTotalPartHealth()
    {
        TestVehicle vehicle = new TestVehicle();
        var primary = EntityTypePropertySupport.read(vehicle, false).orElseThrow();
        var secondary = EntityTypePropertySupport.read(vehicle, true).orElseThrow();
        assertEquals(300, primary.health().orElseThrow());
        assertEquals(2, primary.weapons().get(0).damage());
        assertEquals(3, secondary.weapons().get(0).damage());
        assertEquals("flansmod:primary", primary.weapons().get(0).firingSound());
        assertEquals("flansmod:secondary", secondary.weapons().get(0).firingSound());
        assertEquals(4, primary.shootDelay().orElseThrow());
        assertEquals(8, secondary.shootDelay().orElseThrow());
        assertEquals("flansmod:idle", primary.sounds().get(Sound.IDLE));
        assertEquals("flansmod:engine", primary.sounds().get(Sound.STEP));
        assertTrue(primary.targetRange().isEmpty());
        assertTrue(primary.walkingSpeed().isEmpty());
        assertTrue(EntityTypePropertySupport.read(new PlaneType(), false).isPresent());
        assertTrue(EntityTypePropertySupport.read(new GunType(), false).isEmpty());
    }

    @Test
    void mechaPropertiesUseResolvedMovementAndItsOwnFallAndStepSettings()
    {
        var properties = EntityTypePropertySupport.read(new TestMecha(), false).orElseThrow();
        assertEquals(0.43, properties.walkingSpeed().orElseThrow(), 1.0E-6);
        assertEquals(12, properties.meleeReach().orElseThrow());
        assertFalse(properties.takesFallDamage().orElseThrow());
        assertTrue(properties.worksUnderWater().orElseThrow());
        assertEquals("flansmod:stomp", properties.sounds().get(Sound.STEP));
    }

    @Test
    void aaReloadCycleCountsTriggersByHowTheBarrelsShareAndSpendAmmunition()
    {
        TestAA aa = new TestAA();
        aa.numBarrels = 4;
        aa.reloadTime = 60;
        // Four magazines, one per barrel, all fired together: a magazine lasts as many triggers as it holds rounds.
        var together = EntityTypePropertySupport.reloadCycle(aa, false, 10, 1F).orElseThrow();
        assertEquals(10, together.volleys());
        assertEquals(2, together.shotDelay());
        assertEquals(60, together.reloadTicks());
        assertTrue(together.chamberSoundTick().isEmpty());
        // Alternating barrels spend one round per trigger, so the four magazines last four times as long.
        aa.fireAlternately = true;
        assertEquals(40, EntityTypePropertySupport.reloadCycle(aa, false, 10, 1F).orElseThrow().volleys());
        // One shared magazine feeds one barrel per trigger...
        aa.shareAmmo = true;
        assertEquals(10, EntityTypePropertySupport.reloadCycle(aa, false, 10, 1F).orElseThrow().volleys());
        // ...or all four at once.
        aa.fireAlternately = false;
        assertEquals(3, EntityTypePropertySupport.reloadCycle(aa, false, 10, 1F).orElseThrow().volleys());
        // Heavy ammunition slows the reload, and a reload is never shorter than the firing interval.
        assertEquals(90, EntityTypePropertySupport.reloadCycle(aa, false, 10, 1.5F).orElseThrow().reloadTicks());
        aa.reloadTime = 0;
        assertEquals(2, EntityTypePropertySupport.reloadCycle(aa, false, 0, 1F).orElseThrow().reloadTicks());
        assertEquals(1, EntityTypePropertySupport.reloadCycle(aa, false, 0, 1F).orElseThrow().volleys());
    }

    @Test
    void driveableReloadCycleUsesTheSmallerOfDeclaredRoundsAndTheItemAndTimesTheChamberSound()
    {
        TestVehicle vehicle = new TestVehicle();
        assertTrue(EntityTypePropertySupport.reloadCycle(vehicle, false, 5, 1F).isEmpty());
        vehicle.primary = EnumWeaponType.SHELL;
        vehicle.secondary = EnumWeaponType.GUN;
        vehicle.reloadTimePrimary = 100;
        vehicle.reloadTimeSecondary = 50;
        var shell = EntityTypePropertySupport.reloadCycle(vehicle, false, 1, 2F).orElseThrow();
        assertEquals(1, shell.volleys());
        assertEquals(4, shell.shotDelay());
        // Ordnance chambers at its own pace whatever the round's reload multiplier.
        assertEquals(100, shell.reloadTicks());
        assertTrue(shell.chamberSoundTick().isEmpty());
        vehicle.shootReloadSound = "flansmod:chamber";
        vehicle.reloadSoundTick = 30;
        assertEquals(30, EntityTypePropertySupport.reloadCycle(vehicle, false, 1, 1F).orElseThrow().chamberSoundTick().orElseThrow());
        var belt = EntityTypePropertySupport.reloadCycle(vehicle, true, 250, 2F).orElseThrow();
        assertEquals(250, belt.volleys());
        assertEquals(8, belt.shotDelay());
        // A bank of mounted guns does slow down for heavy ammunition, and never sounds like a shell being chambered.
        assertEquals(100, belt.reloadTicks());
        assertTrue(belt.chamberSoundTick().isEmpty());
        vehicle.reloadRoundsSecondary = 100;
        assertEquals(100, EntityTypePropertySupport.reloadCycle(vehicle, true, 250, 1F).orElseThrow().volleys());
        assertEquals(40, EntityTypePropertySupport.reloadCycle(vehicle, true, 40, 1F).orElseThrow().volleys());
        assertTrue(EntityTypePropertySupport.reloadCycle(new GunType(), false, 5, 1F).isEmpty());
    }

    private static final class TestAA extends AAGunType
    {
        TestAA()
        {
            originalShortName = "aa";
            health = 250;
            targetRange = 80;
            roundsPerMin = 600;
            damage = 10;
        }
    }

    private static final class TestVehicle extends VehicleType
    {
        TestVehicle()
        {
            originalShortName = "vehicle";
            health.put(EnumDriveablePart.CORE, new CollisionBox(200, 0, 0, 0, 16, 16, 16));
            health.put(EnumDriveablePart.TURRET, new CollisionBox(100, 0, 0, 0, 16, 16, 16));
            damageMultiplierPrimary = 2;
            damageMultiplierSecondary = 3;
            shootDelayPrimary = 4;
            shootDelaySecondary = 8;
            shootSoundPrimary = "flansmod:primary";
            shootSoundSecondary = "flansmod:secondary";
            idleSound = "flansmod:idle";
            engineSound = "flansmod:engine";
        }
    }

    private static final class TestMecha extends MechaType
    {
        TestMecha()
        {
            originalShortName = "mecha";
            moveSpeed = 2;
            reach = 12;
            takeFallDamage = false;
            worksUnderWater = true;
            stompSound = "flansmod:stomp";
        }
    }
}
