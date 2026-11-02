package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcTacticStateTest
{
    @Test
    void safetyRemembersOtherAttackersWithoutReplacingTheFiringThreatOrFollowingHiddenMovement()
    {
        NpcTacticState state = new NpcTacticState();
        LivingEntity first = mock(LivingEntity.class);
        LivingEntity vehicle = mock(LivingEntity.class);
        when(first.isAlive()).thenReturn(true);
        when(vehicle.isAlive()).thenReturn(true);
        when(first.getEyePosition()).thenReturn(Vec3.ZERO);
        Vec3 reported = new Vec3(20D, 2D, 0D);
        when(vehicle.getEyePosition()).thenReturn(reported);
        state.threat(first, first, 0L, 100L);
        state.threat(vehicle, first, 1L, 101L);
        when(vehicle.getEyePosition()).thenReturn(new Vec3(30D, 2D, 0D));
        assertSame(first, state.threat(2L));
        assertEquals(2, state.threats(2L).size());
        assertEquals(reported, state.threats(2L).get(1).position());
        assertEquals(1, state.threats(100L).size());
        assertTrue(state.threats(101L).isEmpty());
    }

    @Test
    void aThreatIsRememberedUntilItsMemoryRunsOut()
    {
        NpcTacticState state = new NpcTacticState();
        LivingEntity sniper = mock(LivingEntity.class);
        state.threat(sniper, null, 0L, 300L);
        assertTrue(state.remembers(sniper, 299L));
        assertSame(sniper, state.threat(299L));
        assertFalse(state.remembers(sniper, 300L));
        assertNull(state.threat(300L));
    }

    @Test
    void anotherShooterDoesNotDisplaceTheThreatTheNpcIsFighting()
    {
        NpcTacticState state = new NpcTacticState();
        LivingEntity sniper = mock(LivingEntity.class);
        LivingEntity rifleman = mock(LivingEntity.class);
        state.threat(sniper, null, 0L, 300L);
        state.threat(rifleman, sniper, 100L, 400L);
        assertTrue(state.remembers(sniper, 100L));
        assertFalse(state.remembers(rifleman, 100L));
        // Once the NPC fights someone else, or the threat is forgotten, the latest shooter is remembered.
        state.threat(rifleman, null, 100L, 400L);
        assertTrue(state.remembers(rifleman, 100L));
        state.threat(sniper, rifleman, 500L, 800L);
        assertTrue(state.remembers(sniper, 500L));
    }

    @Test
    void theSameShooterIsRememberedAgainWithEveryShot()
    {
        NpcTacticState state = new NpcTacticState();
        LivingEntity sniper = mock(LivingEntity.class);
        state.threat(sniper, sniper, 0L, 300L);
        state.threat(sniper, sniper, 200L, 500L);
        assertTrue(state.remembers(sniper, 400L));
    }

    @Test
    void anNpcCallsOutAtMostOnceAnInterval()
    {
        NpcTacticState state = new NpcTacticState();
        assertTrue(state.alert(0L, NpcThreats.ALERT_INTERVAL));
        assertFalse(state.alert(NpcThreats.ALERT_INTERVAL - 1L, NpcThreats.ALERT_INTERVAL));
        assertTrue(state.alert(NpcThreats.ALERT_INTERVAL, NpcThreats.ALERT_INTERVAL));
    }

    @Test
    void theWeaponRangeIsMeasuredOnceATick()
    {
        NpcTacticState state = new NpcTacticState();
        int[] measured = new int[1];
        state.weaponRange(5L, () -> ++measured[0]);
        state.weaponRange(5L, () -> ++measured[0]);
        assertEquals(1, measured[0]);
        assertEquals(2, state.weaponRange(6L, () -> ++measured[0]));
    }
}
