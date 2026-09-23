package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableTypeEngineSoundTest
{
    private static final IContentProvider PACK = new ContentPack("test",
        Path.of("build", "test-packs", "engine-sounds"));

    @Test
    void readsEngineStartupTimingAndPitchConfiguration()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of(
            "Driver 0 0 0",
            "StartEngineSoundLength 37",
            "EngineSoundPitchRange 1.2")));

        assertEquals(37, type.getStartEngineSoundLength());
        assertEquals(1.2F, type.getEngineSoundPitchRange());
    }

    @Test
    void usesSlightlyNarrowerDefaultPitchRange()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0")));

        assertEquals(0.8F, type.getEngineSoundPitchRange());
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

        assertEquals("ignition", type.getEngineStartupSound());
        assertEquals(17, type.getEngineStartupSoundLength());
        assertEquals("proper_idle", type.getEngineIdleLoopSound());
        assertEquals(0F, type.getEngineIdleLoopPitchRange());
    }

    @Test
    void vehicleFallsBackToLoopingLegacyStartSoundAtIdle()
    {
        VehicleType type = new VehicleType();
        type.startSound = "legacy_idle";

        assertEquals("legacy_idle", type.getEngineIdleLoopSound());
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
