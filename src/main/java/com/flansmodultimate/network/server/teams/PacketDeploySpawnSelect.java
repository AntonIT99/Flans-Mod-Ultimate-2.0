package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Where a dead player asked to come back on the deployment map: at a capture point, or at
 * headquarters. It only records the wish; the game type decides at respawn whether the point is
 * still theirs to spawn at, so a stale or made-up choice simply falls back to headquarters.
 */
@NoArgsConstructor
public final class PacketDeploySpawnSelect implements IServerPacket
{
    @Nullable
    private UUID capturePointId;

    public PacketDeploySpawnSelect(@Nullable UUID capturePointId)
    {
        this.capturePointId = capturePointId;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(capturePointId != null);
        if (capturePointId != null)
            data.writeUUID(capturePointId);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        capturePointId = data.readBoolean() ? data.readUUID() : null;
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        TeamsManager.getInstance().selectSpawn(player, capturePointId);
    }
}
