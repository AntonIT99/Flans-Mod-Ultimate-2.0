package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrenadeTypeVanillaExplosionTest
{
    @Test
    void existingGrenadesKeepMassBasedExplosionsByDefault()
    {
        GrenadeType type = grenade("ExplosiveMassTNTKg 3");
        assertFalse(type.isUseVanillaExplosion());
        assertTrue(type.useNewExplosionSystem());
    }

    @Test
    void vanillaStrengthUsesRadiusEvenWhenExplosiveMassIsPresent()
    {
        GrenadeType type = grenade("UseVanillaExplosion true", "ExplosionRadius 3", "ExplosiveMassTNTKg 100");
        assertTrue(type.isUseVanillaExplosion());
        assertFalse(type.useNewExplosionSystem());
        assertEquals(3F, type.getExplosionRadius());
    }

    @Test
    void explicitFalseKeepsMassBasedExplosions()
    {
        GrenadeType type = grenade("UseVanillaExplosion false", "ExplosiveMassTNTKg 3");
        assertFalse(type.isUseVanillaExplosion());
        assertTrue(type.useNewExplosionSystem());
    }

    private static GrenadeType grenade(String... lines)
    {
        GrenadeType type = new GrenadeType();
        type.load(new TypeFile("testGrenade", EnumType.GRENADE, new ContentPack("test", Path.of("build", "test-packs", "grenades")), List.of(lines)));
        return type;
    }
}
