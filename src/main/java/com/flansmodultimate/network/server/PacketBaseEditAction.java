package com.flansmodultimate.network.server;

import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.UUID;

/** Validated response from the base editor. */
@NoArgsConstructor
public final class PacketBaseEditAction implements IServerPacket
{
    private UUID baseId = new UUID(0L, 0L);
    private String baseName = "";
    private String mapId = "";
    private int ownerId;

    public PacketBaseEditAction(UUID baseId, String baseName, String mapId, int ownerId)
    {
        this.baseId = baseId;
        this.baseName = baseName;
        this.mapId = mapId;
        this.ownerId = ownerId;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeUUID(baseId);
        data.writeUtf(baseName, 60);
        data.writeUtf(mapId, 128);
        data.writeVarInt(ownerId);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        baseId = data.readUUID();
        baseName = data.readUtf(60);
        mapId = data.readUtf(128);
        ownerId = data.readVarInt();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            return;
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base = manager.getBase(baseId).orElse(null);
        if (base == null || !base.getDimension().equals(level.dimension()) || player.position().distanceToSqr(base.getTeamObjectPosition()) > 64D * 64D)
            return;

        int ownerSlots = manager.getCurrentRound().map(round -> round.getTeamIds().size() + 2).orElse(4);
        base.setBaseName(baseName);
        base.setDefaultOwnerId(Math.max(0, Math.min(ownerId, Math.max(1, ownerSlots - 1))));
        manager.getMap(mapId).filter(map -> map.getDimension().equals(base.getDimension())).ifPresent(map -> manager.assignBaseToMap(base, map));
    }
}
