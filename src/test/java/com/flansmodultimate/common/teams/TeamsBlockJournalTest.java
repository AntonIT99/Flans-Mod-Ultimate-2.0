package com.flansmodultimate.common.teams;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsBlockJournalTest
{
    @BeforeAll
    static void bootstrap()
    {
        TeamsMapTest.bootstrapRegistries();
    }

    @Test
    void keepsFirstStateAcrossRepeatedDestructionAndPlacementAndSaveLoad()
    {
        ServerLevel level = mock(ServerLevel.class);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(2, 65, 3);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getBlockState(pos)).thenReturn(Blocks.GLASS.defaultBlockState());
        TeamsBlockJournal journal = new TeamsBlockJournal();
        journal.capture(level, pos, Blocks.AIR.defaultBlockState());
        when(level.getBlockState(pos)).thenReturn(Blocks.AIR.defaultBlockState());
        journal.capture(level, pos, Blocks.DIRT.defaultBlockState());
        pos.set(99, 99, 99);
        ListTag saved = journal.save();
        assertEquals(1, saved.size());
        assertEquals(new BlockPos(2, 65, 3).asLong(), saved.getCompound(0).getLong("pos"));
        assertEquals("minecraft:glass", saved.getCompound(0).getCompound("state").getString("Name"));
        TeamsBlockJournal loaded = new TeamsBlockJournal();
        loaded.load(saved);
        assertEquals(saved, loaded.save());
    }

    @Test
    void restoresAndClearsJournalButRetainsUnavailableDimensions()
    {
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        BlockPos pos = new BlockPos(1, 70, 1);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getBlockState(pos)).thenReturn(Blocks.GLASS.defaultBlockState());
        TeamsBlockJournal journal = new TeamsBlockJournal();
        journal.capture(level, pos, Blocks.AIR.defaultBlockState());
        journal.restore(server);
        assertEquals(1, journal.save().size());
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        journal.restore(server);
        verify(level).setBlock(eq(pos), eq(Blocks.GLASS.defaultBlockState()), anyInt());
        assertTrue(journal.save().isEmpty());
    }

    @Test
    void unchangedStateIsNotRecordedAndAirIsRememberedForPlacedBlocks()
    {
        ServerLevel level = mock(ServerLevel.class);
        BlockPos pos = BlockPos.ZERO;
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getBlockState(pos)).thenReturn(Blocks.AIR.defaultBlockState());
        TeamsBlockJournal journal = new TeamsBlockJournal();
        journal.capture(level, pos, Blocks.AIR.defaultBlockState());
        assertTrue(journal.save().isEmpty());
        journal.capture(level, pos, Blocks.STONE.defaultBlockState());
        assertEquals("minecraft:air", journal.save().getCompound(0).getCompound("state").getString("Name"));
    }

    @Test
    void blockEntityPayloadSurvivesSaveLoad()
    {
        CompoundTag entry = new CompoundTag();
        entry.putString("dimension", "minecraft:overworld");
        entry.putLong("pos", BlockPos.ZERO.asLong());
        entry.put("state", net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.CHEST.defaultBlockState()));
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:chest");
        entity.putString("CustomName", "Arena chest");
        entry.put("block_entity", entity);
        ListTag saved = new ListTag();
        saved.add(entry);
        TeamsBlockJournal journal = new TeamsBlockJournal();
        journal.load(saved);
        assertEquals(saved, journal.save());
    }
}
