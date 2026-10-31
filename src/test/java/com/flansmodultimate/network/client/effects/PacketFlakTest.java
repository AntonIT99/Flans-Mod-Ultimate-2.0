package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.network.ParticleNameCodec;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class PacketFlakTest
{
    @Test
    void sourceIdentitySurvivesTransportAndOrdinaryBurstsRemainSeparate()
    {
        for (int source : new int[]{-1, 0, 12345})
        {
            PacketBuffer buffer = new PacketBuffer(TestPacketBuffers.buffer());
            try
            {
                PacketFlak packet = source < 0 ? new PacketFlak(new Vec3(12D, 64D, -8D), 50, "flansmod.Smoker1") : new PacketFlak(new Vec3(12D, 64D, -8D), 50, "flansmod.Smoker1", source);
                packet.encodeInto(buffer);
                PacketFlak decoded = new PacketFlak();
                decoded.decodeInto(buffer);
                assertEquals(0, buffer.readableBytes());
                buffer.clear();
                decoded.encodeInto(buffer);
                assertEquals(12D, buffer.readDouble());
                assertEquals(64D, buffer.readDouble());
                assertEquals(-8D, buffer.readDouble());
                assertEquals(50, buffer.readInt());
                assertEquals("flansmod.Smoker1", ParticleNameCodec.read(buffer));
                assertEquals(source, buffer.readInt());
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }
}
