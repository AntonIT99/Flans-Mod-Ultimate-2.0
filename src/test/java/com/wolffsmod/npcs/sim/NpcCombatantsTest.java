package com.wolffsmod.npcs.sim;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCombatantsTest
{
    private static final long FIRST_TICK = 1L;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @AfterEach
    void forgetLevels()
    {
        NpcCombatants.clear();
    }

    @Test
    void theLevelIsWalkedOnceATickHoweverManySearchesAskForIt()
    {
        ServerLevel level = level(List.of(living(false), npc()));
        NpcCombatants first = NpcCombatants.of(level);
        assertEquals(2, first.size());
        assertSame(first, NpcCombatants.of(level));
        assertSame(first, NpcCombatants.of(level));
        verify(level, times(1)).getAllEntities();
    }

    @Test
    void theNextTickTakesTheListAgain()
    {
        ServerLevel level = level(List.of(living(false)));
        NpcCombatants list = NpcCombatants.of(level);
        assertEquals(1, list.size());
        when(level.getGameTime()).thenReturn(2L);
        assertSame(list, NpcCombatants.of(level));
        verify(level, times(2)).getAllEntities();
    }

    @Test
    void removedEntitiesAndEverythingThatDoesNotLiveAreLeftOut()
    {
        ServerLevel level = level(List.of(living(false), living(true), mock(ItemEntity.class)));
        NpcCombatants list = NpcCombatants.of(level);
        assertEquals(1, list.size());
        assertEquals(0, list.npcSize());
    }

    @Test
    void npcsAreListedSeparatelyAndCarryTheirOwnEntryOfTheWholeList()
    {
        LivingEntity other = living(false);
        EntityNPCInterface npc = npc();
        NpcCombatants list = NpcCombatants.of(level(List.of(other, npc)));
        assertEquals(2, list.size());
        assertEquals(1, list.npcSize());
        assertSame(npc, list.npc(0).npc());
        assertSame(npc, list.npc(0).entity());
        // An NPC that states no faction is nobody's ally by faction.
        assertEquals(-1, list.npc(0).faction());
        assertNull(entry(list, other).npc());
        assertNotNull(entry(list, other).bounds());
    }

    @Test
    void aQuieterTickListsOnlyWhatIsStillThere()
    {
        ServerLevel level = level(List.of(living(false), living(false), living(false)));
        assertEquals(3, NpcCombatants.of(level).size());
        LivingEntity survivor = living(false);
        when(level.getGameTime()).thenReturn(2L);
        when(level.getAllEntities()).thenReturn(List.of(survivor));
        NpcCombatants list = NpcCombatants.of(level);
        assertEquals(1, list.size());
        assertSame(survivor, list.get(0).entity());
    }

    private static NpcCombatants.Combatant entry(NpcCombatants list, LivingEntity entity)
    {
        for (int index = 0; index < list.size(); index++)
        {
            if (list.get(index).entity() == entity)
                return list.get(index);
        }
        return fail("the list left out " + entity);
    }

    private static ServerLevel level(List<Entity> entities)
    {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getGameTime()).thenReturn(FIRST_TICK);
        when(level.getAllEntities()).thenReturn(entities);
        return level;
    }

    private static LivingEntity living(boolean removed)
    {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.isRemoved()).thenReturn(removed);
        when(entity.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 1D, 2D, 1D));
        return entity;
    }

    private static EntityNPCInterface npc()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        when(npc.getBoundingBox()).thenReturn(new AABB(0D, 0D, 0D, 1D, 2D, 1D));
        return npc;
    }
}
