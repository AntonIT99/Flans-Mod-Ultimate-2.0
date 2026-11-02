package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Where a dead player asked to come back on the deployment map: at a capture point, or at
 * headquarters, or aboard a template vehicle. It only records the wish; the server checks the point and any free seat
 * again on respawn, so a stale or made-up choice falls back to the normal spawn without displacing an occupant.
 */
@NoArgsConstructor
public final class PacketDeploySpawnSelect implements IServerPacket
{
    @Nullable
    private UUID capturePointId;
    private boolean vehicle;

    public PacketDeploySpawnSelect(@Nullable UUID capturePointId)
    {
        this.capturePointId = capturePointId;
    }

    public static PacketDeploySpawnSelect vehicle(UUID spawner)
    {
        PacketDeploySpawnSelect packet = new PacketDeploySpawnSelect(spawner);
        packet.vehicle = true;
        return packet;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(vehicle);
        data.writeBoolean(capturePointId != null);
        if (capturePointId != null)
            data.writeUUID(capturePointId);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        vehicle = data.readBoolean();
        capturePointId = data.readBoolean() ? data.readUUID() : null;
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        TeamsManager manager = TeamsManager.getInstance();
        if (vehicle)
        {
            if (capturePointId == null || !manager.selectVehicleSpawn(player, capturePointId))
                player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.vehicle_deploy_unavailable"));
        }
        else
            manager.selectSpawn(player, capturePointId);
    }
}
