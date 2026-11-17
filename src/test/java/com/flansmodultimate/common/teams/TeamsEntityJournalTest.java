package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.entity.AAGun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsEntityJournalTest
{
    private static final UUID ENTITY = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeAll
    static void bootstrap()
    {
        TeamsMapTest.bootstrapRegistries();
    }

    private static <T extends Entity> T snapshot(Class<T> type, int uses)
    {
        T entity = mock(type);
        when(entity.getUUID()).thenReturn(ENTITY);
        when(entity.save(any(CompoundTag.class))).thenAnswer(invocation ->
        {
            CompoundTag tag = invocation.getArgument(0);
            tag.putString("id", "flansmodultimate:grenade");
            tag.putInt("supply_uses_remaining", uses);
            return true;
        });
        return entity;
    }

    @Test
    void keepsTheStateFromBeforeTheRoundAndSurvivesSaveLoad()
    {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        TeamsEntityJournal journal = new TeamsEntityJournal();
        journal.capture(level, snapshot(Entity.class, 3));
        // A second look at the same entity during the round must not overwrite how it started.
        journal.capture(level, snapshot(Entity.class, 1));
        ListTag saved = journal.save();
        assertEquals(1, saved.size());
        assertEquals(3, saved.getCompound(0).getCompound("entity").getInt("supply_uses_remaining"));
        TeamsEntityJournal loaded = new TeamsEntityJournal();
        loaded.load(saved);
        assertEquals(saved, loaded.save());
    }

    @Test
    void leavesAnUntouchedEntityAloneAndRetainsUnavailableDimensions()
    {
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        Entity entity = snapshot(Entity.class, 3);
        TeamsEntityJournal journal = new TeamsEntityJournal();
        journal.capture(level, entity);
        journal.restore(server);
        assertEquals(1, journal.save().size());
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(level.getEntity(ENTITY)).thenReturn(entity);
        journal.restore(server);
        verify(entity, never()).discard();
        assertTrue(journal.save().isEmpty());
    }

    @Test
    void replacesAnEntityThatTheRoundChanged()
    {
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        Entity entity = snapshot(Entity.class, 3);
        TeamsEntityJournal journal = new TeamsEntityJournal();
        journal.capture(level, entity);
        Entity changed = snapshot(Entity.class, 1);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(level.getEntity(ENTITY)).thenReturn(changed);
        journal.restore(server);
        verify(changed).discard();
        assertTrue(journal.save().isEmpty());
    }

    @Test
    void replacedAaGunsDoNotSpillTheirContents()
    {
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        TeamsEntityJournal journal = new TeamsEntityJournal();
        journal.capture(level, snapshot(AAGun.class, 3));
        AAGun destroyed = snapshot(AAGun.class, 0);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(level.getEntity(ENTITY)).thenReturn(destroyed);
        journal.restore(server);
        verify(destroyed).discardWithoutDrops();
        verify(destroyed, never()).discard();
    }

    @Test
    void entitiesThatCannotBeSavedAreNotJournalled()
    {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        Entity entity = mock(Entity.class);
        when(entity.getUUID()).thenReturn(ENTITY);
        when(entity.save(any(CompoundTag.class))).thenReturn(false);
        TeamsEntityJournal journal = new TeamsEntityJournal();
        journal.capture(level, entity);
        assertTrue(journal.save().isEmpty());
    }
}
