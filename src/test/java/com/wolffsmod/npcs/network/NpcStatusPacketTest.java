package com.wolffsmod.npcs.network;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.diag.NpcStatus;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import static org.junit.jupiter.api.Assertions.*;

class NpcStatusPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void aRequestCarriesNoStatus()
    {
        NpcStatusPacket copy = roundTrip(NpcStatusPacket.request(42));
        assertEquals(42, copy.entityId());
        assertNull(copy.status());
    }

    @Test
    void aFightingVehicleRoundTrips()
    {
        NpcStatus status = new NpcStatus(NpcStatus.Activity.HELD, 812.5F, 1200F, NpcStatus.Behaviour.FLANKING, Component.literal("Zombie"), 42.5F, true, Component.literal("Skeleton"), true, true, 3,
            4, true, NpcStatus.NEVER, 18.25F, 6.5F);
        assertEquals(status, roundTrip(new NpcStatusPacket(7, status)).status());
    }

    @Test
    void anIdleSoldierWithoutTargetSeatsOrThreatRoundTrips()
    {
        NpcStatus status = new NpcStatus(NpcStatus.Activity.KILLED, 0F, 20F, NpcStatus.Behaviour.IDLE, null, Float.NaN, false, null, false, false, NpcStatus.NO_CREW, NpcStatus.NO_CREW, false, 12, 0F,
            0F);
        assertEquals(status, roundTrip(new NpcStatusPacket(7, status)).status());
    }

    private static NpcStatusPacket roundTrip(NpcStatusPacket packet)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            packet.write(buffer);
            NpcStatusPacket copy = NpcStatusPacket.read(buffer);
            assertEquals(0, buffer.readableBytes());
            return copy;
        }
        finally
        {
            buffer.release();
        }
    }
}
