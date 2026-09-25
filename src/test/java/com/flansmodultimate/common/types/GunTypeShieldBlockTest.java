package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code ShieldBlockChance} decides whether a shield stops a frontal melee hit outright, up to the melee
 * damage of {@code ShieldMaxBlockableMeleeDamage}.
 */
class GunTypeShieldBlockTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));
    private static final float EPSILON = 1.0E-6F;

    @Test
    void anUnauthoredShieldBlocksHalfTheHitsOfEveryBladeButEnergyOnes()
    {
        GunType type = read("ShortName testShield", "Shield 1.0 4.0 -20.0 -8.0 1.0 32.0 16.0");

        assertEquals(0.5F, type.getShieldBlockChance(), EPSILON);
        assertEquals(10F, type.getShieldMaxBlockableMeleeDamage(), EPSILON);
    }

    @Test
    void authoredValuesAreRead()
    {
        GunType type = read("ShortName testShield", "ShieldBlockChance 0.6", "ShieldMaxBlockableMeleeDamage 12");

        assertEquals(0.6F, type.getShieldBlockChance(), EPSILON);
        assertEquals(12F, type.getShieldMaxBlockableMeleeDamage(), EPSILON);
    }

    @Test
    void theChanceIsClampedToAProbability()
    {
        assertEquals(1F, read("ShortName testShield", "ShieldBlockChance 1.5").getShieldBlockChance(), EPSILON);
        assertEquals(0F, read("ShortName testShield", "ShieldBlockChance -1").getShieldBlockChance(), EPSILON);
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testShield", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
