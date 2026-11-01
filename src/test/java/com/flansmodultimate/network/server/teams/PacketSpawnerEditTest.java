package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketSpawnerEditTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void decodedEditsRequirePermissionProximityLoadedChunkAndMatchingIdentity()
    {
        BlockPos pos = new BlockPos(2, 60, 4);
        UUID id = UUID.randomUUID();
        PacketSpawnerEditAction packet = new PacketSpawnerEditAction();
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            new PacketSpawnerEditAction(pos, id, 37).encodeInto(buffer);
            packet.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }

        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        TeamSpawnerBlockEntity spawner = mock(TeamSpawnerBlockEntity.class);
        packet.handleServerSide(player, level);
        verifyNoInteractions(level);

        when(player.hasPermissions(2)).thenReturn(true);
        when(player.distanceToSqr(2.5D, 60.5D, 4.5D)).thenReturn(65D);
        packet.handleServerSide(player, level);
        verifyNoInteractions(level);

        when(player.distanceToSqr(2.5D, 60.5D, 4.5D)).thenReturn(64D);
        ServerChunkCache chunks = mock(ServerChunkCache.class);
        when(level.getChunkSource()).thenReturn(chunks);
        packet.handleServerSide(player, level);
        verify(level, never()).getBlockEntity(pos);

        when(chunks.hasChunk(0, 0)).thenReturn(true);
        when(level.getBlockEntity(pos)).thenReturn(spawner);
        when(spawner.getObjectId()).thenReturn(UUID.randomUUID());
        packet.handleServerSide(player, level);
        verify(spawner, never()).setSpawnDelaySeconds(anyInt());

        when(spawner.getObjectId()).thenReturn(id);
        packet.handleServerSide(player, level);
        verify(spawner).setSpawnDelaySeconds(37);
    }
}
