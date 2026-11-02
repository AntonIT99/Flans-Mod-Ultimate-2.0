package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.entity.ILivingVisualEffects;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketParticle;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BloodEffectsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void serverRuleGatesPlayersAndOptInNpcsButNeverOrdinaryMobs()
    {
        Player player = mock(Player.class);
        LivingEntity npc = mock(LivingEntity.class, withSettings().extraInterfaces(ILivingVisualEffects.class));
        ILivingVisualEffects effects = (ILivingVisualEffects) npc;
        when(effects.flansmodultimateBloodEffects()).thenReturn(true);
        assertTrue(BloodEffects.enabled(player, true));
        assertTrue(BloodEffects.enabled(npc, true));
        assertFalse(BloodEffects.enabled(player, false));
        assertFalse(BloodEffects.enabled(npc, false));
        assertFalse(BloodEffects.enabled(mock(LivingEntity.class), true));
        when(effects.flansmodultimateBloodEffects()).thenReturn(false);
        assertFalse(BloodEffects.enabled(npc, true));
        when(player.isSpectator()).thenReturn(true);
        assertFalse(BloodEffects.enabled(player, true));
    }

    @Test
    void deathsAndHeavyHitsSendBoundedBurstsOnlyToNearbyClients()
    {
        Player player = mock(Player.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.position()).thenReturn(new Vec3(0.5D, 1D, 0.5D));
        when(player.getRandom()).thenReturn(RandomSource.create(23));
        ServerChunkCache chunks = mock(ServerChunkCache.class);
        when(level.getChunkSource()).thenReturn(chunks);
        when(chunks.hasChunk(anyInt(), anyInt())).thenReturn(true);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getFluidState(any(BlockPos.class))).thenReturn(Fluids.EMPTY.defaultFluidState());
        when(level.clip(any(ClipContext.class))).thenReturn(new BlockHitResult(new Vec3(0.5D, 1D, 0.5D), Direction.UP, BlockPos.ZERO, false));
        TeamsManager manager = mock(TeamsManager.class);
        when(manager.isBloodEffects()).thenReturn(true);
        try (var managers = mockStatic(TeamsManager.class); var packets = mockStatic(PacketHandler.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            BloodEffects.onDamage(player, 40F);
            packets.verifyNoInteractions();
            BloodEffects.onDeath(player);
            packets.verify(() -> PacketHandler.sendToAllAround(any(PacketParticle.class), eq(new Vec3(0.5D, 1.003D, 0.5D)), eq(64D), eq(Level.OVERWORLD)));
            packets.clearInvocations();
            BloodEffects.onDamage(player, 41F);
            packets.verify(() -> PacketHandler.sendToAllAround(any(PacketParticle.class), any(Vec3.class), eq(64D), eq(Level.OVERWORLD)), times(7));
            packets.clearInvocations();
            when(player.isPassenger()).thenReturn(true);
            BloodEffects.onDamage(player, 100F);
            packets.verifyNoInteractions();
            when(manager.isBloodEffects()).thenReturn(false);
            BloodEffects.onDeath(player);
            packets.verifyNoInteractions();
        }
    }

    @Test
    void waterMissingGroundAndUnloadedChunksDoNotReceiveSplatters()
    {
        Player player = mock(Player.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.position()).thenReturn(new Vec3(0.5D, 1D, 0.5D));
        when(player.getRandom()).thenReturn(RandomSource.create(23));
        ServerChunkCache chunks = mock(ServerChunkCache.class);
        when(level.getChunkSource()).thenReturn(chunks);
        TeamsManager manager = mock(TeamsManager.class);
        when(manager.isBloodEffects()).thenReturn(true);
        try (var managers = mockStatic(TeamsManager.class); var packets = mockStatic(PacketHandler.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            BloodEffects.onDeath(player);
            verify(level, never()).clip(any(ClipContext.class));
            when(chunks.hasChunk(anyInt(), anyInt())).thenReturn(true);
            when(level.clip(any(ClipContext.class))).thenReturn(BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO));
            BloodEffects.onDeath(player);
            when(level.clip(any(ClipContext.class))).thenReturn(new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false));
            when(level.getFluidState(any(BlockPos.class))).thenReturn(Fluids.WATER.defaultFluidState());
            BloodEffects.onDeath(player);
            packets.verifyNoInteractions();
        }
    }

    @Test
    void onlyFinalPositiveDamageProducesAnAirborneWoundAtTheTracedPosition()
    {
        Player player = mock(Player.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.getRandom()).thenReturn(RandomSource.create(7));
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        TeamsManager manager = mock(TeamsManager.class);
        when(manager.isBloodEffects()).thenReturn(true);
        DamageSource source = mock(DamageSource.class);
        Vec3 wound = new Vec3(2D, 4D, 6D);
        try (var managers = mockStatic(TeamsManager.class); var packets = mockStatic(PacketHandler.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            BloodEffects.hurt(player, wound, new Vec3(1D, 0D, 0D), () ->
            {
                BloodEffects.onDamage(player, source, 0F);
                return true;
            });
            packets.verifyNoInteractions();
            BloodEffects.hurt(player, wound, new Vec3(1D, 0D, 0D), () ->
            {
                BloodEffects.onDamage(player, source, 4F);
                return true;
            });
            packets.verify(() -> PacketHandler.sendToAllAround(any(PacketParticle.class), eq(wound), eq(64D), eq(Level.OVERWORLD)));
            packets.clearInvocations();
            when(manager.isBloodEffects()).thenReturn(false);
            BloodEffects.hurt(player, wound, Vec3.ZERO, () ->
            {
                BloodEffects.onDamage(player, source, 41F);
                return true;
            });
            packets.verifyNoInteractions();
        }
    }

    @Test
    void nestedWoundsRestoreTheirCallerEvenWhenDamageThrows()
    {
        Player player = mock(Player.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.getRandom()).thenReturn(RandomSource.create(8));
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        TeamsManager manager = mock(TeamsManager.class);
        when(manager.isBloodEffects()).thenReturn(true);
        DamageSource source = mock(DamageSource.class);
        Vec3 outer = new Vec3(1D, 2D, 3D);
        Vec3 inner = new Vec3(4D, 5D, 6D);
        try (var managers = mockStatic(TeamsManager.class); var packets = mockStatic(PacketHandler.class))
        {
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            BloodEffects.hurt(player, outer, Vec3.ZERO, () ->
            {
                assertThrows(IllegalStateException.class, () -> BloodEffects.hurt(player, inner, Vec3.ZERO, () ->
                {
                    throw new IllegalStateException("Canceled by another damage handler");
                }));
                BloodEffects.onDamage(player, source, 2F);
                return true;
            });
            packets.verify(() -> PacketHandler.sendToAllAround(any(PacketParticle.class), eq(outer), eq(64D), eq(Level.OVERWORLD)));
            packets.clearInvocations();
            BloodEffects.onDamage(player, source, 2F);
            packets.verifyNoInteractions();
        }
    }

}
