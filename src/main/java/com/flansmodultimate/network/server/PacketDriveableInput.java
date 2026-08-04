package com.flansmodultimate.network.server;

import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.network.IServerPacket;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Client input intent for a driveable. No position, velocity, fuel, inventory,
 * cooldown or damage state is accepted from the client.
 */
@Getter
public final class PacketDriveableInput implements IServerPacket
{
    private static final float MAX_ABSOLUTE_AIM = 360_000F;

    private int driveableId;
    private int inputMask;
    private float aimYaw;
    private float aimPitch;
    private float flightPitch;
    private float flightRoll;
    private boolean mouseControl;
    private int sequence;

    public PacketDriveableInput()
    {
    }

    public PacketDriveableInput(Driveable driveable, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        this(driveable.getId(), inputMask, aimYaw, aimPitch, flightPitch, flightRoll, mouseControl, sequence);
    }

    public PacketDriveableInput(Seat seat, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        this(seat.getDriveable() == null ? -1 : seat.getDriveable().getId(), inputMask, aimYaw, aimPitch,
            flightPitch, flightRoll, mouseControl, sequence);
    }

    public PacketDriveableInput(int driveableId, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        this.driveableId = driveableId;
        this.inputMask = DriveableInput.sanitize(inputMask);
        this.aimYaw = aimYaw;
        this.aimPitch = aimPitch;
        this.flightPitch = Mth.clamp(flightPitch, -1F, 1F);
        this.flightRoll = Mth.clamp(flightRoll, -1F, 1F);
        this.mouseControl = mouseControl;
        this.sequence = sequence;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeVarInt(driveableId);
        data.writeVarInt(DriveableInput.sanitize(inputMask));
        data.writeFloat(aimYaw);
        data.writeFloat(aimPitch);
        data.writeFloat(Mth.clamp(flightPitch, -1F, 1F));
        data.writeFloat(Mth.clamp(flightRoll, -1F, 1F));
        data.writeBoolean(mouseControl);
        data.writeVarInt(sequence);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        driveableId = data.readVarInt();
        inputMask = DriveableInput.sanitize(data.readVarInt());
        aimYaw = data.readFloat();
        aimPitch = data.readFloat();
        flightPitch = data.readFloat();
        flightRoll = data.readFloat();
        mouseControl = data.readBoolean();
        sequence = data.readVarInt();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (driveableId < 0 || !Float.isFinite(aimYaw) || !Float.isFinite(aimPitch)
            || !Float.isFinite(flightPitch) || !Float.isFinite(flightRoll)
            || Math.abs(aimYaw) > MAX_ABSOLUTE_AIM || Math.abs(aimPitch) > MAX_ABSOLUTE_AIM)
            return;

        Entity entity = level.getEntity(driveableId);
        if (entity instanceof Driveable driveable)
            driveable.acceptInput(player, inputMask, aimYaw, aimPitch, Mth.clamp(flightPitch, -1F, 1F),
                Mth.clamp(flightRoll, -1F, 1F), mouseControl, sequence);
    }
}
