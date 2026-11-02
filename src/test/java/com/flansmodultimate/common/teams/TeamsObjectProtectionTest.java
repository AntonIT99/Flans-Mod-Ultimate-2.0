package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.block.CapturePointBlock;
import com.flansmodultimate.common.block.TeamBannerBlock;
import com.flansmodultimate.common.block.TeamObjectBlock;
import com.flansmodultimate.common.block.TeamSpawnerBlock;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsObjectProtectionTest
{
    private static java.util.List<TeamObjectBlock> blocks;

    @BeforeAll
    @SuppressWarnings("deprecation") // Plain JUnit must open the block registry that NeoForge's loader normally fills.
    static void bootstrap()
    {
        TeamsMapTest.bootstrapRegistries();
        MappedRegistry<Block> registry = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;
        registry.unfreeze();
        try
        {
            blocks = java.util.List.of(new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.PLAYER, BlockBehaviour.Properties.of()),
                new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.ITEM, BlockBehaviour.Properties.of()), new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.VEHICLE, BlockBehaviour.Properties.of()),
                new TeamBannerBlock(BlockBehaviour.Properties.of()), new CapturePointBlock(BlockBehaviour.Properties.of()));
            for (int i = 0; i < blocks.size(); i++)
                Registry.register(registry, ResourceLocation.fromNamespaceAndPath("flansmodultimate", "test_protected_object_" + i), blocks.get(i));
        }
        finally
        {
            registry.freeze();
        }
    }

    @Test
    void protectsAllInfrastructureOnlyDuringRounds()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            ServerLevel level = mock(ServerLevel.class);
            for (var block : blocks)
            {
                when(level.getBlockState(BlockPos.ZERO)).thenReturn(block.defaultBlockState());
                when(manager.isRoundRunning()).thenReturn(true);
                assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
                assertFalse(block.canDropFromExplosion(block.defaultBlockState(), level, BlockPos.ZERO, mock(Explosion.class)));
                assertEquals(PushReaction.BLOCK, block.getPistonPushReaction(block.defaultBlockState()));
                when(manager.isRoundRunning()).thenReturn(false);
                assertFalse(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
            }
            when(manager.isRoundRunning()).thenReturn(true);
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(Blocks.STONE.defaultBlockState());
            assertFalse(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
        }
    }

    @Test
    void onlyDirectOperatorBreakingGetsAnException()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            ServerLevel level = mock(ServerLevel.class);
            ServerPlayer player = mock(ServerPlayer.class);
            var block = blocks.get(0);
            var state = block.defaultBlockState();
            when(level.getBlockState(BlockPos.ZERO)).thenReturn(state);
            assertFalse(block.onDestroyedByPlayer(state, level, BlockPos.ZERO, player, false, Fluids.EMPTY.defaultFluidState()));
            verify(level, never()).removeBlock(any(), anyBoolean());
            when(player.hasPermissions(2)).thenReturn(true);
            // NeoForge 1.21.1 breaks blocks on the server through Level.removeBlock, which ends in the guarded setBlock.
            when(level.removeBlock(eq(BlockPos.ZERO), anyBoolean())).thenAnswer(call -> !TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
            assertTrue(block.onDestroyedByPlayer(state, level, BlockPos.ZERO, player, false, Fluids.EMPTY.defaultFluidState()));
            // An operator-caused blast still has no direct-edit scope, even immediately after a permitted break.
            assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
            block.onBlockExploded(state, level, BlockPos.ZERO, mock(Explosion.class));
            verify(level, times(1)).removeBlock(any(), anyBoolean());
            verify(level, never()).setBlock(any(), any(), anyInt());
        }
    }

    @Test
    @SuppressWarnings("AutoCloseableResource") // Mockito worlds are borrowed test fixtures, not owned closeable resources.
    void directRemovalCannotLeakToAnotherPositionOrSurviveAnException()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            ServerLevel level = mock(ServerLevel.class);
            ServerLevel otherLevel = mock(ServerLevel.class);
            var state = blocks.get(3).defaultBlockState();
            when(level.getBlockState(any())).thenReturn(state);
            when(otherLevel.getBlockState(any())).thenReturn(state);
            assertThrows(IllegalStateException.class, () -> TeamsObjectProtection.removeDirectly(level, BlockPos.ZERO, () ->
            {
                assertFalse(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
                assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO.above()));
                assertTrue(TeamsObjectProtection.preventsRemoval(otherLevel, BlockPos.ZERO));
                throw new IllegalStateException("Failed edit");
            }));
            assertTrue(TeamsObjectProtection.preventsRemoval(level, BlockPos.ZERO));
        }
    }
}
