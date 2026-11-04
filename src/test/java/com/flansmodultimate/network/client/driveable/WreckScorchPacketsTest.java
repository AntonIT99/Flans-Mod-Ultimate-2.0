package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class WreckScorchPacketsTest
{
    @Test
    void crashAndImpactPacketsPreserveTheWreckIdentityAndEffectData()
    {
        Vec3 position = new Vec3(123.5D, 70D, -40.25D);
        roundTrip(new PacketDriveableCrashFireball(381, position, 6F), new PacketDriveableCrashFireball(), position, 6F, null);
        roundTrip(new PacketDriveableImpact(381, position, 4F, 0.8F), new PacketDriveableImpact(), position, 4F, 0.8F);
    }

    @Test
    void packetsRemainRegisteredOnTheCommonSide()
    {
        assertTrue(PacketHandler.clientPacketTypes().contains(PacketDriveableCrashFireball.class));
        assertTrue(PacketHandler.clientPacketTypes().contains(PacketDriveableImpact.class));
    }

    private static void roundTrip(IClientPacket original, IClientPacket decoded, Vec3 position, float radius, Float severity)
    {
        PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            original.encodeInto(buffer);
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            buffer.clear();
            decoded.encodeInto(buffer);
            assertEquals(381, buffer.readVarInt());
            assertEquals(position.x, buffer.readDouble());
            assertEquals(position.y, buffer.readDouble());
            assertEquals(position.z, buffer.readDouble());
            assertEquals(radius, buffer.readFloat());
            if (severity != null)
                assertEquals(severity.floatValue(), buffer.readFloat());
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
    }
}
