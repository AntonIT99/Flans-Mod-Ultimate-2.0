package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.block.entity.CapturePointBlockEntity;
import com.flansmodultimate.common.teams.ITeamFlagpole;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.client.teams.PacketBaseEditState;
import com.flansmodultimate.network.client.teams.PacketCapturePointEditState;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketPoleHeightTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void editorSnapshotsCarryEachPolesSavedHeight(boolean capturePoint)
    {
        TeamsManager manager = mock(TeamsManager.class);
        ITeamFlagpole pole = capturePoint ? mock(CapturePointBlockEntity.class) : mock(ITeamFlagpole.class);
        when(pole.getObjectId()).thenReturn(UUID.randomUUID());
        when(pole.getBaseName()).thenReturn("Tall pole");
        when(pole.getMapId()).thenReturn("");
        when(pole.getPoleHeight()).thenReturn(19);
        if (pole instanceof CapturePointBlockEntity point)
        {
            PacketCapturePointEditState snapshot = PacketCapturePointEditState.create(manager, point);
            PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
            try
            {
                snapshot.encodeInto(buffer);
                PacketCapturePointEditState decoded = new PacketCapturePointEditState();
                decoded.decodeInto(buffer);
                assertEquals(19, decoded.getPoleHeight());
                assertEquals(pole.getObjectId(), decoded.getPointId());
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
        else
        {
            PacketBaseEditState snapshot = PacketBaseEditState.create(manager, pole);
            PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
            try
            {
                snapshot.encodeInto(buffer);
                PacketBaseEditState decoded = new PacketBaseEditState();
                decoded.decodeInto(buffer);
                assertEquals(19, decoded.getPoleHeight());
                assertEquals(pole.getObjectId(), decoded.getBaseId());
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void decodedEditsRequirePermissionProximityAndTheSameDimension(boolean capturePoint)
    {
        UUID id = UUID.randomUUID();
        IServerPacket original = capturePoint ? new PacketCapturePointEditAction(id, "Pole", 'B', 16, 0, "", 23) : new PacketBaseEditAction(id, "Pole", "", 0, 23);
        IServerPacket decoded = capturePoint ? new PacketCapturePointEditAction() : new PacketBaseEditAction();
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            original.encodeInto(buffer);
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
        ITeamFlagpole pole = capturePoint ? mock(CapturePointBlockEntity.class) : mock(ITeamFlagpole.class);
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        TeamsManager manager = mock(TeamsManager.class);
        when(manager.getBase(id)).thenReturn(Optional.of(pole));
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(pole.getDimension()).thenReturn(Level.OVERWORLD);
        when(pole.getTeamObjectPosition()).thenReturn(Vec3.ZERO);
        when(player.position()).thenReturn(Vec3.ZERO);
        try (var managers = mockStatic(TeamsManager.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            decoded.handleServerSide(player, level);
            verify(pole, never()).setPoleHeight(anyInt());

            when(player.hasPermissions(2)).thenReturn(true);
            when(player.position()).thenReturn(new Vec3(100D, 0D, 0D));
            decoded.handleServerSide(player, level);
            verify(pole, never()).setPoleHeight(anyInt());

            when(player.position()).thenReturn(Vec3.ZERO);
            when(pole.getDimension()).thenReturn(Level.NETHER);
            decoded.handleServerSide(player, level);
            verify(pole, never()).setPoleHeight(anyInt());

            when(pole.getDimension()).thenReturn(Level.OVERWORLD);
            decoded.handleServerSide(player, level);
            verify(pole).setPoleHeight(23);
        }
    }
}
