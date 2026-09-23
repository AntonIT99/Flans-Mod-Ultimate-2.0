package com.flansmodultimate.network.client;

import com.flansmodultimate.common.driveables.DriveablePrediction;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.network.IClientPacket;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The server's state of a driveable after one of its ticks, sent only to a driver
 * whose client predicts it, with the last input step the server applied.
 */
public final class PacketDriveablePrediction implements IClientPacket
{
    private int driveableId;
    private int serverStep;
    private int acknowledgedStep;
    private DriveablePrediction.State state;

    public PacketDriveablePrediction() {}

    public PacketDriveablePrediction(@NotNull Driveable driveable, int serverStep, int acknowledgedStep,
                                     @NotNull DriveablePrediction.State state)
    {
        driveableId = driveable.getId();
        this.serverStep = serverStep;
        this.acknowledgedStep = acknowledgedStep;
        this.state = state;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeVarInt(driveableId);
        data.writeVarInt(serverStep);
        data.writeVarInt(acknowledgedStep);
        data.writeDouble(state.x());
        data.writeDouble(state.y());
        data.writeDouble(state.z());
        data.writeFloat((float) state.vx());
        data.writeFloat((float) state.vy());
        data.writeFloat((float) state.vz());
        data.writeFloat(state.yaw());
        data.writeFloat(state.pitch());
        data.writeFloat(state.roll());
        data.writeFloat(state.throttle());
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        driveableId = data.readVarInt();
        serverStep = data.readVarInt();
        acknowledgedStep = data.readVarInt();
        state = new DriveablePrediction.State(data.readDouble(), data.readDouble(), data.readDouble(),
            data.readFloat(), data.readFloat(), data.readFloat(),
            data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat());
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(driveableId) instanceof Driveable driveable)
            driveable.acceptPredictionReport(serverStep, acknowledgedStep, state);
    }
}
