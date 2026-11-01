package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.api.teams.SpawnerNpcPolicy;
import com.flansmodultimate.client.gui.TeamsSpawnerEditScreen;
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

import java.util.UUID;

/** Server snapshot for the operator's item or vehicle spawner editor. */
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

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBlockPos(pos);
        data.writeUUID(objectId);
        data.writeVarInt(delayTicks);
        data.writeBoolean(vehicle);
        data.writeBoolean(waitForVehicle);
        data.writeNbt(npcPolicy.save());
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
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Minecraft.getInstance().setScreen(new TeamsSpawnerEditScreen(this));
    }
}
