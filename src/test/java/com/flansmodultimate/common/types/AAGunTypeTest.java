package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AAGunTypeTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "aaguns"));

    private static AAGunType read(String... lines)
    {
        AAGunType type = new AAGunType();
        type.read(new TypeFile("testAaGun", EnumType.AA_GUN, PACK, List.of(lines)));
        return type;
    }

    @Test
    void roundsPerMinuteOverridesLegacyShootDelay()
    {
        AAGunType type = read("ShortName testAaGun", "ShootDelay 8", "RoundsPerMin 600");
        assertEquals(2F, type.getShootDelay(), 1.0E-6F);
        assertEquals(600F, type.getRoundsPerMin());
    }

    @Test
    void legacyShootDelayRemainsWhenRoundsPerMinuteIsAbsent()
    {
        AAGunType type = read("ShortName testAaGun", "ShootDelay 8");
        assertEquals(8F, type.getShootDelay(), 1.0E-6F);
    }

    @Test
    void realisticHealthUsesMass()
    {
        AAGunType scaled = read("ShortName testAaGun", "Health 20", "RealMassKg 1000",
            "UseRealisticVehicleHealth true");
        assertTrue(scaled.isRealisticVehicleHealthEnabled());
        assertEquals(500, scaled.getHealth());
    }
}
