package com.flansmodultimate.network.server.driveable;

import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The range a vehicle driver lased for its fire control, or a negative one to clear it. */
@NoArgsConstructor
public class PacketFireControlRange implements IServerPacket
{
    private float range;

    public PacketFireControlRange(float range)
    {
        this.range = range;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeFloat(range);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        range = data.readFloat();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        // Only the driver of the vehicle the player sits in can set its fire control
        if (player.getVehicle() instanceof Seat seat && seat.isDriverSeat() && seat.getDriveable() instanceof Vehicle vehicle && vehicle.getControllingEntity() == player)
            vehicle.setFireControlRange(player, range);
    }
}
