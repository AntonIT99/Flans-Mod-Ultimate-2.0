package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PacketCombatFeedbackTest
{
    @Test
    void flybySoundAndClosestPointSurviveTheWireFormat()
    {
        var original = TestPacketBuffers.buffer();
        var encoded = TestPacketBuffers.buffer();
        try
        {
            // The text fallback can be exercised without a running Forge sound registry.
            new PacketBulletFlyby("Not A Registered Sound", new Vec3(-124.25D, 72.5D, 918.125D)).encodeInto(new PacketBuffer(original));
            byte[] expected = ByteBufUtil.getBytes(original);
            PacketBulletFlyby decoded = new PacketBulletFlyby();
            decoded.decodeInto(new PacketBuffer(original));
            decoded.encodeInto(new PacketBuffer(encoded));
            assertArrayEquals(expected, ByteBufUtil.getBytes(encoded));
        }
        finally
        {
            original.release();
            encoded.release();
        }
    }

    @Test
    void explosionAndDirectionalSuppressionSurviveTheWireFormat()
    {
        for (boolean explosion : new boolean[]{false, true})
        {
            var original = TestPacketBuffers.buffer();
            var encoded = TestPacketBuffers.buffer();
            try
            {
                new PacketCombatFeedback(explosion, 0.78F, -2.3F).encodeInto(new PacketBuffer(original));
                byte[] expected = ByteBufUtil.getBytes(original);
                PacketCombatFeedback decoded = new PacketCombatFeedback();
                decoded.decodeInto(new PacketBuffer(original));
                decoded.encodeInto(new PacketBuffer(encoded));
                assertArrayEquals(expected, ByteBufUtil.getBytes(encoded));
            }
            finally
            {
                original.release();
                encoded.release();
            }
        }
    }
}
