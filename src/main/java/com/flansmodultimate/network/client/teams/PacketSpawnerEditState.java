package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.client.gui.TeamsSpawnerEditScreen;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

/** Server snapshot for the operator's spawner editor, including bases in this dimension. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PacketSpawnerEditState implements IClientPacket
{
    private BlockPos pos = BlockPos.ZERO;
    private UUID objectId = new UUID(0L, 0L);
    private int delayTicks;
    private boolean vehicle;
    private boolean waitForVehicle = true;
    private SpawnerNpcPolicy npcPolicy = SpawnerNpcPolicy.ALL;
    private boolean playerSpawner;
    private boolean npcSpawnAll = true;
    private UUID baseId;
    private String baseName = "";
    private List<BaseChoice> bases = List.of();

    public record BaseChoice(UUID id, String name, String map, BlockPos position)
    {}

    public static PacketSpawnerEditState of(TeamSpawnerBlockEntity spawner)
    {
        TeamsManager manager = TeamsManager.getInstance();
        List<BaseChoice> choices = manager.getBases(spawner.getDimension()).stream()
            .map(base -> new BaseChoice(base.getObjectId(), base.getBaseName(), base.getMapId(), BlockPos.containing(base.getTeamObjectPosition()))).toList();
        String name = spawner.getBaseId() == null ? "" : manager.getBase(spawner.getBaseId()).map(base -> base.getBaseName() + " (" + base.getMapId() + ")").orElse(spawner.getBaseId().toString());
        return new PacketSpawnerEditState(spawner.getBlockPos(), spawner.getObjectId(), spawner.getSpawnDelayTicks(), spawner.getMode() == TeamSpawnerBlockEntity.Mode.VEHICLE,
            spawner.isWaitForVehicle(), spawner.getNpcPolicy(), spawner.getMode() == TeamSpawnerBlockEntity.Mode.PLAYER, spawner.isNpcSpawnAll(), spawner.getBaseId(), name, choices);
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBlockPos(pos);
        data.writeUUID(objectId);
        data.writeVarInt(delayTicks);
        data.writeBoolean(vehicle);
        data.writeBoolean(waitForVehicle);
        data.writeNbt(npcPolicy.save());
        data.writeBoolean(playerSpawner);
        data.writeBoolean(npcSpawnAll);
        data.writeBoolean(baseId != null);
        if (baseId != null)
            data.writeUUID(baseId);
        data.writeUtf(baseName);
        data.writeCollection(bases, (buffer, base) ->
        {
            buffer.writeUUID(base.id());
            buffer.writeUtf(base.name());
            buffer.writeUtf(base.map());
            buffer.writeBlockPos(base.position());
        });
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        pos = data.readBlockPos();
        objectId = data.readUUID();
        delayTicks = data.readVarInt();
        vehicle = data.readBoolean();
        waitForVehicle = data.readBoolean();
        npcPolicy = SpawnerNpcPolicy.load(java.util.Objects.requireNonNull(data.readNbt()));
        playerSpawner = data.readBoolean();
        npcSpawnAll = data.readBoolean();
        baseId = data.readBoolean() ? data.readUUID() : null;
        baseName = data.readUtf();
        bases = data.readList(buffer -> new BaseChoice(buffer.readUUID(), buffer.readUtf(), buffer.readUtf(), buffer.readBlockPos()));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Minecraft.getInstance().setScreen(new TeamsSpawnerEditScreen(this));
    }
}
