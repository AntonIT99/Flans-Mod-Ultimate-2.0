package com.flansmodultimate.network.server.teams;

import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
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

/** Validates operator access and the spawner identity before saving a delay. */
@NoArgsConstructor
@AllArgsConstructor
public final class PacketSpawnerEditAction implements IServerPacket
{
    private BlockPos pos = BlockPos.ZERO;
    private UUID objectId = new UUID(0L, 0L);
    private int seconds;
    private boolean waitForVehicle = true;
    private SpawnerNpcPolicy npcPolicy = SpawnerNpcPolicy.ALL;
    private boolean npcSpawnAll = true;
    private boolean updateBase;
    private UUID baseId;

    public PacketSpawnerEditAction(BlockPos pos, UUID objectId, int seconds, boolean waitForVehicle, SpawnerNpcPolicy npcPolicy)
    {
        this(pos, objectId, seconds, waitForVehicle, npcPolicy, true, false, null);
    }

    public PacketSpawnerEditAction(BlockPos pos, UUID objectId, int seconds)
    {
        this(pos, objectId, seconds, true, SpawnerNpcPolicy.ALL);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBlockPos(pos);
        data.writeUUID(objectId);
        data.writeVarInt(seconds);
        data.writeBoolean(waitForVehicle);
        data.writeNbt(npcPolicy.save());
        data.writeBoolean(npcSpawnAll);
        data.writeBoolean(updateBase);
        data.writeBoolean(baseId != null);
        if (baseId != null)
            data.writeUUID(baseId);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        pos = data.readBlockPos();
        objectId = data.readUUID();
        seconds = data.readVarInt();
        waitForVehicle = data.readBoolean();
        npcPolicy = SpawnerNpcPolicy.load(java.util.Objects.requireNonNull(data.readNbt()));
        npcSpawnAll = data.readBoolean();
        updateBase = data.readBoolean();
        baseId = data.readBoolean() ? data.readUUID() : null;
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (!player.hasPermissions(2) || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64D || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
            return;
        if (!(level.getBlockEntity(pos) instanceof TeamSpawnerBlockEntity spawner) || !spawner.getObjectId().equals(objectId))
            return;
        if (spawner.getMode() != TeamSpawnerBlockEntity.Mode.PLAYER && (seconds < 1 || seconds > TeamSpawnerBlockEntity.MAX_SPAWN_DELAY_SECONDS))
            return;
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base = updateBase && baseId != null ? manager.getBase(baseId).orElse(null) : null;
        if (updateBase && baseId != null && (base == null || !base.getDimension().equals(level.dimension())))
            return;
        spawner.setSpawnDelaySeconds(seconds);
        spawner.setVehicleSettings(waitForVehicle, npcPolicy);
        spawner.setNpcSpawnAll(npcSpawnAll);
        assignBase(manager, spawner, base);
    }

    private void assignBase(TeamsManager manager, TeamSpawnerBlockEntity spawner, ITeamBase base)
    {
        if (!updateBase)
            return;
        if (base == null)
            manager.disconnectObject(spawner);
        else
            manager.connectObject(base, spawner);
    }
}
