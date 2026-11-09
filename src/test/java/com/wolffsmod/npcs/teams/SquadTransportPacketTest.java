package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.network.BotOrderPacket;
import com.wolffsmod.npcs.network.BotOrdersStatePacket;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SquadTransportPacketTest
{
    @Test
    void ordersRoundTripTransportUuidAndCancellationWithoutAClientPosition()
    {
        for (BotOrderPacket packet : List.of(new BotOrderPacket(BotOrderPacket.Action.BOARD_TRANSPORT, 7, PlayerOrder.Kind.RELEASE, BotOrders.HERE, null, UUID.randomUUID()),
            new BotOrderPacket(BotOrderPacket.Action.DISEMBARK_TRANSPORT, 7, PlayerOrder.Kind.RELEASE, BotOrders.HERE, null), BotOrderPacket.refresh()))
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try
            {
                packet.write(buffer);
                assertEquals(packet, BotOrderPacket.read(buffer));
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @Test
    void serverStateCarriesCapacityAndPartialBoardingProgressWithoutOpeningOnRefresh()
    {
        BotOrdersStatePacket packet = new BotOrdersStatePacket(true, 0, false, "", List.of(new BotOrdersStatePacket.Squad(7, "Section", 6, "hold", true, 12, 10D, 20D, "boarding", 4, 6)),
            List.of(), BotOrdersStatePacket.Leading.NONE, true, List.of(new BotOrdersStatePacket.Transport(UUID.randomUUID(), "APC", 2)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            packet.opening(false).write(buffer);
            BotOrdersStatePacket received = BotOrdersStatePacket.read(buffer);
            assertEquals(packet.opening(false), received);
            assertFalse(received.open());
            assertEquals(4, received.squads().get(0).aboard());
            assertEquals(2, received.transports().get(0).seats());
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
    }
}
