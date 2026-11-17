package com.wolffsmod.npcs.combat;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.Test;

import net.minecraft.world.damagesource.DamageSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void anNpcImmuneToItsOwnBlastIgnoresOnlyExplosionsItSetOffItself() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        EntityNPCInterface other = mock(EntityNPCInterface.class);
        DamageSource blast = mock(DamageSource.class);
        when(blast.getMsgId()).thenReturn("explosion");
        when(blast.getEntity()).thenReturn(npc);
        DamageSource gunfire = mock(DamageSource.class);
        when(gunfire.getMsgId()).thenReturn("thrown");
        when(gunfire.getEntity()).thenReturn(npc);
        try (var safety = mockStatic(NpcBlastSafety.class))
        {
            safety.when(() -> NpcBlastSafety.ignoresOwnBlast(npc)).thenReturn(false);
            assertFalse(NpcProtection.ownBlast(npc, blast), "an NPC that keeps its distance is still caught by its own blast");
            safety.when(() -> NpcBlastSafety.ignoresOwnBlast(npc)).thenReturn(true);
            assertTrue(NpcProtection.ownBlast(npc, blast));
            assertFalse(NpcProtection.ownBlast(npc, gunfire), "its own gunfire is no explosion");
            when(blast.getEntity()).thenReturn(other);
            assertFalse(NpcProtection.ownBlast(npc, blast), "another's explosion still hurts it");
        }
    }

    @Test
    void knockbackFollowsTheModelUnlessTheNpcOverridesIt()
    {
        assertFalse(NpcProtection.knockbackImmune(Knockback.MODEL));

        assertTrue(NpcProtection.knockbackImmune(Knockback.IMMUNE));
        assertFalse(NpcProtection.knockbackImmune(Knockback.NORMAL));
    }
}
