package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketVehicleDeployTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void vehicleAndObjectiveAndHeadquartersChoicesRoundTripWithoutClientSeatOrPositionAuthority()
    {
        TeamsManager manager = mock(TeamsManager.class);
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        UUID id = UUID.randomUUID();
        when(manager.selectVehicleSpawn(player, id)).thenReturn(true);
        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            roundTrip(PacketDeploySpawnSelect.vehicle(id)).handleServerSide(player, level);
            verify(manager).selectVehicleSpawn(player, id);
            roundTrip(new PacketDeploySpawnSelect(id)).handleServerSide(player, level);
            verify(manager).selectSpawn(player, id);
            roundTrip(new PacketDeploySpawnSelect(null)).handleServerSide(player, level);
            verify(manager).selectSpawn(player, null);
        }
    }

    private static PacketDeploySpawnSelect roundTrip(PacketDeploySpawnSelect original)
    {
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            original.encodeInto(buffer);
            PacketDeploySpawnSelect decoded = new PacketDeploySpawnSelect();
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        }
        finally
        {
            buffer.release();
        }
    }
}
