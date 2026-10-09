package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.ProjectileSources;
import com.flansmodultimate.api.ProjectileSources.Source;
import com.wolffsmod.npcs.client.RangedControlLocks.Page;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangedControlLocksTest
{
    @Test
    void ordinaryProjectilesOrDisabledIntegrationKeepBothPagesEditable()
    {
        var content = new ProjectileSources(Source.WEAPON, Source.WEAPON, Source.AMMUNITION, true);
        for (Page page : Page.values())
        {
            var locks = RangedControlLocks.create(page, false, content, true);
            assertTrue(locks.textFields().isEmpty());
            assertTrue(locks.buttons().isEmpty());
            assertTrue(locks.labels().isEmpty());
        }
    }

    @Test
    void rangeTimingTargetingAndShotCountRemainEditableWhenWeaponStatsAreActive()
    {
        var content = new ProjectileSources(Source.WEAPON, Source.WEAPON, Source.WEAPON, true);
        var locks = RangedControlLocks.create(Page.RANGED, true, content, true);
        assertEquals(Reason.WEAPON_STATS, locks.textFields().get(1));
        assertEquals(Reason.FIRING_SOUND, locks.textFields().get(7));
        assertEquals(Reason.FIRING_SOUND, locks.buttons().get(7));
        for (int id : new int[]{2, 3, 4, 5, 6, 8, 9})
            assertFalse(locks.textFields().containsKey(id));
        for (int id : new int[]{9, 13, 66})
            assertFalse(locks.buttons().containsKey(id));
    }

    @Test
    void disablingFiringSoundsOrMissingContentSoundRetainsTheNpcSoundFallback()
    {
        var withSound = new ProjectileSources(Source.CALLER, Source.CALLER, Source.CALLER, true);
        var silentWeapon = new ProjectileSources(Source.CALLER, Source.CALLER, Source.CALLER, false);
        for (var locks : new RangedControlLocks[]{RangedControlLocks.create(Page.RANGED, true, withSound, false), RangedControlLocks.create(Page.RANGED, true, silentWeapon, true)})
        {
            assertFalse(locks.textFields().containsKey(1));
            assertFalse(locks.textFields().containsKey(7));
            assertFalse(locks.buttons().containsKey(7));
            assertEquals(Reason.AMMUNITION, locks.textFields().get(10));
            assertEquals(Reason.AMMUNITION, locks.textFields().get(11));
        }
    }

    @Test
    void callerDamageAndSpeedStayEditableWhileNativeProjectileEffectsAreReadOnly()
    {
        var fallback = new ProjectileSources(Source.CALLER, Source.CALLER, Source.CALLER, false);
        var locks = RangedControlLocks.create(Page.PROJECTILE, true, fallback, false);
        assertFalse(locks.textFields().containsKey(1));
        assertFalse(locks.textFields().containsKey(4));
        for (int id : new int[]{2, 3, 5})
            assertEquals(Reason.AMMUNITION, locks.textFields().get(id));
        for (int id : new int[]{0, 1, 3, 4, 5, 6, 7, 8, 9, 10})
            assertEquals(Reason.AMMUNITION, locks.buttons().get(id));
        assertFalse(locks.buttons().containsKey(66));
    }

    @Test
    void ammunitionAndWeaponOverridesExplainWhichSettingActuallyControlsTheValue()
    {
        var content = new ProjectileSources(Source.AMMUNITION, Source.WEAPON, Source.WEAPON, true);
        var locks = RangedControlLocks.create(Page.PROJECTILE, true, content, true);
        assertEquals(Reason.AMMUNITION, locks.textFields().get(1));
        assertEquals(Reason.WEAPON_STATS, locks.textFields().get(4));
        assertEquals(Reason.AMMUNITION, locks.labels().get(1));
        assertEquals(Reason.WEAPON_STATS, locks.labels().get(4));
    }

    @Test
    void anAlternatingMixedBankKeepsAnyUsedFallbackEditable()
    {
        var weapon = new ProjectileSources(Source.WEAPON, Source.WEAPON, Source.WEAPON, false);
        var ammo = new ProjectileSources(Source.AMMUNITION, Source.CALLER, Source.CALLER, true);
        var combined = RangedControlLocks.combine(List.of(weapon, ammo));
        assertEquals(Source.WEAPON, combined.damage());
        assertEquals(Source.CALLER, combined.spread());
        assertEquals(Source.CALLER, combined.speed());
        assertFalse(combined.firingSound());
        assertFalse(RangedControlLocks.create(Page.PROJECTILE, true, combined, true).textFields().containsKey(4));
        assertFalse(RangedControlLocks.create(Page.RANGED, true, combined, true).textFields().containsKey(1));
    }
}
