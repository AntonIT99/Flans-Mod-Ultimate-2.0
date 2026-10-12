package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.network.server.gun.PacketGunSecondaryMode;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

import static org.junit.jupiter.api.Assertions.*;

class PacketGunSecondaryModeTest
{
    @Test
    void requestCarriesOnlyTheHandAndAcknowledgmentCarriesServerTiming()
    {
        FriendlyByteBuf request = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf acknowledgment = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf roundTrip = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            new PacketGunSecondaryMode(InteractionHand.OFF_HAND).encodeInto(new PacketBuffer(request));
            assertEquals(InteractionHand.OFF_HAND, request.readEnum(InteractionHand.class));
            assertEquals(0, request.readableBytes());
            new PacketGunSecondaryModeClient(InteractionHand.OFF_HAND, true, 9).encodeInto(new PacketBuffer(acknowledgment));
            byte[] expected = ByteBufUtil.getBytes(acknowledgment);
            PacketGunSecondaryModeClient decoded = new PacketGunSecondaryModeClient();
            decoded.decodeInto(new PacketBuffer(acknowledgment));
            assertEquals(0, acknowledgment.readableBytes());
            decoded.encodeInto(new PacketBuffer(roundTrip));
            assertArrayEquals(expected, ByteBufUtil.getBytes(roundTrip));
            assertEquals(InteractionHand.OFF_HAND, roundTrip.readEnum(InteractionHand.class));
            assertTrue(roundTrip.readBoolean());
            assertEquals(9, roundTrip.readVarInt());
        }
        finally
        {
            request.release();
            acknowledgment.release();
            roundTrip.release();
        }
    }
}
