package com.flansmodultimate.network.client.effects;

import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PacketCombatFeedbackTest
{
    @Test
    void flybySoundAndClosestPointSurviveTheWireFormat()
    {
        RegistryFriendlyByteBuf original = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        RegistryFriendlyByteBuf encoded = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
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
            RegistryFriendlyByteBuf original = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            RegistryFriendlyByteBuf encoded = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
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
