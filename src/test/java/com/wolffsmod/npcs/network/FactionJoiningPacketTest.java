package com.wolffsmod.npcs.network;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.teams.FactionJoining;
import com.wolffsmod.npcs.teams.FactionRelations;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class FactionJoiningPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void joiningChangesRoundTrip()
    {
        for (FactionJoiningPacket packet : List.of(new FactionJoiningPacket(42, 3, FactionJoining.Rule.POINTS, 250, 30, FactionJoiningPacket.Membership.KEEP),
            new FactionJoiningPacket(42, 7, FactionJoining.Rule.INVITE, 0, 0, FactionJoiningPacket.Membership.LEAVE)))
            assertEquals(packet, roundTrip(packet, FactionJoiningPacket::write, FactionJoiningPacket::read));
    }

    @Test
    void theFactionListCarriesJoiningRulesMembersAndTheBuildersFaction()
    {
        FactionRelationsPacket.Entry entry = new FactionRelationsPacket.Entry(3, Component.literal("Red Army"), 0xAA0000, List.of(7), List.of(), FactionJoining.Rule.OPEN, 100, 15, 4);
        FactionRelationsPacket state = new FactionRelationsPacket(42, null, List.of(), List.of(entry), 3);
        FactionRelationsPacket request = FactionRelationsPacket.set(42, FactionRelations.Relation.ALLIED, List.of(3, 7));
        assertEquals(state, roundTrip(state, FactionRelationsPacket::write, FactionRelationsPacket::read));
        assertEquals(request, roundTrip(request, FactionRelationsPacket::write, FactionRelationsPacket::read));
    }

    private static <P> P roundTrip(P packet, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            write.accept(packet, buffer);
            P copy = read.apply(buffer);
            assertEquals(0, buffer.readableBytes());
            return copy;
        }
        finally
        {
            buffer.release();
        }
    }
}
