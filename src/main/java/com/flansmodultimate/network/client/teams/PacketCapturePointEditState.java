package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.client.gui.TeamsCapturePointEditScreen;
import com.flansmodultimate.common.block.entity.CapturePointBlockEntity;
import com.flansmodultimate.common.teams.ITeamCapturePoint;
import com.flansmodultimate.common.teams.ITeamFlagpole;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Operator-only snapshot of one capture point, sent to open its editor. */
@Getter
@NoArgsConstructor
public final class PacketCapturePointEditState implements IClientPacket
{
    private UUID pointId = new UUID(0L, 0L);
    private String pointName = "";
    private char symbol = CapturePointBlockEntity.FIRST_SYMBOL;
    private int captureRadius = CapturePointBlockEntity.DEFAULT_RADIUS;
    private int ownerId;
    private int poleHeight;
    private String selectedMap = "";
    private List<PacketBaseEditState.MapChoice> maps = List.of();

    public static PacketCapturePointEditState create(TeamsManager manager, ITeamCapturePoint point)
    {
        PacketCapturePointEditState packet = new PacketCapturePointEditState();
        packet.pointId = point.getObjectId();
        packet.pointName = point.getBaseName();
        packet.symbol = point.getSymbol();
        packet.captureRadius = point.getCaptureRadius();
        packet.ownerId = point.getDefaultOwnerId();
        packet.poleHeight = point instanceof ITeamFlagpole pole ? pole.getPoleHeight() : 0;
        packet.selectedMap = point.getMapId();
        packet.maps = manager.getMaps().stream().filter(map -> map.getDimension().equals(point.getDimension())).map(map -> new PacketBaseEditState.MapChoice(map.getShortName(), map.getName()))
            .toList();
        return packet;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(pointId);
        data.writeUtf(pointName, CapturePointBlockEntity.MAX_NAME_LENGTH);
        data.writeChar(symbol);
        data.writeVarInt(captureRadius);
        data.writeVarInt(ownerId);
        data.writeVarInt(poleHeight);
        data.writeUtf(selectedMap, 128);
        data.writeCollection(maps, (buf, map) ->
        {
            buf.writeUtf(map.id(), 128);
            buf.writeUtf(map.name(), 128);
        });
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        pointId = data.readUUID();
        pointName = data.readUtf(CapturePointBlockEntity.MAX_NAME_LENGTH);
        symbol = data.readChar();
        captureRadius = data.readVarInt();
        ownerId = data.readVarInt();
        poleHeight = data.readVarInt();
        selectedMap = data.readUtf(128);
        maps = data.readList(buf -> new PacketBaseEditState.MapChoice(buf.readUtf(128), buf.readUtf(128)));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Minecraft.getInstance().setScreen(new TeamsCapturePointEditScreen(this));
    }
}
