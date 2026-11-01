package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketGunShootClientTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void deathStopsBothHandsEvenWhileThePlayerEntityIsUnloaded()
    {
        UUID playerId = UUID.randomUUID();
        PlayerData clientData = PlayerData.getClientInstance(playerId);
        PlayerData serverData = PlayerData.getInstance(playerId);
        Player receiver = mock(Player.class);
        Level level = mock(Level.class);
        for (InteractionHand hand : InteractionHand.values())
        {
            clientData.setShooting(hand, true);
            serverData.setShooting(hand, true);
            new PacketGunShootClient(playerId, hand, false).handleClientSide(receiver, level);
            assertFalse(PlayerData.getClientInstance(playerId).isShooting(hand));
            assertTrue(serverData.isShooting(hand));
        }
        PlayerData.removeServerData(playerId);
    }
}
