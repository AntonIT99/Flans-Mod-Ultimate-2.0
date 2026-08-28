package com.flansmodultimate.network.server;

import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.network.IServerPacket;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client input intent for a driveable. No position, velocity, fuel, inventory,
 * cooldown or damage state is accepted from the client.
 */
@Getter
public final class PacketDriveableInput implements IServerPacket
{
    private static final float MAX_ABSOLUTE_AIM = 360_000F;
    private static final float MAX_FLIGHT_CONTROL = 20F;

    private int driveableId;
    private int inputMask;
    private float aimYaw;
    private float aimPitch;
    private float flightPitch;
    private float flightRoll;
    private boolean mouseControl;
    @Nullable
    private Vec3 barrelPitchPivot;
    private int sequence;

    public PacketDriveableInput()
    {
    }

    public PacketDriveableInput(Driveable driveable, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        this(driveable.getId(), inputMask, aimYaw, aimPitch, flightPitch, flightRoll, mouseControl, null, sequence);
    }

    public PacketDriveableInput(Driveable driveable, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl,
                                @Nullable Vec3 barrelPitchPivot, int sequence)
    {
        this(driveable.getId(), inputMask, aimYaw, aimPitch, flightPitch, flightRoll, mouseControl,
            barrelPitchPivot, sequence);
    }

    public PacketDriveableInput(Seat seat, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        this(seat.getDriveable() == null ? -1 : seat.getDriveable().getId(), inputMask, aimYaw, aimPitch,
            flightPitch, flightRoll, mouseControl, null, sequence);
    }

    public PacketDriveableInput(Seat seat, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl,
                                @Nullable Vec3 barrelPitchPivot, int sequence)
    {
        this(seat.getDriveable() == null ? -1 : seat.getDriveable().getId(), inputMask, aimYaw, aimPitch,
            flightPitch, flightRoll, mouseControl, barrelPitchPivot, sequence);
    }

    public PacketDriveableInput(int driveableId, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl, int sequence)
    {
        this(driveableId, inputMask, aimYaw, aimPitch, flightPitch, flightRoll, mouseControl, null, sequence);
    }

    public PacketDriveableInput(int driveableId, int inputMask, float aimYaw, float aimPitch,
                                float flightPitch, float flightRoll, boolean mouseControl,
                                @Nullable Vec3 barrelPitchPivot, int sequence)
    {
        this.driveableId = driveableId;
        this.inputMask = DriveableInput.sanitize(inputMask);
        this.aimYaw = aimYaw;
        this.aimPitch = aimPitch;
        this.flightPitch = Mth.clamp(flightPitch, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL);
        this.flightRoll = Mth.clamp(flightRoll, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL);
        this.mouseControl = mouseControl;
        this.barrelPitchPivot = barrelPitchPivot;
        this.sequence = sequence;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeVarInt(driveableId);
        data.writeVarInt(DriveableInput.sanitize(inputMask));
        data.writeFloat(aimYaw);
        data.writeFloat(aimPitch);
        data.writeFloat(Mth.clamp(flightPitch, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL));
        data.writeFloat(Mth.clamp(flightRoll, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL));
        data.writeBoolean(mouseControl);
        data.writeBoolean(barrelPitchPivot != null);
        if (barrelPitchPivot != null)
        {
            data.writeDouble(barrelPitchPivot.x);
            data.writeDouble(barrelPitchPivot.y);
            data.writeDouble(barrelPitchPivot.z);
        }
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
        barrelPitchPivot = data.readBoolean()
            ? new Vec3(data.readDouble(), data.readDouble(), data.readDouble()) : null;
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
        {
            Seat seat = driveable.getSeat(player);
            if (seat != null && seat.isDriverSeat())
                driveable.setModelBarrelPitchPivot(barrelPitchPivot);
            else if (seat != null)
                driveable.setModelPassengerGunAimPivot(seat.getSeatIndex(), barrelPitchPivot);
            driveable.acceptInput(player, inputMask, aimYaw, aimPitch,
                Mth.clamp(flightPitch, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL),
                Mth.clamp(flightRoll, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL), mouseControl, sequence);
        }
    }
}
