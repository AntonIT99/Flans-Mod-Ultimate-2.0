package com.wolffsmod.npcs.network;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NpcRemotePacketTest
{
    @Test
    void radiusUsesExactInclusiveDistanceAndOrdersNumerically()
    {
        var near = entry(2, 2.01, 1);
        var sameRoundedDistance = entry(1, 2.04, 1);
        var boundary = entry(3, 16, 1);
        var outside = entry(4, 16.001, 1);
        var far = entry(5, 100, 1);
        var packet = new NpcRemotePacket(List.of(far, outside, boundary, sameRoundedDistance, near));

        assertEquals(List.of(near, sameRoundedDistance, boundary), packet.filtered(16, null));
        assertEquals(List.of(near, sameRoundedDistance, boundary, outside, far), packet.filtered(0, null));
    }

    @Test
    void combinesFactionAndRadiusAndRetainsDuplicateNamesWithStableTies()
    {
        var first = entry(1, 10, 1);
        var second = entry(2, 10, 1);
        var otherFaction = entry(3, 5, 2);
        var outside = entry(4, 50, 1);
        var packet = new NpcRemotePacket(List.of(second, otherFaction, outside, first));

        assertEquals(List.of(first, second), packet.filtered(16, 1));
        assertEquals(List.of(first, second, outside), packet.filtered(0, 1));
        assertTrue(packet.filtered(16, 3).isEmpty());
        assertTrue(new NpcRemotePacket(List.of()).filtered(0, null).isEmpty());
    }

    @Test
    void snapshotRoundTripPreservesPrecisionIdentityAndFactions()
    {
        var translated = new NpcRemotePacket.Entry(43, "Crewman", 16.002 * 16.002, 2, "Army", Component.translatableWithFallback("class.pack.crew", "Crewman"),
            Component.translatableWithFallback("team.pack.army", "Army"));
        var packet = new NpcRemotePacket(List.of(entry(42, 16.001, 1), translated));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            packet.write(buffer);
            assertEquals(packet, NpcRemotePacket.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
    }

    private static NpcRemotePacket.Entry entry(int id, double distance, int faction)
    {
        return new NpcRemotePacket.Entry(id, "Guard", distance * distance, faction, "Faction " + faction);
    }
}
