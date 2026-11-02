package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.teams.SpawnerVehicles;
import com.flansmodultimate.common.teams.TeamsMap;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpawnerVehiclesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void unloadedVehiclesStillBlockButWrecksAndVehiclesOutsideTheMapDoNot()
    {
        SpawnerVehicles data = new SpawnerVehicles();
        UUID spawner = UUID.randomUUID();
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        Driveable vehicle = mock(Driveable.class);
        CompoundTag tag = new CompoundTag();
        tag.putUUID(SpawnerVehicles.SPAWNER, spawner);
        when(vehicle.getPersistentData()).thenReturn(tag);
        when(vehicle.getUUID()).thenReturn(UUID.randomUUID());
        when(vehicle.level()).thenReturn(level);
        when(vehicle.isAlive()).thenReturn(true);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getServer()).thenReturn(server);
        when(server.getAllLevels()).thenReturn(List.of(level));
        when(level.getAllEntities()).thenReturn(List.of(vehicle));
        TeamsMap map = new TeamsMap("test", "Test", Level.OVERWORLD);
        map.setBounds(new TeamsMap.Bounds(-10, -10, -10, 10, 10, 10));
        assertTrue(data.exists(level, spawner, map));

        when(level.getAllEntities()).thenReturn(List.of());
        when(vehicle.isAlive()).thenReturn(false);
        when(vehicle.getRemovalReason()).thenReturn(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        data.observe(vehicle);
        assertTrue(data.exists(level, spawner, map));

        when(vehicle.getX()).thenReturn(11D);
        data.observe(vehicle);
        assertFalse(data.exists(level, spawner, map));
        assertTrue(data.exists(level, spawner, null));

        when(vehicle.isWreck()).thenReturn(true);
        data.observe(vehicle);
        assertFalse(data.exists(level, spawner, null));
    }
}
