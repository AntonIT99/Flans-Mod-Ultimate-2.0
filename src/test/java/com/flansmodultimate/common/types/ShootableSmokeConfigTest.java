package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShootableSmokeConfigTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "smoke"));

    @Test
    void bulletTypesReadTheSharedSmokeConfiguration()
    {
        assertSmokeConfiguration(load(new BulletType(), EnumType.BULLET));
    }

    @Test
    void grenadeTypesRetainTheSharedSmokeConfiguration()
    {
        assertSmokeConfiguration(load(new GrenadeType(), EnumType.GRENADE));
    }

    private static ShootableType load(ShootableType type, EnumType enumType)
    {
        type.load(new TypeFile("syntheticSmoke", enumType, PACK, List.of(
            "SmokeTime 240",
            "SmokeParticleType cloud",
            "SmokeParticlesCount 17",
            "SmokeRadius 6.5"
        )));
        return type;
    }

    private static void assertSmokeConfiguration(ShootableType type)
    {
        assertEquals(240, type.getSmokeTime());
        assertEquals("cloud", type.getSmokeParticleType());
        assertEquals(17, type.getSmokeParticlesCount());
        assertEquals(6.5F, type.getSmokeRadius());
    }
}
