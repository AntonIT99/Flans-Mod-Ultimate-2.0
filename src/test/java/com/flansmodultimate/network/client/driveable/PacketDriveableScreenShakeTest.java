package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketDriveableScreenShakeTest
{
    @Test
    void cameraKickSurvivesThePacketRoundTrip()
    {
        PacketDriveableScreenShake original = new PacketDriveableScreenShake(new DriveableType.ScreenShake(2.5F, 0.18F));
        byte[] bytes = encode(original);
        assertEquals(8, bytes.length);
        var input = TestPacketBuffers.wrap(bytes);
        try
        {
            assertEquals(2.5F, input.getFloat(0));
            assertEquals(0.18F, input.getFloat(4));
            PacketDriveableScreenShake decoded = new PacketDriveableScreenShake();
            decoded.decodeInto(new PacketBuffer(input));
            assertEquals(0, input.readableBytes());
            assertArrayEquals(bytes, encode(decoded));
        }
        finally
        {
            input.release();
        }
        assertTrue(PacketHandler.clientPacketTypes().contains(PacketDriveableScreenShake.class));
        assertFalse(PacketHandler.serverPacketTypes().contains(PacketDriveableScreenShake.class));
    }

    private static byte[] encode(PacketDriveableScreenShake packet)
    {
        var buffer = TestPacketBuffers.buffer();
        try
        {
            packet.encodeInto(new PacketBuffer(buffer));
            return ByteBufUtil.getBytes(buffer);
        }
        finally
        {
            buffer.release();
        }
    }
}
