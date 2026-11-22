package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.collision.CollisionBox;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AmmoRackCookOffTest
{
    private static final ContentPack PACK = new ContentPack("test", Path.of("build", "test-packs", "ammo-rack"));

    @Test
    void burnsThePartDownOverTwiceTheCookTime()
    {
        // 40 pulses of five ticks: ten seconds for a five-second cook time
        assertEquals(10F, AmmoRackCookOff.damagePerPulse(400F, 5F));
        assertEquals(1F, AmmoRackCookOff.damagePerPulse(2F, 10F));
        assertEquals(AmmoRackCookOff.damagePerPulse(400F, 0.25F), AmmoRackCookOff.damagePerPulse(400F, Float.NaN));
    }

    @Test
    void destroyedRackBurnsTheCoreBeforeTheTurret()
    {
        Map<EnumDriveablePart, DriveablePart> parts = new EnumMap<>(EnumDriveablePart.class);
        parts.put(EnumDriveablePart.CORE, part(EnumDriveablePart.CORE, 200F));
        parts.put(EnumDriveablePart.TURRET, part(EnumDriveablePart.TURRET, 100F));
        DriveablePart hullRack = part(EnumDriveablePart.WEAK_SPOT_2, 15F);
        DriveablePart turretRack = part(EnumDriveablePart.TURRET_WEAK, 15F);
        parts.put(EnumDriveablePart.WEAK_SPOT_2, hullRack);
        parts.put(EnumDriveablePart.TURRET_WEAK, turretRack);
        assertNull(AmmoRackCookOff.burningPart(parts));

        turretRack.damage(15F, false);
        assertEquals(EnumDriveablePart.TURRET, AmmoRackCookOff.burningPart(parts));
        hullRack.damage(15F, false);
        assertEquals(EnumDriveablePart.CORE, AmmoRackCookOff.burningPart(parts));

        parts.get(EnumDriveablePart.CORE).damage(200F, false);
        assertEquals(EnumDriveablePart.TURRET, AmmoRackCookOff.burningPart(parts));
        parts.get(EnumDriveablePart.TURRET).damage(100F, false);
        assertNull(AmmoRackCookOff.burningPart(parts));
        assertNull(AmmoRackCookOff.burningPart(null));
    }

    @Test
    void readsWeakspotCookTime()
    {
        VehicleType defaults = new VehicleType();
        defaults.load(new TypeFile("rackDefault", EnumType.VEHICLE, PACK, List.of("ShortName rackDefault")));
        assertEquals(3F, defaults.getWeakspotCookTime());
        assertFalse(defaults.isTurretStabilization());

        VehicleType tuned = new VehicleType();
        tuned.load(new TypeFile("rackTuned", EnumType.VEHICLE, PACK, List.of("ShortName rackTuned", "WeakspotCookTime 10", "TurretStabilization True")));
        assertEquals(10F, tuned.getWeakspotCookTime());
        assertTrue(tuned.isTurretStabilization());
    }

    private static DriveablePart part(EnumDriveablePart type, float health)
    {
        return new DriveablePart(type, new CollisionBox(health, 0F, 0F, 0F, 1F, 1F, 1F));
    }
}
