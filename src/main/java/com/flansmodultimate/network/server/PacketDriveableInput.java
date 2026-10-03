package com.flansmodultimate.network.server;

import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    /** The client predicts the driveable and wants the server's movement reports. */
    private boolean predicting;
    /**
     * Driver only: per point of each weapon bank, the pivot of the barrel section
     * it is built on ({@code null} for the main gun's), read off the client's model.
     */
    @Nullable
    private Vec3[] primaryPitchPivots;
    @Nullable
    private Vec3[] secondaryPitchPivots;

    /** More shoot points than any bank holds; a larger count is a malformed packet. */
    private static final int MAX_SHOOT_POINT_PIVOTS = 64;

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

    /** Marks this input as coming from a client that predicts the driveable's movement. */
    public PacketDriveableInput withPrediction(boolean predicting)
    {
        this.predicting = predicting;
        return this;
    }

    /** Attaches the driver's per-shoot-point pitch pivots, in bank order. */
    public PacketDriveableInput withShootPointPitchPivots(@Nullable Vec3[] primary, @Nullable Vec3[] secondary)
    {
        this.primaryPitchPivots = primary;
        this.secondaryPitchPivots = secondary;
        return this;
    }

    private static void writePivots(PacketBuffer data, @Nullable Vec3[] pivots)
    {
        int count = pivots == null ? 0 : Math.min(pivots.length, MAX_SHOOT_POINT_PIVOTS);
        data.writeVarInt(count);
        for (int index = 0; index < count; index++)
        {
            Vec3 pivot = pivots[index];
            data.writeBoolean(pivot != null);
            if (pivot != null)
            {
                data.writeFloat((float) pivot.x);
                data.writeFloat((float) pivot.y);
                data.writeFloat((float) pivot.z);
            }
        }
    }

    private static Vec3[] readPivots(PacketBuffer data)
    {
        int count = data.readVarInt();
        if (count < 0 || count > MAX_SHOOT_POINT_PIVOTS)
            throw new DecoderException("Too many shoot-point pivots: " + count);
        Vec3[] pivots = new Vec3[count];
        for (int index = 0; index < count; index++)
        {
            if (data.readBoolean())
                pivots[index] = new Vec3(data.readFloat(), data.readFloat(), data.readFloat());
        }
        return pivots;
    }

    @Override
    public void encodeInto(PacketBuffer data)
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
        data.writeBoolean(predicting);
        writePivots(data, primaryPitchPivots);
        writePivots(data, secondaryPitchPivots);
    }

    @Override
    public void decodeInto(PacketBuffer data)
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
        predicting = data.readBoolean();
        primaryPitchPivots = readPivots(data);
        secondaryPitchPivots = readPivots(data);
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
            if (seat != null)
                seat.setInputPredicted(predicting && seat.isDriverSeat());
            if (seat != null && seat.isDriverSeat())
            {
                driveable.setModelBarrelPitchPivot(barrelPitchPivot);
                driveable.setModelShootPointPitchPivots(false, primaryPitchPivots);
                driveable.setModelShootPointPitchPivots(true, secondaryPitchPivots);
            }
            else if (seat != null)
                driveable.setModelPassengerGunAimPivot(seat.getSeatIndex(), barrelPitchPivot);
            driveable.acceptInput(player, inputMask, aimYaw, aimPitch,
                Mth.clamp(flightPitch, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL),
                Mth.clamp(flightRoll, -MAX_FLIGHT_CONTROL, MAX_FLIGHT_CONTROL), mouseControl, sequence);
        }
    }
}
