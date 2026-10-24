package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.IPlayerClass;
import com.flansmodultimate.api.ITeam;
import com.wolffsmod.npcs.network.SoldierNamePacket;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SoldierNamePacketTest
{
    @Test
    void requestAndGeneratedStateRoundTripWithStableListIdentity()
    {
        IPlayerClass playerClass = mock(IPlayerClass.class);
        ITeam team = mock(ITeam.class);
        when(team.getShortName()).thenReturn("team");
        when(playerClass.getShortName()).thenReturn("class");
        SoldierNamePacket request = SoldierNamePacket.request(42, new FlanSoldierSelection(team, playerClass));
        assertEquals(new SoldierNamePacket(42, "team", "class", false, "", ""), request);
        assertEquals("", SoldierNamePacket.request(42, new FlanSoldierSelection(null, playerClass)).team());
        for (SoldierNamePacket packet : new SoldierNamePacket[]{request, new SoldierNamePacket(42, "team", "class", true, "wolffsmodnpcs:german_20th", "Hans Müller")})
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try
            {
                packet.write(buffer);
                assertEquals(packet, SoldierNamePacket.read(buffer));
                assertEquals(0, buffer.readableBytes());
            }
            finally
            {
                buffer.release();
            }
        }
    }

    @Test
    void stateRequiresConfirmationAndRequestsIgnoreClientSuppliedNames()
    {
        try (var requests = mockStatic(FlanSoldierNames.class))
        {
            SoldierNamePacket state = new SoldierNamePacket(42, "team", "class", true, "wolffsmodnpcs:german", "Injected name");
            state.handle(null);
            requests.verify(() -> FlanSoldierNames.confirm(null, state));
            requests.verifyNoMoreInteractions();
            requests.when(() -> FlanSoldierNames.request(null, 42, "team", "class")).thenReturn(Optional.empty());
            new SoldierNamePacket(42, "team", "class", false, "wolffsmodnpcs:german", "Injected name").handle(null);
            requests.verify(() -> FlanSoldierNames.request(null, 42, "team", "class"));
        }
    }
}
