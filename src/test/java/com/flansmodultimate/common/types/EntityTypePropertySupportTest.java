package com.flansmodultimate.common.types;

import com.flansmodultimate.api.EntityTypeProperties.Sound;
import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTypePropertySupportTest
{
    @Test
    void aaPropertiesExposeHealthTargetingAndCadenceButDoNotInventVelocityOrAmbientSounds()
    {
        var properties = EntityTypePropertySupport.read(new TestAA(), false).orElseThrow();
        assertEquals(250, properties.health().orElseThrow());
        assertEquals(80, properties.targetRange().orElseThrow());
        assertEquals(2, properties.shootDelay().orElseThrow());
        assertEquals(10, properties.weapons().get(0).damage());
        assertTrue(properties.weapons().get(0).speed().isEmpty());
        assertTrue(properties.sounds().isEmpty());
        assertTrue(properties.takesFallDamage().isEmpty());
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
