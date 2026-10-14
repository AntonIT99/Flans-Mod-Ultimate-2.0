package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Hull and gun directions of an NPC drawn as a content-pack model, sent to the clients tracking it whenever they
 * change. Angles are in degrees and travel as whole half-degree steps.
 *
 * @param shots
 *            shots fired so far; a client shows a recoil stroke when the count grows
 * @param barrel
 *            the barrel that fired last
 */
// Packet arrays are written once and read once; nothing else holds on to them.
@SuppressWarnings({"java:S6218", "java:S2384"})
public record VehicleStatePacket(int entityId, float hullYaw, float turretYaw, float turretPitch, float[] seatYaw, float[] seatPitch, int shots, int barrel)
{

    private static final int MAX_SEATS = 64;

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeShort(steps(hullYaw));
        buffer.writeShort(steps(turretYaw));
        buffer.writeShort(steps(turretPitch));
        int seats = Math.min(MAX_SEATS, Math.min(seatYaw.length, seatPitch.length));
        buffer.writeByte(seats);
        for (int seat = 0; seat < seats; seat++)
        {
            buffer.writeShort(steps(seatYaw[seat]));
            buffer.writeShort(steps(seatPitch[seat]));
        }
        buffer.writeVarInt(shots);
        buffer.writeByte(Math.min(127, barrel));
    }

    public static VehicleStatePacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        float hullYaw = degrees(buffer.readShort());
        float turretYaw = degrees(buffer.readShort());
        float turretPitch = degrees(buffer.readShort());
        int seats = Math.min(MAX_SEATS, buffer.readByte() & 0xFF);
        float[] seatYaw = new float[seats];
        float[] seatPitch = new float[seats];
        for (int seat = 0; seat < seats; seat++)
        {
            seatYaw[seat] = degrees(buffer.readShort());
            seatPitch[seat] = degrees(buffer.readShort());
        }
        return new VehicleStatePacket(entityId, hullYaw, turretYaw, turretPitch, seatYaw, seatPitch, buffer.readVarInt(), buffer.readByte());
    }

    private static int steps(float degrees)
    {
        return Math.round(Math.max(-16000F, Math.min(16000F, degrees)) * 2F);
    }

    private static float degrees(int steps)
    {
        return steps / 2F;
    }
}
