package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.LivingEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcPursuitTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aPursuitKeepsItsPathForTheIntervalAndStartsAtTheMobsOwnMoment()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        LivingEntity target = mock(LivingEntity.class);
        LivingEntity other = mock(LivingEntity.class);
        when(npc.getId()).thenReturn(3);
        when(target.getId()).thenReturn(10);
        when(other.getId()).thenReturn(11);
        try (var mobility = mockStatic(NpcMobility.class))
        {
            mobility.when(() -> NpcMobility.pursuitInterval(npc)).thenReturn(10);
            NpcPursuit pursuit = new NpcPursuit();
            // A new pursuit searches now and next at tick 100 + 10 + its own offset of 3.
            assertFalse(pursuit.keeps(npc, target, 100L, true));
            assertTrue(pursuit.keeps(npc, target, 112L, true));
            // A path that ran out is searched again at once, and the interval starts over.
            assertFalse(pursuit.keeps(npc, target, 112L, false));
            assertTrue(pursuit.keeps(npc, target, 121L, true));
            assertFalse(pursuit.keeps(npc, target, 122L, true));
            // Another entity is searched for at once.
            assertFalse(pursuit.keeps(npc, other, 123L, true));
            mobility.when(() -> NpcMobility.pursuitInterval(npc)).thenReturn(1);
            assertFalse(pursuit.keeps(npc, other, 124L, true));
        }
    }
}
