package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArmorPerformanceTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "armor-performance"));

    @Test
    void inspectionUsesTheActualBeltRoundAndWeaponOverridesBeforePlatformOverrides()
    {
        BulletType bullet = new BulletType();
        bullet.load(new TypeFile("testBelt", EnumType.BULLET, PACK, List.of("ShortName testBelt", "RoundsPerItem 2", "AddRound AP 1 45 0 800 30", "AddRound Ball 1 42 0 800 5")));
        TestVehicle platform = new TestVehicle();
        platform.readDefinition("AmmoPenetrationAt100m testBelt 90", "AmmoMass testBelt 200");
        TestVehicle gun = new TestVehicle();
        gun.readDefinition("AmmoPenetrationAt100m testBelt 60", "AmmoMass testBelt 50");
        var raw = ExternalProjectileSupport.armorPerformance(new FiredShot(null, bullet, null, null, 1));
        assertEquals(5D, raw.penetrationMm());
        assertEquals(42D, raw.projectileMassGrams());
        var fallback = ExternalProjectileSupport.armorPerformance(new FiredShot(null, bullet, null, null, 1, platform));
        assertEquals(90D, fallback.penetrationMm());
        assertEquals(200D, fallback.projectileMassGrams());
        var mounted = ExternalProjectileSupport.armorPerformance(new FiredShot(new FireableGun(gun, 1F, 0F, 3F, EnumSpreadPattern.CIRCLE), bullet, null, null, 1, platform));
        assertEquals(60D, mounted.penetrationMm());
        assertEquals(50D, mounted.projectileMassGrams());
    }

    private static final class TestVehicle extends VehicleType
    {
        private void readDefinition(String penetration, String mass)
        {
            read(new TypeFile("testVehicle", EnumType.VEHICLE, PACK, List.of("Driver 0 0 0", penetration, mass)));
        }
    }
}
