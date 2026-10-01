package com.flansmodultimate.common.types;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DebugHitboxTest
{
    @Test void setupPartGeometryRoundTripsForVehiclesAndPlanes()
    {
        float[] pixels = {-13F, 7F, -29F, 31F, 19F, 67F};
        for (DriveableType type : new DriveableType[] {new VehicleType(), new PlaneType(), new MechaType()})
        {
            CollisionBox box = type.debugHitboxFromPixels(100F, pixels, 9F, 0.5F);
            assertArrayEquals(pixels, type.debugHitboxPixels(box), 0.0001F);
            assertEquals(9F, box.getPenetrationResistance());
            assertEquals(0.5F, box.getCrewDamageMultiplier());
        }
    }

    @Test void resetRestoresOriginalBoxesAfterRepeatedEditsAndRemoval()
    {
        DriveableType type = new VehicleType();
        CollisionBox original = new CollisionBox(100F, 0, 0, 0, 16, 16, 16);
        type.getHealth().put(EnumDriveablePart.CORE, original);
        type.setDebugHitboxes(Map.of(EnumDriveablePart.GENERIC_0, original));
        type.setDebugHitboxes(Map.of());
        type.resetDebugHitboxes();
        assertEquals(Map.of(EnumDriveablePart.CORE, original), type.getHealth());
        assertEquals(3L, type.getDebugHitboxRevision());
        type.resetDebugHitboxes();
        assertEquals(3L, type.getDebugHitboxRevision());
    }

    @Test void geometryEditsAndRemovalDoNotRepairOrDestroyParts()
    {
        CollisionBox box = new CollisionBox(100F, 0, 0, 0, 16, 16, 16);
        DriveablePart part = new DriveablePart(EnumDriveablePart.CORE, box);
        part.damage(25F, false);
        part.updateDebugBox(new CollisionBox(200F, 4, 3, 2, 32, 16, 16));
        assertEquals(150F, part.getHealth());
        part.updateDebugBox(null);
        assertNull(part.getBox());
        assertFalse(part.isDestroyed());
        part.updateDebugBox(box);
        assertEquals(75F, part.getHealth());
        part.damage(100F, false);
        part.updateDebugBox(box);
        assertTrue(part.isDestroyed());
    }
}
