package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.ContentPack;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleOpticsTest
{
    private final List<SeatInfo> seats = List.of(seat(0), seat(1));
    private final List<String> warnings = new ArrayList<>();

    private static SeatInfo seat(int id)
    {
        return new SeatInfo(id, new Vector3f(), EnumDriveablePart.CORE, id == 0,
            -180, 180, -45, 45, null, null);
    }

    private VehicleOptics read(String... lines)
    {
        var pack = new ContentPack("test", Path.of("build", "test-packs", "optics"));
        return VehicleOpticsReader.read(new TypeFile("optics", EnumType.VEHICLE, pack, List.of(lines)), seats, warnings::add);
    }

    @Test void driverListsAndThermalIndicesMatchLegacy()
    {
        VehicleOptics optics = read("HasScope true", "Gunsight day night thermal", "GunsightZoom 2 4", "ThermalGuis 3");
        assertTrue(optics.available());
        assertEquals(3, optics.sightCount());
        assertEquals("thermal", optics.overlay(2));
        assertEquals(4F, optics.zoom(2));
        assertFalse(optics.thermal(1));
        assertTrue(optics.thermal(2));
        assertTrue(warnings.isEmpty());
    }

    @Test void driverCycleCountComesFromOverlaysButPassengerCountIncludesZoomsAndThermal()
    {
        VehicleOptics driver = read("Gunsight day", "GunsightZoom 2 4 8", "ThermalGuis 4",
            "SeatGunsight 1 day", "SeatGunsightZooms 1 2 4 8", "SeatThermalGUIs 1 4");
        assertEquals(1, driver.sightCount());
        assertEquals(4, seats.get(1).getOptics().sightCount());
    }

    @Test void aliasesAndRepeatedLinesApplyInSourceOrder()
    {
        VehicleOptics driver = read("  hasScope true", "HasScope false", "gunsightZoom 2", "GunsightZoom 6",
            "PassengerHasScope 1 true", "SeatHasScope 1 false", "PassengerZoom 1 3",
            "PassengerNightSight 1 true", "nightScope true");
        assertFalse(driver.available());
        assertEquals(6F, driver.zoom(0));
        assertTrue(driver.isNightSight());
        assertFalse(seats.get(1).getOptics().isHasScope());
        assertEquals(3F, seats.get(1).getOptics().zoom(0));
        assertTrue(seats.get(1).getOptics().isNightSight());
    }

    @Test void seatGunsightAutoEnablesOverlayUntilOpticsModeIsDeclared()
    {
        read("PassengerGunsights 1 tv flir", "SeatThermalGuis 1 2", "OpticsModeSeat 1 true");
        VehicleOptics optics = seats.get(1).getOptics();
        assertTrue(optics.available());
        assertTrue(optics.isOpticsMode());
        assertFalse(optics.forced());
        assertTrue(optics.thermal(1));
    }

    @Test void seatGunsightStartsScopedButOnlyAutoScopeIsForced()
    {
        read("SeatGunsight 1 sight", "SeatAutoScope 0 true");
        assertTrue(seats.get(1).getOptics().startsActive());
        assertFalse(seats.get(1).getOptics().forced());
        assertTrue(seats.get(0).getOptics().startsActive());
        assertTrue(seats.get(0).getOptics().forced());
    }

    @Test void overlaySeatStartsScopedOnEntryAndTheGunnerMayLowerIt()
    {
        read("SeatGunsight 1 sight");
        VehicleOptics definition = seats.get(1).getOptics();
        OpticsState state = new OpticsState();
        state.update(definition, true, 1, false, false);
        assertTrue(state.isActive());
        state.update(definition, true, 2, true, false);
        assertFalse(state.isActive());
        for (int tick = 3; tick < 40; tick++)
            state.update(definition, true, tick, false, false);
        assertFalse(state.isActive());
        state.update(definition, true, 40, true, false);
        assertTrue(state.isActive());
        // Leaving the seat resets it, so the next gunner starts scoped again.
        state.update(definition, false, 41, false, false);
        state.update(definition, true, 42, false, false);
        assertTrue(state.isActive());
    }

    @Test void autoScopeCannotBeLowered()
    {
        read("SeatAutoScope 1 true");
        VehicleOptics definition = seats.get(1).getOptics();
        OpticsState state = new OpticsState();
        state.update(definition, true, 1, false, false);
        state.update(definition, true, 20, true, false);
        assertTrue(state.isActive());
    }

    @Test void camerasUsePixelsAndSeparateEnableFlags()
    {
        VehicleOptics driver = read("GunsightPos 16 32 -48", "HasGunsightPos true",
            "PilotOptics true", "DriverOpticsCamera 64 80 96", "SeatOpticsCamera 1 8 24 40");
        assertTrue(driver.isHasCamera());
        assertEquals(1F, driver.getCamera().x);
        assertEquals(-3F, driver.getCamera().z);
        assertTrue(seats.get(0).getOptics().isOpticsMode());
        assertEquals(4F, seats.get(0).getOptics().getCamera().x);
        assertEquals(2.5F, seats.get(1).getOptics().getCamera().z);
    }

    @Test void sharedHelicopterOverlayDoesNotEnableScopes()
    {
        read("PassengerGunsight legacy", "SeatOverlay 1 true");
        assertEquals("legacy", seats.get(1).getOptics().overlay(0));
        assertTrue(seats.get(1).getOptics().startsActive());
        assertFalse(seats.get(0).getOptics().available());
    }

    @Test void malformedSeatAndNonfiniteNumbersDoNotAbortLaterLines()
    {
        VehicleOptics driver = read("SeatGunsight -1 bad", "PassengerZoom 1 NaN", "GunsightZoom Infinity",
            "SeatOpticsCamera 99 0 0 0", "HasScope true", "GunsightZoom 4");
        assertEquals(4, warnings.size());
        assertTrue(driver.available());
        assertEquals(4F, driver.zoom(0));
        assertEquals(1F, seats.get(1).getOptics().zoom(0));
    }

    @Test void hudOverridesUseOneBasedSightAndFallbackToDefaults()
    {
        read("SeatOpticsHUD 1 true", "SeatOpticsHUDColor 1 255 128 0", "SeatOpticsHUDColorSight 1 2 0x112233",
            "SeatOpticsHUDScale 1 1.5", "SeatOpticsHUDScaleSight 1 2 2",
            "SeatOpticsRangePos 1 -20 30", "SeatOpticsRangePosSight 1 2 -40 60",
            "SeatOpticsRangeTextureSight 1 2 range", "SeatOpticsRangeTextureSize 1 90 20",
            "SeatOpticsCompassMarkerSight 1 2 marker", "SeatOpticsElevationIndicator 1 true");
        OpticsHud hud = seats.get(1).getOptics().getHud();
        assertTrue(hud.isEnabled());
        assertEquals(0xFF8000, hud.color(0));
        assertEquals(0x112233, hud.color(1));
        assertEquals(1.5F, hud.scale(0));
        assertEquals(2F, hud.scale(1));
        assertEquals(-20, hud.getElements()[0].x(0));
        assertEquals(-40, hud.getElements()[0].x(1));
        assertEquals("range", hud.getElements()[0].texture(1));
        assertEquals(90, hud.getElements()[0].getWidth());
        assertEquals("marker", hud.marker(1));
        assertTrue(hud.isElevationIndicator());
    }

    @Test void pilotHudDefaultsAndExplicitInheritanceRemainDistinct()
    {
        read("PilotOpticsHUD true", "SeatOpticsHUDInherit 1 NONE");
        assertTrue(seats.get(0).getOptics().getHud().isEnabled());
        assertFalse(seats.get(0).getOptics().getHud().isOverridePilotDefaults());
        assertEquals(-1, seats.get(1).getOptics().getHud().getInheritSeat());
        assertTrue(seats.get(1).getOptics().getHud().isOverridePilotDefaults());
    }

    @Test void independentOccupantsDoNotShareScopeOrSightState()
    {
        VehicleOptics definition = read("HasScope true", "Gunsight tv flir", "ThermalGuis 2");
        OpticsState first = new OpticsState(), second = new OpticsState();
        first.update(definition, true, 100, true, true);
        second.update(definition, true, 100, false, false);
        assertTrue(first.isActive());
        assertTrue(first.isThermal());
        assertEquals(1, first.getSight());
        assertFalse(second.isActive());
        assertEquals(0, second.getSight());
        assertEquals("tv", definition.overlay(0));
    }

    @Test void toggleCooldownUsesServerTicksAndPermissionRevocationResets()
    {
        VehicleOptics definition = read("HasScope true");
        OpticsState state = new OpticsState();
        state.update(definition, true, 10, true, false);
        for (int i = 0; i < 20; i++) state.update(definition, true, 10, true, false);
        assertTrue(state.isActive());
        state.update(definition, true, 20, true, false);
        assertFalse(state.isActive());
        state.update(definition, true, 30, true, false);
        state.update(definition, false, 31, false, false);
        assertFalse(state.isActive());
        assertEquals(0, state.getSight());
    }

    @Test void forcedScopeStartsOnThermalChannelAndSingleSightThermalCanToggle()
    {
        read("SeatGunsight 1 flir", "SeatThermalGuis 1 1");
        VehicleOptics definition = seats.get(1).getOptics();
        OpticsState state = new OpticsState();
        state.update(definition, true, 1, false, false);
        assertTrue(state.isActive());
        assertTrue(state.isThermal());
        state.update(definition, true, 2, false, true);
        assertFalse(state.isThermal());
        state.update(definition, true, 5, false, true);
        assertTrue(state.isThermal());
        state.reset();
        assertFalse(state.isActive());
        assertFalse(state.isThermal());
    }

    @Test void cycleDebouncesAndWraps()
    {
        VehicleOptics definition = read("HasScope true", "Gunsight a b c");
        OpticsState state = new OpticsState();
        state.update(definition, true, 0, true, true);
        state.update(definition, true, 0, false, true);
        assertEquals(1, state.getSight());
        state.update(definition, true, 3, false, true);
        assertEquals(2, state.getSight());
        state.update(definition, true, 6, false, true);
        assertEquals(0, state.getSight());
    }

    @Test void opticsIntentsAreEdgesAndUnknownBitsAreRejected()
    {
        assertEquals(DriveableInput.TOGGLE_SCOPE | DriveableInput.CYCLE_SIGHT,
            DriveableInput.sanitize(DriveableInput.TOGGLE_SCOPE | DriveableInput.CYCLE_SIGHT | (1 << 30)));
        assertEquals(0, DriveableInput.CONTINUOUS_MASK & (DriveableInput.TOGGLE_SCOPE | DriveableInput.CYCLE_SIGHT));
    }
}
