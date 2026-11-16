package com.wolffsmod.npcs.properties;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reading the stats sets the most health directly; the share of health the NPC has left must survive it either way. */
class NpcStatsLoadHealthTest
{
    private final NpcTypeProperties properties = new NpcTypeProperties();
    private float health;
    private float maximum;
    private boolean killed;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private EntityCustomNpc npc()
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.stats = mock(DataStats.class);
        when(npc.getMaxHealth()).thenAnswer(invocation -> maximum);
        when(npc.getHealth()).thenAnswer(invocation -> health);
        when(npc.isKilled()).thenAnswer(invocation -> killed);
        doAnswer(invocation ->
        {
            health = Math.min(invocation.<Float> getArgument(0), maximum);
            return null;
        }).when(npc).setHealth(anyFloat());
        return npc;
    }

    /** Reads stats that set the given most health, as Custom NPCs does, between the two hooks. */
    private void load(EntityCustomNpc npc, float loadedMaximum)
    {
        properties.beforeStatsLoad(npc);
        maximum = loadedMaximum;
        properties.afterStatsLoad(npc);
    }

    @Test
    void anOwnMaximumThatDropsAndComesBackKeepsTheShareOfHealth()
    {
        EntityCustomNpc npc = npc();
        maximum = 4977F;
        health = 4977F;
        load(npc, 20F);
        assertEquals(20F, health, 1.0E-3F);
        load(npc, 4977F);
        assertEquals(4977F, health, 1.0E-3F);
    }

    @Test
    void aWoundedNpcStaysAsWounded()
    {
        EntityCustomNpc npc = npc();
        maximum = 1000F;
        health = 250F;
        load(npc, 4000F);
        assertEquals(1000F, health, 1.0E-3F);
    }

    @Test
    void anUnchangedMaximumLeavesHealthAlone()
    {
        EntityCustomNpc npc = npc();
        maximum = 100F;
        health = 40F;
        load(npc, 100F);
        verify(npc, never()).setHealth(anyFloat());
    }

    @Test
    void aKilledNpcIsNotRevived()
    {
        EntityCustomNpc npc = npc();
        maximum = 100F;
        health = -1F;
        killed = true;
        load(npc, 200F);
        verify(npc, never()).setHealth(anyFloat());
    }
}
