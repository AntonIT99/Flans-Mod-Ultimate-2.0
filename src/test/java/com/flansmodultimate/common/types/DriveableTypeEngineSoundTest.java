package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableTypeEngineSoundTest
{
    private static final com.flansmodultimate.common.driveables.EngineSoundPitch VEHICLE_DEFAULTS = new com.flansmodultimate.common.driveables.EngineSoundPitch(0.5F, 0.8F, 1.2F);
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "engine-sounds"));

    @Test
    void readsEngineStartupTimingAndPitchConfiguration()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0", "StartEngineSoundLength 37", "EngineSoundPitchRange 1.2", "EngineSoundPitchBase 0.4")));

        assertEquals(37, type.getStartEngineSoundLength());
        assertEquals(1.2F, type.getEngineSoundPitchRange());
        assertEquals(0.4F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).base());
        assertEquals(1.6F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).full());
    }

    @Test
    void usesVehicleClientPitchDefaultsWhenNotAuthored()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0")));

        assertEquals(VEHICLE_DEFAULTS, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS));
    }

    @Test
    void rangeWithoutBaseStartsAtClientDefaultBase()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0", "EngineSoundPitchRange 1.0")));

        assertEquals(0.5F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).base());
        assertEquals(1.0F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).half());
        assertEquals(1.5F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).full());
    }

    @Test
    void authoredHalfAndFullPitchOverrideTheClientDefaults()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0", "EngineSoundPitchBase 0.6", "EngineSoundPitchAt50 0.9", "EngineSoundPitchAt100 1.4")));

        assertEquals(0.6F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).base());
        assertEquals(0.9F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).half());
        assertEquals(1.4F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS).full());
    }

    @Test
    void vehicleUsesDedicatedStartupThenPrioritizedIdleLoop()
    {
        VehicleType type = new VehicleType();
        type.startSound = "legacy_idle";
        type.startSoundLength = 41;
        type.startEngineSound = "ignition";
        type.startEngineSoundLength = 17;
        type.idleSound = "proper_idle";
        type.engineSound = "running";

        assertEquals("ignition", type.getEngineStartupSound());
        assertEquals(17, type.getEngineStartupSoundLength());
        assertEquals("proper_idle", type.getEngineIdleLoopSound());
        assertFalse(type.usesEngineSoundAsIdleLoop());
        assertEquals(0F, type.getEngineIdleLoopPitchRange());
    }

    @Test
    void vehicleFallsBackToLoopingLegacyStartSoundAtIdle()
    {
        VehicleType type = new VehicleType();
        type.startSound = "legacy_idle";
        type.engineSound = "running";

        assertEquals("legacy_idle", type.getEngineIdleLoopSound());
        assertFalse(type.usesEngineSoundAsIdleLoop());
    }

    @Test
    void vehicleWithOnlyStartupAndEngineSoundsLoopsEngineAtBasePitchWhileIdle()
    {
        VehicleType type = new VehicleType();
        type.startEngineSound = "ignition";
        type.engineSound = "running";
        type.startSound = " ";
        type.idleSound = "";
        type.engineSoundPitchBase = 0.65F;

        assertEquals("ignition", type.getEngineStartupSound());
        assertEquals("running", type.getEngineIdleLoopSound());
        assertTrue(type.usesEngineSoundAsIdleLoop());
        assertEquals(0.65F, com.flansmodultimate.common.driveables.physics.DriveableControlPhysics.engineSoundPitch(0F, type.getEngineSoundPitchCurve(VEHICLE_DEFAULTS), 1F));
    }

    @Test
    void planePrioritizesDedicatedStartupAndIdleSounds()
    {
        PlaneType type = new PlaneType();
        type.startSound = "prop_start";
        type.startSoundLength = 31;
        type.startEngineSound = "ignition";
        type.startEngineSoundLength = 19;
        type.idleSound = "engine_idle";
        type.engineSound = "propeller";

        assertEquals("ignition", type.getEngineStartupSound());
        assertEquals(19, type.getEngineStartupSoundLength());
        assertEquals("engine_idle", type.getEngineIdleLoopSound());
        assertEquals(0F, type.getEngineIdleLoopPitchRange());
        assertEquals("propeller", type.getEngineSound());
    }

    @Test
    void planeFallsBackToStartSoundOnceThenLowestPitchPropLoop()
    {
        PlaneType type = new PlaneType();
        type.startSound = "prop_start";
        type.startSoundLength = 31;
        type.engineSound = "propeller";
        type.engineSoundPitchRange = 1.1F;

        assertEquals("prop_start", type.getEngineStartupSound());
        assertEquals(31, type.getEngineStartupSoundLength());
        assertEquals("propeller", type.getEngineIdleLoopSound());
        assertEquals(1.1F, type.getEngineIdleLoopPitchRange());
    }
}
