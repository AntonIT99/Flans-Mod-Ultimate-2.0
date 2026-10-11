package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcProtectionTest
{
    @Test
    void aHitOfExactlyTheMinimumCountsAndZeroDisablesTheMinimum()
    {
        assertTrue(NpcProtection.belowMinimum(10, 9.99F));
        assertFalse(NpcProtection.belowMinimum(10, 10F));
        assertFalse(NpcProtection.belowMinimum(10, 25F));
        assertFalse(NpcProtection.belowMinimum(0, 0.01F));
        assertFalse(NpcProtection.belowMinimum(0, 0F));
    }

    @Test
    void everyDamageKindUsesItsOwnResistanceOnTheCustomNpcsScale()
    {
        // 1 is neutral, 2 immune and 0 doubles the damage.
        assertEquals(5F, NpcProtection.resist(NpcDamageCategory.PROJECTILE, 1.5F, 1F, 1F, 10F));
        assertEquals(0F, NpcProtection.resist(NpcDamageCategory.MELEE, 1F, 2F, 1F, 10F));
        assertEquals(20F, NpcProtection.resist(NpcDamageCategory.EXPLOSION, 1F, 1F, 0F, 10F));
        assertEquals(10F, NpcProtection.resist(NpcDamageCategory.OTHER, 2F, 2F, 2F, 10F));
    }

    @Test
    void knockbackFollowsTheModelUnlessTheNpcOverridesIt()
    {
        assertTrue(NpcProtection.knockbackImmune(Knockback.MODEL, true));
        assertFalse(NpcProtection.knockbackImmune(Knockback.MODEL, false));
        assertTrue(NpcProtection.knockbackImmune(Knockback.IMMUNE, false));
        assertFalse(NpcProtection.knockbackImmune(Knockback.NORMAL, true));
    }
}
