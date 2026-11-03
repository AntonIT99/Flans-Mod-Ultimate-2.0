package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Hull and gun directions of an NPC drawn as a content-pack model, sent to the clients tracking it whenever they
 * change. Angles are in degrees; gun angles travel in half-degree steps and terrain angles in 1/32-degree steps.
 *
 * @param shots
 *            shots fired so far; a client shows a recoil stroke when the count grows
 * @param barrel
 *            the barrel that fired last
 * @param attitudePitch
 *            degrees a plane in flight is pitched nose down, negative nose up
 * @param attitudeBank
 *            degrees a plane in flight is rolled to its right
 * @param attitudeLift
 *            blocks a ground vehicle's model is drawn above, negative below, the NPC's feet, in steps of 1/{@value #LIFT_STEPS}
 */
// Packet arrays are written once and read once; nothing else holds on to them.
@SuppressWarnings({"java:S6218", "java:S2384"})
public record VehicleStatePacket(int entityId, float hullYaw, float turretYaw, float turretPitch, float[] seatYaw, float[] seatPitch, int shots, int barrel, float attitudePitch, float attitudeBank,
    float attitudeLift)
{
    /** Steps per block in which the lift travels. */
    public static final int LIFT_STEPS = 64;
    /** Steps per degree for the model's pitch and bank. */
    public static final int ATTITUDE_STEPS = 32;

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
        buffer.writeShort(Math.round(Math.max(-180F, Math.min(180F, attitudePitch)) * ATTITUDE_STEPS));
        buffer.writeShort(Math.round(Math.max(-180F, Math.min(180F, attitudeBank)) * ATTITUDE_STEPS));
        buffer.writeShort(Math.round(Math.max(-500F, Math.min(500F, attitudeLift)) * LIFT_STEPS));
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
        return new VehicleStatePacket(entityId, hullYaw, turretYaw, turretPitch, seatYaw, seatPitch, buffer.readVarInt(), buffer.readByte(), buffer.readShort() / (float) ATTITUDE_STEPS,
            buffer.readShort() / (float) ATTITUDE_STEPS, buffer.readShort() / (float) LIFT_STEPS);
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
