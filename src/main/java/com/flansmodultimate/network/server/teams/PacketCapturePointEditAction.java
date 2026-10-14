package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.block.entity.CapturePointBlockEntity;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Validated response from the capture point editor. */
@NoArgsConstructor
public final class PacketCapturePointEditAction implements IServerPacket
{
    /** A capture point starts neutral or in the hands of one of the two teams, never the spectators. */
    private static final int MAX_OWNER_ID = 3;

    private UUID pointId = new UUID(0L, 0L);
    private String pointName = "";
    private char symbol = CapturePointBlockEntity.FIRST_SYMBOL;
    private int captureRadius = CapturePointBlockEntity.DEFAULT_RADIUS;
    private int ownerId;
    private String mapId = "";

    public PacketCapturePointEditAction(UUID pointId, String pointName, char symbol, int captureRadius, int ownerId, String mapId)
    {
        this.pointId = pointId;
        this.pointName = pointName;
        this.symbol = symbol;
        this.captureRadius = captureRadius;
        this.ownerId = ownerId;
        this.mapId = mapId;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(pointId);
        data.writeUtf(pointName, CapturePointBlockEntity.MAX_NAME_LENGTH);
        data.writeChar(symbol);
        data.writeVarInt(captureRadius);
        data.writeVarInt(ownerId);
        data.writeUtf(mapId, 128);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        pointId = data.readUUID();
        pointName = data.readUtf(CapturePointBlockEntity.MAX_NAME_LENGTH);
        symbol = data.readChar();
        captureRadius = data.readVarInt();
        ownerId = data.readVarInt();
        mapId = data.readUtf(128);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!ItemOpStick.canUse(player))
            return;
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base = manager.getBase(pointId).orElse(null);
        if (!(base instanceof CapturePointBlockEntity point) || !point.getDimension().equals(level.dimension()) || player.position().distanceToSqr(point.getTeamObjectPosition()) > 64D * 64D)
            return;

        point.applySettings(pointName, symbol, captureRadius);
        int owner = ownerId == 1 ? 0 : Math.max(0, Math.min(MAX_OWNER_ID, ownerId));
        if (owner != point.getDefaultOwnerId())
            point.setDefaultOwnerId(owner);
        manager.getMap(mapId).filter(map -> map.getDimension().equals(point.getDimension())).ifPresent(map -> manager.assignBaseToMap(point, map));
        manager.syncObjectives();
    }
}
