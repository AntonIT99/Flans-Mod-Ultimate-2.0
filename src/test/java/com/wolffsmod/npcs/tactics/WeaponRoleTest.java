package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.EquippedWeaponProperties;
import com.flansmodultimate.api.EquippedWeaponProperties.Impact;
import com.flansmodultimate.api.WeaponTraits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponRoleTest
{
    private static final Impact BULLET = Impact.NONE;
    private static final Impact ROCKET = new Impact(1D, 0D, 1D, 3D, "", "", false);
    private static final WeaponTraits IRON_SIGHTS = new WeaponTraits(1.5F, 30);

    @Test
    void aWeaponIsSortedBySightMagazinePelletsAndExplosive()
    {
        assertEquals(WeaponRole.THROWN, WeaponRole.of(weapon(true, 1, false, BULLET), null));
        assertEquals(WeaponRole.CLOSE, WeaponRole.of(weapon(false, 8, false, BULLET), IRON_SIGHTS));
        assertEquals(WeaponRole.LAUNCHER, WeaponRole.of(weapon(false, 1, false, ROCKET), IRON_SIGHTS));
        assertEquals(WeaponRole.MARKSMAN, WeaponRole.of(weapon(false, 1, false, BULLET), new WeaponTraits(WeaponRole.MARKSMAN_ZOOM, 5)));
        assertEquals(WeaponRole.SUPPORT, WeaponRole.of(weapon(false, 1, true, BULLET), new WeaponTraits(1.5F, WeaponRole.SUPPORT_MAGAZINE)));
        assertEquals(WeaponRole.GENERAL, WeaponRole.of(weapon(false, 1, true, BULLET), IRON_SIGHTS), "an assault rifle");
        assertEquals(WeaponRole.GENERAL, WeaponRole.of(weapon(false, 1, false, BULLET), null), "a gun without traits, such as a bow");
    }

    @Test
    void eachRoleFightsAtAShareOfItsFiringRange()
    {
        for (WeaponRole role : WeaponRole.values())
        {
            assertTrue(role.share() > 0D && role.share() <= 1D, role.name());
        }
    }

    private static EquippedWeaponProperties weapon(boolean throwable, int projectiles, boolean automatic, Impact impact)
    {
        return new EquippedWeaponProperties(true, throwable, false, 10D, 2D, 3D, 1D, projectiles, 2D, 40D, 0, 0, "", automatic, 0F, 0F, impact);
    }
}
