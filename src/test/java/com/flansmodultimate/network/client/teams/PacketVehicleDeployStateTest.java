package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.common.teams.ObjectiveMode;
import com.flansmodultimate.common.teams.VehicleDeploySnapshot;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketVehicleDeployStateTest
{
    @Test
    void snapshotRoundTripRetainsMarkerIdentityPositionNameAndCooldown() throws ReflectiveOperationException
    {
        VehicleDeploySnapshot point = new VehicleDeploySnapshot(UUID.randomUUID(), Component.translatable("gui.flansmod.teams.deploy.vehicle.ready"), 20.5D, -40.5D,
            VehicleDeploySnapshot.Availability.COOLDOWN, 2399);
        PacketTeamsState packet = new PacketTeamsState();
        set(packet, "objectiveMode", ObjectiveMode.CONQUEST);
        set(packet, "vehicleDeployments", List.of(point));
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            packet.encodeInto(buffer);
            PacketTeamsState decoded = new PacketTeamsState();
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            assertEquals(List.of(point), decoded.getVehicleDeployments());
        }
        finally
        {
            buffer.release();
        }
    }

    @Test
    void realTimeCountdownDoesNotRestartOrGrantAvailabilityBetweenServerUpdates()
    {
        VehicleDeploySnapshot point = new VehicleDeploySnapshot(UUID.randomUUID(), Component.literal("Tank"), 0D, 0D, VehicleDeploySnapshot.Availability.COOLDOWN, 41);
        assertEquals(3, point.remainingSeconds(0L));
        assertEquals(2, point.remainingSeconds(50L));
        assertEquals(1, point.remainingSeconds(1050L));
        assertEquals(0, point.remainingSeconds(2050L));
        assertEquals(0, point.remainingSeconds(Long.MAX_VALUE));
        assertEquals(VehicleDeploySnapshot.Availability.COOLDOWN, point.availability());
    }

    private static void set(PacketTeamsState packet, String name, Object value) throws ReflectiveOperationException
    {
        var field = PacketTeamsState.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(packet, value);
    }
}
