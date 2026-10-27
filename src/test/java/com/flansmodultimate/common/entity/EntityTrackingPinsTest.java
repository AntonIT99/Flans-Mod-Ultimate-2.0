package com.flansmodultimate.common.entity;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EntityTrackingPinsTest
{
    private ServerLevel level;
    private EntityPairingUpdate pairing;
    private ServerPlayer player;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException
    {
        level = mock(ServerLevel.class);
        ServerChunkCache chunkSource = mock(ServerChunkCache.class);
        ChunkMap chunkMap = mock(ChunkMap.class, withSettings().extraInterfaces(EntityPairingUpdate.class));
        var field = ServerChunkCache.class.getDeclaredField("chunkMap");
        field.setAccessible(true);
        field.set(chunkSource, chunkMap);
        when(level.getChunkSource()).thenReturn(chunkSource);
        pairing = (EntityPairingUpdate) chunkMap;
        player = playerIn(level);
    }

    @AfterEach
    void tearDown()
    {
        EntityTrackingPins.clear();
    }

    @Test
    void pinningPairsAtOnceAndReplacingUnpairsThePreviousEntity()
    {
        Entity first = entityIn(level);
        Entity second = entityIn(level);

        EntityTrackingPins.pin(player, first);
        assertTrue(EntityTrackingPins.isPinned(first, player));
        verify(pairing).flansmodultimateUpdatePairing(first, player);

        EntityTrackingPins.pin(player, second);
        assertFalse(EntityTrackingPins.isPinned(first, player));
        assertTrue(EntityTrackingPins.isPinned(second, player));
        verify(pairing, times(2)).flansmodultimateUpdatePairing(first, player);
        verify(pairing).flansmodultimateUpdatePairing(second, player);

        EntityTrackingPins.pin(player, null);
        assertFalse(EntityTrackingPins.isPinned(second, player));
        verify(pairing, times(2)).flansmodultimateUpdatePairing(second, player);
    }

    @Test
    void aPinBelongsToOnePlayerAndEndsWhenTheyLeave()
    {
        Entity npc = entityIn(level);
        ServerPlayer other = playerIn(level);

        EntityTrackingPins.pin(player, npc);
        assertFalse(EntityTrackingPins.isPinned(npc, other));

        EntityTrackingPins.unpin(player.getUUID());
        assertFalse(EntityTrackingPins.isPinned(npc, player));
    }

    @Test
    void anEntityInAnotherLevelIsNotPairedAcrossLevels()
    {
        Entity elsewhere = entityIn(mock(ServerLevel.class));

        EntityTrackingPins.pin(player, elsewhere);

        assertTrue(EntityTrackingPins.isPinned(elsewhere, player));
        verifyNoInteractions(pairing);
    }

    private static ServerPlayer playerIn(ServerLevel level)
    {
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.level()).thenReturn(level);
        return player;
    }

    private static Entity entityIn(ServerLevel level)
    {
        Entity entity = mock(Entity.class);
        when(entity.level()).thenReturn(level);
        return entity;
    }
}
