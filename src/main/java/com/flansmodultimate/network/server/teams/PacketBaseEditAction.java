package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Validated response from the base editor. */
@NoArgsConstructor
public final class PacketBaseEditAction implements IServerPacket
{
    private UUID baseId = new UUID(0L, 0L);
    private String baseName = "";
    private String mapId = "";
    private int ownerId;
    private int poleHeight;

    public PacketBaseEditAction(UUID baseId, String baseName, String mapId, int ownerId, int poleHeight)
    {
        this.baseId = baseId;
        this.baseName = baseName;
        this.mapId = mapId;
        this.ownerId = ownerId;
        this.poleHeight = poleHeight;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeUUID(baseId);
        data.writeUtf(baseName, 60);
        data.writeUtf(mapId, 128);
        data.writeVarInt(ownerId);
        data.writeVarInt(poleHeight);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        baseId = data.readUUID();
        baseName = data.readUtf(60);
        mapId = data.readUtf(128);
        ownerId = data.readVarInt();
        poleHeight = data.readVarInt();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!player.hasPermissions(2))
            return;
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base = manager.getBase(baseId).orElse(null);
        if (base == null || !base.getDimension().equals(level.dimension()) || player.position().distanceToSqr(base.getTeamObjectPosition()) > 64D * 64D)
            return;

        base.setBaseName(baseName);
        if (base instanceof ITeamFlagpole pole)
            pole.setPoleHeight(poleHeight);
        // Any team slot a round could have may own a base; a round with fewer teams leaves the rest unowned.
        base.setDefaultOwnerId(Math.max(0, Math.min(ownerId, TeamsRound.MAX_TEAMS + 1)));
        manager.getMap(mapId).filter(map -> map.getDimension().equals(base.getDimension())).ifPresent(map -> manager.assignBaseToMap(base, map));
    }
}
