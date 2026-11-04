package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.block.entity.TeamBannerBlockEntity;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PacketBannerEditTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void assignmentRequiresPermissionProximityIdentityAndSameDimension()
    {
        UUID id = UUID.randomUUID();
        UUID baseId = UUID.randomUUID();
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        PacketBannerEditAction packet = new PacketBannerEditAction();
        try
        {
            new PacketBannerEditAction(BlockPos.ZERO, id, baseId).encodeInto(buffer);
            packet.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        ServerChunkCache chunks = mock(ServerChunkCache.class);
        TeamBannerBlockEntity banner = mock(TeamBannerBlockEntity.class);
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            packet.handleServerSide(player, level);
            verifyNoInteractions(level);
            when(player.hasPermissions(2)).thenReturn(true);
            when(player.distanceToSqr(0.5D, 0.5D, 0.5D)).thenReturn(65D);
            packet.handleServerSide(player, level);
            verifyNoInteractions(level);
            when(player.distanceToSqr(0.5D, 0.5D, 0.5D)).thenReturn(0D);
            when(level.getChunkSource()).thenReturn(chunks);
            packet.handleServerSide(player, level);
            verify(level, never()).getBlockEntity(BlockPos.ZERO);
            when(chunks.hasChunk(0, 0)).thenReturn(true);
            when(level.getBlockEntity(BlockPos.ZERO)).thenReturn(banner);
            when(banner.getObjectId()).thenReturn(UUID.randomUUID());
            packet.handleServerSide(player, level);
            verifyNoInteractions(manager);
            when(banner.getObjectId()).thenReturn(id);
            packet.handleServerSide(player, level);
            ITeamBase base = mock(ITeamBase.class);
            when(manager.getBase(baseId)).thenReturn(Optional.of(base));
            when(base.getDimension()).thenReturn(Level.NETHER);
            when(level.dimension()).thenReturn(Level.OVERWORLD);
            packet.handleServerSide(player, level);
            verify(manager, never()).connectObject(any(), any());
            when(base.getDimension()).thenReturn(Level.OVERWORLD);
            packet.handleServerSide(player, level);
            verify(manager).connectObject(base, banner);
            new PacketBannerEditAction(BlockPos.ZERO, id, null).handleServerSide(player, level);
            verify(manager).disconnectObject(banner);
        }
    }
}
