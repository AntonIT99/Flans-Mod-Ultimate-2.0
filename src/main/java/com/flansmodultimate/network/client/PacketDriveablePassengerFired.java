package com.flansmodultimate.network.client;

import com.flansmodultimate.client.render.entity.DriveableMuzzleFlashes;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** One successful shot from one barrel of a passenger gun. */
@NoArgsConstructor
public class PacketDriveablePassengerFired implements IClientPacket
{
    private int entityId;
    private int seat;
    private int barrel;

    public PacketDriveablePassengerFired(int entityId, int seat, int barrel)
    {
        this.entityId = entityId;
        this.seat = seat;
        this.barrel = barrel;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeVarInt(seat);
        data.writeVarInt(barrel);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        seat = data.readVarInt();
        barrel = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof Driveable driveable)
        {
            DriveableMuzzleFlashes.passengerFired(driveable, seat, barrel);
            driveable.spawnPassengerParticles(seat, barrel);
        }
    }
}
