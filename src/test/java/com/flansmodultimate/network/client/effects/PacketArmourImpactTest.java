package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class PacketArmourImpactTest
{
    @Test
    void blockedAndPenetratingContactsKeepTheirPositionDirectionAndSize()
    {
        for (boolean blocked : new boolean[]{true, false})
        {
            PacketBuffer original = new PacketBuffer(TestPacketBuffers.buffer());
            PacketBuffer encoded = new PacketBuffer(TestPacketBuffers.buffer());
            try
            {
                new PacketArmourImpact(new Vec3(29000123.125D, 72.5D, -918.125D), new Vec3(1D, 0D, 0D), new Vec3(-1D, 0D, 0D), 2.5F, blocked).encodeInto(original);
                byte[] expected = ByteBufUtil.getBytes(original);
                PacketArmourImpact decoded = new PacketArmourImpact();
                decoded.decodeInto(original);
                assertEquals(0, original.readableBytes());
                decoded.encodeInto(encoded);
                assertArrayEquals(expected, ByteBufUtil.getBytes(encoded));
            }
            finally
            {
                original.release();
                encoded.release();
            }
        }
    }

    @Test
    void effectIsRegisteredWithoutLoadingClientRenderingClasses()
    {
        assertTrue(PacketHandler.clientPacketTypes().contains(PacketArmourImpact.class));
    }
}
