package com.flansmodultimate.common.teams;

import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.PacketIO;

import net.minecraft.network.chat.Component;

import java.util.UUID;

/** A server-authored vehicle deployment marker. Delay ticks are measured when the snapshot is made. */
public record VehicleDeploySnapshot(UUID id, Component name, double x, double z, Availability availability, int delayTicks)
{
    public enum Availability
    {
        READY, FULL, COOLDOWN, UNAVAILABLE, LOCKED
    }

    /** Counts down between server updates using elapsed real milliseconds; reaching zero does not grant availability. */
    public int remainingSeconds(long elapsedMillis)
    {
        return (int) Math.max(0L, (Math.max(0, delayTicks) * 50L - Math.max(0L, elapsedMillis) + 999L) / 1000L);
    }

    public void write(PacketBuffer buffer)
    {
        buffer.writeUUID(id);
        PacketIO.writeComponent(buffer, name);
        buffer.writeDouble(x);
        buffer.writeDouble(z);
        buffer.writeEnum(availability);
        buffer.writeVarInt(delayTicks);
    }

    public static VehicleDeploySnapshot read(PacketBuffer buffer)
    {
        return new VehicleDeploySnapshot(buffer.readUUID(), PacketIO.readComponent(buffer), buffer.readDouble(), buffer.readDouble(), buffer.readEnum(Availability.class),
            Math.max(0, buffer.readVarInt()));
    }
}
