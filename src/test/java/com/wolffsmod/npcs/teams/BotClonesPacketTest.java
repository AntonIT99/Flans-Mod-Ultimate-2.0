package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.CrewSource;
import com.wolffsmod.npcs.network.BotClonesPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import io.netty.buffer.Unpooled;
import noppes.npcs.controllers.ServerCloneController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotClonesPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void requestsCarryNoCloneDataAndAnswersKeepTheTabNameAndToken()
    {
        CompoundTag data = new CompoundTag();
        data.putString("Name", "Grenadier");
        UUID token = UUID.randomUUID();
        BotClonesPacket request = BotClonesPacket.preview(4, "Grenadier", token);
        assertEquals(request, roundTrip(request));
        // A request claiming data is sent without it: only the server supplies clone data.
        assertEquals(request, roundTrip(new BotClonesPacket(4, "Grenadier", token, true, List.of("Other"), data)));
        BotClonesPacket answer = request.answer(List.of("Grenadier", "Rifleman"), data);
        assertEquals(new BotClonesPacket(4, "Grenadier", token, false, List.of("Grenadier", "Rifleman"), data), roundTrip(answer));
    }

    private static BotClonesPacket roundTrip(BotClonesPacket packet)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            packet.write(buffer);
            BotClonesPacket decoded = BotClonesPacket.read(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        }
        finally
        {
            buffer.release();
        }
    }

    @Test
    void serverAnswersOperatorsOnlyAndReadsOnlyListedClones()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerCloneController previous = ServerCloneController.Instance;
        ServerCloneController clones = mock(ServerCloneController.class);
        ServerCloneController.Instance = clones;
        when(clones.getClones(3)).thenReturn(List.of("Grenadier"));
        CompoundTag data = new CompoundTag();
        data.putString("Name", "Grenadier");
        when(clones.getCloneData(null, "Grenadier", 3)).thenReturn(data);
        UUID token = UUID.randomUUID();
        try (var network = mockStatic(NpcNetwork.class))
        {
            BotClonesPacket.list(3, token).handle(player);
            verifyNoInteractions(clones);

            when(player.hasPermissions(anyInt())).thenReturn(true);
            BotClonesPacket.list(0, token).handle(player);
            BotClonesPacket.list(CrewSource.MAX_CLONE_TAB + 1, token).handle(player);
            verifyNoInteractions(clones);

            BotClonesPacket.list(3, token).handle(player);
            network.verify(() -> NpcNetwork.sendTo(player, new BotClonesPacket(3, "", token, false, List.of("Grenadier"), new CompoundTag())));

            BotClonesPacket.preview(3, "../secret", token).handle(player);
            verify(clones, never()).getCloneData(any(), anyString(), anyInt());
            network.verify(() -> NpcNetwork.sendTo(player, new BotClonesPacket(3, "../secret", token, false, List.of(), new CompoundTag())));

            // Answers arriving at the server are ignored.
            new BotClonesPacket(3, "Grenadier", token, false, List.of(), data).handle(player);
            verify(clones, never()).getCloneData(any(), anyString(), anyInt());

            BotClonesPacket.preview(3, "Grenadier", token).handle(player);
            verify(clones).getCloneData(null, "Grenadier", 3);
            network.verify(() -> NpcNetwork.sendTo(player, new BotClonesPacket(3, "Grenadier", token, false, List.of(), data)));
            verify(clones, never()).spawn(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyString(), any());
        }
        finally
        {
            ServerCloneController.Instance = previous;
        }
    }
}
