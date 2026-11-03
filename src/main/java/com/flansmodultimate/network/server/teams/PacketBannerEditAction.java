package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.block.entity.TeamBannerBlockEntity;
import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Assigns a banner after validating the operator and the target. */
@NoArgsConstructor
@AllArgsConstructor
public final class PacketBannerEditAction implements IServerPacket
{
    private BlockPos pos = BlockPos.ZERO;
    private UUID objectId = new UUID(0L, 0L);
    private UUID baseId;

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBlockPos(pos);
        data.writeUUID(objectId);
        data.writeBoolean(baseId != null);
        if (baseId != null)
            data.writeUUID(baseId);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        pos = data.readBlockPos();
        objectId = data.readUUID();
        baseId = data.readBoolean() ? data.readUUID() : null;
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!player.hasPermissions(2) || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64D || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
            return;
        if (!(level.getBlockEntity(pos) instanceof TeamBannerBlockEntity banner) || !banner.getObjectId().equals(objectId))
            return;
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base = baseId == null ? null : manager.getBase(baseId).orElse(null);
        if (baseId != null && (base == null || !base.getDimension().equals(level.dimension())))
            return;
        if (base == null)
            manager.disconnectObject(banner);
        else
            manager.connectObject(base, banner);
    }
}
