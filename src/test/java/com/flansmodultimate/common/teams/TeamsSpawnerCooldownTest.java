package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.TeamsSpawner;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsSpawnerCooldownTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void apiUsesLiveReadinessAndRejectsMissingStaleAndOffThreadPlates()
    {
        TeamsSpawner snapshot = new TeamsSpawner(UUID.randomUUID(), TeamsSpawner.Kind.VEHICLE, Level.OVERWORLD, BlockPos.ZERO, null, null);
        TeamsManager manager = mock(TeamsManager.class);
        TeamSpawnerBlockEntity plate = mock(TeamSpawnerBlockEntity.class);
        when(manager.isServerThread()).thenReturn(true);
        when(manager.getObject(snapshot.id())).thenReturn(Optional.of(plate));
        when(plate.getMode()).thenReturn(TeamSpawnerBlockEntity.Mode.VEHICLE);
        when(plate.getDimension()).thenReturn(Level.OVERWORLD);
        when(plate.getBlockPos()).thenReturn(BlockPos.ZERO);
        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            assertFalse(FlansTeams.isVehicleSpawnerReady(snapshot));
            when(plate.isSpawnReady()).thenReturn(true);
            assertTrue(FlansTeams.isVehicleSpawnerReady(snapshot));
            FlansTeams.recordVehicleSpawnerSpawn(snapshot);
            verify(plate).recordSpawn();
            when(plate.isRemoved()).thenReturn(true);
            assertFalse(FlansTeams.isVehicleSpawnerReady(snapshot));
            FlansTeams.recordVehicleSpawnerSpawn(snapshot);
            when(plate.isRemoved()).thenReturn(false);
            when(plate.getDimension()).thenReturn(Level.NETHER);
            assertFalse(FlansTeams.isVehicleSpawnerReady(snapshot));
            FlansTeams.recordVehicleSpawnerSpawn(snapshot);
            when(manager.getObject(snapshot.id())).thenReturn(Optional.empty());
            assertFalse(FlansTeams.isVehicleSpawnerReady(snapshot));
            when(manager.isServerThread()).thenReturn(false);
            FlansTeams.recordVehicleSpawnerSpawn(snapshot);
            assertFalse(FlansTeams.isVehicleSpawnerReady(snapshot));
            verify(plate, times(1)).recordSpawn();
        }
    }
}
