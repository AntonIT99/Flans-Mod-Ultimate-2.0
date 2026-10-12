package com.flansmodultimate;

import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.wolffsmod.npcs.model.FlanModelFolder;
import com.wolffsmod.npcs.model.FlanModelKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipCreativeTabTest
{
    private static class WaterVehicle extends VehicleType
    {
        WaterVehicle(boolean amphibious)
        {
            placeableOnWater = true;
            floatOnWater = true;
            placeableOnLand = amphibious;
        }
    }

    private static class WaterPlane extends PlaneType
    {
        WaterPlane(boolean flightControls)
        {
            placeableOnWater = true;
            floatOnWater = true;
            placeableOnLand = false;
            lookUpModifier = flightControls ? 1F : 0F;
            lookDownModifier = lookUpModifier;
            rollLeftModifier = lookUpModifier;
            rollRightModifier = lookUpModifier;
        }
    }

    @Test
    void boatsMoveButAmphibiousVehiclesStay()
    {
        assertTrue(new WaterVehicle(false).isShip());
        assertFalse(new WaterVehicle(true).isShip());
        assertFalse(new VehicleType().isShip());
    }

    @Test
    void legacyPlaneShipsMoveButSeaplanesStay()
    {
        assertTrue(new WaterPlane(false).isShip());
        assertFalse(new WaterPlane(true).isShip());
    }

    @Test
    void browserMovesShipsWithoutChangingTheirEntityKind()
    {
        WaterVehicle boat = new WaterVehicle(false);
        WaterPlane legacyShip = new WaterPlane(false);
        assertEquals(FlanModelFolder.SHIP, FlanModelFolder.of(boat));
        assertEquals(FlanModelFolder.SHIP, FlanModelFolder.of(legacyShip));
        assertEquals(FlanModelFolder.VEHICLE, FlanModelFolder.of(new WaterVehicle(true)));
        assertEquals(FlanModelFolder.PLANE, FlanModelFolder.of(new WaterPlane(true)));
        assertEquals(FlanModelKind.VEHICLE, FlanModelKind.of(boat));
        assertEquals(FlanModelKind.PLANE, FlanModelKind.of(legacyShip));
    }
}
