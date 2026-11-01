package com.flansmodultimate.common.block.entity;

import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamSpawnerSettingsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static TeamSpawnerBlockEntity spawner(TeamSpawnerBlockEntity.Mode mode)
    {
        return new TeamSpawnerBlockEntity(mock(BlockEntityType.class), BlockPos.ZERO, Blocks.AIR.defaultBlockState(), mode);
    }

    @Test
    void connectingImmediatelySyncsTheDisplayColourWithoutAnActiveRound()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            UUID baseId = UUID.randomUUID();
            ITeamBase base = mock(ITeamBase.class);
            Team team = mock(Team.class);
            when(manager.getBase(baseId)).thenReturn(Optional.of(base));
            when(manager.getDisplayTeamForBase(base)).thenReturn(team);
            when(team.getTeamColour()).thenReturn(0x123456);
            for (TeamSpawnerBlockEntity.Mode mode : TeamSpawnerBlockEntity.Mode.values())
            {
                TeamSpawnerBlockEntity spawner = spawner(mode);
                ServerLevel level = mock(ServerLevel.class);
                spawner.setLevel(level);
                spawner.setBaseId(baseId);
                assertEquals(0x123456, spawner.getTeamColour());
                assertNull(spawner.getOwningTeam(), "Display colour must not grant gameplay ownership outside a round");
                verify(level, atLeastOnce()).sendBlockUpdated(BlockPos.ZERO, spawner.getBlockState(), spawner.getBlockState(), 3);
                CompoundTag tag = new CompoundTag();
                spawner.saveData(tag, null);
                TeamSpawnerBlockEntity client = spawner(mode);
                client.loadData(tag, null);
                assertEquals(0x123456, client.getTeamColour());
                spawner.setBaseId(null);
                assertEquals(TeamSpawnerBlockEntity.UNOWNED_COLOUR, spawner.getTeamColour());
            }
        }
    }

    @Test
    void oldAndNewVehicleSpawnersDefaultToAllAndSequentialSurvivesSaving()
    {
        TeamSpawnerBlockEntity spawner = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        assertTrue(spawner.isNpcSpawnAll());
        CompoundTag old = new CompoundTag();
        old.putString("mode", "VEHICLE");
        spawner.loadData(old, null);
        assertTrue(spawner.isNpcSpawnAll());
        spawner.setNpcSpawnAll(false);
        CompoundTag saved = new CompoundTag();
        spawner.saveData(saved, null);
        TeamSpawnerBlockEntity restored = spawner(TeamSpawnerBlockEntity.Mode.VEHICLE);
        restored.loadData(saved, null);
        assertFalse(restored.isNpcSpawnAll());
    }
}
