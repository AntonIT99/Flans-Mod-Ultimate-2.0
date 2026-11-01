package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.client.teams.PacketSpawnerEditState;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
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

    @Test
    void playerSpawnerCanAssignAndDisconnectWithoutAValidDelayButRejectsUnknownAndOtherDimensionBases()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            UUID object = UUID.randomUUID();
            UUID baseId = UUID.randomUUID();
            ServerPlayer player = mock(ServerPlayer.class);
            ServerLevel level = mock(ServerLevel.class);
            ServerChunkCache chunks = mock(ServerChunkCache.class);
            TeamSpawnerBlockEntity spawner = mock(TeamSpawnerBlockEntity.class);
            when(player.hasPermissions(2)).thenReturn(true);
            when(level.dimension()).thenReturn(Level.OVERWORLD);
            when(level.getChunkSource()).thenReturn(chunks);
            when(chunks.hasChunk(0, 0)).thenReturn(true);
            when(level.getBlockEntity(BlockPos.ZERO)).thenReturn(spawner);
            when(spawner.getObjectId()).thenReturn(object);
            when(spawner.getMode()).thenReturn(TeamSpawnerBlockEntity.Mode.PLAYER);
            PacketSpawnerEditAction packet = decode(new PacketSpawnerEditAction(BlockPos.ZERO, object, 0, true, SpawnerNpcPolicy.ALL, false, true, baseId));
            when(manager.getBase(baseId)).thenReturn(Optional.empty());
            packet.handleServerSide(player, level);
            verify(manager, never()).connectObject(any(), any());
            verify(spawner, never()).setSpawnDelaySeconds(anyInt());
            ITeamBase base = mock(ITeamBase.class);
            when(manager.getBase(baseId)).thenReturn(Optional.of(base));
            when(base.getDimension()).thenReturn(Level.NETHER);
            packet.handleServerSide(player, level);
            verify(spawner, never()).setSpawnDelaySeconds(anyInt());
            when(base.getDimension()).thenReturn(Level.OVERWORLD);
            packet.handleServerSide(player, level);
            verify(manager).connectObject(base, spawner);
            decode(new PacketSpawnerEditAction(BlockPos.ZERO, object, 0, true, SpawnerNpcPolicy.ALL, true, true, null)).handleServerSide(player, level);
            verify(manager).disconnectObject(spawner);
        }
    }

    @Test
    void snapshotCarriesBaseChoicesAndDeploymentMode()
    {
        UUID object = UUID.randomUUID();
        UUID base = UUID.randomUUID();
        PacketSpawnerEditState state = new PacketSpawnerEditState(BlockPos.ZERO, object, 400, true, false, SpawnerNpcPolicy.ALL, false, false, base, "Headquarters",
            List.of(new PacketSpawnerEditState.BaseChoice(base, "Headquarters", "testmap", new BlockPos(8, 64, 2))));
        PacketSpawnerEditState copy = new PacketSpawnerEditState();
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            state.encodeInto(buffer);
            copy.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
        assertEquals(base, copy.getBaseId());
        assertEquals(state.getBases(), copy.getBases());
        assertFalse(copy.isNpcSpawnAll());
        assertTrue(copy.isVehicle());
        assertEquals(400, copy.getDelayTicks());
    }

    private static PacketSpawnerEditAction decode(PacketSpawnerEditAction source)
    {
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            source.encodeInto(buffer);
            PacketSpawnerEditAction result = new PacketSpawnerEditAction();
            result.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            return result;
        }
        finally
        {
            buffer.release();
        }
    }
}
